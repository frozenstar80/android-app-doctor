# AppDoctor Getting Started

## 1. Install AppDoctor in `Application`

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppDoctor.install(this)
    }
}
```

## 2. Optional: configure runtime behavior

```kotlin
AppDoctor.install(
    application = this,
    config = AppDoctorConfig(
        captureNetwork = true,
        captureDatabase = true,
        captureCompose = true,
        enableDiagnostics = false,
        enableTimeline = false,
        enableSessionReports = false,
        enableAi = false,
    ),
)
```

## 3. Integrate optional collectors

### OkHttp network capture

```kotlin
val networkPlugin = AppDoctorNetworkPlugin.installed()
val client = OkHttpClient.Builder().apply {
    networkPlugin?.let { addInterceptor(it.createInterceptor()) }
}.build()
```

### Room database capture

```kotlin
val db = Room.databaseBuilder(context, AppDatabase::class.java, "app.db")
    .enableAppDoctor()
    .build()
```

### Compose composable tracking (optional)

```kotlin
@Composable
fun ProductCard(product: Product) {
    TrackRecompositions("ProductCard")
}
```
