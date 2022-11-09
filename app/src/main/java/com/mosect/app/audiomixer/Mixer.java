package com.mosect.app.audiomixer;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import com.mosect.lib.audiomixer.AudioMixer;
import com.mosect.lib.audiomixer.PcmType;

public class Mixer {

    private static final String TAG = "Mixer";
    private final static int SAMPLE_RATE = 48000;

    private int state = 0;

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

    public void putPcm() {

    }

    private void loop() {
        AudioTrack audioTrack = null;
        AudioMixer mixer = null;
        try {
            int bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
            audioTrack.play();
            mixer = new AudioMixer(SAMPLE_RATE, 2, 10000, PcmType.BIT16);

        } catch (Exception e) {
            Log.e(TAG, "loop: ", e);
        } finally {
            if (null != audioTrack) audioTrack.release();
            if (null != mixer) mixer.release();
        }
    }

}
