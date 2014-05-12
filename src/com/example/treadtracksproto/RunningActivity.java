package com.example.treadtracksproto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Stack;
import java.util.concurrent.*;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
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
import be.hogent.tarsos.dsp.onsets.OnsetHandler;

import com.smp.soundtouchandroid.SoundTouchPlayable;

public class RunningActivity extends Activity {
	private String TAG = "treadtracks";
    private RunningActivity mainActivity = this;
    final Integer PLAYLIST_ACTIVITY = 1;

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
	private AlertDialog modeDetDialog;
	private boolean isPlaying = false;
	private boolean isShuffle = false;
	private int currentSongIndex;
	private Stack<Integer> prevSongs = new Stack<Integer>();
	private Stack<Integer> nextSongs = new Stack<Integer>();
	private float currentSongBpm = -1;

	private long startTime = 0;
	private long endTime = 0;

    //Clap/Accelerometer variables
    float targetChange = 1f;
    float realChange = 1f;

    //Clap detection variables
    ScheduledExecutorService clapExecutorService = Executors.newSingleThreadScheduledExecutor();
    ScheduledFuture<?> clapTargetHandle;
    ScheduledFuture<?> targetToRealHandle;
    ClapDetector clapDetector;

    // Detection mode and beat detection variables
    private int detMode = 0; // 0 = Manual, 1 = Clap, 2 = Accelerometer
    ScheduledFuture<?> accelTargetHandle;
    boolean refreshed = false;

    // Accelerometer-based step detector
    private StepDetector stepDetector;

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

		OnsetHandler stepHandler = new OnsetHandler() {
			@Override
			public void handleOnset(double time, double salience) {
				onStepDetected(time);
			}
		};
		stepDetector = new StepDetector(
				(SensorManager) this.getSystemService(SENSOR_SERVICE),
				stepHandler);

		setPlaylist("all", "0"); // set playlist to all music and song to the
									// first one

		final String[] detChoices = { "Manual", "Claps", "Accelerometer" };
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Choose Detection Mode");
		builder.setSingleChoiceItems(detChoices, 0,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
                        int prevDetMode = detMode;
                        detMode = i;
						tempoSeekBar.setEnabled(i == 0);
						dialogInterface.dismiss();
                        if(detMode != prevDetMode){
                            //Leaving mode cleanup
                            if(prevDetMode == 1) { //Switching away from clap detection mode
                                clapDetector.finishRecord();
                                targetToRealHandle.cancel(true);
                                clapTargetHandle.cancel(true);
                            } else if (prevDetMode == 2){
                                targetToRealHandle.cancel(true);
                                accelTargetHandle.cancel(true);
                            }

                            //Entering mode init
                            if(detMode == 1){   //Clap Detection Mode
                                //Start listening for claps
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        clapDetector = new ClapDetector(mainActivity);
                                        clapDetector.startRecord();
                                    }
                                }).start();

