import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// 签名信息放本地的 keystore/keystore.properties（不入库），不存在时 release 构建不签名
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore/keystore.properties")
    if (f.exists()) FileInputStream(f).use { load(it) }
}

android {
    namespace = "com.dsbalance.widget"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dsbalance.widget"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "1.0.3"
        // 应用文案只有中英两套，把 androidx 库带进来的其余几十种语言资源全裁掉，resources.arsc 能小一大截喵
        resourceConfigurations += listOf("zh", "en")
    }

    packaging {
        resources {
            excludes += setOf(
                // 协程调试探针和构建期元数据，运行时都用不到喵
                "DebugProbesKt.bin",
                "kotlin-tooling-metadata.json",
                "META-INF/version-control-info.textproto",
                "META-INF/com/android/build/gradle/app-metadata.properties",
                "META-INF/*.version",
            )
        }
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (keystoreProps.isNotEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.glance.appwidget)
    implementation(libs.androidx.activity)
    implementation(libs.kotlinx.coroutines.android)
}
