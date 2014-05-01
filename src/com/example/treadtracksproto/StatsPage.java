package com.example.treadtracksproto;

import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseQueryAdapter;
import com.parse.SaveCallback;

public class StatsPage extends Activity {

	private ParseQueryAdapter.QueryFactory<StatsPost> factory;
	private ParseQueryAdapter<StatsPost> posts;
	private ListView postsView;

	private long millis;
	private int seconds;
	private int minutes;
	private int hours;

	private AlertDialog dialog;

	private NumberFormat df = new DecimalFormat("00");

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_stats_page);

		getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		getActionBar().setCustomView(R.layout.actionbar);

		factory = new ParseQueryAdapter.QueryFactory<StatsPost>() {
			public ParseQuery<StatsPost> create() {
				ParseQuery<StatsPost> query = StatsPost.getQuery();
				query.orderByDescending("createdAt");
				query.setLimit(5);
				return query;
			}
		};

		posts = new ParseQueryAdapter<StatsPost>(this, factory) {

			@Override
			public View getItemView(StatsPost post, View view, ViewGroup parent) {
				if (view == null) {
					view = View
							.inflate(getContext(), R.layout.stats_item, null);
				}
				super.getItemView(post, view, parent);
				TextView dateView = (TextView) view.findViewById(R.id.dateView);
				TextView distanceView = (TextView) view
						.findViewById(R.id.distView);
				TextView paceView = (TextView) view.findViewById(R.id.paceView);
				TextView timeView = (TextView) view.findViewById(R.id.timeView);

				dateView.setText(post.getDate());
				timeView.setText(post.getTime());
				distanceView.setText(post.getDistance());
				paceView.setText(post.getPace());
				return view;
			}
		};

		posts.setAutoload(false);

		postsView = (ListView) this.findViewById(R.id.stats_view);
		postsView.setDividerHeight(10);
		postsView.setAdapter(posts);

		AlertDialog.Builder alert = new AlertDialog.Builder(StatsPage.this);
		alert.setTitle("Enter Distance Ran In Miles");
		final EditText input = new EditText(StatsPage.this);
		input.setInputType(InputType.TYPE_CLASS_NUMBER
				| InputType.TYPE_NUMBER_FLAG_DECIMAL);
		alert.setView(input);
		// Handle the dialog input
		alert.setPositiveButton("Done", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				// Create a post.
				StatsPost post = new StatsPost();

				Format sdf = new SimpleDateFormat("MM-dd-yy");
				String date = sdf.format(new Date());
				post.setDate(date);

				String dist = input.getText().toString();
				post.setDistance(dist);

				String time = timeFormat();
				post.setTime(time);

				int distNum = Integer.parseInt(dist);
				String pace = paceFormat(distNum);
				post.setPace(pace);

				ParseACL acl = new ParseACL();
				// Give public read access
				acl.setPublicReadAccess(true);
				post.setACL(acl);
				// Save the post
				post.saveInBackground(new SaveCallback() {
					@Override
					public void done(ParseException e) {
						// Update the list view
						posts.loadObjects();
					}
				});
			}
		});
		alert.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// Do nothing.
					}
				});
		alert.create().show();
	}

	private String paceFormat(int dist) {
		int paceNum = (int) Math.floor(seconds / dist);

		int paceSeconds = (int) Math.floor(paceNum);
		int paceMinutes = paceSeconds / 60;
		paceMinutes = (int) Math.floor(paceMinutes);
		paceSeconds = paceSeconds % 60;
		paceMinutes = paceMinutes % 60;
		String pace = df.format(paceMinutes) + ":" + df.format(paceSeconds);
		return pace;
	}

	private String timeFormat() {
		millis = getIntent().getLongExtra("runDuration", 0);
		seconds = (int) (millis / 1000);
		seconds = (int) Math.floor(seconds);
		minutes = seconds / 60;
		minutes = (int) Math.floor(minutes);
		hours = minutes / 60;
		hours = (int) Math.floor(hours);
		int roundedSeconds = seconds % 60;
		int roundedMinutes = minutes % 60;
		String time = df.format(hours) + ":" + df.format(roundedMinutes) + ":"
				+ df.format(roundedSeconds);
		return time;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.stats_page, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_player:
			startActivity(new Intent(this, RunningActivity.class));
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onResume() {
		super.onResume();
		posts.loadObjects();
	}

	/*
	 * Called when the Activity is no longer visible at all. Stop updates and
	 * disconnect.
	 */
	@Override
	public void onStop() {
		super.onStop();
	}

	/*
	 * Called when the Activity is restarted, even before it becomes visible.
	 */
	@Override
	public void onStart() {
		super.onStart();
	}

}
