package mobi.acpm.inspeckage.hooks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import mobi.acpm.inspeckage.Module;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class DumpDexHook extends XC_MethodHook {
    public static final String TAG = "Inspeckage_DumpDexHook:";
    private static XSharedPreferences sPrefs;

    public static void loadPrefs() {
        sPrefs = new XSharedPreferences(Module.class.getPackage().getName(), Module.PREFS);
        sPrefs.makeWorldReadable();
    }

    public static void initAllHooks(XC_LoadPackage.LoadPackageParam lpparam) {
        findAndHookMethod("java.lang.ClassLoader", lpparam.classLoader, "loadClass", String.class, Boolean.TYPE, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                loadPrefs();
                String packagename = sPrefs.getString("package", "");

                Method getBytes = Class.forName("com.android.dex.Dex").getDeclaredMethod("getBytes", new Class[0]);
                Method getDex = Class.forName("java.lang.Class").getDeclaredMethod("getDex", new Class[0]);

                Class cls = (Class) param.getResult();
                if (cls == null) {
                    return;
                }
                byte[] bArr = (byte[]) getBytes.invoke(getDex.invoke(cls, new Object[0]), new Object[0]);
                if (bArr == null) {
                    return;
                }
                String dex_path = "/data/data/" + packagename + "/" + packagename + "_" + bArr.length + ".dex";
                XposedBridge.log(TAG + dex_path);
                File file = new File(dex_path);
                if (file.exists()) return;
                writeByte(bArr, file.getAbsolutePath());
            }
        });
    }

    private static void writeByte(byte[] bArr, String str) {
        try {
            OutputStream outputStream = new FileOutputStream(str);
            outputStream.write(bArr);
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            XposedBridge.log("文件写出失败");
        }
    }
}