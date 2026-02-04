package com.example.afyatrack;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class ANCProfileActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FirebaseUser currentUser;

    // UI Components
    private TextView toolbarTitle, tvMotherName, tvMotherAge, tvBloodGroup, tvWeeksBadge;
    private TextView tvLMPDate, tvEDD, tvWeeksPregnant, tvTrimester;
    private TextView tvGravida, tvParity;
    private TextView tvPhoneNumber, tvEmergencyContact, tvAddress, tvLastUpdated;
    private ImageView motherAvatar;
    private BottomNavigationView bottomNavigation;
    private ProgressBar loadingProgress;
    private ImageButton btnHome, btnSettings, btnHelp;
    private FloatingActionButton fabEdit;
    private com.google.android.material.button.MaterialButton btnSave;
    private CardView basicInfoCard, pregnancyInfoCard, additionalInfoCard;
    private LinearLayout profileContainer;

    // Edit mode components
    private EditText etMotherName, etMotherAge, etBloodGroup, etPhoneNumber, etEmergencyContact, etAddress;
    private EditText etLMPDate, etEDD, etGravida, etParity;
    private boolean isEditMode = false;
    private HashMap<String, String> originalValues = new HashMap<>();
    private HashMap<String, String> currentValues = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ancprofile);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUser = mAuth.getCurrentUser();

        // Initialize UI Components
        initializeViews();
        setupBottomNavigation();
        setupUI();
        setupTopNavigation();

        // Load profile data
        loadProfileData();
    }

    private void initializeViews() {
        toolbarTitle = findViewById(R.id.toolbar_title);
        motherAvatar = findViewById(R.id.motherAvatar);
        loadingProgress = findViewById(R.id.loadingProgress);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        profileContainer = findViewById(R.id.profileContainer);
        fabEdit = findViewById(R.id.fab_edit);
        btnSave = findViewById(R.id.btn_save);

        // Basic Info TextViews
        tvMotherName = findViewById(R.id.tvMotherName);
        tvMotherAge = findViewById(R.id.tvMotherAge);
        tvBloodGroup = findViewById(R.id.tvBloodGroup);
        tvPhoneNumber = findViewById(R.id.tvPhoneNumber);
        tvEmergencyContact = findViewById(R.id.tvEmergencyContact);
        tvAddress = findViewById(R.id.tvAddress);
        tvWeeksBadge = findViewById(R.id.tvWeeksBadge);

        // Pregnancy Info TextViews
        tvLMPDate = findViewById(R.id.tvLMPDate);
        tvEDD = findViewById(R.id.tvEDD);
        tvTrimester = findViewById(R.id.tvTrimester);
        tvGravida = findViewById(R.id.tvGravida);
        tvParity = findViewById(R.id.tvParity);

        // Additional Info
        tvLastUpdated = findViewById(R.id.tvLastUpdated);

        // Top Navigation Buttons
        btnHome = findViewById(R.id.btn_home);
        btnSettings = findViewById(R.id.btn_settings);
        btnHelp = findViewById(R.id.btn_help);

        // Cards
        basicInfoCard = findViewById(R.id.basicInfoCard);
        pregnancyInfoCard = findViewById(R.id.pregnancyInfoCard);

    }

    private void setupTopNavigation() {
        btnHome.setOnClickListener(v -> {
            startActivity(new Intent(this, Welcome.class));
            finish();
        });

        btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });

        btnHelp.setOnClickListener(v -> {
            startActivity(new Intent(this, HelpActivity.class));
        });
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_digital_records) {
                    startActivity(new Intent(ANCProfileActivity.this, ANCRecordsActivity.class));
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    return true;
                } else if (itemId == R.id.nav_appointment) {
                    startActivity(new Intent(ANCProfileActivity.this, ANCAppointmentsActivity.class));
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    return true;
                } else if (itemId == R.id.nav_dashboard) {
                    startActivity(new Intent(ANCProfileActivity.this, AntenatalActivity.class));
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    return true;
                } else if (itemId == R.id.nav_notifications) {
                    startActivity(new Intent(ANCProfileActivity.this, ANCNotificationsActivity.class));
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    return true;
                } else if (itemId == R.id.nav_child_profile) {
                    // Already on profile
                    return true;
                }
                return false;
            }
        });

        bottomNavigation.setSelectedItemId(R.id.nav_child_profile);
    }

    private void setupUI() {
        toolbarTitle.setText("Mother's Profile");

        fabEdit.setOnClickListener(v -> toggleEditMode());

        btnSave.setOnClickListener(v -> saveProfileChanges());

        // Initialize EditTexts (they'll be created dynamically when entering edit mode)
    }

    private void loadProfileData() {
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadingProgress.setVisibility(View.VISIBLE);

        mDatabase.child("users").child(currentUser.getUid()).child("anc_registration")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        loadingProgress.setVisibility(View.GONE);
                        profileContainer.setVisibility(View.VISIBLE);

                        if (dataSnapshot.exists()) {
                            // Extract all data from ANC registration
                            String name = dataSnapshot.child("name").getValue(String.class);
                            String age = dataSnapshot.child("age").getValue(String.class);
                            String bloodGroup = dataSnapshot.child("bloodGroup").getValue(String.class);
                            String lmpDate = dataSnapshot.child("LmpDate").getValue(String.class);
                            String edd = dataSnapshot.child("expectedDueDate").getValue(String.class);
                            String weeksPregnant = dataSnapshot.child("weeksPregnant").getValue(String.class);
                            String gravida = dataSnapshot.child("gravida").getValue(String.class);
                            String parity = dataSnapshot.child("parity").getValue(String.class);
                            String phoneNumber = dataSnapshot.child("phoneNumber").getValue(String.class);
                            String emergencyContact = dataSnapshot.child("emergencyContact").getValue(String.class);
                            String address = dataSnapshot.child("address").getValue(String.class);

                            // Calculate weeks from LMP if not stored
                            int weeks = 0;
                            if (weeksPregnant == null || weeksPregnant.equals("0")) {
                                weeks = calculateWeeksFromLMP(lmpDate);
                            } else {
                                try {
                                    weeks = Integer.parseInt(weeksPregnant);
                                } catch (NumberFormatException e) {
                                    weeks = calculateWeeksFromLMP(lmpDate);
                                }
                            }

                            String trimester = getTrimester(weeks);

                            // Store original values
                            originalValues.put("name", name != null ? name : "");
                            originalValues.put("age", age != null ? age : "");
                            originalValues.put("bloodGroup", bloodGroup != null ? bloodGroup : "");
                            originalValues.put("LmpDate", lmpDate != null ? lmpDate : "");
                            originalValues.put("expectedDueDate", edd != null ? edd : "");
                            originalValues.put("gravida", gravida != null ? gravida : "");
                            originalValues.put("parity", parity != null ? parity : "");
                            originalValues.put("phoneNumber", phoneNumber != null ? phoneNumber : "");
                            originalValues.put("emergencyContact", emergencyContact != null ? emergencyContact : "");
                            originalValues.put("address", address != null ? address : "");

                            // Update Basic Info
                            tvMotherName.setText(name != null ? name : "Not specified");
                            tvMotherAge.setText(age != null ? age : "Not specified");
                            tvBloodGroup.setText(bloodGroup != null && !bloodGroup.isEmpty() ? bloodGroup : "Not specified");
                            tvPhoneNumber.setText(phoneNumber != null && !phoneNumber.isEmpty() ? phoneNumber : "Not specified");
                            tvEmergencyContact.setText(emergencyContact != null && !emergencyContact.isEmpty() ? emergencyContact : "Not specified");
                            tvAddress.setText(address != null && !address.isEmpty() ? address : "Not specified");
                            tvWeeksBadge.setText(weeks + " Weeks Pregnant");

                            // Update Pregnancy Info
                            tvLMPDate.setText(lmpDate != null ? lmpDate : "Not specified");
                            tvEDD.setText(edd != null ? edd : "Not specified");
                            tvTrimester.setText(trimester);
                            tvGravida.setText(gravida != null ? gravida : "Not specified");
                            tvParity.setText(parity != null ? parity : "Not specified");

                            // Update Last Updated
                            String lastUpdated = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
                            tvLastUpdated.setText(lastUpdated);

                            // Load additional profile data
                            loadAdditionalProfileData();
                        } else {
                            Toast.makeText(ANCProfileActivity.this,
                                    "No ANC registration found. Please register first.",
                                    Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        loadingProgress.setVisibility(View.GONE);
                        Toast.makeText(ANCProfileActivity.this,
                                "Error loading profile: " + databaseError.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadAdditionalProfileData() {
        if (currentUser.getPhotoUrl() != null) {
            motherAvatar.setImageResource(R.drawable.girlavatar);
        }
    }

    private void toggleEditMode() {
        isEditMode = !isEditMode;

        if (isEditMode) {
            // Switch to edit mode
            enterEditMode();
        } else {
            // Switch back to view mode without saving
            exitEditMode(false);
        }
    }

    private void enterEditMode() {
        // Change FAB icon to cancel
        fabEdit.setImageResource(R.drawable.baseline_cancel_24);

        // Show save button
        btnSave.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false); // Initially disabled until changes are made

        // Replace TextViews with EditTexts for editable fields
        replaceTextViewsWithEditTexts();

        // Store current values
        storeCurrentValues();
    }

    private void exitEditMode(boolean saveChanges) {
        // Reset FAB icon to edit
        fabEdit.setImageResource(R.drawable.baseline_edit_24);

        // Hide save button
        btnSave.setVisibility(View.GONE);

        if (saveChanges) {
            // Save changes were made, reload data
            loadProfileData();
        } else {
            // No changes or cancelled, restore original view
            replaceEditTextsWithTextViews();
        }

        isEditMode = false;
    }

    private void replaceTextViewsWithEditTexts() {
        // Basic Info Card
        replaceTextViewWithEditText(basicInfoCard, tvMotherName, "name");
        replaceTextViewWithEditText(basicInfoCard, tvMotherAge, "age");
        replaceTextViewWithEditText(basicInfoCard, tvBloodGroup, "bloodGroup");
        replaceTextViewWithEditText(basicInfoCard, tvPhoneNumber, "phoneNumber");
        replaceTextViewWithEditText(basicInfoCard, tvEmergencyContact, "emergencyContact");
        replaceTextViewWithEditText(basicInfoCard, tvAddress, "address");

        // Pregnancy Info Card
        replaceTextViewWithEditText(pregnancyInfoCard, tvLMPDate, "LmpDate");
        replaceTextViewWithEditText(pregnancyInfoCard, tvEDD, "expectedDueDate");
        replaceTextViewWithEditText(pregnancyInfoCard, tvGravida, "gravida");
        replaceTextViewWithEditText(pregnancyInfoCard, tvParity, "parity");
    }

    private void replaceTextViewWithEditText(CardView card, TextView textView, String fieldKey) {
        // Get the parent layout
        LinearLayout parentLayout = (LinearLayout) textView.getParent();
        int index = parentLayout.indexOfChild(textView);

        // Create EditText with same properties
        EditText editText = new EditText(this);
        editText.setLayoutParams(textView.getLayoutParams());
        editText.setText(textView.getText().toString().equals("Not specified") ? "" : textView.getText().toString());
        editText.setTextColor(textView.getTextColors());
        editText.setTextSize(14);
        editText.setTypeface(textView.getTypeface());
        editText.setHint("Enter " + getFieldLabel(fieldKey));
        editText.setTag(fieldKey);

        // Add text change listener
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkForChanges();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Replace TextView with EditText
        parentLayout.removeViewAt(index);
        parentLayout.addView(editText, index);

        // Store reference
        switch (fieldKey) {
            case "name": etMotherName = editText; break;
            case "age": etMotherAge = editText; break;
            case "bloodGroup": etBloodGroup = editText; break;
            case "phoneNumber": etPhoneNumber = editText; break;
            case "emergencyContact": etEmergencyContact = editText; break;
            case "address": etAddress = editText; break;
            case "LmpDate": etLMPDate = editText; break;
            case "expectedDueDate": etEDD = editText; break;
            case "gravida": etGravida = editText; break;
            case "parity": etParity = editText; break;
        }
    }

    private void replaceEditTextsWithTextViews() {
        // Basic Info Card
        replaceEditTextWithTextView(basicInfoCard, etMotherName, tvMotherName);
        replaceEditTextWithTextView(basicInfoCard, etMotherAge, tvMotherAge);
        replaceEditTextWithTextView(basicInfoCard, etBloodGroup, tvBloodGroup);
        replaceEditTextWithTextView(basicInfoCard, etPhoneNumber, tvPhoneNumber);
        replaceEditTextWithTextView(basicInfoCard, etEmergencyContact, tvEmergencyContact);
        replaceEditTextWithTextView(basicInfoCard, etAddress, tvAddress);

        // Pregnancy Info Card
        replaceEditTextWithTextView(pregnancyInfoCard, etLMPDate, tvLMPDate);
        replaceEditTextWithTextView(pregnancyInfoCard, etEDD, tvEDD);
        replaceEditTextWithTextView(pregnancyInfoCard, etGravida, tvGravida);
        replaceEditTextWithTextView(pregnancyInfoCard, etParity, tvParity);

        // Clear references
        etMotherName = etMotherAge = etBloodGroup = etPhoneNumber = etEmergencyContact = etAddress = null;
        etLMPDate = etEDD = etGravida = etParity = null;
    }

    private void replaceEditTextWithTextView(CardView card, EditText editText, TextView textView) {
        if (editText == null) return;

        LinearLayout parentLayout = (LinearLayout) editText.getParent();
        int index = parentLayout.indexOfChild(editText);

        // Restore original text value
        String fieldKey = (String) editText.getTag();
        String originalValue = originalValues.get(fieldKey);
        textView.setText(originalValue != null && !originalValue.isEmpty() ? originalValue : "Not specified");

        // Replace EditText with TextView
        parentLayout.removeViewAt(index);
        parentLayout.addView(textView, index);
    }

    private void storeCurrentValues() {
        currentValues.clear();

        // Store values from EditTexts if they exist
        if (etMotherName != null) currentValues.put("name", etMotherName.getText().toString().trim());
        if (etMotherAge != null) currentValues.put("age", etMotherAge.getText().toString().trim());
        if (etBloodGroup != null) currentValues.put("bloodGroup", etBloodGroup.getText().toString().trim());
        if (etPhoneNumber != null) currentValues.put("phoneNumber", etPhoneNumber.getText().toString().trim());
        if (etEmergencyContact != null) currentValues.put("emergencyContact", etEmergencyContact.getText().toString().trim());
        if (etAddress != null) currentValues.put("address", etAddress.getText().toString().trim());
        if (etLMPDate != null) currentValues.put("LmpDate", etLMPDate.getText().toString().trim());
        if (etEDD != null) currentValues.put("expectedDueDate", etEDD.getText().toString().trim());
        if (etGravida != null) currentValues.put("gravida", etGravida.getText().toString().trim());
        if (etParity != null) currentValues.put("parity", etParity.getText().toString().trim());
    }

    private void checkForChanges() {
        storeCurrentValues();

        boolean hasChanges = false;
        for (String key : originalValues.keySet()) {
            String original = originalValues.get(key);
            String current = currentValues.get(key);

            if (current != null && !current.equals(original)) {
                hasChanges = true;
                break;
            }
        }

        btnSave.setEnabled(hasChanges);
        btnSave.setAlpha(hasChanges ? 1.0f : 0.5f);
    }

    private void saveProfileChanges() {
        if (currentUser == null) return;

        loadingProgress.setVisibility(View.VISIBLE);

        // Prepare update data
        HashMap<String, Object> updates = new HashMap<>();
        for (String key : currentValues.keySet()) {
            String value = currentValues.get(key);
            if (value != null && !value.isEmpty() && !value.equals(originalValues.get(key))) {
                updates.put(key, value);
            }
        }

        if (updates.isEmpty()) {
            loadingProgress.setVisibility(View.GONE);
            Toast.makeText(this, "No changes to save", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add update timestamp
        String currentDate = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        updates.put("lastUpdated", currentDate);

        // Update in Firebase
        mDatabase.child("users").child(currentUser.getUid()).child("anc_registration")
                .updateChildren(updates)
                .addOnCompleteListener(task -> {
                    loadingProgress.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        // Update original values
                        for (String key : updates.keySet()) {
                            if (!key.equals("lastUpdated")) {
                                originalValues.put(key, currentValues.get(key));
                            }
                        }

                        Toast.makeText(ANCProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                        Snackbar.make(findViewById(android.R.id.content), "Profile saved", Snackbar.LENGTH_SHORT).show();

                        // Exit edit mode and refresh
                        exitEditMode(true);
                    } else {
                        Toast.makeText(ANCProfileActivity.this, "Failed to save changes: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String getFieldLabel(String fieldKey) {
        switch (fieldKey) {
            case "name": return "Name";
            case "age": return "Age";
            case "bloodGroup": return "Blood Group";
            case "phoneNumber": return "Phone Number";
            case "emergencyContact": return "Emergency Contact";
            case "address": return "Address";
            case "LmpDate": return "Last Menstrual Period (dd/mm/yyyy)";
            case "expectedDueDate": return "Expected Due Date (dd/mm/yyyy)";
            case "gravida": return "Gravida";
            case "parity": return "Parity";
            default: return fieldKey;
        }
    }

    private int calculateWeeksFromLMP(String lmpDate) {
        if (lmpDate == null || lmpDate.isEmpty()) return 0;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Calendar lmp = Calendar.getInstance();
            lmp.setTime(sdf.parse(lmpDate));

            Calendar today = Calendar.getInstance();
            long diff = today.getTimeInMillis() - lmp.getTimeInMillis();
            long days = diff / (24 * 60 * 60 * 1000);
            return (int) (days / 7);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private String getTrimester(int weeks) {
        if (weeks <= 12) return "First Trimester";
        else if (weeks <= 27) return "Second Trimester";
        else return "Third Trimester";
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isEditMode) {
            loadProfileData();
        }
    }
}