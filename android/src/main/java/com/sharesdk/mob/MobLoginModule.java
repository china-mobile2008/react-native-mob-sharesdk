package com.sharesdk.mob;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.mob.MobSDK;

import java.util.HashMap;

import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.PlatformActionListener;
import cn.sharesdk.framework.PlatformDb;
import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.onekeyshare.OnekeyShare;
import cn.sharesdk.onekeyshare.OnekeyShareTheme;
import cn.sharesdk.tencent.qq.QQ;
import cn.sharesdk.wechat.friends.Wechat;

import static com.facebook.react.bridge.UiThreadUtil.runOnUiThread;

public class MobLoginModule extends ReactContextBaseJavaModule {
    private Context mContext;
    private Promise mPromise;
    private final int SUCCESS = 1;
    private final int ERROR = 2;
    private final int CANCEL = 3;

    public MobLoginModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.mContext = reactContext.getApplicationContext();
    }

    @Override
    public String getName() {
        return "MobLogin";
    }

    @Override
    public void initialize() {
        super.initialize();
        //初始化Mob
        MobSDK.init(mContext, "", "");
    }

    @ReactMethod
    public void loginWithQQ(Promise promise) {
        Platform qq = ShareSDK.getPlatform(QQ.NAME);
        if (qq.isAuthValid()) {
            qq.removeAccount(true);
        }
        qq.setPlatformActionListener(new PlatformActionListener() {
            @Override
            public void onComplete(Platform platform, int action, HashMap<String, Object> hashMap) {

                if (action == Platform.ACTION_USER_INFOR) {
                    PlatformDb platDB = platform.getDb();
                    WritableMap map = Arguments.createMap();
                    map.putString("token", platDB.getToken());
                    map.putString("user_id", platDB.getUserId());
                    map.putString("user_name", platDB.getUserName());
                    map.putString("user_gender", platDB.getUserGender());
                    map.putString("user_icon", platDB.getUserIcon());
                    mPromise.resolve(map);
                }
            }

            @Override
            public void onError(Platform platform, int i, Throwable throwable) {
                mPromise.reject("QQ LoginError", throwable.getMessage());
            }

            @Override
            public void onCancel(Platform platform, int i) {

            }
        });
        qq.showUser(null);
        mPromise = promise;
    }

    @ReactMethod
    public void loginWithWeChat(Promise promise) {
        Platform wechat = ShareSDK.getPlatform(Wechat.NAME);
        if (wechat.isAuthValid()) {
            wechat.removeAccount(true);
        }
        wechat.setPlatformActionListener(new PlatformActionListener() {
            @Override
            public void onComplete(Platform platform, int action, HashMap<String, Object> hashMap) {
                if (action == Platform.ACTION_USER_INFOR) {
                    PlatformDb platDB = platform.getDb();
                    WritableMap map = Arguments.createMap();
                    map.putString("token", platDB.getToken());
                    map.putString("user_id", platDB.getUserId());
                    map.putString("user_name", platDB.getUserName());
                    map.putString("user_gender", platDB.getUserGender());
                    map.putString("user_icon", platDB.getUserIcon());
                    mPromise.resolve(map);
                }
            }

            @Override
            public void onError(Platform platform, int i, Throwable throwable) {
                mPromise.reject("WeChat LoginError", throwable.getMessage());
            }

            @Override
            public void onCancel(Platform platform, int i) {

            }
        });
        wechat.showUser(null);
        mPromise = promise;
    }

    @ReactMethod
    public void shareWithText(String title, String text, String url, String imageUrl, final Callback successCallback) {
        OnekeyShare oks = new OnekeyShare();
        oks.setSilent(true);
        //ShareSDK快捷分享提供两个界面第一个是九宫格 CLASSIC  第二个是SKYBLUE
        oks.setTheme(OnekeyShareTheme.CLASSIC);
        // 令编辑页面显示为Dialog模式
        oks.setDialogMode(true);
        // 在自动授权时可以禁用SSO方式
        oks.disableSSOWhenAuthorize();
        // title标题，印象笔记、邮箱、信息、微信、人人网、QQ和QQ空间使用
        oks.setTitle(title);
        // titleUrl是标题的网络链接，仅在Linked-in,QQ和QQ空间使用
        oks.setTitleUrl(url);
        // text是分享文本，所有平台都需要这个字段
        oks.setText(text);
        //分享网络图片，新浪微博分享网络图片需要通过审核后申请高级写入接口，否则请注释掉测试新浪微博
        oks.setImageUrl(imageUrl);
        // url仅在微信（包括好友和朋友圈）中使用
        oks.setUrl(url);
        // 注册回调
        oks.setCallback(new PlatformActionListener() {
            @Override
            public void onComplete(Platform platform, int i, HashMap<String, Object> hashMap) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        successCallback.invoke(SUCCESS, "success");
                    }
                });
            }

            @Override
            public void onError(Platform platform, int i, final Throwable throwable) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        successCallback.invoke(ERROR, throwable.getMessage());
                    }
                });
            }

            @Override
            public void onCancel(Platform platform, int i) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        successCallback.invoke(CANCEL, "cancel");
                    }
                });
            }
        });
        // 启动分享GUI
        oks.show(mContext);
    }

    @ReactMethod
    public void shareWithImage(String title, String content, String url, String imgUrl, final Callback successCallback) {
        OnekeyShare oks = new OnekeyShare();
        //关闭sso授权
        oks.disableSSOWhenAuthorize();

        // 分享时Notification的图标和文字  2.5.9以后的版本不调用此方法
        //oks.setNotification(R.drawable.ic_launcher, getString(R.string.app_name));
        // title标题，印象笔记、邮箱、信息、微信、人人网和QQ空间使用
        oks.setTitle(title);
        // titleUrl是标题的网络链接，仅在人人网和QQ空间使用
        oks.setTitleUrl(url);
        // text是分享文本，所有平台都需要这个字段
        oks.setText(content);
        // imagePath是图片的本地路径，Linked-In以外的平台都支持此参数
        oks.setImagePath(FileUtils.getImageAbsolutePath(getCurrentActivity(), Uri.parse(imgUrl)));//确保SDcard下面存在此张图片
        // url仅在微信（包括好友和朋友圈）中使用
        //oks.setUrl("https://t.cn/RmXVDpb");
        // comment是我对这条分享的评论，仅在人人网和QQ空间使用
        //oks.setComment("我是测试评论文本");
        // site是分享此内容的网站名称，仅在QQ空间使用
        oks.setSite(getCurrentActivity().getResources().getString(R.string.app_name));
        oks.setDialogMode(true);
        // siteUrl是分享此内容的网站地址，仅在QQ空间使用
        oks.setSiteUrl("https://t.cn/RmXVDpb");
        // 注册回调
        oks.setCallback(new PlatformActionListener() {
            @Override
            public void onComplete(Platform platform, int i, HashMap<String, Object> hashMap) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        successCallback.invoke(SUCCESS, "success");
                    }
                });
            }

            @Override
            public void onError(Platform platform, int i, final Throwable throwable) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        successCallback.invoke(ERROR, throwable.getMessage());
                    }
                });
            }

            @Override
            public void onCancel(Platform platform, int i) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        successCallback.invoke(CANCEL, "cancel");
                    }
                });
            }
        });
        // 启动分享GUI
        oks.show(mContext);
    }
}
