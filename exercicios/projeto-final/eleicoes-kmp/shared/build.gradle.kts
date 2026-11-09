import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.buildkonfig)
}

/**
 * A chave da API nunca vai para o git. Ordem de busca:
 * 1. variável de ambiente ELEICOES_API_KEY;
 * 2. arquivo .env nesta pasta ou numa pasta acima, até a raiz do repositório;
 * 3. local.properties, com eleicoes.apiKey=...
 * Sem chave, o build passa e o app mostra como configurar.
 */
fun resolveApiKey(): String {
    System.getenv("ELEICOES_API_KEY")?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    var dir: File? = rootProject.projectDir
    while (dir != null) {
        val env = File(dir, ".env")
        if (env.isFile) {
            env.readLines()
                .map { it.trim().removePrefix("export ").trim() }
                .firstOrNull { it.startsWith("ELEICOES_API_KEY=") }
                ?.substringAfter("=")?.trim()?.trim('"', '\'')
                ?.takeIf { it.isNotEmpty() }
                ?.let { return it }
        }
        if (File(dir, ".git").exists()) break
        dir = dir.parentFile
    }
    val localProperties = rootProject.file("local.properties")
    if (localProperties.isFile) {
        val props = Properties().apply { localProperties.inputStream().use { load(it) } }
        props.getProperty("eleicoes.apiKey")?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    }
    return ""
}

buildkonfig {
    packageName = "com.puciec.eleicoes"
    exposeObjectWithName = "BuildKonfig"
    defaultConfigs {
        buildConfigField(STRING, "ELEICOES_API_KEY", resolveApiKey())
    }
}

kotlin {
    androidLibrary {
        namespace = "com.puciec.eleicoes.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        withHostTest { }
    }

    // iOS: framework estático "Shared", como o da A5, com a lógica e a UI Compose (MainViewController.kt),
    // que o iosApp hospeda. Compilar e testar estes alvos exige macOS (workflow pf-eleicoes-kmp-ios);
    // no Linux o build do APK não depende deles.
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.io.core)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.uiBackhandler)
            implementation(libs.compose.material3)
            implementation(libs.compose.materialIcons)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.datastore.preferences)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.android)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
    }
}
