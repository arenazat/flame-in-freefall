import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    application
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("org.example.project.ingestion.FlameDataIngestorKt")
}

dependencies {
    implementation(project(":shared"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    // Ktor Client with CIO engine (for real HTTP fetching from NASA PSI/NTRS)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)

    // Supabase
    implementation(project.dependencies.platform(libs.supabase.bom))
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.storage)
    implementation(libs.supabase.functions)

    // Apache PDFBox for real document text extraction
    implementation(libs.pdfbox)
}
