//rootProject.buildFileName = "build.gradle.kts"

rootProject.name = "aws-ktx"
include("awx-iot")
include("awx-cognito")
include(":awx-lambda-kotlinx-serialization")
include(":awx-lambda-moshi")
include(":awx-lambda")
