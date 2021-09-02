plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("version.gradle") {
            id = "version.gradle"
            implementationClass = "version.gradle.VersionGradle"
        }
    }
}
