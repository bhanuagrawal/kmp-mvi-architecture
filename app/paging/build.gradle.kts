import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    android {
        namespace = "com.bhanuagrawal.kmplab.paging"
        compileSdk {
            version = release(libs.versions.android.compileSdk.get().toInt()) {
                minorApiLevel = 1
            }
        }
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions.jvmTarget = JvmTarget.JVM_11
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
        }
    }
}
