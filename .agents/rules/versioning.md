# AI AGENT VERSIONING & BUILD RULES FOR INNOVA 2KD LAUNCHER

Whenever modifying any code or asset in this project:
1. ALWAYS increment `versionCode` (+1) and `versionName` (SemVer) in `app/build.gradle.kts`.
2. ALWAYS synchronize `version.json` with the new version, changelog, and release date.
3. Target repository: `https://github.com/krisdwiantara12/innova2kd`
4. ALWAYS rebuild the APK using Gradle with JDK 17 (`F:\BOT\jdk-17\jdk-17.0.20.1+1`) and Android SDK (`F:\BOT\android-sdk`).
5. ALWAYS output the ready-to-install APK to `RELEASE_APK/Innova2KD_CoPilot_v<versionName>.apk`.
