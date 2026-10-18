plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    // Kotlin 2.0+ ships the Compose compiler; versioned with Kotlin in settings.gradle.kts.
    id("org.jetbrains.kotlin.plugin.compose")
    id("maven-publish")
}

android {
    namespace = "com.outerjoyn.sdk"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    // Sources live in src/main/kotlin, which is not one of the default source roots.
    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/kotlin")
        }
    }

    buildFeatures {
        compose = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.compose.ui:ui:1.6.0")
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.compose.foundation:foundation:1.6.0")
    implementation("androidx.browser:browser:1.7.0") // Chrome Custom Tabs
    implementation("androidx.webkit:webkit:1.9.0")
}

// Installed through JitPack from github.com/OuterJoyn/outerjoyn-android:
//   implementation("com.github.OuterJoyn:outerjoyn-android:<version>")
publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.github.OuterJoyn"
            artifactId = "outerjoyn-android"
            version = "1.2.0"

            // AGP creates the "release" component during its own evaluation.
            afterEvaluate {
                from(components["release"])
            }
        }
    }
}
