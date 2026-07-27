# Family Time - Agent Log

## Overview
This document serves as a summary of the development process and decisions made while building the "Family Time" Portal TV proof-of-concept application.

## Development Journey

### 1. Initial Setup & Requirements
- Identified the goal: A headless video conferencing app for the Meta Portal TV to easily invite family members to a call.
- Selected **Daily.co** as the WebRTC provider due to its Android Client SDK compatibility (minSdk 21, no GMS requirement) and ease of prebuilt web-links for guests.
- Initialized the Android project (`com.familytime`) by refactoring the provided `portal-samples` boilerplate to ensure Portal TV manifest requirements (`LEANBACK_LAUNCHER`, `android:banner`) were intact.

### 2. Backend Infrastructure (Apps Script)
- Set up a Google Sheet to store dynamic contact lists (Names and Emails).
- Created a Google Apps Script (`backend/Code.gs`) to serve as a serverless backend.
- Implemented `doGet` to fetch contacts for the Android app.
- Implemented `doPost` to securely interact with the Daily.co REST API (`/v1/rooms`) to generate meeting links using a hidden API key (`PropertiesService`).
- Used Apps Script's `MailApp` to instantly blast invitations to selected family members.
- Added a `dailyHealthCheck()` function that can be scheduled to monitor Daily.co API uptime and billing status.

### 3. Android Client Development
- Integrated the Daily.co Client SDK (`0.38.1`) and Retrofit for networking.
- Added necessary permissions (`INTERNET`, `CAMERA`, `RECORD_AUDIO`, `MODIFY_AUDIO_SETTINGS`).
- Built a 10-foot UI using Jetpack Compose featuring a `LazyColumn` of contacts with checkboxes, fully navigable via the Portal TV's D-pad remote.
- Implemented the Daily `CallClient` and `CallClientListener` to track participant states.
- Utilized Kotlin type inference with the `Participant` object to directly bind video tracks to the `DailyVideoView`, solving WebRTC track import namespace issues.
- Built a split-screen video rendering layout to display the local camera and remote family members side-by-side.

### 4. Developer Experience (DX)
- Established a headless ADB testing workflow using `scrcpy`.
- Integrated `scrcpy` directly into the Android Studio "Before Launch" run configuration for 1-click deployments.
- Synchronized the finalized project to a private GitHub repository (`cjmcqueen/family-time`).

### 5. Final Repository Cleanup
- Removed unused sample boilerplate code from the original Meta repository.
- Stripped original copyright headers and replaced with a personal MIT license, transitioning the repository fully to a personal project while maintaining an acknowledgment to the original Meta boilerplate in the README.

## Conclusion
The proof-of-concept is complete. The Portal TV successfully boots the app, fetches live contacts from Google Sheets, creates a room, emails the guests, and successfully renders the WebRTC video feeds using the native Daily Client SDK.
