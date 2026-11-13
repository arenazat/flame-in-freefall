import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "org.example.project.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Deb)
            // Ağ (Ktor/Supabase) ve serileştirme için gereken Java modülleri paketlenmiş sürümde eksik kalmasın
            modules("java.net.http", "jdk.unsupported", "java.sql", "java.naming", "jdk.crypto.ec")
            packageName = "FlameFall"
            packageVersion = "1.0.0"
            description = "Flame in Freefall — NASA Microgravity Combustion Lab"
        }
    }
}