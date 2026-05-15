# StepAside — Phase 1 Final (sync fixes)

This zip is the **complete, ready-to-build state** of your project with
Phase 1 changes applied AND the sync issues fixed.

## What was broken before this drop

| # | Problem | What I changed |
|---|---------|----------------|
| 1 | `gradle-wrapper.properties` requested **Gradle 9.3.1**, which is incompatible with AGP 8.7.3 | Downgraded to **Gradle 8.9** (the version AGP 8.7.3 wants) |
| 2 | `gradle/libs.versions.toml` declared **AGP 9.1.0** and **Kotlin 2.2.10** (not used anywhere but confusing AS) | Deleted the file entirely — your `build.gradle.kts` uses hardcoded versions, no catalog needed |
| 3 | `gradle/gradle-daemon-jvm.properties` pinned a JDK toolchain via foojay.io | Deleted — Gradle 8.9 doesn't need this and it causes problems on Windows |
| 4 | `keystore.properties` had placeholder `storePassword=your-store-password`, would fail any release build | Deleted — we'll re-add this in Phase 4 when you make a real keystore |
| 5 | `local.properties` had `SUPABASE_URL=https://your-project-ref.supabase.co` | Replaced with real URL + `REPLACE_WITH_YOUR_REAL_ANON_KEY` marker for the key |
| 6 | `themes.xml` parent was `Theme.Material3.DayNight.NoActionBar` | Switched to `android:Theme.Material.NoActionBar` (system theme, no AppCompat needed) — added `windowBackground` to prevent white flash on launch |

## Migration steps

### 1. Make sure you're on `phase1-attempt2` (or any branch other than `pre-phase1`)

```bash
cd C:/Users/muham/AndroidStudioProjects/StepAside
git status                  # confirm clean
git checkout -b phase1-fix  # new branch off whatever you're on
```

### 2. Delete the entire current project contents EXCEPT the .git folder

In a terminal:

```bash
cd C:/Users/muham/AndroidStudioProjects/StepAside
# Windows PowerShell:
Get-ChildItem -Force | Where-Object { $_.Name -ne '.git' } | Remove-Item -Recurse -Force

# OR Windows CMD:
# (close Android Studio first, then in cmd:)
# for /d %i in (*) do if /i not "%i"==".git" rd /s /q "%i"
# for %i in (*) do del /f /q "%i"
```

This keeps your git history intact but clears everything else.

### 3. Extract this zip directly into the project folder

Open `stepaside-phase1-final.zip`. Inside there's one folder, `StepAside/`.
Copy the **contents** of `StepAside/` into `C:/Users/muham/AndroidStudioProjects/StepAside/`.

You should end up with `C:/Users/muham/AndroidStudioProjects/StepAside/app/`,
`build.gradle.kts`, `gradlew`, etc. directly in the project root — not nested
inside another `StepAside/` folder.

### 4. Fix `local.properties` with your real Supabase key

Open `local.properties`. Replace `REPLACE_WITH_YOUR_REAL_ANON_KEY` with your
actual Supabase publishable / anon key from:
https://supabase.com/dashboard/project/mfvlupwxzfugirppspow/settings/api

The `sdk.dir` line is already set for your machine.

### 5. Open the project in Android Studio

1. **File → Open** → navigate to `C:/Users/muham/AndroidStudioProjects/StepAside`
2. When AS prompts to sync, click **Sync Now**
3. First sync downloads Gradle 8.9 — takes a few minutes
4. Subsequent syncs are fast

### 6. If sync STILL fails

Copy the full error from the "Build" tab and paste it back. The most likely
culprits at this point are:
- JDK not set to 21 — go to **File → Settings → Build → Build Tools → Gradle → Gradle JDK** and pick 21
- Cached corrupt state — **File → Invalidate Caches → Invalidate and Restart**
- Internet/proxy issue downloading Gradle 8.9 distribution

### 7. Build and run

```bash
./gradlew clean :app:assembleDebug
```

Or just `Shift+F10` in Android Studio. Install on the Pixel.

## What you should see

- Consent screen → Auth screen → Onboarding → Home screen
- Home screen shows the green progress ring, "0 steps", "10,000 to goal"
- Add the widget to home screen, tap it → app opens
- Walk around → step count updates on the home screen and widget
