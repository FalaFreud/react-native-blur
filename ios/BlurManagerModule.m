#import "BlurManagerModule.h"

#import <AVFoundation/AVFoundation.h>
#import <UIKit/UIKit.h>
#import <React/RCTLog.h>
#import <React/UIView+React.h>
#import <React/RCTUtils.h>
#import <React/RCTConvert.h>
#import <React/RCTScrollView.h>
#import <React/RCTUIManager.h>

#if __has_include(<React/RCTUIManagerUtils.h>)
#import <React/RCTUIManagerUtils.h>
#endif
#import <React/RCTBridge.h>

@implementation BlurManagerModule

RCT_EXPORT_MODULE()

@synthesize bridge = _bridge;
UIVisualEffectView *effectView;
NSString * const SHOW_BLUR_WHEN_APPLICATION_INACTIVE = @"hideContentWhenApplicationInactive";

- (dispatch_queue_t)methodQueue {

    return RCTGetUIManagerQueue();
}

RCT_EXPORT_METHOD(captureScreen: (NSDictionary *)options
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {

    [self captureRef: [NSNumber numberWithInt:-1] withOptions:options resolve:resolve reject:reject];
}

RCT_EXPORT_METHOD(releaseCapture:(nonnull NSString *)uri) {

    NSString *directory = [NSTemporaryDirectory() stringByAppendingPathComponent:@"ReactNative"];
    // Ensure it's a valid file in the tmp directory
    if ([uri hasPrefix:directory] && ![uri isEqualToString:directory]) {
        NSFileManager *fileManager = [NSFileManager new];
        if ([fileManager fileExistsAtPath:uri]) {
            [fileManager removeItemAtPath:uri error:NULL];
        }
    }
}

RCT_EXPORT_METHOD(captureRef:(nonnull NSNumber *)target
                  withOptions:(NSDictionary *)options
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {

    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {

        UIView *view;

        if ([target intValue] == -1) {
            UIWindow *window = [[UIApplication sharedApplication] keyWindow];
            window.backgroundColor = [UIColor whiteColor];
            view = window.rootViewController.view;
        } else {
            view = viewRegistry[target];
        }
        if (!view) {
            reject(RCTErrorUnspecified, [NSString stringWithFormat:@"No view found with reactTag: %@", target], nil);
            return;
        }

        if (!UIAccessibilityIsReduceTransparencyEnabled()) {

            view.backgroundColor = [UIColor whiteColor];
            UIBlurEffect *blurEffect = [UIBlurEffect effectWithStyle:UIBlurEffectStyleLight];
            effectView = [[UIVisualEffectView alloc] initWithEffect:blurEffect];
            effectView.frame = view.bounds;
            effectView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;

            [view addSubview:effectView]; //if you have more UIViews, use an insertSubview API to place it where needed

            resolve(@(1));
        } else {
            view.backgroundColor = [UIColor blackColor];
            resolve(@(0));
        }
    }];
}

RCT_EXPORT_METHOD(removeBlurView:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject) {

    dispatch_async(dispatch_get_main_queue(), ^{
        UIWindow *window = [[UIApplication sharedApplication] keyWindow];
        UIVisualEffectView *effectView = [window.rootViewController.view viewWithTag:0001];
        if (effectView) {
            if ([self isToHideContentWhenApplicationInactive]) {
                [window.rootViewController.view bringSubviewToFront: effectView];
            }
            [UIView animateWithDuration:0.2 animations:^{
                effectView.alpha = 0;
                } completion:^(BOOL finished) {
                    [window.rootViewController.view sendSubviewToBack: effectView];
            }];
        }
        resolve(@(1));
    });
}

RCT_EXPORT_METHOD(hideContentWhenApplicationInactive:(BOOL)enable) {

    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [defaults setBool:enable forKey: SHOW_BLUR_WHEN_APPLICATION_INACTIVE];
    [defaults synchronize];
}

- (BOOL) isToHideContentWhenApplicationInactive {

    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    BOOL enable = [defaults boolForKey: SHOW_BLUR_WHEN_APPLICATION_INACTIVE];
    return enable;
}

@end
