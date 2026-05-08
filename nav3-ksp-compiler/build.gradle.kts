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

group = "io.github.aleyn97"
version = libs.versions.navHelper.get()

mavenPublishing {
    publishToMavenCentral()
    coordinates(group.toString(), "nav3-ksp-compiler", version.toString())

    pom {
        name = "nav3KspCompiler"
        description = "Kotlin Multiplatform Router compiler"
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