package com.example.treadtracksproto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Stack;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.smp.soundtouchandroid.SoundTouchPlayable;

public class RunningActivity extends Activity {

	// start/stop run variables
	private Button startRun;
	private boolean isRunning = false; // initial state is not running
	private boolean isFirstTimeRunning = true;

	private Intent calibrationIntent;
	private Intent statsPageIntent;

	// music player variables
	private ImageButton btnPlay;
	private ImageButton btnNext;
	private ImageButton btnPrevious;
	private ImageButton bpmUp;
	private ImageButton bpmDown;
	private TextView bpmDisplay;
	private TextView songName;
	private TextView artistName;
	private ImageView albumArt;

	// private MediaPlayer mediaPlayer;
	// MediaMetadataRetriever retriever;

	private Cursor cursor = null;
	private SoundTouchPlayable st = null;
	private SongAdapter adapter;
	private AlertDialog dialog;
	private boolean isPlaying = false;
	private int currentSongIndex;
	private Stack<Integer> prevSongs = new Stack<Integer>();
	private Stack<Integer> nextSongs = new Stack<Integer>();
	private int bpm = 100;
	// private ArrayList<HashMap<String, String>> songList = new
	// ArrayList<HashMap<String, String>>();
	// private ArrayList<String> songList = new ArrayList<String>();

	private long startTime = 0;

