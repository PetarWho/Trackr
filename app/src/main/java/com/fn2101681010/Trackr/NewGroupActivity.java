package com.fn2101681010.Trackr;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewGroupActivity extends AppCompatActivity {

    private EditText groupNameEditText;
    private EditText passcodeEditText;
    private FirebaseFirestore db;
    private final List<String> members = new ArrayList<>();
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_USER_EMAIL = "userEmail";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_group);
        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();

        groupNameEditText = findViewById(R.id.editTextGroupName);
        passcodeEditText = findViewById(R.id.editTextPasscode);

        Button createGroupButton = findViewById(R.id.btnCreateGroup);
        createGroupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createGroup();
            }
        });
    }

    private void createGroup() {
        String groupName = groupNameEditText.getText().toString().trim();
        String passcode = passcodeEditText.getText().toString().trim();
        String email = getUserEmail();
        if(email == null){
            Toast.makeText(this, "Invalid user. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (groupName.isEmpty() || passcode.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        members.add(email);

        Map<String, Object> group = new HashMap<>();
        group.put("groupName", groupName);
        group.put("passcode", passcode);
        group.put("members", members);
        db.collection("groups").document()
                .set(group)
                .addOnSuccessListener(aVoid -> {
                    Intent intent = new Intent(NewGroupActivity.this, PeopleActivity.class);
                    overridePendingTransition(0, 0);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(NewGroupActivity.this, "Failed to create a group. Please try again", Toast.LENGTH_SHORT).show();
                });
    }
    private String getUserEmail() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(KEY_USER_EMAIL, null);
    }
}
