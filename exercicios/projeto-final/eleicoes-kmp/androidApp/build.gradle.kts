import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

android {
    namespace = "com.puciec.eleicoes.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.puciec.eleicoes"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(projects.shared)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.uiBackhandler)
    implementation(libs.compose.material3)
    implementation(libs.compose.materialIcons)
    implementation(libs.compose.uiToolingPreview)
    implementation(libs.androidx.lifecycle.viewmodelCompose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor3)

    // Já declaradas para as issues não disputarem este arquivo.
    implementation(libs.androidx.datastore.preferences) // #5 colinha, #12 cache, #13 tema
    implementation(libs.androidx.splashscreen) // #13 splash

    debugImplementation(libs.compose.uiTooling)

    testImplementation(libs.kotlin.testJunit)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
