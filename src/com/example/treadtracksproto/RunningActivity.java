package com.example.treadtracksproto;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Random;
import java.util.Stack;

import android.content.Context;
import android.os.AsyncTask;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.smp.soundtouchandroid.SoundTouchPlayable;

public class RunningActivity extends Activity {
	private String API_KEY = "0SU5PIXAMC7BHFFLK";
	private String TAG = "treadtracks";
    private Context context = this;

	// start/stop run variables
	private Button startRun;
	private boolean isRunning = false; // initial state is not running

	private Intent statsPageIntent;

	// music player variables
	private ImageButton playImageButton;
	private ImageButton nextImageButton;
	private ImageButton previousImageButton;
	private SeekBar tempoSeekBar;
	private TextView songNameTextView;
	private TextView artistNameTextView;
	private ImageView albumArtImageView;

	private Cursor cursor = null;
	private SoundTouchPlayable st = null;
	private SongAdapter songAdapter;
	private AlertDialog songListDialog;
	private boolean isPlaying = false;
	private int currentSongIndex;
	private Stack<Integer> prevSongs = new Stack<Integer>();
	private Stack<Integer> nextSongs = new Stack<Integer>();
	private int currentSongBpm = -1;

	private String playlistID = null;
	private String songPosition = null;

	private long startTime = 0;
	private long endTime = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_running);

		getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		getActionBar().setCustomView(R.layout.actionbar);

		// initialize variables for music player
		playImageButton = (ImageButton) findViewById(R.id.play);
		nextImageButton = (ImageButton) findViewById(R.id.next_song);
		previousImageButton = (ImageButton) findViewById(R.id.previous_song);
		tempoSeekBar = (SeekBar) findViewById(R.id.tempoSeekBar);
		songNameTextView = (TextView) findViewById(R.id.song_title);
		artistNameTextView = (TextView) findViewById(R.id.artist);
		albumArtImageView = (ImageView) findViewById(R.id.album_art);

		// initialize variables for start/stop run
		startRun = (Button) findViewById(R.id.start_run);
		statsPageIntent = new Intent(this, StatsPage.class);

		Intent intent = getIntent();

		playlistID = intent.getStringExtra("playlistID");
		songPosition = intent.getStringExtra("songPosition");
		Log.d("TAG", "recieving playlistID: " + playlistID + " songPosition: "
				+ songPosition);

		ArrayList<SongItem> songData = new ArrayList<SongItem>();
		String selection = MediaStore.Audio.Media.IS_MUSIC + "!=0";
		ContentResolver cr = this.getContentResolver();
		if (playlistID == null || playlistID.equals("all")) {
			// Query the MediaStore for all music files
			String[] projection = { MediaStore.Audio.Media.TITLE,
					MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DATA,
					MediaStore.Audio.Media.ALBUM_ID };
			String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
			Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
			cursor = cr.query(uri, projection, selection, null, sortOrder);
			cursor.moveToFirst();

			for (int i = 0; i < cursor.getCount(); i++) {
				songData.add(new SongItem(
						this,
						cursor.getString(cursor
								.getColumnIndex(MediaStore.Audio.Media.TITLE)),
						cursor.getString(cursor
								.getColumnIndex(MediaStore.Audio.Media.ARTIST)),
						cursor.getString(cursor
								.getColumnIndex(MediaStore.Audio.Media.DATA)),
						cursor.getString(cursor
								.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))));
				cursor.moveToNext();
			}
			cursor.close();
		} else {
			String[] projection = { MediaStore.Audio.Playlists.Members.TITLE,
					MediaStore.Audio.Playlists.Members.ARTIST,
					MediaStore.Audio.Playlists.Members.DATA,
					MediaStore.Audio.Playlists.Members.ALBUM_ID };
			Long id = Long.parseLong(playlistID);
			Uri uri = MediaStore.Audio.Playlists.Members.getContentUri(
					"external", id);
			cursor = cr.query(uri, projection, selection, null, null);
			cursor.moveToFirst();

			for (int i = 0; i < cursor.getCount(); i++) {
				songData.add(new SongItem(
						this,
						cursor.getString(cursor
								.getColumnIndex(MediaStore.Audio.Playlists.Members.TITLE)),
						cursor.getString(cursor
								.getColumnIndex(MediaStore.Audio.Playlists.Members.ARTIST)),
						cursor.getString(cursor
								.getColumnIndex(MediaStore.Audio.Playlists.Members.DATA)),
						cursor.getString(cursor
								.getColumnIndex(MediaStore.Audio.Playlists.Members.ALBUM_ID))));
				cursor.moveToNext();
			}
			cursor.close();
		}

		songAdapter = new SongAdapter(this, R.layout.song_row_item,
				songData.toArray(new SongItem[0]));
		if (songPosition == null) {
			currentSongIndex = pickRandomSong();
		} else {
			currentSongIndex = Integer.parseInt(songPosition);
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.dialog_title);

		builder.setAdapter(songAdapter, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				prevSongs.push(currentSongIndex);
				setNewSong(i);
			}
		});
		songListDialog = builder.create();

		playImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isPlaying) {
                    if (!isRunning) {
                        isRunning = true;
                        startRun.setText(R.string.stop_run);
                        startRun.setBackgroundResource(R.drawable.rounded_button_red);
                        setNewSong(currentSongIndex);
                    }
                    st.play();
                    playImageButton.setBackgroundResource(R.drawable.icon_22165);
                    isPlaying = true;
                } else {
                    st.pause();
                    playImageButton.setBackgroundResource(R.drawable.icon_22164);
                    isPlaying = false;
                }
            }
        });

		nextImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                prevSongs.push(currentSongIndex);
                if (!isRunning) {
                    isRunning = true;
                    startRun.setText(R.string.stop_run);
                    startRun.setBackgroundResource(R.drawable.rounded_button_red);
                }
                if (!nextSongs.empty()) {
                    setNewSong(nextSongs.pop());
                } else {
                    setNewSong(pickRandomSong());
                }

            }
        });

		previousImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (st.getPlayedDuration() > 5000000l) { // 5 seconds?
                    st.pause();
                    st.seekTo(0, true);
                    st.play();
                } else {
                    nextSongs.push(currentSongIndex);
                    if (!isRunning) {
                        isRunning = true;
                        startRun.setText(R.string.stop_run);
                        startRun.setBackgroundResource(R.drawable.rounded_button_red);
                        setNewSong(currentSongIndex);
                    }
                    if (!prevSongs.empty()) {
                        setNewSong(prevSongs.pop());
                    } else {
                        setNewSong(pickRandomSong());
                    }
                }
            }
        });

		startRun.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (!isRunning) {
					isRunning = true;
					startRun.setText(R.string.stop_run);
					startRun.setBackgroundResource(R.drawable.rounded_button_red);
					setNewSong(currentSongIndex);
					startTime = System.currentTimeMillis();
				} else { // else take user to the calibration page
					isRunning = false;
					startRun.setText(R.string.start_run);
					startRun.setBackgroundResource(R.drawable.rounded_button);
					st.pause();
					playImageButton.setBackgroundResource(R.drawable.icon_22164);
					isPlaying = false;
					endTime = System.currentTimeMillis();
					long runDuration = endTime - startTime;
					statsPageIntent.putExtra("runDuration", runDuration);
					startActivity(statsPageIntent);
				}
			}
		});

		tempoSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar arg0) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {
            }

            @Override
            public void onProgressChanged(SeekBar bar, int value,
                    boolean unused) {
                // Sets the tempo based on the seek bar value
                // Seek bar goes from 0 to 100, so we need to adjust
                // value
                // Current range: 50 to 150
                float tempo = (value + 50);
                if (st != null) {
                    // Want the value to range from .5 to 1.5
                    st.setTempo(tempo / 100f);
                }
            }
        });
	}

	private void setNewSong(int i) {
		currentSongIndex = i;
		updateCurrentBPM();
        try {
			if (st != null) {
				st.stop();
				st = null;
				isPlaying = false;
			}
			SongItem item = songAdapter.getSongItem(i);
			st = new SoundTouchPlayable(new SongProgressListener(),
					item.getFilepath(), 0, 1, 0) {

			};
			new Thread(st).start();
			songNameTextView.setText(item.getTitle());
			artistNameTextView.setText(item.getArtist());
			albumArtImageView.setImageBitmap(item.getAlbumArt());
			playImageButton.setBackgroundResource(R.drawable.icon_22165);
            st.play();
            isPlaying = true;
        } catch (IOException e) {
			e.printStackTrace();
		}
	}

	private int pickRandomSong() {
		return new Random().nextInt(songAdapter.getCount());
	}

	private class SongProgressListener implements
			SoundTouchPlayable.OnProgressChangedListener {
		@Override
		public void onTrackEnd(int track) {
			prevSongs.push(currentSongIndex);
			setNewSong(pickRandomSong());
		}

		@Override
		public void onProgressChanged(int track, double currentPercentage,
				long position) {

		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.running, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_previous_runs:
			startActivity(new Intent(this, StatsPage.class));
			break;

		case R.id.action_songs:
			// show dialog menu
			songListDialog.show();
			break;

		case R.id.action_playlists:
			startActivity(new Intent(this, PlaylistActivity.class));
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (st != null) {
			st.stop();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		SongItem item = songAdapter.getSongItem(currentSongIndex);
		songNameTextView.setText(item.getTitle());
		artistNameTextView.setText(item.getArtist());
		albumArtImageView.setImageBitmap(item.getAlbumArt());
	}

    private class GetBpmAsync extends AsyncTask<String, Integer, Integer>{
        @Override
        protected Integer doInBackground(String... params) {
            String song = params[0];
            String artist = params[1];
            return getBpm(song, artist);
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            currentSongBpm = integer;
            //REMOVE LATER///////////////////////////
            Toast.makeText(context, "BPM:" + currentSongBpm, Toast.LENGTH_SHORT).show();
            /////////////////////////////////////////
        }

        private JSONObject getJSON(String url) throws JSONException{
            final StringBuilder builder = new StringBuilder();
            final HttpClient client = new DefaultHttpClient();
            final HttpGet httpGet = new HttpGet(url);
            try {
                HttpResponse response = client.execute(httpGet);
                HttpEntity entity = response.getEntity();
                InputStream content = entity.getContent();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(content));
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return new JSONObject(builder.toString());
        }

        private Integer getBpm(String song, String artist){
            String base = "http://developer.echonest.com/api/v4/song/";
            String url1 = base + "search?api_key=" + API_KEY + "&artist="
                    + artist.replaceAll("&", "%20").replaceAll(" ", "%20")
                    + "&title="
                    + song.replaceAll("&", "%20").replaceAll(" ", "%20");
            try {
                JSONArray songsArray1 = getJSON(url1).getJSONObject("response")
                        .getJSONArray("songs");
                if (songsArray1.length() > 0) {
                    String songID = songsArray1.getJSONObject(0).getString("id");
                    String url2 = base + "profile?api_key=" + API_KEY + "&id="
                            + songID + "&bucket=audio_summary";
                    JSONArray songsArray2 = getJSON(url2).getJSONObject("response")
                            .getJSONArray("songs");
                    if (songsArray2.length() > 0) {
                        String tempo = songsArray2.getJSONObject(0)
                                .getJSONObject("audio_summary").getString("tempo");
                        return Math.round(Float.parseFloat(tempo));
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return -1;
        }
    }

    private void updateCurrentBPM(){
        currentSongBpm = -1;
        SongItem item = songAdapter.getSongItem(currentSongIndex);
        new GetBpmAsync().execute(item.getTitle(), item.getArtist());
    }
}
