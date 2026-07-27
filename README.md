# Family Time

Family Time is a proof-of-concept video conferencing application designed specifically for the Meta Portal TV, powered by the [Daily.co Video SDK](https://www.daily.co/). 

The goal of this project is to allow users to easily start a video meeting from their Portal TV and instantly invite a pre-determined group of family members to join. Invited guests can join the call directly from their mobile or desktop web browsers without needing to install any apps.

## Architecture

1. **Portal TV Client (Android App):** Built using Kotlin and Jetpack Compose. It integrates the Daily.co Android Client SDK and communicates with the backend via Retrofit.
2. **Serverless Backend (Google Apps Script):** Dynamically fetches contacts from a Google Sheet, communicates with the Daily REST API to create rooms, and emails the meeting links to guests.
3. **Guest Experience (Daily Prebuilt):** Guests join the call via Daily Prebuilt's mobile-optimized web interface (runs seamlessly in Safari/Chrome without an app).

---

## 1. Backend Setup (Google Apps Script)

To set up the backend, follow these steps to configure your Google Apps Script environment:

1. **Create the Google Sheet:**
   - Create a new Google Sheet.
   - Name the first tab `Contacts`.
   - In row 1, add headers: `Name` (Column A) and `Email` (Column B).
   - Fill in your family members' details starting on row 2.
   - Note the **Sheet ID** from the URL (e.g., `https://docs.google.com/spreadsheets/d/<SHEET_ID>/edit`).

2. **Set up the Apps Script:**
   - Go to [script.google.com](https://script.google.com/) and create a new project.
   - Copy the contents of the local `backend/Code.gs` file into the editor.
   
3. **Configure Script Properties (Secrets):**
   - In the Apps Script editor, click the **Project Settings** (gear icon) on the left sidebar.
   - Scroll down to **Script Properties** and add two properties:
     - `DAILY_API_KEY`: Your private API key from your Daily.co dashboard.
     - `SHEET_ID`: The ID of the Google Sheet you created in Step 1.

4. **Deploy as a Web App:**
   - Click **Deploy > New deployment** in the top right.
   - Select **Web app** as the type.
   - Set **Execute as** to "Me" and **Who has access** to "Anyone".
   - Deploy and copy the resulting **Web App URL**.

5. **Connect the Android App:**
   - Open `/app/src/main/java/com/familytime/Network.kt` in this repository.
   - Replace the `BASE_URL` string with the Web App URL you just generated.

*(Optional) Daily Health Check:* 
You can use the Apps Script "Triggers" menu (clock icon) to run the `dailyHealthCheck()` function once a day to ensure your Daily.co billing and API are functioning properly.

---

## 2. Portal TV Installation & Setup

You must build the project via Android Studio or Gradle and push it to the Portal TV over Wi-Fi ADB.

### Prerequisites
- Android Studio or standard Android SDK tools (JDK 17+).
- A Meta Portal TV connected to the same Wi-Fi network as your computer, with **Developer Mode / ADB enabled**.

### Installation Steps

1. **Connect to the Portal TV:**
   Find the Portal TV's IP address and connect via ADB:
   ```bash
   adb connect <PORTAL_IP_ADDRESS>:5555
   adb devices
   ```

2. **Build and Install:**
   From the root of this repository, run:
   ```bash
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

3. **Launch the App:**
   You can launch the app directly from your terminal:
   ```bash
   adb shell am start -n com.familytime/.MainActivity
   ```
   Or, you can use the Portal TV remote to navigate to the app drawer and open **"Family Time"**. (The app uses the `LEANBACK_LAUNCHER` intent and includes an `android:banner` to ensure it is visible in the UI).

### Headless Development
If you are developing without an HDMI monitor plugged into the Portal TV, you can use `scrcpy` to mirror the TV's interface directly to your desktop:
```bash
scrcpy
```
