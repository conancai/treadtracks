package com.example.treadtracksproto;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class ClapDetector {
    static final String TAG = "clapper";
    short[] buffer;
    public static final int SAMPLE_RATE = 8000;
    private AudioRecord recorder;
    private boolean isRecording = true;
    private int amplitudeThreshold = 6000;
    RunningActivity mainActivity;

    int stepIndex = 0;
    long [] stepTimes = new long[12];
    int numSteps;
    private static final double STEP_TIMEOUT = 10000;

    private MovingAverage ambientNoise = new MovingAverage(5);

    public ClapDetector(RunningActivity run) {
        mainActivity = run;
    }

    public void startRecord()
    {
        Log.d(TAG, "start");
        int minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        buffer = new short[minBufferSize];
        recorder = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize);

        recorder.startRecording();
        while(isRecording){
            double sum = 0;
            int amplitude = 0;
            int readSize = recorder.read(buffer, 0, buffer.length);
            for (int i = 0; i < readSize; i++) {
                sum += buffer [i] * buffer [i];
            }
            if (readSize > 0) {
                amplitude = (int) Math.sqrt(sum / readSize);
            }
            //Log.d(TAG, Integer.toString(amplitude));
            int ampDifference = amplitude - ambientNoise.getAverage();
            if (ampDifference >= amplitudeThreshold && amplitude >= 7000) {

                numSteps += 1;
                long timestamp = System.currentTimeMillis();
                stepTimes[stepIndex] = timestamp;
                stepIndex += 1;
                stepIndex %= stepTimes.length;

                mainActivity.onClapDetected(timestamp);

               // Log.d(TAG, Long.toString(timestamp));

                //Discard bytes
                recorder.read(buffer, 0, buffer.length);
                recorder.read(buffer, 0, buffer.length);
                recorder.read(buffer, 0, buffer.length);
                recorder.read(buffer, 0, buffer.length);
            } else {
                ambientNoise.add(amplitude);
            }
        }
    }

    public void finishRecord()
    {
        if (recorder != null)
        {
            if (isRecording())
            {
                isRecording = false;
            }
            //release resources
            recorder.stop();
            recorder.release();

        }
    }

    public double getClapsPerMinute(){
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
            long timeForSteps = timestamp - oldestStep;
            double stepsPerMin = (double) 60000 / (double) timeForSteps *
                    (stepTimes.length - stepsSkipped);

            //return stepsSkipped;
            if (oldestStep > 0 && timeForSteps > 2 && stepsSkipped < 3) {
                //	// Got a valid pace
                Log.d(TAG, Double.toString(stepsPerMin));
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

    public boolean isRecording()
    {
        return isRecording;
    }

    private class MovingAverage{
        int[] samples;
        int windowSize;
        int total = 0;
        int index = 0;

        public MovingAverage(int size){
            windowSize = size;
            samples = new int[windowSize];
        }

        public void add(int x){
            total -= samples[index%windowSize];
            samples[index%windowSize] = x;
            total += x;
            index++;
        }

        int getAverage() {
            return total / windowSize;
        }
    }
}
