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
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://api.xposed.info/")
        maven("https://jitpack.io")
    }
}

rootProject.name = "WaEnhancer"
include(":app")

check(file("third_party/miuix/miuix-nav/src/commonMain/kotlin/top/yukonga/miuix/kmp/nav/gesture/NavSwipeDismiss.kt")
    .readText().contains("Ownership ends with the initiating pointer")) {
    "Apply the pinned Miuix gesture fix with ./scripts/apply-miuix-patches.sh"
}

includeBuild("third_party/miuix") {
    dependencySubstitution {
        substitute(module("top.yukonga.miuix.kmp:miuix-icons-android"))
            .using(project(":miuix-icons"))
        substitute(module("top.yukonga.miuix.kmp:miuix-preference-android"))
            .using(project(":miuix-preference"))
        substitute(module("top.yukonga.miuix.kmp:miuix-blur-android"))
            .using(project(":miuix-blur"))
        substitute(module("top.yukonga.miuix.kmp:miuix-ui"))
            .using(project(":miuix-ui"))
        substitute(module("top.yukonga.miuix.kmp:miuix-nav"))
            .using(project(":miuix-nav"))
    }
}
