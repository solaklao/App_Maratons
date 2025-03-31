package com.example.maratons;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class ChatDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "chat_history.db";
    private static final int DATABASE_VERSION = 1;

    // Имена таблиц
    public static final String TABLE_CHATS = "chats";
    public static final String TABLE_MESSAGES = "messages";

    // Общие названия столбцов
    public static final String COLUMN_ID = "id";

    // Названия столбцов таблицы чатов
    public static final String COLUMN_CHAT_TIMESTAMP = "timestamp";
    public static final String COLUMN_CHAT_FIRST_MESSAGE = "first_message";

    // Названия столбцов таблицы сообщений
    public static final String COLUMN_CHAT_ID = "chat_id";
    public static final String COLUMN_MESSAGE_TEXT = "message_text";
    public static final String COLUMN_IS_ASSISTANT = "is_assistant";
    public static final String COLUMN_MESSAGE_TIMESTAMP = "timestamp";

    // SQL запросы для создания таблиц
    private static final String CREATE_TABLE_CHATS = "CREATE TABLE " + TABLE_CHATS + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_CHAT_TIMESTAMP + " INTEGER, "
            + COLUMN_CHAT_FIRST_MESSAGE + " TEXT"
            + ")";

    private static final String CREATE_TABLE_MESSAGES = "CREATE TABLE " + TABLE_MESSAGES + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_CHAT_ID + " INTEGER, "
            + COLUMN_MESSAGE_TEXT + " TEXT, "
            + COLUMN_IS_ASSISTANT + " INTEGER, "
            + COLUMN_MESSAGE_TIMESTAMP + " INTEGER, "
            + "FOREIGN KEY(" + COLUMN_CHAT_ID + ") REFERENCES " + TABLE_CHATS + "(" + COLUMN_ID + ")"
            + ")";

    public ChatDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_CHATS);
        db.execSQL(CREATE_TABLE_MESSAGES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Удаляем старые таблицы, если они существуют
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHATS);

        // Создаем таблицы заново
        onCreate(db);
    }

    // Создать новый чат и вернуть его ID
    public long createChat(String firstMessage) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_CHAT_TIMESTAMP, System.currentTimeMillis());
        values.put(COLUMN_CHAT_FIRST_MESSAGE, firstMessage);

        // Вставляем запись
        long chatId = db.insert(TABLE_CHATS, null, values);

        db.close();
        return chatId;
    }

    // Добавить сообщение в существующий чат
    public long addMessage(long chatId, String messageText, boolean isAssistant) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_CHAT_ID, chatId);
        values.put(COLUMN_MESSAGE_TEXT, messageText);
        values.put(COLUMN_IS_ASSISTANT, isAssistant ? 1 : 0);
        values.put(COLUMN_MESSAGE_TIMESTAMP, System.currentTimeMillis());

        // Вставляем запись
        long messageId = db.insert(TABLE_MESSAGES, null, values);

        db.close();
        return messageId;
    }

    // Получить все чаты
    public List<ChatSession> getAllChats() {
        List<ChatSession> chatList = new ArrayList<>();

        String selectQuery = "SELECT * FROM " + TABLE_CHATS + " ORDER BY " + COLUMN_CHAT_TIMESTAMP + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // Перебираем все строки и добавляем в список
        if (cursor.moveToFirst()) {
            do {
                ChatSession chat = new ChatSession();
                chat.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                chat.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CHAT_TIMESTAMP)));
                chat.setFirstMessage(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CHAT_FIRST_MESSAGE)));

                chatList.add(chat);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return chatList;
    }

    // Получить все сообщения для конкретного чата
    public List<Message> getChatMessages(long chatId) {
        List<Message> messageList = new ArrayList<>();

        String selectQuery = "SELECT * FROM " + TABLE_MESSAGES +
                " WHERE " + COLUMN_CHAT_ID + " = " + chatId +
                " ORDER BY " + COLUMN_MESSAGE_TIMESTAMP + " ASC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // Перебираем все строки и добавляем в список
        if (cursor.moveToFirst()) {
            do {
                String messageText = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_TEXT));
                boolean isAssistant = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_ASSISTANT)) == 1;

                Message message = new Message(messageText, isAssistant);
                messageList.add(message);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return messageList;
    }

    // Удалить чат и его сообщения
    public void deleteChat(long chatId) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Сначала удаляем сообщения (из-за внешнего ключа)
        db.delete(TABLE_MESSAGES, COLUMN_CHAT_ID + " = ?", new String[]{String.valueOf(chatId)});

        // Затем удаляем чат
        db.delete(TABLE_CHATS, COLUMN_ID + " = ?", new String[]{String.valueOf(chatId)});

        db.close();
    }
}