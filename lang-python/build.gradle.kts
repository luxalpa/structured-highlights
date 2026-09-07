dependencies {
    implementation(project(":shared"))
    intellijPlatform {
        pycharm("2026.2")

        bundledModule("intellij.python.psi")
        bundledModule("intellij.python.ast")
    }
}
