import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:uuid/uuid.dart';
import 'package:watcher/watcher.dart';

import '../models/record_output_model.dart';

class EdScreenRecorder {
  static const MethodChannel _channel = MethodChannel('ed_screen_recorder');

  /// [FileWatcher] It monitors changes to contents of directories and sends notifications when files have been added, removed, or modified.
  FileWatcher? watcher;

  /// [startRecordScreen] function takes the necessary parameters. The user can change all of these according to himself.
  /// Thanks to the [uuid] and [videoHash] variables, we can detect that each recorded video is unique from each other.
  /// After the process, we get a model result called [RecordOutput].
  /// On the front end we can see this result as [Map] .
  Future<Map<String, dynamic>> startRecordScreen(
      {required String fileName,
      String? dirPathToSave,
      bool? addTimeCode = true,
      String? fileOutputFormat = "MPEG_4",
      String? fileExtension = "mp4",
      int? videoBitrate = 3000000,
      int? videoFrame = 30,
      required bool audioEnable}) async {
    var uuid = const Uuid();
    String videoHash = uuid.v1().replaceAll('-', '');
    var dateNow = DateTime.now().microsecondsSinceEpoch;
    var response = await _channel.invokeMethod('startRecordScreen', {
      "audioenable": audioEnable,
      "filename": fileName,
      "dirpathtosave": dirPathToSave,
      "addtimecode": addTimeCode,
      "videoframe": videoFrame,
      "videobitrate": videoBitrate,
      "fileoutputformat": fileOutputFormat,
      "fileextension": fileExtension,
      "videohash": videoHash,
      "startdate": dateNow,
    });
    var formatResponse = RecordOutput.fromJson(json.decode(response));
    if (kDebugMode) {
      debugPrint("""
      >>> Start Record Response Output:  
      File: ${formatResponse.file} 
      Event Name: ${formatResponse.eventName}  
      Progressing: ${formatResponse.isProgress} 
      Message: ${formatResponse.message} 
      Success: ${formatResponse.success} 
      Video Hash: ${formatResponse.videoHash} 
      Start Date: ${formatResponse.startDate} 
      End Date: ${formatResponse.endDate}
      """);
    }
    fileWatcherStart(formatResponse.file);
    return formatResponse.toJson();
  }

  Future<void> fileWatcherStart(File file) async {
    watcher = FileWatcher(file.path);
  }

  Future<Map<String, dynamic>> stopRecord() async {
    var dateNow = DateTime.now().microsecondsSinceEpoch;
    var response = await _channel.invokeMethod('stopRecordScreen', {
      "enddate": dateNow,
    });

    var formatResponse = RecordOutput.fromJson(json.decode(response));
    if (kDebugMode) {
      debugPrint("""
      >>> Stop Record Response Output:  
      File: ${formatResponse.file} 
      Event Name: ${formatResponse.eventName}  
      Progressing: ${formatResponse.isProgress} 
      Message: ${formatResponse.message} 
      Success: ${formatResponse.success} 
      Video Hash: ${formatResponse.videoHash} 
      Start Date: ${formatResponse.startDate} 
      End Date: ${formatResponse.endDate}
      """);
    }
    return formatResponse.toJson();
  }

  Future<void> pauseRecord() async {
    await _channel.invokeMethod('pauseRecordScreen');
  }

  Future<void> resumeRecord() async {
    await _channel.invokeMethod('resumeRecordScreen');
  }
}
