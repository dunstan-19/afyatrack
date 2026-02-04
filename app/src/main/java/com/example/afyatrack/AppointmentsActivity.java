package com.example.afyatrack;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class AppointmentsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FirebaseUser currentUser;

    // UI Components
    private MaterialToolbar toolbar;
    private Spinner spinnerChildren, spinnerAppointmentType;
    private TextView tvChildDetails, tvFormTitle, tvAppointmentsTitle;
    private LinearLayout llNoAppointments;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private MaterialCardView cardForm;
    private FloatingActionButton fabAdd;
    private NestedScrollView scrollView;

    // Form Fields
    private TextInputEditText edtDate, edtTime, edtFacility, edtDoctor, edtReason;
    private TextInputLayout tilDate, tilTime, tilFacility, tilDoctor, tilReason;
    private MaterialButton btnSubmit, btnCancelForm;
    private TextView tvAppointmentId, tvChildId;
    private ImageButton btnCloseForm;

    // Data
    private List<Child> childrenList = new ArrayList<>();
    private List<ChildAppointment> appointmentsList = new ArrayList<>();
    private ChildAppointmentAdapter appointmentAdapter;
    private String selectedChildId = "";
    private boolean isEditMode = false;

    // Tag for logging
    private static final String TAG = "ChildAppointmentsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointments);

        Log.d(TAG, "Activity created");

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Log.d(TAG, "User not authenticated, redirecting to login");
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        Log.d(TAG, "User authenticated: " + currentUser.getUid());

        initializeViews();
        setupUI();
        setupListeners();
        loadChildren();
    }

    private void initializeViews() {
        Log.d(TAG, "Initializing views");

        // Toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Main views
        spinnerChildren = findViewById(R.id.spinnerChildren);
        spinnerAppointmentType = findViewById(R.id.spinnerAppointmentType);
        tvChildDetails = findViewById(R.id.tvChildDetails);
        tvFormTitle = findViewById(R.id.tvFormTitle);
        tvAppointmentsTitle = findViewById(R.id.tvAppointmentsTitle);
        llNoAppointments = findViewById(R.id.llNoAppointments);
        progressBar = findViewById(R.id.progressBar);
        recyclerView = findViewById(R.id.recyclerView);
        cardForm = findViewById(R.id.cardForm);
        fabAdd = findViewById(R.id.fabAdd);
        scrollView = findViewById(R.id.scrollView);

        // Form fields
        tvAppointmentId = findViewById(R.id.tvAppointmentId);
        tvChildId = findViewById(R.id.tvChildId);

        edtDate = findViewById(R.id.edtDate);
        edtTime = findViewById(R.id.edtTime);
        edtFacility = findViewById(R.id.edtFacility);
        edtDoctor = findViewById(R.id.edtDoctor);
        edtReason = findViewById(R.id.edtReason);

        tilDate = findViewById(R.id.tilDate);
        tilTime = findViewById(R.id.tilTime);
        tilFacility = findViewById(R.id.tilFacility);
        tilDoctor = findViewById(R.id.tilDoctor);
        tilReason = findViewById(R.id.tilReason);

        btnSubmit = findViewById(R.id.btnSubmit);
        btnCancelForm = findViewById(R.id.btnCancelForm);
        btnCloseForm = findViewById(R.id.btnCloseForm);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        appointmentAdapter = new ChildAppointmentAdapter(appointmentsList);
        recyclerView.setAdapter(appointmentAdapter);
    }

    private void setupUI() {

        // Setup appointment type spinner
        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(this,
                R.array.child_appointment_types, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAppointmentType.setAdapter(typeAdapter);

        // Hide appointments title initially
        tvAppointmentsTitle.setVisibility(View.GONE);
    }

    private void setupListeners() {
        fabAdd.setOnClickListener(v -> {
            Log.d(TAG, "FAB clicked");
            showAddForm();
        });

        btnCloseForm.setOnClickListener(v -> {
            Log.d(TAG, "Close form clicked");
            hideForm();
        });

        btnSubmit.setOnClickListener(v -> {
            Log.d(TAG, "Submit clicked");
            validateAndSaveAppointment();
        });

        btnCancelForm.setOnClickListener(v -> {
            Log.d(TAG, "Cancel form clicked");
            hideForm();
        });

        // Setup date picker
        edtDate.setOnClickListener(v -> showDatePicker());
        tilDate.setEndIconOnClickListener(v -> showDatePicker());

        // Setup time picker
        edtTime.setOnClickListener(v -> showTimePicker());
        tilTime.setEndIconOnClickListener(v -> showTimePicker());

        // Setup child selection listener
        spinnerChildren.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "Child selected at position: " + position);
                if (position > 0 && position - 1 < childrenList.size()) {
                    Child selectedChild = childrenList.get(position - 1);
                    selectedChildId = selectedChild.getId();
                    updateChildDetails(selectedChild);
                    loadAppointments(selectedChildId);
                } else {
                    selectedChildId = "";
                    tvChildDetails.setText("Select a child to view appointments");
                    clearAppointmentsList();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedChildId = "";
            }
        });
    }

    private void loadChildren() {
        if (currentUser == null) return;

        Log.d(TAG, "Loading children for user: " + currentUser.getUid());

        mDatabase.child("users").child(currentUser.getUid()).child("children")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d(TAG, "Children data received: " + snapshot.getChildrenCount());
                        childrenList.clear();
                        List<String> childNames = new ArrayList<>();
                        childNames.add("Select a child");

                        if (snapshot.exists()) {
                            for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                                Child child = childSnapshot.getValue(Child.class);
                                if (child != null) {
                                    child.setId(childSnapshot.getKey());
                                    childrenList.add(child);
                                    childNames.add(child.getName());
                                    Log.d(TAG, "Child loaded: " + child.getName());
                                }
                            }
                        } else {
                            Log.d(TAG, "No children found in database");
                        }

                        // Setup spinner
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                AppointmentsActivity.this,
                                android.R.layout.simple_spinner_item,
                                childNames
                        );
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerChildren.setAdapter(adapter);

                        Log.d(TAG, "Spinner populated with " + childNames.size() + " items");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to load children: " + error.getMessage());
                        Toast.makeText(AppointmentsActivity.this,
                                "Failed to load children", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateChildDetails(Child child) {
        String details = String.format("%s • %s",
                child.getName(),
                child.getGender());
        tvChildDetails.setText(details);
    }

    private void loadAppointments(String childId) {
        if (currentUser == null || TextUtils.isEmpty(childId)) {
            Log.d(TAG, "Cannot load appointments: user or childId is null");
            return;
        }

        Log.d(TAG, "Loading appointments for child: " + childId);

        progressBar.setVisibility(View.VISIBLE);
        llNoAppointments.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        tvAppointmentsTitle.setVisibility(View.GONE);

        mDatabase.child("users").child(currentUser.getUid())
                .child("children").child(childId).child("appointments")
                .orderByChild("dateTimestamp")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        progressBar.setVisibility(View.GONE);
                        appointmentsList.clear();

                        Log.d(TAG, "Appointments data received: " + snapshot.getChildrenCount());

                        if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                            for (DataSnapshot appointmentSnapshot : snapshot.getChildren()) {
                                ChildAppointment appointment = appointmentSnapshot.getValue(ChildAppointment.class);
                                if (appointment != null) {
                                    appointment.setId(appointmentSnapshot.getKey());
                                    appointmentsList.add(appointment);
                                }
                            }

                            // Sort by date (nearest first)
                            Collections.sort(appointmentsList, (a1, a2) ->
                                    Long.compare(a1.getDateTimestamp(), a2.getDateTimestamp()));

                            appointmentAdapter.notifyDataSetChanged();
                            llNoAppointments.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                            tvAppointmentsTitle.setVisibility(View.VISIBLE);

                            Log.d(TAG, "Appointments loaded: " + appointmentsList.size());
                        } else {
                            llNoAppointments.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                            tvAppointmentsTitle.setVisibility(View.GONE);
                            Log.d(TAG, "No appointments found");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "Failed to load appointments: " + error.getMessage());
                        Toast.makeText(AppointmentsActivity.this,
                                "Failed to load appointments", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showAddForm() {
        if (TextUtils.isEmpty(selectedChildId)) {
            Toast.makeText(this, "Please select a child first", Toast.LENGTH_SHORT).show();
            return;
        }

        isEditMode = false;
        tvFormTitle.setText("New Appointment");
        btnSubmit.setText("Save Appointment");
        clearForm();
        cardForm.setVisibility(View.VISIBLE);

        // Auto-suggest facility based on previous appointments
        if (!appointmentsList.isEmpty()) {
            edtFacility.setText(appointmentsList.get(0).getFacility());
        }

        scrollView.post(() -> scrollView.smoothScrollTo(0, cardForm.getTop()));

        Log.d(TAG, "Add form shown for child: " + selectedChildId);
    }

    private void showEditForm(ChildAppointment appointment) {
        isEditMode = true;
        tvFormTitle.setText("Edit Appointment");
        btnSubmit.setText("Update Appointment");

        // Fill form with appointment data
        tvAppointmentId.setText(appointment.getId());
        tvChildId.setText(selectedChildId);

        // Set appointment type
        for (int i = 0; i < spinnerAppointmentType.getCount(); i++) {
            if (spinnerAppointmentType.getItemAtPosition(i).toString().equals(appointment.getType())) {
                spinnerAppointmentType.setSelection(i);
                break;
            }
        }

        edtDate.setText(appointment.getDate());
        edtTime.setText(appointment.getTime());
        edtFacility.setText(appointment.getFacility());
        edtDoctor.setText(appointment.getDoctor());
        edtReason.setText(appointment.getReason());

        cardForm.setVisibility(View.VISIBLE);
        scrollView.post(() -> scrollView.smoothScrollTo(0, cardForm.getTop()));

        Log.d(TAG, "Edit form shown for appointment: " + appointment.getId());
    }

    private void hideForm() {
        cardForm.setVisibility(View.GONE);
        clearForm();
        isEditMode = false;
        Log.d(TAG, "Form hidden");
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
                    edtDate.setText(sdf.format(selectedDate.getTime()));
                }, year, month, day);

        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
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
                    edtTime.setText(time);
                }, hour, minute, true);

        timePickerDialog.show();
    }

    private void validateAndSaveAppointment() {
        String type = spinnerAppointmentType.getSelectedItem().toString();
        String date = edtDate.getText().toString().trim();
        String time = edtTime.getText().toString().trim();
        String facility = edtFacility.getText().toString().trim();
        String doctor = edtDoctor.getText().toString().trim();
        String reason = edtReason.getText().toString().trim();

        // Validate required fields
        if (TextUtils.isEmpty(date)) {
            tilDate.setError("Required");
            return;
        } else {
            tilDate.setError(null);
        }

        if (TextUtils.isEmpty(time)) {
            tilTime.setError("Required");
            return;
        } else {
            tilTime.setError(null);
        }

        if (TextUtils.isEmpty(facility)) {
            tilFacility.setError("Required");
            return;
        } else {
            tilFacility.setError(null);
        }

        if (isEditMode) {
            updateAppointment(type, date, time, facility, doctor, reason);
        } else {
            createAppointment(type, date, time, facility, doctor, reason);
        }
    }

    private void createAppointment(String type, String date, String time, String facility,
                                   String doctor, String reason) {
        if (currentUser == null || TextUtils.isEmpty(selectedChildId)) {
            Log.e(TAG, "Cannot create appointment: user or childId is null");
            return;
        }

        // Get selected child
        Child selectedChild = null;
        for (Child child : childrenList) {
            if (child.getId().equals(selectedChildId)) {
                selectedChild = child;
                break;
            }
        }

        if (selectedChild == null) {
            Log.e(TAG, "Selected child not found");
            return;
        }

        Log.d(TAG, "Creating appointment for child: " + selectedChild.getName());

        // Create appointment data
        Map<String, Object> appointmentData = new HashMap<>();
        appointmentData.put("type", type);
        appointmentData.put("date", date);
        appointmentData.put("time", time);
        appointmentData.put("facility", facility);
        appointmentData.put("doctor", doctor);
        appointmentData.put("reason", reason);
        appointmentData.put("status", "Scheduled");
        appointmentData.put("childName", selectedChild.getName());
        appointmentData.put("childAge", selectedChild.getAgeMonths());
        appointmentData.put("dateTimestamp", getDateTimestamp(date, time));
        appointmentData.put("createdAt", System.currentTimeMillis());

        // Save to Firebase
        String appointmentId = mDatabase.child("users").child(currentUser.getUid())
                .child("children").child(selectedChildId)
                .child("appointments").push().getKey();

        if (appointmentId != null) {
            mDatabase.child("users").child(currentUser.getUid())
                    .child("children").child(selectedChildId)
                    .child("appointments").child(appointmentId)
                    .setValue(appointmentData)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Appointment created successfully");
                            Toast.makeText(this, "Appointment saved successfully", Toast.LENGTH_SHORT).show();
                            hideForm();
                        } else {
                            Log.e(TAG, "Failed to create appointment: " + task.getException());
                            Toast.makeText(this, "Failed to save appointment", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void updateAppointment(String type, String date, String time, String facility,
                                   String doctor, String reason) {
        if (currentUser == null || TextUtils.isEmpty(selectedChildId)) return;

        String appointmentId = tvAppointmentId.getText().toString();
        if (TextUtils.isEmpty(appointmentId)) return;

        Log.d(TAG, "Updating appointment: " + appointmentId);

        // Create update data
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("type", type);
        updateData.put("date", date);
        updateData.put("time", time);
        updateData.put("facility", facility);
        updateData.put("doctor", doctor);
        updateData.put("reason", reason);
        updateData.put("dateTimestamp", getDateTimestamp(date, time));
        updateData.put("updatedAt", System.currentTimeMillis());

        // Update in Firebase
        mDatabase.child("users").child(currentUser.getUid())
                .child("children").child(selectedChildId)
                .child("appointments").child(appointmentId)
                .updateChildren(updateData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Appointment updated successfully");
                        Toast.makeText(this, "Appointment updated successfully", Toast.LENGTH_SHORT).show();
                        hideForm();
                    } else {
                        Log.e(TAG, "Failed to update appointment: " + task.getException());
                        Toast.makeText(this, "Failed to update appointment", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void deleteAppointment(String appointmentId) {
        if (currentUser == null || TextUtils.isEmpty(selectedChildId) || TextUtils.isEmpty(appointmentId)) {
            Log.e(TAG, "Cannot delete appointment: missing parameters");
            return;
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this,
                R.style.DatePickerDialogTheme);
        builder.setTitle("Delete Appointment");
        builder.setMessage("Are you sure you want to delete this appointment?");
        builder.setPositiveButton("Delete", (dialog, which) -> {
            Log.d(TAG, "Deleting appointment: " + appointmentId);
            mDatabase.child("users").child(currentUser.getUid())
                    .child("children").child(selectedChildId)
                    .child("appointments").child(appointmentId)
                    .removeValue()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Appointment deleted successfully");
                            Toast.makeText(this, "Appointment deleted", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e(TAG, "Failed to delete appointment: " + task.getException());
                            Toast.makeText(this, "Failed to delete appointment", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private long getDateTimestamp(String date, String time) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Date dateObj = sdf.parse(date + " " + time);
            return dateObj != null ? dateObj.getTime() : System.currentTimeMillis();
        } catch (Exception e) {
            Log.e(TAG, "Error parsing date: " + e.getMessage());
            return System.currentTimeMillis();
        }
    }

    private void clearForm() {
        spinnerAppointmentType.setSelection(0);
        edtDate.setText("");
        edtTime.setText("");
        edtFacility.setText("");
        edtDoctor.setText("");
        edtReason.setText("");
        tvAppointmentId.setText("");
        tvChildId.setText("");

        tilDate.setError(null);
        tilTime.setError(null);
        tilFacility.setError(null);
        tilDoctor.setError(null);
        tilReason.setError(null);
    }

    private void clearAppointmentsList() {
        appointmentsList.clear();
        appointmentAdapter.notifyDataSetChanged();
        llNoAppointments.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        tvAppointmentsTitle.setVisibility(View.GONE);
        Log.d(TAG, "Appointments list cleared");
    }

    // Child model
    public static class Child {
        private String id;
        private String name;
        private String gender;
        private String ageMonths;
        private String birthDate;

        public Child() {}

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
        public String getAgeMonths() { return ageMonths; }
        public void setAgeMonths(String ageMonths) { this.ageMonths = ageMonths; }
        public String getBirthDate() { return birthDate; }
        public void setBirthDate(String birthDate) { this.birthDate = birthDate; }
    }

    // ChildAppointment model
    public static class ChildAppointment {
        private String id;
        private String type;
        private String date;
        private String time;
        private String facility;
        private String doctor;
        private String reason;
        private String status;
        private String childName;
        private String childAge;
        private long dateTimestamp;
        private long createdAt;
        private long updatedAt;

        public ChildAppointment() {}

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }
        public String getFacility() { return facility; }
        public void setFacility(String facility) { this.facility = facility; }
        public String getDoctor() { return doctor; }
        public void setDoctor(String doctor) { this.doctor = doctor; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getChildName() { return childName; }
        public void setChildName(String childName) { this.childName = childName; }
        public String getChildAge() { return childAge; }
        public void setChildAge(String childAge) { this.childAge = childAge; }
        public long getDateTimestamp() { return dateTimestamp; }
        public void setDateTimestamp(long dateTimestamp) { this.dateTimestamp = dateTimestamp; }
        public long getCreatedAt() { return createdAt; }
        public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
        public long getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    }

    // ChildAppointment Adapter
    private class ChildAppointmentAdapter extends RecyclerView.Adapter<ChildAppointmentAdapter.ViewHolder> {

        private List<ChildAppointment> appointments;

        public ChildAppointmentAdapter(List<ChildAppointment> appointments) {
            this.appointments = appointments;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_child_appointment, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ChildAppointment appointment = appointments.get(position);
            holder.bind(appointment);
        }

        @Override
        public int getItemCount() {
            return appointments.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private TextView tvType, tvDateTime, tvFacility, tvChildName, tvStatus;
            private MaterialButton btnEdit, btnDelete;
            private MaterialCardView cardAppointment;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvType = itemView.findViewById(R.id.tvVaccine);
                tvDateTime = itemView.findViewById(R.id.tvDateTime);
                tvFacility = itemView.findViewById(R.id.tvFacility);
                tvChildName = itemView.findViewById(R.id.tvChildName);
                tvStatus = itemView.findViewById(R.id.tvStatus);
                btnEdit = itemView.findViewById(R.id.btnEdit);
                btnDelete = itemView.findViewById(R.id.btnDelete);
                cardAppointment = itemView.findViewById(R.id.cardAppointment);
            }

            public void bind(ChildAppointment appointment) {
                tvType.setText(appointment.getType());
                tvDateTime.setText(String.format("%s, %s", appointment.getDate(), appointment.getTime()));
                tvFacility.setText(appointment.getFacility());
                tvChildName.setText(appointment.getChildName());
                tvStatus.setText(appointment.getStatus());

                // Set status background
                int statusBg = R.drawable.bg_status_scheduled;
                switch (appointment.getStatus()) {
                    case "Completed":
                        statusBg = R.drawable.bg_status_completed;
                        break;
                    case "Cancelled":
                        statusBg = R.drawable.bg_status_cancelled;
                        break;
                    case "Missed":
                        statusBg = R.drawable.bg_status_missed;
                        break;
                }
                tvStatus.setBackgroundResource(statusBg);

                // Set click listeners
                btnEdit.setOnClickListener(v -> showEditForm(appointment));
                btnDelete.setOnClickListener(v -> deleteAppointment(appointment.getId()));

                itemView.setOnClickListener(v -> showAppointmentDetails(appointment));
            }
        }
    }

    private void showAppointmentDetails(ChildAppointment appointment) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Appointment Details");

        StringBuilder message = new StringBuilder();
        message.append("📋 Type: ").append(appointment.getType()).append("\n\n");
        message.append("📅 Date: ").append(appointment.getDate()).append("\n");
        message.append("⏰ Time: ").append(appointment.getTime()).append("\n");
        message.append("🏥 Facility: ").append(appointment.getFacility()).append("\n");
        if (!TextUtils.isEmpty(appointment.getDoctor())) {
            message.append("👨‍⚕️ Doctor: ").append(appointment.getDoctor()).append("\n");
        }
        message.append("👶 Child: ").append(appointment.getChildName()).append("\n");
        message.append("📊 Status: ").append(appointment.getStatus()).append("\n");
        if (!TextUtils.isEmpty(appointment.getReason())) {
            message.append("\n📝 Reason: ").append(appointment.getReason());
        }

        builder.setMessage(message.toString());
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());

        if (!"Completed".equals(appointment.getStatus()) && !"Cancelled".equals(appointment.getStatus())) {
            builder.setNeutralButton("Edit", (dialog, which) -> showEditForm(appointment));
        }

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Activity resumed");
        if (!TextUtils.isEmpty(selectedChildId)) {
            loadAppointments(selectedChildId);
        }
    }
}