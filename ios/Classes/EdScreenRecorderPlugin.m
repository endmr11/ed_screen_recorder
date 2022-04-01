#import "EdScreenRecorderPlugin.h"
#if __has_include(<ed_screen_recorder/ed_screen_recorder-Swift.h>)
#import <ed_screen_recorder/ed_screen_recorder-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "ed_screen_recorder-Swift.h"
#endif

@implementation EdScreenRecorderPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftEdScreenRecorderPlugin registerWithRegistrar:registrar];
}
@end
