package com.example.treadtracksproto;

import android.os.Bundle;
import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class CalibrationPage extends Activity {
	
	// Views to be udpated
	TextView strideText;
	TextView paceText;
	EditText distanceEdit;
	
	// Run variables
	Integer time = 9 * 60; // Time in seconds
	Integer steps = 2000; // Steps taken
	Double distance = 1.5; // Distance entered (mi)
	
	// For stride calculation
	final static int FEET_PER_MILE = 5280;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_calibration_page);
		
		strideText = (TextView) this.findViewById(R.id.strideText);
		paceText = (TextView) this.findViewById(R.id.paceText);
		distanceEdit = (EditText) this.findViewById(R.id.distanceEdit);
		distanceEdit.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable arg0) {
				try {
					distance = Double.valueOf(arg0.toString());
					recalculateStride();
					recalculatePace();
				} catch (Exception e) {
					// Do nothing, bad value
				}
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,
					int arg2, int arg3) {
			}

			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,
					int arg3) {
			}
			
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.calibration_page, menu);
		return true;
	}
	
	private void recalculateStride() {
		// Updates the stride value on the page
		int feetWalked = (int) (FEET_PER_MILE * distance);
		Double newStride = (double) feetWalked / steps;
		strideText.setText(String.format("%.2f", newStride));
		strideText.invalidate();
	}
	
	private void recalculatePace() {
		// Updates the pace on the page
		double newPace = (60 * 60 / (double) time) * distance;
		paceText.setText(String.format("%.2f",  newPace));
		paceText.invalidate();
	}
	
	public void finishRun(View v) {
		finish();
	}

}
