package com.mosect.app.audiomixer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaFormat;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.mosect.lib.audiomixer.AudioTrack;
import com.mosect.lib.audiomixer.PcmType;

import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Act/Main";

    private Button btnAction;
    private boolean running = false;
    private AudioDecoder audioDecoder;
    private AudioPlayer audioPlayer;
    private MixPlayer mixPlayer;
    private MicCapture micCapture;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityResultLauncher<String> audioPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                result -> {
                    Log.d(TAG, "audioPermissionLauncher: " + result);
                }
        );
        running = false;

        setContentView(R.layout.activity_main);
        btnAction = findViewById(R.id.btn_action);
        btnAction.setOnClickListener(v -> {
            if (running) {
                stopAction();
            } else {
                startAction();
            }
            updateActionUI();
        });
        updateActionUI();

        if (!hasAudioRecordPermission()) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAction();
    }

    private void startAction() {
        running = true;
        mixPlayer = new MixPlayer();
        mixPlayer.start();
        audioDecoder = new AudioDecoder(this, "test01.mp3");
        audioDecoder.setSyncPlayTime(false);
        audioDecoder.setCallback(new AudioDecoder.Callback() {
            private AudioTrack track = null;

            @Override
            public void onFormatConfigured(MediaFormat format, int sampleRate, int channelCount) {
                track = mixPlayer.requestTrack(sampleRate, channelCount, PcmType.BIT16);
            }

            @Override
            public void onWritePcm(ByteBuffer data) {
                int size = data.remaining();
                int offset = data.position();
                int limit = data.limit();
                Log.d(TAG, "audioDecoder/onWritePcm: " + data);
                int writeLen = 0;
                while (writeLen < size) {
                    data.position(writeLen + offset);
                    data.limit(limit);
                    int len = track.write(data);
                    if (len < 0) {
                        if (len == AudioTrack.WRITE_RESULT_FULL) {
                            // 已填满
                            track.flush();
                            Log.d(TAG, "audioDecoder/onWritePcm: waitUnlock");
                            track.waitUnlock();
                        } else {
                            // 发生错误
                            break;
                        }
                    } else {
                        writeLen += len;
                    }
                }
            }
        });
        audioDecoder.start();
        if (hasAudioRecordPermission()) {
            micCapture = new MicCapture();
            micCapture.setCallback(new MicCapture.Callback() {
                private AudioTrack track = null;

                @Override
                public void onConfigure(int sampleRate, int channelCount) {
                    track = mixPlayer.requestTrack(sampleRate, channelCount, PcmType.BIT16);
                }

                @Override
                public void onWritePcm(byte[] data) {
                    Log.d(TAG, "micCapture/onWritePcm: ");
                    int writeLen = 0;
                    while (writeLen < data.length) {
                        int len = track.write(data, 0, data.length);
                        if (len < 0) {
                            if (len == AudioTrack.WRITE_RESULT_FULL) {
                                // 已填满
                                track.flush();
                                Log.d(TAG, "micCapture/onWritePcm: waitUnlock");
                                track.waitUnlock();
                            } else {
                                // 发生错误
                                break;
                            }
                        } else {
                            writeLen += len;
                        }
                    }
                }
            });
            micCapture.start();
        }
//        audioPlayer = new AudioPlayer(new AudioDecoder(this, "test01.mp3"));
//        audioPlayer.start();
    }

    private void stopAction() {
        running = false;
        if (null != mixPlayer) {
            mixPlayer.release();
            mixPlayer = null;
        }
        if (null != audioDecoder) {
            audioDecoder.release();
            audioDecoder = null;
        }
        if (null != audioPlayer) {
            audioPlayer.release();
            audioPlayer = null;
        }
        if (null != micCapture) {
            micCapture.release();
            micCapture = null;
        }
    }

    private void updateActionUI() {
        if (running) {
            btnAction.setText("停止");
        } else {
            btnAction.setText("开始");
        }
    }

    private boolean hasAudioRecordPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }
}
