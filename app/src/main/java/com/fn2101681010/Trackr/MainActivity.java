package com.fn2101681010.Trackr;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

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
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.FirebaseApp;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Check if location permission is granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request location permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Permission has been granted, initialize map and start location updates
            initMap();
            startLocationUpdates();
        }
    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, initialize map and start location updates
                initMap();
                startLocationUpdates();
            } else {
                // Permission denied, show a message or handle it gracefully
                Toast.makeText(this, "Location permission denied. Map functionality will be limited.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Fetch all users from Firestore
        db.collection("users").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (DocumentSnapshot document : task.getResult()) {
                    String username = document.getString("username");
                    double latitude = document.getDouble("latitude");
                    double longitude = document.getDouble("longitude");
                    LatLng userLocation = new LatLng(latitude, longitude);
                    MarkerOptions markerOptions = new MarkerOptions()
                            .position(userLocation)
                            .title(username);
                    mMap.addMarker(markerOptions);
                    builder.include(userLocation);
                }
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 50));
            } else {
                Toast.makeText(this, "Failed to load users.", Toast.LENGTH_SHORT).show();
            }
        });

        // Enable user location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop location updates
        stopLocationUpdates();
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
}
