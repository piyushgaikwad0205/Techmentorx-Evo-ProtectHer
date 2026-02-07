# ProtectHer - Smart Women Safety SOS App

A comprehensive Android safety application designed to protect women through intelligent emergency response features, real-time location tracking, and smart defense mechanisms.

## 🌟 Features

### 🚨 Emergency SOS
- **Shake-to-SOS**: Trigger emergency alerts by shaking your device
- **Emergency Contacts**: Quickly alert trusted contacts via SMS and calls
- **Live Location Sharing**: Share real-time GPS coordinates during emergencies
- **Automated SOS Calls**: Direct emergency calling functionality

### 🔐 Anti-Tamper Protection
- **Bluetooth Device Monitoring**: Detects unauthorized removal of connected wearables
- **Biometric Authentication**: Fingerprint verification for authorized device disconnection
- **5-Second Countdown Alert**: Visual and haptic warnings before auto-triggering SOS
- **Background Monitoring Service**: Continuous tamper detection via foreground service

[Learn more about Anti-Tamper Mechanism](ANTI_TAMPER_MECHANISM.md)

### 📍 Location & Tracking
- **Real-time GPS Tracking**: Continuous location monitoring during emergencies
- **Route Tracking**: Record and share your route for safety
- **Nearby Help**: Connect with nearby users who can assist in emergencies
- **Safety Analysis**: Analyze routes and areas for safety insights

### 👨‍👩‍👧 Parental Controls
- **Parent Dashboard**: Monitor child's safety status and emergency alerts
- **QR Code Pairing**: Secure parent-child account linking
- **Emergency Notifications**: Real-time alerts to parents during SOS events
- **Location Monitoring**: Track child's location and safety status

### 🛡️ Smart Defense
- **Medical Information Storage**: Store critical medical details for emergencies
- **Safety Tips & Guides**: Educational content on personal safety
- **Camera Integration**: Quick photo/video capture during emergencies
- **Audio Recording**: Record audio evidence during dangerous situations

### 📱 Additional Features
- **User Authentication**: Secure Firebase-based login and signup
- **Profile Management**: Manage personal information and emergency contacts
- **History Tracking**: View past emergency events and SOS triggers
- **Settings Hub**: Customize app behavior and preferences
- **Offline Support**: Core features work without internet connectivity

## 🛠️ Technology Stack

### Core Technologies
- **Platform**: Android (Java)
- **Min SDK**: 27 (Android 8.1)
- **Target SDK**: 35 (Android 15)
- **Build System**: Gradle

### Key Dependencies
- **Firebase Suite**
  - Firebase Authentication
  - Cloud Firestore
  - Realtime Database
  - Firebase Storage
  - Firebase Analytics
  
- **Google Services**
  - Google Maps API
  - Google Play Services Location
  - Google Sign-In

- **Sensors & Detection**
  - Shake Detector
  - Sensey (Motion & Gesture Detection)
  
- **Camera & Media**
  - CameraX for photo/video capture
  - Glide for image loading
  
- **Security**
  - AndroidX Biometric API
  
- **Mapping**
  - MapLibre GL (9.6.0)
  - OpenStreetMap (OSMDroid)
  
- **Networking**
  - Retrofit 2
  - Gson Converter
  
- **QR Code**
  - ZXing Android Embedded

## 📋 Prerequisites

- Android Studio Arctic Fox or later
- JDK 17
- Android SDK 35
- Firebase account with project setup
- Google Maps API key

## 🚀 Getting Started

### 1. Clone the Repository
```bash
git clone https://github.com/yourusername/ProtectHer.git
cd ProtectHer
```

### 2. Firebase Setup
1. Create a new Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Add an Android app to your Firebase project
3. Download `google-services.json`
4. Place it in `app/` directory
5. Enable the following in Firebase Console:
   - Authentication (Email/Password & Google Sign-In)
   - Cloud Firestore
   - Realtime Database
   - Firebase Storage

