plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    id("app.cash.sqldelight") version "2.1.0"
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidLibrary {
        namespace = "dev.staticvar.vlr.localsource"
        compileSdk = 36
        minSdk = 24
    }

    jvm("desktop")

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "LocalSource"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.coroutines.core)
                implementation(libs.koin.core)
                implementation(libs.kotlinx.datetime)
                // SQLDelight coroutines extensions for Flow support
                implementation("app.cash.sqldelight:coroutines-extensions:2.1.0")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.koin.test)
                implementation(libs.coroutine.test)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.koin.android)
                implementation("app.cash.sqldelight:android-driver:2.1.0")
            }
        }

        val iosMain by getting {
            dependencies {
                implementation("app.cash.sqldelight:native-driver:2.1.0")
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation("app.cash.sqldelight:sqlite-driver:2.1.0")
            }
        }
    }
}

sqldelight {
    databases {
        create("VlrDatabase") {
            packageName.set("dev.staticvar.vlr.localsource.database")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            // Disable migration verification for initial schema
            verifyMigrations.set(false)
        }
    }
}
