package com.example.afyatrack;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class ChildVaccineActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private String currentUserId;
    private String selectedChildId;

    private MaterialCardView newUserCard;
    private LinearLayout existingChildDashboard;
    private LinearLayout childSelectionLayout;
    private Spinner spinnerChildren;
    private ImageButton btnManageChildren;

    private MaterialCardView urgentVaccineCard, dueVaccineCard, nextVaccineCard, missedVaccineCard, healthConditionCard;
    private ImageView imgChildAvatar;
    private TextView tvChildName, tvChildAge, tvChildGender, tvBirthWeight, tvHealthConditions, tvHealthStatus;
    private TextView tvUrgentVaccine, tvUrgentReason, tvUrgentDays;
    private TextView tvDueVaccine, tvDueDate;
    private TextView tvNextVaccine, tvNextDate;
    private TextView tvMissedVaccines, tvMissedRecommendation, tvMissedCount;
    private Button btnRegisterChild, btnAddChild;
    private ImageButton btnHome, btnSettings, btnHelp;
    private BottomNavigationView bottomNavigationView;

    private int childAgeInMonths;
    private boolean hasSpecialConditions = false;

    private List<Child> childrenList = new ArrayList<>();
    private ArrayAdapter<Child> childrenAdapter;

    private TextView notificationBadge;
    private boolean hasUnreadNotifications = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_vaccine);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        currentUserId = user.getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        initViews();
        setupTopNavigation();
        setupBottomNavigation();
        loadChildrenList();
        setupNotificationBadge();
    }

    private void initViews() {
        newUserCard = findViewById(R.id.newUserCard);
        existingChildDashboard = findViewById(R.id.existingChildDashboard);
        childSelectionLayout = findViewById(R.id.childSelectionLayout);
        spinnerChildren = findViewById(R.id.spinnerChildren);
        btnManageChildren = findViewById(R.id.btnManageChildren);
        imgChildAvatar = findViewById(R.id.imgChildAvatar);

        // Profile Views
        tvChildName = findViewById(R.id.tvChildName);
        tvChildAge = findViewById(R.id.tvChildAge);
        tvChildGender = findViewById(R.id.tvChildGender);
        tvBirthWeight = findViewById(R.id.tvBirthWeight);
        tvHealthConditions = findViewById(R.id.tvHealthConditions);
        tvHealthStatus = findViewById(R.id.tvHealthStatus);

        // Vaccine Cards
        urgentVaccineCard = findViewById(R.id.urgentVaccineCard);
        dueVaccineCard = findViewById(R.id.dueVaccineCard);
        nextVaccineCard = findViewById(R.id.nextVaccineCard);
        missedVaccineCard = findViewById(R.id.missedVaccineCard);
        healthConditionCard = findViewById(R.id.healthConditionCard);

        // Vaccine TextViews
        tvUrgentVaccine = findViewById(R.id.tvUrgentVaccine);
        tvUrgentReason = findViewById(R.id.tvUrgentReason);
        tvUrgentDays = findViewById(R.id.tvUrgentDays);
        tvDueVaccine = findViewById(R.id.tvDueVaccine);
        tvDueDate = findViewById(R.id.tvDueDate);
        tvNextVaccine = findViewById(R.id.tvNextVaccine);
        tvNextDate = findViewById(R.id.tvNextDate);
        tvMissedVaccines = findViewById(R.id.tvMissedVaccines);
        tvMissedRecommendation = findViewById(R.id.tvMissedRecommendation);
        tvMissedCount = findViewById(R.id.tvMissedCount);

        // Top Navigation Buttons
        btnHome = findViewById(R.id.btn_home);
        btnSettings = findViewById(R.id.btn_settings);
        btnHelp = findViewById(R.id.btn_help);

        // Bottom Navigation
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Buttons
        btnRegisterChild = findViewById(R.id.btnRegisterChild);
        btnAddChild = findViewById(R.id.btnAddChild);

        btnRegisterChild.setOnClickListener(v ->
                startActivity(new Intent(this, ChildRegister.class)));

        btnAddChild.setOnClickListener(v ->
                startActivity(new Intent(this, ChildRegister.class)));

        btnManageChildren.setOnClickListener(v -> showManageChildrenDialog());

        // Set click listeners for vaccine cards
        urgentVaccineCard.setOnClickListener(v -> showVaccineDialog("URGENT"));
        dueVaccineCard.setOnClickListener(v -> showVaccineDialog("DUE"));
        nextVaccineCard.setOnClickListener(v -> showVaccineDialog("NEXT SCHEDULED"));
        missedVaccineCard.setOnClickListener(v -> showVaccineDialog("MISSED VACCINES"));
        healthConditionCard.setOnClickListener(v -> showHealthConditionsDialog());

        // Setup spinner adapter
        childrenAdapter = new ArrayAdapter<Child>(this, android.R.layout.simple_spinner_item, childrenList) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                if (position < childrenList.size()) {
                    textView.setText(childrenList.get(position).getName());
                }
                return textView;
            }

            @Override
            public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                TextView textView = (TextView) super.getDropDownView(position, convertView, parent);
                if (position < childrenList.size()) {
                    textView.setText(childrenList.get(position).getName());
                }
                return textView;
            }
        };
        childrenAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerChildren.setAdapter(childrenAdapter);

        spinnerChildren.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < childrenList.size()) {
                    Child selectedChild = childrenList.get(position);
                    selectedChildId = selectedChild.getId();
                    // Save selection to database
                    mDatabase.child("users").child(currentUserId).child("selectedChild")
                            .setValue(selectedChildId);
                    // Load child data
                    loadChildData(selectedChildId);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
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


    private void setupNotificationBadge() {
        // Create badge for notifications menu item
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        View notificationView = bottomNavigationView.findViewById(R.id.nav_notifications);
        notificationBadge = new TextView(this);

        notificationBadge.setBackgroundResource(R.drawable.badge_background);
        notificationBadge.setTextColor(Color.WHITE);
        notificationBadge.setTextSize(10);
        notificationBadge.setGravity(Gravity.CENTER);
        notificationBadge.setPadding(4, 2, 4, 2);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.addRule(RelativeLayout.ALIGN_TOP, notificationView.getId());
        params.addRule(RelativeLayout.ALIGN_RIGHT, notificationView.getId());
        params.topMargin = 5;
        params.rightMargin = 15;

        bottomNavigationView.addView(notificationBadge, params);
        notificationBadge.setVisibility(View.GONE);

        // Update notification badge on resume
        updateNotificationBadge();
    }

    // Add this method:
    private void updateNotificationBadge() {
        mDatabase.child("users").child(currentUserId).child("notifications")
                .orderByChild("read").equalTo(false)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int unreadCount = (int) snapshot.getChildrenCount();
                        hasUnreadNotifications = unreadCount > 0;

                        runOnUiThread(() -> {
                            if (unreadCount > 0) {
                                notificationBadge.setText(String.valueOf(unreadCount > 99 ? "99+" : unreadCount));
                                notificationBadge.setVisibility(View.VISIBLE);
                            } else {
                                notificationBadge.setVisibility(View.GONE);
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Handle error
                    }
                });
    }

    // Update the bottom navigation setup in setupBottomNavigation():
    private void setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_digital_records) {
                startActivity(new Intent(this, DigitalRecords.class));
                return true;
            } else if (itemId == R.id.nav_appointment) {
                startActivity(new Intent(this, AppointmentsActivity.class));
                return true;
            } else if (itemId == R.id.nav_dashboardd) {
                checkExistingChildData();
                return true;
            } else if (itemId == R.id.nav_notifications) {
                // Clear badge when notifications are opened
                notificationBadge.setVisibility(View.GONE);
                startActivity(new Intent(this, ANCNotificationsActivity.class));
                return true;
            } else if (itemId == R.id.nav_child_profile) {
                if (selectedChildId != null) {
                    Intent intent = new Intent(this, ChildProfile.class);
                    intent.putExtra("CHILD_ID", selectedChildId);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Please select a child first", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });

        bottomNavigationView.setSelectedItemId(R.id.nav_dashboardd);
    }


    private void loadChildrenList() {
        mDatabase.child("users").child(currentUserId).child("children")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        childrenList.clear();

                        if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                            for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                                String childId = childSnapshot.getKey();
                                String name = childSnapshot.child("name").getValue(String.class);
                                String dob = childSnapshot.child("dateOfBirth").getValue(String.class);

                                if (name != null && childId != null) {
                                    childrenList.add(new Child(childId, name, dob));
                                }
                            }

                            childSelectionLayout.setVisibility(View.VISIBLE);
                            existingChildDashboard.setVisibility(View.VISIBLE);
                            newUserCard.setVisibility(View.GONE);
                            bottomNavigationView.setVisibility(View.VISIBLE);

                            childrenAdapter.notifyDataSetChanged();

                            loadSelectedChild();

                        } else {
                            showNewUserUI();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ChildVaccineActivity.this, "Failed to load children list", Toast.LENGTH_SHORT).show();
                    }
                });
        // After loadChildrenList() call
        mDatabase.child("users").child(currentUserId).child("childDetails")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // Old data exists, offer migration
                            showMigrationDialog();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

    }
    private void showMigrationDialog(){}

    private void loadSelectedChild() {
        mDatabase.child("users").child(currentUserId).child("selectedChild")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            selectedChildId = snapshot.getValue(String.class);
                            for (int i = 0; i < childrenList.size(); i++) {
                                if (childrenList.get(i).getId().equals(selectedChildId)) {
                                    spinnerChildren.setSelection(i);
                                    loadChildData(selectedChildId);
                                    break;
                                }
                            }
                        } else if (!childrenList.isEmpty()) {
                            selectedChildId = childrenList.get(0).getId();
                            spinnerChildren.setSelection(0);
                            mDatabase.child("users").child(currentUserId).child("selectedChild")
                                    .setValue(selectedChildId);
                            loadChildData(selectedChildId);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Handle error
                    }
                });
    }

    private void loadChildData(String childId) {
        mDatabase.child("users").child(currentUserId).child("children").child(childId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            showChildData(snapshot);
                            generateVaccineRecommendations(snapshot);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ChildVaccineActivity.this, "Failed to load child data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showManageChildrenDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this,
                R.style.DatePickerDialogTheme);
        builder.setTitle("Manage Children");

        if (childrenList.isEmpty()) {
            builder.setMessage("No children registered.");
        } else {
            String[] childNames = new String[childrenList.size()];
            for (int i = 0; i < childrenList.size(); i++) {
                childNames[i] = childrenList.get(i).getName();
            }

            builder.setItems(childNames, (dialog, which) -> {
                Child selectedChild = childrenList.get(which);
                showChildOptionsDialog(selectedChild);
            });
        }

        builder.setPositiveButton("Add New Child", (dialog, which) -> {
            startActivity(new Intent(this, ChildRegister.class));
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showChildOptionsDialog(Child child) {
        String[] options = {"View Profile", "Set as Active", "Delete Child"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Options for " + child.getName());
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // View Profile
                    Intent intent = new Intent(this, ChildProfile.class);
                    intent.putExtra("CHILD_ID", child.getId());
                    startActivity(intent);
                    break;
                case 1: // Set as Active
                    selectedChildId = child.getId();
                    mDatabase.child("users").child(currentUserId).child("selectedChild")
                            .setValue(selectedChildId);
                    Toast.makeText(this, child.getName() + " set as active", Toast.LENGTH_SHORT).show();
                    loadChildData(selectedChildId);
                    break;
                case 2: // Delete Child
                    showDeleteConfirmationDialog(child);
                    break;
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showDeleteConfirmationDialog(Child child) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this,
                R.style.DatePickerDialogTheme);
        builder.setTitle("Delete Child");
        builder.setMessage("Are you sure you want to delete " + child.getName() + "'s profile? This action cannot be undone.");

        builder.setPositiveButton("Delete", (dialog, which) -> {
            deleteChild(child.getId());
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void deleteChild(String childId) {
        mDatabase.child("users").child(currentUserId).child("children").child(childId)
                .removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Child deleted successfully", Toast.LENGTH_SHORT).show();

                        // Remove from list
                        childrenList.removeIf(child -> child.getId().equals(childId));
                        childrenAdapter.notifyDataSetChanged();

                        if (!childrenList.isEmpty()) {
                            // Select another child
                            Child newSelected = childrenList.get(0);
                            selectedChildId = newSelected.getId();
                            mDatabase.child("users").child(currentUserId).child("selectedChild")
                                    .setValue(selectedChildId);
                            loadChildData(selectedChildId);
                        } else {
                            // No children left
                            showNewUserUI();
                        }
                    } else {
                        Toast.makeText(this, "Failed to delete child", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkExistingChildData() {
        mDatabase.child("users").child(currentUserId).child("children")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                            loadChildrenList();
                        } else {
                            showNewUserUI();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ChildVaccineActivity.this, "Failed to load data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showNewUserUI() {
        newUserCard.setVisibility(View.VISIBLE);
        existingChildDashboard.setVisibility(View.GONE);
        childSelectionLayout.setVisibility(View.GONE);
        bottomNavigationView.setVisibility(View.GONE);
    }

    private void showChildData(DataSnapshot snapshot) {
        String name = snapshot.child("name").getValue(String.class);
        String dob = snapshot.child("dateOfBirth").getValue(String.class);
        String gender = snapshot.child("gender").getValue(String.class);
        String weight = snapshot.child("birthWeight").getValue(String.class);
        String healthCondition = snapshot.child("healthCondition").getValue(String.class);

        tvChildName.setText(name != null ? name : "Not provided");
        tvChildAge.setText("Age: " + calculateAge(dob));
        tvChildGender.setText(gender != null ? gender : "Not specified");
        tvBirthWeight.setText(weight != null ? weight + " kg" : "Not recorded");

        if (healthCondition != null && !healthCondition.isEmpty()) {
            hasSpecialConditions = true;
            tvHealthConditions.setText(healthCondition);
            tvHealthStatus.setText("Special Care");
            healthConditionCard.setCardBackgroundColor(getResources().getColor(R.color.light_purple));
            healthConditionCard.setStrokeColor(getResources().getColor(R.color.primary_purple));
        } else {
            hasSpecialConditions = false;
            tvHealthConditions.setText("None reported");
            tvHealthStatus.setText("Normal");
        }

        imgChildAvatar.setImageResource(
                "Male".equalsIgnoreCase(gender)
                        ? R.drawable.boyavatar
                        : R.drawable.girlavatar
        );
    }

    private String calculateAge(String dob) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Calendar birth = Calendar.getInstance();
            birth.setTime(sdf.parse(dob));
            Calendar today = Calendar.getInstance();

            int years = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR);
            int months = today.get(Calendar.MONTH) - birth.get(Calendar.MONTH);

            childAgeInMonths = years * 12 + months;

            if (years > 0) {
                return years + (years == 1 ? " year" : " years") +
                        (months > 0 ? ", " + months + (months == 1 ? " month" : " months") : "");
            } else {
                return months + (months == 1 ? " month" : " months");
            }
        } catch (Exception e) {
            childAgeInMonths = 0;
            return "Age not available";
        }
    }

    private void generateVaccineRecommendations(DataSnapshot snapshot) {
        Calendar today = Calendar.getInstance();
        Calendar nextWeek = Calendar.getInstance();
        nextWeek.add(Calendar.DAY_OF_MONTH, 7);
        Calendar nextMonth = Calendar.getInstance();
        nextMonth.add(Calendar.DAY_OF_MONTH, 30);

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

        // URGENT VACCINE (within 3 days)
        if (childAgeInMonths == 6) {
            tvUrgentVaccine.setText("Pentavalent Vaccine (3rd dose)");
            tvUrgentReason.setText("Standard 6-month immunization");
            tvUrgentDays.setText("IN 3 DAYS");
            urgentVaccineCard.setVisibility(View.VISIBLE);
        } else {
            tvUrgentVaccine.setText("No urgent vaccines required");
            tvUrgentReason.setText("All vaccines up to date");
            urgentVaccineCard.setVisibility(View.GONE);
        }

        // DUE VACCINE (within 7 days)
        if (childAgeInMonths == 9) {
            tvDueVaccine.setText("Measles-Rubella Vaccine (1st dose)");
            tvDueDate.setText("Due by: " + sdf.format(nextWeek.getTime()));
            dueVaccineCard.setVisibility(View.VISIBLE);
        } else if (childAgeInMonths == 12) {
            tvDueVaccine.setText("Hepatitis A Vaccine");
            tvDueDate.setText("Due by: " + sdf.format(nextWeek.getTime()));
            dueVaccineCard.setVisibility(View.VISIBLE);
        } else {
            tvDueVaccine.setText("No due vaccines");
            tvDueDate.setText("");
            dueVaccineCard.setVisibility(View.VISIBLE);
        }

        // NEXT VACCINE SCHEDULE
        if (childAgeInMonths < 18) {
            tvNextVaccine.setText("DPT Booster");
            tvNextDate.setText("Scheduled: " + sdf.format(nextMonth.getTime()));
            nextVaccineCard.setVisibility(View.VISIBLE);
        } else if (childAgeInMonths >= 18 && childAgeInMonths < 24) {
            tvNextVaccine.setText("Measles-Rubella (2nd dose)");
            tvNextDate.setText("Scheduled: " + sdf.format(nextMonth.getTime()));
            nextVaccineCard.setVisibility(View.VISIBLE);
        } else {
            tvNextVaccine.setText("No upcoming vaccines");
            tvNextDate.setText("Schedule complete for current age");
            nextVaccineCard.setVisibility(View.VISIBLE);
        }

        // MISSED VACCINES
        List<String> missedVaccines = new ArrayList<>();

        if (childAgeInMonths > 2 && childAgeInMonths < 6) {
            missedVaccines.add("BCG");
            tvMissedCount.setText("1");
        } else if (childAgeInMonths > 6 && childAgeInMonths < 9) {
            missedVaccines.add("OPV (Oral Polio)");
            missedVaccines.add("Rotavirus");
            tvMissedCount.setText("2");
        } else if (childAgeInMonths > 12) {
            missedVaccines.add("Vitamin A Supplement");
            tvMissedCount.setText("1");
        }

        if (!missedVaccines.isEmpty()) {
            tvMissedVaccines.setText("Missed: " + String.join(", ", missedVaccines));
            tvMissedRecommendation.setText("Consult healthcare provider for catch-up schedule");
            missedVaccineCard.setVisibility(View.VISIBLE);
        } else {
            tvMissedVaccines.setText("No missed vaccines");
            tvMissedRecommendation.setText("All vaccinations are up to date");
            tvMissedCount.setText("0");
            missedVaccineCard.setVisibility(View.VISIBLE);
        }
    }

    private void showVaccineDialog(String type) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this,
                R.style.DatePickerDialogTheme);
        builder.setTitle(type);
        String message = "No recommendations available yet.\n\n";
        builder.setMessage(message);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showHealthConditionsDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this,
                R.style.DatePickerDialogTheme);
        builder.setTitle("Health Conditions");
        String conditions = tvHealthConditions.getText().toString();
        String message = hasSpecialConditions ?
                "Current health conditions: " + conditions :
                "No special health conditions reported.";
        builder.setMessage(message);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChildrenList();
            updateNotificationBadge(); // Update badge count
        }
    }

