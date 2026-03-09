# AccessButtons

Premium floating media volume controller for Android, built in Kotlin.

AccessButtons provides a glass-style overlay with `+` / `-` controls for `STREAM_MUSIC`, designed for MIUI/Xiaomi reliability using a foreground service architecture.

## Features
- Floating glassmorphic overlay for quick volume control
- Vertical controls:
  - Top: `+` volume
  - Bottom: `-` volume
- Smooth drag + snap-to-edge behavior
- Haptic click feedback on every press
- Foreground service with persistent notification
- Permission manager flow for:
  - Display over other apps
  - MIUI Autostart (Xiaomi/Redmi)
  - Battery optimization whitelist
- Mode-ready service architecture:
  - `SYSTEM_UI` (shows Android system volume slider)
  - `CUSTOM` (reserved for future in-app slider)

## Tech Stack
- Kotlin (1.9+ compatible; currently Kotlin 2.x in project)
- Android SDK (targeting modern Android; tested use case includes Android 11 MIUI 12.5)
- Jetpack Compose (app screen)
- Android Foreground Service + WindowManager overlay
- Kotlin Coroutines

## Project Structure
- `app/src/main/java/com/example/accessbuttons/MainActivity.kt`: permission manager and service control UI
- `app/src/main/java/com/example/accessbuttons/VolumeService.kt`: overlay engine, drag/snap logic, haptics, volume actions
- `app/src/main/res/layout/floating_layout.xml`: floating UI definition
- `AGENTS.md`: engineering operating manual
- `CODEX.md`: project memory, troubleshooting, roadmap

## Requirements
- Android Studio (latest stable recommended)
- Android SDK / Build tools installed
- A real device for overlay behavior validation (recommended)

## Getting Started
1. Clone the repository.
2. Open in Android Studio.
3. Build and install:
   - Windows: `./gradlew.bat installDebug`
   - macOS/Linux: `./gradlew installDebug`
4. Launch app and grant required permissions.
5. Start the floating controller from the app screen.

## Required Runtime Permissions
- `Display over other apps` (overlay)
- Notification permission (Android 13+)
- Battery optimization whitelist (recommended for MIUI)
- Xiaomi/Redmi Autostart (recommended for persistence)

## Verification Commands
- `./gradlew.bat lint`
- `./gradlew.bat test`
- `./gradlew.bat clean assembleDebug`
- `./gradlew.bat connectedDebugAndroidTest` (for UI/integration behavior changes)

## Troubleshooting (MIUI)
- Overlay not showing:
  - Confirm overlay permission is enabled
  - Restart service from app
- Service killed in background:
  - Enable MIUI Autostart
  - Set battery mode to No restrictions
  - Ensure persistent notification remains visible

## Roadmap
- Add custom in-overlay volume slider mode
- Persist mode and UI settings with DataStore
- Add expanded panel (mute/presets/theme)
- Add stronger instrumentation test coverage

## Contributing
Contributions are welcome.

Before opening a PR:
1. Run verification commands.
2. Ensure no secrets or local machine files are included.
3. Update docs (`README.md`, `AGENTS.md`, `CODEX.md`) when behavior or architecture changes.

## Security
Do not commit credentials, signing keys, or environment secrets.

If you find a security issue, open a private report to the maintainer before public disclosure.

## License
This project is licensed under the MIT License. See [LICENSE](LICENSE).
