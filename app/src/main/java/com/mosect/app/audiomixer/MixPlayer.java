package com.mosect.app.audiomixer;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.util.Log;

import com.mosect.lib.audiomixer.AudioMixer;
import com.mosect.lib.audiomixer.PcmType;

import java.nio.ByteBuffer;

public class MixPlayer {

    private static final String TAG = "Mixer";
    private final static int SAMPLE_RATE = 48000;

    private int state = 0;
    private AudioMixer mixer;

    public MixPlayer() {
        mixer = new AudioMixer(SAMPLE_RATE, 2, 10000, PcmType.BIT16);
    }

    public void start() {
        if (state == 0) {
            state = 1;
            new Thread(this::loop).start();
        }
    }

    public void release() {
        if (state != 2) {
            state = 2;
            mixer.release();
            mixer = null;
        }
    }

    public com.mosect.lib.audiomixer.AudioTrack requestTrack(int sampleRate, int channelCount, PcmType pcmType) {
        return mixer.requestTrack(sampleRate, channelCount, pcmType);
    }

    private void loop() {
        AudioTrack audioTrack = null;
        try {
            int bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
            audioTrack.play();
            byte[] data = null;
            while (state == 1) {
                ByteBuffer buffer = mixer.tickAndWait();
                if (null != buffer) {
                    int size = buffer.remaining();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        int limit = buffer.limit();
                        int writeLen = 0;
                        int offset = buffer.position();
                        while (writeLen < size) {
                            buffer.position(offset + writeLen);
                            buffer.limit(limit);
                            int len = audioTrack.write(buffer, size - writeLen, AudioTrack.WRITE_BLOCKING);
                            if (len < 0) break;
                            writeLen += len;
                        }
                    } else {
                        if (null == data || data.length < size) {
                            data = new byte[size];
                        }
                        buffer.get(data, 0, size);
                        int writeLen = 0;
                        while (writeLen < size) {
                            int len = audioTrack.write(data, writeLen, size - writeLen);
                            if (len < 0) break;
                            writeLen += len;
                        }
                    }
                    mixer.unlock();
                    audioTrack.flush();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "loop: ", e);
        } finally {
            if (null != audioTrack) audioTrack.release();
        }
    }
}
