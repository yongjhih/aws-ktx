plugins {
    id("com.android.library")
    id("kotlin-android")
}

android {
    compileSdkVersion(30)

    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(30)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        //consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

}

dependencies {
    // Align versions of all Kotlin components
    //implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.4.10"))
    implementation(Depends.kotlinStdLibJdk8)

    implementation(Depends.awsAndroidCore)
    implementation(Depends.awsAndroidAuth)
    implementation(Depends.kotlinxCoroutinesCore)

    testImplementation(Depends.junit)
    testImplementation(Depends.truth)
    testImplementation(Depends.mockitoKotlin)
    testImplementation(Depends.kotlinxCoroutinesCoreTest)
    //testImplementation(Dependencies.awsAndroidTestUtils)
    //testImplementation("org.apache.httpcomponents:httpcore:4.4.5")
    testImplementation(Depends.robolectric)
    testImplementation(Depends.androidxTestCore)
    testImplementation(Depends.androidxTestCoreKtx)
}