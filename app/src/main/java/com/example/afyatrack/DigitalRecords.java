package com.example.afyatrack;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;

public class DigitalRecords extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FirebaseUser currentUser;

    // UI Components
    private MaterialToolbar toolbar;
    private Spinner spinnerChildren;
    private LinearLayout llLoading, llRecordsContainer, llEmptyState, llVaccinationRecords;
    private TextView tvNoVaccinations, tvReportTitle, tvReportDate;
    private TextView tvChildName, tvChildGender, tvChildAge, tvChildDOB, tvBirthWeight, tvBirthPlace, tvGuardian;
    private TextView tvHealthConditions, tvDoctorRecommendations, tvMedicalHistory;
    private MaterialButton btnDownload, btnShare;

    // Data
    private List<Child> childrenList = new ArrayList<>();
    private List<VaccinationRecord> vaccinationRecords = new ArrayList<>();
    private String selectedChildId = "";
    private Child currentChild;
    private static final String TAG = "DigitalRecords";

    // Permissions
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int PERMISSION_REQUEST_MANAGE_STORAGE = 101;
    private static final int REQUEST_CODE_MANAGE_STORAGE = 102;

    // Date formats
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final SimpleDateFormat reportDateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_digital_records);

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

        initializeViews();
        setupUI();
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
        llLoading = findViewById(R.id.llLoading);
        llRecordsContainer = findViewById(R.id.llRecordsContainer);
        llEmptyState = findViewById(R.id.llEmptyState);
        llVaccinationRecords = findViewById(R.id.llVaccinationRecords);

        tvNoVaccinations = findViewById(R.id.tvNoVaccinations);
        tvReportTitle = findViewById(R.id.tvReportTitle);
        tvReportDate = findViewById(R.id.tvReportDate);

        // Child profile fields
        tvChildName = findViewById(R.id.tvChildName);
        tvChildGender = findViewById(R.id.tvChildGender);
        tvChildAge = findViewById(R.id.tvChildAge);
        tvChildDOB = findViewById(R.id.tvChildDOB);
        tvBirthWeight = findViewById(R.id.tvBirthWeight);
        tvBirthPlace = findViewById(R.id.tvBirthPlace);
        tvGuardian = findViewById(R.id.tvGuardian);

        // Health information fields
        tvHealthConditions = findViewById(R.id.tvHealthConditions);
        tvDoctorRecommendations = findViewById(R.id.tvDoctorRecommendations);
        tvMedicalHistory = findViewById(R.id.tvMedicalHistory);

        // Action buttons
        btnDownload = findViewById(R.id.btnDownload);
        btnShare = findViewById(R.id.btnShare);
    }

    private void setupUI() {

        // Set current date for report
        String currentDate = reportDateFormat.format(new Date());
        tvReportDate.setText("Generated: " + currentDate);

        // Setup child selection listener
        spinnerChildren.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "Child selected at position: " + position);
                if (position > 0 && position - 1 < childrenList.size()) {
                    Child selectedChild = childrenList.get(position - 1);
                    selectedChildId = selectedChild.getId();
                    currentChild = selectedChild;
                    loadChildRecords(selectedChild);
                } else {
                    selectedChildId = "";
                    currentChild = null;
                    showEmptyState();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedChildId = "";
                currentChild = null;
                showEmptyState();
            }
        });

        // Setup download button
        btnDownload.setOnClickListener(v -> {
            if (TextUtils.isEmpty(selectedChildId)) {
                Toast.makeText(this, "Please select a child first", Toast.LENGTH_SHORT).show();
                return;
            }
            checkStoragePermissionAndDownload();
        });

        // Setup share button
        btnShare.setOnClickListener(v -> {
            if (TextUtils.isEmpty(selectedChildId)) {
                Toast.makeText(this, "Please select a child first", Toast.LENGTH_SHORT).show();
                return;
            }
            shareHealthRecord();
        });
    }

    private void checkStoragePermissionAndDownload() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ - Use MediaStore API
            // No permission needed for saving to Downloads directory using MediaStore
            generateAndDownloadPDF();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11-12 - Check for MANAGE_EXTERNAL_STORAGE permission
            if (Environment.isExternalStorageManager()) {
                generateAndDownloadPDF();
            } else {
                requestManageExternalStoragePermission();
            }
        } else {
            // Android 10 and below - Use WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                generateAndDownloadPDF();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            }
        }
    }

    private void requestManageExternalStoragePermission() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this,
                R.style.DatePickerDialogTheme);
        builder.setTitle("Storage Permission Required");
        builder.setMessage("To save PDF files to your device, we need permission to manage storage. Please grant this permission in the next screen.");
        builder.setPositiveButton("Continue", (dialog, which) -> {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_MANAGE_STORAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    generateAndDownloadPDF();
                } else {
                    Toast.makeText(this, "Storage permission is required to download PDF", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                generateAndDownloadPDF();
            } else {
                Toast.makeText(this, "Storage permission is required to download PDF", Toast.LENGTH_SHORT).show();
            }
        }
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

                                    // Extract additional fields from the database structure
                                    if (childSnapshot.child("dateOfBirth").exists()) {
                                        child.setDateOfBirth(childSnapshot.child("dateOfBirth").getValue(String.class));
                                    }
                                    if (childSnapshot.child("gender").exists()) {
                                        child.setGender(childSnapshot.child("gender").getValue(String.class));
                                    }
                                    if (childSnapshot.child("birthWeight").exists()) {
                                        child.setBirthWeight(childSnapshot.child("birthWeight").getValue(String.class));
                                    }
                                    if (childSnapshot.child("birthPlace").exists()) {
                                        child.setBirthPlace(childSnapshot.child("birthPlace").getValue(String.class));
                                    }
                                    if (childSnapshot.child("guardianName").exists()) {
                                        child.setGuardianName(childSnapshot.child("guardianName").getValue(String.class));
                                    }
                                    if (childSnapshot.child("guardianContact").exists()) {
                                        child.setGuardianContact(childSnapshot.child("guardianContact").getValue(String.class));
                                    }

                                    // Get name from child data
                                    String name = childSnapshot.child("name").exists() ?
                                            childSnapshot.child("name").getValue(String.class) :
                                            "Unnamed Child";
                                    child.setName(name);

                                    childrenList.add(child);
                                    childNames.add(name);
                                    Log.d(TAG, "Child loaded: " + name);
                                }
                            }
                        } else {
                            Log.d(TAG, "No children found in database");
                        }

                        // Setup spinner
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                DigitalRecords.this,
                                android.R.layout.simple_spinner_item,
                                childNames
                        );
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerChildren.setAdapter(adapter);

                        Log.d(TAG, "Spinner populated with " + childNames.size() + " items");

                        // Show empty state initially
                        showEmptyState();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to load children: " + error.getMessage());
                        Toast.makeText(DigitalRecords.this,
                                "Failed to load children", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadChildRecords(Child child) {
        if (child == null) return;

        Log.d(TAG, "Loading records for child: " + child.getName());

        // Show loading state
        llLoading.setVisibility(View.VISIBLE);
        llRecordsContainer.setVisibility(View.GONE);
        llEmptyState.setVisibility(View.GONE);

        // Update child profile
        updateChildProfile(child);

        // Load child-specific data
        loadChildDataFromDatabase(child.getId());
    }

    private void updateChildProfile(Child child) {
        tvChildName.setText(!TextUtils.isEmpty(child.getName()) ? child.getName() : "Not specified");
        tvChildGender.setText(!TextUtils.isEmpty(child.getGender()) ? child.getGender() : "Not specified");

        // Calculate and display age from date of birth
        String ageText = calculateAgeFromDOB(child.getDateOfBirth());
        tvChildAge.setText(ageText);

        tvChildDOB.setText(!TextUtils.isEmpty(child.getDateOfBirth()) ? child.getDateOfBirth() : "Not specified");
        tvBirthWeight.setText(!TextUtils.isEmpty(child.getBirthWeight()) ? child.getBirthWeight() + " kg" : "Not specified");
        tvBirthPlace.setText(!TextUtils.isEmpty(child.getBirthPlace()) ? child.getBirthPlace() : "Not specified");

        // Format guardian info
        String guardianInfo = "";
        if (!TextUtils.isEmpty(child.getGuardianName())) {
            guardianInfo = child.getGuardianName();
            if (!TextUtils.isEmpty(child.getGuardianContact())) {
                guardianInfo += " (" + child.getGuardianContact() + ")";
            }
        }
        tvGuardian.setText(!TextUtils.isEmpty(guardianInfo) ? guardianInfo : "Not specified");

        // Update report title
        String childName = !TextUtils.isEmpty(child.getName()) ? child.getName() : "Child";
        String reportTitle = childName.toUpperCase() + "'S - BIO DATA";
        tvReportTitle.setText(reportTitle);
    }

    private String calculateAgeFromDOB(String dob) {
        if (TextUtils.isEmpty(dob)) {
            return "Not specified";
        }

        try {
            Date birthDate = dateFormat.parse(dob);
            if (birthDate == null) {
                return "Invalid date format";
            }

            Calendar dobCal = Calendar.getInstance();
            dobCal.setTime(birthDate);
            Calendar today = Calendar.getInstance();

            int years = today.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR);
            int months = today.get(Calendar.MONTH) - dobCal.get(Calendar.MONTH);

            // Adjust for negative months
            if (months < 0) {
                years--;
                months += 12;
            }

            // Adjust for days
            int days = today.get(Calendar.DAY_OF_MONTH) - dobCal.get(Calendar.DAY_OF_MONTH);
            if (days < 0) {
                months--;
                // Get days in previous month
                Calendar temp = (Calendar) dobCal.clone();
                temp.add(Calendar.MONTH, months + 1);
                int daysInMonth = temp.getActualMaximum(Calendar.DAY_OF_MONTH);
                days += daysInMonth;
            }

            if (years > 0) {
                return years + " years, " + months + " months";
            } else if (months > 0) {
                return months + " months, " + days + " days";
            } else {
                return days + " days";
            }

        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date: " + dob, e);
            return "Invalid date";
        }
    }

    private void loadChildDataFromDatabase(String childId) {
        if (currentUser == null || TextUtils.isEmpty(childId)) return;

        DatabaseReference childRef = mDatabase.child("users").child(currentUser.getUid())
                .child("children").child(childId);

        childRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Hide loading
                llLoading.setVisibility(View.GONE);
                llRecordsContainer.setVisibility(View.VISIBLE);

                if (snapshot.exists()) {
                    // Load vaccination records
                    loadVaccinationRecords(childId);

                    // Load health conditions
                    String healthConditions = snapshot.child("healthConditions").getValue(String.class);
                    if (!TextUtils.isEmpty(healthConditions)) {
                        tvHealthConditions.setText(healthConditions);
                    } else {
                        tvHealthConditions.setText("No known health conditions. Allergies: Not specified.");
                    }

                    // Load doctor recommendations
                    String recommendations = snapshot.child("doctorRecommendations").getValue(String.class);
                    if (!TextUtils.isEmpty(recommendations)) {
                        tvDoctorRecommendations.setText(recommendations);
                    } else {
                        tvDoctorRecommendations.setText("No specific recommendations from doctor at this time.");
                    }

                    // Load medical history
                    String medicalHistory = snapshot.child("medicalHistory").getValue(String.class);
                    if (!TextUtils.isEmpty(medicalHistory)) {
                        tvMedicalHistory.setText(medicalHistory);
                    } else {
                        tvMedicalHistory.setText("Routine checkups only. No significant medical history recorded.");
                    }
                } else {
                    // Set default values
                    setDefaultValues();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                llLoading.setVisibility(View.GONE);
                Log.e(TAG, "Failed to load child data: " + error.getMessage());
                Toast.makeText(DigitalRecords.this,
                        "Failed to load child data", Toast.LENGTH_SHORT).show();
                setDefaultValues();
            }
        });
    }

    private void loadVaccinationRecords(String childId) {
        if (currentUser == null || TextUtils.isEmpty(childId)) return;

        DatabaseReference vaccinesRef = mDatabase.child("users").child(currentUser.getUid())
                .child("children").child(childId).child("vaccinations");

        vaccinesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                llVaccinationRecords.removeAllViews();
                vaccinationRecords.clear();

                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    tvNoVaccinations.setVisibility(View.GONE);

                    int count = 0;
                    for (DataSnapshot vaccineSnapshot : snapshot.getChildren()) {
                        VaccinationRecord record = vaccineSnapshot.getValue(VaccinationRecord.class);
                        if (record != null) {
                            vaccinationRecords.add(record);
                            addVaccinationRecordItem(record, ++count);
                        }
                    }
                } else {
                    tvNoVaccinations.setVisibility(View.VISIBLE);
                    addNoVaccinationMessage();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load vaccination records: " + error.getMessage());
                tvNoVaccinations.setVisibility(View.VISIBLE);
                addNoVaccinationMessage();
            }
        });
    }

    private void addVaccinationRecordItem(VaccinationRecord record, int count) {
        View itemView = LayoutInflater.from(this)
                .inflate(R.layout.item_vaccination_record, llVaccinationRecords, false);

        TextView tvVaccineName = itemView.findViewById(R.id.tvVaccineName);
        TextView tvAdministeredDate = itemView.findViewById(R.id.tvAdministeredDate);
        TextView tvHealthFacility = itemView.findViewById(R.id.tvHealthFacility);
        TextView tvDoctorName = itemView.findViewById(R.id.tvDoctorName);
        TextView tvCount = itemView.findViewById(R.id.tvCount);

        tvCount.setText(String.valueOf(count));
        tvVaccineName.setText(!TextUtils.isEmpty(record.getVaccineName()) ? record.getVaccineName() : "Not specified");
        tvAdministeredDate.setText(!TextUtils.isEmpty(record.getDate()) ? record.getDate() : "Date not recorded");
        tvHealthFacility.setText(!TextUtils.isEmpty(record.getFacility()) ? record.getFacility() : "Facility not specified");
        tvDoctorName.setText(!TextUtils.isEmpty(record.getDoctor()) ? "Dr. " + record.getDoctor() : "Doctor not specified");

        // Set different background for alternate rows
        if (count % 2 == 0) {
            itemView.setBackgroundResource(R.drawable.bg_vaccination_item);
        }

        llVaccinationRecords.addView(itemView);
    }

    private void addNoVaccinationMessage() {
        View itemView = LayoutInflater.from(this)
                .inflate(R.layout.item_vaccination_record_empty, llVaccinationRecords, false);
        llVaccinationRecords.addView(itemView);
    }

    private void setDefaultValues() {
        tvHealthConditions.setText("No known health conditions. Allergies: Not specified.");
        tvDoctorRecommendations.setText("No specific recommendations from doctor at this time.");
        tvMedicalHistory.setText("Routine checkups only. No significant medical history recorded.");
    }

    private void showEmptyState() {
        llLoading.setVisibility(View.GONE);
        llRecordsContainer.setVisibility(View.GONE);
        llEmptyState.setVisibility(View.VISIBLE);
    }

    // Replace the generateAndDownloadPDF() method:

    private void generateAndDownloadPDF() {
        if (currentChild == null) {
            Toast.makeText(this, "Please select a child first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress dialog
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Generating PDF...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Generate PDF in background thread
        new Thread(() -> {
            try {
                // Save PDF to file
                File pdfFile = generateProperPDF();

                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    if (pdfFile != null && pdfFile.exists()) {
                        showDownloadSuccessDialog(pdfFile);
                    } else {
                        Toast.makeText(DigitalRecords.this,
                                "Failed to generate PDF", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error generating PDF", e);
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(DigitalRecords.this,
                            "Error generating PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private File generateProperPDF() {
        try {
            File pdfFile;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10+, use app-specific directory
                File appSpecificDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
                if (appSpecificDir == null) {
                    appSpecificDir = getFilesDir();
                }

                String childName = currentChild != null ? currentChild.getName().replaceAll("[^a-zA-Z0-9]", "_") : "Child";
                String fileName = "Health_Record_" + childName + "_" +
                        new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".pdf";

                pdfFile = new File(appSpecificDir, fileName);
            } else {
                // For Android 9 and below, use Downloads directory with permission
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs();
                }

                String childName = currentChild != null ? currentChild.getName().replaceAll("[^a-zA-Z0-9]", "_") : "Child";
                String fileName = "Health_Record_" + childName + "_" +
                        new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".pdf";

                pdfFile = new File(downloadsDir, fileName);
            }

            // Create PDF writer
            PdfWriter writer = new PdfWriter(pdfFile);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Create fonts
            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont normalFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            // Header
            Paragraph header = new Paragraph("CHILD HEALTH RECORD")
                    .setFont(boldFont)
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(header);

            // Generated date
            Paragraph date = new Paragraph("Generated on: " + reportDateFormat.format(new Date()))
                    .setFont(normalFont)
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(30);
            document.add(date);

            // Child Profile Section
            Paragraph profileHeader = new Paragraph("CHILD PROFILE")
                    .setFont(boldFont)
                    .setFontSize(14)
                    .setMarginBottom(10);
            document.add(profileHeader);

            // Create table for child profile
            Table profileTable = new Table(UnitValue.createPercentArray(new float[]{40, 60}));
            profileTable.setWidth(UnitValue.createPercentValue(100));

            addTableRow(profileTable, "Full Name:", tvChildName.getText().toString(), normalFont);
            addTableRow(profileTable, "Gender:", tvChildGender.getText().toString(), normalFont);
            addTableRow(profileTable, "Age:", tvChildAge.getText().toString(), normalFont);
            addTableRow(profileTable, "Date of Birth:", tvChildDOB.getText().toString(), normalFont);
            addTableRow(profileTable, "Birth Weight:", tvBirthWeight.getText().toString(), normalFont);
            addTableRow(profileTable, "Birth Place:", tvBirthPlace.getText().toString(), normalFont);
            addTableRow(profileTable, "Guardian:", tvGuardian.getText().toString(), normalFont);

            document.add(profileTable);
            document.add(new Paragraph("\n"));

            // Vaccination History Section
            Paragraph vaccineHeader = new Paragraph("VACCINATION HISTORY")
                    .setFont(boldFont)
                    .setFontSize(14)
                    .setMarginBottom(10);
            document.add(vaccineHeader);

            if (vaccinationRecords.isEmpty()) {
                document.add(new Paragraph("No vaccination records available")
                        .setFont(normalFont)
                        .setFontSize(12)
                        .setMarginBottom(20));
            } else {
                Table vaccineTable = new Table(UnitValue.createPercentArray(new float[]{10, 30, 20, 20, 20}));
                vaccineTable.setWidth(UnitValue.createPercentValue(100));

                // Table headers
                vaccineTable.addHeaderCell(new Cell().add(new Paragraph("#").setFont(boldFont)));
                vaccineTable.addHeaderCell(new Cell().add(new Paragraph("Vaccine").setFont(boldFont)));
                vaccineTable.addHeaderCell(new Cell().add(new Paragraph("Date").setFont(boldFont)));
                vaccineTable.addHeaderCell(new Cell().add(new Paragraph("Facility").setFont(boldFont)));
                vaccineTable.addHeaderCell(new Cell().add(new Paragraph("Doctor").setFont(boldFont)));

                // Table rows
                for (int i = 0; i < vaccinationRecords.size(); i++) {
                    VaccinationRecord record = vaccinationRecords.get(i);
                    vaccineTable.addCell(new Cell().add(new Paragraph(String.valueOf(i + 1)).setFont(normalFont)));
                    vaccineTable.addCell(new Cell().add(new Paragraph(
                            !TextUtils.isEmpty(record.getVaccineName()) ? record.getVaccineName() : "Not specified")
                            .setFont(normalFont)));
                    vaccineTable.addCell(new Cell().add(new Paragraph(
                            !TextUtils.isEmpty(record.getDate()) ? record.getDate() : "Not recorded")
                            .setFont(normalFont)));
                    vaccineTable.addCell(new Cell().add(new Paragraph(
                            !TextUtils.isEmpty(record.getFacility()) ? record.getFacility() : "Not specified")
                            .setFont(normalFont)));
                    vaccineTable.addCell(new Cell().add(new Paragraph(
                            !TextUtils.isEmpty(record.getDoctor()) ? record.getDoctor() : "Not specified")
                            .setFont(normalFont)));
                }

                document.add(vaccineTable);
            }
            document.add(new Paragraph("\n"));

            // Health Conditions Section
            Paragraph conditionsHeader = new Paragraph("HEALTH CONDITIONS")
                    .setFont(boldFont)
                    .setFontSize(14)
                    .setMarginBottom(10);
            document.add(conditionsHeader);

            document.add(new Paragraph(tvHealthConditions.getText().toString())
                    .setFont(normalFont)
                    .setFontSize(12)
                    .setMarginBottom(20));

            // Doctor's Recommendations Section
            Paragraph recommendationsHeader = new Paragraph("DOCTOR'S RECOMMENDATIONS")
                    .setFont(boldFont)
                    .setFontSize(14)
                    .setMarginBottom(10);
            document.add(recommendationsHeader);

            document.add(new Paragraph(tvDoctorRecommendations.getText().toString())
                    .setFont(normalFont)
                    .setFontSize(12)
                    .setMarginBottom(20));

            // Medical History Section
            Paragraph historyHeader = new Paragraph("OTHER MEDICAL HISTORY")
                    .setFont(boldFont)
                    .setFontSize(14)
                    .setMarginBottom(10);
            document.add(historyHeader);

            document.add(new Paragraph(tvMedicalHistory.getText().toString())
                    .setFont(normalFont)
                    .setFontSize(12)
                    .setMarginBottom(20));

            // Footer
            Paragraph footer = new Paragraph("--- END OF REPORT ---\nGenerated by MamaCare App")
                    .setFont(normalFont)
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(30);
            document.add(footer);

            // Close document
            document.close();

            // If using app-specific directory on Android 10+, add to MediaStore
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                addFileToMediaStore(pdfFile);
            }

            Log.d(TAG, "PDF generated successfully at: " + pdfFile.getAbsolutePath());
            return pdfFile;

        } catch (Exception e) {
            Log.e(TAG, "Error generating PDF", e);
            e.printStackTrace();
            return null;
        }
    }

    private void addTableRow(Table table, String label, String value, PdfFont font) {
        table.addCell(new Cell().add(new Paragraph(label).setFont(font)));
        table.addCell(new Cell().add(new Paragraph(value).setFont(font)));
    }

    @SuppressLint("Range")
    private void addFileToMediaStore(File file) {
        // This is a simplified version. In a real app, you'd use MediaStore API
        // to add the file to the Downloads collection
        try {
            // Notify system about new file
            android.media.MediaScannerConnection.scanFile(this,
                    new String[]{file.getAbsolutePath()},
                    new String[]{"application/pdf"},
                    null);
        } catch (Exception e) {
            Log.e(TAG, "Error adding file to MediaStore", e);
        }
    }

    private void showDownloadSuccessDialog(File pdfFile) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this,
                R.style.DatePickerDialogTheme);
        builder.setTitle("PDF Generated Successfully");

        String message;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            message = "Health record has been saved as:\n" + pdfFile.getName();
        } else {
            message = "Health record has been saved as:\n" + pdfFile.getName();
        }

        builder.setMessage(message);

        builder.setPositiveButton("Open File", (dialog, which) -> openPDFFile(pdfFile));
        builder.setNegativeButton("Share", (dialog, which) -> sharePDFFile(pdfFile));
        builder.setNeutralButton("OK", null);

        builder.show();
    }

    private void openPDFFile(File pdfFile) {
        try {
            Uri fileUri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".provider",
                    pdfFile);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

            // Verify there's an app to handle PDF files
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                // No PDF viewer, share instead
                sharePDFFile(pdfFile);
            }
        } catch (Exception e) {
            Toast.makeText(this, "No PDF viewer app found", Toast.LENGTH_SHORT).show();
            // Open with any text viewer or share
            sharePDFFile(pdfFile);
        }
    }

    private void sharePDFFile(File pdfFile) {
        try {
            Uri fileUri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".provider",
                    pdfFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Health Record - " + tvChildName.getText());
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Health record for " + tvChildName.getText() +
                    "\n\nGenerated by AfyaTrack App");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share Health Record via"));
        } catch (Exception e) {
            Log.e(TAG, "Error sharing PDF", e);
            Toast.makeText(this, "Error sharing file", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareHealthRecord() {
        if (currentChild == null) {
            Toast.makeText(this, "Please select a child first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create shareable text content
        StringBuilder shareContent = new StringBuilder();
        shareContent.append("*CHILD HEALTH RECORD*\n\n");

        shareContent.append("*Child Profile:*\n");
        shareContent.append("Name: ").append(tvChildName.getText()).append("\n");
        shareContent.append("Gender: ").append(tvChildGender.getText()).append("\n");
        shareContent.append("Age: ").append(tvChildAge.getText()).append("\n");
        shareContent.append("Date of Birth: ").append(tvChildDOB.getText()).append("\n\n");

        shareContent.append("*Health Conditions:*\n");
        shareContent.append(tvHealthConditions.getText()).append("\n\n");

        shareContent.append("*Doctor's Recommendations:*\n");
        shareContent.append(tvDoctorRecommendations.getText()).append("\n\n");

        shareContent.append("*Medical History:*\n");
        shareContent.append(tvMedicalHistory.getText()).append("\n\n");

        shareContent.append("_Generated by AfyaTrack App_");

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Health Record - " + tvChildName.getText());
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareContent.toString());

        startActivity(Intent.createChooser(shareIntent, "Share Health Record"));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Child model - Updated to match your database structure
    public static class Child {
        private String id;
        private String name;
        private String gender;
        private String dateOfBirth;
        private String birthWeight;
        private String birthPlace;
        private String guardianName;
        private String guardianContact;

        public Child() {}

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }

        public String getDateOfBirth() { return dateOfBirth; }
        public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

        public String getBirthWeight() { return birthWeight; }
        public void setBirthWeight(String birthWeight) { this.birthWeight = birthWeight; }

        public String getBirthPlace() { return birthPlace; }
        public void setBirthPlace(String birthPlace) { this.birthPlace = birthPlace; }

        public String getGuardianName() { return guardianName; }
        public void setGuardianName(String guardianName) { this.guardianName = guardianName; }

        public String getGuardianContact() { return guardianContact; }
        public void setGuardianContact(String guardianContact) { this.guardianContact = guardianContact; }
    }

    // Vaccination Record model
    public static class VaccinationRecord {
        private String vaccineName;
        private String date;
        private String facility;
        private String doctor;
        private String batchNumber;
        private String notes;

        public VaccinationRecord() {}

        public String getVaccineName() { return vaccineName; }
        public void setVaccineName(String vaccineName) { this.vaccineName = vaccineName; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getFacility() { return facility; }
        public void setFacility(String facility) { this.facility = facility; }
        public String getDoctor() { return doctor; }
        public void setDoctor(String doctor) { this.doctor = doctor; }
        public String getBatchNumber() { return batchNumber; }
        public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }
}