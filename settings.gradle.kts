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
        maven("https://raw.githubusercontent.com/lineage-next/camerax-aperture/f325416f8533fb2ff540259ea955c541460ea785/.m2")
        maven("https://raw.githubusercontent.com/lineage-next/zxingcpp-aperture/7c0350df39a3e10a91d660e0e3b83af86e09f997/.m2")
        google()
        mavenCentral()
    }
}

rootProject.name = "Aperture"
include(":app")
include(":lens_launcher")
