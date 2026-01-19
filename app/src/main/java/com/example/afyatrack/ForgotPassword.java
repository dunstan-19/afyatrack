package com.example.afyatrack;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPassword extends AppCompatActivity {

    private EditText etEmail;
    private Button btnResetPassword;
    private TextView tvBackToLogin;
    private ProgressBar progressBar;
    private ImageView backButton;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        etEmail = findViewById(R.id.etEmail);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);
        progressBar = findViewById(R.id.progressBar);
        backButton = findViewById(R.id.backButton);
        // Set click listeners
        btnResetPassword.setOnClickListener(v -> resetPassword());

        tvBackToLogin.setOnClickListener(v -> {
            startActivity(new Intent(ForgotPassword.this, MainActivity.class));
            finish();
        });

        backButton.setOnClickListener(v -> {
            startActivity(new Intent(ForgotPassword.this, MainActivity.class));
            finish();
        });
    }

    private void resetPassword() {
        String email = etEmail.getText().toString().trim();

        // Validate email
        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email");
            etEmail.requestFocus();
            return;
        }

        // Show progress bar and disable button
        progressBar.setVisibility(View.VISIBLE);
        btnResetPassword.setEnabled(false);

        // Send password reset email
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(Task<Void> task) {
                        progressBar.setVisibility(View.GONE);
                        btnResetPassword.setEnabled(true);

                        if (task.isSuccessful()) {
                            // Password reset email sent successfully
                            showSuccessMessage();
                        } else {
                            // Failed to send reset email
                            String errorMessage = "Failed to send reset email. Please try again.";
                            if (task.getException() != null) {
                                errorMessage = task.getException().getMessage();
                            }
                            Toast.makeText(ForgotPassword.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void showSuccessMessage() {
        // Create a custom dialog or show a success message
        Toast.makeText(this, "Password reset email sent! Please check your inbox.", Toast.LENGTH_LONG).show();

        // Optionally, you can redirect to login after a delay
        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        startActivity(new Intent(ForgotPassword.this, MainActivity.class));
                        finish();
                    }
                },
                3000); // 3 seconds delay
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(ForgotPassword.this, MainActivity.class));
        finish();
    }
}