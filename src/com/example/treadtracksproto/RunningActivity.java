package com.example.treadtracksproto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import be.hogent.tarsos.dsp.AudioEvent;
import be.hogent.tarsos.dsp.onsets.OnsetHandler;
import be.hogent.tarsos.dsp.onsets.PercussionOnsetDetector;

import com.smp.soundtouchandroid.SoundTouchPlayable;

public class RunningActivity extends Activity implements
		AudioProc.OnAudioEventListener, OnsetHandler {
	private String TAG = "treadtracks";
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
	private float bpm = 0, currentSongBpm = -1;
	ExecutorService networkService = Executors.newSingleThreadExecutor();

	// private String playlistID = null;
	// private String songPosition = null;
	private long startTime = 0;
	private long endTime = 0;

	// Detection mode and beat detection variables
	private int detMode = 0; // 0 = Manual, 1 = Clap, 2 = Accelerometer
	private double sens = 120, thres = 30; // Might vary depending on phone
	private double[] times = new double[10]; // Stores up to five times to
												// calculate average
	double curTempo = -1;
	private AudioProc mAudioProc;
	private PercussionOnsetDetector onsetDetector;
	private static final int SAMPLE_RATE = 16000;

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
						detMode = i;
						tempoSeekBar.setEnabled(i == 0);
						dialogInterface.dismiss();
						if (detMode == 1) {
							refreshBeats();
						} else if (mAudioProc.isRecording()) {
							mAudioProc.stop();
						}
						// detModeItem.setTitle("Detection: " + detChoices[i]);
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
					}
					st.play();
					refreshBeats();
					playImageButton
							.setBackgroundResource(R.drawable.icon_22165);
					isPlaying = true;
				} else {
					st.pause();
					if (mAudioProc.isRecording())
						mAudioProc.stop();
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
					if (mAudioProc.isRecording())
						mAudioProc.stop();
					st.seekTo(0, true);
					st.play();
					refreshBeats();
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
					if (mAudioProc.isRecording())
						mAudioProc.stop();
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
							// Current range: 50 to 150
							float tempo = (value + 50);
							if (st != null) {
								// Want the value to range from .5 to 1.5
								st.setTempo(tempo / 100f);
							}
						}
					}
				});

		mAudioProc = new AudioProc(SAMPLE_RATE);
		onsetDetector = new PercussionOnsetDetector(SAMPLE_RATE,
				mAudioProc.getBufferSize() / 2, this, sens, thres);
		mAudioProc.setOnAudioEventListener(this);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == PLAYLIST_ACTIVITY) {
			if (resultCode == Activity.RESULT_OK) {

				Log.d("Tag",
						"onactivityresult playlistID: "
								+ data.getStringExtra("playlistID")
								+ " songPosition: "
								+ data.getStringExtra("songPosition"));

				setPlaylist(data.getStringExtra("playlistID"),
						data.getStringExtra("songPosition"));

				setNewSong(currentSongIndex);
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
				setNewSong(i);
			}
		});
		songListDialog = builder.create();
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
			if (mAudioProc.isRecording())
				mAudioProc.stop();
			SongItem item = songAdapter.getSongItem(i);
			st = new SoundTouchPlayable(new SongProgressListener(),
					item.getFilepath(), 0, 1f, 0) {
			};
			new Thread(st).start();
			songNameTextView.setText(item.getTitle());
			artistNameTextView.setText(item.getArtist());
			albumArtImageView.setImageBitmap(item.getAlbumArt());
			playImageButton.setBackgroundResource(R.drawable.icon_22165);
			tempoSeekBar.setProgress(50);
			st.play();
			refreshBeats();
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

	// Beat Detection
	@Override
	public void processAudioProcEvent(AudioEvent ae) {
		onsetDetector.process(ae);
	}

	@Override
	public void handleOnset(final double time, double salience) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				int ct = 0;
				double sum = 0, last = -1;
				times[times.length - 1] = time;
				for (int i = 0; i < times.length; i++) {
					if (times[i] > 0) {
						if (last > 0) {
							// (times[i]-last) is onset interval in seconds
							sum += times[i] - last;
							ct++;
						}
						last = times[i];
					}
					if (i > 0)
						times[i - 1] = times[i];
				}
				if (ct > 0) {
					float songBpm = (currentSongBpm > 0) ? currentSongBpm : 80;
					// (sum/ct) is average interval between onset detections
					bpm = (float) (60 / (sum / ct));
					float tempo = bpm / songBpm;
					artistNameTextView.setText(Float.toString(tempo));
					if (tempo < 0.5f)
						tempo = 0.5f;
					else if (tempo > 1.5f)
						tempo = 1.5f;
					st.setTempo(tempo);
					tempoSeekBar.setProgress((int) (tempo * 100 - 49.5));
				}
			}
		});
	}

	private void refreshBeats() {
		if (detMode == 1) {
			for (int j = 0; j < times.length; j++)
				times[j] = -1;
			if (!mAudioProc.isRecording())
				mAudioProc.listen();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (st != null) {
			st.stop();
		}
		if (mAudioProc.isRecording()) {
			mAudioProc.stop();
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

	private void onStepDetected(double timestamp) {
		if (detMode == 2) {
			double pace = stepDetector.getStepsPerMinute();
			// Want it to be full at 200 steps/min
			// Bottom at ~50 steps/min
			double songBpm = (currentSongBpm > 0) ? currentSongBpm : 100;
			double tempoRatio = pace / songBpm;
			if (tempoRatio < .7)
				tempoRatio = .7;
			if (tempoRatio > 1.5)
				tempoRatio = 1.5;

			// artistNameTextView.setText(String.format("%.2f", tempoRatio));

			if (pace > 0) {
				if (Math.abs(curTempo - tempoRatio) > .1) {
					curTempo = tempoRatio;
					st.setTempo((float) tempoRatio);

					int seekProgress = (int) (pace / 2);
					tempoSeekBar.setProgress(seekProgress);
					artistNameTextView.setText(String.format("%.2f", pace));
				}
			}
		}
	}

	public void setCurrentSongBpm(float currSongBpm) {
		currentSongBpm = currSongBpm;
	}
}
