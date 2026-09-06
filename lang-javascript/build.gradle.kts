dependencies {
    implementation(project(":shared"))
    intellijPlatform {
        bundledModule("intellij.javascript.psi.impl")
    }
}
