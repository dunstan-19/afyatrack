package com.example.afyatrack;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvSign, tvForgotPassword;
    private LottieAnimationView lLoading;
    private ImageView ivTogglePassword;
    private FirebaseAuth mAuth;
    private boolean passwordShowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        etEmail = findViewById(R.id.etemail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnlogin);
        tvSign = findViewById(R.id.tvsign);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        lLoading = findViewById(R.id.lLoading);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);

        mAuth = FirebaseAuth.getInstance();

        // Eye icon toggle for password visibility
        ivTogglePassword.setOnClickListener(v -> togglePasswordVisibility());

        // Login button click listener
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(MainActivity.this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
            } else if (password.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter your password", Toast.LENGTH_SHORT).show();
            } else {
                loginUser(email, password);
            }
        });

        // Sign up text click listener
        tvSign.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RegisterrActivity.class));
        });

        // Forgot password text click listener
        tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ForgotPassword.class));
        });
    }

    private void togglePasswordVisibility() {
        if (passwordShowing) {
            hidePassword();
        } else {
            showPassword();
        }
    }

    private void showPassword() {
        etPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        ivTogglePassword.setImageResource(R.drawable.eyeopen);
        passwordShowing = true;
        etPassword.setSelection(etPassword.length());
    }

    private void hidePassword() {
        etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        ivTogglePassword.setImageResource(R.drawable.eyeclosed);
        passwordShowing = false;
        etPassword.setSelection(etPassword.length());
    }

    private void loginUser(String email, String password) {
        lLoading.setVisibility(View.VISIBLE);
        lLoading.playAnimation();
        btnLogin.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    lLoading.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Toast.makeText(MainActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(MainActivity.this, Welcome.class));
                            finish();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
