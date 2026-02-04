package com.example.afyatrack;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private ImageButton btnChangePassword, btnDeleteAccount;
    private ImageButton btnAutoLock, btnClearCache, btnFontSize;
    private MaterialButton btnAddChild, btnSignOut;

    private TextView tvAutoLock, tvCacheSize, tvFontSize;
    private CardView cardChildren;
    private LinearLayout childrenListContainer;
    private TextView tvNoChildren;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private SharedPreferences sharedPreferences;
    private String currentUserId;

    private List<ChildItem> childrenList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        currentUserId = user.getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);

        initViews();
        loadSettings();
        loadChildren();
        setupClickListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);

        // Children section
        cardChildren = findViewById(R.id.card_children);
        childrenListContainer = findViewById(R.id.children_list_container);
        tvNoChildren = findViewById(R.id.tv_no_children);
        btnAddChild = findViewById(R.id.btn_add_child);

        // Account management
        btnChangePassword = findViewById(R.id.btn_change_password);
        btnDeleteAccount = findViewById(R.id.btn_delete_account);

        // Privacy & Security
        tvAutoLock = findViewById(R.id.tv_auto_lock);
        tvCacheSize = findViewById(R.id.tv_cache_size);
        btnAutoLock = findViewById(R.id.btn_auto_lock);
        btnClearCache = findViewById(R.id.btn_clear_cache);

        // App Preferences
        tvFontSize = findViewById(R.id.tv_font_size);
        btnFontSize = findViewById(R.id.btn_font_size);

        // Sign Out
        btnSignOut = findViewById(R.id.btn_sign_out);
    }

    private void loadSettings() {
        // Load auto-lock setting
        String autoLockValue = sharedPreferences.getString("auto_lock", "5 minutes");
        tvAutoLock.setText(autoLockValue);

        // Load font size setting and apply it
        String fontSize = sharedPreferences.getString("font_size", "Medium");
        tvFontSize.setText(fontSize);
        applyFontSize(fontSize);

        // Calculate actual cache size
        updateCacheSize();
    }

    private void loadChildren() {
        mDatabase.child("users").child(currentUserId).child("children")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        childrenList.clear();
                        childrenListContainer.removeAllViews();

                        if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                            cardChildren.setVisibility(View.VISIBLE);
                            tvNoChildren.setVisibility(View.GONE);

                            for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                                String childId = childSnapshot.getKey();
                                String name = childSnapshot.child("name").getValue(String.class);
                                String dob = childSnapshot.child("dateOfBirth").getValue(String.class);
                                String gender = childSnapshot.child("gender").getValue(String.class);

                                if (name != null && childId != null) {
                                    ChildItem childItem = new ChildItem(childId, name, dob, gender);
                                    childrenList.add(childItem);
                                    addChildToView(childItem);
                                }
                            }
                        } else {
                            cardChildren.setVisibility(View.VISIBLE);
                            tvNoChildren.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(SettingsActivity.this,
                                "Failed to load children", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addChildToView(ChildItem childItem) {
        View childView = LayoutInflater.from(this).inflate(R.layout.child_item_layout, null);

        ImageView imgAvatar = childView.findViewById(R.id.img_child_avatar);
        TextView tvName = childView.findViewById(R.id.tv_child_name);
        TextView tvAge = childView.findViewById(R.id.tv_child_age);
        ImageButton btnOptions = childView.findViewById(R.id.btn_child_options);

        // Set child data
        tvName.setText(childItem.getName());
        tvAge.setText(calculateAge(childItem.getDob()));

        // Set avatar based on gender
        imgAvatar.setImageResource(
                "Male".equalsIgnoreCase(childItem.getGender())
                        ? R.drawable.boyavatar
                        : R.drawable.girlavatar
        );

        // Set click listeners
        childView.setOnClickListener(v -> {
            // Open ChildProfile activity
            Intent intent = new Intent(SettingsActivity.this, ChildProfile.class);
            intent.putExtra("CHILD_ID", childItem.getId());
            startActivity(intent);
        });

        btnOptions.setOnClickListener(v -> {
            showChildOptionsDialog(childItem);
        });

        childrenListContainer.addView(childView);
    }

    private String calculateAge(String dob) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Calendar birth = Calendar.getInstance();
            birth.setTime(sdf.parse(dob));
            Calendar today = Calendar.getInstance();

            int years = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR);
            int months = today.get(Calendar.MONTH) - birth.get(Calendar.MONTH);

            if (months < 0) {
                years--;
                months += 12;
            }

            if (years > 0) {
                return years + (years == 1 ? " year" : " years");
            } else {
                return months + (months == 1 ? " month" : " months");
            }
        } catch (Exception e) {
            return "Age not available";
        }
    }

    private void showChildOptionsDialog(ChildItem childItem) {
        String[] options = {"View Profile", "Set as Active", "Delete Child"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(childItem.getName());
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // View Profile
                    Intent intent = new Intent(this, ChildProfile.class);
                    intent.putExtra("CHILD_ID", childItem.getId());
                    startActivity(intent);
                    break;

                case 1: // Set as Active
                    mDatabase.child("users").child(currentUserId).child("selectedChild")
                            .setValue(childItem.getId())
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(this,
                                            childItem.getName() + " set as active child",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                    break;

                case 2: // Delete Child
                    showDeleteChildConfirmation(childItem);
                    break;
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showDeleteChildConfirmation(ChildItem childItem) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Child");
        builder.setMessage("Are you sure you want to delete " + childItem.getName() + "'s profile?");

        builder.setPositiveButton("Delete", (dialog, which) -> {
            deleteChild(childItem);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void deleteChild(ChildItem childItem) {
        mDatabase.child("users").child(currentUserId).child("children").child(childItem.getId())
                .removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                childItem.getName() + "'s profile deleted",
                                Toast.LENGTH_SHORT).show();
                        loadChildren(); // Refresh the list
                    } else {
                        Toast.makeText(this,
                                "Failed to delete child profile",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());

        btnAddChild.setOnClickListener(v -> {
            startActivity(new Intent(this, ChildRegister.class));
        });

        btnChangePassword.setOnClickListener(v -> {
            showChangePasswordDialog();
        });

        btnDeleteAccount.setOnClickListener(v -> {
            showDeleteAccountConfirmation();
        });

        btnAutoLock.setOnClickListener(v -> {
            showAutoLockOptions();
        });

        btnClearCache.setOnClickListener(v -> {
            showClearCacheConfirmation();
        });

        btnFontSize.setOnClickListener(v -> {
            showFontSizeOptions();
        });

        btnSignOut.setOnClickListener(v -> {
            showSignOutConfirmation();
        });
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this,
                R.style.DatePickerDialogTheme);
        builder.setTitle("Change Password");
        builder.setMessage("A password reset link will be sent to your email. Continue?");

        builder.setPositiveButton("Send Link", (dialog, which) -> {
            sendPasswordResetEmail();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void sendPasswordResetEmail() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            mAuth.sendPasswordResetEmail(user.getEmail())
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this,
                                    "Password reset email sent to " + user.getEmail(),
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this,
                                    "Failed to send reset email",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void showDeleteAccountConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this,
                R.style.DatePickerDialogTheme);
        builder.setTitle("Delete Account");
        builder.setMessage("Are you sure you want to delete your account? This action cannot be undone. All your data will be permanently lost.");

        builder.setPositiveButton("Delete", (dialog, which) -> {
            deleteUserAccount();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void deleteUserAccount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // First delete all user data from database
            mDatabase.child("users").child(currentUserId).removeValue()
                    .addOnSuccessListener(aVoid -> {
                        // Then delete the user account
                        user.delete()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(this,
                                                "Account deleted successfully",
                                                Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(this, MainActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(this,
                                                "Failed to delete account",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this,
                                "Failed to delete user data: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void showAutoLockOptions() {
        String[] options = {"Immediately", "30 seconds", "1 minute", "5 minutes", "10 minutes", "Never"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Auto-Lock Timer");
        builder.setItems(options, (dialog, which) -> {
            String selectedOption = options[which];
            tvAutoLock.setText(selectedOption);
            saveSetting("auto_lock", selectedOption);

            // Save the actual timeout value in seconds
            int timeoutSeconds = getAutoLockTimeout(selectedOption);
            saveSetting("auto_lock_timeout", timeoutSeconds);

            Toast.makeText(this, "Auto-lock set to " + selectedOption, Toast.LENGTH_SHORT).show();
        });
        builder.show();
    }

    private int getAutoLockTimeout(String option) {
        switch (option) {
            case "Immediately": return 0;
            case "30 seconds": return 30;
            case "1 minute": return 60;
            case "5 minutes": return 300;
            case "10 minutes": return 600;
            case "Never": return -1;
            default: return 300; // 5 minutes default
        }
    }

    private void updateCacheSize() {
        try {
            // Calculate actual cache size
            long cacheSize = getCacheSize();
            String sizeText;

            if (cacheSize < 1024) {
                sizeText = cacheSize + " B";
            } else if (cacheSize < 1024 * 1024) {
                sizeText = String.format("%.1f KB", cacheSize / 1024.0);
            } else {
                sizeText = String.format("%.1f MB", cacheSize / (1024.0 * 1024.0));
            }

            tvCacheSize.setText(sizeText);
        } catch (Exception e) {
            tvCacheSize.setText("Unknown");
        }
    }

    private long getCacheSize() {
        long size = 0;
        try {
            // Get internal cache directory size
            File cacheDir = getCacheDir();
            size += getFolderSize(cacheDir);

            // Get external cache directory size (if available)
            File externalCacheDir = getExternalCacheDir();
            if (externalCacheDir != null) {
                size += getFolderSize(externalCacheDir);
            }

            // Add SharedPreferences file size
            File sharedPrefsFile = new File(getFilesDir().getParent() + "/shared_prefs/AppSettings.xml");
            if (sharedPrefsFile.exists()) {
                size += sharedPrefsFile.length();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return size;
    }

    private long getFolderSize(File directory) {
        long size = 0;
        if (directory != null && directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        size += file.length();
                    } else if (file.isDirectory()) {
                        size += getFolderSize(file);
                    }
                }
            }
        }
        return size;
    }

    private void showClearCacheConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this,
                R.style.DatePickerDialogTheme);
        builder.setTitle("Clear Cache");
        builder.setMessage("Clear app cache? This will free up storage space.");

        builder.setPositiveButton("Clear", (dialog, which) -> {
            clearAppCache();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void clearAppCache() {
        try {
            // Clear internal cache
            File cacheDir = getCacheDir();
            deleteRecursive(cacheDir);
            cacheDir.mkdirs(); // Recreate directory

            // Clear external cache
            File externalCacheDir = getExternalCacheDir();
            if (externalCacheDir != null) {
                deleteRecursive(externalCacheDir);
                externalCacheDir.mkdirs();
            }

            // Clear WebView cache (if using WebView)
            deleteRecursive(new File(getCacheDir().getParent() + "/app_webview"));

            // Update cache size display
            updateCacheSize();

            Toast.makeText(this, "Cache cleared successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to clear cache: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] files = fileOrDirectory.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursive(child);
                }
            }
        }
        if (!fileOrDirectory.getName().equals("lib")) { // Don't delete lib directory
            fileOrDirectory.delete();
        }
    }

    private void showFontSizeOptions() {
        String[] fontSizes = {"Small", "Medium", "Large", "Extra Large"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Font Size");
        builder.setItems(fontSizes, (dialog, which) -> {
            String selectedSize = fontSizes[which];
            tvFontSize.setText(selectedSize);
            saveSetting("font_size", selectedSize);

            // Apply font size immediately
            applyFontSize(selectedSize);

            Toast.makeText(this, "Font size changed to " + selectedSize, Toast.LENGTH_SHORT).show();
        });
        builder.show();
    }

    private void applyFontSize(String fontSize) {
        float scaleFactor = getFontScaleFactor(fontSize);

        // Apply to all TextViews in the activity
        applyFontSizeToViewGroup((android.view.ViewGroup) getWindow().getDecorView().getRootView(), scaleFactor);
    }

    private float getFontScaleFactor(String fontSize) {
        switch (fontSize) {
            case "Small": return 0.75f;
            case "Medium": return 1.0f;
            case "Large": return 1.25f;
            case "Extra Large": return 1.3f;
            default: return 1.0f;
        }
    }

    private void applyFontSizeToViewGroup(android.view.ViewGroup viewGroup, float scaleFactor) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            android.view.View view = viewGroup.getChildAt(i);

            if (view instanceof TextView) {
                TextView textView = (TextView) view;
                float originalSize = textView.getTextSize();
                textView.setTextSize(originalSize * scaleFactor / getResources().getDisplayMetrics().scaledDensity);
            } else if (view instanceof android.view.ViewGroup) {
                applyFontSizeToViewGroup((android.view.ViewGroup) view, scaleFactor);
            }
        }
    }

    private void saveSetting(String key, boolean value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    private void saveSetting(String key, String value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    private void saveSetting(String key, int value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    private void showSignOutConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this,
                R.style.DatePickerDialogTheme);
        builder.setTitle("Sign Out");
        builder.setMessage("Are you sure you want to sign out?");

        builder.setPositiveButton("Sign Out", (dialog, which) -> {
            signOutUser();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void signOutUser() {
        mAuth.signOut();
        Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChildren(); // Refresh children list
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    // Child item class
    private static class ChildItem {
        private String id;
        private String name;
        private String dob;
        private String gender;

        public ChildItem(String id, String name, String dob, String gender) {
            this.id = id;
            this.name = name;
            this.dob = dob;
            this.gender = gender;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getDob() { return dob; }
        public String getGender() { return gender; }
    }
}