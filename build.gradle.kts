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
//        phpstorm("2026.2")
//        intellijIdea("2026.2")   // Ultimate: Java / Kotlin / JavaScript / TypeScript
//        rider("2026.2")          // C#
//        goland("2026.2")         // Go
//        pycharmProfessional("2026.2") // Python (also bundled in CLion)
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
    implementation(project(":lang-toml"))
    implementation(project(":lang-javascript"))
    implementation(project(":lang-python"))
    implementation(project(":lang-csharp"))

    // The following language modules target IDEs other than the currently-active one (CLion) and
    // therefore only compile when the matching IDE is selected above. Uncomment them together with
    // the appropriate IDE dependency (e.g. `intellijIdea(...)`, `phpstorm(...)`, `goland(...)`).
//    implementation(project(":lang-java"))
//    implementation(project(":lang-kotlin"))
//    implementation(project(":lang-php"))
//    implementation(project(":lang-go"))
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

// Java / Kotlin / JavaScript / TypeScript
val runIdea by intellijPlatformTesting.runIde.registering {
    type = IntelliJPlatformType.IntellijIdeaUltimate
    version = "2026.2"
    task {
        jvmArgs("-Didea.log.debug.categories=StructuredHighlights")
        environment("PYTHONDONTWRITEBYTECODE", "1")
    }
}

// PHP
val runPhpStorm by intellijPlatformTesting.runIde.registering {
    type = IntelliJPlatformType.PhpStorm
    version = "2026.2"
    task {
        jvmArgs("-Didea.log.debug.categories=StructuredHighlights")
        environment("PYTHONDONTWRITEBYTECODE", "1")
    }
}

// Go
val runGoLand by intellijPlatformTesting.runIde.registering {
    type = IntelliJPlatformType.GoLand
    version = "2026.2"
    task {
        jvmArgs("-Didea.log.debug.categories=StructuredHighlights")
        environment("PYTHONDONTWRITEBYTECODE", "1")
    }
}

// Python
val runPyCharm by intellijPlatformTesting.runIde.registering {
    type = IntelliJPlatformType.PyCharmProfessional
    version = "2026.2"
    task {
        jvmArgs("-Didea.log.debug.categories=StructuredHighlights")
        environment("PYTHONDONTWRITEBYTECODE", "1")
    }
}

// C#
val runRider by intellijPlatformTesting.runIde.registering {
    type = IntelliJPlatformType.Rider
    version = "2026.2"
    task {
        jvmArgs("-Didea.log.debug.categories=StructuredHighlights")
        environment("PYTHONDONTWRITEBYTECODE", "1")
    }
}