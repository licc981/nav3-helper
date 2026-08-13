plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.maven.publish)
}

kotlin {

    jvm()

    jvmToolchain(17)

    sourceSets {
        commonMain.dependencies {
            implementation(project(":navigation3-helper"))
            implementation(libs.kotlinpoet)
            implementation(libs.kotlinpoet.ksp)
            implementation(libs.ksp.api)
        }

        jvmTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

group = "io.github.licc981"
version = libs.versions.navHelper.get()

mavenPublishing {
    publishToMavenCentral()
    coordinates(group.toString(), "nav3-ksp-compiler", version.toString())

    pom {
        name = "nav3KspCompiler"
        description = "Kotlin Multiplatform Router compiler"
        inceptionYear = "2026"
        url = "https://github.com/licc981/nav3-helper"

        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }

        developers {
            developer {
                id = "licc981"
                name = "lcc"
                url = "https://github.com/licc981"
                email = "lccbyxy@163.com"
                organization = "personal"
                organizationUrl = "https://github.com/licc981"
            }
        }

        scm {
            url = "https://github.com/licc981/nav3-helper"
            connection = "scm:git:https://github.com/licc981/nav3-helper.git"
            developerConnection = "scm:git:ssh://git@github.com/licc981/nav3-helper.git"
        }
    }
    signAllPublications()
}
