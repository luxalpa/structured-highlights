dependencies {
    implementation(project(":shared"))
    intellijPlatform {
        bundledModule("intellij.java.impl")
    }
}
