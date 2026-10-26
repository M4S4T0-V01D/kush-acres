import java.util.Properties
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

// The desktop build: the phone game's model, art and screens, compiled for the JVM with Compose for Desktop.
// The few Android classes they use come from small stand-ins in src/main/kotlin/android (drawn with Skia).
plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

val versions = Properties().apply { rootProject.file("version.properties").inputStream().use { load(it) } }
val shared = "../app/src/main/java/dev/m4s4t0/kushacres"

// Shared sources: everything but the Android-only entry points, updater and sound player (patterns are
// relative to each source root, so they don't touch this module's own files under dev/m4s4t0/...).
kotlin {
    jvmToolchain(17)
    sourceSets.main {
        kotlin.srcDir(shared)
        kotlin.exclude("MainActivity.kt", "KushApp.kt", "Updater.kt", "audio/Audio.kt")
    }
}

// The version shows in Settings and decides when the updater offers a newer release.
val versionRes = layout.buildDirectory.dir("generated/version")
val writeVersion by tasks.registering {
    val out = versionRes
    val name = versions.getProperty("versionName")
    inputs.property("v", name)
    outputs.dir(out)
    doLast { out.get().file("kushacres-version.txt").asFile.apply { parentFile.mkdirs(); writeText(name) } }
}
sourceSets.main { resources.srcDir(writeVersion) }

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation("org.json:json:20250517")
}

compose.desktop {
    application {
        mainClass = "dev.m4s4t0.kushacres.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.AppImage)
            packageName = "KushAcres"
            packageVersion = versions.getProperty("versionName")
            description = "Kush Acres"
            modules("java.net.http", "java.desktop", "jdk.unsupported")
            linux { iconFile.set(project.file("src/main/resources/kush-acres.png")) }
        }
    }
}
