package com.example.treadtracksproto;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class RunningActivity extends Activity implements OnCompletionListener, OnPreparedListener {

	// start/stop run variables
	private Button startRun;
	private boolean isRunning = false; // initial state is not running
	private boolean isFirstTimeRunning = true;
	
	private Intent calibrationIntent;
	private Intent statsPageIntent;
	
	//music player variables
	private ImageButton btnPlay;
	private ImageButton btnNext;
	private ImageButton btnPrevious;
	private TextView songName;
	private TextView artistName;
	private ImageView albumArt;
	
	private MediaPlayer mediaPlayer;
	MediaMetadataRetriever retriever;
	
	private int currentSongIndex = 0;
	//private ArrayList<HashMap<String, String>> songList = new ArrayList<HashMap<String, String>>();
	private ArrayList<String> songList = new ArrayList<String>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_running);

		// initialize variables for music player
		btnPlay = (ImageButton) findViewById(R.id.play);
		btnNext = (ImageButton) findViewById(R.id.next_song);
		btnPrevious = (ImageButton) findViewById(R.id.previous_song);
		songName = (TextView) findViewById(R.id.textView3);
		artistName = (TextView) findViewById(R.id.textView2);
		albumArt = (ImageView) findViewById(R.id.album_art);
		mediaPlayer = new MediaPlayer();
		retriever = new MediaMetadataRetriever();
		
		mediaPlayer.setOnCompletionListener(this);
		
		// initialize variables for start/stop run
		startRun = (Button) findViewById(R.id.start_run);
		calibrationIntent = new Intent(this, CalibrationPage.class);
		statsPageIntent = new Intent(this, StatsPage.class);
		
		// get mp3s
		File home = Environment.getExternalStorageDirectory();
		getPlayList(home);

		// start by adding first song to musicPlayer
		Uri uri = Uri.parse(songList.get(currentSongIndex));
		try {
			mediaPlayer.setDataSource(this, uri);
			mediaPlayer.prepare();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		btnPlay.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mediaPlayer.isPlaying()) {
					if(mediaPlayer != null) {
						mediaPlayer.pause();
						btnPlay.setBackgroundResource(R.drawable.icon_22164);
					}
				} else {
					if(mediaPlayer != null) {
						if (!isRunning) {
							isRunning = true;
							startRun.setText("Stop Run");
							startRun.setBackgroundResource(R.drawable.rounded_button_red);
						}
						mediaPlayer.start();
						btnPlay.setBackgroundResource(R.drawable.icon_22165);
					}
				}
			}
		});
		
		// next song button click event
		btnNext.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if(currentSongIndex < (songList.size() - 1)) {
					playSong(currentSongIndex + 1);
					currentSongIndex = currentSongIndex + 1;
				} else {
					playSong(0);
					currentSongIndex = 0;
				}
			}
		});
		
		// prev song button click event
		btnPrevious.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (currentSongIndex < (songList.size() - 1)) {
					playSong(currentSongIndex + 1);
					currentSongIndex = currentSongIndex + 1;
				} else {
					playSong(0);
					currentSongIndex = 0;
				}
			}
		});
		
		startRun.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// if pressed when not running, change button to red and change
				// text to "Stop Run", start music
				if (!isRunning) {
					isRunning = true;
					startRun.setText("Stop Run");
					startRun.setBackgroundResource(R.drawable.rounded_button_red);
					playSong(currentSongIndex);
					btnPlay.setBackgroundResource(R.drawable.icon_22165);
				} else { // else take user to the calibration page
					isRunning = false;
					startRun.setText("Start Run");
					startRun.setBackgroundResource(R.drawable.rounded_button);
					btnPlay.setBackgroundResource(R.drawable.icon_22164);
					mediaPlayer.pause();
					mediaPlayer.reset();
					
					if (isFirstTimeRunning) {
						isFirstTimeRunning = false;
						startActivity(calibrationIntent);
					} else {
						startActivity(statsPageIntent);
					}
				}
			}
		});
	}
	
	public void getPlayList(File dir) {   
        if (dir.listFiles(new FileExtensionFilter()).length > 0) {
            for (File file : dir.listFiles()) {
            	Log.d("debug", "found .mp3 file");
            	Log.d("debug", file.toString());
            	
            	songList.add(file.getPath());
            	
                /*HashMap<String, String> song = new HashMap<String, String>();
                String path = file.getPath();

                retriever.setDataSource(this, Uri.parse(path));
                song.put("songPath", path);
                song.put("songTitle", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
                song.put("songArtist", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
                
                // Adding each song to SongList
                songList.add(song);*/
            }
        }
        
        File[] files = dir.listFiles();
        
        for (File file : files) {
        	if (file.isDirectory())
        		getPlayList(file);
        }
    }
	
	class FileExtensionFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return (name.endsWith(".mp3") || name.endsWith(".MP3"));
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
	    }
	    return super.onOptionsItemSelected(item);
	}

	
	// receiving song index from playlist view and playing the song
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == 100) {
			currentSongIndex = data.getExtras().getInt("songIndex");
			playSong(currentSongIndex);
		}
	}
	
	public void playSong(int songIndex) {
		try {
			Log.d("debug", "in playSong function");
			Log.d("debug", "songPath: " + songList.get(songIndex));
			
			Uri uri = Uri.parse(songList.get(songIndex));
			
			mediaPlayer.reset();
			mediaPlayer.setDataSource(this, uri);
			mediaPlayer.prepare();
			mediaPlayer.start();
			
			retriever.setDataSource(this, uri);
			String songTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
			String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
			byte[] albumArt = retriever.getEmbeddedPicture();
			
			if (albumArt != null) {
				Log.d("debug", "album art exists");
			} else {
				Log.d("debug", "album art does not exist");
			}
			
			songName.setText(songTitle);
			artistName.setText(artist);
			
			//TODO: set artistName to artist name
			
			//updateProgressBar();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onPrepared(MediaPlayer mp) {
		//mp.start();
	}
	
	@Override
	public void onCompletion(MediaPlayer arg0) {
		if(currentSongIndex < (songList.size() - 1)) {
			playSong(currentSongIndex + 1);
			currentSongIndex = currentSongIndex + 1;
		} else {
			playSong(0);
			currentSongIndex = 0;
		}
	}
	
	public void onDestroy() {
		super.onDestroy();
		mediaPlayer.release();
	}
}

