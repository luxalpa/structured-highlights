dependencies {
    implementation(project(":shared"))
    intellijPlatform {
        bundledModule("intellij.rider.cpp.core.languages")
    }
}