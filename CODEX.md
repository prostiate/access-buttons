# CODEX Project Memory

## Project Summary
AccessButtons is a native Kotlin Android app that provides a premium floating media volume controller for `STREAM_MUSIC`.
Primary device profile: Redmi Note 8 Pro, Android 11, MIUI 12.5.

## Current Features
- Foreground `VolumeService` with high-priority ongoing notification.
- Floating glass-styled overlay with vertical actions:
  - Top: Volume up (`+`)
  - Bottom: Volume down (`-`)
- Draggable overlay with smooth snap-to-left/right-edge animation.
- Drag can start from the whole floating control surface (not only drag-handle strip).
- Tactile feedback on button press via `HapticFeedbackConstants`.
- Mode-ready architecture in service:
  - `SYSTEM_UI` mode (shows system volume slider)
  - `CUSTOM` mode placeholder (system slider suppressed)
- Permission manager UX in `MainActivity`:
  - Overlay permission (`Display over other apps`)
  - Xiaomi Autostart shortcut
  - Battery optimization whitelist flow

## MIUI Troubleshooting Notes
### 1) Overlay not visible
- Confirm overlay permission is enabled for AccessButtons.
- Re-open app and start service again.
- If app crashes at service start, verify overlay inflation uses a themed context in service (`ContextThemeWrapper` with app theme).

### 2) Service stops unexpectedly
- In MIUI Security app, enable **Autostart**.
- Set battery saver to **No restrictions** for AccessButtons.
- Verify foreground notification remains visible.

### 3) Volume buttons respond slowly
- Check if device is in aggressive power-saving mode.
- Re-launch service from app UI.
- If drag feels too sensitive or too stiff, tune `touchSlop` threshold in `VolumeService` touch listener.

### 4) Notification missing on Android 13+
- Grant notification permission (`POST_NOTIFICATIONS`).

## Custom Slider Roadmap
### Phase 1 (Current)
- Keep system slider as default for reliability (`FLAG_SHOW_UI`).

### Phase 2
- Add custom in-overlay slider UI component.
- Persist selected mode and slider position with `DataStore`.
- Add service command actions for mode switch from notification.

### Phase 3
- Add expanded panel with mute, profile presets, and theme options.
- Introduce animation/haptic tuning profile per device class.

### Phase 4
- Add instrumentation tests for drag physics, mode switching, and service recovery on process death.

## Build/Run Commands
- Install debug build (Windows): `./gradlew.bat installDebug`
- Install debug build (Unix): `./gradlew installDebug`
- Compile only: `./gradlew.bat assembleDebug`

## Change Log (2026-03-09)
- Fixed startup crash in `VolumeService` overlay inflation:
  - Root cause: Material component inflation from non-themed service context.
  - Fix: use `ContextThemeWrapper(this, R.style.Theme_AccessButtons)` during `floating_layout` inflation.
- Updated overlay UX:
  - Vertical button layout with explicit `+` and `-` icons.
  - Full-surface drag handling with tap-vs-drag gesture separation.
- Repository hygiene:
  - Hardened root `.gitignore` for Android/Gradle/IDE artifacts and secret file types (`.jks`, `.keystore`, `.env*`, `secrets.properties`).
- Public repository readiness:
  - Added `README.md` with setup, permissions, verification, troubleshooting, and contribution guidance.
  - Added `LICENSE` (MIT).

## Verification Workflow
- Recommended local verification order:
  - `./gradlew lint`
  - `./gradlew test`
  - `./gradlew clean assembleDebug`
- Optional style gate (if ktlint plugin is added to Gradle):
  - `./gradlew ktlintCheck`

## Web Dev Command Equivalents
| Android/Kotlin | TS/JS Equivalent | Command |
| --- | --- | --- |
| Android Lint | ESLint | `./gradlew lint` |
| ktlint | Prettier | `./gradlew ktlintCheck` |
| Unit tests | Jest/Vitest | `./gradlew test` |
| Clean rebuild | `npm run build` | `./gradlew clean assembleDebug` |

## Commit Checklist
- Lint passes: `./gradlew.bat lint`
- Unit tests pass: `./gradlew.bat test`
- App builds from clean state: `./gradlew.bat clean assembleDebug`
- Run instrumented tests when behavior/UI changes: `./gradlew.bat connectedDebugAndroidTest`
- Keep project memory current: update `CODEX.md` whenever architecture, behavior, or troubleshooting guidance changes.
- Inspect pending changes for secrets before commit:
  - Keystores/certs (`*.jks`, `*.keystore`, `*.p12`, `*.pem`, `*.key`)
  - Env/secrets (`.env*`, `secrets.properties`)
  - Local machine files (`local.properties`)
