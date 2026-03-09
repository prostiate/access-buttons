# AGENTS Operating Manual

## Build and Install
- Windows: `./gradlew.bat installDebug`
- macOS/Linux: `./gradlew installDebug`
- Quick compile check: `./gradlew.bat assembleDebug`

## Verification Commands (Web Dev Analogy)
| Tool | Analogous to (TS/JS) | Command | What it does |
| --- | --- | --- | --- |
| Lint | ESLint | `./gradlew.bat lint` | Checks for Android-specific bugs, performance issues, and accessibility gaps. |
| ktlint | Prettier | `./gradlew.bat ktlintCheck` | If configured, enforces Kotlin style guide rules (indentation, spacing, imports). |
| Unit Tests | Jest/Vitest | `./gradlew.bat test` | Runs JVM/unit tests for logic behavior. |
| Instrumented Tests | Playwright/E2E | `./gradlew.bat connectedDebugAndroidTest` | Runs device/emulator integration tests. |
| Clean Build | `npm run build` | `./gradlew.bat clean assembleDebug` | Wipes build artifacts and rebuilds from scratch to catch cache-related issues. |

## Definition of Done (Before Commit)
- Lint passes: `./gradlew.bat lint`
- Unit tests pass: `./gradlew.bat test`
- App builds from clean state: `./gradlew.bat clean assembleDebug`
- Run instrumented tests when behavior/UI changes: `./gradlew.bat connectedDebugAndroidTest`
- `CODEX.md` is updated when architecture, behavior, or troubleshooting guidance changes.

## Security Rules
- Never hardcode credentials, API keys, or tokens in source files.
- Never commit signing materials (`*.jks`, `*.keystore`, `*.pem`, `*.key`, `*.p12`).
- Keep secrets in local-only config and ensure ignored files stay in `.gitignore`.
- Avoid logging sensitive user/device identifiers in production builds.

## Technical Standards
- Language: Kotlin 1.9+ (project currently uses Kotlin 2.x)
- Android baseline: SDK 30+ behavior compatibility (device target: Android 11 / MIUI 12.5)
- Min SDK: 24
- Foreground service is mandatory for overlay persistence on Xiaomi/MIUI devices.
- All new background work must use Kotlin Coroutines with structured concurrency.
- Keep service logic resilient: no blocking calls on main thread.

## Architecture Rules
- `VolumeService` is the single source of truth for floating overlay state.
- `MainActivity` is responsible for user permission orchestration and service lifecycle triggers.
- Volume mode abstraction is required:
  - `SYSTEM_UI` mode: use `AudioManager.FLAG_SHOW_UI`
  - `CUSTOM` mode: suppress system slider for future custom implementation
- UI resources must remain lightweight and support low-RAM devices.

## MIUI Production Rules
- Always guide users through:
  - Display over other apps
  - Autostart (MIUI Security app)
  - Battery optimization whitelist
- Use high-priority ongoing notification for the foreground service.
- Treat process death as normal; service should restart cleanly.

## Foreground Service Policy
- Foreground service must be used only for user-noticeable ongoing behavior (floating controller active).
- Ongoing notification must remain visible while overlay is running.
- Service stop action should be available and respected immediately.
- Start/stop paths must remain resilient across app restarts and process recreation.

## QA Checklist
- Dragging works and snaps to nearest edge.
- Haptic feedback triggers on every `+/-` press.
- Overlay survives app backgrounding.
- Media volume (`STREAM_MUSIC`) changes reliably.
- Notification action can stop service.
