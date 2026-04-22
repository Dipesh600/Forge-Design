// Top-level build file — plugin declarations only. No dependencies here.
plugins {
    alias(libs.plugins.android.application)  apply false
    alias(libs.plugins.kotlin.android)       apply false
    alias(libs.plugins.kotlin.ksp)           apply false
    alias(libs.plugins.hilt.android)         apply false
    alias(libs.plugins.google.services)      apply false
    alias(libs.plugins.kotlin.parcelize)     apply false
}