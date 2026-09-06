dependencies {
    implementation(project(":shared"))
    intellijPlatform {
        goland("2026.2")
        bundledPlugin("org.jetbrains.plugins.go")
    }
}
