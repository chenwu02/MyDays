plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.ybc.mydays"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.ybc.mydays"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "2.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.vanniktech:android-image-cropper:4.5.0")
    // 引入 Glide 核心图片加载库（自动管理内存防闪退）
    implementation("com.github.bumptech.glide:glide:4.16.0")
    // 引入 Glide 图片变换特效库（包含高斯模糊）
    implementation("jp.wasabeef:glide-transformations:4.3.0")
    // 引入官方的后台任务管理器
    implementation("androidx.work:work-runtime:2.9.0")
    // 引入第三方颜色选择器
    implementation("com.github.skydoves:colorpickerview:2.3.0")
}
