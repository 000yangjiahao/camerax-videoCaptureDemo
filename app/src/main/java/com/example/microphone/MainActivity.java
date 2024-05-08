package com.example.microphone;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.core.VideoCapture;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ListenableFuture<ProcessCameraProvider> processCameraProviderListenableFuture;
    PreviewView previewView;
    Button captureButton;
    private VideoCapture videoCapture;

    private static final int RECORD_AUDIO_REQUEST_CODE = 1;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 2;
    private static final int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        captureButton = findViewById(R.id.captureButton);
        captureButton.setOnClickListener(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            initializeCamera();
        }

    }

    private void initializeCamera() {
        processCameraProviderListenableFuture = ProcessCameraProvider.getInstance(this);
        processCameraProviderListenableFuture.addListener(() -> {
            try {
                ProcessCameraProvider processCameraProvider = processCameraProviderListenableFuture.get();
                start(processCameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @SuppressLint("RestrictedApi")
    private void start(ProcessCameraProvider processCameraProvider) {
        processCameraProvider.unbindAll();

        List<CameraInfo> cameraInfoList = processCameraProvider.getAvailableCameraInfos();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .addCameraFilter(cameraInfo -> Collections.singletonList(cameraInfoList.get(1)))
                .build();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        videoCapture = new VideoCapture.Builder()
                .setVideoFrameRate(30)
                .build();

        processCameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.captureButton) {
            if (captureButton.getText().equals("录像")) {
                captureButton.setText("停止");
                captureVideo();
            } else {
                captureButton.setText("录像");
                videoCapture.stopRecording();
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private void captureVideo() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_REQUEST_CODE);
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
            return;
        }

        if (videoCapture != null) {
            long timeStamp = System.currentTimeMillis();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, timeStamp);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");

            videoCapture.startRecording(
                    new VideoCapture.OutputFileOptions.Builder(
                            getContentResolver(),
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            contentValues
                    ).build(),
                    ContextCompat.getMainExecutor(this),
                    new VideoCapture.OnVideoSavedCallback() {
                        @Override
                        public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {
                            Toast.makeText(MainActivity.this, "Saving...", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
                            Toast.makeText(MainActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeCamera();
            } else {
                Toast.makeText(this, "需要摄像头权限才能使用相机功能", Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == RECORD_AUDIO_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                captureVideo();
            } else {
                Toast.makeText(this, "需要录音权限才能录制视频", Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == WRITE_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                captureVideo();
            } else {
                Toast.makeText(this, "需要写入外部存储权限才能录制视频", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
