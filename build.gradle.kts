plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog")
}

tasks.runIde {
    jvmArgs("-Didea.log.debug.categories=StructuredHighlights")
}

dependencies {
    intellijPlatform {
        rustRover("2025.2")

        // Plugin Dependencies -> https://plugins.jetbrains.com/docs/intellij/plugin-dependencies.html
        bundledPlugin("com.jetbrains.rust")
    }
}

tasks.patchPluginXml {
    // Supported build number ranges and IntelliJ Platform versions -> https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html
    sinceBuild = "242"
    untilBuild = provider { null }
}