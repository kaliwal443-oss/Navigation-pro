# Navigation Pro - Tactical Navigation App

A production-ready native Android tactical navigation application built with Kotlin, Jetpack Compose, and osmdroid.

## Features

### Core Navigation
- **Real-time GPS tracking** with high-accuracy location updates
- **Custom compass integration** using magnetometer and accelerometer fusion
- **Multiple map tile sources**: Tactical Dark, Satellite, Topographic, Standard
- **Offline tile caching** for use in areas with limited connectivity

### Indian Grid System (IGS)
- **WGS84 to Indian Grid conversion** using Lambert Conformal Conic projection
- **Everest 1830 ellipsoid** with Indian 1956 datum
- **Auto-zone detection** for all 8 Indian Grid zones (Zone 0, I, IIA, IIB, IIIA, IIIB, IVA, IVB)
- **Dynamic grid overlay** on the map showing easting/northing lines

### Waypoint Management
- **Drop markers** at current location or map center
- **Manual coordinate entry** (WGS84 or Indian Grid)
- **Color-coded waypoints** (Red, Green, Blue, Yellow, Purple, Orange)
- **Room Database** for persistent storage
- **GPX Import/Export** for interoperability with other GPS applications

### Heads-Up Display (HUD)
- **Top-left panel**: WGS84 and Indian Grid coordinates with current zone
- **Top-right panel**: GPS status, accuracy, altitude, satellite count
- **Bottom-left panel**: Rotating compass rose with cardinal directions
- **Bottom-right FAB**: Opens the Control Deck

### Control Deck (Bottom Sheet)
- **Tools Tab**: Fullscreen compass data, satellite info, Sun/Moon almanac
- **Plan Tab**: Waypoint manager with add/delete/export functionality
- **Map Tab**: Layer selector and grid overlay toggle

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin 1.9.20 |
| UI Framework | Jetpack Compose (Material3) |
| Architecture | MVVM with Hilt DI |
| Map Engine | osmdroid-android 6.1.18 |
| Database | Room 2.6.1 |
| GPS | Google Play Services Location |
| Sensors | Android SensorManager |
| Async | Kotlin Coroutines |

## Project Structure

```
NavigationPro/
├── app/
│   ├── src/main/java/com/navigationpro/
│   │   ├── data/
│   │   │   ├── database/        # Room database
│   │   │   ├── dao/             # Data Access Objects
│   │   │   └── model/           # Entity classes
│   │   ├── domain/
│   │   │   ├── repository/      # Repository pattern
│   │   │   ├── service/         # GPX, Compass, Location services
│   │   │   └── util/            # ProjectionMath
│   │   ├── presentation/
│   │   │   ├── components/      # UI components
│   │   │   ├── screens/         # MapScreen, MainActivity
│   │   │   ├── state/           # UI state classes
│   │   │   └── viewmodel/       # MapViewModel
│   │   ├── di/                  # Hilt modules
│   │   └── NavigationProApplication.kt
│   └── src/main/res/            # Resources
├── build.gradle.kts
└── settings.gradle.kts
```

## Key Files

### ProjectionMath.kt
Core mathematics for Indian Grid System conversion:
- Everest 1830 ellipsoid parameters
- Lambert Conformal Conic projection
- Auto-zone detection
- Forward and inverse coordinate transformations

### CompassSensorManager.kt
Sensor fusion for smooth compass readings:
- Rotation vector sensor (preferred) or accelerometer + magnetometer
- Low-pass filter for smoothing
- Screen rotation adjustment
- Cardinal direction calculation

### IndianGridOverlay.kt
Custom osmdroid overlay for grid visualization:
- Dynamic grid line rendering based on zoom level
- Zone boundary display
- Coordinate labels

### MapViewModel.kt
Central state management:
- Location updates via FusedLocationProvider
- Compass data collection
- Waypoint CRUD operations
- Sun/Moon position calculations

## Build Instructions

1. Clone the repository
2. Open in Android Studio (Giraffe or later)
3. Sync Gradle files
4. Build and run on an Android device (API 26+)

```bash
# Or build from command line
./gradlew assembleDebug
```

## Permissions Required

- `ACCESS_FINE_LOCATION` - Precise GPS positioning
- `ACCESS_COARSE_LOCATION` - Approximate location
- `INTERNET` - Map tile downloads
- `WRITE_EXTERNAL_STORAGE` - GPX export (Android 9 and below)

## Design System

### Tactical Dark Mode
- **Background**: Deep Slate (#0f172a)
- **Primary Text**: White / Slate-100
- **Accent Green** (#10b981): GPS fix, good status
- **Accent Amber** (#f59e0b): Headings, warnings
- **Fonts**: JetBrains Mono for coordinates, Inter for labels

## Map Tile Sources

| Layer | URL |
|-------|-----|
| Tactical Dark | `https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png` |
| Satellite | `https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}` |
| Topographic | `https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png` |
| Standard | `https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png` |

## Indian Grid Zones

| Zone | Origin (Lat, Lng) | Coverage |
|------|-------------------|----------|
| Zone 0 | 39.5°N, 68.0°E | Northern Pakistan/Afghanistan |
| Zone I | 32.5°N, 68.0°E | Northern India/Pakistan |
| Zone IIA | 26.0°N, 74.0°E | Western India |
| Zone IIB | 26.0°N, 84.0°E | Eastern India |
| Zone IIIA | 19.0°N, 80.0°E | Central India (West) |
| Zone IIIB | 19.0°N, 84.0°E | Central India (East) |
| Zone IVA | 12.0°N, 80.0°E | Southern India (West) |
| Zone IVB | 12.0°N, 84.0°E | Southern India (East) |

## Dependencies

Key dependencies (see `app/build.gradle.kts` for complete list):

```kotlin
// osmdroid
implementation("org.osmdroid:osmdroid-android:6.1.18")

// Room
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")

// Hilt
implementation("com.google.dagger:hilt-android:2.48")
kapt("com.google.dagger:hilt-compiler:2.48")

// GPX
implementation("io.jenetics:jpx:3.1.0")

// Sun/Moon calculations
implementation("org.shredzone.commons:commons-suncalc:3.8")
```

## License

MIT License - See LICENSE file for details

## Author

Senior Android Engineer - Tactical Navigation Systems