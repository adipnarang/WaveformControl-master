
package com.newventuresoftware.waveformdemo;

/* record():
   1) Use Android's AudioRecord class to create an instance of it
   2) The stopRecording() and Read() will read the data into a buffer(array)

   startReading():
   1) Creates a runnable thread to run record()

   stopReading():
   1) stops the runnable thread
*/

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

// This class starts and stops recording
public class RecordingThread {

    // Static variables cant be used by the instance
    private static final String LOG_TAG = RecordingThread.class.getSimpleName(); // Tag = "RecordingThread"
    private static final int SAMPLE_RATE = 44100;

    // Constructor to initialize the variable mListener
    public RecordingThread(AudioDataReceivedListener listener) {
        mListener = listener;
    }

    // Variable declaration
    private boolean mShouldContinue;
    private AudioDataReceivedListener mListener; // variable of the interface AudioDataReceivedListener type
    private Thread mThread;

    public boolean recording() {
        return mThread != null;
    }

    // Method to start recording
    public void startRecording() {
        if (mThread != null) // case when already recording
            return;

        mShouldContinue = true;
        mThread = new Thread(new Runnable() { // Creating a thread using Runnable to tell mThread what code to run
            @Override
            public void run() {
                record(); // tells the thread that the code to run is record()
            }
        });
        mThread.start();
    }

    // Method to stop recording
    public void stopRecording() {
        if (mThread == null) // case when not recording
            return;

        mShouldContinue = false;
        mThread = null; // stop thread i.e. stop recording
    }

    // How to record
    private void record() {
        Log.v(LOG_TAG, "Start"); // Logging in Android
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO); //priority given to audio threads

        // Returns the minimum buffer size required for the successful creation of an AudioRecord object, in byte units
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        // Increase buffer size in case of error
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2;
        }

        short[] audioBuffer = new short[bufferSize / 2]; // Buffer to which the audio data is written

        //Instance of the Audio record class of Android
        AudioRecord record1 = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, /* right now AudioSource set to default
                                                                                might have to change it to Microphone */
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, /* The format specified in the AudioRecord constructor should be
                                                    ENCODING_PCM_16BIT to correspond to the data in the array */
                bufferSize);

        if (record1.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(LOG_TAG, "Audio Record can't initialize!");
            return;
        }
        record1.startRecording(); //startRecording here is android's internal method to start recording

        Log.v(LOG_TAG, "Start recording"); // Logging

        long shortsRead = 0;
        while (mShouldContinue) {
            int numberOfShort = record1.read(audioBuffer, 0, audioBuffer.length); /* Reads audio data from the audio hardware
                                                                                    for recording into a short array. Returns
                                                                                    0 or positive on success */
            shortsRead += numberOfShort;

            // Notify waveform
            mListener.onAudioDataReceived(audioBuffer);
        }

        record1.stop(); // Stops recording from the instance record1
        record1.release(); // Releases the native AudioRecord resources.

        Log.v(LOG_TAG, String.format("Recording stopped. Samples read: %d", shortsRead));
    }
}
