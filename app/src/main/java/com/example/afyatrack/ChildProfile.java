package com.example.afyatrack;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ChildProfile extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private String currentUserId;
    private String childId;

    // UI Elements
    private ImageView imgChildAvatar;
    private TextView tvChildName, tvAgeBadge, tvDob, tvGender, tvBirthWeight;
    private TextView tvBirthPlace, tvHealthConditions;
    private TextView tvParentName, tvPhone;
    private ImageButton btnBack, btnEditSave;
    private MaterialButton btnEditSaveProfile, btnDeleteChild, btnSignOut;

    // Edit fields
    private TextInputLayout nameInputLayout, dobInputLayout, weightInputLayout;
    private TextInputLayout birthPlaceInputLayout, healthConditionsInputLayout;
    private TextInputLayout parentNameInputLayout, phoneInputLayout;
    private TextInputEditText etChildName, etDob, etBirthWeight, etBirthPlace;
    private TextInputEditText etHealthConditions, etParentName, etPhone;
    private Spinner spinnerGender;

    private boolean isEditMode = false;
    private String originalDob;
    private Calendar calendar;
    private ArrayAdapter<CharSequence> genderAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_profile);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        currentUserId = user.getUid();
        childId = getIntent().getStringExtra("CHILD_ID");
        mDatabase = FirebaseDatabase.getInstance().getReference();
        calendar = Calendar.getInstance();

        initViews();
        setupClickListeners();
        setupGenderSpinner();
        setupDatePicker();

        if (childId != null) {
            loadChildProfileData(childId);
        } else {
            loadSelectedChild();
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        btnEditSave = findViewById(R.id.btn_edit_save);
        imgChildAvatar = findViewById(R.id.img_child_avatar);

        // TextViews
        tvChildName = findViewById(R.id.tv_child_name);
        tvAgeBadge = findViewById(R.id.tv_age_badge);
        tvDob = findViewById(R.id.tv_dob);
        tvGender = findViewById(R.id.tv_gender);
        tvBirthWeight = findViewById(R.id.tv_birth_weight);
        tvBirthPlace = findViewById(R.id.tv_birth_place);
        tvHealthConditions = findViewById(R.id.tv_health_conditions);
        tvParentName = findViewById(R.id.tv_parent_name);
        tvPhone = findViewById(R.id.tv_phone);

        // Buttons
        btnEditSaveProfile = findViewById(R.id.btn_edit_save_profile);
        btnDeleteChild = findViewById(R.id.btn_delete_child);
        btnSignOut = findViewById(R.id.btn_sign_out);

        // Input Layouts
        nameInputLayout = findViewById(R.id.name_input_layout);
        dobInputLayout = findViewById(R.id.dob_input_layout);
        weightInputLayout = findViewById(R.id.weight_input_layout);
        birthPlaceInputLayout = findViewById(R.id.birth_place_input_layout);
        healthConditionsInputLayout = findViewById(R.id.health_conditions_input_layout);
        parentNameInputLayout = findViewById(R.id.parent_name_input_layout);
        phoneInputLayout = findViewById(R.id.phone_input_layout);

        // EditTexts
        etChildName = findViewById(R.id.et_child_name);
        etDob = findViewById(R.id.et_dob);
        etBirthWeight = findViewById(R.id.et_birth_weight);
        etBirthPlace = findViewById(R.id.et_birth_place);
        etHealthConditions = findViewById(R.id.et_health_conditions);
        etParentName = findViewById(R.id.et_parent_name);
        etPhone = findViewById(R.id.et_phone);

        // Spinner
        spinnerGender = findViewById(R.id.spinner_gender);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());

        btnEditSave.setOnClickListener(v -> toggleEditMode());

        btnEditSaveProfile.setOnClickListener(v -> {
            if (isEditMode) {
                if (validateForm()) {
                    saveChanges();
                }
            } else {
                toggleEditMode();
            }
        });

        btnDeleteChild.setOnClickListener(v -> {
            if (childId != null) {
                showDeleteConfirmationDialog();
            } else {
                Toast.makeText(this, "No child selected", Toast.LENGTH_SHORT).show();
            }
        });

        btnSignOut.setOnClickListener(v -> {
            showSignOutConfirmation();
        });

        // Date picker for DOB field
        etDob.setOnClickListener(v -> showDatePicker());
    }

    private void setupGenderSpinner() {
        genderAdapter = ArrayAdapter.createFromResource(this,
                R.array.gender_array, android.R.layout.simple_spinner_item);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(genderAdapter);

        spinnerGender.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Gender selected
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void setupDatePicker() {
        etDob.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    ChildProfile.this,
                    new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                            calendar.set(Calendar.YEAR, year);
                            calendar.set(Calendar.MONTH, month);
                            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                            updateDobLabel();
                        }
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );

            // Set max date to today
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            datePickerDialog.show();
        });
    }

    private void updateDobLabel() {
        String dateFormat = "dd/MM/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat, Locale.getDefault());
        etDob.setText(sdf.format(calendar.getTime()));
    }

    private void toggleEditMode() {
        isEditMode = !isEditMode;

        if (isEditMode) {
            // Switch to edit mode
            btnEditSave.setImageResource(R.drawable.baseline_save_24);
            btnEditSave.setContentDescription("Save Changes");
            btnEditSaveProfile.setText("Save Changes");

            // Show edit fields
            showEditFields();

            // Scroll to top for better UX
            findViewById(R.id.scrollView).post(() ->
                    findViewById(R.id.scrollView).scrollTo(0, 0));

        } else {
            // Switch to view mode
            btnEditSave.setImageResource(R.drawable.baseline_edit_24);
            btnEditSave.setContentDescription("Edit Profile");
            btnEditSaveProfile.setText("Edit Profile");

            // Hide edit fields
            hideEditFields();

            // Reload data to show any saved changes
            if (childId != null) {
                loadChildProfileData(childId);
            }
        }
    }


    private void showEditFields() {
        // Hide TextViews
        tvChildName.setVisibility(View.GONE);
        tvDob.setVisibility(View.GONE);
        tvGender.setVisibility(View.GONE);
        tvBirthWeight.setVisibility(View.GONE);
        tvBirthPlace.setVisibility(View.GONE);
        tvHealthConditions.setVisibility(View.GONE);
        tvParentName.setVisibility(View.GONE);
        tvPhone.setVisibility(View.GONE);

        // Show EditTexts
        nameInputLayout.setVisibility(View.VISIBLE);
        dobInputLayout.setVisibility(View.VISIBLE);
        spinnerGender.setVisibility(View.VISIBLE);
        weightInputLayout.setVisibility(View.VISIBLE);
        birthPlaceInputLayout.setVisibility(View.VISIBLE);
        healthConditionsInputLayout.setVisibility(View.VISIBLE);
        parentNameInputLayout.setVisibility(View.VISIBLE);
        phoneInputLayout.setVisibility(View.VISIBLE);

        // Populate edit fields with current data
        etChildName.setText(tvChildName.getText().toString());
        etBirthWeight.setText(tvBirthWeight.getText().toString().replace(" kg", ""));
        etBirthPlace.setText(tvBirthPlace.getText().toString());
        etHealthConditions.setText(tvHealthConditions.getText().toString().equals("None reported") ?
                "" : tvHealthConditions.getText().toString());
        etParentName.setText(tvParentName.getText().toString());
        etPhone.setText(tvPhone.getText().toString());

        // Set gender spinner
        String currentGender = tvGender.getText().toString();
        int position = genderAdapter.getPosition(currentGender);
        if (position >= 0) {
            spinnerGender.setSelection(position);
        }

        // Set DOB in edit format
        if (originalDob != null) {
            etDob.setText(originalDob);
        }
    }

    private void hideEditFields() {
        // Show TextViews
        tvChildName.setVisibility(View.VISIBLE);
        tvDob.setVisibility(View.VISIBLE);
        tvGender.setVisibility(View.VISIBLE);
        tvBirthWeight.setVisibility(View.VISIBLE);
        tvBirthPlace.setVisibility(View.VISIBLE);
        tvHealthConditions.setVisibility(View.VISIBLE);
        tvParentName.setVisibility(View.VISIBLE);
        tvPhone.setVisibility(View.VISIBLE);

        // Hide EditTexts
        nameInputLayout.setVisibility(View.GONE);
        dobInputLayout.setVisibility(View.GONE);
        spinnerGender.setVisibility(View.GONE);
        weightInputLayout.setVisibility(View.GONE);
        birthPlaceInputLayout.setVisibility(View.GONE);
        healthConditionsInputLayout.setVisibility(View.GONE);
        parentNameInputLayout.setVisibility(View.GONE);
        phoneInputLayout.setVisibility(View.GONE);
    }

    private boolean validateForm() {
        boolean valid = true;

        // Validate name
        if (TextUtils.isEmpty(etChildName.getText().toString().trim())) {
            nameInputLayout.setError("Child name is required");
            valid = false;
        } else {
            nameInputLayout.setError(null);
        }

        // Validate date of birth
        if (TextUtils.isEmpty(etDob.getText().toString().trim())) {
            dobInputLayout.setError("Date of birth is required");
            valid = false;
        } else {
            dobInputLayout.setError(null);
        }

        // Validate birth weight
        if (TextUtils.isEmpty(etBirthWeight.getText().toString().trim())) {
            weightInputLayout.setError("Birth weight is required");
            valid = false;
        } else {
            weightInputLayout.setError(null);
        }

        // Validate parent name
        if (TextUtils.isEmpty(etParentName.getText().toString().trim())) {
            parentNameInputLayout.setError("Parent name is required");
            valid = false;
        } else {
            parentNameInputLayout.setError(null);
        }

        // Validate phone
        if (TextUtils.isEmpty(etPhone.getText().toString().trim())) {
            phoneInputLayout.setError("Contact number is required");
            valid = false;
        } else {
            phoneInputLayout.setError(null);
        }

        return valid;
    }

    private void saveChanges() {
        String name = etChildName.getText().toString().trim();
        String dob = etDob.getText().toString().trim();
        String gender = spinnerGender.getSelectedItem().toString();
        String weight = etBirthWeight.getText().toString().trim();
        String birthPlace = etBirthPlace.getText().toString().trim();
        String healthCondition = etHealthConditions.getText().toString().trim();
        String parentName = etParentName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("dateOfBirth", dob);
        updates.put("gender", gender);
        updates.put("birthWeight", weight);
        updates.put("birthPlace", birthPlace);
        updates.put("healthCondition", healthCondition.isEmpty() ? "" : healthCondition);
        updates.put("guardianName", parentName);
        updates.put("guardianContact", phone);

        mDatabase.child("users").child(currentUserId).child("children").child(childId)
                .updateChildren(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(ChildProfile.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                        toggleEditMode(); // Switch back to view mode
                    } else {
                        Toast.makeText(ChildProfile.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadSelectedChild() {
        mDatabase.child("users").child(currentUserId).child("selectedChild")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            childId = snapshot.getValue(String.class);
                            loadChildProfileData(childId);
                        } else {
                            showNoDataMessage();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ChildProfile.this, "Failed to load selected child", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadChildProfileData(String childId) {
        mDatabase.child("users").child(currentUserId).child("children").child(childId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            displayChildData(snapshot);
                        } else {
                            showNoDataMessage();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ChildProfile.this, "Failed to load profile data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void displayChildData(DataSnapshot snapshot) {
        String name = snapshot.child("name").getValue(String.class);
        String dob = snapshot.child("dateOfBirth").getValue(String.class);
        String gender = snapshot.child("gender").getValue(String.class);
        String weight = snapshot.child("birthWeight").getValue(String.class);
        String birthPlace = snapshot.child("birthPlace").getValue(String.class);
        String healthCondition = snapshot.child("healthCondition").getValue(String.class);
        String guardianName = snapshot.child("guardianName").getValue(String.class);
        String guardianContact = snapshot.child("guardianContact").getValue(String.class);

        tvChildName.setText(name != null ? name : "Not provided");
        tvGender.setText(gender != null ? gender : "Not specified");
        tvBirthWeight.setText(weight != null ? weight + " kg" : "Not recorded");
        tvBirthPlace.setText(birthPlace != null ? birthPlace : "Not specified");
        tvHealthConditions.setText(healthCondition != null && !healthCondition.isEmpty() ?
                healthCondition : "None reported");
        tvParentName.setText(guardianName != null ? guardianName : "Not provided");
        tvPhone.setText(guardianContact != null ? guardianContact : "Not provided");

        if (dob != null) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date date = inputFormat.parse(dob);
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                tvDob.setText(outputFormat.format(date));

                // Store original DOB for edit mode
                originalDob = dob;

                String age = calculateAge(dob);
                tvAgeBadge.setText(age);
            } catch (ParseException e) {
                tvDob.setText(dob);
                tvAgeBadge.setText("Age not available");
                originalDob = dob;
            }
        } else {
            tvDob.setText("Not provided");
            tvAgeBadge.setText("Age not available");
            originalDob = "";
        }

        if (gender != null) {
            imgChildAvatar.setImageResource(
                    "Male".equalsIgnoreCase(gender)
                            ? R.drawable.boyavatar
                            : R.drawable.girlavatar
            );
        } else {
            imgChildAvatar.setImageResource(R.drawable.avator);
        }
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

    private void showDatePicker() {
        if (originalDob != null && !originalDob.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date date = sdf.parse(originalDob);
                calendar.setTime(date);
            } catch (ParseException e) {
                calendar = Calendar.getInstance();
            }
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDobLabel();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void showNoDataMessage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("No Profile Data")
                .setMessage("No child profile data found. Please register your child first.")
                .setPositiveButton("Register", (dialog, which) -> {
                    startActivity(new Intent(this, ChildRegister.class));
                    finish();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    onBackPressed();
                })
                .setCancelable(false)
                .show();
    }

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Child Profile")
                .setMessage("Are you sure you want to delete this child's profile? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteChildProfile();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteChildProfile() {
        if (childId != null) {
            mDatabase.child("users").child(currentUserId).child("children").child(childId)
                    .removeValue()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Child profile deleted", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, ChildVaccineActivity.class));
                            finish();
                        } else {
                            Toast.makeText(this, "Failed to delete profile", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void showSignOutConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Sign Out", (dialog, which) -> {
                    signOutUser();
                })
                .setNegativeButton("Cancel", null)
                .show();
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
        if (childId != null && !isEditMode) {
            loadChildProfileData(childId);
        }
    }

    @Override
    public void onBackPressed() {
        if (isEditMode) {
            // Ask for confirmation before exiting edit mode
            new AlertDialog.Builder(this)
                    .setTitle("Discard Changes?")
                    .setMessage("You have unsaved changes. Are you sure you want to discard them?")
                    .setPositiveButton("Discard", (dialog, which) -> {
                        toggleEditMode();
                        super.onBackPressed();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }
}