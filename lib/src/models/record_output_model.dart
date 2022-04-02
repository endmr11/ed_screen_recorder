import 'dart:convert';
import 'dart:io';

RecordOutput recordOutputFromJson(String str) =>
    RecordOutput.fromJson(json.decode(str));

String recordOutputToJson(RecordOutput data) => json.encode(data.toJson());

class RecordOutput {
  RecordOutput({
    required this.success,
    required this.file,
    required this.isProgress,
    required this.eventName,
    required this.message,
    required this.videoHash,
    required this.startDate,
    required this.endDate,
  });

  bool success;
  File file;
  bool isProgress;
  String eventName;
  String? message;
  String videoHash;
  int startDate;
  int? endDate;

  factory RecordOutput.fromJson(Map<String, dynamic> json) {
    return RecordOutput(
      success: json["success"],
      file: File(json["file"]),
      isProgress: json["isProgress"],
      eventName: json["eventname"],
      message: json["message"],
      videoHash: json["videohash"],
      startDate: json['startdate'],
      endDate: json['enddate'],
    );
  }

  Map<String, dynamic> toJson() => {
        "success": success,
        "file": file,
        "progressing": isProgress,
        "eventname": eventName,
        "message": message,
        "videohash": videoHash,
        "startdate": startDate,
        "enddate": endDate,
      };
}
