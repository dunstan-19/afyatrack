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
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ANCRecordsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FirebaseUser currentUser;

    // UI Components
    private MaterialToolbar toolbar;
    private LinearLayout llLoading, llRecordsContainer;
    private TextView tvReportTitle, tvReportDate;
    private TextView tvMotherName, tvMotherAge, tvMotherContact, tvEmergencyContact;
    private TextView tvGestationalAge, tvEDD, tvLMP, tvParity;
    private TextView tvMedicalConditions, tvComplications, tvAllergies;
    private TextView tvRecommendations;
    private TextView tvNextAppointmentDate, tvNextAppointmentTime, tvNextAppointmentPurpose;
    private TextView tvNoVitalSigns, tvNoANCVisits;
    private LinearLayout llTrimesterProgress, llVitalSigns, llANCVisits;
    private MaterialButton btnDownload, btnShare;

    // Data
    private ANCMother currentMother;
    private List<ANCVisit> ancVisits = new ArrayList<>();
    private List<VitalSign> vitalSigns = new ArrayList<>();
    private static final String TAG = "ANCRecordsActivity";

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
        setContentView(R.layout.activity_ancrecords);

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
        loadMotherRecords();
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
        llLoading = findViewById(R.id.llLoading);
        llRecordsContainer = findViewById(R.id.llRecordsContainer);

        tvReportTitle = findViewById(R.id.tvReportTitle);
        tvReportDate = findViewById(R.id.tvReportDate);

        // Mother profile fields
        tvMotherName = findViewById(R.id.tvMotherName);
        tvMotherAge = findViewById(R.id.tvMotherAge);
        tvMotherContact = findViewById(R.id.tvMotherContact);
        tvEmergencyContact = findViewById(R.id.tvEmergencyContact);

        // Pregnancy progress fields
        tvGestationalAge = findViewById(R.id.tvGestationalAge);
        tvEDD = findViewById(R.id.tvEDD);
        tvLMP = findViewById(R.id.tvLMP);
        tvParity = findViewById(R.id.tvParity);
        llTrimesterProgress = findViewById(R.id.llTrimesterProgress);

        // Medical history fields
        tvMedicalConditions = findViewById(R.id.tvMedicalConditions);
        tvComplications = findViewById(R.id.tvComplications);
        tvAllergies = findViewById(R.id.tvAllergies);

        // Vital signs
        llVitalSigns = findViewById(R.id.llVitalSigns);
        tvNoVitalSigns = findViewById(R.id.tvNoVitalSigns);

        // ANC visits
        llANCVisits = findViewById(R.id.llANCVisits);
        tvNoANCVisits = findViewById(R.id.tvNoANCVisits);

        // Recommendations
        tvRecommendations = findViewById(R.id.tvRecommendations);

        // Next appointment
        tvNextAppointmentDate = findViewById(R.id.tvNextAppointmentDate);
        tvNextAppointmentTime = findViewById(R.id.tvNextAppointmentTime);
        tvNextAppointmentPurpose = findViewById(R.id.tvNextAppointmentPurpose);

        // Action buttons
        btnDownload = findViewById(R.id.btnDownload);
        btnShare = findViewById(R.id.btnShare);
    }

    private void setupUI() {
        toolbar.setTitle("Antenatal Records");

        // Set current date for report
        String currentDate = reportDateFormat.format(new Date());
        tvReportDate.setText("Generated: " + currentDate);

        // Setup download button
        btnDownload.setOnClickListener(v -> checkStoragePermissionAndDownload());

        // Setup share button
        btnShare.setOnClickListener(v -> shareANCReport());
    }

    private void loadMotherRecords() {
        if (currentUser == null) return;

        Log.d(TAG, "Loading mother records for user: " + currentUser.getUid());

        // Show loading state
        llLoading.setVisibility(View.VISIBLE);
        llRecordsContainer.setVisibility(View.GONE);

        // Load mother registration data
        loadMotherRegistrationData();

        // Load ANC visits
        loadANCVisits();

        // Load vital signs
        loadVitalSigns();
    }

    private void loadMotherRegistrationData() {
        DatabaseReference ancRef = mDatabase.child("users").child(currentUser.getUid()).child("anc_registration");

        ancRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentMother = snapshot.getValue(ANCMother.class);
                    if (currentMother != null) {
                        updateMotherProfile(currentMother);
                    } else {
                        setDefaultValues();
                    }
                } else {
                    Log.d(TAG, "No ANC registration found");
                    setDefaultValues();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load mother data: " + error.getMessage());
                setDefaultValues();
            }
        });
    }

    private void updateMotherProfile(ANCMother mother) {
        // Update mother profile
        tvMotherName.setText(!TextUtils.isEmpty(mother.getName()) ? mother.getName() : "Not specified");
        tvMotherAge.setText(!TextUtils.isEmpty(mother.getAge()) ? mother.getAge() + " years" : "Not specified");
        tvMotherContact.setText(!TextUtils.isEmpty(mother.getPhone()) ? mother.getPhone() : "Not specified");
        tvEmergencyContact.setText(!TextUtils.isEmpty(mother.getEmergencyContact()) ? mother.getEmergencyContact() : "Not specified");

        // Update pregnancy progress
        tvGestationalAge.setText(!TextUtils.isEmpty(mother.getWeeksPregnant()) ? mother.getWeeksPregnant() + " weeks" : "Not specified");
        tvEDD.setText(!TextUtils.isEmpty(mother.getEdd()) ? mother.getEdd() : "Not specified");
        tvLMP.setText(!TextUtils.isEmpty(mother.getLmp()) ? mother.getLmp() : "Not specified");
        tvParity.setText(!TextUtils.isEmpty(mother.getParity()) ? mother.getParity() : "Not specified");

        // Update medical history
        tvMedicalConditions.setText(!TextUtils.isEmpty(mother.getMedicalConditions()) ? mother.getMedicalConditions() : "No known pre-existing conditions");
        tvComplications.setText(!TextUtils.isEmpty(mother.getComplications()) ? mother.getComplications() : "No complications reported");
        tvAllergies.setText(!TextUtils.isEmpty(mother.getAllergies()) ? mother.getAllergies() : "No known allergies");

        // Update recommendations
        tvRecommendations.setText(!TextUtils.isEmpty(mother.getRecommendations()) ? mother.getRecommendations() : "No specific recommendations at this time");

        // Update report title
        String motherName = !TextUtils.isEmpty(mother.getName()) ? mother.getName() : "Mother";
        String reportTitle = motherName.toUpperCase() + " - ANTENATAL CARE RECORD";
        tvReportTitle.setText(reportTitle);

        // Update trimester progress
        updateTrimesterProgress(mother.getWeeksPregnant());

        // Hide loading and show content
        llLoading.setVisibility(View.GONE);
        llRecordsContainer.setVisibility(View.VISIBLE);
    }

    private void updateTrimesterProgress(String weeksPregnant) {
        llTrimesterProgress.removeAllViews();

        if (TextUtils.isEmpty(weeksPregnant)) {
            return;
        }

        try {
            int weeks = Integer.parseInt(weeksPregnant);
            String[] trimesters = {
                    "First Trimester (1-12 weeks)",
                    "Second Trimester (13-27 weeks)",
                    "Third Trimester (28-40+ weeks)"
            };

            String[] trimesterDetails = {
                    "Focus: Confirmation of pregnancy, initial tests, managing early symptoms",
                    "Focus: Anatomy scan, glucose screening, feeling baby movements",
                    "Focus: Growth monitoring, birth planning, preparation for delivery"
            };

            for (int i = 0; i < trimesters.length; i++) {
                View trimesterView = LayoutInflater.from(this)
                        .inflate(R.layout.item_trimester_progress, llTrimesterProgress, false);

                TextView tvTrimesterName = trimesterView.findViewById(R.id.tvTrimesterName);
                TextView tvTrimesterStatus = trimesterView.findViewById(R.id.tvTrimesterStatus);
                TextView tvTrimesterDetails = trimesterView.findViewById(R.id.tvTrimesterDetails);
                ProgressBar progressBar = trimesterView.findViewById(R.id.progressBar);

                tvTrimesterName.setText(trimesters[i]);
                tvTrimesterDetails.setText(trimesterDetails[i]);

                // Determine trimester status
                int startWeek = i == 0 ? 1 : (i == 1 ? 13 : 28);
                int endWeek = i == 0 ? 12 : (i == 1 ? 27 : 40);

                if (weeks < startWeek) {
                    tvTrimesterStatus.setText("Upcoming");
                    progressBar.setProgress(0);
                    progressBar.setSecondaryProgress(0);
                } else if (weeks > endWeek) {
                    tvTrimesterStatus.setText("Completed");
                    progressBar.setProgress(100);
                    progressBar.setSecondaryProgress(100);
                } else {
                    // In this trimester
                    int progress = ((weeks - startWeek + 1) * 100) / (endWeek - startWeek + 1);
                    tvTrimesterStatus.setText("Week " + weeks + " (" + progress + "%)");
                    progressBar.setProgress(progress);
                    progressBar.setSecondaryProgress(progress);
                }

                llTrimesterProgress.addView(trimesterView);
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid weeks pregnant value: " + weeksPregnant);
        }
    }

    private void loadANCVisits() {
        if (currentUser == null) return;

        DatabaseReference visitsRef = mDatabase.child("users").child(currentUser.getUid()).child("anc_visits");

        visitsRef.orderByChild("visitDate").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                llANCVisits.removeAllViews();
                ancVisits.clear();

                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    tvNoANCVisits.setVisibility(View.GONE);

                    int count = 0;
                    for (DataSnapshot visitSnapshot : snapshot.getChildren()) {
                        ANCVisit visit = visitSnapshot.getValue(ANCVisit.class);
                        if (visit != null) {
                            ancVisits.add(visit);
                            addANCVisitItem(visit, ++count);
                        }
                    }

                    // Sort by date (newest first)
                    Collections.sort(ancVisits, (v1, v2) -> {
                        try {
                            Date date1 = dateFormat.parse(v1.getVisitDate());
                            Date date2 = dateFormat.parse(v2.getVisitDate());
                            return date2.compareTo(date1);
                        } catch (ParseException e) {
                            return 0;
                        }
                    });

                    // Update UI with sorted visits
                    llANCVisits.removeAllViews();
                    for (int i = 0; i < ancVisits.size(); i++) {
                        addANCVisitItem(ancVisits.get(i), i + 1);
                    }
                } else {
                    tvNoANCVisits.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load ANC visits: " + error.getMessage());
                tvNoANCVisits.setVisibility(View.VISIBLE);
            }
        });
    }

    private void addANCVisitItem(ANCVisit visit, int count) {
        View itemView = LayoutInflater.from(this)
                .inflate(R.layout.item_anc_visit, llANCVisits, false);

        TextView tvVisitNumber = itemView.findViewById(R.id.tvVisitNumber);
        TextView tvVisitDate = itemView.findViewById(R.id.tvVisitDate);
        TextView tvVisitPurpose = itemView.findViewById(R.id.tvVisitPurpose);
        TextView tvFindings = itemView.findViewById(R.id.tvFindings);

        tvVisitNumber.setText("Visit #" + count);
        tvVisitDate.setText(!TextUtils.isEmpty(visit.getVisitDate()) ? visit.getVisitDate() : "Date not recorded");
        tvVisitPurpose.setText(!TextUtils.isEmpty(visit.getPurpose()) ? visit.getPurpose() : "Routine checkup");
        tvFindings.setText(!TextUtils.isEmpty(visit.getFindings()) ? visit.getFindings() : "No significant findings");

        // Set different background for alternate rows
        if (count % 2 == 0) {
            itemView.setBackgroundResource(R.drawable.bg_trimester_item);
        }

        llANCVisits.addView(itemView);
    }

    private void loadVitalSigns() {
        if (currentUser == null) return;

        DatabaseReference vitalsRef = mDatabase.child("users").child(currentUser.getUid()).child("vital_signs");

        vitalsRef.orderByChild("dateTaken").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                llVitalSigns.removeAllViews();
                vitalSigns.clear();

                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    tvNoVitalSigns.setVisibility(View.GONE);

                    // Get the most recent vital signs (limit to 3)
                    int count = 0;
                    for (DataSnapshot vitalSnapshot : snapshot.getChildren()) {
                        if (count >= 3) break; // Only show 3 most recent

                        VitalSign vital = vitalSnapshot.getValue(VitalSign.class);
                        if (vital != null) {
                            vitalSigns.add(vital);
                            addVitalSignItem(vital, ++count);
                        }
                    }
                } else {
                    tvNoVitalSigns.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load vital signs: " + error.getMessage());
                tvNoVitalSigns.setVisibility(View.VISIBLE);
            }
        });
    }

    private void addVitalSignItem(VitalSign vital, int count) {
        View itemView = LayoutInflater.from(this)
                .inflate(R.layout.item_vital_sign, llVitalSigns, false);

        TextView tvDateTaken = itemView.findViewById(R.id.tvDateTaken);
        TextView tvBloodPressure = itemView.findViewById(R.id.tvBloodPressure);
        TextView tvWeight = itemView.findViewById(R.id.tvWeight);
        TextView tvFetalHeartRate = itemView.findViewById(R.id.tvFetalHeartRate);
        TextView tvFundalHeight = itemView.findViewById(R.id.tvFundalHeight);

        tvDateTaken.setText(!TextUtils.isEmpty(vital.getDateTaken()) ? vital.getDateTaken() : "Date not recorded");
        tvBloodPressure.setText(!TextUtils.isEmpty(vital.getBloodPressure()) ? vital.getBloodPressure() + " mmHg" : "Not measured");
        tvWeight.setText(!TextUtils.isEmpty(vital.getWeight()) ? vital.getWeight() + " kg" : "Not measured");
        tvFetalHeartRate.setText(!TextUtils.isEmpty(vital.getFetalHeartRate()) ? vital.getFetalHeartRate() + " bpm" : "Not measured");
        tvFundalHeight.setText(!TextUtils.isEmpty(vital.getFundalHeight()) ? vital.getFundalHeight() + " cm" : "Not measured");

        // Set different background for alternate rows
        if (count % 2 == 0) {
            itemView.setBackgroundResource(R.drawable.bg_rounded_light_green);
        }

        llVitalSigns.addView(itemView);
    }

    private void setDefaultValues() {
        tvMotherName.setText("Not specified");
        tvMotherAge.setText("Not specified");
        tvMotherContact.setText("Not specified");
        tvEmergencyContact.setText("Not specified");

        tvGestationalAge.setText("Not specified");
        tvEDD.setText("Not specified");
        tvLMP.setText("Not specified");
        tvParity.setText("Not specified");

        tvMedicalConditions.setText("No known pre-existing conditions");
        tvComplications.setText("No complications reported");
        tvAllergies.setText("No known allergies");

        tvRecommendations.setText("No specific recommendations at this time");

        tvNextAppointmentDate.setText("Not scheduled");
        tvNextAppointmentTime.setText("Not specified");
        tvNextAppointmentPurpose.setText("Routine checkup");

        // Hide loading and show content
        llLoading.setVisibility(View.GONE);
        llRecordsContainer.setVisibility(View.VISIBLE);
    }

    private void checkStoragePermissionAndDownload() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ - Use MediaStore API
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
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
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

    private void generateAndDownloadPDF() {
        if (currentMother == null) {
            Toast.makeText(this, "No antenatal records found", Toast.LENGTH_SHORT).show();
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
                // Save PDF to file using HTML format (simpler)
                File pdfFile = generateHTMLFile();

                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    if (pdfFile != null && pdfFile.exists()) {
                        showDownloadSuccessDialog(pdfFile);
                    } else {
                        Toast.makeText(ANCRecordsActivity.this,
                                "Failed to generate PDF", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error generating PDF", e);
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(ANCRecordsActivity.this,
                            "Error generating PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private File generateHTMLFile() {
        try {
            File htmlFile;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10+, use app-specific directory
                File appSpecificDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
                if (appSpecificDir == null) {
                    appSpecificDir = getFilesDir();
                }

                String motherName = currentMother != null ?
                        currentMother.getName().replaceAll("[^a-zA-Z0-9]", "_") : "Mother";
                String fileName = "ANC_Record_" + motherName + "_" +
                        new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".html";

                htmlFile = new File(appSpecificDir, fileName);
            } else {
                // For Android 9 and below, use Downloads directory with permission
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs();
                }

                String motherName = currentMother != null ?
                        currentMother.getName().replaceAll("[^a-zA-Z0-9]", "_") : "Mother";
                String fileName = "ANC_Record_" + motherName + "_" +
                        new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".html";

                htmlFile = new File(downloadsDir, fileName);
            }

            // Generate HTML content
            String htmlContent = generateHTMLContent();

            // Write to file
            FileOutputStream fos = new FileOutputStream(htmlFile);
            fos.write(htmlContent.getBytes());
            fos.close();

            // Notify system
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                addFileToMediaStore(htmlFile);
            }

            Log.d(TAG, "HTML file saved to: " + htmlFile.getAbsolutePath());
            return htmlFile;

        } catch (Exception e) {
            Log.e(TAG, "Error saving HTML file", e);
            return null;
        }
    }

    private String generateHTMLContent() {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<title>Antenatal Care Record</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: Arial, sans-serif; margin: 40px; line-height: 1.6; }\n");
        html.append("h1 { color: #7B1FA2; text-align: center; border-bottom: 3px solid #7B1FA2; padding-bottom: 15px; }\n");
        html.append("h2 { color: #388E3C; border-bottom: 2px solid #C8E6C9; padding-bottom: 8px; margin-top: 30px; }\n");
        html.append("h3 { color: #1976D2; margin-top: 20px; }\n");
        html.append("table { width: 100%; border-collapse: collapse; margin: 15px 0; }\n");
        html.append("th { background-color: #F3E5F5; text-align: left; padding: 10px; border: 1px solid #E1BEE7; }\n");
        html.append("td { padding: 10px; border: 1px solid #E1BEE7; }\n");
        html.append(".section { margin-bottom: 30px; background-color: #f9f9f9; padding: 20px; border-radius: 8px; }\n");
        html.append(".highlight { background-color: #E8F5E9; padding: 15px; border-radius: 5px; margin: 15px 0; }\n");
        html.append(".footer { text-align: center; margin-top: 40px; color: #757575; font-size: 12px; border-top: 1px solid #ddd; padding-top: 20px; }\n");
        html.append(".visit-item { border: 1px solid #FFCC80; padding: 15px; margin: 10px 0; border-radius: 5px; background-color: #FFF3E0; }\n");
        html.append(".vital-signs { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; }\n");
        html.append(".vital-item { background-color: #E3F2FD; padding: 12px; border-radius: 5px; }\n");
        html.append("</style>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        // Header
        html.append("<h1>ANTENATAL CARE RECORD</h1>\n");
        html.append("<p style=\"text-align: center; color: #757575;\">Generated on: ")
                .append(reportDateFormat.format(new Date()))
                .append("</p>\n");

        // Mother Profile
        html.append("<div class=\"section\">\n");
        html.append("<h2>MOTHER PROFILE</h2>\n");
        html.append("<table>\n");
        html.append("<tr><th style=\"width: 30%;\">Field</th><th>Details</th></tr>\n");
        html.append("<tr><td>Full Name</td><td>").append(escapeHtml(tvMotherName.getText().toString())).append("</td></tr>\n");
        html.append("<tr><td>Age</td><td>").append(escapeHtml(tvMotherAge.getText().toString())).append("</td></tr>\n");
        html.append("<tr><td>Contact</td><td>").append(escapeHtml(tvMotherContact.getText().toString())).append("</td></tr>\n");
        html.append("<tr><td>Emergency Contact</td><td>").append(escapeHtml(tvEmergencyContact.getText().toString())).append("</td></tr>\n");
        html.append("</table>\n");
        html.append("</div>\n");

        // Pregnancy Progress
        html.append("<div class=\"section\">\n");
        html.append("<h2>PREGNANCY PROGRESS</h2>\n");
        html.append("<div class=\"highlight\">\n");
        html.append("<table>\n");
        html.append("<tr><td><strong>Gestational Age</strong></td><td>").append(escapeHtml(tvGestationalAge.getText().toString())).append("</td></tr>\n");
        html.append("<tr><td><strong>Expected Due Date (EDD)</strong></td><td>").append(escapeHtml(tvEDD.getText().toString())).append("</td></tr>\n");
        html.append("<tr><td><strong>Last Menstrual Period (LMP)</strong></td><td>").append(escapeHtml(tvLMP.getText().toString())).append("</td></tr>\n");
        html.append("<tr><td><strong>Parity (G/P/A)</strong></td><td>").append(escapeHtml(tvParity.getText().toString())).append("</td></tr>\n");
        html.append("</table>\n");
        html.append("</div>\n");

        // Trimester Progress
        html.append("<h3>Trimester Progress</h3>\n");
        try {
            int weeks = Integer.parseInt(currentMother.getWeeksPregnant());
            html.append("<p><strong>Current Status:</strong> ");
            if (weeks <= 12) {
                html.append("First Trimester (Week ").append(weeks).append(" of 12)");
            } else if (weeks <= 27) {
                html.append("Second Trimester (Week ").append(weeks).append(" of 27)");
            } else {
                html.append("Third Trimester (Week ").append(weeks).append(" of 40+)");
            }
            html.append("</p>\n");
        } catch (NumberFormatException e) {
            html.append("<p>Trimester information not available</p>\n");
        }
        html.append("</div>\n");

        // Medical History & Conditions
        html.append("<div class=\"section\">\n");
        html.append("<h2>MEDICAL HISTORY & CONDITIONS</h2>\n");

        html.append("<h3>Pre-existing Medical Conditions</h3>\n");
        html.append("<p>").append(escapeHtml(tvMedicalConditions.getText().toString())).append("</p>\n");

        html.append("<h3>Pregnancy Complications</h3>\n");
        html.append("<p>").append(escapeHtml(tvComplications.getText().toString())).append("</p>\n");

        html.append("<h3>Allergies</h3>\n");
        html.append("<p>").append(escapeHtml(tvAllergies.getText().toString())).append("</p>\n");
        html.append("</div>\n");

        // Vital Signs
        html.append("<div class=\"section\">\n");
        html.append("<h2>RECENT VITAL SIGNS</h2>\n");

        if (vitalSigns.isEmpty()) {
            html.append("<p><em>No vital signs recorded yet</em></p>\n");
        } else {
            html.append("<div class=\"vital-signs\">\n");
            for (VitalSign vital : vitalSigns) {
                html.append("<div class=\"vital-item\">\n");
                html.append("<p><strong>Date:</strong> ").append(escapeHtml(vital.getDateTaken())).append("</p>\n");
                html.append("<p><strong>Blood Pressure:</strong> ").append(escapeHtml(vital.getBloodPressure())).append(" mmHg</p>\n");
                html.append("<p><strong>Weight:</strong> ").append(escapeHtml(vital.getWeight())).append(" kg</p>\n");
                if (!TextUtils.isEmpty(vital.getFetalHeartRate())) {
                    html.append("<p><strong>Fetal Heart Rate:</strong> ").append(escapeHtml(vital.getFetalHeartRate())).append(" bpm</p>\n");
                }
                if (!TextUtils.isEmpty(vital.getFundalHeight())) {
                    html.append("<p><strong>Fundal Height:</strong> ").append(escapeHtml(vital.getFundalHeight())).append(" cm</p>\n");
                }
                html.append("</div>\n");
            }
            html.append("</div>\n");
        }
        html.append("</div>\n");

        // ANC Visits History
        html.append("<div class=\"section\">\n");
        html.append("<h2>ANC VISITS HISTORY</h2>\n");

        if (ancVisits.isEmpty()) {
            html.append("<p><em>No ANC visits recorded yet</em></p>\n");
        } else {
            for (int i = 0; i < ancVisits.size(); i++) {
                ANCVisit visit = ancVisits.get(i);
                html.append("<div class=\"visit-item\">\n");
                html.append("<h3>Visit #").append(i + 1).append(" - ").append(escapeHtml(visit.getVisitDate())).append("</h3>\n");
                html.append("<p><strong>Purpose:</strong> ").append(escapeHtml(visit.getPurpose())).append("</p>\n");
                if (!TextUtils.isEmpty(visit.getFindings())) {
                    html.append("<p><strong>Findings:</strong> ").append(escapeHtml(visit.getFindings())).append("</p>\n");
                }
                if (!TextUtils.isEmpty(visit.getRecommendations())) {
                    html.append("<p><strong>Recommendations:</strong> ").append(escapeHtml(visit.getRecommendations())).append("</p>\n");
                }
                html.append("</div>\n");
            }
        }
        html.append("</div>\n");

        // Doctor's Recommendations
        html.append("<div class=\"section\">\n");
        html.append("<h2>DOCTOR'S RECOMMENDATIONS</h2>\n");
        html.append("<div class=\"highlight\">\n");
        html.append("<p>").append(escapeHtml(tvRecommendations.getText().toString())).append("</p>\n");
        html.append("</div>\n");
        html.append("</div>\n");

        // Next Appointment
        html.append("<div class=\"section\">\n");
        html.append("<h2>NEXT APPOINTMENT</h2>\n");
        html.append("<div class=\"highlight\">\n");
        html.append("<table>\n");
        html.append("<tr><td><strong>Date</strong></td><td>").append(escapeHtml(tvNextAppointmentDate.getText().toString())).append("</td></tr>\n");
        html.append("<tr><td><strong>Time</strong></td><td>").append(escapeHtml(tvNextAppointmentTime.getText().toString())).append("</td></tr>\n");
        html.append("<tr><td><strong>Purpose</strong></td><td>").append(escapeHtml(tvNextAppointmentPurpose.getText().toString())).append("</td></tr>\n");
        html.append("</table>\n");
        html.append("</div>\n");
        html.append("</div>\n");

        // Footer
        html.append("<div class=\"footer\">\n");
        html.append("<hr>\n");
        html.append("<p>--- END OF ANTENATAL CARE RECORD ---</p>\n");
        html.append("<p>Generated by AfyaTrack App</p>\n");
        html.append("<p><em>This is an official medical record. Keep for future reference.</em></p>\n");
        html.append("</div>\n");

        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }

    private String escapeHtml(String text) {
        if (TextUtils.isEmpty(text)) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replace("\n", "<br>");
    }

    @SuppressLint("Range")
    private void addFileToMediaStore(File file) {
        try {
            android.media.MediaScannerConnection.scanFile(this,
                    new String[]{file.getAbsolutePath()},
                    new String[]{"text/html"},
                    null);
        } catch (Exception e) {
            Log.e(TAG, "Error adding file to MediaStore", e);
        }
    }

    private void showDownloadSuccessDialog(File htmlFile) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("ANC Record Generated Successfully");

        String message;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            message = "Antenatal record has been saved to:\n" + htmlFile.getName() +
                    "\n\nLocation: App Documents folder";
        } else {
            message = "Antenatal record has been saved to:\n" + htmlFile.getName() +
                    "\n\nLocation: Downloads folder";
        }

        builder.setMessage(message);

        builder.setPositiveButton("Open File", (dialog, which) -> openHTMLFile(htmlFile));
        builder.setNegativeButton("Share", (dialog, which) -> shareHTMLFile(htmlFile));
        builder.setNeutralButton("OK", null);

        builder.show();
    }

    private void openHTMLFile(File file) {
        try {
            Uri fileUri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".provider",
                    file);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, "text/html");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

            // Verify there's an app to handle HTML files
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                // No HTML viewer, share instead
                shareHTMLFile(file);
            }
        } catch (Exception e) {
            Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show();
            shareHTMLFile(file);
        }
    }

    private void shareHTMLFile(File file) {
        try {
            Uri fileUri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".provider",
                    file);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/html");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Antenatal Record - " + tvMotherName.getText());
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Antenatal care record for " + tvMotherName.getText() +
                    "\n\nGenerated by AfyaTrack App");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share ANC Record via"));
        } catch (Exception e) {
            Log.e(TAG, "Error sharing HTML file", e);
            Toast.makeText(this, "Error sharing file", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareANCReport() {
        if (currentMother == null) {
            Toast.makeText(this, "No antenatal records found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create shareable text content
        StringBuilder shareContent = new StringBuilder();
        shareContent.append("*ANTENATAL CARE RECORD*\n\n");

        shareContent.append("*Mother Profile:*\n");
        shareContent.append("Name: ").append(tvMotherName.getText()).append("\n");
        shareContent.append("Age: ").append(tvMotherAge.getText()).append("\n");
        shareContent.append("Contact: ").append(tvMotherContact.getText()).append("\n");
        shareContent.append("Emergency Contact: ").append(tvEmergencyContact.getText()).append("\n\n");

        shareContent.append("*Pregnancy Progress:*\n");
        shareContent.append("Gestational Age: ").append(tvGestationalAge.getText()).append("\n");
        shareContent.append("EDD: ").append(tvEDD.getText()).append("\n");
        shareContent.append("LMP: ").append(tvLMP.getText()).append("\n");
        shareContent.append("Parity: ").append(tvParity.getText()).append("\n\n");

        shareContent.append("*Medical Conditions:*\n");
        shareContent.append(tvMedicalConditions.getText()).append("\n\n");

        shareContent.append("*Complications:*\n");
        shareContent.append(tvComplications.getText()).append("\n\n");

        shareContent.append("*Allergies:*\n");
        shareContent.append(tvAllergies.getText()).append("\n\n");

        shareContent.append("*Doctor's Recommendations:*\n");
        shareContent.append(tvRecommendations.getText()).append("\n\n");

        shareContent.append("*Next Appointment:*\n");
        shareContent.append("Date: ").append(tvNextAppointmentDate.getText()).append("\n");
        shareContent.append("Purpose: ").append(tvNextAppointmentPurpose.getText()).append("\n\n");

        shareContent.append("_Generated by AfyaTrack App_");

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Antenatal Record - " + tvMotherName.getText());
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareContent.toString());

        startActivity(Intent.createChooser(shareIntent, "Share ANC Record"));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Data Models

    public static class ANCMother {
        private String name;
        private String age;
        private String phone;
        private String emergencyContact;
        private String weeksPregnant;
        private String edd;
        private String lmp;
        private String parity;
        private String medicalConditions;
        private String complications;
        private String allergies;
        private String recommendations;

        public ANCMother() {}

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getAge() { return age; }
        public void setAge(String age) { this.age = age; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getEmergencyContact() { return emergencyContact; }
        public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }
        public String getWeeksPregnant() { return weeksPregnant; }
        public void setWeeksPregnant(String weeksPregnant) { this.weeksPregnant = weeksPregnant; }
        public String getEdd() { return edd; }
        public void setEdd(String edd) { this.edd = edd; }
        public String getLmp() { return lmp; }
        public void setLmp(String lmp) { this.lmp = lmp; }
        public String getParity() { return parity; }
        public void setParity(String parity) { this.parity = parity; }
        public String getMedicalConditions() { return medicalConditions; }
        public void setMedicalConditions(String medicalConditions) { this.medicalConditions = medicalConditions; }
        public String getComplications() { return complications; }
        public void setComplications(String complications) { this.complications = complications; }
        public String getAllergies() { return allergies; }
        public void setAllergies(String allergies) { this.allergies = allergies; }
        public String getRecommendations() { return recommendations; }
        public void setRecommendations(String recommendations) { this.recommendations = recommendations; }
    }

    public static class ANCVisit {
        private String visitDate;
        private String purpose;
        private String findings;
        private String recommendations;

        public ANCVisit() {}

        public String getVisitDate() { return visitDate; }
        public void setVisitDate(String visitDate) { this.visitDate = visitDate; }
        public String getPurpose() { return purpose; }
        public void setPurpose(String purpose) { this.purpose = purpose; }
        public String getFindings() { return findings; }
        public void setFindings(String findings) { this.findings = findings; }
        public String getRecommendations() { return recommendations; }
        public void setRecommendations(String recommendations) { this.recommendations = recommendations; }
    }

    public static class VitalSign {
        private String dateTaken;
        private String bloodPressure;
        private String weight;
        private String fetalHeartRate;
        private String fundalHeight;

        public VitalSign() {}

        public String getDateTaken() { return dateTaken; }
        public void setDateTaken(String dateTaken) { this.dateTaken = dateTaken; }
        public String getBloodPressure() { return bloodPressure; }
        public void setBloodPressure(String bloodPressure) { this.bloodPressure = bloodPressure; }
        public String getWeight() { return weight; }
        public void setWeight(String weight) { this.weight = weight; }
        public String getFetalHeartRate() { return fetalHeartRate; }
        public void setFetalHeartRate(String fetalHeartRate) { this.fetalHeartRate = fetalHeartRate; }
        public String getFundalHeight() { return fundalHeight; }
        public void setFundalHeight(String fundalHeight) { this.fundalHeight = fundalHeight; }
    }
}