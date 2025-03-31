package com.example.maratons;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatHistoryAdapter extends RecyclerView.Adapter<ChatHistoryAdapter.ChatHistoryViewHolder> {

    private final Context context;
    private final List<ChatSession> chatList;
    private final OnChatClickListener chatClickListener;

    // Интерфейс для обработки кликов по элементам
    public interface OnChatClickListener {
        void onChatClick(ChatSession chatSession);
        void onChatLongClick(ChatSession chatSession, int position);
    }

    public ChatHistoryAdapter(Context context, List<ChatSession> chatList, OnChatClickListener listener) {
        this.context = context;
        this.chatList = chatList;
        this.chatClickListener = listener;
    }

    @NonNull
    @Override
    public ChatHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_history, parent, false);
        return new ChatHistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatHistoryViewHolder holder, int position) {
        ChatSession chatSession = chatList.get(position);

        // Устанавливаем время
        holder.chatTimeTextView.setText(chatSession.getFormattedTime());

        // Устанавливаем текст превью (первое сообщение)
        String previewText = chatSession.getFirstMessage();
        if (previewText.length() > 50) {
            previewText = previewText.substring(0, 47) + "...";
        }
        holder.chatPreviewTextView.setText(previewText);

        // Устанавливаем обработчики кликов
        holder.itemView.setOnClickListener(view -> chatClickListener.onChatClick(chatSession));
        holder.itemView.setOnLongClickListener(view -> {
            chatClickListener.onChatLongClick(chatSession, holder.getAdapterPosition());
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    // Удалить чат из списка
    public void removeChat(int position) {
        if (position >= 0 && position < chatList.size()) {
            chatList.remove(position);
            notifyItemRemoved(position);
        }
    }

    static class ChatHistoryViewHolder extends RecyclerView.ViewHolder {
        TextView chatTimeTextView;
        TextView chatPreviewTextView;

        ChatHistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            chatTimeTextView = itemView.findViewById(R.id.text_chat_time);
            chatPreviewTextView = itemView.findViewById(R.id.text_chat_preview);
        }
    }
}