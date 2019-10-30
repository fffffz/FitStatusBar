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
import android.widget.LinearLayout;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Properties;

public class StatusBarManager2 {

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
    public static ViewGroup setStatusBarColor(Activity activity, View view, int color) {
        if (view == null) {
            return null;
        }
        boolean isSimilarWhite = isColorSimilar(color, Color.WHITE);
        return setStatusBarColor(activity, view, color, isSimilarWhite, isSimilarWhite);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static ViewGroup setStatusBarColor(Activity activity, View view, int color, boolean isTextColorBlack, boolean shouldAddMaskBelowM) {
        if (view == null) {
            return null;
        }
        Context context = view.getContext();
        StatusBarLayout statusBarLayout;
        StatusBar statusBar;
        if (view instanceof StatusBarLayout) {
            statusBarLayout = (StatusBarLayout) view;
            statusBar = (StatusBar) statusBarLayout.getChildAt(0);
            statusBar.setVisibility(View.VISIBLE);
        } else {
            int statusBarHeight = getStatusBarHeight(activity);
            statusBarLayout = new StatusBarLayout(context);
            statusBar = new StatusBar(context);
            statusBarLayout.addView(statusBar, ViewGroup.LayoutParams.MATCH_PARENT, statusBarHeight);
            statusBarLayout.addView(view);
        }
        if (setStatusBarTextColor(activity, isTextColorBlack)) {
            statusBar.setAlpha(1);
        } else if (shouldAddMaskBelowM) {
            statusBar.setAlpha(0.8f);
        }
        statusBar.setBackgroundColor(color);
        return statusBarLayout;
    }

    public static void removePlaceholder(View view) {
        if (!(view instanceof StatusBarLayout)) {
            return;
        }
        StatusBarLayout statusBarLayout = (StatusBarLayout) view;
        statusBarLayout.getChildAt(0).setVisibility(View.GONE);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static void setStatusBarTransparent(Activity activity) {
        Window window = activity.getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        } else {
            window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    public static void fitStatusBarTextColor(Activity activity, int color) {
        boolean isSimilarWhite = isColorSimilar(color, Color.WHITE);
        setStatusBarTextColor(activity, isSimilarWhite);
    }

    public static boolean setStatusBarTextColor(Activity activity, boolean isTextColorBlack) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return setStatusBarTextColorForM(activity, isTextColorBlack);
        } else if (isMIUI()) {
            return setStatusBarColorForMIUI(activity, isTextColorBlack);
        } else if (isFlymeUI()) {
            return setStatusBarColorForFlymeUI(activity, isTextColorBlack);
        }
        return false;
    }

    private static boolean setStatusBarTextColorForM(Activity activity, boolean isTextColorBlack) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
            return true;
        }
        return false;
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

    private static class StatusBarLayout extends LinearLayout {
        public StatusBarLayout(Context context) {
            super(context);
            setOrientation(VERTICAL);
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
