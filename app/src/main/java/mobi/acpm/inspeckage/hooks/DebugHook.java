package mobi.acpm.inspeckage.hooks;

import android.app.Activity;
import android.content.Intent;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by tim on 2017/10/16.
 */

public class DebugHook extends XC_MethodHook {
    public static void initAllHooks(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XC_MethodHook myHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Activity app = (Activity) param.thisObject;
                Intent intent = app.getIntent();
                intent.putExtra("com.android.settings.extra.DEBUGGABLE", false);
                app.setIntent(intent);
            }
        };
        if (loadPackageParam.packageName.equals("com.android.settings")) {
            String className = "";
            try {
                XposedHelpers.findClass("com.android.settings.AppPicker", loadPackageParam.classLoader);
                className = "com.android.settings.AppPicker";
                XposedBridge.log("XDebuggable: Class com.android.settings.AppPicker FOUND!");
            } catch (XposedHelpers.ClassNotFoundError e) {
                XposedBridge.log("XDebuggable: Class com.android.settings.AppPicker not found.");
            }
            try {
                XposedHelpers.findClass("com.android.settings.development.AppPicker", loadPackageParam.classLoader);
                className = "com.android.settings.development.AppPicker";
                XposedBridge.log("XDebuggable: Class com.android.settings.development.AppPicker FOUND!");
            } catch (XposedHelpers.ClassNotFoundError e) {
                XposedBridge.log("XDebuggable: Class com.android.settings.development.AppPicker not found.");
            }

            if (!className.equals("")) {
                XposedHelpers.findAndHookMethod(
                        className,
                        loadPackageParam.classLoader,
                        "onCreate",
                        "android.os.Bundle",
                        myHook
                );
            }

        } else if (loadPackageParam.packageName.equals("android")) {
            // Inspired from https://github.com/jecelyin/buildprop/blob/master/app/src/main/java/com/jecelyin/buildprop/BuildPropEditor.java
            XposedBridge.hookAllMethods(android.os.Process.class, "start", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    // http://androidxref.com/7.1.1_r6/xref/frameworks/base/core/java/android/os/Process.java#start
                    String niceName = (String) param.args[1];
                    int flags = (int) param.args[5];

//                    if (niceName.equals(Settings.Global.getString(new ContentResolver(null) {}, Settings.Global.DEBUG_APP))) {
                    param.args[5] = flags | 1;  // http://androidxref.com/7.1.1_r6/xref/frameworks/base/core/java/com/android/internal/os/Zygote.java#DEBUG_ENABLE_DEBUGGER
                    XposedBridge.log("Set flag DEBUG_ENABLE_DEBUGGER for app " + niceName);
//                    }
                }
            });
        }
    }
}
