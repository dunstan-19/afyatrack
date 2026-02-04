package com.example.afyatrack;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder> {

    private List<NotificationModel> notificationsList;
    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationModel notification, int position);
        void onDeleteClick(String notificationId);
    }

    public NotificationsAdapter(List<NotificationModel> notificationsList, OnNotificationClickListener listener) {
        this.notificationsList = notificationsList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationModel notification = notificationsList.get(position);

        holder.titleTextView.setText(notification.getTitle());
        holder.messageTextView.setText(notification.getMessage());
        holder.timeTextView.setText(notification.getFormattedTime());

        // Set icon based on notification type
        int iconRes = getIconForType(notification.getType());
        int colorRes = getColorForType(notification.getType());

        holder.iconImageView.setImageResource(iconRes);
        holder.iconImageView.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), colorRes));

        // Set unread indicator
        if (!notification.isRead()) {
            holder.unreadIndicator.setVisibility(View.VISIBLE);
            holder.cardView.setCardBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.text_secondary)
            );
        } else {
            holder.unreadIndicator.setVisibility(View.GONE);
            holder.cardView.setCardBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), android.R.color.white)
            );
        }

        // Set click listeners
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(notification, position);
            }
        });

        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(notification.getId());
            }
        });
    }

    private int getIconForType(String type) {
        switch (type) {
            case "profile_edited":
                return R.drawable.baseline_person_pin_24;
            case "vaccine_due":
            case "vaccine_urgent":
                return R.drawable.baseline_circle_notifications_24;
            case "appointment":
                return R.drawable.calendar_clock_24px;
            case "system":
                return R.drawable.baseline_circle_notifications_24;
            default:
                return R.drawable.baseline_circle_notifications_24;
        }
    }

    private int getColorForType(String type) {
        switch (type) {
            case "profile_edited":
                return R.color.primary_blue;
            case "vaccine_due":
                return R.color.primary_orange;
            case "vaccine_urgent":
                return R.color.primary_red;
            case "appointment":
                return R.color.primary_purple;
            case "system":
                return R.color.primary_emerald;
            default:
                return R.color.text_primary;
        }
    }

    @Override
    public int getItemCount() {
        return notificationsList.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        ImageView iconImageView;
        TextView titleTextView, messageTextView, timeTextView;
        View unreadIndicator;
        ImageView deleteButton;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_notification);
            iconImageView = itemView.findViewById(R.id.iconImageView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);
            deleteButton = itemView.findViewById(R.id.btn_delete);
        }
    }
}