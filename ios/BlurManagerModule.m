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

- (dispatch_queue_t)methodQueue
{
    return RCTGetUIManagerQueue();
}

RCT_EXPORT_METHOD(captureScreen: (NSDictionary *)options
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
    [self captureRef: [NSNumber numberWithInt:-1] withOptions:options resolve:resolve reject:reject];
}

RCT_EXPORT_METHOD(releaseCapture:(nonnull NSString *)uri)
{
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
                  reject:(RCTPromiseRejectBlock)reject)
{
    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        // Get view
        UIView *view;

        if ([target intValue] == -1) {
            UIWindow *window = [[UIApplication sharedApplication] keyWindow];
            view = window.rootViewController.view;
        } else {
            view = viewRegistry[target];
        }

        if (!view) {
            reject(RCTErrorUnspecified, [NSString stringWithFormat:@"No view found with reactTag: %@", target], nil);
            return;
        }
        if (!UIAccessibilityIsReduceTransparencyEnabled()) {
            view.backgroundColor = [UIColor clearColor];

            UIBlurEffect *blurEffect = [UIBlurEffect effectWithStyle:UIBlurEffectStyleLight];
//            UIVibrancyEffect * effect = [UIVibrancyEffect effectForBlurEffect: blurEffect];

//            UIVisualEffectView * effectView = [[UIVisualEffectView alloc] initWithEffect: effect];
            effectView = [[UIVisualEffectView alloc] initWithEffect:blurEffect];

            //always fill the view
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

RCT_EXPORT_METHOD(removeBlurView:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
    dispatch_async(dispatch_get_main_queue(), ^{
        if (effectView != nil) {
            [effectView removeFromSuperview];
        }
        resolve(@(1));
    });
}

@end
