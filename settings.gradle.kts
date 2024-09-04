/*
 * SPDX-FileCopyrightText: 2022-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://raw.githubusercontent.com/lineage-next/camerax-aperture/d6fe3a7d39ecad777c368fd5f4f981b79fbb18cf/.m2")
        maven("https://raw.githubusercontent.com/lineage-next/zxingcpp-aperture/7c0350df39a3e10a91d660e0e3b83af86e09f997/.m2")
        google()
        mavenCentral()
    }
}
rootProject.name = "Aperture"
include(":app")
include(":lens_launcher")
