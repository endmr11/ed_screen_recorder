import Flutter
import UIKit
import ReplayKit
import Photos


  struct JsonObj : Codable {
    var success: Bool!
    var file: String
    var isProgress: Bool!
    var eventname: String!
    var message: String?
    var videohash: String!
    var startdate: Int?
    var enddate: Int?
  }

public class SwiftEdScreenRecorderPlugin: NSObject, FlutterPlugin {

  let recorder = RPScreenRecorder.shared()

  var videoOutputURL : URL?
  var videoWriter : AVAssetWriter?

  var audioInput:AVAssetWriterInput!
  var videoWriterInput : AVAssetWriterInput?

  var fileName: String = ""
  var dirPathToSave:NSString = ""
  var isAudioEnabled: Bool! = false;
  var addTimeCode: Bool! = false;
  var filePath: NSString = "";
  var videoFrame: Int?;
  var videoBitrate: Int?;
  var fileOutputFormat: String? = "";
  var fileExtension: String? = "";
  var success: Bool! = false;
  var videoHash: String! = "";
  var startDate: Int?;
  var endDate: Int?;
  var isProgress: Bool! = false;
  var eventName: String! = "";
  var message: String? = "";

    var startRecordingResult: FlutterResult?
    var stopRecordingResult: FlutterResult?
  var myResult: FlutterResult?

  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "ed_screen_recorder", binaryMessenger: registrar.messenger())
    let instance = SwiftEdScreenRecorderPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
      initAllResults()
    if(call.method == "startRecordScreen"){
        startRecordingResult = result
        let args = call.arguments as? Dictionary<String, Any>
        self.isAudioEnabled=((args?["audioenable"] as? Bool?)! ?? false)!
        self.fileName=(args?["filename"] as? String)!+".mp4"
        self.dirPathToSave = ((args?["dirpathtosave"] as? NSString) ?? "")
        self.addTimeCode=((args?["addtimecoe"] as? Bool?)! ?? false)!
        self.videoFrame=(args?["videoframe"] as? Int)!
        self.videoBitrate=(args?["videobitrate"] as? Int)!

        self.fileOutputFormat=(args?["fileoutputformat"] as? String)!
        self.fileExtension=(args?["fileextension"] as? String)!
        self.videoHash=(args?["videohash"] as? String)!

        self.isProgress=Bool(true)
        self.eventName=String("startRecordScreen")
        
        recorder.isMicrophoneEnabled = self.isAudioEnabled
        
        recorder.startRecording(handler: startRecordingHandler)
    }else if(call.method == "stopRecordScreen"){
        stopRecordingResult = result
        if #available(iOS 14.0, *) {
            let saveUrl = URL(fileURLWithPath:self.dirPathToSave.appendingPathComponent(self.fileName))
            recorder.stopRecording(withOutput: saveUrl, completionHandler: stopRecordingHandler)
        } else {
            stopRecordingResult!(FlutterError(code: "Not Supported", message: "Current iOS Version is lower than 14", details: nil))
        }
    } else if (call.method == "pauseRecordingScreen") {
        result(true)
    }
      else if (call.method == "resumeRecordingScreen") {
        result(true)
      }
  }
    
    func startRecordingHandler(err:Error?) -> Void {
        if err == nil {
            let jsonObject: JsonObj = JsonObj(
          success: Bool(self.success),
          file: String("\(self.filePath)/\(self.fileName)"),
          isProgress: Bool(self.isProgress),
          eventname: String(self.eventName ?? "eventName"),
          message: String(self.message!),
          videohash: String(self.videoHash),
          startdate: Int(self.startDate ?? Int(NSDate().timeIntervalSince1970 * 1_000)),
          enddate: Int(self.endDate ?? 0)
        )
                    let encoder = JSONEncoder()
                    let json = try! encoder.encode(jsonObject)
                    let jsonStr = String(data:json,encoding: .utf8)
                    startRecordingResult!(jsonStr)
        } else {
            startRecordingResult!(FlutterError(code: err!.localizedDescription, message: err?.localizedDescription, details: err?.localizedDescription))
        }
    }
    
    func stopRecordingHandler(err:Error?) -> Void {
        if err != nil {
            stopRecordingResult!(FlutterError(code: err!.localizedDescription, message: err?.localizedDescription, details: err?.localizedDescription))
        } else {
            let saveUrl = URL(fileURLWithPath:self.dirPathToSave.appendingPathComponent(self.fileName));
            let jsonObject: JsonObj = JsonObj(
                success: Bool(self.success),
                file: String(saveUrl.absoluteURL.path),
                isProgress: Bool(self.isProgress),
                eventname: String(self.eventName ?? "eventName"),
                message: String(self.message!),
                videohash: String(self.videoHash),
                startdate: Int(self.startDate ?? Int(NSDate().timeIntervalSince1970 * 1_000)),
                enddate: Int(self.endDate ?? 0)
              )
                    let encoder = JSONEncoder()
                    let json = try! encoder.encode(jsonObject)
                    let jsonStr = String(data:json,encoding: .utf8)
                    stopRecordingResult!(jsonStr)
        }
        
    }
    
    func initAllResults(){
        startRecordingResult = nil
        myResult = nil
    }
    
    
}
