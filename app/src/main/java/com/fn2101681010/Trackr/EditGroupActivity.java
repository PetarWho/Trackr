package com.fn2101681010.Trackr;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditGroupActivity extends AppCompatActivity {

    private EditText groupNameEditText, passcodeEditText;
    private ImageButton goBackButton;
    private TextView groupNameTitle;
    private Button updateButton;
    private Button leaveButton;
    private String groupName;
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_USER_EMAIL = "userEmail";
    private FirebaseFirestore db;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_group);

        groupNameEditText = findViewById(R.id.groupNameEditText);
        passcodeEditText = findViewById(R.id.passcodeEditText);
        goBackButton = findViewById(R.id.goBackButton);
        updateButton = findViewById(R.id.updateButton);
        groupNameTitle = findViewById(R.id.groupTitleTextView);
        leaveButton = findViewById(R.id.leaveButton);
        db = FirebaseFirestore.getInstance();
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);


        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("groupName")) {
            String value = intent.getStringExtra("groupName");
            if (value == null || value.isEmpty()) {
                Toast.makeText(this, "Invalid group name", Toast.LENGTH_SHORT).show();
                return;
            }
            groupName = value.substring(0, value.indexOf("(")).trim();
            groupNameTitle.setText(groupName);
            groupNameEditText.setText(groupName);
        } else {
            Toast.makeText(this, "Error loading group data", Toast.LENGTH_SHORT).show();
            finish();
        }

        goBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateGroup();
            }
        });

        leaveButton.setOnClickListener(v -> leaveGroup());
    }

    private void updateGroup() {
        String updatedGroupName = groupNameEditText.getText().toString().trim();
        String updatedPasscode = passcodeEditText.getText().toString().trim();

        if (TextUtils.isEmpty(updatedGroupName) || TextUtils.isEmpty(updatedPasscode)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = prefs.getString(KEY_USER_EMAIL, null);
        if (email == null) {
            Toast.makeText(this, "Please login again and try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("groups")
                .whereEqualTo("creator", email)
                .whereEqualTo("groupName", groupName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(EditGroupActivity.this, "You are not the owner of that group", Toast.LENGTH_SHORT).show();
                    } else {
                        db.collection("groups")
                                .whereEqualTo("groupName", updatedGroupName)
                                .get()
                                .addOnSuccessListener(existingGroupQuery -> {
                                    if (!existingGroupQuery.isEmpty() && !groupName.equals(updatedGroupName)) {
                                        Toast.makeText(EditGroupActivity.this, "A group with this name already exists", Toast.LENGTH_SHORT).show();
                                    } else {
                                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                                            String groupId = documentSnapshot.getId();

                                            DocumentReference groupRef = db.collection("groups").document(groupId);
                                            Map<String, Object> updates = new HashMap<>();
                                            updates.put("groupName", updatedGroupName);
                                            updates.put("passcode", updatedPasscode);

                                            groupRef.update(updates)
                                                    .addOnSuccessListener(aVoid -> {
                                                        Toast.makeText(EditGroupActivity.this, "Group updated successfully", Toast.LENGTH_SHORT).show();
                                                        updateGroupList();
                                                        finish();
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(EditGroupActivity.this, "Failed to update group: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                    });
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(EditGroupActivity.this, "Error checking for existing group name: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EditGroupActivity.this, "Error fetching group: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private void leaveGroup() {
        String groupName = groupNameEditText.getText().toString().trim();
        String passcode = passcodeEditText.getText().toString().trim();
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
                        groupRef.get().addOnSuccessListener(documentSnapshot -> {
                            List<String> members = (List<String>) documentSnapshot.get("members");
                            String creator = documentSnapshot.getString("creator");

                            if (members != null && members.contains(userEmail)) {
                                members.remove(userEmail);
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("members", members);

                                if (members.isEmpty()) {
                                    groupRef.delete().addOnSuccessListener(aVoid -> {
                                        Toast.makeText(EditGroupActivity.this, "Group deleted successfully", Toast.LENGTH_SHORT).show();
                                        updateGroupList();
                                        finish();
                                    }).addOnFailureListener(e -> {
                                        Toast.makeText(EditGroupActivity.this, "Failed to delete group: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                                } else {
                                    if (creator.equals(userEmail)) {
                                        updates.put("creator", members.get(0));
                                    }
                                    groupRef.update(updates)
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(EditGroupActivity.this, "Left group successfully", Toast.LENGTH_SHORT).show();
                                                updateGroupList();
                                                finish();
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(EditGroupActivity.this, "Failed to leave group: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });
                                }
                            } else {
                                Toast.makeText(EditGroupActivity.this, "You are not a member of this group", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(EditGroupActivity.this, "Group not found or incorrect passcode", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private String getUserEmail() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(KEY_USER_EMAIL, null);
    }

    private void updateGroupList(){
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean("groupUpdated", true);
        editor.apply();
    }
}
