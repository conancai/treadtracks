package com.example.treadtracksproto;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class StatsPage extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_stats_page);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.stats_page, menu);
		return true;
	}

}
