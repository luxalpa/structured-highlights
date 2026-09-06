dependencies {
    implementation(project(":shared"))
    intellijPlatform {
        bundledModule("intellij.toml.core")
    }
}
