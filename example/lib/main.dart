import 'dart:async';
import 'dart:io';

import 'package:ed_screen_recorder/ed_screen_recorder.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:path_provider/path_provider.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      theme: ThemeData.dark(),
      debugShowCheckedModeBanner: false,
      home: const HomePage(),
    );
  }
}

class HomePage extends StatefulWidget {
  const HomePage({Key? key}) : super(key: key);

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  EdScreenRecorder? screenRecorder;
  RecordOutput? _response;
  bool inProgress = false;

  @override
  void initState() {
    super.initState();
    screenRecorder = EdScreenRecorder();
  }

  Future<void> startRecord({required String fileName, required int width, required int height}) async {
    Directory? tempDir = await getApplicationDocumentsDirectory();
    String? tempPath = tempDir.path;
    try {
      var startResponse = await screenRecorder?.startRecordScreen(
        fileName: "Eren",
        //Optional. It will save the video there when you give the file path with whatever you want.
        //If you leave it blank, the Android operating system will save it to the gallery.
        dirPathToSave: tempPath,
        audioEnable: true,
        width: width,
        height: height,
      );
      setState(() {
        _response = startResponse;
      });
    } on PlatformException {
      kDebugMode ? debugPrint("Error: An error occurred while starting the recording!") : null;
    }
  }

  Future<void> stopRecord() async {
    try {
      var stopResponse = await screenRecorder?.stopRecord();
      setState(() {
        _response = stopResponse;
      });
    } on PlatformException {
      kDebugMode ? debugPrint("Error: An error occurred while stopping recording.") : null;
    }
  }

  Future<void> pauseRecord() async {
    try {
      await screenRecorder?.pauseRecord();
    } on PlatformException {
      kDebugMode ? debugPrint("Error: An error occurred while pause recording.") : null;
    }
  }

  Future<void> resumeRecord() async {
    try {
      await screenRecorder?.resumeRecord();
    } on PlatformException {
      kDebugMode ? debugPrint("Error: An error occurred while resume recording.") : null;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Screen Recording Debug"),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text("File: ${_response?.file.path}"),
            Text("Status: ${_response?.success.toString()}"),
            Text("Event: ${_response?.eventName}"),
            Text("Progress: ${_response?.isProgress.toString()}"),
            Text("Message: ${_response?.message}"),
            Text("Video Hash: ${_response?.videoHash}"),
            Text("Start Date: ${(_response?.startDate).toString()}"),
            Text("End Date: ${(_response?.endDate).toString()}"),
            ElevatedButton(
              onPressed: () => startRecord(
                fileName: "eren",
                width: context.size?.width.toInt() ?? 0,
                height: context.size?.height.toInt() ?? 0,
              ),
              child: const Text('START RECORD'),
            ),
            ElevatedButton(onPressed: () => resumeRecord(), child: const Text('RESUME RECORD')),
            ElevatedButton(onPressed: () => pauseRecord(), child: const Text('PAUSE RECORD')),
            ElevatedButton(onPressed: () => stopRecord(), child: const Text('STOP RECORD')),
          ],
        ),
      ),
    );
  }
}
