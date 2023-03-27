package com.ed_screen_recorder.ed_screen_recorder

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.util.Log
import com.hbisoft.hbrecorder.HBRecorder
import com.hbisoft.hbrecorder.HBRecorderListener
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.PluginRegistry.*
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/** EdScreenRecorderPlugin  */
class EdScreenRecorderPlugin : FlutterPlugin, ActivityAware, MethodCallHandler,
    RequestPermissionsResultListener, ActivityResultListener, HBRecorderListener {
    private var flutterPluginBinding: FlutterPluginBinding? = null
    private var activityPluginBinding: ActivityPluginBinding? = null
    private var startResult: MethodChannel.Result? = null
    private var pauseResult: MethodChannel.Result? = null
    private var resumeResult: MethodChannel.Result? = null
    private var stopResult: MethodChannel.Result? = null
    private var recentResult: MethodChannel.Result? = null
    private var activity: Activity? = null
    private var channel: MethodChannel? = null
    private var hbRecorder: HBRecorder? = null
    private var isAudioEnabled = false
    private var fileName: String? = null
    private var dirPathToSave: String? = null
    private var addTimeCode = false
    private var filePath: String? = null
    private var videoFrame = 0
    private var videoBitrate = 0
    private var fileOutputFormat: String? = null
    private var fileExtension: String? = null
    private var success = false
    private var videoHash: String? = null
    private var startDate: Long = 0
    private var endDate: Long = 0
    override fun onAttachedToEngine(binding: FlutterPluginBinding) {
        flutterPluginBinding = binding
        hbRecorder = HBRecorder(flutterPluginBinding!!.applicationContext, this)
    }

    override fun onDetachedFromEngine(binding: FlutterPluginBinding) {
        flutterPluginBinding = null
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activityPluginBinding = binding
        setupChannels(flutterPluginBinding!!.binaryMessenger, binding.activity)
        binding.addActivityResultListener(this)
        binding.addRequestPermissionsResultListener(this)
    }

    override fun onDetachedFromActivityForConfigChanges() {}
    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onDetachedFromActivity() {}
    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        recentResult = result
        when (call.method) {
            "startRecordScreen" -> {
                Log.i("TAG", "startRecordScreen: ")
                startResult = result
                try {
                    isAudioEnabled = call.argument("audioenable")!!
                    fileName = call.argument("filename")
                    dirPathToSave = call.argument("dirpathtosave")
                    addTimeCode = call.argument("addtimecode")!!
                    videoFrame = call.argument("videoframe")!!
                    videoBitrate = call.argument("videobitrate")!!
                    fileOutputFormat = call.argument("fileoutputformat")
                    fileExtension = call.argument("fileextension")
                    videoHash = call.argument("videohash")
                    startDate = call.argument("startdate")!!
                    customSettings(videoFrame, videoBitrate, fileOutputFormat, addTimeCode, fileName)
                    if (dirPathToSave != null) {
                        println(">>>>>>>>>>> 1")
                        setOutputPath(addTimeCode, fileName, dirPathToSave)
                    }
                    success = startRecordingScreen()
                } catch (e: Exception) {
                    val dataMap: MutableMap<Any?, Any?> = HashMap()
                    dataMap["success"] = false
                    dataMap["isProgress"] = false
                    dataMap["file"] = ""
                    dataMap["eventname"] = "startRecordScreen Error"
                    dataMap["message"] = e.message
                    dataMap["videohash"] = videoHash
                    dataMap["startdate"] = startDate
                    dataMap["enddate"] = endDate
                    val jsonObj = JSONObject(dataMap)
                    result.success(jsonObj.toString())
                    println("Error: " + e.message)
                }
            }
            "pauseRecordScreen" -> {
                Log.i("TAG", "pauseRecordScreen: ")
                pauseResult = result
                hbRecorder!!.pauseScreenRecording()
            }
            "resumeRecordScreen" -> {
                Log.i("TAG", "resumeRecordScreen: ")
                resumeResult = result
                hbRecorder!!.resumeScreenRecording()
            }
            "stopRecordScreen" -> {
                Log.i("TAG", "stopRecordScreen: ")
                stopResult = result
                endDate = call.argument("enddate")!!
                hbRecorder!!.stopScreenRecording()
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == SCREEN_RECORD_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    hbRecorder!!.startScreenRecording(data, resultCode)
                }
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ): Boolean {
        return false
    }

    private fun setupChannels(messenger: BinaryMessenger, activity: Activity?) {
        if (activityPluginBinding != null) {
            activityPluginBinding!!.addActivityResultListener(this)
            activityPluginBinding!!.addRequestPermissionsResultListener(this)
        }
        this.activity = activity
        channel = MethodChannel(messenger, "ed_screen_recorder")
        channel!!.setMethodCallHandler(this)
    }

    override fun HBRecorderOnStart() {
        Log.e("Video Start:", "Start called")
        val dataMap: MutableMap<Any?, Any?> = HashMap()
        dataMap["success"] = success
        dataMap["isProgress"] = true
        if (dirPathToSave != null) {
            dataMap["file"] = "$filePath.$fileExtension"
        } else {
            dataMap["file"] = generateFileName(fileName, addTimeCode) + "." + fileExtension
        }
        dataMap["eventname"] = "startRecordScreen"
        dataMap["message"] = "Started Video"
        dataMap["videohash"] = videoHash
        dataMap["startdate"] = startDate
        dataMap["enddate"] = null
        val jsonObj = JSONObject(dataMap)
        Log.i("Video Start:", jsonObj.toString())
        startResult!!.success(jsonObj.toString())
    }

    override fun HBRecorderOnComplete() {
        Log.i("Video Complete:", "Complete called")
        val dataMap: MutableMap<Any?, Any?> = HashMap()
        dataMap["success"] = success
        dataMap["isProgress"] = false
        if (dirPathToSave != null) {
            dataMap["file"] = "$filePath.$fileExtension"
        } else {
            dataMap["file"] = generateFileName(fileName, addTimeCode) + "." + fileExtension
        }
        dataMap["eventname"] = "stopRecordScreen"
        dataMap["message"] = "Paused Video"
        dataMap["videohash"] = videoHash
        dataMap["startdate"] = startDate
        dataMap["enddate"] = endDate
        val jsonObj = JSONObject(dataMap)
        try {
            Log.e("Video Complete:", jsonObj.toString())
            stopResult!!.success(jsonObj.toString())
        } catch (e: Exception) {
            println("Error:" + e.message)
        }
    }

    override fun HBRecorderOnError(errorCode: Int, reason: String) {
        Log.e("Video Error:", reason)
        recentResult!!.error("HBRecorderOnError", reason, errorCode)
    }

    override fun HBRecorderOnPause() {
        Log.i("Video Pause:", "Pause called")
        pauseResult!!.success(true)
    }

    override fun HBRecorderOnResume() {
        Log.i("Video Resume:", "Resume called")
        resumeResult!!.success(true)
    }

    private fun startRecordingScreen(): Boolean {
        return try {
            hbRecorder!!.enableCustomSettings()
            val mediaProjectionManager = flutterPluginBinding!!
                .applicationContext
                .getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val permissionIntent = mediaProjectionManager.createScreenCaptureIntent()
            activity!!.startActivityForResult(permissionIntent, SCREEN_RECORD_REQUEST_CODE)
            true
        } catch (e: Exception) {
            println("Error:" + e.message)
            false
        }
    }

    private fun customSettings(
        videoFrame: Int, videoBitrate: Int, fileOutputFormat: String?, addTimeCode: Boolean,
        fileName: String?
    ) {
        hbRecorder!!.isAudioEnabled(isAudioEnabled)
        hbRecorder!!.setAudioSource("DEFAULT")
        hbRecorder!!.setVideoEncoder("DEFAULT")
        hbRecorder!!.setVideoFrameRate(videoFrame)
        hbRecorder!!.setVideoBitrate(videoBitrate)
        hbRecorder!!.setOutputFormat(fileOutputFormat)
        if (dirPathToSave == null) {
            println(">>>>>>>>>>> 2$fileName")
            hbRecorder!!.fileName = generateFileName(fileName, addTimeCode)
        }
    }

    @Throws(IOException::class)
    private fun setOutputPath(addTimeCode: Boolean, fileName: String?, dirPathToSave: String?) {
        hbRecorder!!.fileName = generateFileName(fileName, addTimeCode)
        filePath = if (dirPathToSave != null && dirPathToSave !== "") {
            val dirFile = File(dirPathToSave)
            hbRecorder!!.setOutputPath(dirFile.absolutePath)
            dirFile.absolutePath + "/" + generateFileName(fileName, addTimeCode)
        } else {
            hbRecorder!!.setOutputPath(
                flutterPluginBinding!!.applicationContext.externalCacheDir!!.absolutePath
            )
            (flutterPluginBinding!!.applicationContext.externalCacheDir!!.absolutePath + "/"
                    + generateFileName(fileName, addTimeCode))
        }
    }

    private fun generateFileName(fileName: String?, addTimeCode: Boolean): String? {
        val formatter = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
        val curDate = Date(System.currentTimeMillis())
        return if (addTimeCode) {
            fileName + "-" + formatter.format(curDate).replace(" ", "")
        } else {
            fileName
        }
    }

    companion object {
        private const val SCREEN_RECORD_REQUEST_CODE = 777
    }
}