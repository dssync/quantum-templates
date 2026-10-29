import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.4.21"
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // Compile against a locally installed Rider if one is configured (fast, no download),
        // otherwise download the Rider SDK for `platformVersion`.
        val localPath = providers.gradleProperty("platformLocalPath").orNull
        if (localPath != null && file(localPath).exists()) {
            local(localPath)
        } else {
            rider(providers.gradleProperty("platformVersion"))
        }
    }
}

intellijPlatform {
    // Skipped to keep builds fast; the one settings page (Tools > Quantum) is small enough to find by browsing.
    buildSearchableOptions = false

    pluginVerification {
        ides {
            // Check against the Rider SDK the plugin is compiled with (`./gradlew verifyPlugin`).
            val localPath = providers.gradleProperty("platformLocalPath").orNull
            if (localPath != null && file(localPath).exists()) {
                local(file(localPath))
            } else {
                create(IntelliJPlatformType.Rider, providers.gradleProperty("platformVersion"))
            }
        }
    }

    pluginConfiguration {
        version = providers.gradleProperty("pluginVersion")
        // Shown on the Marketplace page and in Rider's plugin details.
        changeNotes = providers.fileContents(layout.projectDirectory.file("change-notes.html")).asText
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            // No upper bound: the plugin only uses long-stable platform APIs.
            untilBuild = provider { null }
        }
    }
}
