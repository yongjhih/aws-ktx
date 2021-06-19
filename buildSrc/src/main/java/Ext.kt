object Version {
    const val androidGradle = "4.1.3"
    const val kotlin = "1.5.0"
    const val kotlinxCoroutines = "1.5.0"
    const val aws = "2.25.0"
    const val junit = "4.12"
    const val truth = "1.1.3"
}

object Dependency {
    const val kotlinxCoroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Version.kotlinxCoroutines}"
    const val kotlinStdLibJdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Version.kotlin}"
    const val awsAndroidCore = "com.amazonaws:aws-android-sdk-core:${Version.aws}"
    const val awsAndroidAuth = "com.amazonaws:aws-android-sdk-auth-userpools:${Version.aws}"
    const val awsAndroidIot = "com.amazonaws:aws-android-sdk-iot:${Version.aws}"
    const val junit = "junit:junit:${Version.junit}"
    const val truth = "com.google.truth:truth:${Version.truth}"
}