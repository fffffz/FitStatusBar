package com.fffz.fitstatusbar;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Properties;

public class StatusBarManager {

    public static int mStatusBarHeight = 0;

    public static int getStatusBarHeight(Context context) {
        if (context == null)
            return 0;
        if (mStatusBarHeight != 0) {
            return mStatusBarHeight;
        }
        try {
            Class<?> clazz = Class.forName("com.android.internal.R$dimen");
            Object obj = clazz.newInstance();
            Field field = clazz.getField("status_bar_height");
            int temp = Integer.parseInt(field.get(obj).toString());
            mStatusBarHeight = context.getResources().getDimensionPixelSize(temp);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mStatusBarHeight;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static void setStatusBarColor(Activity activity, int color) {
        boolean isSimilarWhite = isColorSimilar(color, Color.WHITE);
        setStatusBarColor(activity, color, isSimilarWhite, isSimilarWhite);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static void setStatusBarColor(Activity activity, int color, boolean blackText, boolean addMaskBelowM) {
        ViewGroup rootView = activity.findViewById(android.R.id.content);
        if (rootView == null) {
            return;
        }
        View child = rootView.getChildAt(0);
        if (child == null) {
            return;
        }
        StatusBar statusBar;
        if (child instanceof StatusBar) {
            statusBar = (StatusBar) child;
        } else {
            setStatusBarTransparent(activity);
            int statusBarHeight = getStatusBarHeight(activity);
            FrameLayout.LayoutParams contentLayoutParams = (FrameLayout.LayoutParams) child.getLayoutParams();
            contentLayoutParams.topMargin += statusBarHeight;
            statusBar = new StatusBar(activity);
            ViewGroup.LayoutParams statusBarLayoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, statusBarHeight);
            rootView.addView(statusBar, 0, statusBarLayoutParams);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setStatusBarTextColorForM(activity, blackText);
        } else if (isMIUI()) {
            setStatusBarColorForMIUI(activity, blackText);
        } else if (isFlymeUI()) {
            setStatusBarColorForFlymeUI(activity, blackText);
        } else if (addMaskBelowM) {
            color = Color.argb(255, (int) (Color.red(color) * 0.8f), (int) (Color.green(color) * 0.8f), (int) (Color.blue(color) * 0.8f));
        }
        statusBar.setBackgroundColor(color);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static void setStatusBarTransparent(Activity activity) {
        ViewGroup rootView = activity.findViewById(android.R.id.content);
        if (rootView != null) {
            View child = rootView.getChildAt(0);
            if (child != null && child instanceof StatusBar) {
                View contentView = rootView.getChildAt(1);
                if (contentView != null) {
                    FrameLayout.LayoutParams contentLayoutParams = (FrameLayout.LayoutParams) contentView.getLayoutParams();
                    contentLayoutParams.topMargin -= getStatusBarHeight(activity);
                }
                rootView.removeView(child);
            }
        }
        Window window = activity.getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        } else {
            window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private static void setStatusBarTextColorForM(Activity activity, boolean isTextColorBlack) {
        Window window = activity.getWindow();
        View decorView = window.getDecorView();
        int systemUiVisibility = decorView.getSystemUiVisibility();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        if (isTextColorBlack) {
            systemUiVisibility |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        } else {
            systemUiVisibility &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        decorView.setSystemUiVisibility(systemUiVisibility);
        activity.findViewById(android.R.id.content).setForeground(null);
    }

    private static boolean setStatusBarColorForMIUI(Activity activity, boolean isTextColorBlack) {
        try {
            Window window = activity.getWindow();
            Class<? extends Window> clazz = window.getClass();
            Class<?> layoutParamsClass = Class.forName("android.view.MiuiWindowManager$LayoutParams");
            Field darkModeFlagField = layoutParamsClass.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE");
            int darkModeFlag = darkModeFlagField.getInt(layoutParamsClass);
            Method setExtraFlagsField = clazz.getMethod("setExtraFlags", int.class, int.class);
            setExtraFlagsField.invoke(window, isTextColorBlack ? darkModeFlag : 0, darkModeFlag);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean setStatusBarColorForFlymeUI(Activity activity, boolean isTextColorBlack) {
        try {
            Window window = activity.getWindow();
            WindowManager.LayoutParams attributes = window.getAttributes();
            Field darkFlagField = WindowManager.LayoutParams.class.getDeclaredField("MEIZU_FLAG_DARK_STATUS_BAR_ICON");
            Field meizuFlagsField = WindowManager.LayoutParams.class.getDeclaredField("meizuFlags");
            darkFlagField.setAccessible(true);
            meizuFlagsField.setAccessible(true);
            int darkFlag = darkFlagField.getInt(null);
            int meizuFlags = meizuFlagsField.getInt(attributes);
            if (isTextColorBlack) {
                meizuFlags |= darkFlag;
            } else {
                meizuFlags &= ~darkFlag;
            }
            meizuFlagsField.setInt(attributes, meizuFlags);
            window.setAttributes(attributes);
            darkFlagField.setAccessible(false);
            meizuFlagsField.setAccessible(false);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static class StatusBar extends View {
        public StatusBar(Context context) {
            super(context);
        }
    }

    private static final String KEY_VERSION_CODE_MIUI = "ro.miui.ui.version.code";
    private static final String KEY_VERSION_NAME_MIUI = "ro.miui.ui.version.name";
    private static final String KEY_INTERNAL_STORAGE_MIUI = "ro.miui.internal.storage";

    private static boolean isMIUI() {
        try {
            Properties properties = getBuildProp();
            return properties.getProperty(KEY_VERSION_CODE_MIUI, null) != null
                    || properties.getProperty(KEY_VERSION_NAME_MIUI, null) != null
                    || properties.getProperty(KEY_INTERNAL_STORAGE_MIUI, null) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isFlymeUI() {
        String displayId = getSystemProperty("ro.build.display.id", "");
        if (TextUtils.isEmpty(displayId)) {
            return false;
        }
        if (displayId.contains("Flyme") || displayId.toLowerCase().contains("flyme")) {
            return true;
        }
        return false;
    }

    private static Properties getBuildProp() {
        FileInputStream inputStream = null;
        try {
            Properties properties = new Properties();
            inputStream = new FileInputStream(new File(Environment.getRootDirectory(), "build.prop"));
            properties.load(inputStream);
            return properties;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static String getSystemProperty(String key, String defaultValue) {
        try {
            Class<?> systemPropertiesClass = Class.forName("android.os.SystemProperties");
            Method method = systemPropertiesClass.getMethod("get", String.class, String.class);
            String value = (String) method.invoke(null, key, defaultValue);
            return value;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return defaultValue;
    }

    private static final double COLOR_THRESHOLD = 180.0;

    private static boolean isColorSimilar(int color, int color2) {
        int sampleColor = color | 0xff000000;
        int sampleColor2 = color2 | 0xff000000;
        int red = Color.red(sampleColor) - Color.red(sampleColor2);
        int green = Color.green(sampleColor) - Color.green(sampleColor2);
        int blue = Color.blue(sampleColor) - Color.blue(sampleColor2);
        return Math.sqrt(red * red + green * green + blue * blue) < COLOR_THRESHOLD;
    }

}
