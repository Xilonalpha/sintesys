# API 26 compatibility

The application is configured to RUN on Android API 26 (Android 8.0):
- minSdk = 26
- targetSdk = 34
- compileSdk = 34

`targetSdk` can be higher than `minSdk`; this does not prevent the app from running on API 26.

Build toolchain:
- Gradle 8.7
- AGP 8.6.1
- Kotlin 2.0.21
- Compose compiler plugin 2.0.21
- KSP 2.0.21-1.0.28
- Java/JDK 17

Java API compatibility:
- Core library desugaring is enabled with desugar_jdk_libs 2.0.4.
- No direct API-26-incompatible Android framework calls were found in the project Kotlin sources during static scan.

Important:
The uploaded project's `gradle-wrapper.jar` is an invalid 120-byte placeholder. A genuine binary wrapper JAR cannot be created/downloaded in this offline execution environment. On a connected machine with Gradle 8.7 installed, run:
  gradle wrapper --gradle-version 8.7 --distribution-type bin

Then:
  ./gradlew clean
  ./gradlew assembleDebug
