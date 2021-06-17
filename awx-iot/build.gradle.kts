plugins {
    id("com.android.library")
}

repositories {
    mavenCentral()
}

android {
    compileSdkVersion(30)

    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(30)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.5.10"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.10")

    val awsVersion = "2.25.0"
    implementation("com.amazonaws:aws-android-sdk-core:$awsVersion")
    implementation("com.amazonaws:aws-android-sdk-mobile-client:$awsVersion")
    implementation("com.amazonaws:aws-android-sdk-auth-userpools:$awsVersion")
    implementation("com.amazonaws:aws-android-sdk-lambda:$awsVersion")
    implementation("com.amazonaws:aws-android-sdk-iot:$awsVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")

    testImplementation("junit:junit:4.12")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.5.10")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.5.10")
    testImplementation("com.google.truth:truth:1.1.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.5.0")
}