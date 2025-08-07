plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "be.ydalton.sdrpicker"
    compileSdk = 36

    defaultConfig {
        applicationId = "be.ydalton.sdrpicker"
        minSdk = 28
        targetSdk = 28
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    ndkVersion = "29.0.13846066"

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    /*implementation("androidx.webkit:webkit:1.8.0")*/
}

val goExecutableName = "kiwipicker"
val goSourceDir = File(rootDir, "../backend")
val goOutputDir = File(buildDir, "generated-go-bin")

val abi = "arm64-v8a" // match GOARCH


val buildGo = tasks.register<Exec>("buildGo") {
    description = "Build Go binary for Android"
    group = "build"

    val ndkPath = android.ndkDirectory

    environment("GOOS", "android")
    environment("GOARCH", "arm64")
    environment("CGO_ENABLED", "1")
    environment("CC", "$ndkPath/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android28-clang")

    // Ensure output directory exists
    doFirst {
        goOutputDir.mkdirs()
    }

    workingDir = goSourceDir
    commandLine("go", "build", "-o", File(goOutputDir, goExecutableName), File(goSourceDir, "main.go"))
}

val copyGoBinary = tasks.register<Copy>("copyGoBinary") {
    dependsOn(buildGo)

    val abiLibsDir = File(projectDir, "src/main/assets")

    doFirst {
        abiLibsDir.mkdirs()
    }

    from(File(goOutputDir, goExecutableName))
    into(abiLibsDir)

    fileMode = 0b111_101_101 // 0755 permissions
}

val buildFrontend = tasks.register<Exec>("buildFrontend") {
    workingDir = File(rootDir, "../frontend")
    commandLine("npm", "run", "build")
}

val packageFrontend = tasks.register<Zip>("packageFrontend") {
    dependsOn(buildFrontend)

    from(File(rootDir, "../frontend/dist/frontend/browser"))
    destinationDirectory = File("src/main/assets")
    archiveFileName = "static.zip"
}

tasks.named("preBuild").configure {
    dependsOn(copyGoBinary)
    dependsOn(packageFrontend)
}
