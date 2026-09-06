dependencies {
    implementation(project(":shared"))
    intellijPlatform {
        bundledModule("intellij.rustrover.core")
        bundledModule("intellij.rustrover.common")
//        bundledModule("com.intellij.modules.rustrover")
    }
}