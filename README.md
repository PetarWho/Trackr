# Trackr

A real-time family location tracking Android application that helps parents keep an eye on their children's locations for peace of mind.

## Features

- **Real-time Location Sharing** - See family members' locations on a map in real-time
- **Group-based Organization** - Create or join groups with passcodes to share locations with specific people
- **Background Location Tracking** - Optional foreground service continues tracking even when the app is minimized
- **User Authentication** - Simple email/password registration and login system
- **Auto-login** - Returning users are automatically logged in

## Architecture

### Project Structure

```
app/src/main/java/com/fn2101681010/Trackr/
├── MainActivity.java        # Main map view with group member markers
├── LoginActivity.java       # User login screen
├── RegisterActivity.java    # User registration screen
├── PeopleActivity.java      # Group management screen
├── ProfileActivity.java     # User profile and settings
├── NewGroupActivity.java    # Create/Join groups
├── EditGroupActivity.java   # Edit/Leave groups
├── LocationService.java     # Background location foreground service
├── Group.java               # Data model for groups
└── GroupAdapter.java        # RecyclerView adapter for groups
```

### Data Models

**User Document (Firestore):**
```json
{
  "username": "string",
  "email": "string",
  "password": "string",
  "latitude": "double",
  "longitude": "double"
}
```

**Group Document (Firestore):**
```json
{
  "groupName": "string",
  "passcode": "string",
  "members": ["email1", "email2"],
  "creator": "string"
}
```

## Technologies

### Android SDK
- **minSdk**: 24 (Android 7.0)
- **targetSdk**: 34 (Android 14)
- **Java 8** source compatibility

### Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| AndroidX AppCompat | 1.6.1 | Backward compatibility |
| Material Components | 1.12.0 | UI components following Material Design |
| ConstraintLayout | 2.1.4 | Flexible layout management |
| Firebase BOM | 33.0.0 | Firebase version management |
| Firebase Firestore | 25.0.0 | NoSQL cloud database |
| Firebase Analytics | (via BOM) | App usage analytics |
| Google Play Services Maps | 18.2.0 | Google Maps SDK |
| Google Play Services Location | 21.2.0 | Fused Location Provider |

### Build System
- **Gradle**: 8.13.0 with Kotlin DSL

## APIs & Services

### Firebase
- **Cloud Firestore** - Real-time NoSQL database for users and groups
- **Firebase Analytics** - Usage tracking and analytics

### Google Maps Platform
- **Google Maps SDK** - Interactive map display with markers
- **Fused Location Provider** - High-accuracy GPS location updates

### Real-time Updates
- Firestore snapshot listeners provide instant updates when:
  - Group members' locations change
  - Group membership changes
  - User profiles are updated

## Getting Started

### Prerequisites
- Android Studio (latest version recommended)
- JDK 8 or higher
- Android SDK with API 34
- Google Play Services installed on test device
- Firebase project with Firestore enabled

### Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd Trackr
   ```

2. **Firebase Configuration**
   - Create a Firebase project at [Firebase Console](https://console.firebase.google.com)
   - Enable Firestore Database
   - Download `google-services.json` from your Firebase project settings
   - Place it in the `app/` directory

3. **Google Maps API Key**
   - Get an API key from [Google Cloud Console](https://console.cloud.google.com)
   - Enable Maps SDK for Android
   - Add the API key to `app/src/main/AndroidManifest.xml`:
     ```xml
     <meta-data
         android:name="com.google.android.geo.API_KEY"
         android:value="YOUR_API_KEY_HERE" />
     ```

4. **Build and Run**
   ```bash
   ./gradlew assembleDebug
   ```
   Or open the project in Android Studio and click Run.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Install debug APK on connected device
./gradlew installDebug

# Clean build artifacts
./gradlew clean
```

## Permissions

The app requires the following permissions:

| Permission | Purpose |
|------------|---------|
| `INTERNET` | Network access for Firebase and Maps |
| `ACCESS_FINE_LOCATION` | GPS location access |
| `ACCESS_COARSE_LOCATION` | Network-based location |
| `ACCESS_BACKGROUND_LOCATION` | Location access when app is in background |
| `FOREGROUND_SERVICE` | Run background location service |
| `FOREGROUND_SERVICE_LOCATION` | Location-specific foreground service type |

## How It Works

### Location Tracking Flow

1. **Foreground Mode**
   - App requests location updates every 20 seconds
   - Uses `FusedLocationProviderClient` with high accuracy priority
   - Updates are stored in Firestore under the user's document

2. **Background Mode** (Optional)
   - `LocationService` runs as a foreground service with a persistent notification
   - Updates location every 30 seconds
   - Service restarts automatically if killed by the system

3. **Real-time Sync**
   - Firestore snapshot listeners notify all connected clients of location changes
   - Markers on the map update automatically

### Group Management

1. **Creating a Group**
   - Enter a group name and passcode
   - Creator becomes the group owner
   - Group appears in Firestore under `groups` collection

2. **Joining a Group**
   - Enter the exact group name and passcode
   - User's email is added to the group's members list
   - All group members can now see each other's locations

3. **Leaving a Group**
   - User is removed from the members list
   - If the creator leaves, ownership transfers to the next member
   - Groups are auto-deleted when the last member leaves

### Authentication Flow

1. **Registration**
   - User provides username, email, and password
   - Initial location is captured and stored
   - User document created in Firestore

2. **Login**
   - Email and password are validated against Firestore
   - Session stored in SharedPreferences
   - User is redirected to the main map view

3. **Auto-login**
   - On app launch, checks SharedPreferences for existing session
   - If session exists, skips login screen

## Configuration

### Gradle Properties

Key properties in `gradle.properties`:
```properties
android.useAndroidX=true
android.enableJetifier=true
```

### ProGuard

ProGuard rules are configured in `app/proguard-rules.pro` for release builds.

## Testing

### Unit Tests
Located in `app/src/test/java/`

### Instrumented Tests
Located in `app/src/androidTest/java/`

Run all tests:
```bash
./gradlew test connectedAndroidTest
```

## Security Notes

- Passwords are currently stored in plain text in Firestore
- API keys should be restricted in Google Cloud Console
- Consider implementing proper password hashing for production use
- Consider using Firebase Authentication instead of custom auth

## Requirements

- Android device running Android 7.0 (API 24) or higher
- Google Play Services must be installed
- Location services must be enabled
- Internet connection required

## License

This project is available for educational and personal use.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

---

Built with Android SDK, Firebase, and Google Maps Platform.
