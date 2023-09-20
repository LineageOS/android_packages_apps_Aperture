/*
 * SPDX-FileCopyrightText: 2022-2023 The LineageOS Project
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
        maven("https://raw.githubusercontent.com/lineage-next/camerax-aperture/4093f4855120e7d792c81f18f8f558a5ef5a81f2/.m2")
        google()
        mavenCentral()
    }
}
rootProject.name = "Aperture"
include(":app")
include(":lens_launcher")
