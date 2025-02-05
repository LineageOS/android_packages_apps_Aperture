/*
 * SPDX-FileCopyrightText: 2022-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://raw.githubusercontent.com/lineage-next/gradle-generatebp/v1.21/.m2")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://raw.githubusercontent.com/lineage-next/camerax-aperture/091803a26dc5cab79f8a0567f20d85c41c8fafe0/.m2")
        maven("https://raw.githubusercontent.com/lineage-next/zxingcpp-aperture/5c134c0daf39f62b930870ecb34978480087eda9/.m2")
        google()
        mavenCentral()
    }
}

rootProject.name = "Aperture"
include(":app")
include(":lens_launcher")
