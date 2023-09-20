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
        maven("https://raw.githubusercontent.com/lineage-next/camerax-aperture/54901603849cb157716f4ff65bb85ab0538c30e7/.m2")
        google()
        mavenCentral()
    }
}
rootProject.name = "Aperture"
include(":app")
include(":lens_launcher")
