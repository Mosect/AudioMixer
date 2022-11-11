package com.mosect.app.audiomixer;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaFormat;
import android.os.Build;

import java.nio.ByteBuffer;

public class AudioPlayer {

    private final AudioDecoder decoder;
    private int state = 0;
    private AudioTrack audioTrack;

    public AudioPlayer(AudioDecoder decoder) {
        this.decoder = decoder;
        decoder.setSyncPlayTime(false);
        decoder.setCallback(new AudioDecoder.Callback() {
            private byte[] buffer = null;

            @Override
            public void onFormatConfigured(MediaFormat format, int sampleRate, int channelCount) {
                synchronized (AudioPlayer.this) {
                    if (state == 1) {
                        int channelConfig;
                        switch (channelCount) {
                            case 1:
                                channelConfig = AudioFormat.CHANNEL_OUT_MONO;
                                break;
                            case 2:
                                channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
                                break;
                            default:
                                throw new IllegalArgumentException("Unsupported channelCount: " + channelCount);
                        }
                        int size = AudioTrack.getMinBufferSize(sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT);
                        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                                channelConfig, AudioFormat.ENCODING_PCM_16BIT, size, AudioTrack.MODE_STREAM);
                        audioTrack.play();
                    }
                }
            }

            @Override
            public void onWritePcm(ByteBuffer data) {
                AudioTrack at = audioTrack;
                if (null != at) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        at.write(data, data.remaining(), AudioTrack.WRITE_BLOCKING);
                    } else {
                        int size = data.remaining();
                        if (null == buffer || buffer.length < size) {
                            buffer = new byte[size];
                        }
                        data.get(buffer, 0, size);
                        at.write(buffer, 0, size);
                        at.flush();
                    }
                }
            }
        });
    }

    public void start() {
        synchronized (this) {
            if (state != 0) return;
            state = 1;
            decoder.start();
        }
    }

    public void release() {
        synchronized (this) {
            if (state == 2) return;
            state = 2;
            decoder.release();
            if (null != audioTrack) {
                audioTrack.release();
                audioTrack = null;
            }
        }
    }
}
