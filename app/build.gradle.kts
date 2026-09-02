plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.tfg.burnout"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tfg.burnout"
        minSdk = 28          // Android 9 (Health Connect requiere API 28+)
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    androidResources {
        // El modelo .task (cientos de MB) se empaqueta SIN comprimir:
        // copia inicial más rápida y sin sorpresas del empaquetador.
        noCompress.add("task")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// Room exporta el esquema (exportSchema = true en AppDatabase) y necesita que
// se le diga dónde. Sin esta ruta el procesador avisaba en cada compilación y
// no se generaba nada; con ella, app/schemas/ queda con el JSON de cada
// versión, que documenta la estructura real de la base.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // --- Núcleo Android / Kotlin ---
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // --- Jetpack Compose (BOM gestiona versiones coherentes) ---
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // --- ViewModel + Navegación en Compose ---
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.navigation:navigation-compose:2.8.1")

    // --- Room (persistencia local sobre SQLite) ---
    val room = "2.6.1"
    implementation("androidx.room:room-runtime:$room")
    implementation("androidx.room:room-ktx:$room")
    ksp("androidx.room:room-compiler:$room")

    // --- IA local opcional: MediaPipe LLM Inference (Google AI Edge) ---
    // El modelo (p. ej. Gemma 3 1B int4, ~529 MB, fichero .task) NO se
    // empaqueta en el APK: el usuario lo importa una vez y todo corre en
    // el dispositivo, sin conexión (§2.3.6).
    implementation("com.google.mediapipe:tasks-genai:0.10.27")

    // --- Seguridad: cifrado de la BD (SQLCipher) + clave en Keystore ---
    implementation("net.zetetic:android-database-sqlcipher:4.5.4")
    implementation("androidx.sqlite:sqlite-ktx:2.4.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // --- WorkManager (tareas en segundo plano) ---
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // --- Health Connect (telemetría biométrica) ---
    implementation("androidx.health.connect:connect-client:1.1.0-alpha10")

    // --- Tests ---
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
