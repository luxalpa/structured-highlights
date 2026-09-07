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
        rustRover("2026.2")
        bundledModule("intellij.platform.monolith")
    }

    implementation(project(":shared"))

    implementation(project(":lang-rust"))
    implementation(project(":lang-css"))
    implementation(project(":lang-scss"))
    implementation(project(":lang-toml"))
    implementation(project(":lang-javascript"))
    implementation(project(":lang-python"))
    implementation(project(":lang-csharp"))
    implementation(project(":lang-java"))
    implementation(project(":lang-kotlin"))
    implementation(project(":lang-php"))
    implementation(project(":lang-go"))
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

// Java / Kotlin
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
        useInstaller = false
        jvmArgs("-Didea.log.debug.categories=StructuredHighlights")
        environment("PYTHONDONTWRITEBYTECODE", "1")
    }
}