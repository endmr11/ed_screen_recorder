import Flutter
import UIKit
import ReplayKit
import Photos

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


  var myResult: FlutterResult?

  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "ed_screen_recorder", binaryMessenger: registrar.messenger())
    let instance = SwiftEdScreenRecorderPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    if(call.method == "startRecordScreen"){
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
        var width = args?["width"]; // in pixels
        if(width == nil || width is NSNull) {
            width = Int32(UIScreen.main.nativeBounds.width); // pixels
        } else {
            width = Int32(width as! Int32);
        }
        var height = args?["height"] // in pixels
        if(height == nil || height is NSNull) {
            height = Int32(UIScreen.main.nativeBounds.height); // pixels
        } else {
            height = Int32(height as! Int32);
        }
        self.success=Bool(startRecording(width: width as! Int32 ,height: height as! Int32,dirPathToSave:(self.dirPathToSave as NSString) as String));
        self.startDate=Int(NSDate().timeIntervalSince1970 * 1_000)
        myResult = result

    }else if(call.method == "stopRecordScreen"){
        
        if(videoWriter != nil){
            self.success=Bool(stopRecording())
            self.filePath=NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true)[0] as NSString
            self.isProgress=Bool(false)
            self.eventName=String("stopRecordScreen")
            self.endDate=Int(NSDate().timeIntervalSince1970 * 1_000)
        }else{
            self.success=Bool(false)
        }
        myResult = result
    }
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
      result(jsonStr)
  }

  func randomString(length: Int) -> String {
    let letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    return String((0..<length).map{ _ in letters.randomElement()! })
  }

    @objc func startRecording(width: Int32, height: Int32,dirPathToSave:String) -> Bool {
     var res : Bool = true
    if(recorder.isAvailable){
        NSLog("startRecording: w x h = \(width) x \(height) pixels");
        if dirPathToSave != nil && dirPathToSave != "" {
            var filePath:NSString = dirPathToSave as NSString
            self.videoOutputURL = URL(fileURLWithPath: String(self.filePath.appendingPathComponent(fileName)))
        } else {
            self.filePath = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true)[0] as NSString
            self.videoOutputURL = URL(fileURLWithPath: String(self.filePath.appendingPathComponent(fileName)))
        }
        do {
            try FileManager.default.removeItem(at: videoOutputURL!)
        } catch let error as NSError{
            res = Bool(false);
        }

        do {
            try videoWriter = AVAssetWriter(outputURL: videoOutputURL!, fileType: AVFileType.mp4)
            self.message=String("Started Video")
        } catch let writerError as NSError {
            print("Error opening video file", writerError);
            self.message=String(writerError as! Substring) as String
            videoWriter = nil;
            return  Bool(false)
        }
        if #available(iOS 11.0, *) {
            recorder.isMicrophoneEnabled = isAudioEnabled
            let videoSettings: [String : Any] = [
                AVVideoCodecKey  : AVVideoCodecType.h264,
                AVVideoWidthKey  : NSNumber.init(value: width),
                AVVideoHeightKey : NSNumber.init(value: height),
                AVVideoCompressionPropertiesKey: [
                AVVideoProfileLevelKey: AVVideoProfileLevelH264HighAutoLevel,
                AVVideoAverageBitRateKey: self.videoBitrate!
               ],
            ]
            self.videoWriterInput = AVAssetWriterInput(mediaType: AVMediaType.video, outputSettings: videoSettings);
            self.videoWriterInput?.expectsMediaDataInRealTime = true;
            self.videoWriter?.add(videoWriterInput!);
            if(isAudioEnabled){
                let audioOutputSettings: [String : Any] = [
                    AVNumberOfChannelsKey : 2,
                    AVFormatIDKey : kAudioFormatMPEG4AAC,
                    AVSampleRateKey: 44100,
                    AVEncoderAudioQualityKey: AVAudioQuality.high.rawValue
                ]
                self.audioInput = AVAssetWriterInput(mediaType: AVMediaType.audio, outputSettings: audioOutputSettings)
                self.audioInput?.expectsMediaDataInRealTime = true;
                self.videoWriter?.add(audioInput!);
            }
        }
            if #available(iOS 11.0, *) {
                recorder.startCapture(handler: { 
                    (cmSampleBuffer, rpSampleType, error) in guard error == nil else {
                            return;
                    }
                    switch rpSampleType {
                        case RPSampleBufferType.video:
                            if self.videoWriter?.status == AVAssetWriter.Status.unknown {
                                self.videoWriter?.startWriting()
                                self.videoWriter?.startSession(atSourceTime:  CMSampleBufferGetPresentationTimeStamp(cmSampleBuffer));
                            }else if self.videoWriter?.status == AVAssetWriter.Status.writing {
                                if (self.videoWriterInput?.isReadyForMoreMediaData == true) {
                                    if  self.videoWriterInput?.append(cmSampleBuffer) == false {
                                        print("Problems writing video")
                                        res = Bool(false)
                                        self.message="Error starting capture";
                                    }
                                }
                            }
                            case RPSampleBufferType.audioMic:
                                if(self.isAudioEnabled){
                                    if self.audioInput?.isReadyForMoreMediaData == true {
                                            print("starting audio....");
                                        if self.audioInput?.append(cmSampleBuffer) == false {
                                            print("Problems writing audio")
                                        }
                                    }
                                }
                            default:
                            break;
                    }
                }){(error) in guard error == nil else {
                        print("Screen record not allowed");
                        return
                    }
                }
            }
        }
        return  Bool(res)
    }

    @objc func stopRecording() -> Bool {
        var res : Bool = true;
        if(recorder.isRecording){
            if #available(iOS 11.0, *) {
                recorder.stopCapture( handler: { (error) in
                    print("Stopping recording...");
                    if(error != nil){
                        res = Bool(false)
                        self.message = "Has Got Error in stop record"
                    }
                })
            } else {
                res = Bool(false)
                self.message="You dont Support this plugin"
            }

            self.videoWriterInput?.markAsFinished();
            if(self.isAudioEnabled) {
                self.audioInput?.markAsFinished();
            }

            self.videoWriter?.finishWriting {
                print("Finished writing video");
                PHPhotoLibrary.shared().performChanges({
                    PHAssetChangeRequest.creationRequestForAssetFromVideo(atFileURL: self.videoOutputURL!)
                })
                self.message="stopRecordScreenFromApp"
            }
        }else{
            self.message="You haven't start the recording unit now!"
        }
        return Bool(res);

}
}
