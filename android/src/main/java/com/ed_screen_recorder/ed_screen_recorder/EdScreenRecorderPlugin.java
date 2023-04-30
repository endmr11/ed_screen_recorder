package com.ed_screen_recorder.ed_screen_recorder;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.hbisoft.hbrecorder.HBRecorder;
import com.hbisoft.hbrecorder.HBRecorderCodecInfo;
import com.hbisoft.hbrecorder.HBRecorderListener;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * EdScreenRecorderPlugin
 */
public class EdScreenRecorderPlugin implements FlutterPlugin, ActivityAware, MethodCallHandler,
        HBRecorderListener,PluginRegistry.ActivityResultListener, PluginRegistry.RequestPermissionsResultListener {

    private FlutterPluginBinding flutterPluginBinding;
    private ActivityPluginBinding activityPluginBinding;
    Result recentResult;
    Result startRecordingResult;
    Result stopRecordingResult;
    Result pauseRecordingResult;
    Result resumeRecordingResult;
    Activity activity;
    private static final int SCREEN_RECORD_REQUEST_CODE = 777;
    private HBRecorder hbRecorder;
    boolean isAudioEnabled;
    String fileName;
    String dirPathToSave;
    boolean addTimeCode;
    String filePath;
    int videoFrame;
    int videoBitrate;
    String fileOutputFormat;
    String fileExtension;
    boolean success;
    String videoHash;
    long startDate;
    long endDate;

    boolean micPermission = false;
    boolean mediaPermission = false;

    private void initializeResults() {
        startRecordingResult = null;
        stopRecordingResult = null;
        pauseRecordingResult = null;
        resumeRecordingResult = null;
        recentResult = null;
    }

    public static void registerWith(Registrar registrar) {
        final EdScreenRecorderPlugin instance = new EdScreenRecorderPlugin();
        instance.setupChannels(registrar.messenger(), registrar.activity());
        registrar.addActivityResultListener(instance);
    }

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        this.flutterPluginBinding = binding;
        hbRecorder = new HBRecorder(flutterPluginBinding.getApplicationContext(), this);
        HBRecorderCodecInfo hbRecorderCodecInfo = new HBRecorderCodecInfo();

    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        this.flutterPluginBinding = null;
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        this.activityPluginBinding = binding;
        setupChannels(flutterPluginBinding.getBinaryMessenger(), binding.getActivity());
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        onAttachedToActivity(binding);
    }

    @Override
    public void onDetachedFromActivity() {
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        initializeResults();
        recentResult = result;
        switch (call.method) {
            case "startRecordScreen":
                try {
                    startRecordingResult = result;
                    isAudioEnabled = Boolean.TRUE.equals(call.argument("audioenable"));
                    fileName = call.argument("filename");
                    dirPathToSave = call.argument("dirpathtosave");
                    addTimeCode = Boolean.TRUE.equals(call.argument("addtimecode"));
                    videoFrame = call.argument("videoframe");
                    videoBitrate = call.argument("videobitrate");
                    fileOutputFormat = call.argument("fileoutputformat");
                    fileExtension = call.argument("fileextension");
                    videoHash = call.argument("videohash");
                    startDate = call.argument("startdate");
                    customSettings(videoFrame, videoBitrate, fileOutputFormat, addTimeCode, fileName);
                    if (dirPathToSave != null) {
                        setOutputPath(addTimeCode, fileName, dirPathToSave);
                    }
                    if (isAudioEnabled) {
                        if (ContextCompat.checkSelfPermission(flutterPluginBinding.getApplicationContext(), Manifest.permission.RECORD_AUDIO)
                                != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(activity,
                                    new String[]{Manifest.permission.RECORD_AUDIO},
                                    333);
                        } else {

                            micPermission = true;
                        }
                    } else {
                        micPermission = true;
                    }

                    if (ContextCompat.checkSelfPermission(flutterPluginBinding.getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {

                        ActivityCompat.requestPermissions(activity,
                                new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                444);
                    } else {
                        mediaPermission = true;
                    }
                    if (micPermission && mediaPermission) {
                        success = startRecordingScreen();
                    }

                } catch (Exception e) {
                    Map<Object, Object> dataMap = new HashMap<Object, Object>();
                    dataMap.put("success", false);
                    dataMap.put("isProgress", false);
                    dataMap.put("file", "");
                    dataMap.put("eventname", "startRecordScreen Error");
                    dataMap.put("message", e.getMessage());
                    dataMap.put("videohash", videoHash);
                    dataMap.put("startdate", startDate);
                    dataMap.put("enddate", endDate);
                    JSONObject jsonObj = new JSONObject(dataMap);
                    startRecordingResult.success(jsonObj.toString());
                    startRecordingResult = null;
                    recentResult = null;
                }
                break;
            case "pauseRecordScreen":
                pauseRecordingResult = result;
                hbRecorder.pauseScreenRecording();
                break;
            case "resumeRecordScreen":
                resumeRecordingResult = result;
                hbRecorder.resumeScreenRecording();
                break;
            case "stopRecordScreen":
                stopRecordingResult = result;
                endDate = call.argument("enddate");
                hbRecorder.stopScreenRecording();
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    @Override
    public boolean onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 333) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                micPermission = true;
            } else {
                micPermission = false;
            }
        } else if (requestCode == 444) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mediaPermission = true;
            } else {
                mediaPermission = false;
            }
        }
        return true;
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SCREEN_RECORD_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    hbRecorder.startScreenRecording(data, resultCode);
                }
            }
        }
        return true;
    }

    private void setupChannels(BinaryMessenger messenger, Activity activity) {
        if (activityPluginBinding != null) {
            activityPluginBinding.addActivityResultListener(this);
        }
        this.activity = activity;
        MethodChannel channel = new MethodChannel(messenger, "ed_screen_recorder");
        channel.setMethodCallHandler(this);
    }

    @Override
    public void HBRecorderOnStart() {

        Log.e("Video Start:", "Start called");
        Map<Object, Object> dataMap = new HashMap<Object, Object>();
        dataMap.put("success", success);
        dataMap.put("isProgress", true);
        if (dirPathToSave != null) {
            dataMap.put("file", filePath + "." + fileExtension);
        } else {
            dataMap.put("file", generateFileName(fileName, addTimeCode) + "." + fileExtension);
        }
        dataMap.put("eventname", "startRecordScreen");
        dataMap.put("message", "Started Video");
        dataMap.put("videohash", videoHash);
        dataMap.put("startdate", startDate);
        dataMap.put("enddate", null);
        JSONObject jsonObj = new JSONObject(dataMap);
        if (startRecordingResult != null) {
            startRecordingResult.success(jsonObj.toString());
            startRecordingResult = null;
            recentResult = null;
        }
    }

    @Override
    public void HBRecorderOnComplete() {
        Log.e("Video Complete:", "Complete called");
        Map<Object, Object> dataMap = new HashMap<Object, Object>();
        dataMap.put("success", success);
        dataMap.put("isProgress", false);
        if (dirPathToSave != null) {
            dataMap.put("file", filePath + "." + fileExtension);
        } else {
            dataMap.put("file", generateFileName(fileName, addTimeCode) + "." + fileExtension);
        }
        dataMap.put("eventname", "stopRecordScreen");
        dataMap.put("message", "Paused Video");
        dataMap.put("videohash", videoHash);
        dataMap.put("startdate", startDate);
        dataMap.put("enddate", endDate);
        JSONObject jsonObj = new JSONObject(dataMap);
        try {
            if (stopRecordingResult != null) {
                stopRecordingResult.success(jsonObj.toString());
                stopRecordingResult = null;
                recentResult = null;
            }
        } catch (Exception e) {
            if (recentResult != null) {
                recentResult.error("Error", e.getMessage(), null);
                recentResult = null;
            }
        }
    }

    @Override
    public void HBRecorderOnError(int errorCode, String reason) {
        Log.e("Video Error:", reason);
        if (recentResult != null) {
            recentResult.error("Error", reason, null);
            recentResult = null;
        }
    }

    @Override
    public void HBRecorderOnPause() {
        if (pauseRecordingResult != null) {
            pauseRecordingResult.success(true);
            pauseRecordingResult = null;
            recentResult = null;
        }
    }

    @Override
    public void HBRecorderOnResume() {
        if (resumeRecordingResult != null) {
            resumeRecordingResult.success(true);
            resumeRecordingResult = null;
            recentResult = null;
        }
    }

    private Boolean startRecordingScreen() {

        try {
            hbRecorder.enableCustomSettings();
            MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) flutterPluginBinding
                    .getApplicationContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            Intent permissionIntent = mediaProjectionManager != null
                    ? mediaProjectionManager.createScreenCaptureIntent()
                    : null;
            activity.startActivityForResult(permissionIntent, SCREEN_RECORD_REQUEST_CODE);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void customSettings(int videoFrame, int videoBitrate, String fileOutputFormat, boolean addTimeCode,
                                String fileName) {
        hbRecorder.isAudioEnabled(isAudioEnabled);
        hbRecorder.setAudioSource("DEFAULT");
        hbRecorder.setVideoEncoder("DEFAULT");
        hbRecorder.setVideoFrameRate(videoFrame);
        hbRecorder.setVideoBitrate(videoBitrate);
        hbRecorder.setOutputFormat(fileOutputFormat);
        if (dirPathToSave == null) {
            hbRecorder.setFileName(generateFileName(fileName, addTimeCode));
        }
    }

    private void setOutputPath(boolean addTimeCode, String fileName, String dirPathToSave) throws IOException {
        hbRecorder.setFileName(generateFileName(fileName, addTimeCode));
        if (dirPathToSave != null && !dirPathToSave.equals("")) {
            File dirFile = new File(dirPathToSave);
            hbRecorder.setOutputPath(dirFile.getAbsolutePath());
            filePath = dirFile.getAbsolutePath() + "/" + generateFileName(fileName, addTimeCode);
        } else {
            hbRecorder.setOutputPath(
                    flutterPluginBinding.getApplicationContext().getExternalCacheDir().getAbsolutePath());
            filePath = flutterPluginBinding.getApplicationContext().getExternalCacheDir().getAbsolutePath() + "/"
                    + generateFileName(fileName, addTimeCode);
        }

    }

    private String generateFileName(String fileName, boolean addTimeCode) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault());
        Date curDate = new Date(System.currentTimeMillis());
        if (addTimeCode) {
            return fileName + "-" + formatter.format(curDate).replace(" ", "");
        } else {
            return fileName;
        }
    }
}
