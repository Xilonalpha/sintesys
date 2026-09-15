# Gradle / Kotlin / AGP repair

Selected versions:
- Gradle: 8.7
- Android Gradle Plugin: 8.6.1
- Kotlin: 2.0.21
- KSP: 2.0.21-1.0.28
- JDK: 17

Why:
- AGP 8.6.x requires Gradle 8.7.
- Kotlin 2.0.21 is compatible with Gradle 8.7 and AGP 8.6.x.
- KSP is pinned to the matching Kotlin compiler line.

Wrapper:
- `gradle/wrapper/gradle-wrapper.properties` now points to Gradle 8.7.
- `gradlew` is a valid standard POSIX wrapper script.
- The original `gradle-wrapper.jar` was a 120-byte invalid placeholder.
- This environment cannot download binary artifacts, so no fake JAR was substituted.
- Run `scripts/repair-gradle-wrapper.sh` once with Gradle available to regenerate the official wrapper JAR.

Then run:
  ./gradlew clean
  ./gradlew assembleDebug

The project may still surface source/dependency errors after configuration; those must be fixed from the real Gradle compiler output.
