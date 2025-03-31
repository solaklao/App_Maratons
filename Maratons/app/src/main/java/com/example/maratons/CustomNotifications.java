package com.example.maratons;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.snackbar.Snackbar;

public class CustomNotifications {

    // Кастомный Toast с нужным стилем
    public static void showCustomToast(Context context, String message) {
        // Создаем кастомный макет для Toast
        View view = LayoutInflater.from(context).inflate(R.layout.custom_toast, null);
        TextView textView = view.findViewById(R.id.toast_text);
        textView.setText(message);

        // Устанавливаем шрифт Kurale
        textView.setTypeface(ResourcesCompat.getFont(context, R.font.kurale));

        // Создаем Toast с кастомным макетом
        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(view);
        toast.show();
    }

    // Кастомный длинный Toast
    public static void showCustomLongToast(Context context, String message) {
        View view = LayoutInflater.from(context).inflate(R.layout.custom_toast, null);
        TextView textView = view.findViewById(R.id.toast_text);
        textView.setText(message);
        textView.setTypeface(ResourcesCompat.getFont(context, R.font.kurale));

        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(view);
        toast.show();
    }

    // Кастомный Snackbar с нужным стилем
    public static void showCustomSnackbar(View view, String message) {
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);

        // Получаем доступ к текстовому представлению Snackbar
        View snackbarView = snackbar.getView();
        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);

        // Устанавливаем шрифт Kurale и другие стили
        textView.setTypeface(ResourcesCompat.getFont(view.getContext(), R.font.kurale));

        // Устанавливаем фиксированные цвета вместо зависящих от темы
        snackbarView.setBackgroundColor(0xFF97633B); // Светлый фон (коричневый) для всех тем
        textView.setTextColor(0xFF3C2116); // Темный текст (темно-коричневый) для всех тем

        snackbar.show();
    }
}