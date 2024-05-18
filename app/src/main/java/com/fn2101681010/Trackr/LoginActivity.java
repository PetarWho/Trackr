package com.fn2101681010.Trackr;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private Button registerButton;
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_LOGGED_IN = "isLoggedIn";
    // You can also include UserDataSource if you need to validate login credentials against the database

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        registerButton = findViewById(R.id.login_button_register);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to register activity
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
                finish();
            }
        });
        // Initialize views
        emailEditText = findViewById(R.id.edit_text_email);
        passwordEditText = findViewById(R.id.edit_text_password);
        loginButton = findViewById(R.id.login_button_login);

        // Set click listener for login button
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Perform login

                // Save login status
                saveLoginStatus(true);

                // Navigate to main activity
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish(); // Close login activity to prevent going back to it by pressing back button
            }
        });
    }

    private void saveLoginStatus(boolean isLoggedIn) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(KEY_LOGGED_IN, isLoggedIn);
        editor.apply();
    }

    private void login() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Perform validation (e.g., check if email and password are not empty)
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate user credentials against the database
        UserDataSource dataSource = new UserDataSource(this);
        dataSource.open();
        User user = dataSource.getUserByEmail(email);
        dataSource.close();

        if (user != null && user.getPassword().equals(password)) {
            // Login successful, navigate to the main activity
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish(); // Close the login activity to prevent going back to it by pressing back button
        } else {
            // Login failed, show error message
            Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show();
        }
    }
}
