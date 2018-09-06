//
//  MobLogin.m
//  rnsharesdk
//
//  Created by cc on 2017/2/1.
//  Copyright © 2017年 qq. All rights reserved.
//
#import "MobLogin.h"

@implementation MobLogin

RCT_EXPORT_MODULE();

- (instancetype)init
{
    if(self = [super init]){
        NSLog(@"initShareSdk()!");
        /**
         *  设置ShareSDK的appKey，如果尚未在ShareSDK官网注册过App，请移步到http://mob.com/login 登录后台进行应用注册，
         *  在将生成的AppKey传入到此方法中。我们Demo提供的appKey为内部测试使用，可能会修改配置信息，请不要使用。
         *  方法中的第二个参数用于指定要使用哪些社交平台，以数组形式传入。第三个参数为需要连接社交平台SDK时触发，
         *  在此事件中写入连接代码。第四个参数则为配置本地社交平台时触发，根据返回的平台类型来配置平台信息。
         *  如果您使用的时服务端托管平台信息时，第二、四项参数可以传入nil，第三项参数则根据服务端托管平台来决定要连接的社交SDK。
         */
        [ShareSDK registerActivePlatforms:@[//这样可以隐藏到微信分享
                                            // 不要使用微信总平台进行初始化
                                            //@(SSDKPlatformTypeWechat),
                                            // 使用微信子平台进行初始化，即可
                                            @(SSDKPlatformSubTypeWechatSession),
                                            @(SSDKPlatformSubTypeWechatTimeline),
                                            ]
                                 onImport:^(SSDKPlatformType platformType) {
                                     switch (platformType)
                                     {
                                         case SSDKPlatformTypeWechat:
                                             [ShareSDKConnector connectWeChat:[WXApi class] delegate:self];
                                             break;
                                         default:
                                             break;
                                     }
                                 }
                          onConfiguration:^(SSDKPlatformType platformType, NSMutableDictionary *appInfo) {
                              switch (platformType)
                              {
                                  case SSDKPlatformTypeWechat:
                                      [appInfo SSDKSetupWeChatByAppId:@""
                                                            appSecret:@""];
                                      break;
                                  default:
                                      break;
                              }
                          }];
    }
    return self;
}

+ (BOOL)requiresMainQueueSetup
{
    return YES;
}

RCT_EXPORT_METHOD(loginWithQQ:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    dispatch_async(dispatch_get_main_queue(), ^{
        if([ShareSDK hasAuthorized:(SSDKPlatformTypeQQ)]){
            [ShareSDK cancelAuthorize:(SSDKPlatformTypeQQ)];
        }else{
            [ShareSDK getUserInfo:SSDKPlatformTypeQQ
                   onStateChanged:^(SSDKResponseState state, SSDKUser *user, NSError *error)
             {
                 if (state == SSDKResponseStateSuccess){
                     NSString * const genderStatusName[]={
                         [SSDKGenderMale] = @"m",
                         [SSDKGenderFemale] = @"f",
                         [SSDKGenderUnknown] = @"u",
                     };
                     NSMutableDictionary *result = [NSMutableDictionary dictionary];
                     [result setObject:user.credential.token forKey:@"token"];
                     [result setObject:user.uid forKey:@"user_id"];
                     [result setObject:user.nickname forKey:@"user_name"];
                     [result setObject:genderStatusName[user.gender] forKey:@"user_gender"];
                     [result setObject:user.icon forKey:@"icon"];
                     resolve(result);
                 }else{
                     reject(@"loginWithQQ: ", error, nil);
                 }
             }];
        }
    });
}

