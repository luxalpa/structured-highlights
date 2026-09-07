dependencies {
    implementation(project(":shared"))

    intellijPlatform {
        rider("2026.2", configure = { useInstaller = false })
        bundledModule("intellij.rider.languages")
    }
}
