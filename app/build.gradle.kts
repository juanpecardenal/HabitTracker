plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "dev.juanpe.habittracker"
    // Las versiones actuales de Compose y Navigation exigen compilar contra la API 37.
    // compileSdk solo decide qué APIs se pueden usar; el comportamiento lo marca targetSdk.
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "dev.juanpe.habittracker"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        // Necesario para que Robolectric encuentre el manifest y los recursos.
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    val lifecycleVersion = "2.11.0"
    val roomVersion = "2.8.5"

    // Base de AndroidX
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.13.0")

    // Ciclo de vida: ViewModel + recogida de StateFlow desde Compose
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")

    // Jetpack Compose + Material 3
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    // Solo iconos (vectores); las animaciones y gráficos son Canvas propio.
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    // Navegación
    implementation("androidx.navigation:navigation-compose:2.10.2")

    // Room (persistencia local)
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Tests unitarios: se ejecutan en la JVM del ordenador, sin emulador.
    testImplementation("junit:junit:4.13.2")
    // Robolectric simula Android en la JVM: permite probar el repositorio con una base de datos Room real.
    testImplementation("org.robolectric:robolectric:4.17")
    testImplementation("androidx.test:core:1.7.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
