# Tarkana Android

<div align="center">

<table>
  <tr>
    <td align="center" bgcolor="#16CBB2">
      <strong>Native Android client for Tarkana ranked logic challenges.</strong>
    </td>
  </tr>
</table>

<br>

<a href="https://github.com/KaisAbiyyi/tarkana-android/releases"><img alt="Download Tarkana Android Beta" src="https://img.shields.io/badge/DOWNLOAD%20BETA%20APK-FFD21E?style=for-the-badge&labelColor=17120D&color=FFD21E"></a>
<a href="https://github.com/KaisAbiyyi/tarkana-android/stargazers"><img alt="Star Tarkana Android" src="https://img.shields.io/badge/STAR%20THE%20MOBILE%20APP-16CBB2?style=for-the-badge&labelColor=17120D&color=16CBB2"></a>
<a href="https://github.com/KaisAbiyyi/tarkana"><img alt="Open Tarkana Web" src="https://img.shields.io/badge/OPEN%20WEB%20REPO-FF5C5C?style=for-the-badge&labelColor=17120D&color=FF5C5C"></a>

</div>

## What Is This?

Tarkana Android is the native Java Android app for Tarkana. It lets players sign in, start timed logic sessions, answer ranked challenges, resume unfinished sessions, review results, browse history, check leaderboards, and manage their profile from a phone.

The canonical web application, backend contract, database schema, challenge generation, scoring, and Supabase Edge Functions live in the web repository:

https://github.com/KaisAbiyyi/tarkana

## Beta Release

Current beta version:

```text
0.1.0-beta.1
```

APK releases are published through GitHub Releases:

https://github.com/KaisAbiyyi/tarkana-android/releases

## Tech Stack

- Java 11
- Android Gradle Plugin
- Android SDK 24+
- AppCompat
- Material Components
- ConstraintLayout
- XML layouts and drawable resources
- AndroidX Security Crypto
- SwipeRefreshLayout
- Facebook Shimmer
- Supabase Edge Functions over HTTPS

## Project Structure

```text
app/src/main/java/com/kaisabiyyistudio/tarkana_android/
  ApiClient.java           HTTP client for Supabase Edge Functions
  AuthSession.java         Local auth/session token handling
  LoginActivity.java       Login screen
  RegisterActivity.java    Registration screen
  SplashActivity.java      Initial launch screen
  MainActivity.java        Main app shell and bottom navigation
  ChallengeFragment.java   Challenge mode selection and active-session recovery
  SessionActivity.java     Active challenge gameplay
  DashboardFragment.java   Dashboard overview
  HistoryFragment.java     Session history
  LeaderboardFragment.java Leaderboard screen
  ProfileFragment.java     User profile screen

app/src/main/res/
  layout/                  Android XML layouts
  drawable/                Icons, cards, backgrounds, and UI shapes
  font/                    App typography
  values/                  Colors, dimensions, strings, styles, themes
```

## Backend Contract

This Android app does not own challenge generation, answer validation, scoring, rating, or session rules. Those stay server-side in the Tarkana web/backend repository so the APK does not duplicate business logic or expose answer rules.

Core Edge Functions used by the app:

```text
start-challenge
submit-answer
get-active-challenge
finish-challenge
abandon-challenge
get-dashboard
get-history
get-leaderboard
get-profile
update-profile
```

## Local Configuration

Create or update `local.properties` in the project root:

```properties
supabaseUrl=https://your-project.supabase.co
supabaseKey=your-anon-key
```

These values are injected into `BuildConfig` by `app/build.gradle.kts`.

Do not commit real local credentials. `local.properties` is ignored by Git.

## Build

Use the included Gradle wrapper:

```powershell
.\gradlew.bat assembleDebug
```

The debug APK is generated under:

```text
app/build/outputs/apk/debug/
```

## Test

Run local unit tests:

```powershell
.\gradlew.bat testDebugUnitTest
```

Build an installable beta APK:

```powershell
.\gradlew.bat assembleDebug
```

## Release Naming

Use aligned beta tags with the web repository:

```text
android-v0.1.0-beta.1
```

The uploaded APK asset should use this shape:

```text
tarkana-android-0.1.0-beta.1.apk
```

## Related Repositories

<table>
  <tr>
    <th>Repository</th>
    <th>Role</th>
    <th>Action</th>
  </tr>
  <tr>
    <td><a href="https://github.com/KaisAbiyyi/tarkana-android">tarkana-android</a></td>
    <td>Native Android app</td>
    <td><a href="https://github.com/KaisAbiyyi/tarkana-android/stargazers">Star the mobile app</a></td>
  </tr>
  <tr>
    <td><a href="https://github.com/KaisAbiyyi/tarkana">tarkana</a></td>
    <td>Web app, backend contract, Edge Functions</td>
    <td><a href="https://github.com/KaisAbiyyi/tarkana/stargazers">Star the web app</a></td>
  </tr>
</table>

## Development Notes

- Keep UI work in Activities, Fragments, XML layouts, adapters, and resource files.
- Keep challenge rules and scoring in the Tarkana web/backend repository.
- Use `ApiClient` for authenticated Edge Function requests.
- Use `AuthSession` for local token/session handling.
- Keep `.agents/` local-only; it is ignored and should not be pushed.
