dependencies {
    implementation(project(":shared"))
    intellijPlatform {
        phpstorm("2026.2")
        bundledPlugin("com.jetbrains.php")
    }
}
