plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.jjw.blackscreen"
    // Compose 1.12 가 요구한다. targetSdk 는 36 에 둔다 — 실기기 검증 수단이 없는 상태에서
    // targetSdk 를 올리면 확인하지 못한 동작 변경을 그대로 떠안게 된다.
    compileSdk = 37

    defaultConfig {
        applicationId = "com.jjw.blackscreen"
        // TYPE_APPLICATION_OVERLAY 가 API 26 부터라 이보다 낮출 수 없다.
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
        debug {
            // en-XA / ar-XB 의사 로케일. 감싸이지 않은 글자 = 추출 누락, 잘림 = 고정폭.
            isPseudoLocalesEnabled = true
        }
    }

    androidResources {
        // values-* 를 보고 locales_config.xml 을 만들어 Android 13+ 앱별 언어 목록에 올린다.
        // 기본 로케일은 res/resources.properties 의 unqualifiedResLocale 이 알려 준다.
        generateLocaleConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.savedstate.ktx)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)
}
