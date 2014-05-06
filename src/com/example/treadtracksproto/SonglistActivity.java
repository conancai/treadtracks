package com.example.treadtracksproto;

import java.util.ArrayList;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

public class SonglistActivity extends ListActivity {

	String playlistID;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_songlist);

		getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		getActionBar().setCustomView(R.layout.actionbar);

		Intent intent = getIntent();
		playlistID = intent.getExtras().getString("playlistID");

		TextView title = (TextView) findViewById(R.id.songlist_title);
		title.setText(intent.getExtras().getString("name"));

		Cursor cursor;
		ContentResolver cr = this.getContentResolver();
		String selection = MediaStore.Audio.Media.IS_MUSIC + "!=0";

		ArrayList<SongItem> songData = new ArrayList<SongItem>();

		if (playlistID.equals("all")) {

			// Query the MediaStore for all music files
			String[] projection = { MediaStore.Audio.Media.TITLE,
					MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DATA,
					MediaStore.Audio.Media.ALBUM_ID };
			String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
			Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
			cr = this.getContentResolver();
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

		SongAdapter adapter = new SongAdapter(this, R.layout.song_row_item,
				songData.toArray(new SongItem[0]));

		ListView view = getListView(); // (ListView)
										// findViewById(R.id.songlist);
		view.setAdapter(adapter);

		// view.setOnItemClickListener(listener );
		/*
		 * if (savedInstanceState == null) {
		 * getFragmentManager().beginTransaction() .add(R.id.container, new
		 * PlaceholderFragment()).commit(); }
		 */
	}

	protected void onListItemClick(ListView listView, View view, int position,
			long id) {
		super.onListItemClick(listView, view, position, id);

		// SongItem item = (SongItem) listView.getItemAtPosition(position);
		// Log.d("TAG", "Song name: " + item.getTitle() + " song artist: " +
		// item.getArtist());

		Intent i = new Intent();
		i.putExtra("playlistID", playlistID);
		i.putExtra("songPosition", Integer.toString(position));
		setResult(Activity.RESULT_OK, i);
		
		Log.d("TAG", "results");
		
		finish();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.songlist, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		switch (item.getItemId()) {
		case R.id.action_previous_runs:
			startActivity(new Intent(this, StatsPage.class));
			break;

		case R.id.action_player:
			startActivity(new Intent(this, RunningActivity.class));
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_songlist,
					container, false);
			return rootView;
		}
	}

}
