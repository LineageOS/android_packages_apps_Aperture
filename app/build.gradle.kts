/*
 * SPDX-FileCopyrightText: 2022-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.lineageos.generatebp.GenerateBpPluginExtension
import org.lineageos.generatebp.models.Module

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.lineageos.generatebp)
}

android {
    namespace = "org.lineageos.aperture"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.lineageos.aperture"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            // Enables code shrinking, obfuscation, and optimization.
            isMinifyEnabled = true

            // Enables resource shrinking.
            isShrinkResources = true

            // Includes the default ProGuard rules files.
            setProguardFiles(
                listOf(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            )
        }

        debug {
            // Append .dev to package name so we won't conflict with AOSP build.
            applicationIdSuffix = ".dev"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    lint {
        lintConfig = file("lint.xml")
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.video)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.viewfinder.core) {
        // Invisible in AOSP, we get it via kotlinx.coroutines anyway
        exclude("org.jetbrains.kotlinx", "atomicfu")
    }
    implementation(libs.androidx.camera.extensions)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.recyclerview)
    implementation(libs.material)
    implementation(libs.coil)
    implementation(libs.coil.video)
    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.zxing.core)
    implementation(libs.zxing.cpp.android)
}

configure<GenerateBpPluginExtension> {
    targetSdk.set(android.defaultConfig.targetSdk!!)
    minSdk.set(android.defaultConfig.minSdk!!)
    availableInAOSP.set { module: Module ->
        when {
            module.group.startsWith("androidx") -> {
                // We provide our own androidx.{camera,media3}
                !module.group.startsWith("androidx.camera") &&
                        !module.group.startsWith("androidx.media3")
            }

            module.group.startsWith("org.jetbrains") -> true
            module.group == "com.google.android.material" -> true
            module.group == "com.google.auto.value" -> true
            module.group == "com.google.guava" -> true
            else -> false
        }
    }
}
