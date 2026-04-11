import java.util.Properties

// 안드로이드 앱으로 빌드하기 위해 필요한 기본 플러그인 적용
plugins {
    alias(libs.plugins.android.application)
}

val properties = Properties()
properties.load(project.rootProject.file("local.properties").inputStream())
val kakaoApiKey = properties.getProperty("KAKAO_MAP_KEY") ?: ""

// 안드로이드 앱 빌드에 관련된 핵심 설정들
android {
    // 앱의 고유 식별자 (패키지명)
    namespace = "com.example.safe_route_project"

    // 앱을 컴파일할 때 기준이 되는 최신 안드로이드 SDK 버전 지정
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    // 앱의 기본 설정
    defaultConfig {
        // 플레이스토어에 출시할 때 고유하게 구별되는 앱 ID
        applicationId = "com.example.safe_route_project"
        // 이 앱이 설치될 수 있는 최소 안드로이드 버전 (24 = 안드로이드 7.0)
        minSdk = 24
        // 이 앱이 가장 최적화되어 동작하는 타겟 안드로이드 버전
        targetSdk = 36
        // 앱 스토어 업데이트 시 내부적으로 비교하는 버전 번호
        versionCode = 1
        // 사용자에게 노출되는 앱의 버전 문자열
        versionName = "1.0"
        // 안드로이드 기기에서 자동화 테스트를 실행할 때 쓰는 러너
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        //매니페스트로 키 값 전달
        manifestPlaceholders["KAKAO_MAP_KEY"] = kakaoApiKey

        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "x86_64"))
        }

    }

    // 빌드 종류(디버그, 릴리즈)에 따른 옵션
    buildTypes {
        release {
            // 앱 배포 시 코드 용량을 줄이고 난독화할지 여부 (현재는 끔)
            isMinifyEnabled = false
            // 난독화를 켤 경우 참고할 규칙 파일들 지정
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // 코틀린과 자바 코드를 컴파일할 때 맞출 자바 버전 (Java 11 호환성 유지)
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// 앱을 만들 때 외부에서 가져다 쓰는 라이브러리(종속성) 목록
dependencies {
    // 카카오 API 공통 모듈 (카카오 서비스를 앱에서 쓰기 위한 기본 토대)
    implementation("com.kakao.sdk:v2-common:2.23.3")

    // 카카오맵 V2 SDK (실제 지도 화면을 띄우고 그 위에 마커를 그리기 위해 필수)
    implementation("com.kakao.maps.open:android:2.13.1")

    // 구글 머티리얼 UI 컴포넌트 (하단 탭 바, 동그란 플로팅 액션 버튼 등을 만들기 위해 추가)
    implementation("com.google.android.material:material:1.11.0")

    // 안드로이드 카드뷰 (재난문자 알림 등을 모서리가 둥글고 그림자 진 카드 형태로 예쁘게 띄우기 위함)
    implementation("androidx.cardview:cardview:1.0.0")

    // 구글 위치 서비스 API (기기의 GPS 센서를 통해 빠르고 정확하게 내 위치 위도/경도를 추적하려고 사용)
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // 아래는 안드로이드 스튜디오 기본 라이브러리들 (최신 기능 하위 호환, 제약 레이아웃 구성 등 코어 기능)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // 앱 동작을 검증하기 위한 테스트용 라이브러리들
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}