package com.fn2101681010.Trackr;

import static com.google.firebase.firestore.DocumentChange.Type.ADDED;
import static com.google.firebase.firestore.DocumentChange.Type.MODIFIED;
import static com.google.firebase.firestore.DocumentChange.Type.REMOVED;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final long LOCATION_UPDATE_INTERVAL = 20000; // 20 seconds
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_USER_EMAIL = "userEmail";
    private GoogleMap mMap;
    private FirebaseFirestore db;
    private Handler locationUpdateHandler = new Handler();
    private FusedLocationProviderClient fusedLocationClient;
    private Map<String, Marker> userMarkers = new HashMap<>();
    private ImageButton navPeople;
    private ImageButton navProfile;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        navPeople = findViewById(R.id.nav_people);

        navPeople.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PeopleActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                overridePendingTransition(0, 0);
                startActivity(intent);
            }
        });

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            initMap();
            startLocationUpdates();
            startLocationService();
        }
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
    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        mapFragment.getMapAsync(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, initialize map and start location updates
                initMap();
                startLocationUpdates();
                startLocationService();
            } else {
                // Permission denied, show a message or handle it gracefully
                Toast.makeText(this, "Location permission denied. Map functionality will be limited.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Fetch user's own location from Firestore and add a marker
        String userEmail = getUserEmail();
        if (userEmail != null) {
            DocumentReference userRef = db.collection("users").document(userEmail);
            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    double latitude = documentSnapshot.getDouble("latitude");
                    double longitude = documentSnapshot.getDouble("longitude");
                    LatLng userLocation = new LatLng(latitude, longitude);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
                }
            });
        }

        // Enable user location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        setUpFirestoreListener();
    }

    private void setUpFirestoreListener() {
        db.collection("users").addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                // Handle errors
                return;
            }

            for (DocumentChange dc : snapshot.getDocumentChanges()) {
                DocumentSnapshot document = dc.getDocument();
                String username = document.getString("username");
                double latitude = document.getDouble("latitude");
                double longitude = document.getDouble("longitude");
                LatLng userLocation = new LatLng(latitude, longitude);

                // Check the type of change (added, modified, or removed)
                switch (dc.getType()) {
                    case ADDED:
                    case MODIFIED:
                        // Update or add the marker on the map
                        addOrUpdateMarker(username, userLocation);
                        break;
                    case REMOVED:
                        // Remove the marker from the map if the user is removed from Firestore
                        removeMarker(username);
                        break;
                }
            }
        });
    }

    private void addOrUpdateMarker(String username, LatLng userLocation) {
        Marker existingMarker = userMarkers.get(username);
        if (existingMarker != null) {
            // Update existing marker
            existingMarker.setPosition(userLocation);
        } else {
            // Add new marker
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(userLocation)
                    .title(username);
            Marker marker = mMap.addMarker(markerOptions);
            userMarkers.put(username, marker);
        }
    }

    private void removeMarker(String username) {
        Marker markerToRemove = userMarkers.get(username);
        if (markerToRemove != null) {
            markerToRemove.remove();
            userMarkers.remove(username);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop location updates
        stopLocationUpdates();
        stopLocationService();
    }

    private void startLocationUpdates() {
        locationUpdateHandler.postDelayed(locationRunnable, LOCATION_UPDATE_INTERVAL);
    }

    private void stopLocationUpdates() {
        locationUpdateHandler.removeCallbacks(locationRunnable);
    }

    private final Runnable locationRunnable = new Runnable() {
        @Override
        public void run() {
            getCurrentLocation();
            locationUpdateHandler.postDelayed(this, LOCATION_UPDATE_INTERVAL);
        }
    };

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener(MainActivity.this, location -> {
                if (location != null) {
                    saveLocationToFirestore(location.getLatitude(), location.getLongitude());
                }
            });
        }
    }

    private void saveLocationToFirestore(double latitude, double longitude) {
        String userEmail = getUserEmail();
        if (userEmail != null) {
            DocumentReference userRef = db.collection("users").document(userEmail);
            Map<String, Object> updates = new HashMap<>();
            updates.put("latitude", latitude);
            updates.put("longitude", longitude);
            userRef.update(updates).addOnSuccessListener(aVoid -> {
                // Location updated successfully
            }).addOnFailureListener(e -> {
                Toast.makeText(MainActivity.this, "Failed to update location.", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private String getUserEmail() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(KEY_USER_EMAIL, null);
    }

    private void startLocationService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        startService(serviceIntent);
    }

    private void stopLocationService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        stopService(serviceIntent);
    }
}
