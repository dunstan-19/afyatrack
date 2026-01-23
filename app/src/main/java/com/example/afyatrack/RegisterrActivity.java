package com.example.afyatrack;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterrActivity extends AppCompatActivity {

    private EditText etUsername, etEmail, etPassword, etCPassword;
    private Button btnSignup;
    private TextView tvback;
    private ImageView ivTogglePassword, ivToggleCPassword;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private boolean passwordShowing = false;
    private boolean cPasswordShowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_registerr);

        // Initialize views
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etemail);
        etPassword = findViewById(R.id.etPassword);
        etCPassword = findViewById(R.id.etCPassword);
        btnSignup = findViewById(R.id.btnSignup);
        tvback = findViewById(R.id.tvback);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        ivToggleCPassword = findViewById(R.id.ivToggleCPassword);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("Users");

        // Back to login
        tvback.setOnClickListener(v -> {
            startActivity(new Intent(RegisterrActivity.this, MainActivity.class));
            finish();
        });

        // Toggle password visibility
        ivTogglePassword.setOnClickListener(v -> togglePasswordVisibility(etPassword, ivTogglePassword, true));
        ivToggleCPassword.setOnClickListener(v -> togglePasswordVisibility(etCPassword, ivToggleCPassword, false));

        // Signup button
        btnSignup.setOnClickListener(v -> validateAndRegister());
    }

    private void togglePasswordVisibility(EditText editText, ImageView imageView, boolean isMainPassword) {
        boolean showing = isMainPassword ? passwordShowing : cPasswordShowing;
        if (showing) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            imageView.setImageResource(R.drawable.eyeclosed);
        } else {
            editText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            imageView.setImageResource(R.drawable.eyeopen);
        }
        if (isMainPassword) passwordShowing = !passwordShowing;
        else cPasswordShowing = !cPasswordShowing;
        editText.setSelection(editText.length());
    }

    private void validateAndRegister() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etCPassword.getText().toString().trim();

        if (username.isEmpty()) {
            Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show();
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
        } else if (!isValidPassword(password)) {
            Toast.makeText(this, "Password must be at least 5 characters and contain both letters and numbers", Toast.LENGTH_SHORT).show();
        } else if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
        } else {
            registerUser(username, email, password);
        }
    }

    private void registerUser(String username, String email, String password) {
        btnSignup.setEnabled(false);
        btnSignup.setText("Creating Account...");
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        User newUser = new User(username, email);

                        // Save to Realtime Database
                        mDatabase.child(userId).setValue(newUser)
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        Toast.makeText(RegisterrActivity.this, "Registration Successful! Please log in.", Toast.LENGTH_LONG).show();

                                        // Log out the just registered user to avoid duplicate sessions
                                        mAuth.signOut();

                                        // Redirect to login page
                                        Intent intent = new Intent(RegisterrActivity.this, MainActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(RegisterrActivity.this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Exception e = task.getException();
                        if (e instanceof FirebaseAuthUserCollisionException) {
                            Toast.makeText(RegisterrActivity.this, "This email is already registered. Please log in.", Toast.LENGTH_LONG).show();
                            startActivity(new Intent(RegisterrActivity.this, MainActivity.class));
                            finish();
                        } else {
                            Toast.makeText(RegisterrActivity.this, "Registration Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private boolean isValidPassword(String password) {
        if (password.length() < 5) return false;
        boolean hasLetter = false, hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) hasLetter = true;
            else if (Character.isDigit(c)) hasDigit = true;
        }
        return hasLetter && hasDigit;
    }
}

// User model (no roles)
class User {
    public String username, email;

    public User() {
        // Default constructor for Firebase
    }

    public User(String username, String email) {
        this.username = username;
        this.email = email;
    }
}
