dependencies {
    implementation(project(":shared"))
    intellijPlatform {
        bundledModule("intellij.python.psi")
        bundledModule("intellij.python.ast")
    }
}
