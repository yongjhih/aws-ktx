// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.1.3")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.10")
    }
}

plugins {
    id("version.gradle")
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "version.gradle")
}

tasks {
    val clean by registering(Delete::class) {
        delete(buildDir)
    }
}