package com.phantom.onetapfacebookmodule;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import java.io.PrintWriter;
import java.io.StringWriter;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class FacebookLiteHook implements IXposedHookLoadPackage {
    private static final String FACEBOOK_PACKAGE_NAME = "com.facebook.lite";
    private static final String CLASS_NAME = "com.facebook.lite.q.l";
    private static final String CLASS_NAME_1 = "com.a.a.a.e.b";

    private static final String ACTION_SAVE_URI = "com.phantom.onetapvideodownload.action.saveurl";
    private static final String ONE_TAP_PACKAGE_NAME = "com.phantom.onetapvideodownload";
    private static final String IPC_SERVICE_CLASS_NAME = ONE_TAP_PACKAGE_NAME + ".IpcService";
    public static final String EXTRA_URL = ONE_TAP_PACKAGE_NAME + ".extra.url";
    public static final String EXTRA_PACKAGE_NAME = ONE_TAP_PACKAGE_NAME + ".extra.package_name";

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
            protected void afterHookedMethod(MethodHookParam hookParams) throws Throwable {
                try {
                    String url = hookParams.args[1].toString();
                    XposedBridge.log(url);
                    Intent intent = new Intent(ACTION_SAVE_URI);
                    intent.setClassName(ONE_TAP_PACKAGE_NAME, IPC_SERVICE_CLASS_NAME);
                    intent.putExtra(EXTRA_URL, url);
                    intent.putExtra(EXTRA_PACKAGE_NAME, getContext().getPackageName());
                    context.startService(intent);
                } catch (Exception e) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    XposedBridge.log("One Tap Video Download Exception" + sw.toString());
                }
            }
        };

        int packageVersion = context.getPackageManager().getPackageInfo(FACEBOOK_PACKAGE_NAME
                , PackageManager.GET_META_DATA).versionCode;

        if (!findClass(lpparam.classLoader, CLASS_NAME)
                || !findClass(lpparam.classLoader, CLASS_NAME)) {
            XposedBridge.log("Class not found. Package version : " + packageVersion);
            return;
        }

        Class mainClass = XposedHelpers.findClass(CLASS_NAME, lpparam.classLoader);
        Class subClass = XposedHelpers.findClass(CLASS_NAME_1, lpparam.classLoader);

        Object [] objects = new Object[] {
                String.class,
                String.class,
                int.class,
                int.class,
                int.class,
                subClass,
                long.class,
                boolean.class,
                long.class,
                String.class,
                methodHook
        };

        XposedHelpers.findAndHookConstructor(mainClass,  objects);
        XposedBridge.log("One Tap Facebook Download : lite hooking successful for version "
                + packageVersion);
    }

}
