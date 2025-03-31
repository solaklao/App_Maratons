package com.example.maratons;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.maratons.databinding.ActivityProfileBinding;

import java.util.List;

public class ProfileActivity extends AppCompatActivity implements ChatHistoryAdapter.OnChatClickListener {
    private ActivityProfileBinding binding;
    private ChatDatabaseHelper dbHelper;
    private List<ChatSession> chatList;
    private ChatHistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Применяем сохраненную тему перед созданием активности
        ThemeHelper.applyTheme(this);

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Установить начальное состояние изображений
        ThemeHelper.setInitialImageState(this);

        // Добавляем обработчик клика на лампу для смены темы
        binding.lampLight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ThemeHelper.toggleTheme(ProfileActivity.this);
            }
        });

        // Инициализируем помощник для работы с базой данных
        dbHelper = new ChatDatabaseHelper(this);

        // Настраиваем кнопку нового чата
        binding.jaunsChats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ProfileActivity.this, ChatActivity.class));
            }
        });

        // Добавляем обработчик для кнопки "назад"
        binding.atpakal5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Создаем Intent для перехода на LoginActivity
                Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                startActivity(intent);
                // Опционально: закрываем текущую Activity
                finish();
            }
        });

        // Настраиваем RecyclerView
        binding.recyclerViewHistory.setLayoutManager(new LinearLayoutManager(this));

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChatHistory();
    }

    private void loadChatHistory() {
        // Получаем историю чатов из базы данных
        chatList = dbHelper.getAllChats();

        // Настраиваем адаптер
        adapter = new ChatHistoryAdapter(this, chatList, this);
        binding.recyclerViewHistory.setAdapter(adapter);

        // Показываем/скрываем текст о пустой истории
        if (chatList.isEmpty()) {
            binding.textEmptyHistory.setVisibility(View.VISIBLE);
        } else {
            binding.textEmptyHistory.setVisibility(View.GONE);
        }
    }

    @Override
    public void onChatClick(ChatSession chatSession) {
        // Открываем чат с этим ID сессии
        Intent intent = new Intent(ProfileActivity.this, ChatActivity.class);
        intent.putExtra("chat_id", chatSession.getId());
        startActivity(intent);
    }

    @Override
    public void onChatLongClick(ChatSession chatSession, int position) {
        // Создайте AlertDialog.Builder с вашей кастомной темой
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialogTheme);

        // Настройте диалог
        builder.setTitle("Izdzest čatu?")
                .setMessage("Esi parliecināts/ta, ka gribi izdzēst čātu?")
                .setPositiveButton("Izdzēst", (dialog, which) -> {
                    // Удаляем чат из базы данных
                    dbHelper.deleteChat(chatSession.getId());

                    // Удаляем из адаптера
                    adapter.removeChat(position);

                    // Показываем/скрываем текст о пустой истории
                    if (chatList.isEmpty()) {
                        binding.textEmptyHistory.setVisibility(View.VISIBLE);
                    }
                })
                .setNegativeButton("Atcelt", null);

        // Создайте и покажите диалог
        AlertDialog dialog = builder.create();
        dialog.show();

        // Применяем стили к тексту программно
        TextView messageView = dialog.findViewById(android.R.id.message);
        if (messageView != null) {
            messageView.setTextColor(getResources().getColor(R.color.textColor));
            messageView.setTypeface(ResourcesCompat.getFont(this, R.font.kurale));
        }

        // Применяем стили к кнопкам программно
        Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);

        if (positiveButton != null) {
            positiveButton.setTextColor(getResources().getColor(R.color.textColor));
            positiveButton.setTypeface(ResourcesCompat.getFont(this, R.font.kurale));
        }

        if (negativeButton != null) {
            negativeButton.setTextColor(getResources().getColor(R.color.textColor));
            negativeButton.setTypeface(ResourcesCompat.getFont(this, R.font.kurale));
        }
    }
}