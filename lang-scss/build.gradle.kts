dependencies {
    implementation(project(":shared"))
    implementation(project(":lang-css"))
    intellijPlatform {
        bundledModule("intellij.sass")
        bundledModule("intellij.css")
        bundledModule("intellij.css.common.psi")
    }
}