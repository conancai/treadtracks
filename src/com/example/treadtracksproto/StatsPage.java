package com.example.treadtracksproto;

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

	private String estimatedDistance;
	private EditText distBox;
	private TextView finalText;
	private ParseQueryAdapter.QueryFactory<StatsPost> factory;
	private ParseQueryAdapter<StatsPost> posts;

	private void doListQuery() {
		posts.loadObjects();
	}

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
						.findViewById(R.id.distanceView);
				TextView paceView = (TextView) view.findViewById(R.id.paceView);
				TextView timeView = (TextView) view.findViewById(R.id.timeView);
				String date = post.getDate();
				String dist = post.getDistance();
				String pace = post.getPace();
				String time = post.getTime();
				dateView.setText(post.getDate());
				distanceView.setText(post.getDistance());
				paceView.setText(post.getPace());
				timeView.setText(post.getTime());
				return view;
			}
		};

		posts.setAutoload(false);

		ListView postsView = (ListView) this.findViewById(R.id.stats_view);
		postsView.setAdapter(posts);

		AlertDialog.Builder alert = new AlertDialog.Builder(StatsPage.this);
		alert.setTitle("Enter Distance");
		final EditText input = new EditText(StatsPage.this);
		input.setInputType(InputType.TYPE_CLASS_TEXT
				| InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
		alert.setView(input);
		// Handle the dialog input
		alert.setPositiveButton("Done", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				// Create a post.
				StatsPost post = new StatsPost();
				post.setDate("2-4-14");
				post.setDistance(input.getText().toString());
				post.setPace("10:05 Mi.");
				post.setTime("30 Minutes");
				ParseACL acl = new ParseACL();
				// Give public read access
				acl.setPublicReadAccess(true);
				post.setACL(acl);
				// Save the post
				post.saveInBackground(new SaveCallback() {
					@Override
					public void done(ParseException e) {
						// Update the list view
						doListQuery();
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

		// finalText = (TextView) findViewById(R.id.lastDistanceFinal);
		// distBox = (EditText) findViewById(R.id.lastDistanceEdit);
		// estimatedDistance = distBox.getText().toString();
	}

	// public void confirm(View v) {
	// estimatedDistance = distBox.getText().toString();
	// Button confirm = (Button) findViewById(R.id.confirmButton);
	// Button clear = (Button) findViewById(R.id.clearButton);
	// confirm.setVisibility(View.GONE);
	// clear.setVisibility(View.GONE);
	// distBox.setVisibility(View.GONE);
	// finalText.setText(estimatedDistance);
	// finalText.setVisibility(View.VISIBLE);
	// }
	//
	// public void reset(View v) {
	// distBox.setText(estimatedDistance, TextView.BufferType.EDITABLE);
	// }

	@Override
	protected void onResume() {
		super.onResume();
		doListQuery();
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
