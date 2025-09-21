package com.fn2101681010.Trackr;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class ProfileActivity extends AppCompatActivity {

    private TextView textViewUserName;
    private TextView textViewUserEmail;
    private TextView textViewGroupCount;
    private Button buttonLogout;
    private ImageButton navMap;
    private ImageButton navPeople;
    private CheckBox checkboxBackgroundTracking;

    private FirebaseFirestore db;
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_LOGGED_IN = "isLoggedIn";
    private static final String KEY_BACKGROUND_TRACKING = "backgroundTracking";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        navMap = findViewById(R.id.nav_map);
        navPeople = findViewById(R.id.nav_people);

        textViewUserName = findViewById(R.id.textViewUserName);
        textViewUserEmail = findViewById(R.id.textViewUserEmail);
        textViewGroupCount = findViewById(R.id.textViewGroupCount);
        buttonLogout = findViewById(R.id.buttonLogout);
        checkboxBackgroundTracking = findViewById(R.id.checkboxBackgroundTracking);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isTrackingEnabled = prefs.getBoolean(KEY_BACKGROUND_TRACKING, false);
        checkboxBackgroundTracking.setChecked(isTrackingEnabled);

        if (isTrackingEnabled) {
            startLocationService();
        }

        checkboxBackgroundTracking.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(KEY_BACKGROUND_TRACKING, isChecked);
            editor.apply();

            if (isChecked) {
                startLocationService();
                Toast.makeText(ProfileActivity.this, "Background tracking enabled", Toast.LENGTH_SHORT).show();
            } else {
                stopLocationService();
                Toast.makeText(ProfileActivity.this, "Background tracking disabled", Toast.LENGTH_SHORT).show();
            }
        });

        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();

        fetchUserProfile();
        fetchGroupCount();

        navMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                overridePendingTransition(0, 0);
                startActivity(intent);
            }
        });

        navPeople.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileActivity.this, PeopleActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                overridePendingTransition(0, 0);
                startActivity(intent);
            }
        });

        buttonLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });
    }

    private void startLocationService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        startService(serviceIntent);
    }

    private void stopLocationService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        stopService(serviceIntent);
    }

    private void fetchUserProfile() {
        String userEmail = getUserEmail();
        if (userEmail == null) {
            Toast.makeText(this, "User email not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(userEmail).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String userName = documentSnapshot.getString("username");
                textViewUserName.setText(userName);
                textViewUserEmail.setText(userEmail);
            } else {
                Toast.makeText(this, "User not found.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to fetch user profile.", Toast.LENGTH_SHORT).show();
        });
    }

    private void fetchGroupCount() {
        String userEmail = getUserEmail();
        if (userEmail == null) {
            Toast.makeText(this, "User email not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("groups").whereArrayContains("members", userEmail).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                int groupCount = 0;
                for (QueryDocumentSnapshot document : task.getResult()) {
                    groupCount++;
                }
                if(groupCount != 1){
                    textViewGroupCount.setText("Member of " + groupCount + " groups");
                } else{
                    textViewGroupCount.setText("Member of " + groupCount + " group");
                }
            } else {
                Toast.makeText(this, "Failed to fetch group count.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getUserEmail() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(KEY_USER_EMAIL, null);
    }

    private void logout() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_USER_EMAIL);
        editor.remove(KEY_LOGGED_IN);
        editor.apply();

        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        overridePendingTransition(0, 0);
    }
}
