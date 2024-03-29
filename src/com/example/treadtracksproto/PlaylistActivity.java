package com.example.treadtracksproto;

import java.util.ArrayList;
import java.util.HashMap;

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
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class PlaylistActivity extends ListActivity {

	final Integer PLAYLIST_ACTIVITY = 1;
	
	private TextView playlistName;
	private HashMap<String, String> playlists;

	private ArrayList<String> playlistN;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_playlist);

		getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		getActionBar().setCustomView(R.layout.actionbar);

		// http://www.howtosolvenow.com/2013/02/how-to-create-a-playlist/
		ContentResolver cr = this.getContentResolver();
		Uri uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
		String[] projection = { MediaStore.Audio.Playlists._ID,
				MediaStore.Audio.Playlists.NAME };
		Cursor cursor = cr.query(uri, projection, null, null, null);
		cursor.moveToFirst();

		playlists = new HashMap<String, String>();
		playlistN = new ArrayList<String>();

		String id = "all";
		String name = "All Songs";
		playlists.put(name, id);
		playlistN.add(name);

		for (int i = 0; i < cursor.getCount(); i++) {
			id = cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Playlists._ID));
			name = cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Playlists.NAME));
			// PlaylistItem item = new PlaylistItem(id, name);
			playlists.put(name, id);
			playlistN.add(name);
			cursor.moveToNext();
		}

		playlistName = (TextView) findViewById(R.id.playlist_name);

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				R.layout.playlist_row_item, playlistN);
		// PlaylistAdapter adapter = new PlaylistAdapter(this,
		// R.layout.activity_playlist, playlists);

		setListAdapter(adapter);

		/*
		 * if (savedInstanceState == null) {
		 * getFragmentManager().beginTransaction() .add(R.id.container, new
		 * PlaceholderFragment()).commit(); }
		 */
	}

	protected void onListItemClick(ListView listView, View view, int position,
			long id) {
		super.onListItemClick(listView, view, position, id);
		// PlaylistItem item = (PlaylistItem)
		// listView.getItemAtPosition(position);

		String name = (String) getListAdapter().getItem(position);
		String playlistID = playlists.get(name);

		Log.d("Debug", "name: " + name + " playlistID: " + playlistID);

		Intent i = new Intent(this, SonglistActivity.class);
		i.putExtra("playlistID", playlistID);
		i.putExtra("name", name);
		startActivityForResult(i, PLAYLIST_ACTIVITY);

		// send info to other view, populate other view with items.
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == PLAYLIST_ACTIVITY) {
			if (resultCode == Activity.RESULT_OK){ 
				Intent i = new Intent();
				i.putExtra("playlistID", data.getStringExtra("playlistID"));
				i.putExtra("songPosition", data.getStringExtra("songPosition"));
				
				setResult(Activity.RESULT_OK, i);
				finish();
			}
		}
	}	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.playlist, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_previous_runs:
			startActivity(new Intent(this, StatsPage.class));
			break;

		case R.id.action_player:
			startActivity(new Intent(this, RunningActivity.class));
			break;
		/*
		 * case R.id.action_songs: // show dialog menu dialog.show(); break;
		 */
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
			View rootView = inflater.inflate(R.layout.fragment_playlist,
					container, false);
			return rootView;
		}
	}

}
