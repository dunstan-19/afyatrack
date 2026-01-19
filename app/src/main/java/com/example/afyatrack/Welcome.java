package com.example.afyatrack;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.navigation.NavigationView;
import java.util.ArrayList;
import java.util.List;

public class Welcome extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ViewPager2 viewPager;
    private LinearLayout dotContainer;
    private Handler sliderHandler = new Handler();
    private List<ViewPagerItem> viewPagerItems;
private Button btn_child_vaccine;
    // Runnable for auto-sliding
    private Runnable sliderRunnable = new Runnable() {
        @Override
        public void run() {
            if (viewPager.getCurrentItem() == viewPagerItems.size() - 1) {
                viewPager.setCurrentItem(0);
            } else {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // Initialize views
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        viewPager = findViewById(R.id.viewPager);
        dotContainer = findViewById(R.id.dotContainer);
        btn_child_vaccine = findViewById(R.id.btn_child_vaccine);

        btn_child_vaccine.setOnClickListener(v -> {
            Intent intent = new Intent(Welcome.this, ChildVaccineActivity.class);
            startActivity(intent);

        });

        // Set up toolbar
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Enable home button and set hamburger icon
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.menu_24px);
        }

        // Set navigation item selected listener
        navigationView.setNavigationItemSelectedListener(this);

        // Set up profile icon click listener
        ImageButton profileIcon = findViewById(R.id.profile_icon);
        profileIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(Welcome.this, "Profile Clicked", Toast.LENGTH_SHORT).show();
                // TODO: Open profile activity
            }
        });

        // Set up quick action buttons
        findViewById(R.id.btn_antenatal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAntenatalCare();
            }
        });

        findViewById(R.id.btn_child_vaccine).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openChildVaccination();
            }
        });

        // Initialize and setup ViewPager
        setupViewPager();
    }

    private void setupViewPager() {
        // Create sample data for ViewPager
        viewPagerItems = new ArrayList<>();
        //lottie animation
        viewPagerItems.add(new ViewPagerItem(R.raw.pregnant, "Antenatal Care", "Complete antenatal care tracking and reminders"));
        viewPagerItems.add(new ViewPagerItem( R.raw.vaccinate, "Child Vaccination", "Never miss your child's important vaccination dates"));
        viewPagerItems.add(new ViewPagerItem(R.raw.record, "Health Records", "Digital health records at your fingertips"));


        // Setup adapter
        ViewPagerAdapter adapter = new ViewPagerAdapter(viewPagerItems);
        viewPager.setAdapter(adapter);

        // Setup dot indicators
        setupDotIndicators();
        setCurrentDotIndicator(0);

        // ViewPager transformations for better UX
        CompositePageTransformer compositePageTransformer = new CompositePageTransformer();
        compositePageTransformer.addTransformer(new MarginPageTransformer(40));
        compositePageTransformer.addTransformer(new ViewPager2.PageTransformer() {
            @Override
            public void transformPage(@NonNull View page, float position) {
                float r = 1 - Math.abs(position);
                page.setScaleY(0.85f + r * 0.15f);
            }
        });

        viewPager.setPageTransformer(compositePageTransformer);

        // Auto slide every 3 seconds
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                setCurrentDotIndicator(position);
                sliderHandler.removeCallbacks(sliderRunnable);
                sliderHandler.postDelayed(sliderRunnable, 5000); // Slide every 5 seconds
            }
        });

        // Start auto-sliding
        sliderHandler.postDelayed(sliderRunnable, 5000);
    }

    private void setupDotIndicators() {
        ImageView[] dots = new ImageView[viewPagerItems.size()];
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8, 0, 8, 0);

        for (int i = 0; i < dots.length; i++) {
            dots[i] = new ImageView(this);
            dots[i].setImageDrawable(ContextCompat.getDrawable(
                    this,
                    R.drawable.dot_indicator_inactive
            ));
            dots[i].setLayoutParams(params);
            dotContainer.addView(dots[i]);
        }
    }

    private void setCurrentDotIndicator(int position) {
        int childCount = dotContainer.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ImageView imageView = (ImageView) dotContainer.getChildAt(i);
            if (i == position) {
                imageView.setImageDrawable(ContextCompat.getDrawable(
                        this,
                        R.drawable.dot_indicator
                ));
            } else {
                imageView.setImageDrawable(ContextCompat.getDrawable(
                        this,
                        R.drawable.dot_indicator_inactive
                ));
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            // Already on dashboard, just close drawer
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        } else if (id == R.id.nav_antenatal) {
            openAntenatalCare();
        } else if (id == R.id.nav_child_vaccination) {
            openChildVaccination();
        } else if (id == R.id.nav_settings) {
            openSettings();
        } else if (id == R.id.nav_help) {
            openHelp();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void openAntenatalCare() {
        Toast.makeText(this, "Opening Antenatal Care", Toast.LENGTH_SHORT).show();
        // TODO: Start AntenatalCareActivity
        // Intent intent = new Intent(this, AntenatalCareActivity.class);
        // startActivity(intent);
    }

    private void openChildVaccination() {
        Toast.makeText(this, "Opening Child Vaccination", Toast.LENGTH_SHORT).show();
        // TODO: Start ChildVaccinationActivity
            Intent intent = new Intent(Welcome.this, ChildVaccineActivity.class);
            startActivity(intent);
            // Close the cover activity
    }


    private void openSettings() {
        Toast.makeText(this, "Opening Settings", Toast.LENGTH_SHORT).show();
        // TODO: Start SettingsActivity
        // Intent intent = new Intent(this, SettingsActivity.class);
        // startActivity(intent);
    }

    private void openHelp() {
        Toast.makeText(this, "Opening Help", Toast.LENGTH_SHORT).show();
        // TODO: Start HelpActivity
        // Intent intent = new Intent(this, HelpActivity.class);
        // startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sliderHandler.removeCallbacks(sliderRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sliderHandler.postDelayed(sliderRunnable, 5000);
    }
}