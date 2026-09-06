dependencies {
    implementation(project(":shared"))
    intellijPlatform {
        bundledPlugin("org.jetbrains.plugins.go")
    }
}
