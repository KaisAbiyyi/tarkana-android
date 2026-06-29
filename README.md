# Tarkana Android

Tarkana Android is the native Android client for the Tarkana ranked logic challenge app. The app lets users sign in, start logic challenge sessions, answer questions, review progress, view history, check leaderboard standings, and manage their profile from an Android device.

This repository contains only the Android application. The canonical web application, backend contract, database schema, and Supabase Edge Functions are maintained in the Tarkana website repository:

https://github.com/KaisAbiyyi/tarkana

## Tech Stack

- Java 11
- Android Gradle Plugin
- Android SDK 24+
- AppCompat
- Material Components
- ConstraintLayout
- RecyclerView-style adapters
- XML layouts and drawable resources
- AndroidX Security Crypto
- SwipeRefreshLayout
- Facebook Shimmer
- Supabase Edge Functions over HTTPS

## Project Structure

```text
app/src/main/java/com/kaisabiyyistudio/tarkana_android/
  ApiClient.java          HTTP client for Supabase Edge Functions
  AuthSession.java        Local auth/session token handling
  LoginActivity.java      Login screen
  RegisterActivity.java   Registration screen
  SplashActivity.java     Initial launch screen
  MainActivity.java       Main app shell and bottom navigation
  ChallengeFragment.java  Challenge mode selection
  SessionActivity.java    Active challenge session gameplay
  DashboardFragment.java  Dashboard overview
  HistoryFragment.java    Session history
  LeaderboardFragment.java Leaderboard screen
  ProfileFragment.java    User profile screen

app/src/main/res/
  layout/                 Android XML layouts
  drawable/               Icons, cards, backgrounds, and UI shapes
  font/                   App typography
  values/                 Colors, dimensions, strings, styles, themes
```

## Backend Relationship

The Android app calls deployed Supabase Edge Functions using HTTPS. The Edge Functions are not owned by this Android repository. They are kept in the website repository so challenge generation, answer validation, scoring, rating, and session rules stay canonical in one place.

Reference backend repository:

https://github.com/KaisAbiyyi/tarkana

The Android project should not duplicate challenge generation, answer validation, scoring, or rating logic locally. Keeping that logic server-side helps avoid stale mobile logic and reduces the chance of exposing answer rules from the APK.

## Local Configuration

Create or update `local.properties` in the project root:

```properties
supabaseUrl=https://your-project.supabase.co
supabaseKey=your-anon-key
```

These values are injected into `BuildConfig` by `app/build.gradle.kts`:

```kotlin
buildConfigField("String", "SUPABASE_URL", ...)
buildConfigField("String", "SUPABASE_KEY", ...)
```

Do not commit real local credentials. `local.properties` is ignored by Git.

## Build

Use the included Gradle wrapper:

```powershell
.\gradlew.bat assembleDebug
```

The debug APK will be generated under:

```text
app/build/outputs/apk/debug/
```

## Test

Run local unit tests:

```powershell
.\gradlew.bat testDebugUnitTest
```

Run an Android debug build:

```powershell
.\gradlew.bat assembleDebug
```

## Development Notes

- Keep UI code in Android Activities, Fragments, XML layouts, adapters, and resource files.
- Keep server rules in the Tarkana website/backend repository.
- Use `ApiClient` for authenticated Edge Function requests.
- Use `AuthSession` for local token/session handling.
- Keep `.agents/` local-only; it is ignored and should not be pushed.

## Related Repository

- Tarkana website and backend: https://github.com/KaisAbiyyi/tarkana
