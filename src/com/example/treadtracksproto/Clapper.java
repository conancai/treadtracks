package com.example.treadtracksproto;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class Clapper {
    static final String TAG = "clapper";
    short[] buffer;
    public static final int SAMPLE_RATE = 8000;
    private AudioRecord recorder;
    private boolean isRecording = true;
    private int amplitudeThreshold = 7000;
    RunningActivity mainActivity;

    private MovingAverage ambientNoise = new MovingAverage(5);

    public Clapper(RunningActivity run) {
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
            Log.d(TAG, Integer.toString(amplitude));
            int ampDifference = amplitude - ambientNoise.getAverage();
            if (ampDifference >= amplitudeThreshold && amplitude >= 8000) {
                mainActivity.addClapCount();

                //Discard bytes
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
