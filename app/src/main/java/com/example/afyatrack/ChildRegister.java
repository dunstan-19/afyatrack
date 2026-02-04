package com.example.afyatrack;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class ChildRegister extends AppCompatActivity {

    private EditText etChildName, etBirthWeight, etGuardianName, etGuardianContact, etHealthCondition, etBirthPlace;
    private RadioGroup rgGender;
    private DatePicker datePicker;
    private Button btnSaveChild;

    private DatabaseReference databaseRef;
    private String userId;

    // Validation patterns
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(?:254|\\+254|0)?(7\\d{8})$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z\\s]{2,50}$");
    private static final Pattern PLACE_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s,.-]{2,100}$");

    // Date format
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_register);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        userId = user.getUid();
        databaseRef = FirebaseDatabase.getInstance().getReference();

        initViews();
        setupDatePicker();
        btnSaveChild.setOnClickListener(v -> validateAndSaveChildData());
    }

    private void initViews() {
        etChildName = findViewById(R.id.etChildName);
        etBirthWeight = findViewById(R.id.etBirthWeight);
        rgGender = findViewById(R.id.rgGender);
        datePicker = findViewById(R.id.datePicker);
        btnSaveChild = findViewById(R.id.btnSaveChild);
        etGuardianName = findViewById(R.id.etGuardianName);
        etGuardianContact = findViewById(R.id.etGuardianContact);
        etHealthCondition = findViewById(R.id.etHealthCondition);
        etBirthPlace = findViewById(R.id.etBirthPlace);
    }

    private void setupDatePicker() {
        // Set maximum date to today
        Calendar maxDate = Calendar.getInstance();
        datePicker.setMaxDate(maxDate.getTimeInMillis());

        // Set minimum date to 5 years ago (for vaccination timeline)
        Calendar minDate = Calendar.getInstance();
        minDate.add(Calendar.YEAR, -5);
        datePicker.setMinDate(minDate.getTimeInMillis());

        // Set default date to 1 year ago
        Calendar defaultDate = Calendar.getInstance();
        defaultDate.add(Calendar.YEAR, -1);
        datePicker.updateDate(defaultDate.get(Calendar.YEAR),
                defaultDate.get(Calendar.MONTH),
                defaultDate.get(Calendar.DAY_OF_MONTH));
    }

    private void validateAndSaveChildData() {
        // Clear previous errors
        clearErrors();

        // Get input values
        String name = etChildName.getText().toString().trim();
        String weight = etBirthWeight.getText().toString().trim();
        String guardianName = etGuardianName.getText().toString().trim();
        String guardianContact = etGuardianContact.getText().toString().trim();
        String healthCondition = etHealthCondition.getText().toString().trim();
        String birthPlace = etBirthPlace.getText().toString().trim();

        // Validate each field
        boolean isValid = true;

        // Validate child name
        if (!validateName(name, etChildName, "Child name")) {
            isValid = false;
        }

        // Validate birth weight
        if (!validateBirthWeight(weight)) {
            isValid = false;
        }

        // Validate guardian name
        if (!validateName(guardianName, etGuardianName, "Guardian name")) {
            isValid = false;
        }

        // Validate guardian contact
        if (!validatePhoneNumber(guardianContact)) {
            isValid = false;
        }

        // Validate health condition (optional but if provided, validate)
        if (!healthCondition.isEmpty() && healthCondition.length() < 2) {
            etHealthCondition.setError("Health condition must be at least 2 characters");
            isValid = false;
        }

        // Validate birth place
        if (!validateBirthPlace(birthPlace)) {
            isValid = false;
        }

        // Validate gender
        if (rgGender.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select child's gender", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        // Validate date of birth and calculate age
        int day = datePicker.getDayOfMonth();
        int month = datePicker.getMonth() + 1; // DatePicker month is 0-indexed
        int year = datePicker.getYear();
        String dob = String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month, year);

        if (!validateDateOfBirth(dob)) {
            isValid = false;
        }

        if (!isValid) {
            Toast.makeText(this, "Please correct the errors before saving", Toast.LENGTH_SHORT).show();
            return;
        }

        // All validations passed, save child data
        saveChildData(name, weight, guardianName, guardianContact, healthCondition, birthPlace, dob);
    }

    private boolean validateName(String name, EditText editText, String fieldName) {
        if (TextUtils.isEmpty(name)) {
            editText.setError(fieldName + " is required");
            return false;
        }

        if (!NAME_PATTERN.matcher(name).matches()) {
            editText.setError("Enter a valid " + fieldName.toLowerCase() + " (2-50 letters only)");
            return false;
        }

        return true;
    }

    private boolean validateBirthWeight(String weight) {
        if (TextUtils.isEmpty(weight)) {
            etBirthWeight.setError("Birth weight is required");
            return false;
        }

        try {
            double weightValue = Double.parseDouble(weight);

            // Validate weight range (0.5kg to 5.5kg for newborns)
            if (weightValue < 0.5 || weightValue > 10) {
                etBirthWeight.setError("Birth weight must be between 0.5kg and 10 kg");
                return false;
            }

            // Format to 1 decimal place
            String formattedWeight = String.format(Locale.getDefault(), "%.1f", weightValue);
            etBirthWeight.setText(formattedWeight);

        } catch (NumberFormatException e) {
            etBirthWeight.setError("Enter a valid number (e.g., 3.2)");
            return false;
        }

        return true;
    }

    private boolean validatePhoneNumber(String phone) {
        if (TextUtils.isEmpty(phone)) {
            etGuardianContact.setError("Phone number is required");
            return false;
        }

        // Remove any whitespace
        phone = phone.replaceAll("\\s+", "");

        // Check if it matches Kenyan phone pattern
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            etGuardianContact.setError("Enter a valid Kenyan phone number (e.g., 0712345678)");
            return false;
        }

        // Format to 254 format for consistency
        String formattedPhone = formatPhoneNumber(phone);
        etGuardianContact.setText(formattedPhone);

        return true;
    }

    private String formatPhoneNumber(String phone) {
        // Remove all non-digit characters
        String digits = phone.replaceAll("\\D", "");

        // Format to 254XXXXXXXXX
        if (digits.startsWith("0") && digits.length() == 10) {
            return "254" + digits.substring(1);
        } else if (digits.startsWith("254") && digits.length() == 12) {
            return digits;
        } else if (digits.startsWith("7") && digits.length() == 9) {
            return "254" + digits;
        } else if (digits.startsWith("+254") && digits.length() == 13) {
            return digits.substring(1);
        }

        return phone;
    }

    private boolean validateBirthPlace(String place) {
        if (TextUtils.isEmpty(place)) {
            etBirthPlace.setError("Birth place is required");
            return false;
        }

        if (!PLACE_PATTERN.matcher(place).matches()) {
            etBirthPlace.setError("Enter a valid place name (2-100 characters)");
            return false;
        }

        return true;
    }

    private boolean validateDateOfBirth(String dob) {
        try {
            Date birthDate = dateFormat.parse(dob);
            Date today = new Date();

            // Calculate age in months
            Calendar birthCal = Calendar.getInstance();
            birthCal.setTime(birthDate);
            Calendar todayCal = Calendar.getInstance();
            todayCal.setTime(today);

            int years = todayCal.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR);
            int months = todayCal.get(Calendar.MONTH) - birthCal.get(Calendar.MONTH);

            // Adjust for negative months
            if (months < 0) {
                years--;
                months += 12;
            }

            int totalMonths = (years * 12) + months;

            // Check if child is within vaccination timeline (0-60 months)
            if (totalMonths < 0) {
                Toast.makeText(this, "Date of birth cannot be in the future", Toast.LENGTH_SHORT).show();
                return false;
            }

            if (totalMonths > 60) {
                Toast.makeText(this,
                        "Child is too old for vaccination program (must be under 5 years)",
                        Toast.LENGTH_LONG).show();
                return false;
            }

            // Show age information to user
            String ageText;
            if (years > 0) {
                ageText = years + " year" + (years > 1 ? "s" : "") +
                        (months > 0 ? ", " + months + " month" + (months > 1 ? "s" : "") : "");
            } else {
                ageText = totalMonths + " month" + (totalMonths > 1 ? "s" : "");
            }

            Toast.makeText(this, "Child age: " + ageText, Toast.LENGTH_LONG).show();

            // Check for very young babies (premature warning)
            if (totalMonths < 1) {
                Toast.makeText(this,
                        "Note: Special care needed for newborns under 1 month",
                        Toast.LENGTH_LONG).show();
            }

            return true;

        } catch (ParseException e) {
            Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void clearErrors() {
        etChildName.setError(null);
        etBirthWeight.setError(null);
        etGuardianName.setError(null);
        etGuardianContact.setError(null);
        etHealthCondition.setError(null);
        etBirthPlace.setError(null);
    }

    private void saveChildData(String name, String weight, String guardianName,
                               String guardianContact, String healthCondition,
                               String birthPlace, String dob) {

        String gender = (rgGender.getCheckedRadioButtonId() == R.id.rbMale)
                ? "Male" : "Female";

        // Calculate age in months for easy reference
        int ageMonths = calculateAgeInMonths(dob);

        // Generate unique child ID
        String childId = databaseRef.child("users").child(userId).child("children").push().getKey();

        Map<String, Object> childData = new HashMap<>();
        childData.put("childId", childId);
        childData.put("name", name);
        childData.put("guardianName", guardianName);
        childData.put("guardianContact", guardianContact);
        childData.put("gender", gender);
        childData.put("dateOfBirth", dob);
        childData.put("birthWeight", weight);
        childData.put("healthCondition", healthCondition.isEmpty() ? "None reported" : healthCondition);
        childData.put("birthPlace", birthPlace);
        childData.put("ageMonths", String.valueOf(ageMonths));
        childData.put("isActive", true);
        childData.put("registrationDate", new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(new Date()));

        // Save to children node
        databaseRef.child("users")
                .child(userId)
                .child("children")
                .child(childId)
                .setValue(childData)
                .addOnSuccessListener(unused -> {
                    // Check if this is the first child
                    databaseRef.child("users").child(userId).child("children")
                            .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                                @Override
                                public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                                    if (snapshot.getChildrenCount() == 1) {
                                        // This is the first child, set as selected
                                        databaseRef.child("users").child(userId).child("selectedChild")
                                                .setValue(childId);
                                    }
                                    showSuccessDialog(name, ageMonths);
                                }

                                @Override
                                public void onCancelled(com.google.firebase.database.DatabaseError error) {
                                    Toast.makeText(ChildRegister.this, "Failed to register child", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private int calculateAgeInMonths(String dob) {
        try {
            Date birthDate = dateFormat.parse(dob);
            Date today = new Date();

            Calendar birthCal = Calendar.getInstance();
            birthCal.setTime(birthDate);
            Calendar todayCal = Calendar.getInstance();
            todayCal.setTime(today);

            int years = todayCal.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR);
            int months = todayCal.get(Calendar.MONTH) - birthCal.get(Calendar.MONTH);

            // Adjust for negative months
            if (months < 0) {
                years--;
                months += 12;
            }

            return (years * 12) + months;
        } catch (ParseException e) {
            return 0;
        }
    }

    private void showSuccessDialog(String childName, int ageMonths) {
        // Create success message with vaccination info
        StringBuilder message = new StringBuilder();
        message.append("Child registered successfully!\n\n");
        message.append("Name: ").append(childName).append("\n");
        message.append("Age: ").append(ageMonths).append(" months\n\n");

        // Add vaccination schedule information based on age
        message.append("Recommended Vaccinations:\n");

        if (ageMonths == 0) {
            message.append("- BCG (At birth)\n");
            message.append("- Hepatitis B (At birth)\n");
        } else if (ageMonths <= 6) {
            message.append("- DTaP (6 weeks)\n");
            message.append("- Rotavirus (6 weeks)\n");
            message.append("- PCV (14 weeks)\n");
        } else if (ageMonths <= 12) {
            message.append("- Measles (9 months)\n");
            message.append("- Yellow Fever (9 months)\n");
        } else if (ageMonths <= 24) {
            message.append("- MMR (12 months)\n");
            message.append("- Chickenpox (15 months)\n");
        } else {
            message.append("- Catch-up vaccinations as needed\n");
            message.append("- Annual flu vaccine\n");
        }

        // Show success dialog
        new androidx.appcompat.app.AlertDialog.Builder(this,
                R.style.DatePickerDialogTheme)
                .setTitle("Registration Successful!")
                .setMessage(message.toString())
                .setPositiveButton("Continue", (dialog, which) -> {
                    Intent intent = new Intent(ChildRegister.this, ChildVaccineActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .setIcon(android.R.drawable.ic_dialog_info)
                .setCancelable(false)
                .show();
    }

    // Helper method to validate weight with unit conversion
    private boolean validateWeightWithUnit(String weightInput) {
        if (TextUtils.isEmpty(weightInput)) {
            etBirthWeight.setError("Birth weight is required");
            return false;
        }

        // Remove any unit text and extract number
        String cleanInput = weightInput.replaceAll("[^0-9.]", "").trim();

        if (cleanInput.isEmpty()) {
            etBirthWeight.setError("Enter a valid weight (e.g., 3.2 or 3.2kg)");
            return false;
        }

        try {
            double weightValue = Double.parseDouble(cleanInput);

            // Check if input contains 'lb' or 'lbs' - convert from pounds to kg
            if (weightInput.toLowerCase().contains("lb")) {
                weightValue = weightValue * 0.453592; // Convert pounds to kg
                String convertedWeight = String.format(Locale.getDefault(), "%.1f kg", weightValue);
                etBirthWeight.setText(convertedWeight);
            } else {
                // Assume kg if no unit specified
                String formattedWeight = String.format(Locale.getDefault(), "%.1f kg", weightValue);
                etBirthWeight.setText(formattedWeight);
            }

            // Validate weight range
            if (weightValue < 0.5) {
                etBirthWeight.setError("Weight is too low (minimum 0.5kg)");
                return false;
            } else if (weightValue > 10) {
                etBirthWeight.setError("Weight is too high for newborn (maximum 10 kg)");
                return false;
            } else if (weightValue < 2.5) {
                // Warning for low birth weight
                Toast.makeText(this,
                        "Note: Low birth weight detected. Extra monitoring recommended.",
                        Toast.LENGTH_LONG).show();
            }

            return true;

        } catch (NumberFormatException e) {
            etBirthWeight.setError("Enter a valid number (e.g., 3.2 or 3.2kg)");
            return false;
        }
    }

    // Override onBackPressed to confirm exit
    @Override
    public void onBackPressed() {
        new androidx.appcompat.app.AlertDialog.Builder(this,
                R.style.DatePickerDialogTheme)
                .setTitle("Exit Registration")
                .setMessage("Are you sure you want to exit? All unsaved data will be lost.")
                .setPositiveButton("Exit", (dialog, which) -> super.onBackPressed())
                .setNegativeButton("Cancel", null)
                .show();
    }
}