package com.example.afyatrack;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ANCNotificationsActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private String currentUserId;
    private RecyclerView notificationsRecyclerView;
    private NotificationsAdapter notificationsAdapter;
    private List<NotificationModel> notificationsList;
    private LinearLayout emptyStateLayout;
    private TextView emptyStateText;
    private ImageButton btnBack, btnClearAll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ancnotifications);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        currentUserId = user.getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        initViews();
        setupClickListeners();
        loadNotifications();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        btnClearAll = findViewById(R.id.btn_clear_all);
        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        emptyStateText = findViewById(R.id.emptyStateText);

        notificationsList = new ArrayList<>();
        notificationsAdapter = new NotificationsAdapter(notificationsList, new NotificationsAdapter.OnNotificationClickListener() {
            @Override
            public void onNotificationClick(NotificationModel notification, int position) {
                markAsRead(notification.getId());

                // Handle different notification types
                handleNotificationAction(notification);
            }

            @Override
            public void onDeleteClick(String notificationId) {
                deleteNotification(notificationId);
            }
        });

        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notificationsRecyclerView.setAdapter(notificationsAdapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());

        btnClearAll.setOnClickListener(v -> {
            if (!notificationsList.isEmpty()) {
                showClearAllConfirmation();
            }
        });
    }

    private void loadNotifications() {
        mDatabase.child("users").child(currentUserId).child("notifications")
                .orderByChild("timestamp")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        notificationsList.clear();

                        if (snapshot.exists()) {
                            for (DataSnapshot notificationSnapshot : snapshot.getChildren()) {
                                NotificationModel notification = notificationSnapshot.getValue(NotificationModel.class);
                                if (notification != null) {
                                    notification.setId(notificationSnapshot.getKey());
                                    notificationsList.add(notification);
                                }
                            }

                            // Sort by timestamp (newest first)
                            Collections.sort(notificationsList, new Comparator<NotificationModel>() {
                                @Override
                                public int compare(NotificationModel n1, NotificationModel n2) {
                                    return Long.compare(n2.getTimestamp(), n1.getTimestamp());
                                }
                            });

                            updateUI();
                        } else {
                            showEmptyState();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showEmptyState();
                    }
                });
    }

    private void updateUI() {
        if (notificationsList.isEmpty()) {
            showEmptyState();
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            notificationsRecyclerView.setVisibility(View.VISIBLE);
            notificationsAdapter.notifyDataSetChanged();
        }
    }

    private void showEmptyState() {
        emptyStateLayout.setVisibility(View.VISIBLE);
        notificationsRecyclerView.setVisibility(View.GONE);
        emptyStateText.setText("No notifications yet");
    }

    private void markAsRead(String notificationId) {
        mDatabase.child("users").child(currentUserId).child("notifications")
                .child(notificationId).child("read").setValue(true);
    }

    private void markAllAsRead() {
        for (NotificationModel notification : notificationsList) {
            if (!notification.isRead()) {
                mDatabase.child("users").child(currentUserId).child("notifications")
                        .child(notification.getId()).child("read").setValue(true);
            }
        }
    }

    private void deleteNotification(String notificationId) {
        mDatabase.child("users").child(currentUserId).child("notifications")
                .child(notificationId).removeValue();
    }

    private void clearAllNotifications() {
        mDatabase.child("users").child(currentUserId).child("notifications")
                .removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        notificationsList.clear();
                        updateUI();
                    }
                });
    }

    private void showClearAllConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Clear All Notifications")
                .setMessage("Are you sure you want to clear all notifications?")
                .setPositiveButton("Clear All", (dialog, which) -> clearAllNotifications())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void handleNotificationAction(NotificationModel notification) {
        switch (notification.getType()) {
            case "profile_edited":
                // Open child profile
                Intent profileIntent = new Intent(this, ChildProfile.class);
                profileIntent.putExtra("CHILD_ID", notification.getChildId());
                startActivity(profileIntent);
                break;
            case "vaccine_due":
            case "vaccine_urgent":
                // Open vaccine dashboard
                Intent vaccineIntent = new Intent(this, ChildVaccineActivity.class);
                startActivity(vaccineIntent);
                break;
            case "appointment":
                // Open appointments
                Intent appointmentIntent = new Intent(this, AppointmentsActivity.class);
                startActivity(appointmentIntent);
                break;
            // Add more cases as needed
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Mark all as read when user opens the notifications screen
        markAllAsRead();
        updateNotificationBadge();
    }

    private void updateNotificationBadge() {
        // This will be called from ChildVaccineActivity
    }

    // Static method to send notification (call this from other activities)
    public static void sendNotification(String userId, String title, String message, String type, String childId) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        String notificationId = dbRef.child("users").child(userId).child("notifications").push().getKey();

        if (notificationId != null) {
            NotificationModel notification = new NotificationModel(
                    notificationId, title, message, type, childId
            );

            dbRef.child("users").child(userId).child("notifications")
                    .child(notificationId).setValue(notification);
        }
    }
}