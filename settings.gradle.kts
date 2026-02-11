/*
 * SPDX-FileCopyrightText: 2022-2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven("https://raw.githubusercontent.com/lineage-next/gradle-generatebp/v1.31/.m2")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://raw.githubusercontent.com/lineage-next/camerax-aperture/3582fbc62c15fa6d18b2abfd69a9672c3446e367/.m2")
        google()
        mavenCentral()
    }
}

rootProject.name = "Aperture"
include(":app")
include(":lens_launcher")
