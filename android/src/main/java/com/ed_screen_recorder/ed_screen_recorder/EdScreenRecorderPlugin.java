package com.ed_screen_recorder.ed_screen_recorder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.util.Log;

import androidx.annotation.NonNull;

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

/** EdScreenRecorderPlugin */
public class EdScreenRecorderPlugin implements FlutterPlugin, ActivityAware, MethodCallHandler,
        PluginRegistry.RequestPermissionsResultListener,
        PluginRegistry.ActivityResultListener, HBRecorderListener {

    private FlutterPluginBinding flutterPluginBinding;
    private ActivityPluginBinding activityPluginBinding;
    Result flutterResult;
    Activity activity;
    private MethodChannel channel;
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

    public static void registerWith(Registrar registrar) {
        final EdScreenRecorderPlugin instance = new EdScreenRecorderPlugin();
        instance.setupChannels(registrar.messenger(), registrar.activity());
        registrar.addActivityResultListener(instance);
        registrar.addRequestPermissionsResultListener(instance);
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
        this.flutterResult = result;
        if (call.method.equals("startRecordScreen")) {
            try {
                isAudioEnabled = call.argument("audioenable");
                fileName = call.argument("filename");
                dirPathToSave = call.argument("dirpathtosave");
                addTimeCode = call.argument("addtimecode");
                videoFrame = call.argument("videoframe");
                videoBitrate = call.argument("videobitrate");
                fileOutputFormat = call.argument("fileoutputformat");
                fileExtension = call.argument("fileextension");
                videoHash = call.argument("videohash");
                startDate = call.argument("startdate");
                customSettings(videoFrame, videoBitrate, fileOutputFormat, addTimeCode, fileName);
                if (dirPathToSave != null) {
                    System.out.println(">>>>>>>>>>> 1");
                    setOutputPath(addTimeCode, fileName, dirPathToSave);
                }
                success = startRecordingScreen();
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
                result.success(jsonObj.toString());
                System.out.println("Error: " + e.getMessage());
            }
        } else if (call.method.equals("pauseRecordScreen")) {
            hbRecorder.pauseScreenRecording();
        } else if (call.method.equals("resumeRecordScreen")) {
            hbRecorder.resumeScreenRecording();
        } else if (call.method.equals("stopRecordScreen")) {
            endDate = call.argument("enddate");
            hbRecorder.stopScreenRecording();
        } else {
            result.notImplemented();
        }
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == SCREEN_RECORD_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    if (resultCode == Activity.RESULT_OK) {
                        hbRecorder.startScreenRecording(data, resultCode, activity);
                    }
                }
            }
        }
        return true;
    }

    @Override
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        return false;
    }

    private void setupChannels(BinaryMessenger messenger, Activity activity) {
        if (activityPluginBinding != null) {
            activityPluginBinding.addActivityResultListener(this);
            activityPluginBinding.addRequestPermissionsResultListener(this);
        }
        this.activity = activity;
        channel = new MethodChannel(messenger, "ed_screen_recorder");
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
        flutterResult.success(jsonObj.toString());
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
            flutterResult.success(jsonObj.toString());
        } catch (Exception e) {
            System.out.println("Error:" + e.getMessage());
        }
    }

    @Override
    public void HBRecorderOnError(int errorCode, String reason) {
        Log.e("Video Error:", reason);
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
            System.out.println("Error:" + e.getMessage());
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
            System.out.println(">>>>>>>>>>> 2" + fileName);
            hbRecorder.setFileName(generateFileName(fileName, addTimeCode));
        }
    }

    private void setOutputPath(boolean addTimeCode, String fileName, String dirPathToSave) throws IOException {
        hbRecorder.setFileName(generateFileName(fileName, addTimeCode));
        if (dirPathToSave != null && dirPathToSave != "") {
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
