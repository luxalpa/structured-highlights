dependencies {
    implementation(project(":shared"))
    intellijPlatform {
        intellijIdea("2026.2")
        bundledPlugin("org.jetbrains.kotlin")
        bundledModule("intellij.java.psi")
    }
}
