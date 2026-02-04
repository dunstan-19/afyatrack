// AntenatalActivity.java
package com.example.afyatrack;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.graphics.Color;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ImageView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AntenatalActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FirebaseUser currentUser;

    // UI Components
    private TextView welcomeText, pregnancyStatusText, toolbarTitle;
    private Button registerButton, viewProfileButton, sendReportButton,btn_schedule;
    private CardView welcomeCard, progressCard, nextVisitCard, healthCard;
    private CardView missedVisitCard, reportIssueCard, recommendationCard, motherInfoCard;
    private LinearLayout mainContainer, dashboardContainer;
    private ProgressBar loadingProgress;
    private ImageView pregnancyIcon, motherAvatar;
    private BottomNavigationView bottomNavigation;
    private LinearProgressIndicator pregnancyProgress;
    private ImageButton btnHome, btnSettings, btnHelp;

    // Data display TextViews
    private TextView tvWeeksPregnant, tvDueDate, tvTrimester, tvLastVisit, tvNextVisit;
    private TextView tvBloodGroup, tvGravida, tvParity, tvFacilityType, tvDeliveryPlan;
    private TextView tvMotherName, tvMotherAge, tvMissedVisitInfo, tvRecommendation;
    private EditText edtHealthIssue;

    // Database references
    private DatabaseReference ancRef;
    private DatabaseReference healthIssuesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_antenatal);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            ancRef = mDatabase.child("users").child(currentUser.getUid()).child("anc_registration");
            healthIssuesRef = mDatabase.child("users").child(currentUser.getUid()).child("health_issues");
        }

        // Initialize UI Components
        initializeViews();
        setupBottomNavigation();
        setupUI();
        setupTopNavigation();

        // Check if user has ANC registration
        checkANCStatus();
    }

    private void initializeViews() {
        welcomeText = findViewById(R.id.welcomeText);
        pregnancyStatusText = findViewById(R.id.pregnancyStatusText);
        registerButton = findViewById(R.id.registerButton);
        viewProfileButton = findViewById(R.id.viewProfileButton);
        sendReportButton = findViewById(R.id.sendReportButton);
        welcomeCard = findViewById(R.id.welcomeCard);
        mainContainer = findViewById(R.id.mainContainer);
        loadingProgress = findViewById(R.id.loadingProgress);
        pregnancyIcon = findViewById(R.id.pregnancyIcon);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        toolbarTitle = findViewById(R.id.toolbar_title);
        pregnancyProgress = findViewById(R.id.pregnancyProgress);
        dashboardContainer = findViewById(R.id.dashboardContainer);
        motherAvatar = findViewById(R.id.motherAvatar);
        btn_schedule = findViewById(R.id.btn_schedule);


        // Data display views
        tvWeeksPregnant = findViewById(R.id.tvWeeksPregnant);
        tvDueDate = findViewById(R.id.tvDueDate);
        tvTrimester = findViewById(R.id.tvTrimester);
        tvLastVisit = findViewById(R.id.tvLastVisit);
        tvNextVisit = findViewById(R.id.tvNextVisit);
        tvBloodGroup = findViewById(R.id.tvBloodGroup);
        tvGravida = findViewById(R.id.tvGravida);
        tvParity = findViewById(R.id.tvParity);
        tvFacilityType = findViewById(R.id.tvFacilityType);
        tvDeliveryPlan = findViewById(R.id.tvDeliveryPlan);
        tvMotherName = findViewById(R.id.tvMotherName);
        tvMotherAge = findViewById(R.id.tvMotherAge);
        tvMissedVisitInfo = findViewById(R.id.tvMissedVisitInfo);
        tvRecommendation = findViewById(R.id.tvRecommendation);

        // Input field
        edtHealthIssue = findViewById(R.id.edtHealthIssue);

        // Top Navigation Buttons
        btnHome = findViewById(R.id.btn_home);
        btnSettings = findViewById(R.id.btn_settings);
        btnHelp = findViewById(R.id.btn_help);

        // Cards
        progressCard = findViewById(R.id.progressCard);
        nextVisitCard = findViewById(R.id.nextVisitCard);
        healthCard = findViewById(R.id.healthCard);
        missedVisitCard = findViewById(R.id.missedVisitCard);
        reportIssueCard = findViewById(R.id.reportIssueCard);
        recommendationCard = findViewById(R.id.recommendationCard);
        motherInfoCard = findViewById(R.id.motherInfoCard);
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
                    startActivity(new Intent(AntenatalActivity.this, ANCRecordsActivity.class));
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    return true;
                } else if (itemId == R.id.nav_appointment) {
                    startActivity(new Intent(AntenatalActivity.this, ANCAppointmentsActivity.class));
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    return true;
                } else if (itemId == R.id.nav_dashboard) {
                    return true;
                } else if (itemId == R.id.nav_notifications) {
                    startActivity(new Intent(AntenatalActivity.this, ANCNotificationsActivity.class));
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    return true;
                } else if (itemId == R.id.nav_child_profile) {
                    startActivity(new Intent(AntenatalActivity.this, ANCProfileActivity.class));
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    return true;
                }
                return false;
            }
        });

        bottomNavigation.setSelectedItemId(R.id.nav_dashboard);
    }

    private void setupUI() {
        // Set welcome message
        if (currentUser != null && currentUser.getDisplayName() != null) {
            welcomeText.setText("Welcome " + currentUser.getDisplayName() + "! 👶");
        } else {
            welcomeText.setText("Welcome!");
        }

        // Set button click listeners
        registerButton.setOnClickListener(v -> {
            animateButtonClick(registerButton);
            startActivity(new Intent(AntenatalActivity.this, AntenatalRegister.class));
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        btn_schedule.setOnClickListener(v -> {
            startActivity(new Intent(this, ANCAppointmentsActivity.class));
            finish();
        });

        viewProfileButton.setOnClickListener(v -> {
            animateButtonClick(viewProfileButton);
            startActivity(new Intent(AntenatalActivity.this, ANCProfileActivity.class));
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        sendReportButton.setOnClickListener(v -> {
            reportHealthIssue();
        });

        // Set recommendation text
        tvRecommendation.setText("No recommendations made yet. Your doctor will provide personalized recommendations during your next visit.");
    }

    private void checkANCStatus() {
        if (currentUser == null) return;

        loadingProgress.setVisibility(View.VISIBLE);

        mDatabase.child("users").child(currentUser.getUid()).child("anc_registration")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        loadingProgress.setVisibility(View.GONE);

                        if (dataSnapshot.exists()) {
                            showDashboard(dataSnapshot);
                            welcomeCard.setVisibility(View.GONE);
                            dashboardContainer.setVisibility(View.VISIBLE);
                            toolbarTitle.setText("Antenatal Care Dashboard");

                            // Load mother info from ANC registration data
                            loadMotherInfo(dataSnapshot);
                            checkMissedVisits();
                        } else {
                            showWelcomeScreen();
                            welcomeCard.setVisibility(View.VISIBLE);
                            dashboardContainer.setVisibility(View.GONE);
                            toolbarTitle.setText("Antenatal Care");
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        loadingProgress.setVisibility(View.GONE);
                        pregnancyStatusText.setText("Error loading your status. Please try again.");
                    }
                });
    }

    private void showDashboard(DataSnapshot dataSnapshot) {
        // Get data from Firebase based on your screenshot structure
        String name = dataSnapshot.child("name").getValue(String.class);
        String age = dataSnapshot.child("age").getValue(String.class);
        String lmpDate = dataSnapshot.child("LmpDate").getValue(String.class); // Note: capital L in LmpDate
        String edd = dataSnapshot.child("expectedDueDate").getValue(String.class);
        String bloodGroup = dataSnapshot.child("bloodGroup").getValue(String.class);
        String lastVisit = dataSnapshot.child("lastVisitDate").getValue(String.class);
        String nextVisit = dataSnapshot.child("nextVisitDate").getValue(String.class);

        // Get optional fields (might not exist in your structure yet)
        String weeksPregnant = dataSnapshot.child("weeksPregnant").getValue(String.class);
        String gravida = dataSnapshot.child("gravida").getValue(String.class);
        String parity = dataSnapshot.child("parity").getValue(String.class);
        String facilityType = dataSnapshot.child("facilityType").getValue(String.class);
        String deliveryPlan = dataSnapshot.child("deliveryPlan").getValue(String.class);

        // Calculate weeks pregnant from LMP if not stored
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

        // Update UI with data
        tvWeeksPregnant.setText(weeks + " weeks");
        tvDueDate.setText(edd != null ? edd : "Not set");
        tvTrimester.setText(trimester);
        tvLastVisit.setText(lastVisit != null ? lastVisit : "No visits yet");
        tvNextVisit.setText(nextVisit != null ? nextVisit : "Not scheduled");
        tvBloodGroup.setText(bloodGroup != null && !bloodGroup.isEmpty() ? bloodGroup : "Not specified");
        tvGravida.setText(gravida != null ? gravida : "Not specified");
        tvParity.setText(parity != null ? parity : "Not specified");
        tvFacilityType.setText(facilityType != null ? facilityType : "Not specified");
        tvDeliveryPlan.setText(deliveryPlan != null ? deliveryPlan : "Not specified");

        // Calculate and set pregnancy progress
        calculatePregnancyProgress(weeks);
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

    private void loadMotherInfo(DataSnapshot ancData) {
        // Get mother info directly from ANC registration data
        String name = ancData.child("name").getValue(String.class);
        String age = ancData.child("age").getValue(String.class);

        if (name != null && !name.isEmpty()) {
            tvMotherName.setText(name);
        } else if (currentUser != null && currentUser.getDisplayName() != null) {
            tvMotherName.setText(currentUser.getDisplayName());
        } else {
            tvMotherName.setText("Not specified");
        }

        if (age != null && !age.isEmpty()) {
            tvMotherAge.setText(age + " years");
        } else {
            tvMotherAge.setText("Not specified");
        }

        // Set default avatar (female for mothers)
        motherAvatar.setImageResource(R.drawable.girlavatar);
    }

    private void checkMissedVisits() {
        if (currentUser == null || ancRef == null) return;

        ancRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String lastVisit = dataSnapshot.child("lastVisitDate").getValue(String.class);

                    if (lastVisit != null && !lastVisit.equals("No visits yet")) {
                        try {
                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                            Calendar lastVisitDate = Calendar.getInstance();
                            lastVisitDate.setTime(sdf.parse(lastVisit));

                            Calendar today = Calendar.getInstance();
                            long diff = today.getTimeInMillis() - lastVisitDate.getTimeInMillis();
                            long days = diff / (24 * 60 * 60 * 1000);

                            // If last visit was more than 30 days ago, show alert
                            if (days > 30) {
                                tvMissedVisitInfo.setText("Missed follow-up visit\nLast visit was " + days + " days ago");
                                missedVisitCard.setVisibility(View.VISIBLE);
                            } else {
                                tvMissedVisitInfo.setText("No missed visits\nYou're on track with your ANC schedule");
                                missedVisitCard.setVisibility(View.VISIBLE);
                            }
                        } catch (Exception e) {
                            tvMissedVisitInfo.setText("No missed visits detected");
                            missedVisitCard.setVisibility(View.VISIBLE);
                        }
                    } else {
                        tvMissedVisitInfo.setText("No ANC visits recorded yet\nSchedule your first appointment");
                        missedVisitCard.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                missedVisitCard.setVisibility(View.GONE);
            }
        });
    }

    private void reportHealthIssue() {
        String healthIssue = edtHealthIssue.getText().toString().trim();

        if (TextUtils.isEmpty(healthIssue)) {
            edtHealthIssue.setError("Please describe your health issue");
            edtHealthIssue.requestFocus();
            return;
        }

        if (currentUser == null || healthIssuesRef == null) return;

        // Create health issue data
        Map<String, Object> issueData = new HashMap<>();
        issueData.put("issue", healthIssue);
        issueData.put("timestamp", System.currentTimeMillis());
        issueData.put("status", "reported");

        // Add mother's name to the report
        String motherName = tvMotherName.getText().toString();
        if (!motherName.equals("Not specified")) {
            issueData.put("motherName", motherName);
        }

        // Save to Firebase
        String issueId = healthIssuesRef.push().getKey();
        if (issueId != null) {
            healthIssuesRef.child(issueId).setValue(issueData)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(AntenatalActivity.this,
                                    "Health issue reported successfully",
                                    Toast.LENGTH_SHORT).show();
                            edtHealthIssue.setText("");

                            // Update recommendation text to inform doctor will review
                            tvRecommendation.setText("Health issue reported. Your doctor will review and provide recommendations during your next visit.");
                        } else {
                            Toast.makeText(AntenatalActivity.this,
                                    "Failed to report issue: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void calculatePregnancyProgress(int weeks) {
        int totalWeeks = 40;
        int progress = (weeks * 100) / totalWeeks;
        if (progress < 0) progress = 0;
        if (progress > 100) progress = 100;
        pregnancyProgress.setProgressCompat(progress, true);
    }

    private String getTrimester(int weeks) {
        if (weeks <= 12) return "First Trimester";
        else if (weeks <= 27) return "Second Trimester";
        else return "Third Trimester";
    }

    private void showWelcomeScreen() {
        pregnancyStatusText.setText("Start your beautiful pregnancy journey with us!\n\n" +
                "Register now to:\n" +
                "✓ Track your pregnancy week by week\n" +
                "✓ Get personalized health advice\n" +
                "✓ Schedule doctor appointments\n" +
                "✓ Monitor baby's development\n\n" +
                "Click 'Register for ANC' to begin! 💕");

        registerButton.setVisibility(View.VISIBLE);
        viewProfileButton.setVisibility(View.GONE);
    }

    private void animateButtonClick(View view) {
        view.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start())
                .start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkANCStatus();
    }
}