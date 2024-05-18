package com.fn2101681010.Trackr;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.Manifest;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private GoogleMap mMap;
    private UserDataSource dataSource;
    private Marker currentUserMarker;
    private Handler locationUpdateHandler = new Handler();
    private static final long LOCATION_UPDATE_INTERVAL = 20000; // 20 seconds
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_USER_EMAIL = "userEmail";
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize database
        dataSource = new UserDataSource(this);
        dataSource.open();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Check if location permission is not granted
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
            // Check if the permission is granted
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
        // Get all users from the database
        List<User> users = dataSource.getAllUsers();
        for (User user : users) {
            // Add a marker for each user
            LatLng userLocation = new LatLng(user.getLatitude(), user.getLongitude());
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(userLocation)
                    .title(user.getUsername()); // Set the username as marker title
            mMap.addMarker(markerOptions);
        }

        // Move the camera to show all markers
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(getBounds(users), 50));

        // Enable user location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
    }

    // Calculate the bounds for camera zoom
    private LatLngBounds getBounds(List<User> users) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (User user : users) {
            builder.include(new LatLng(user.getLatitude(), user.getLongitude()));
        }
        return builder.build();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Close database connection
        dataSource.close();
        // Stop location updates
        stopLocationUpdates();
    }

    private void startLocationUpdates() {
        locationUpdateHandler.postDelayed(locationRunnable, LOCATION_UPDATE_INTERVAL);
    }

    private void stopLocationUpdates() {
        locationUpdateHandler.removeCallbacks(locationRunnable);
    }

    private Runnable locationRunnable = new Runnable() {
        @Override
        public void run() {
            // Get current location and save to database
            getCurrentLocation();
            // Schedule the next location update
            locationUpdateHandler.postDelayed(this, LOCATION_UPDATE_INTERVAL);
        }
    };

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted, request it
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Permission granted, get current location
            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL).build();
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener(MainActivity.this, location -> {
                if (location != null) {
                    saveLocationToDatabase(location.getLatitude(), location.getLongitude());
                }
            });
        }
    }

    private void saveLocationToDatabase(double latitude, double longitude) {
        // Retrieve user email from SharedPreferences
        String userEmail = getUserEmail();
        if (userEmail != null) {
            // Get current user from database
            User currentUser = dataSource.getUserByEmail(userEmail);
            if (currentUser != null) {
                currentUser.setLatitude(latitude);
                currentUser.setLongitude(longitude);
                dataSource.updateUser(currentUser); // Update user in the database
            }
        }
    }

    private String getUserEmail() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(KEY_USER_EMAIL, null);
    }
}
