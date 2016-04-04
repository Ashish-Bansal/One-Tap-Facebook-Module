package com.phantom.onetapfacebookmodule;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class FacebookHook implements IXposedHookLoadPackage {
    private static final String FACEBOOK_PACKAGE_NAME = "com.facebook.katana";
    private static final String VIDEO_PLAY_REQUEST = "com.facebook.exoplayer.ipc.VideoPlayRequest";
    private static final String VIDEO_URI_SOURCE_TYPE = "com.facebook.exoplayer.ipc.VideoPlayRequest$VideoUriSourceType";

    private static final String ACTION_SAVE_URI = "com.phantom.onetapvideodownload.action.saveurl";
    private static final String ONE_TAP_PACKAGE_NAME = "com.phantom.onetapvideodownload";
    private static final String IPC_SERVICE_CLASS_NAME = ONE_TAP_PACKAGE_NAME + ".IpcService";
    public static final String EXTRA_URL = ONE_TAP_PACKAGE_NAME + ".extra.url";
    public static final String EXTRA_PACKAGE_NAME = ONE_TAP_PACKAGE_NAME + ".extra.package_name";
    public static final String EXTRA_METADATA = ONE_TAP_PACKAGE_NAME + ".extra.metadata";

    public Context getContext() {
        Class activityThreadClass = XposedHelpers.findClass("android.app.ActivityThread", null);
        Object activityThread = XposedHelpers.callStaticMethod(activityThreadClass, "currentActivityThread");
        return (Context) XposedHelpers.callMethod(activityThread, "getSystemContext");
    }

    public boolean findClass(ClassLoader loader, String className) {
        try {
            loader.loadClass(className);
            return true;
        } catch( ClassNotFoundException e ) {
            return false;
        }
    }

    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(FACEBOOK_PACKAGE_NAME)) {
            return;
        }

        final Context context = getContext();
        final XC_MethodHook methodHook = new XC_MethodHook() {
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam hookParams) throws Throwable {
                try {
                    Uri uri = (Uri) hookParams.args[0];
                    String stringUri = "";
                    Set<String> keys = uri.getQueryParameterNames();
                    for (String key : keys) {
                        String value = uri.getQueryParameter(key);
                        if (Uri.parse(value).getScheme() != null) {
                            stringUri = value;
                        }
                    }

                    Intent intent = new Intent(ACTION_SAVE_URI);
                    intent.setClassName(ONE_TAP_PACKAGE_NAME, IPC_SERVICE_CLASS_NAME);
                    intent.putExtra(EXTRA_URL, stringUri);
                    intent.putExtra(EXTRA_PACKAGE_NAME, getContext().getPackageName());

                    Calendar cal = Calendar.getInstance();
                    intent.putExtra(EXTRA_METADATA, DateFormat.getDateTimeInstance().format(cal.getTime()));

                    context.startService(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        int packageVersion = context.getPackageManager().getPackageInfo(FACEBOOK_PACKAGE_NAME
                , PackageManager.GET_META_DATA).versionCode;

        if (!findClass(lpparam.classLoader, VIDEO_PLAY_REQUEST)
                || !findClass(lpparam.classLoader, VIDEO_PLAY_REQUEST)) {
            XposedBridge.log("Class not found. Package version : " + packageVersion);
            return;
        }

        Class mainClass = XposedHelpers.findClass(VIDEO_PLAY_REQUEST, lpparam.classLoader);
        Class subClass = XposedHelpers.findClass(VIDEO_URI_SOURCE_TYPE, lpparam.classLoader);

        Object [] objects = new Object[] {
                Uri.class,
                String.class,
                String.class,
                Uri.class,
                String.class,
                ParcelFileDescriptor.class,
                subClass,
                methodHook
        };

        XposedHelpers.findAndHookConstructor(mainClass,  objects);
        XposedBridge.log("One Tap Facebook Download : hooking successful for version "
                + packageVersion);
    }

}