### 3. Google Maps API Setup
1. Get a Google Maps API key from [Google Cloud Console](https://console.cloud.google.com/)
2. Create or update `app/src/main/res/values/strings.xml`:
```xml
<resources>
    <string name="app_name">ProtectHer</string>
    <string name="google_api_key">YOUR_GOOGLE_MAPS_API_KEY</string>
</resources>
```

### 4. Build Configuration
1. Open the project in Android Studio
2. Sync Gradle files
3. Update `local.properties` with your SDK path if needed

### 5. Build & Run
```bash
# Debug build
./gradlew assembleDebug

# Or use the build_debug.bat script on Windows
build_debug.bat

# Install on connected device
./gradlew installDebug
```

## 📁 Project Structure

```
ProtectHer/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/sos/
│   │   │   │   ├── WelcomeActivity.java       # App entry point
│   │   │   │   ├── LoginActivity.java         # User authentication
│   │   │   │   ├── SignupActivity.java        # User registration
│   │   │   │   ├── MainActivity.java          # Main dashboard
│   │   │   │   ├── HomeActivity.java          # Home screen
│   │   │   │   ├── SosCall.java               # Emergency SOS handler
│   │   │   │   ├── TamperAlertActivity.java   # Anti-tamper alert UI
│   │   │   │   ├── TamperMonitorService.java  # Bluetooth monitoring
│   │   │   │   ├── ServiceMine.java           # Location tracking service
│   │   │   │   ├── DeviceConnectActivity.java # Wearable pairing
│   │   │   │   ├── ParentDashboardActivity.java
│   │   │   │   ├── NearbyHelpActivity.java
│   │   │   │   ├── RouteActivity.java
│   │   │   │   ├── CameraActivity.java
│   │   │   │   └── ...
│   │   │   ├── res/                           # Resources
│   │   │   └── AndroidManifest.xml
│   │   ├── androidTest/                       # Instrumentation tests
│   │   └── test/                              # Unit tests
│   ├── build.gradle                           # App-level build config
│   ├── google-services.json                   # Firebase config
│   └── release-key.jks                        # Release signing key
├── gradle/                                     # Gradle wrapper
├── build.gradle                                # Project-level build config
├── settings.gradle
├── ANTI_TAMPER_MECHANISM.md                   # Anti-tamper documentation
└── README.md                                   # This file
```

## 🔑 Required Permissions

The app requires the following permissions:

- **Location**: For real-time tracking and route recording
- **Contacts**: To select emergency contacts
- **SMS**: To send emergency alerts
- **Phone**: To make emergency calls
- **Camera**: For photo/video evidence capture
- **Microphone**: For audio recording
- **Bluetooth**: For wearable device monitoring
- **Biometric**: For fingerprint authentication
- **Vibration**: For haptic feedback
- **Internet**: For Firebase and mapping services

## 🏗️ Key Features Implementation

### Emergency SOS Flow
1. User triggers SOS (shake, button, or auto-trigger)
2. App collects current GPS location
3. Sends SMS to all emergency contacts with location link
4. Initiates emergency calls
5. Activates location tracking service
6. Notifies parent accounts (if linked)

### Anti-Tamper Protection Flow
1. User pairs a Bluetooth wearable device
2. `TamperMonitorService` starts monitoring
3. On unauthorized disconnection:
   - `TamperAlertActivity` launches immediately
   - 5-second countdown begins
   - Phone vibrates continuously
   - User can authenticate or cancel
4. If countdown expires → SOS triggered automatically

### Parent-Child Linking
1. Parent account generates QR code
2. Child scans QR code with in-app scanner
3. Accounts linked in Firebase Realtime Database
4. Parent receives all emergency notifications

## 🧪 Testing

### Prerequisites
- Android device or emulator (API 27+)
- Physical device recommended for sensor testing

### Running Tests
```bash
# Unit tests
./gradlew test

# Instrumentation tests
./gradlew connectedAndroidTest
```

### Manual Testing Checklist
- [ ] User registration and login
- [ ] Emergency contact addition
- [ ] Shake-to-SOS trigger
- [ ] Location sharing accuracy
- [ ] Bluetooth device pairing
- [ ] Anti-tamper alert flow
- [ ] Parent-child QR linking
- [ ] Camera capture functionality
- [ ] Offline mode operation

## 🔒 Security & Privacy

- All biometric authentication occurs locally on-device
- Location data encrypted in transit
- Firebase Security Rules protect user data
- No biometric data transmitted or stored remotely
- Emergency contacts stored securely in Firestore
- Parent-child links use secure Firebase authentication

## 🐛 Troubleshooting

### Common Issues

**Shake detection not working**
- Ensure accelerometer permission granted
- Test on physical device (emulators may not support)
- Check sensitivity settings

**Location not accurate**
- Enable high-accuracy GPS mode
- Check location permissions granted
- Ensure GPS is enabled on device

**Firebase connection errors**
- Verify `google-services.json` is properly placed
- Check package name matches Firebase configuration
- Ensure Firebase services are enabled

**Anti-tamper not triggering**
- Verify Bluetooth permissions granted
- Check wearable is properly paired
- Ensure `TamperMonitorService` is running

## 📱 Build & Release

### Debug Build
```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Release Build
```bash
./gradlew assembleRelease
# Requires signing key configured in build.gradle
# Output: app/build/outputs/apk/release/app-release.apk
```

### Signing Configuration
The project includes `release-key.jks` for signing. Update `build.gradle` with:
```gradle
signingConfigs {
    release {
        storeFile file('release-key.jks')
        storePassword 'your-password'
        keyAlias 'your-alias'
        keyPassword 'your-password'
    }
}
```

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- Firebase for backend infrastructure
- Google Maps Platform for mapping services
- MapLibre for open-source mapping
- Community contributors and testers

## 📞 Support

For issues and questions:
- Open an issue on GitHub
- Contact: [your-email@example.com]

## 🗺️ Roadmap

- [ ] iOS version development
- [ ] Multi-language support
- [ ] Geofencing for safe zones
- [ ] AI-based threat detection
- [ ] Integration with police emergency systems
- [ ] Smartwatch companion app
- [ ] Voice-activated SOS
- [ ] Community safety reports

---

**Made with ❤️ for women's safety**
