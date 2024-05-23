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
import java.util.Map;

public class EditGroupActivity extends AppCompatActivity {

    private EditText groupNameEditText, passcodeEditText;
    private ImageButton goBackButton;
    private TextView groupNameTitle;
    private Button updateButton;
    private String groupName;
    private static final String PREFS_NAME = "UserPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_group);

        groupNameEditText = findViewById(R.id.groupNameEditText);
        passcodeEditText = findViewById(R.id.passcodeEditText);
        goBackButton = findViewById(R.id.goBackButton);
        updateButton = findViewById(R.id.updateButton);
        groupNameTitle = findViewById(R.id.groupTitleTextView);
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
    }

    private void updateGroup() {
        String updatedGroupName = groupNameEditText.getText().toString().trim();
        String updatedPasscode = passcodeEditText.getText().toString().trim();

        if (TextUtils.isEmpty(updatedGroupName) || TextUtils.isEmpty(updatedPasscode)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("groups")
                .whereEqualTo("groupName", groupName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        String groupId = documentSnapshot.getId();

                        DocumentReference groupRef = db.collection("groups").document(groupId);
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("groupName", updatedGroupName);
                        updates.put("passcode", updatedPasscode);

                        groupRef.update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(EditGroupActivity.this, "Group updated successfully", Toast.LENGTH_SHORT).show();
                                    SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                                    editor.putBoolean("groupUpdated", true);
                                    editor.apply();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(EditGroupActivity.this, "Failed to update group: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EditGroupActivity.this, "Failed to find group: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
