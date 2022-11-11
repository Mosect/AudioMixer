package com.mosect.app.audiomixer;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

public class MicCapture {

    private static final String TAG = "MicCapture";

    private int state = 0;
    private final int sampleRate = 44100;
    private Callback callback;

    public void start() {
        if (state == 0) {
            state = 1;
            new Thread(this::loop).start();
        }
    }

    public void release() {
        if (state != 2) {
            state = 2;
        }
    }

    @SuppressLint("MissingPermission")
    private void loop() {
        AudioRecord audioRecord = null;
        try {
            int size = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, size);
            audioRecord.startRecording();
            configure();
            byte[] buffer = new byte[size];
            while (state == 1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    audioRecord.read(buffer, 0, buffer.length, AudioRecord.READ_BLOCKING);
                } else {
                    int readLen = 0;
                    while (readLen < size) {
                        int len = audioRecord.read(buffer, readLen, size - readLen);
                        if (len < 0) break;
                        if (len > 0) {
                            readLen += len;
                        }
                    }
                }
                Callback callback = this.callback;
                if (null != callback) {
                    callback.onWritePcm(buffer);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "loop: ", e);
        } finally {
            if (null != audioRecord) audioRecord.release();
        }
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    private void configure() {
        Callback callback = this.callback;
        if (null != callback) {
            int channelCount = 1;
            callback.onConfigure(sampleRate, channelCount);
        }
    }

    public interface Callback {

        void onConfigure(int sampleRate, int channelCount);

        void onWritePcm(byte[] data);
    }
}
