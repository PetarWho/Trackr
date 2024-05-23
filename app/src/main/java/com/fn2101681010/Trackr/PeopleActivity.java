package com.fn2101681010.Trackr;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PeopleActivity extends AppCompatActivity {

    private static final String ARG_COLUMN_COUNT = "column-count";
    private int mColumnCount = 1;
    private ImageButton navMap;
    private ImageButton navProfile;
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final int REQUEST_CREATE_GROUP = 1;
    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private GroupAdapter groupAdapter;
    private List<String> groupList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_people);
        navMap = findViewById(R.id.nav_map);
        navProfile = findViewById(R.id.nav_profile);

        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        groupAdapter = new GroupAdapter(groupList);
        recyclerView.setAdapter(groupAdapter);

        fetchUserGroups();
        findViewById(R.id.btnCreateGroup).setOnClickListener(v -> createNewGroup());

        if (getIntent().getExtras() != null) {
            mColumnCount = getIntent().getExtras().getInt(ARG_COLUMN_COUNT);
        }

        navMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PeopleActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                overridePendingTransition(0, 0);
                startActivity(intent);
            }
        });
        navProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PeopleActivity.this, ProfileActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                overridePendingTransition(0, 0);
                startActivity(intent);
            }
        });

    }

    private void createNewGroup() {
        Intent intent = new Intent(PeopleActivity.this, NewGroupActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        overridePendingTransition(0, 0);
        startActivity(intent);
    }

    private void fetchUserGroups() {
        String userEmail = getUserEmail();
        if (userEmail == null) {
            Toast.makeText(this, "User email not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("groups").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    List<String> members = (List<String>) document.get("members");
                    if (members != null) {
                        if (members.contains(userEmail)) {
                            String groupName = document.getString("groupName");
                            long membersCount = members.size();
                            groupList.add(groupName + "\n(Members: " + membersCount + ")");
                        }
                    }
                }
                groupAdapter.notifyDataSetChanged();
            } else {
                Toast.makeText(this, "Failed to fetch groups.", Toast.LENGTH_SHORT).show();
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
