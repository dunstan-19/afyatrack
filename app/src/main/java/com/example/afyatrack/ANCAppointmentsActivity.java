package com.example.afyatrack;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class ANCAppointmentsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FirebaseUser currentUser;

    // UI Components
    private TextView toolbarTitle, tvFormTitle, tvAppointmentId;
    private ImageButton btnHome, btnSettings, btnHelp, btnCloseForm;
    private BottomNavigationView bottomNavigation;
    private CardView bookingCard, historyCard;
    private TextInputEditText edtAppointmentDate, edtAppointmentTime, edtDoctor, edtFacility, edtReason;
    private TextInputLayout tilDate, tilTime, tilDoctor, tilFacility, tilReason;
    private Spinner spinnerAppointmentType, spinnerStatus;
    private TextView tvStatusLabel;
    private RadioGroup rgPriority;
    private MaterialButton btnBookAppointment, btnCancelBooking, btnBookNew, btnUpdateAppointment, btnDeleteAppointment;
    private ProgressBar progressBar, historyProgress;
    private RecyclerView recyclerViewAppointments;
    private LinearLayout llNoAppointments, llActionButtons, llEditButtons;
    private NestedScrollView scrollView;

    // Data
    private List<Appointment> appointmentsList = new ArrayList<>();
    private AppointmentAdapter appointmentAdapter;
    private String motherName = "";
    private String weeksPregnant = "";
    private boolean isEditMode = false;
    private String currentAppointmentId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ancappointments);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // Initialize UI Components
        initializeViews();
        setupBottomNavigation();
        setupUI();
        setupTopNavigation();

        // Load mother info and appointments
        loadMotherInfo();
        loadAppointments();
    }

    private void initializeViews() {
        toolbarTitle = findViewById(R.id.toolbar_title);
        tvFormTitle = findViewById(R.id.tvFormTitle);
        tvAppointmentId = findViewById(R.id.tvAppointmentId);
        scrollView = findViewById(R.id.scrollView);

        // Top Navigation Buttons
        btnHome = findViewById(R.id.btn_home);
        btnSettings = findViewById(R.id.btn_settings);
        btnHelp = findViewById(R.id.btn_help);

        // Bottom Navigation
        bottomNavigation = findViewById(R.id.bottom_navigation);

        // Booking Form Views
        bookingCard = findViewById(R.id.bookingCard);
        btnCloseForm = findViewById(R.id.btnCloseForm);
        edtAppointmentDate = findViewById(R.id.edtAppointmentDate);
        edtAppointmentTime = findViewById(R.id.edtAppointmentTime);
        edtDoctor = findViewById(R.id.edtDoctor);
        edtFacility = findViewById(R.id.edtFacility);
        edtReason = findViewById(R.id.edtReason);

        tilDate = findViewById(R.id.tilDate);
        tilTime = findViewById(R.id.tilTime);
        tilDoctor = findViewById(R.id.tilDoctor);
        tilFacility = findViewById(R.id.tilFacility);
        tilReason = findViewById(R.id.tilReason);

        spinnerAppointmentType = findViewById(R.id.spinnerAppointmentType);
        spinnerStatus = findViewById(R.id.spinnerStatus);
        tvStatusLabel = findViewById(R.id.tvStatusLabel);
        rgPriority = findViewById(R.id.rgPriority);

        btnBookAppointment = findViewById(R.id.btnBookAppointment);
        btnCancelBooking = findViewById(R.id.btnCancelBooking);
        btnUpdateAppointment = findViewById(R.id.btnUpdateAppointment);
        btnDeleteAppointment = findViewById(R.id.btnDeleteAppointment);
        progressBar = findViewById(R.id.progressBar);

        // Layout containers
        llActionButtons = findViewById(R.id.llActionButtons);
        llEditButtons = findViewById(R.id.llEditButtons);

        // Appointment History Views
        historyCard = findViewById(R.id.historyCard);
        recyclerViewAppointments = findViewById(R.id.recyclerViewAppointments);
        llNoAppointments = findViewById(R.id.llNoAppointments);
        btnBookNew = findViewById(R.id.btnBookNew);
        historyProgress = findViewById(R.id.historyProgress);

        // Setup RecyclerView
        recyclerViewAppointments.setLayoutManager(new LinearLayoutManager(this));
        appointmentAdapter = new AppointmentAdapter(appointmentsList);
        recyclerViewAppointments.setAdapter(appointmentAdapter);
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
                    startActivity(new Intent(ANCAppointmentsActivity.this, ANCRecordsActivity.class));
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    return true;
                } else if (itemId == R.id.nav_appointment) {
                    // Already on appointments
                    return true;
                } else if (itemId == R.id.nav_dashboard) {
                    startActivity(new Intent(ANCAppointmentsActivity.this, AntenatalActivity.class));
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    return true;
                } else if (itemId == R.id.nav_notifications) {
                    startActivity(new Intent(ANCAppointmentsActivity.this, ANCNotificationsActivity.class));
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    return true;
                } else if (itemId == R.id.nav_child_profile) {
                    startActivity(new Intent(ANCAppointmentsActivity.this, ANCProfileActivity.class));
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    return true;
                }
                return false;
            }
        });

        bottomNavigation.setSelectedItemId(R.id.nav_appointment);
    }

    private void setupUI() {
        toolbarTitle.setText("Appointments");

        // Setup appointment type spinner
        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(this,
                R.array.appointment_types, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAppointmentType.setAdapter(typeAdapter);

        // Setup status spinner
        ArrayAdapter<CharSequence> statusAdapter = ArrayAdapter.createFromResource(this,
                R.array.appointment_status, android.R.layout.simple_spinner_item);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(statusAdapter);

        // Setup date picker on click
        tilDate.setEndIconOnClickListener(v -> showDatePicker());
        edtAppointmentDate.setOnClickListener(v -> showDatePicker());

        // Setup time picker on click
        tilTime.setEndIconOnClickListener(v -> showTimePicker());
        edtAppointmentTime.setOnClickListener(v -> showTimePicker());

        // Setup book appointment button
        btnBookAppointment.setOnClickListener(v -> validateAndBookAppointment());

        // Setup update appointment button
        btnUpdateAppointment.setOnClickListener(v -> validateAndUpdateAppointment());

        // Setup delete appointment button
        btnDeleteAppointment.setOnClickListener(v -> deleteAppointment());

        // Setup cancel button
        btnCancelBooking.setOnClickListener(v -> clearForm());

        // Setup close form button
        btnCloseForm.setOnClickListener(v -> {
            hideBookingForm();
            resetFormMode();
        });

        // Setup book new appointment button (when no appointments exist)
        btnBookNew.setOnClickListener(v -> showBookingForm());

        // Set default priority
        rgPriority.check(R.id.rbNormal);

        // Setup auto-fill for doctor and facility
        setupAutoFill();
    }

    private void setupAutoFill() {
        // Common doctors and facilities for auto-fill suggestions
        String[] commonDoctors = {
                "Dr. Wanjiku - Obstetrician",
                "Dr. Kamau - Gynecologist",
                "Dr. Akinyi - Midwife",
                "Dr. Mohamed - Pediatrician"
        };

        String[] commonFacilities = {
                "Moi Teaching and Referral Hospital",
                "Kenyatta National Hospital",
                "Nairobi Hospital",
                "Aga Khan University Hospital"
        };

        // Set click listeners for dropdown suggestions
        edtDoctor.setOnClickListener(v -> showSuggestionDialog("Select Doctor", commonDoctors, edtDoctor));
        edtFacility.setOnClickListener(v -> showSuggestionDialog("Select Facility", commonFacilities, edtFacility));
    }

    private void showSuggestionDialog(String title, String[] items, TextInputEditText editText) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(title);
        builder.setItems(items, (dialog, which) -> {
            editText.setText(items[which]);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                R.style.DatePickerDialogTheme,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(selectedYear, selectedMonth, selectedDay);

                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    edtAppointmentDate.setText(sdf.format(selectedDate.getTime()));
                }, year, month, day);

        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        // Set maximum date to 9 months from now
        calendar.add(Calendar.MONTH, 9);
        datePickerDialog.getDatePicker().setMaxDate(calendar.getTimeInMillis());

        datePickerDialog.show();
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                R.style.DatePickerDialogTheme,
                (view, selectedHour, selectedMinute) -> {
                    String time = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
                    edtAppointmentTime.setText(time);
                }, hour, minute, true);

        timePickerDialog.show();
    }

    private void loadMotherInfo() {
        if (currentUser == null) return;

        mDatabase.child("users").child(currentUser.getUid()).child("anc_registration")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            motherName = snapshot.child("name").getValue(String.class);
                            weeksPregnant = snapshot.child("weeksPregnant").getValue(String.class);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Handle error
                    }
                });
    }

    private void loadAppointments() {
        if (currentUser == null) return;

        historyProgress.setVisibility(View.VISIBLE);
        llNoAppointments.setVisibility(View.GONE);
        recyclerViewAppointments.setVisibility(View.GONE);

        mDatabase.child("users").child(currentUser.getUid()).child("appointments")
                .orderByChild("timestamp")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        historyProgress.setVisibility(View.GONE);
                        appointmentsList.clear();

                        if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                            for (DataSnapshot appointmentSnapshot : snapshot.getChildren()) {
                                Appointment appointment = appointmentSnapshot.getValue(Appointment.class);
                                if (appointment != null) {
                                    appointment.setId(appointmentSnapshot.getKey());
                                    appointmentsList.add(appointment);
                                }
                            }

                            // Sort by date (newest first)
                            Collections.sort(appointmentsList, (a1, a2) ->
                                    Long.compare(a2.getTimestamp(), a1.getTimestamp()));

                            appointmentAdapter.notifyDataSetChanged();
                            llNoAppointments.setVisibility(View.GONE);
                            recyclerViewAppointments.setVisibility(View.VISIBLE);
                        } else {
                            llNoAppointments.setVisibility(View.VISIBLE);
                            recyclerViewAppointments.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        historyProgress.setVisibility(View.GONE);
                        Toast.makeText(ANCAppointmentsActivity.this,
                                "Failed to load appointments",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showBookingForm() {
        bookingCard.setVisibility(View.VISIBLE);
        scrollView.post(() -> scrollView.smoothScrollTo(0, bookingCard.getTop()));
    }

    private void hideBookingForm() {
        bookingCard.setVisibility(View.GONE);
        clearForm();
    }

    private void showEditForm(Appointment appointment) {
        isEditMode = true;
        currentAppointmentId = appointment.getId();

        // Update form title
        tvFormTitle.setText("Edit Appointment");

        // Set form data
        tvAppointmentId.setText(appointment.getId());
        edtAppointmentDate.setText(appointment.getDate());
        edtAppointmentTime.setText(appointment.getTime());
        edtDoctor.setText(appointment.getDoctor());
        edtFacility.setText(appointment.getFacility());
        edtReason.setText(appointment.getReason());

        // Set appointment type
        for (int i = 0; i < spinnerAppointmentType.getCount(); i++) {
            if (spinnerAppointmentType.getItemAtPosition(i).toString().equals(appointment.getType())) {
                spinnerAppointmentType.setSelection(i);
                break;
            }
        }

        // Set status (show status field in edit mode)
        tvStatusLabel.setVisibility(View.VISIBLE);
        spinnerStatus.setVisibility(View.VISIBLE);
        for (int i = 0; i < spinnerStatus.getCount(); i++) {
            if (spinnerStatus.getItemAtPosition(i).toString().equals(appointment.getStatus())) {
                spinnerStatus.setSelection(i);
                break;
            }
        }

        // Set priority
        if ("Urgent".equals(appointment.getPriority())) {
            rgPriority.check(R.id.rbUrgent);
        } else {
            rgPriority.check(R.id.rbNormal);
        }

        // Show edit buttons, hide create buttons
        llActionButtons.setVisibility(View.GONE);
        llEditButtons.setVisibility(View.VISIBLE);

        // Show form
        showBookingForm();
    }

    private void resetFormMode() {
        isEditMode = false;
        currentAppointmentId = "";
        tvFormTitle.setText("New Appointment");
        tvStatusLabel.setVisibility(View.GONE);
        spinnerStatus.setVisibility(View.GONE);
        llActionButtons.setVisibility(View.VISIBLE);
        llEditButtons.setVisibility(View.GONE);
        clearForm();
    }

    private void validateAndBookAppointment() {
        // Get all values
        String date = edtAppointmentDate.getText().toString().trim();
        String time = edtAppointmentTime.getText().toString().trim();
        String doctor = edtDoctor.getText().toString().trim();
        String facility = edtFacility.getText().toString().trim();
        String reason = edtReason.getText().toString().trim();
        String appointmentType = spinnerAppointmentType.getSelectedItem().toString();

        // Get priority
        String priority = "Normal";
        int selectedPriorityId = rgPriority.getCheckedRadioButtonId();
        if (selectedPriorityId == R.id.rbUrgent) {
            priority = "Urgent";
        }

        // Validate required fields
        if (!validateForm(date, time, doctor, facility, reason)) {
            return;
        }

        // Create appointment
        bookAppointment(date, time, doctor, facility, reason, appointmentType, priority);
    }

    private void validateAndUpdateAppointment() {
        // Get all values
        String date = edtAppointmentDate.getText().toString().trim();
        String time = edtAppointmentTime.getText().toString().trim();
        String doctor = edtDoctor.getText().toString().trim();
        String facility = edtFacility.getText().toString().trim();
        String reason = edtReason.getText().toString().trim();
        String appointmentType = spinnerAppointmentType.getSelectedItem().toString();
        String status = spinnerStatus.getSelectedItem().toString();

        // Get priority
        String priority = "Normal";
        int selectedPriorityId = rgPriority.getCheckedRadioButtonId();
        if (selectedPriorityId == R.id.rbUrgent) {
            priority = "Urgent";
        }

        // Validate required fields
        if (!validateForm(date, time, doctor, facility, reason)) {
            return;
        }

        // Update appointment
        updateAppointment(date, time, doctor, facility, reason, appointmentType, priority, status);
    }

    private boolean validateForm(String date, String time, String doctor, String facility, String reason) {
        boolean isValid = true;

        if (TextUtils.isEmpty(date)) {
            tilDate.setError("Required");
            isValid = false;
        } else {
            tilDate.setError(null);
        }

        if (TextUtils.isEmpty(time)) {
            tilTime.setError("Required");
            isValid = false;
        } else {
            tilTime.setError(null);
        }

        if (TextUtils.isEmpty(doctor)) {
            tilDoctor.setError("Required");
            isValid = false;
        } else {
            tilDoctor.setError(null);
        }

        if (TextUtils.isEmpty(facility)) {
            tilFacility.setError("Required");
            isValid = false;
        } else {
            tilFacility.setError(null);
        }

        if (TextUtils.isEmpty(reason)) {
            tilReason.setError("Required");
            isValid = false;
        } else {
            tilReason.setError(null);
        }

        if (!isValid) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void bookAppointment(String date, String time, String doctor, String facility,
                                 String reason, String type, String priority) {
        progressBar.setVisibility(View.VISIBLE);
        btnBookAppointment.setEnabled(false);

        // Auto-fill reason based on pregnancy stage if empty
        if (TextUtils.isEmpty(reason) && weeksPregnant != null) {
            try {
                int weeks = Integer.parseInt(weeksPregnant);
                if (weeks <= 12) {
                    reason = "First trimester checkup";
                } else if (weeks <= 27) {
                    reason = "Second trimester scan and tests";
                } else {
                    reason = "Third trimester monitoring";
                }
            } catch (NumberFormatException e) {
                reason = "Routine antenatal checkup";
            }
        }

        // Create appointment data
        Map<String, Object> appointmentData = new HashMap<>();
        appointmentData.put("date", date);
        appointmentData.put("time", time);
        appointmentData.put("doctor", doctor);
        appointmentData.put("facility", facility);
        appointmentData.put("reason", reason);
        appointmentData.put("type", type);
        appointmentData.put("priority", priority);
        appointmentData.put("status", "Scheduled");
        appointmentData.put("motherName", motherName);
        appointmentData.put("weeksPregnant", weeksPregnant != null ? weeksPregnant : "Not specified");
        appointmentData.put("timestamp", System.currentTimeMillis());
        appointmentData.put("createdAt", new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(Calendar.getInstance().getTime()));

        // Generate unique appointment ID under users/userId/appointments
        String appointmentId = mDatabase.child("users").child(currentUser.getUid())
                .child("appointments").push().getKey();

        if (appointmentId != null) {
            mDatabase.child("users").child(currentUser.getUid())
                    .child("appointments").child(appointmentId)
                    .setValue(appointmentData)
                    .addOnCompleteListener(task -> {
                        progressBar.setVisibility(View.GONE);
                        btnBookAppointment.setEnabled(true);

                        if (task.isSuccessful()) {
                            // Show success dialog
                            showSuccessDialog(date, time, doctor, facility, type, "Booked");

                            // Clear form and hide booking card
                            clearForm();
                            hideBookingForm();
                            resetFormMode();

                            // Update ANC registration with next visit date
                            updateNextVisitDate(date);

                            // Refresh appointments list
                            loadAppointments();
                        } else {
                            Toast.makeText(ANCAppointmentsActivity.this,
                                    "Failed to book appointment: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void updateAppointment(String date, String time, String doctor, String facility,
                                   String reason, String type, String priority, String status) {
        progressBar.setVisibility(View.VISIBLE);
        btnUpdateAppointment.setEnabled(false);

        // Create appointment data
        Map<String, Object> appointmentData = new HashMap<>();
        appointmentData.put("date", date);
        appointmentData.put("time", time);
        appointmentData.put("doctor", doctor);
        appointmentData.put("facility", facility);
        appointmentData.put("reason", reason);
        appointmentData.put("type", type);
        appointmentData.put("priority", priority);
        appointmentData.put("status", status);
        appointmentData.put("motherName", motherName);
        appointmentData.put("weeksPregnant", weeksPregnant != null ? weeksPregnant : "Not specified");
        appointmentData.put("updatedAt", new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(Calendar.getInstance().getTime()));

        // Update appointment
        mDatabase.child("users").child(currentUser.getUid())
                .child("appointments").child(currentAppointmentId)
                .updateChildren(appointmentData)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    btnUpdateAppointment.setEnabled(true);

                    if (task.isSuccessful()) {
                        // Show success dialog
                        showSuccessDialog(date, time, doctor, facility, type, "Updated");

                        // Clear form and hide booking card
                        clearForm();
                        hideBookingForm();
                        resetFormMode();

                        // Update ANC registration with next visit date if status is scheduled
                        if ("Scheduled".equals(status)) {
                            updateNextVisitDate(date);
                        }

                        // Refresh appointments list
                        loadAppointments();
                    } else {
                        Toast.makeText(ANCAppointmentsActivity.this,
                                "Failed to update appointment: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void deleteAppointment() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Delete Appointment");
        builder.setMessage("Are you sure you want to delete this appointment? This action cannot be undone.");
        builder.setPositiveButton("Delete", (dialog, which) -> {
            progressBar.setVisibility(View.VISIBLE);

            mDatabase.child("users").child(currentUser.getUid())
                    .child("appointments").child(currentAppointmentId)
                    .removeValue()
                    .addOnCompleteListener(task -> {
                        progressBar.setVisibility(View.GONE);

                        if (task.isSuccessful()) {
                            Toast.makeText(ANCAppointmentsActivity.this,
                                    "Appointment deleted successfully", Toast.LENGTH_SHORT).show();

                            // Clear form and hide booking card
                            clearForm();
                            hideBookingForm();
                            resetFormMode();

                            // Refresh appointments list
                            loadAppointments();
                        } else {
                            Toast.makeText(ANCAppointmentsActivity.this,
                                    "Failed to delete appointment", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showSuccessDialog(String date, String time, String doctor, String facility, String type, String action) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Appointment " + action + "!");
        builder.setIcon(R.drawable.circle_background_primary);

        String message = "Your appointment has been successfully " + action.toLowerCase() + ".\n\n" +
                "📅 Date: " + date + "\n" +
                "⏰ Time: " + time + "\n" +
                "👨‍⚕️ Doctor: " + doctor + "\n" +
                "🏥 Facility: " + facility + "\n" +
                "📝 Type: " + type + "\n\n" +
                (action.equals("Booked") ? "A reminder will be sent before your appointment." : "Changes have been saved.");

        builder.setMessage(message);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateNextVisitDate(String nextVisitDate) {
        if (currentUser == null) return;

        mDatabase.child("users").child(currentUser.getUid())
                .child("anc_registration").child("nextVisitDate")
                .setValue(nextVisitDate);
    }

    private void clearForm() {
        edtAppointmentDate.setText("");
        edtAppointmentTime.setText("");
        edtDoctor.setText("");
        edtFacility.setText("");
        edtReason.setText("");
        spinnerAppointmentType.setSelection(0);
        spinnerStatus.setSelection(0);
        rgPriority.check(R.id.rbNormal);

        // Clear errors
        tilDate.setError(null);
        tilTime.setError(null);
        tilDoctor.setError(null);
        tilFacility.setError(null);
        tilReason.setError(null);
    }

    // Appointment model class
    public static class Appointment {
        private String id;
        private String date;
        private String time;
        private String doctor;
        private String facility;
        private String reason;
        private String type;
        private String priority;
        private String status;
        private String motherName;
        private String weeksPregnant;
        private long timestamp;
        private String createdAt;
        private String updatedAt;

        public Appointment() {}

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }

        public String getDoctor() { return doctor; }
        public void setDoctor(String doctor) { this.doctor = doctor; }

        public String getFacility() { return facility; }
        public void setFacility(String facility) { this.facility = facility; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getMotherName() { return motherName; }
        public void setMotherName(String motherName) { this.motherName = motherName; }

        public String getWeeksPregnant() { return weeksPregnant; }
        public void setWeeksPregnant(String weeksPregnant) { this.weeksPregnant = weeksPregnant; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    }

    // Appointment Adapter
    private class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.ViewHolder> {

        private List<Appointment> appointments;

        public AppointmentAdapter(List<Appointment> appointments) {
            this.appointments = appointments;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_appointment, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Appointment appointment = appointments.get(position);
            holder.bind(appointment);
        }

        @Override
        public int getItemCount() {
            return appointments.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private TextView tvAppointmentDate, tvAppointmentTime, tvDoctor, tvFacility, tvStatus, tvType, tvPriority;
            private MaterialCardView cardAppointment;
            private ImageButton btnEdit, btnDelete;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvAppointmentDate = itemView.findViewById(R.id.tvAppointmentDate);
                tvAppointmentTime = itemView.findViewById(R.id.tvAppointmentTime);
                tvDoctor = itemView.findViewById(R.id.tvDoctor);
                tvFacility = itemView.findViewById(R.id.tvFacility);
                tvStatus = itemView.findViewById(R.id.tvStatus);
                tvType = itemView.findViewById(R.id.tvType);
                tvPriority = itemView.findViewById(R.id.tvPriority);
                cardAppointment = itemView.findViewById(R.id.cardAppointment);
                btnEdit = itemView.findViewById(R.id.btnEdit);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }

            public void bind(Appointment appointment) {
                tvAppointmentDate.setText(appointment.getDate());
                tvAppointmentTime.setText(appointment.getTime());
                tvDoctor.setText(appointment.getDoctor());
                tvFacility.setText(appointment.getFacility());
                tvStatus.setText(appointment.getStatus());
                tvType.setText(appointment.getType());
                tvPriority.setText(appointment.getPriority());

                // Set status color
                int statusColor = R.color.primary_blue; // default for Scheduled
                int strokeColor = R.color.primary_blue;

                switch (appointment.getStatus()) {
                    case "Pending":
                        statusColor = R.color.primary_yellow;
                        strokeColor = R.color.primary_yellow;
                        break;
                    case "Scheduled":
                        statusColor = R.color.primary_blue;
                        strokeColor = R.color.primary_blue;
                        break;
                    case "Completed":
                        statusColor = R.color.primary_green;
                        strokeColor = R.color.primary_green;
                        break;
                    case "Cancelled":
                        statusColor = R.color.primary_red;
                        strokeColor = R.color.primary_red;
                        break;
                    case "Missed":
                        statusColor = R.color.primary_orange;
                        strokeColor = R.color.primary_orange;
                        break;
                }

                cardAppointment.setStrokeColor(ContextCompat.getColor(ANCAppointmentsActivity.this, strokeColor));
                tvStatus.setTextColor(ContextCompat.getColor(ANCAppointmentsActivity.this, statusColor));

                // Set priority color
                if ("Urgent".equals(appointment.getPriority())) {
                    tvPriority.setTextColor(ContextCompat.getColor(ANCAppointmentsActivity.this, R.color.primary_red));
                } else {
                    tvPriority.setTextColor(ContextCompat.getColor(ANCAppointmentsActivity.this, R.color.text_primary));
                }

                // Set click listeners
                itemView.setOnClickListener(v -> showAppointmentDetails(appointment));

                // Edit button
                btnEdit.setOnClickListener(v -> showEditForm(appointment));

                // Delete button
                btnDelete.setOnClickListener(v -> confirmDeleteAppointment(appointment));
            }
        }
    }

    private void confirmDeleteAppointment(Appointment appointment) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Delete Appointment");
        builder.setMessage("Are you sure you want to delete the appointment with " +
                appointment.getDoctor() + " on " + appointment.getDate() + "?");
        builder.setPositiveButton("Delete", (dialog, which) -> {
            deleteAppointmentFromDatabase(appointment.getId());
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void deleteAppointmentFromDatabase(String appointmentId) {
        progressBar.setVisibility(View.VISIBLE);

        mDatabase.child("users").child(currentUser.getUid())
                .child("appointments").child(appointmentId)
                .removeValue()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        Toast.makeText(ANCAppointmentsActivity.this,
                                "Appointment deleted successfully", Toast.LENGTH_SHORT).show();
                        loadAppointments();
                    } else {
                        Toast.makeText(ANCAppointmentsActivity.this,
                                "Failed to delete appointment", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showAppointmentDetails(Appointment appointment) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Appointment Details");
        builder.setIcon(R.drawable.circle_background_primary);

        StringBuilder message = new StringBuilder();
        message.append("📅 Date: ").append(appointment.getDate()).append("\n");
        message.append("⏰ Time: ").append(appointment.getTime()).append("\n");
        message.append("👨‍⚕️ Doctor: ").append(appointment.getDoctor()).append("\n");
        message.append("🏥 Facility: ").append(appointment.getFacility()).append("\n");
        message.append("📝 Type: ").append(appointment.getType()).append("\n");
        message.append("🎯 Priority: ").append(appointment.getPriority()).append("\n");
        message.append("📋 Reason: ").append(appointment.getReason()).append("\n");
        message.append("📊 Status: ").append(appointment.getStatus()).append("\n");
        message.append("🤰 Weeks Pregnant: ").append(appointment.getWeeksPregnant()).append("\n");
        if (appointment.getCreatedAt() != null) {
            message.append("📝 Created: ").append(appointment.getCreatedAt()).append("\n");
        }
        if (appointment.getUpdatedAt() != null) {
            message.append("✏️ Updated: ").append(appointment.getUpdatedAt()).append("\n");
        }

        builder.setMessage(message.toString());

        // Add action buttons
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());

        if (!"Completed".equals(appointment.getStatus()) && !"Cancelled".equals(appointment.getStatus())) {
            builder.setNegativeButton("Edit", (dialog, which) -> showEditForm(appointment));
            builder.setNeutralButton("Delete", (dialog, which) -> confirmDeleteAppointment(appointment));
        }

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAppointments();
        bottomNavigation.setSelectedItemId(R.id.nav_appointment);
    }
}