RCT_EXPORT_METHOD(loginWithWeChat:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    dispatch_async(dispatch_get_main_queue(), ^{
        if([ShareSDK hasAuthorized:(SSDKPlatformTypeWechat)]){
            [ShareSDK cancelAuthorize:(SSDKPlatformTypeWechat)];
        }else{
            [ShareSDK getUserInfo:SSDKPlatformTypeWechat
                   onStateChanged:^(SSDKResponseState state, SSDKUser *user, NSError *error)
             {
                 if (state == SSDKResponseStateSuccess){
                     NSString * const genderStatusName[]={
                         [SSDKGenderMale] = @"m",
                         [SSDKGenderFemale] = @"f",
                         [SSDKGenderUnknown] = @"u",
                     };
                     NSMutableDictionary *result = [NSMutableDictionary dictionary];
                     [result setObject:user.credential.token forKey:@"token"];
                     [result setObject:user.uid forKey:@"user_id"];
                     [result setObject:user.nickname forKey:@"user_name"];
                     [result setObject:genderStatusName[user.gender] forKey:@"user_gender"];
                     [result setObject:user.icon forKey:@"icon"];
                     resolve(result);
                 }else{
                     reject(@"loginWithWeChat: ", error, nil);
                 }
             }];
        }
    });
}

RCT_EXPORT_METHOD(shareWithText:(NSString *)title :(NSString *)content :(NSString *)url :(NSString *)imgUrl completion:(RCTResponseSenderBlock)completion) {
    dispatch_async(dispatch_get_main_queue(), ^{
        //1、创建分享参数（必要）
        NSMutableDictionary *shareParams = [NSMutableDictionary dictionary];
        NSArray* imageArray = @[imgUrl];
        [shareParams SSDKSetupShareParamsByText:content
                                         images:imageArray
                                            url:[NSURL URLWithString:url]
                                          title:title
                                           type:SSDKContentTypeAuto];
        SSUIShareSheetConfiguration *config = [[SSUIShareSheetConfiguration alloc] init];
        config.columnPortraitCount = 2;
        config.itemAlignment = SSUIItemAlignmentCenter;
        
        //2、分享（可以弹出我们的分享菜单和编辑界面）
        [ShareSDK showShareActionSheet:nil customItems:nil shareParams:shareParams sheetConfiguration:config onStateChanged:^(SSDKResponseState state, SSDKPlatformType platformType, NSDictionary *userData, SSDKContentEntity *contentEntity, NSError *error, BOOL end) {

                       switch (state) {
                           case SSDKResponseStateSuccess:
                           {
                               completion(@[@(state), @"share success"]);
                               break;
                           }
                           case SSDKResponseStateFail:
                           {
                               completion(@[@(state), @"share fail"]);
                               break;
                           }
                           default:
                               completion(@[@(state), @"share cancel"]);
                               break;
                       }
                   }
         ];
    });
}

RCT_EXPORT_METHOD(shareWithImage:(NSString *)title :(NSString *)content :(NSString *)url :(NSString *)imgUrl completion:(RCTResponseSenderBlock)completion) {
    dispatch_async(dispatch_get_main_queue(), ^{
        //1、创建分享参数
        NSArray * imageArray = @[imgUrl];
        //（注意：图片必须要在Xcode左边目录里面，名称必须要传正确，如果要分享网络图片，可以这样传iamge参数 images:@[@"http://mob.com/Assets/images/logo.png?v=20150320"]）
        if (imageArray) {
            
            NSMutableDictionary *shareParams = [NSMutableDictionary dictionary];
            [shareParams SSDKSetupShareParamsByText:content
                                             images:imageArray
                                                url:nil//[NSURL URLWithString:url]
                                              title:title
                                               type:SSDKContentTypeAuto];
            
            SSUIShareSheetConfiguration *config = [[SSUIShareSheetConfiguration alloc] init];
            config.columnPortraitCount = 2;
            config.itemAlignment = SSUIItemAlignmentCenter;
            
            [ShareSDK showShareActionSheet:nil customItems:nil shareParams:shareParams sheetConfiguration:config onStateChanged:^(SSDKResponseState state, SSDKPlatformType platformType, NSDictionary *userData, SSDKContentEntity *contentEntity, NSError *error, BOOL end) {
                switch (state) {
                    case SSDKResponseStateSuccess:
                    {
                        completion(@[@(state), @"share success"]);
                        break;
                    }
                    case SSDKResponseStateFail:
                    {
                        completion(@[@(state), @"share fail"]);
                        break;
                    }
                    default:
                        completion(@[@(state), @"share cancel"]);
                        break;
                }
            }
             ];
        }
    });
}
@end