                                //Update target percent change based on claps detected
                                //Taken from accelerometer onStepDetected
                                clapTargetHandle = clapExecutorService.scheduleAtFixedRate(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(!refreshed){
                                            targetChange = 1f;
                                        }
                                        refreshed = false;
                                    }
                                }, 0, 1500, TimeUnit.MILLISECONDS);

                                //Gently change real % change to target % change to avoid sudden jumps
                                targetToRealHandle = clapExecutorService.scheduleAtFixedRate(new Runnable() {
                                    @Override
                                    public void run() {
                                        float diff = targetChange - realChange;
                                        if(Math.abs(diff) > 0.05f){
                                            realChange += (diff/Math.abs(diff))*0.05f;
                                        } else {
                                            realChange = targetChange;
                                        }

                                        if(st != null){
                                            st.setTempo(realChange);
                                        }

                                        tempoSeekBar.setProgress(percentToProg(realChange));
                                    }
                                }, 0, 200, TimeUnit.MILLISECONDS);
                            }
                            else if(detMode == 2){
                                accelTargetHandle = clapExecutorService.scheduleAtFixedRate(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(!refreshed){
                                            targetChange = 1f;
                                        }
                                        refreshed = false;
                                    }
                                }, 0, 1500, TimeUnit.MILLISECONDS);

                                //Gently change real % change to target % change to avoid sudden jumps
                                targetToRealHandle = clapExecutorService.scheduleAtFixedRate(new Runnable() {
                                    @Override
                                    public void run() {
                                        float diff = targetChange - realChange;
                                        if(Math.abs(diff) > 0.05f){
                                            realChange += (diff/Math.abs(diff))*0.05f;
                                        } else {
                                            realChange = targetChange;
                                        }

                                        if(st != null){
                                            st.setTempo(realChange);
                                        }
                                        tempoSeekBar.setProgress(percentToProg(realChange));
                                    }
                                }, 0, 200, TimeUnit.MILLISECONDS);
                            }
                        }
					}
				});
		modeDetDialog = builder.create();

		playImageButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (!isPlaying) {
					if (!isRunning) {
						isRunning = true;
						startRun.setText(R.string.stop_run);
						startRun.setBackgroundResource(R.drawable.rounded_button_red);
						setNewSong(currentSongIndex);
						startTime = SystemClock.elapsedRealtime();
					}
					st.play();
					playImageButton
							.setBackgroundResource(R.drawable.icon_22165);
					isPlaying = true;
				} else {
					st.pause();
					playImageButton
							.setBackgroundResource(R.drawable.icon_22164);
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
					if (isShuffle) {
						Log.d("Tag", "NEXT PICKING RANDOM SONG");
						setNewSong(pickRandomSong());
					} else {
						Log.d("Tag", "NEXT NOT PICKING RANDOM SONG");
						if (currentSongIndex + 1 < songAdapter.getCount()) {
							setNewSong(currentSongIndex + 1);
						} else {
							setNewSong(0);
						}
					}
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
						if (isShuffle) {
							setNewSong(pickRandomSong());
						} else {
							if (currentSongIndex - 1 >= 0) {
								setNewSong(currentSongIndex - 1);
							} else {
								setNewSong(songAdapter.getCount() - 1);
							}
						}
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
					startTime = SystemClock.elapsedRealtime();
				} else { // else take user to the stats page
					SharedPreferences settings = getSharedPreferences(
							"TreadTracksPref", 0);
					SharedPreferences.Editor editor = settings.edit();
					editor.putBoolean("dialogShown", true);
					editor.commit();
					isRunning = false;
					startRun.setText(R.string.start_run);
					startRun.setBackgroundResource(R.drawable.rounded_button);
					st.pause();
					playImageButton
							.setBackgroundResource(R.drawable.icon_22164);
					isPlaying = false;
					endTime = SystemClock.elapsedRealtime();
					long runDuration = endTime - startTime;
					statsPageIntent.putExtra("runDuration", runDuration);
					startActivity(statsPageIntent);
				}
			}
		});

		tempoSeekBar
				.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
					@Override
					public void onStopTrackingTouch(SeekBar arg0) {
					}

					@Override
					public void onStartTrackingTouch(SeekBar arg0) {
					}

					@Override
					public void onProgressChanged(SeekBar bar, int value,
							boolean unused) {
                        if (detMode == 0) {
                            // Sets the tempo based on the seek bar value
                            // Seek bar goes from 0 to 100, so we need to adjust
                            // value
                            // Current range: 85 to 150
                            if (st != null) {
                                // Want the value to range from .85 to 1.5
                                st.setTempo(progToPercent(value));
                            }
                        }
					}
				});
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == PLAYLIST_ACTIVITY) {
			if (resultCode == Activity.RESULT_OK) {

				setPlaylist(data.getStringExtra("playlistID"),
						data.getStringExtra("songPosition"));

				setSongTitleAndArtist(currentSongIndex);

				if (isPlaying) {
					setNewSong(currentSongIndex);
				}

				// setNewSong(currentSongIndex);
			}
		}
	}

	public void setPlaylist(String playlistID, String songPosition) {

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
		if (songPosition == null || songPosition.equals("shuffle")) {
			Log.d("TAG", "shuffle is true, picking random song");
			isShuffle = true;
			currentSongIndex = pickRandomSong();
		} else {
			isShuffle = false;
			currentSongIndex = Integer.parseInt(songPosition);
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.dialog_title);
		builder.setAdapter(songAdapter, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				prevSongs.push(currentSongIndex);

				if (isPlaying) {
					setNewSong(i);
				} else {
					setSongTitleAndArtist(i);
				}
			}
		});
		songListDialog = builder.create();
	}

	private void setSongTitleAndArtist(int i) {
		if (!isPlaying) {
			SongItem item = songAdapter.getSongItem(i);
			songNameTextView.setText(item.getTitle());
			artistNameTextView.setText(item.getArtist());
			albumArtImageView.setImageBitmap(item.getAlbumArt());
		}
	}

	private void setNewSong(int i) {

		Log.d(TAG, "setting new song");

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
					item.getFilepath(), 0, 1f, 0) {
			};
			new Thread(st).start();
			songNameTextView.setText(item.getTitle());
			artistNameTextView.setText(item.getArtist());
			albumArtImageView.setImageBitmap(item.getAlbumArt());
			playImageButton.setBackgroundResource(R.drawable.icon_22165);
            tempoSeekBar.setProgress(percentToProg(1f));
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
			if (isShuffle) {
				setNewSong(pickRandomSong());
			} else {
				if (currentSongIndex + 1 < songAdapter.getCount()) {
					setNewSong(currentSongIndex + 1);
				} else {
					setNewSong(0);
				}
			}
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

			Intent i = new Intent(this, PlaylistActivity.class);
			startActivityForResult(i, PLAYLIST_ACTIVITY);
			break;

		case R.id.action_detection_mode:
			modeDetDialog.show();
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

		// Register the accelerometer
		stepDetector.registerListener();
	}

	private void updateCurrentBPM() {
		currentSongBpm = -1;
		SongItem item = songAdapter.getSongItem(currentSongIndex);
		new SongBpmRetriever().getBpm(item.getTitle(), item.getArtist(), this);
	}

    private void onStepDetected (double timestamp) {
        if (detMode == 2) {
            double pace = stepDetector.getStepsPerMinute();
            // Want it to be full at 200 steps/min
            // Bottom at ~50 steps/min
            double songBpm = (currentSongBpm > 0) ? currentSongBpm : 100;
            double tempoRatio = pace / songBpm;
            if (pace == -1) tempoRatio = 1;
            else if (tempoRatio < .85) tempoRatio = .85;
            else if (tempoRatio > 1.5) tempoRatio = 1.5;

            targetChange = (float) tempoRatio;
            refreshed = true;
        }
    }

    public void onClapDetected (double timestamp) {
        if (detMode == 1) {
            double pace = clapDetector.getClapsPerMinute();
            double songBpm = (currentSongBpm > 0) ? currentSongBpm : 100;
            double tempoRatio = pace / songBpm;
            if (pace == -1) tempoRatio = 1;
            else if (tempoRatio < .85) tempoRatio = .85;
            else if (tempoRatio > 1.5) tempoRatio = 1.5;

            targetChange = (float) tempoRatio;
            refreshed = true;
        }
    }

    public float progToPercent(int progress){
        if(progress < 50){
            return 0.003f * progress + 0.85f;
        } else if(progress > 50){
            return 0.01f * progress + 0.5f;
        } else {
            return 1;
        }
    }

    public int percentToProg(float percent){
        if(percent < 1){
            return (int) (333.333f * percent - 283.333f);
        } else if(percent > 1){
            return (int) (100f * percent - 50f);
        } else {
            return 50;
        }
    }
	
	public void setCurrentSongBpm(float currSongBpm) {
		currentSongBpm = currSongBpm;
	}
}
