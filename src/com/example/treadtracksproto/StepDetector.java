package com.example.treadtracksproto;

import java.util.Calendar;

import be.hogent.tarsos.dsp.onsets.OnsetHandler;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class StepDetector implements SensorEventListener {


	private OnsetHandler handler;
	
	private Sensor mAccelerometerSensor;
	private SensorManager mSensorManager;
	
	int stepIndex = 0;
	long [] stepTimes = new long[6];
	int numSteps;
	
	private boolean currentlyStepping;
	
	private static final double STEP_START_THRESHOLD = 2;
	private static final double STEP_END_THRESHOLD = 1;
	// Used to ignore old steps if a break is taken
	private static final double STEP_TIMEOUT = 10000;
	
	public StepDetector(SensorManager manager, OnsetHandler handler) {
		// Initializes 
		this.handler = handler;
		this.mSensorManager = manager;
		this.mAccelerometerSensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	}
	
	public void registerListener() {
        mSensorManager.registerListener(this, mAccelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
	}
	
	public void unregisterListener() {
		mSensorManager.unregisterListener(this);		
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// Acceleration values
		float x = event.values[0];
		float y = event.values[1];
		float z = event.values[2];
		// Force on the phone
		float g = (x * x + y * y + z * z) / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);

		if (currentlyStepping == false) {
			if (g > STEP_START_THRESHOLD) {
				// Record a step
				numSteps += 1;
				currentlyStepping = true;
				
				long timestamp = System.currentTimeMillis();
				stepTimes[stepIndex] = timestamp;
				stepIndex += 1;
				stepIndex %= stepTimes.length;
				
				// Calls the handler
				this.handler.handleOnset(timestamp, 1);
			}
		} else {
			// Currently taking a step
			if (g < STEP_END_THRESHOLD) {
				currentlyStepping = false;
			}
		}
	}
	
	public double getStepsPerMinute() {
		// Our index will now be our oldest step
		if (numSteps >= stepTimes.length) {
			long timestamp = System.currentTimeMillis();
			long oldestStep = -1; //stepTimes[stepIndex];
			int stepsSkipped = 0;
			for (int i = 0; i < stepTimes.length; i++) {
				int index = (i + stepIndex) % stepTimes.length;
				if ((timestamp - stepTimes[index]) < STEP_TIMEOUT) {
					oldestStep = stepTimes[index];
					break;
				}
				stepsSkipped += 1;
			}
			// Milliseconds for 10 steps
			long timeForSteps = timestamp - oldestStep;
			double stepsPerMin = (double) 60000 / (double) timeForSteps *
					(stepTimes.length - stepsSkipped);
			
			//return stepsSkipped;
			if (oldestStep > 0 && timeForSteps > 1 && stepsSkipped < 3) {
			//	// Got a valid pace
				return stepsPerMin;
			} else {
				// No valid points
				return -1;
			}
		} else {
			// Return -1 if we haven't got 10 steps yet
			return -1;
		}
	}
}
