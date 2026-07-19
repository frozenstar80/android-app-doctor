# AppDoctor Installation

## Requirements

- Android `minSdk` 24+
- AGP 9.x
- Gradle 9.x
- JDK 17+

## Module dependency model

Ship `appdoctor-core` in all variants, and include UI-heavy modules in debug variants.

```kotlin
dependencies {
    implementation(project(":appdoctor-core"))
    debugImplementation(project(":appdoctor-ui"))
    debugImplementation(project(":appdoctor-network"))
    debugImplementation(project(":appdoctor-database"))
    debugImplementation(project(":appdoctor-compose"))
    debugImplementation(project(":appdoctor-diagnostics"))
    debugImplementation(project(":appdoctor-timeline"))
    debugImplementation(project(":appdoctor-session"))
    debugImplementation(project(":appdoctor-ai"))
    implementation(project(":appdoctor-extension"))
}
```

`appdoctor-core` self-disables in non-debuggable builds unless explicitly overridden by
`AppDoctorConfig(enabledInReleaseBuilds = true)`.
