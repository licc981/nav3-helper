import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.maven.publish)
}

kotlin {

    jvmToolchain(17)

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { browser() }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.bundles.navigation3)
            implementation(libs.kotlinx.serialization.json)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

    }

    targets.withType<KotlinNativeTarget> {
        compilations["main"].compileTaskProvider.configure {
            compilerOptions {
                freeCompilerArgs.add("-Xexport-kdoc")
            }
        }
    }


}

group = "io.github.aleyn97"
version = libs.versions.navHelper.get()

mavenPublishing {
    publishToMavenCentral()
    coordinates(group.toString(), "navigation3-helper", version.toString())

    pom {
        name = "nav3Helper"
        description = "Kotlin Multiplatform Router"
        inceptionYear = "2026"
        url = "https://github.com/aleyn97/nav3-helper"

        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }

        developers {
            developer {
                id = "aleyn"
                name = "Aleyn Developer"
                url = "https://github.com/aleyn97"
                email = "pclckk@gmail.com"
                organization = "personal"
                organizationUrl = "https://github.com/aleyn97"
            }
        }

        scm {
            url = "https://github.com/aleyn97/nav-helper"
            connection = "scm:git:git://github.com/aleyn97/nav-helper.git"
            developerConnection = "scm:git:ssh://git@github.com/aleyn97/nav-helper.git"
        }
    }
    if (project.hasProperty("signing.keyId")) signAllPublications()
}
