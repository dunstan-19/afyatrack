// AntenatalRegister.java
package com.example.afyatrack;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.widget.ScrollView;
import android.widget.LinearLayout;

public class AntenatalRegister extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FirebaseUser currentUser;

    // UI Components
    private TextInputEditText edtLMP, edtEDD, edtBloodGroup, etName, etAge;
    private TextInputEditText edtPrevComplications, edtPhoneNumber, edtEmergencyContact, edtLocation;
    private Button btnCalculateEDD, btnRegister, btnCancel;
    private ProgressBar progressBar, pregnancyProgress;
    private ScrollView scrollView;
    private LinearLayout formContainer;
    private TextView tvWeeksPregnant, tvTrimester;
    private TextInputLayout tilName, tilAge, tilLMP, tilBloodGroup, tilPhone, tilEmergency, tilLocation;

    private Calendar calendar = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    // Validation patterns
    private static final Pattern BLOOD_GROUP_PATTERN = Pattern.compile("^(A|B|AB|O)[+-]$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]?[0-9]{10,15}$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_antenatal_register);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUser = mAuth.getCurrentUser();

        // Initialize UI Components
        initializeViews();
        setupUI();
        setupDatePickers();
        setupRealTimeWeekCalculator();
    }

    private void initializeViews() {
        edtLMP = findViewById(R.id.edtLMP);
        edtEDD = findViewById(R.id.edtEDD);
        edtBloodGroup = findViewById(R.id.edtBloodGroup);
        edtPrevComplications = findViewById(R.id.edtPrevComplications);
        etName = findViewById(R.id.etName);
        etAge = findViewById(R.id.etAge);
        edtPhoneNumber = findViewById(R.id.edtPhoneNumber);
        edtEmergencyContact = findViewById(R.id.edtEmergencyContact);
        edtLocation = findViewById(R.id.edtLocation);

        btnCalculateEDD = findViewById(R.id.btnCalculateEDD);
        btnRegister = findViewById(R.id.btnRegister);
        btnCancel = findViewById(R.id.btnCancel);

        progressBar = findViewById(R.id.progressBar);
        pregnancyProgress = findViewById(R.id.pregnancyProgress);
        scrollView = findViewById(R.id.scrollView);
        formContainer = findViewById(R.id.formContainer);
        tvWeeksPregnant = findViewById(R.id.tvWeeksPregnant);
        tvTrimester = findViewById(R.id.tvTrimester);

        // TextInputLayouts for validation styling
        tilName = findViewById(R.id.tilName);
        tilAge = findViewById(R.id.tilAge);
        tilLMP = findViewById(R.id.tilLMP);
        tilBloodGroup = findViewById(R.id.tilBloodGroup);
        tilPhone = findViewById(R.id.tilPhone);
        tilEmergency = findViewById(R.id.tilEmergency);
        tilLocation = findViewById(R.id.tilLocation);

        // Style buttons
        styleButton(btnCalculateEDD, Color.parseColor("#2196F3"), Color.WHITE); // Blue color
        styleButton(btnRegister, Color.parseColor("#4CAF50"), Color.WHITE); // Green color
        styleButton(btnCancel, Color.parseColor("#F44336"), Color.WHITE); // Red color

        // Style progress bar
        pregnancyProgress.getProgressDrawable().setColorFilter(
                Color.parseColor("#2196F3"),
                android.graphics.PorterDuff.Mode.SRC_IN
        );
    }

    private void setupUI() {
        // Set click listeners
        btnCalculateEDD.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateEDDAndWeeks();
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateAndRegister();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            }
        });

        // Add text change listeners for real-time validation
        addValidationListeners();
    }

    private void addValidationListeners() {
        // Name validation
        etName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    tilName.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Age validation
        etAge.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    validateAge(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Blood group validation
        edtBloodGroup.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    validateBloodGroup(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Phone validation
        edtPhoneNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    validatePhoneNumber(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Emergency contact validation
        edtEmergencyContact.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    validateEmergencyContact(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // LMP date listener
        edtLMP.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!TextUtils.isEmpty(s.toString())) {
                    calculateEDDAndWeeks();
                    validateLMPDate(s.toString());
                }
            }
        });
    }

    private void setupDatePickers() {
        // LMP Date Picker with custom theme
        edtLMP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker(edtLMP, true);
            }
        });

        // EDD Date Picker with custom theme
        edtEDD.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker(edtEDD, false);
            }
        });
    }

    private void showDatePicker(final TextInputEditText editText, boolean isLMP) {
        final Calendar currentDate = Calendar.getInstance();
        int year = currentDate.get(Calendar.YEAR);
        int month = currentDate.get(Calendar.MONTH);
        int day = currentDate.get(Calendar.DAY_OF_MONTH);

        // Create DatePickerDialog with custom theme
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                R.style.DatePickerDialogTheme, // Custom theme for button colors
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(year, month, dayOfMonth);
                        editText.setText(dateFormat.format(selectedDate.getTime()));

                        if (editText == edtLMP) {
                            calculateEDDAndWeeks();
                            validateLMPDate(editText.getText().toString());
                        }
                    }
                }, year, month, day);

        // Set date constraints
        if (isLMP) {
            // For LMP: cannot be in the future and cannot be beyond 9 months ago
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            Calendar minDate = Calendar.getInstance();
            minDate.add(Calendar.MONTH, -9);
            datePickerDialog.getDatePicker().setMinDate(minDate.getTimeInMillis());
        } else {
            // For EDD: cannot be in the past
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        }

        datePickerDialog.show();
    }

    private void setupRealTimeWeekCalculator() {
        // Update weeks every minute (you can adjust this interval)
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // Start a thread to update weeks (simplified approach)

        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000); // Check every minute
                    runOnUiThread(() -> {
                        if (!TextUtils.isEmpty(edtLMP.getText().toString())) {
                            calculateEDDAndWeeks();
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void calculateEDDAndWeeks() {
        String lmpString = edtLMP.getText().toString().trim();

        if (TextUtils.isEmpty(lmpString)) {
            tvWeeksPregnant.setText("Please enter LMP first");
            return;
        }

        try {
            Calendar lmpDate = Calendar.getInstance();
            lmpDate.setTime(dateFormat.parse(lmpString));

            // Calculate EDD (LMP + 280 days)
            Calendar eddDate = (Calendar) lmpDate.clone();
            eddDate.add(Calendar.DAY_OF_YEAR, 280);
            edtEDD.setText(dateFormat.format(eddDate.getTime()));

            // Calculate weeks pregnant with decimal precision
            Calendar today = Calendar.getInstance();
            long diff = today.getTimeInMillis() - lmpDate.getTimeInMillis();
            long days = diff / (24 * 60 * 60 * 1000);
            int weeks = (int) (days / 7);
            int remainingDays = (int) (days % 7);

            // Calculate progress (0-40 weeks)
            int totalWeeks = 40;
            int progress = (weeks * 100) / totalWeeks;
            if (progress < 0) progress = 0;
            if (progress > 100) progress = 100;

            if (weeks >= 0 && weeks <= 42) {
                String weekText = "You are " + weeks + " weeks";
                if (remainingDays > 0) {
                    weekText += " " + remainingDays + " days";
                }
                weekText += " pregnant";

                tvWeeksPregnant.setText(weekText);
                tvWeeksPregnant.setTextColor(Color.parseColor("#4CAF50"));

                // Update progress bar
                pregnancyProgress.setProgress(progress);

                // Show trimester info
                String trimester = getTrimester(weeks);
                tvTrimester.setText(trimester);
                tvTrimester.setVisibility(View.VISIBLE);
            } else {
                tvWeeksPregnant.setText("Please check your LMP date");
                tvWeeksPregnant.setTextColor(Color.parseColor("#F44336"));
                tvTrimester.setVisibility(View.GONE);
            }

        } catch (Exception e) {
            e.printStackTrace();
            tvWeeksPregnant.setText("Invalid date format");
            tvWeeksPregnant.setTextColor(Color.parseColor("#F44336"));
        }
    }

    // Validation methods
    private boolean validateAge(String ageStr) {
        try {
            int age = Integer.parseInt(ageStr);
            if (age < 12) {
                tilAge.setError("Minimum age for pregnancy is 12 years");
                return false;
            } else if (age > 55) {
                tilAge.setError("Maximum age for pregnancy is 55 years");
                return false;
            }
            tilAge.setError(null);
            return true;
        } catch (NumberFormatException e) {
            tilAge.setError("Please enter a valid age");
            return false;
        }
    }

    private boolean validateBloodGroup(String bloodGroup) {
        if (!BLOOD_GROUP_PATTERN.matcher(bloodGroup.trim()).matches()) {
            tilBloodGroup.setError("Invalid blood group. Use format: A+, B-, AB+, O-");
            return false;
        }
        tilBloodGroup.setError(null);
        return true;
    }

    private boolean validatePhoneNumber(String phone) {
        if (!PHONE_PATTERN.matcher(phone.trim()).matches()) {
            tilPhone.setError("Invalid phone number format");
            return false;
        }
        tilPhone.setError(null);
        return true;
    }

    private boolean validateEmergencyContact(String emergencyContact) {
        if (!PHONE_PATTERN.matcher(emergencyContact.trim()).matches()) {
            tilEmergency.setError("Invalid phone number format");
            return false;
        }
        tilEmergency.setError(null);
        return true;
    }

    private boolean validateLMPDate(String lmpDate) {
        try {
            Calendar lmp = Calendar.getInstance();
            lmp.setTime(dateFormat.parse(lmpDate));
            Calendar today = Calendar.getInstance();

            // LMP cannot be in the future
            if (lmp.after(today)) {
                tilLMP.setError("LMP date cannot be in the future");
                return false;
            }

            // LMP cannot be more than 9 months ago
            Calendar nineMonthsAgo = Calendar.getInstance();
            nineMonthsAgo.add(Calendar.MONTH, -9);

            if (lmp.before(nineMonthsAgo)) {
                tilLMP.setError("LMP date cannot be more than 9 months ago");
                return false;
            }

            tilLMP.setError(null);
            return true;
        } catch (Exception e) {
            tilLMP.setError("Invalid date format");
            return false;
        }
    }

    private String getTrimester(int weeks) {
        if (weeks <= 12) return "First Trimester";
        else if (weeks <= 27) return "Second Trimester";
        else return "Third Trimester";
    }

    private void validateAndRegister() {
        // Get all values
        String name = etName.getText().toString().trim();
        String age = etAge.getText().toString().trim();
        String lmp = edtLMP.getText().toString().trim();
        String edd = edtEDD.getText().toString().trim();
        String bloodGroup = edtBloodGroup.getText().toString().trim();
        String prevComplications = edtPrevComplications.getText().toString().trim();
        String phoneNumber = edtPhoneNumber.getText().toString().trim();
        String emergencyContact = edtEmergencyContact.getText().toString().trim();
        String location = edtLocation.getText().toString().trim();

        // Validate all fields
        boolean isValid = true;

        if (TextUtils.isEmpty(name)) {
            tilName.setError("Name is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(age) || !validateAge(age)) {
            isValid = false;
        }

        if (TextUtils.isEmpty(lmp) || !validateLMPDate(lmp)) {
            isValid = false;
        }

        if (TextUtils.isEmpty(bloodGroup) || !validateBloodGroup(bloodGroup)) {
            isValid = false;
        }

        if (TextUtils.isEmpty(phoneNumber) || !validatePhoneNumber(phoneNumber)) {
            isValid = false;
        }

        if (TextUtils.isEmpty(emergencyContact) || !validateEmergencyContact(emergencyContact)) {
            isValid = false;
        }

        if (TextUtils.isEmpty(location)) {
            tilLocation.setError("Location is required");
            isValid = false;
        }

        if (!isValid) {
            Toast.makeText(this, "Please fix all validation errors", Toast.LENGTH_SHORT).show();
            return;
        }

        // Calculate weeks pregnant
        int weeksPregnant = 0;
        try {
            Calendar lmpDate = Calendar.getInstance();
            lmpDate.setTime(dateFormat.parse(lmp));
            Calendar today = Calendar.getInstance();
            long diff = today.getTimeInMillis() - lmpDate.getTimeInMillis();
            long days = diff / (24 * 60 * 60 * 1000);
            weeksPregnant = (int) (days / 7);
        } catch (Exception e) {
            weeksPregnant = 0;
        }

        // Prepare data for Firebase
        Map<String, Object> ancData = new HashMap<>();
        ancData.put("name", name);
        ancData.put("age", age);
        ancData.put("LmpDate", lmp);
        ancData.put("expectedDueDate", edd);
        ancData.put("weeksPregnant", String.valueOf(weeksPregnant));
        ancData.put("bloodGroup", bloodGroup.toUpperCase()); // Store in uppercase
        ancData.put("previousComplications", prevComplications.isEmpty() ? "None reported" : prevComplications);
        ancData.put("phoneNumber", phoneNumber);
        ancData.put("emergencyContact", emergencyContact);
        ancData.put("address", location);
        ancData.put("registrationDate", System.currentTimeMillis());
        ancData.put("lastVisitDate", "No visits yet");
        ancData.put("nextVisitDate", calculateNextVisitDate());

        // Additional optional fields with defaults
        ancData.put("gravida", "1"); // Assuming first pregnancy
        ancData.put("parity", "0"); // Assuming no previous live births
        ancData.put("facilityType", "Not specified");
        ancData.put("deliveryPlan", "Hospital Delivery");
        ancData.put("allergies", "None reported");
        ancData.put("chronicConditions", "None reported");

        // Show progress bar
        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        // Save to Firebase under user's node
        if (currentUser != null) {
            mDatabase.child("users").child(currentUser.getUid()).child("anc_registration")
                    .setValue(ancData)
                    .addOnCompleteListener(task -> {
                        progressBar.setVisibility(View.GONE);
                        btnRegister.setEnabled(true);

                        if (task.isSuccessful()) {
                            // Registration successful
                            Toast.makeText(AntenatalRegister.this,
                                    "ANC Registration Successful! ",
                                    Toast.LENGTH_LONG).show();

                            // Return to dashboard
                            Intent intent = new Intent(AntenatalRegister.this, AntenatalActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                            finish();
                        } else {
                            Toast.makeText(AntenatalRegister.this,
                                    "Registration failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private String calculateNextVisitDate() {
        Calendar nextVisit = Calendar.getInstance();
        nextVisit.add(Calendar.DAY_OF_YEAR, 30); // Default next visit in 30 days
        return dateFormat.format(nextVisit.getTime());
    }

    private void styleButton(Button button, int backgroundColor, int textColor) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setCornerRadius(50);
        gradientDrawable.setColor(backgroundColor);

        button.setBackground(gradientDrawable);
        button.setTextColor(textColor);
        button.setPadding(30, 15, 30, 15);
        button.setTextSize(14);
        button.setElevation(6);

        // Add animation
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                } else if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                }
                return false;
            }
        });
    }
}