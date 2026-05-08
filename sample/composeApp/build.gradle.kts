import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.ksp)
}

kotlin {
    android {
        namespace = "com.aleyn.navigation"
        compileSdk = 36
        androidResources { enable = true }
        compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
    }

    jvmToolchain(17)

    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.runtime)
                implementation(libs.compose.ui)
                implementation(libs.components.resources)
                implementation(libs.compose.ui.tooling.preview)
                implementation(libs.androidx.lifecycle.viewmodelCompose)
                implementation(libs.androidx.lifecycle.runtimeCompose)
                implementation(libs.bundles.navigation3)

                implementation(project(":sample:child_first"))
                implementation(project(":sample:child_second"))
                api(project(":navigation3-helper"))

            }
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
        }



        androidMain.dependencies {
            implementation(libs.activity.compose)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }

    }
}

dependencies {
    val project = project(":nav3-ksp-compiler")
    add("kspCommonMainMetadata", project)
    add("kspAndroid", project)
    add("kspIosX64", project)
    add("kspIosArm64", project)
    add("kspIosSimulatorArm64", project)
}

// KSP Metadata Trigger
tasks.matching { it.name.startsWith("ksp") && it.name != "kspCommonMainKotlinMetadata" }
    .configureEach {
        dependsOn("kspCommonMainKotlinMetadata")
    }

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "sample"
            packageVersion = "1.0.0"
        }
    }
}
