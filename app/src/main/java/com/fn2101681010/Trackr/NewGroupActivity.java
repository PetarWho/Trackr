package com.fn2101681010.Trackr;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewGroupActivity extends AppCompatActivity {

    private EditText groupNameEditText;
    private EditText passcodeEditText;
    private EditText groupNameEditTextJoin;
    private EditText passcodeEditTextJoin;
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
        groupNameEditTextJoin = findViewById(R.id.editTextGroupNameJoin);
        passcodeEditTextJoin = findViewById(R.id.editTextPasscodeJoin);

        Button createGroupButton = findViewById(R.id.btnCreateGroup);
        Button joinGroupButton = findViewById(R.id.btnJoinGroup);
        ImageButton goBackButton = findViewById(R.id.go_back_btn);
        createGroupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createGroup();
            }
        });

        joinGroupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                joinGroup();
            }
        });

        goBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }


    private void createGroup() {
        String groupName = groupNameEditText.getText().toString().trim();
        String passcode = passcodeEditText.getText().toString().trim();
        String email = getUserEmail();
        if (email == null) {
            Toast.makeText(this, "Invalid user. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (groupName.isEmpty() || passcode.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("groups")
                .whereEqualTo("groupName", groupName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        Toast.makeText(NewGroupActivity.this, "A group with this name already exists", Toast.LENGTH_SHORT).show();
                    } else {
                        members.add(email);

                        Map<String, Object> group = new HashMap<>();
                        group.put("groupName", groupName);
                        group.put("passcode", passcode);
                        group.put("members", members);
                        group.put("creator", email);

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
                });
    }

    private void joinGroup() {
        String groupName = groupNameEditTextJoin.getText().toString().trim();
        String passcode = passcodeEditTextJoin.getText().toString().trim();
        String userEmail = getUserEmail();

        if (groupName.isEmpty() || passcode.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("groups")
                .whereEqualTo("groupName", groupName)
                .whereEqualTo("passcode", passcode)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentReference groupRef = task.getResult().getDocuments().get(0).getReference();
                        groupRef.update("members", FieldValue.arrayUnion(userEmail))
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(NewGroupActivity.this, "Joined group successfully", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(NewGroupActivity.this, PeopleActivity.class);
                                    overridePendingTransition(0, 0);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(NewGroupActivity.this, "Failed to join group. Please try again", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(NewGroupActivity.this, "Group not found or incorrect passcode", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String getUserEmail() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(KEY_USER_EMAIL, null);
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
