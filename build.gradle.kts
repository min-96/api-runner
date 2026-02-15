import org.gradle.api.tasks.testing.Test

plugins {
    kotlin("jvm") version "1.9.23" apply false
}

allprojects {
    group = "com.apirunner"
    version = "1.0.2"

    repositories {
        mavenCentral()
    }
}

subprojects {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        dependencies {
            "testImplementation"(kotlin("test"))
            "testImplementation"("org.junit.jupiter:junit-jupiter:5.10.2")
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }
}
