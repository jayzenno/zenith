pluginManagement {
    repositories {
        // Try Alibaba mirror first (publicly available, often works in restricted environments)
        maven {
            url = uri("https://maven.aliyun.com/repository/google")
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("org\\.jetbrains.*")
            }
        }
        // Gradle Central Plugin Portal
        gradlePluginPortal()
        // Maven Central as fallback
        mavenCentral()
        // Google as fallback if accessible
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Try Alibaba mirror first
        maven {
            url = uri("https://maven.aliyun.com/repository/google")
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        // Maven Central
        mavenCentral()
        // Google as fallback
        google()
    }
}

rootProject.name = "ZenPlayer"
include(":app")