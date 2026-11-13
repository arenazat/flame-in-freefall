import java.util.Properties
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm()
    
    js {
        browser()
        binaries.executable()
    }
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }
    
    android {
       namespace = "org.example.project.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)

            // DI: Koin
            api(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // Supabase (Postgrest, Storage, Functions)
            implementation(project.dependencies.platform(libs.supabase.bom))
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.storage)
            implementation(libs.supabase.functions)

            // Ktor
            implementation(libs.ktor.client.core)
        }
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.ktor.client.okhttp)
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
        jsMain.dependencies {
            implementation(libs.wrappers.browser)
            implementation(libs.ktor.client.js)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

abstract class GenerateFlameBuildConfigTask : DefaultTask() {
    @get:Input
    abstract val supabaseUrl: Property<String>

    @get:Input
    abstract val supabaseAnonKey: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val url = supabaseUrl.get()
        val key = supabaseAnonKey.get()
        val file = outputDir.get().asFile.resolve("org/example/project/utils/FlameBuildConfig.kt")
        file.parentFile.mkdirs()
        file.writeText(
            """
            package org.example.project.utils

            internal object FlameBuildConfig {
                const val SUPABASE_URL: String = "$url"
                const val SUPABASE_ANON_KEY: String = "$key"
            }
            """.trimIndent()
        )
    }
}

val localProps = Properties().apply {
    val propFile = rootProject.file("local.properties")
    if (propFile.exists()) {
        propFile.inputStream().use { load(it) }
    }
}

val defaultUrl = localProps.getProperty("SUPABASE_URL") ?: ""
val defaultKey = localProps.getProperty("SUPABASE_ANON_KEY") ?: ""

val supabaseUrlProvider = providers.environmentVariable("SUPABASE_URL")
    .orElse(providers.gradleProperty("SUPABASE_URL"))
    .orElse(defaultUrl)

val supabaseAnonKeyProvider = providers.environmentVariable("SUPABASE_ANON_KEY")
    .orElse(providers.gradleProperty("SUPABASE_ANON_KEY"))
    .orElse(defaultKey)

val generateFlameBuildConfig by tasks.registering(GenerateFlameBuildConfigTask::class) {
    supabaseUrl.set(supabaseUrlProvider)
    supabaseAnonKey.set(supabaseAnonKeyProvider)
    outputDir.set(layout.buildDirectory.dir("generated/flame/config"))
}

kotlin.sourceSets.named("commonMain") {
    kotlin.srcDir(generateFlameBuildConfig)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    dependsOn(generateFlameBuildConfig)
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}