dependencies {
    implementation(project(":shared"))
    intellijPlatform {
        intellijIdea("2026.2")
        bundledModule("intellij.java")
        bundledModule("intellij.java.psi")
    }
}
