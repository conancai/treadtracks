package com.example.treadtracksproto;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class RunningActivity extends Activity {

	private Button startRun;
	private boolean isRunning = false; // initial state is not running
	private boolean isFirstTimeRunning = true;

	private Intent calibrationIntent;
	private Intent statsPageIntent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_running);

		// initialize variables
		startRun = (Button) findViewById(R.id.start_run);
		calibrationIntent = new Intent(this, CalibrationPage.class);
		statsPageIntent = new Intent(this, StatsPage.class);

		startRun.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// if pressed when not running, change button to red and change
				// text to "Stop Run"
				if (!isRunning) {
					isRunning = true;
					startRun.setText("Stop Run");
					startRun.setBackgroundResource(R.drawable.rounded_button_red);
				} else { // else take user to the calibration page
					isRunning = false;
					if (isFirstTimeRunning) {
						isFirstTimeRunning = false;
						startActivity(calibrationIntent);
						startRun.setText("Start Run");
						startRun.setBackgroundResource(R.drawable.rounded_button);
					} else {
						startActivity(statsPageIntent);
					} // if, else
				} // if, else
			} // onClick
		} // onClickListener
		); // setOnClickListener
	} // onCreate

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.running, menu);
		return true;
	}

}
