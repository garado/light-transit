import java.util.Properties

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

val localProperties = Properties()
val localPropertiesFile = file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}
val ghUsername = localProperties.getProperty("gpr.user") ?: System.getenv("GH_PACKAGES_USER")
val ghPassword = localProperties.getProperty("gpr.key") ?: System.getenv("GH_PACKAGES_TOKEN")

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            name = "GitHubPackages-Keyboard"
            url = uri("https://maven.pkg.github.com/lightphone/light-keyboard")
            credentials {
                username = ghUsername
                password = ghPassword
            }
        }
    }
    versionCatalogs {
        create("libs") {
            from(files("light-sdk/gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "light-sdk-template"

// Overlay local light-sdk-patch patches before light-sdk's build files/sources are read below.
// git restore'd after build to keep light-sdk submodule clean
val sdkPatchDir = file("light-sdk-patch")
val sdkDir = file("light-sdk")
if (sdkPatchDir.exists()) {
    val patchedRelativePaths = sdkPatchDir.walkTopDown()
        .filter { it.isFile && it.name != "README.md" }
        .map { it.relativeTo(sdkPatchDir).path }
        .toList()

    for (relativePath in patchedRelativePaths) {
        val target = sdkDir.resolve(relativePath)
        target.parentFile.mkdirs()
        sdkPatchDir.resolve(relativePath).copyTo(target, overwrite = true)
    }

    gradle.buildFinished {
        for (relativePath in patchedRelativePaths) {
            // tracked files: restore to their committed contents
            ProcessBuilder("git", "checkout", "--", relativePath)
                .directory(sdkDir)
                .redirectErrorStream(true)
                .start()
                .waitFor()
            // untracked/new files: remove them entirely
            ProcessBuilder("git", "clean", "-f", "--", relativePath)
                .directory(sdkDir)
                .redirectErrorStream(true)
                .start()
                .waitFor()
        }
    }
}

includeBuild("light-sdk/plugin")

include(":lint-rules")
project(":lint-rules").projectDir = file("light-sdk/lint-rules")

include(":sdk:shared")
project(":sdk:shared").projectDir = file("light-sdk/sdk/shared")

include(":sdk:ui")
project(":sdk:ui").projectDir = file("light-sdk/sdk/ui")

include(":sdk:client")
project(":sdk:client").projectDir = file("light-sdk/sdk/client")

include(":sdk:server")
project(":sdk:server").projectDir = file("light-sdk/sdk/server")

include(":sdk:emulator")
project(":sdk:emulator").projectDir = file("light-sdk/sdk/emulator")

project(":sdk").projectDir = file("light-sdk/sdk")

include(":tool")
