package com.example.treadtracksproto;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.parse.Parse;
import com.parse.ParseAnalytics;

public class StatsPage extends Activity {

	private String estimatedDistance;
	private EditText distBox;
	private TextView finalText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_stats_page);
		finalText = (TextView) findViewById(R.id.lastDistanceFinal);
		distBox = (EditText) findViewById(R.id.lastDistanceEdit);
		estimatedDistance = distBox.getText().toString();
	}

	public void confirm(View v) {
		estimatedDistance = distBox.getText().toString();
		Button confirm = (Button) findViewById(R.id.confirmButton);
		Button clear = (Button) findViewById(R.id.clearButton);
		confirm.setVisibility(View.GONE);
		clear.setVisibility(View.GONE);
		distBox.setVisibility(View.GONE);
		finalText.setText(estimatedDistance);
		finalText.setVisibility(View.VISIBLE);
	}

	public void reset(View v) {
		distBox.setText(estimatedDistance, TextView.BufferType.EDITABLE);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		// getMenuInflater().inflate(R.menu.stats_page, menu);
		// No menu on non-main pages yet
		return true;
	}

}
