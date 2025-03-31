package com.example.maratons;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeHelper {
    private static final String PREFERENCES_NAME = "theme_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";

    // Сохранить выбранный режим темы
    public static void setThemeMode(Context context, int mode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply();

        // Установить режим темы
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    // Получить сохраненный режим темы
    public static int getThemeMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        // По умолчанию используется светлая тема
        return prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_NO);
    }

    // Переключение между режимами
    public static void toggleTheme(AppCompatActivity activity) {
        int currentMode = getThemeMode(activity);
        int newMode = (currentMode == AppCompatDelegate.MODE_NIGHT_YES)
                ? AppCompatDelegate.MODE_NIGHT_NO
                : AppCompatDelegate.MODE_NIGHT_YES;

        // Перед изменением темы обновляем изображения
        updateImagesForTheme(activity, newMode == AppCompatDelegate.MODE_NIGHT_YES);

        // Сохраняем и применяем новую тему
        setThemeMode(activity, newMode);
    }

    // Метод для обновления изображений в соответствии с темой
    private static void updateImagesForTheme(AppCompatActivity activity, boolean isDarkMode) {
        // Находим все ImageView на экране, использующие селекторы
        // и устанавливаем их selected state в соответствии с темой

        // Для MainActivity
        if (activity instanceof MainActivity) {
            updateImageIfExists(activity, R.id.lampDark, isDarkMode);
            updateImageIfExists(activity, R.id.book, isDarkMode);
        }
        // Для LoginActivity
        else if (activity instanceof LoginActivity) {
            updateImageIfExists(activity, R.id.lampLight, isDarkMode);
            updateImageIfExists(activity, R.id.atpakal3, isDarkMode);
        }
        // Для RegisterActivity
        else if (activity instanceof RegisterActivity) {
            updateImageIfExists(activity, R.id.lampLight, isDarkMode);
            updateImageIfExists(activity, R.id.atpakal, isDarkMode);
        }
        // Для ProfileActivity
        else if (activity instanceof ProfileActivity) {
            updateImageIfExists(activity, R.id.lampLight, isDarkMode);
            updateImageIfExists(activity, R.id.atpakal5, isDarkMode);
        }
        // Для ChatActivity
        else if (activity instanceof ChatActivity) {
            updateImageIfExists(activity, R.id.lampLight2, isDarkMode);
            updateImageIfExists(activity, R.id.button_back, isDarkMode);
            updateImageIfExists(activity, R.id.button_chatbox_send, isDarkMode);
        }
    }

    // Вспомогательный метод для обновления состояния одного изображения
    private static void updateImageIfExists(AppCompatActivity activity, int viewId, boolean selected) {
        ImageView imageView = activity.findViewById(viewId);
        if (imageView != null) {
            imageView.setSelected(selected);
        }
    }

    // Применить сохраненную тему при запуске приложения
    public static void applyTheme(Context context) {
        int themeMode = getThemeMode(context);
        AppCompatDelegate.setDefaultNightMode(themeMode);
    }

    // Метод для установки начального состояния изображений при запуске активности
    public static void setInitialImageState(AppCompatActivity activity) {
        boolean isDarkMode = getThemeMode(activity) == AppCompatDelegate.MODE_NIGHT_YES;
        updateImagesForTheme(activity, isDarkMode);
    }
}