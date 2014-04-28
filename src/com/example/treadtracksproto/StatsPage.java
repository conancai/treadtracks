package com.example.treadtracksproto;

import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_stats_page);

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

				post.setDistance(input.getText().toString());
				post.setPace("10:05");

				String time = timeFormat();
				post.setTime(time);
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

	private String timeFormat() {
		NumberFormat df = new DecimalFormat("00");
		long millis = getIntent().getLongExtra("runDuration", 0);
		int seconds = (int) (millis / 1000);
		seconds = (int) Math.floor(seconds);
		int minutes = seconds / 60;
		minutes = (int) Math.floor(minutes);
		int hours = minutes / 60;
		hours = (int) Math.floor(hours);
		seconds = seconds % 60;
		minutes = minutes % 60;
		String time = df.format(hours) + ":" + df.format(minutes) + ":"
				+ df.format(seconds);
		return time;
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		// getMenuInflater().inflate(R.menu.stats_page, menu);
		// No menu on non-main pages yet
		return true;
	}

}
