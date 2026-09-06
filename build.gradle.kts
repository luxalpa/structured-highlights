import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog")
}

tasks.runIde {
    jvmArgs("-Didea.log.debug.categories=StructuredHighlights")
    environment("PYTHONDONTWRITEBYTECODE", "1")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(23))
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.intellij.platform.module")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(23))
        }
    }
}

dependencies {
    intellijPlatform {
//        rustRover("2026.2")
        clion("2026.2")
//        phpstorm("2025.2")
//        intellijIdea("2025.2")
//        rider("2025.2")
        bundledModule("intellij.platform.monolith")

        // Plugin Dependencies -> https://plugins.jetbrains.com/docs/intellij/plugin-dependencies.html
//        plugins(
//            "com.jetbrains.rust:252.23892.452",
//            "org.jetbrains.kotlin:252.28539.97-IJ",
//            "com.intellij.java:252.23892.409",
//            "com.jetbrains.php:252.23892.458",
//            "org.toml.lang:252.28238.9",
//            "dev.blachut.svelte.lang:252.23892.298",
//            "com.intellij.css:252.27397.28",
//            "org.jetbrains.plugins.sass:252.23892.358",
//        )
//        plugins("JavaScript:252.23892.452")

//        bundledPlugin("JavaScript")
    }
    implementation(project(":shared"))
    implementation(project(":lang-cpp"))
    implementation(project(":lang-rust"))
    implementation(project(":lang-css"))
    implementation(project(":lang-scss"))
}

tasks.patchPluginXml {
    // Supported build number ranges and IntelliJ Platform versions -> https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html
    sinceBuild = "242"
    untilBuild = provider { null }

    pluginDescription = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
        val start = "<!-- Plugin description -->"
        val end = "<!-- Plugin description end -->"

        with(it.lines()) {
            if (!containsAll(listOf(start, end))) {
                throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
            }
            subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
        }
    }
}

val runCLion by intellijPlatformTesting.runIde.registering {
    type = IntelliJPlatformType.CLion
    version = "2026.2"
    task {
        jvmArgs("-Didea.log.debug.categories=StructuredHighlights")
        environment("PYTHONDONTWRITEBYTECODE", "1")
    }
}

val runRustRover by intellijPlatformTesting.runIde.registering {
    type = IntelliJPlatformType.RustRover
    version = "2026.2"
    task {
        jvmArgs("-Didea.log.debug.categories=StructuredHighlights")
        environment("PYTHONDONTWRITEBYTECODE", "1")
    }
}