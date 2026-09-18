pluginManagement {
    repositories {
        // Gradle Central Plugin Portal (for KSP and other plugins)
        gradlePluginPortal()
        // Maven Central (for KSP and other plugins)
        mavenCentral()
        // Try Alibaba mirror for Android/Google artifacts
        maven {
            url = uri("https://maven.aliyun.com/repository/google")
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("org\\.jetbrains.*")
            }
        }
        // Google as fallback if accessible
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Maven Central (for most open-source dependencies)
        mavenCentral()
        // Try Alibaba mirror for Android/Google artifacts
        maven {
            url = uri("https://maven.aliyun.com/repository/google")
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        // Google as fallback
        google()
    }
}

rootProject.name = "ZenPlayer"
include(":app")