	private long endTime = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_running);

		// initialize variables for music player
		btnPlay = (ImageButton) findViewById(R.id.play);
		btnNext = (ImageButton) findViewById(R.id.next_song);
		btnPrevious = (ImageButton) findViewById(R.id.previous_song);
		bpmUp = (ImageButton) findViewById(R.id.bpm_up);
		bpmDown = (ImageButton) findViewById(R.id.bpm_down);
		bpmDisplay = (TextView) findViewById(R.id.bpm_num);
		songName = (TextView) findViewById(R.id.textView3);
		artistName = (TextView) findViewById(R.id.textView2);
		albumArt = (ImageView) findViewById(R.id.album_art);

		// mediaPlayer = new MediaPlayer();
		// retriever = new MediaMetadataRetriever();
		//
		// mediaPlayer.setOnCompletionListener(this);

		// initialize variables for start/stop run
		startRun = (Button) findViewById(R.id.start_run);
		calibrationIntent = new Intent(this, CalibrationPage.class);
		statsPageIntent = new Intent(this, StatsPage.class);

		// Query the MediaStore for all music files
		String[] projection = { MediaStore.Audio.Media.TITLE,
				MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DATA,
				MediaStore.Audio.Media.ALBUM_ID };
		String selection = MediaStore.Audio.Media.IS_MUSIC + "!=0";
		String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
		Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		ContentResolver cr = this.getContentResolver();
		cursor = cr.query(uri, projection, selection, null, sortOrder);
		cursor.moveToFirst();

		ArrayList<SongItem> songData = new ArrayList<SongItem>();
		for (int i = 0; i < cursor.getCount(); i++) {
			songData.add(new SongItem(this, cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.TITLE)), cursor
					.getString(cursor
							.getColumnIndex(MediaStore.Audio.Media.ARTIST)),
					cursor.getString(cursor
							.getColumnIndex(MediaStore.Audio.Media.DATA)),
					cursor.getString(cursor
							.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))));
			cursor.moveToNext();
		}
		cursor.close();

		adapter = new SongAdapter(this, R.layout.song_row_item,
				songData.toArray(new SongItem[0]));
		currentSongIndex = pickRandomSong();
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.dialog_title);

		builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				prevSongs.push(currentSongIndex);
				setNewSong(i);
			}
		});
		dialog = builder.create();

		btnPlay.setOnClickListener(new View.OnClickListener() {
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
					btnPlay.setBackgroundResource(R.drawable.icon_22165);
					isPlaying = true;
				} else {
					st.pause();
					btnPlay.setBackgroundResource(R.drawable.icon_22164);
					isPlaying = false;
				}
			}
		});

		btnNext.setOnClickListener(new View.OnClickListener() {
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

		btnPrevious.setOnClickListener(new View.OnClickListener() {
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
					btnPlay.setBackgroundResource(R.drawable.icon_22164);
					isPlaying = false;
					endTime = System.currentTimeMillis();
					long runDuration = endTime - startTime;

					if (isFirstTimeRunning) {
						isFirstTimeRunning = false;
						startActivity(calibrationIntent);
					} else {
						statsPageIntent.putExtra("runDuration", runDuration);
						startActivity(statsPageIntent);
					}
				}
			}
		});

		bpmDown.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (bpm - 5 >= 0) {
					bpm -= 5;
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							bpmDisplay.setText(String.valueOf(bpm - 100));
						}
					});
				}
				if (st != null) {
					st.setTempo(bpm / 100f);
				}
			}
		});

		bpmUp.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (bpm + 5 <= 200) {
					bpm += 5;
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							bpmDisplay.setText(String.valueOf(bpm - 100));
						}
					});
				}
				if (st != null) {
					st.setTempo(bpm / 100f);
				}
			}
		});

		// // get mp3s
		// File home = Environment.getExternalStorageDirectory();
		// getPlayList(home);
		//
		// // start by adding first song to musicPlayer
		// Uri uri = Uri.parse(songList.get(currentSongIndex));
		// try {
		// mediaPlayer.setDataSource(this, uri);
		// mediaPlayer.prepare();
		// } catch (IllegalArgumentException e) {
		// e.printStackTrace();
		// } catch (SecurityException e) {
		// e.printStackTrace();
		// } catch (IllegalStateException e) {
		// e.printStackTrace();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }

		// btnPlay.setOnClickListener(new View.OnClickListener() {
		// @Override
		// public void onClick(View v) {
		// if (mediaPlayer.isPlaying()) {
		// if(mediaPlayer != null) {
		// mediaPlayer.pause();
		// btnPlay.setBackgroundResource(R.drawable.icon_22164);
		// }
		// } else {
		// if(mediaPlayer != null) {
		// if (!isRunning) {
		// isRunning = true;
		// startRun.setText("Stop Run");
		// startRun.setBackgroundResource(R.drawable.rounded_button_red);
		// }
		// mediaPlayer.start();
		// btnPlay.setBackgroundResource(R.drawable.icon_22165);
		// }
		// }
		// }
		// });

		// next song button click event
		// btnNext.setOnClickListener(new View.OnClickListener() {
		// @Override
		// public void onClick(View arg0) {
		// if(currentSongIndex < (songList.size() - 1)) {
		// playSong(currentSongIndex + 1);
		// currentSongIndex = currentSongIndex + 1;
		// } else {
		// playSong(0);
		// currentSongIndex = 0;
		// }
		// }
		// });
		//
		// // prev song button click event
		// btnPrevious.setOnClickListener(new View.OnClickListener() {
		// @Override
		// public void onClick(View v) {
		// if (currentSongIndex < (songList.size() - 1)) {
		// playSong(currentSongIndex + 1);
		// currentSongIndex = currentSongIndex + 1;
		// } else {
		// playSong(0);
		// currentSongIndex = 0;
		// }
		// }
		// });

		// startRun.setOnClickListener(new View.OnClickListener() {
		// public void onClick(View v) {
		// // if pressed when not running, change button to red and change
		// // text to "Stop Run", start music
		// if (!isRunning) {
		// isRunning = true;
		// startRun.setText("Stop Run");
		// startRun.setBackgroundResource(R.drawable.rounded_button_red);
		// playSong(currentSongIndex);
		// btnPlay.setBackgroundResource(R.drawable.icon_22165);
		// } else { // else take user to the calibration page
		// isRunning = false;
		// startRun.setText("Start Run");
		// startRun.setBackgroundResource(R.drawable.rounded_button);
		// btnPlay.setBackgroundResource(R.drawable.icon_22164);
		// mediaPlayer.pause();
		// mediaPlayer.reset();
		//
		// if (isFirstTimeRunning) {
		// isFirstTimeRunning = false;
		// startActivity(calibrationIntent);
		// } else {
		// startActivity(statsPageIntent);
		// }
		// }
		// }
		// });
	}

	private void setNewSong(int i) {
		currentSongIndex = i;
		try {
			if (st != null) {
				st.stop();
				st = null;
				isPlaying = false;
			}
			SongItem item = adapter.getSongItem(i);
			st = new SoundTouchPlayable(new SongProgressListener(),
					item.getFilepath(), 0, bpm / 100f, 0) {

			};
			new Thread(st).start();
			songName.setText(item.getTitle());
			artistName.setText(item.getArtist());
			albumArt.setImageBitmap(item.getAlbumArt());
			btnPlay.setBackgroundResource(R.drawable.icon_22165);
			st.play();
			isPlaying = true;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private int pickRandomSong() {
		return new Random().nextInt(adapter.getCount());
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

	// public void getPlayList(File dir) {
	// if (dir.listFiles(new FileExtensionFilter()).length > 0) {
	// for (File file : dir.listFiles()) {
	// Log.d("debug", "found .mp3 file");
	// Log.d("debug", file.toString());
	//
	// songList.add(file.getPath());
	//
	// /*HashMap<String, String> song = new HashMap<String, String>();
	// String path = file.getPath();
	//
	// retriever.setDataSource(this, Uri.parse(path));
	// song.put("songPath", path);
	// song.put("songTitle",
	// retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
	// song.put("songArtist",
	// retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
	//
	// // Adding each song to SongList
	// songList.add(song);*/
	// }
	// }
	//
	// File[] files = dir.listFiles();
	//
	// for (File file : files) {
	// if (file.isDirectory())
	// getPlayList(file);
	// }
	// }

	// class FileExtensionFilter implements FilenameFilter {
	// public boolean accept(File dir, String name) {
	// return (name.endsWith(".mp3") || name.endsWith(".MP3"));
	// }
	// }

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
			dialog.show();
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	// // receiving song index from playlist view and playing the song
	// protected void onActivityResult(int requestCode, int resultCode, Intent
	// data) {
	// super.onActivityResult(requestCode, resultCode, data);
	// if (resultCode == 100) {
	// currentSongIndex = data.getExtras().getInt("songIndex");
	// playSong(currentSongIndex);
	// }
	// }

	// public void playSong(int songIndex) {
	// try {
	// Log.d("debug", "in playSong function");
	// Log.d("debug", "songPath: " + songList.get(songIndex));
	//
	// Uri uri = Uri.parse(songList.get(songIndex));
	//
	// mediaPlayer.reset();
	// mediaPlayer.setDataSource(this, uri);
	// mediaPlayer.prepare();
	// mediaPlayer.start();
	//
	// retriever.setDataSource(this, uri);
	// String songTitle =
	// retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
	// String artist =
	// retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
	// byte[] albumArt = retriever.getEmbeddedPicture();
	//
	// if (albumArt != null) {
	// Log.d("debug", "album art exists");
	// } else {
	// Log.d("debug", "album art does not exist");
	// }
	//
	// songName.setText(songTitle);
	// artistName.setText(artist);
	//
	// //TODO: set artistName to artist name
	//
	// //updateProgressBar();
	// } catch (IllegalArgumentException e) {
	// e.printStackTrace();
	// } catch (IllegalStateException e) {
	// e.printStackTrace();
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// }

	// @Override
	// public void onPrepared(MediaPlayer mp) {
	// //mp.start();
	// }

	// @Override
	// public void onCompletion(MediaPlayer arg0) {
	// if(currentSongIndex < (songList.size() - 1)) {
	// playSong(currentSongIndex + 1);
	// currentSongIndex = currentSongIndex + 1;
	// } else {
	// playSong(0);
	// currentSongIndex = 0;
	// }
	// }
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (st != null) {
			st.stop();
		}
		// mediaPlayer.release();
	}

	// @Override
	// protected void onPause() {
	// super.onPause();
	// if(isPlaying){
	// st.pause();
	// btnPlay.setBackgroundResource(R.drawable.icon_22164);
	// isPlaying = false;
	// }
	//
	// }

	@Override
	protected void onResume() {
		super.onResume();
		SongItem item = adapter.getSongItem(currentSongIndex);
		songName.setText(item.getTitle());
		artistName.setText(item.getArtist());
		albumArt.setImageBitmap(item.getAlbumArt());
	}
}
