plugins {
    kotlin("jvm")
    id("org.jetbrains.intellij.platform") version "2.2.1"
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

intellijPlatform {
    pluginConfiguration {
        version = project.version.toString()
        ideaVersion {
            sinceBuild = "243"
            untilBuild = "243.*"
        }
    }
}

dependencies {
    implementation(project(":apirunner-core"))
    implementation(project(":apirunner-generator"))
    implementation(project(":apirunner-http"))

    intellijPlatform {
        intellijIdeaCommunity("2024.3.6")
        bundledPlugin("com.intellij.java")
    }
}
