// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}

// Global: exclude com.intellij:annotations (conflicts with org.jetbrains:annotations
// that comes from Kotlin stdlib). The IntelliJ artifact is legacy and exists in
// some transitive deps like older okhttp/retrofit.
subprojects {
    configurations.all {
        exclude(group = "com.intellij", module = "annotations")
        resolutionStrategy {
            force("org.jetbrains:annotations:23.0.0")
        }
    }
}
