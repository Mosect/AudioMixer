package com.mosect.app.audiomixer;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Act/Main";

    private Button btnAction;
    private boolean running = false;
    private AudioPlayer audioPlayer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAction();
    }

    private void startAction() {
        running = true;
        audioPlayer = new AudioPlayer(this, "test01.mp3");
        audioPlayer.setOnPcmPlayListener((data, frameTime, size) -> {
            Log.d(TAG, "playPcm: " + frameTime);
        });
        audioPlayer.start();
    }

    private void stopAction() {
        running = false;
        if (null != audioPlayer) {
            audioPlayer.release();
            audioPlayer = null;
        }
    }

    private void updateActionUI() {
        if (running) {
            btnAction.setText("停止");
        } else {
            btnAction.setText("开始");
        }
    }
}
