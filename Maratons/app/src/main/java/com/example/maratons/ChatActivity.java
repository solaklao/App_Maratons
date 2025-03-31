package com.example.maratons;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.util.Log;
import android.graphics.Rect;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity {

    private static final String KEY_CHAT_ID = "current_chat_id";

    private RecyclerView recyclerView;
    private EditText messageInput;
    private List<Message> messageList;
    private MessageAdapter messageAdapter;
    private View rootView;

    // Помощник для работы с базой данных
    private ChatDatabaseHelper dbHelper;
    private long currentChatId = -1;

    // Высота клавиатуры
    private int keyboardHeight = 0;
    private boolean keyboardVisible = false;

    // JSON media type
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    // OkHttp client с увеличенными таймаутами
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)  // увеличенный таймаут подключения
            .readTimeout(60, TimeUnit.SECONDS)     // увеличенный таймаут чтения
            .writeTimeout(30, TimeUnit.SECONDS)    // увеличенный таймаут записи
            .retryOnConnectionFailure(true)        // включить повторные попытки
            .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Применяем сохраненную тему перед созданием активности
        ThemeHelper.applyTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Установить начальное состояние изображений
        ThemeHelper.setInitialImageState(this);

        // Инициализируем помощник для работы с базой данных
        dbHelper = new ChatDatabaseHelper(this);

        // Initialize views
        rootView = findViewById(R.id.main);
        recyclerView = findViewById(R.id.recyclerView);
        messageInput = findViewById(R.id.edittext_chatbox);
        LinearLayout chatContainer = findViewById(R.id.chat_container);
        ImageView sendButton = findViewById(R.id.button_chatbox_send);
        ImageView backButton = findViewById(R.id.button_back);
        ImageView lampButton = findViewById(R.id.lampLight2);

        // Добавляем обработчик клика на лампу для смены темы
        lampButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ThemeHelper.toggleTheme(ChatActivity.this);
            }
        });

        // Initialize message list and adapter
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(this, messageList);

        // Set up RecyclerView with a LinearLayoutManager that stacks from bottom
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Сообщения начинаются снизу
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(messageAdapter);

        // Восстанавливаем ID чата из сохраненного состояния или Intent
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_CHAT_ID)) {
            currentChatId = savedInstanceState.getLong(KEY_CHAT_ID);
            if (currentChatId != -1) {
                loadChatMessages(currentChatId);
            }
        } else if (getIntent().hasExtra("chat_id")) {
            currentChatId = getIntent().getLongExtra("chat_id", -1);
            loadChatMessages(currentChatId);
        } else {
            // Добавляем приветственное сообщение для новых чатов
            Message welcomeMessage = new Message("Hello! I can help you with:\n1. Finding Latvian names\n2. Generating random users\n3. Searching cities by letter\n\nType 'help' for more information.", true);
            messageList.add(welcomeMessage);
            messageAdapter.notifyItemInserted(0);
            scrollToBottom();
        }

        // Добавляем слушатель для отслеживания изменений размера клавиатуры
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            private final Rect r = new Rect();
            private int lastVisibleHeight;

            @Override
            public void onGlobalLayout() {
                // Получаем размеры экрана
                rootView.getWindowVisibleDisplayFrame(r);
                int visibleHeight = r.height();

                // Если это первый вызов, инициализируем начальную высоту
                if (lastVisibleHeight == 0) {
                    lastVisibleHeight = visibleHeight;
                    return;
                }

                // Определяем, появилась или исчезла клавиатура
                int heightDiff = lastVisibleHeight - visibleHeight;

                if (heightDiff > 200) { // Клавиатура появилась
                    keyboardHeight = heightDiff;
                    keyboardVisible = true;

                    // Прокручиваем чат к последнему сообщению
                    scrollToBottom();
                } else if (heightDiff < -200) { // Клавиатура исчезла
                    keyboardVisible = false;
                }

                lastVisibleHeight = visibleHeight;
            }
        });

        // Set up back button click listener
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Закрыть активность при нажатии на кнопку назад
                finish();
            }
        });

        // Set up send button click listener
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = messageInput.getText().toString().trim();
                if (!message.isEmpty()) {
                    sendMessage(message);
                }
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(KEY_CHAT_ID, currentChatId);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadChatMessages(long chatId) {
        // Получаем сообщения из базы данных
        List<Message> messages = dbHelper.getChatMessages(chatId);

        // Добавляем их в messageList
        messageList.clear();
        messageList.addAll(messages);
        messageAdapter.notifyDataSetChanged();

        // Прокручиваем к последнему сообщению
        scrollToBottom();
    }

    private void scrollToBottom() {
        if (!messageList.isEmpty()) {
            recyclerView.post(new Runnable() {
                @Override
                public void run() {
                    recyclerView.smoothScrollToPosition(messageList.size() - 1);
                }
            });
        }
    }

    private void sendMessage(String messageText) {
        // Добавляем сообщение в список
        Message userMessage = new Message(messageText, false);
        messageList.add(userMessage);
        messageAdapter.notifyItemInserted(messageList.size() - 1);

        // Создаем новый чат, если это первое сообщение
        if (currentChatId == -1) {
            currentChatId = dbHelper.createChat(messageText);
        }

        // Сохраняем сообщение в базу данных
        dbHelper.addMessage(currentChatId, messageText, false);

        // Очищаем поле ввода
        messageInput.setText("");

        // Прокручиваем вниз
        scrollToBottom();

        // Отправляем сообщение в API
        sendMessageToApi(messageText);
    }

    private void sendMessageToApi(String messageText) {
        try {
            // Create JSON request body
            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("message", messageText);
            RequestBody body = RequestBody.create(jsonRequest.toString(), JSON);

            // Log the request for debugging
            Log.d("ChatActivity", "Sending request: " + jsonRequest.toString());

            // Create request
            String API_URL = "https://e9a4-85-254-74-32.ngrok-free.app/api/chat";
            Request request = new Request.Builder()
                    .url(API_URL)
                    .post(body)
                    .build();

            // Send request asynchronously
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e("ChatActivity", "Network error details: " + e.getMessage(), e);

                    String errorMessage = "Network error: ";
                    if (e instanceof java.net.UnknownHostException) {
                        errorMessage += "Can't reach server. Please check your connection.";
                    } else if (e instanceof java.net.SocketTimeoutException) {
                        errorMessage += "Connection timeout. Server is taking too long to respond.";
                    } else if (e instanceof javax.net.ssl.SSLHandshakeException) {
                        errorMessage += "SSL Certificate error. Check your connection security.";
                    } else {
                        errorMessage += e.getMessage();
                    }

                    final String finalErrorMessage = errorMessage;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ChatActivity.this, finalErrorMessage, Toast.LENGTH_LONG).show();

                            // Add error message to chat
                            Message errorMessage = new Message(finalErrorMessage, true);
                            messageList.add(errorMessage);
                            messageAdapter.notifyItemInserted(messageList.size() - 1);
                            scrollToBottom();
                        }
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String responseData = response.body() != null ? response.body().string() : "No body";
                    Log.d("ChatActivity", "Response received: " + responseData);

                    if (response.isSuccessful()) {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseData);
                            final String messageResponse;

                            // Check if it's a system command
                            if (jsonResponse.optBoolean("system_command", false)) {
                                messageResponse = jsonResponse.getString("response");
                            } else {
                                messageResponse = jsonResponse.getString("response");
                            }

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Message apiMessage = new Message(messageResponse, true);
                                    messageList.add(apiMessage);
                                    messageAdapter.notifyItemInserted(messageList.size() - 1);

                                    // Сохраняем ответ ассистента в базу данных
                                    dbHelper.addMessage(currentChatId, messageResponse, true);

                                    scrollToBottom();
                                }
                            });
                        } catch (JSONException e) {
                            Log.e("ChatActivity", "Error parsing JSON: " + e.getMessage(), e);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(ChatActivity.this, "Error parsing response: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                                    // Добавляем сообщение об ошибке в чат
                                    Message errorMessage = new Message("Error parsing response. Technical details: " + e.getMessage(), true);
                                    messageList.add(errorMessage);
                                    messageAdapter.notifyItemInserted(messageList.size() - 1);
                                    scrollToBottom();
                                }
                            });
                        }
                    } else {
                        Log.e("ChatActivity", "Server error: " + response.code() + " - " + responseData);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String errorMsg = "Server error: " + response.code();
                                Toast.makeText(ChatActivity.this, errorMsg, Toast.LENGTH_SHORT).show();

                                // Add error message to chat
                                Message errorMessage = new Message("Server error. Please try again later. (Code: " + response.code() + ")", true);
                                messageList.add(errorMessage);
                                messageAdapter.notifyItemInserted(messageList.size() - 1);
                                scrollToBottom();
                            }
                        });
                    }
                }
            });
        } catch (JSONException e) {
            Log.e("ChatActivity", "Error creating JSON request: " + e.getMessage(), e);
        }
    }
}