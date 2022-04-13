#import "EdScreenRecorderPlugin.h"
#if __has_include(<ed_screen_recorder/ed_screen_recorder-Swift.h>)
#import <ed_screen_recorder/ed_screen_recorder-Swift.h>
#else
#import "ed_screen_recorder-Swift.h"
#endif

@implementation EdScreenRecorderPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftEdScreenRecorderPlugin registerWithRegistrar:registrar];
}
@end
