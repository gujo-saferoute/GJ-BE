package com.example.safe_route_project

// 안드로이드 OS 기본 기능, 권한(GPS), 생명주기 처리용
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

// 기기의 현재 GPS 위치를 추적하고 가져오기 위한 구글 위치 서비스 API
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

// 화면 하단 탭 메뉴와 지도 위의 동그란 플로팅 버튼을 사용하기 위한 머티리얼 UI 컴포넌트
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

// 카카오맵 V2 SDK 요소들 (지도 렌더링, 카메라 이동, 위경도 좌표, 커스텀 마커 제작용)
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles

class MainActivity : AppCompatActivity() {

    // 지도 객체 담아둘 변수
    private var kakaoMap: KakaoMap? = null
    // GPS 센서 조작용 클라이언트
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    // 탭을 왔다갔다 할 때 지도가 중복으로 켜지는 걸 막는 플래그
    private var isMapStarted = false

    // 내 위치를 표시할 지도 위 마커(라벨)
    private var myLocationLabel: Label? = null
    // 위치 정보가 갱신될 때마다 실행될 콜백 함수
    private var locationCallback: LocationCallback? = null

    // 내 위치 버튼을 눌렀을 때 카메라를 이동시키기 위해 가장 마지막 위치를 기억하는 변수
    private var currentLatLng: LatLng? = null

    // 안드로이드 최신 버전의 권한 요청 방식 (팝업 띄우고 결과 받기)
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            startContinuousLocationTracking()
        } else {
            Toast.makeText(this, "위치 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 상단 상태바와 하단 네비게이션바 영역까지 화면을 넓게 쓰기 위한 최신 UI 설정
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // 위치 서비스 초기화
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 화면을 넓게 쓰면서 시스템 UI(상태바 등)와 앱 화면이 겹치지 않게 여백 조정
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // XML에 있는 UI 요소들 연결
        val mapView = findViewById<MapView>(R.id.map_view)
        val homeLayout = findViewById<View>(R.id.home_layout)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        val btnMyLocation = findViewById<FloatingActionButton>(R.id.btn_my_location)

        // 내 위치 버튼 동작 설정
        btnMyLocation.setOnClickListener {
            currentLatLng?.let { position ->
                // 저장해둔 최신 좌표로 지도 중심 이동. 15는 적당히 확대된 줌 레벨
                kakaoMap?.moveCamera(CameraUpdateFactory.newCenterPosition(position, 15))
            } ?: run {
                Toast.makeText(this, "위치 정보를 불러오는 중입니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // 하단 탭 터치 시 화면 전환 로직
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.tab_home -> {
                    // 홈 화면 켜고 지도 관련 뷰 끄기
                    homeLayout.visibility = View.VISIBLE
                    mapView.visibility = View.GONE
                    btnMyLocation.visibility = View.GONE
                    true
                }
                R.id.tab_map -> {
                    // 지도 화면 켜고 홈 화면 끄기
                    homeLayout.visibility = View.GONE
                    mapView.visibility = View.VISIBLE
                    btnMyLocation.visibility = View.VISIBLE

                    // 지도가 아직 안 켜졌을 때만 최초 실행
                    if (!isMapStarted) {
                        startKakaoMap(mapView)
                        isMapStarted = true
                    }
                    true
                }
                else -> false
            }
        }

        // 앱 실행 시 기본으로 홈 탭이 눌려있도록 설정
        bottomNav.selectedItemId = R.id.tab_home
    }

    // 카카오맵 렌더링 시작
    private fun startKakaoMap(mapView: MapView) {
        mapView.start(object : MapLifeCycleCallback() {
            override fun onMapDestroy() {}
            override fun onMapError(error: Exception?) {
                Log.e("KakaoMap", "지도 에러: ${error?.message}")
            }
        }, object : KakaoMapReadyCallback() {
            override fun onMapReady(map: KakaoMap) {
                // 지도가 정상적으로 다 그려지면 객체를 저장하고 위치 권한 체크 시작
                kakaoMap = map
                checkLocationPermission()
            }
        })
    }

    // 위치 권한이 있는지 확인하고 없으면 요청 팝업 띄우기
    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startContinuousLocationTracking()
        } else {
            locationPermissionRequest.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    // 실제 GPS 추적을 시작하는 핵심 함수
    private fun startContinuousLocationTracking() {
        // 권한이 없는데 실행되는 걸 막는 안전장치 (앱 튕김 방지)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        // 2초(2000ms)마다 높은 정확도로 위치를 요청하도록 설정
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L).build()

        // 새 위치 정보가 들어올 때마다 작동할 내용 정의
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    // 받아온 위도, 경도를 카카오맵 전용 좌표 객체로 변환
                    val myPosition = LatLng.from(location.latitude, location.longitude)

                    // 버튼용 최신 위치 갱신
                    currentLatLng = myPosition

                    if (myLocationLabel == null) {
                        // 앱 켜고 위치를 처음 찾았을 때 1번만 카메라를 그쪽으로 옮김
                        kakaoMap?.moveCamera(CameraUpdateFactory.newCenterPosition(myPosition))

                        // 지도 위에 마커를 그리기 위한 레이어 가져오기
                        kakaoMap?.labelManager?.layer?.let { layer ->
                            // 직접 그린 동그라미 이미지를 마커 스타일로 지정. 0.5f는 이미지 정중앙을 좌표에 맞춘다는 뜻
                            val style = LabelStyle.from(createSmallRedDot()).setAnchorPoint(0.5f, 0.5f)
                            val options = LabelOptions.from(myPosition).setStyles(LabelStyles.from(style))

                            // 지도에 마커 추가 후 변수에 저장
                            myLocationLabel = layer.addLabel(options)
                        }
                    } else {
                        // 두 번째 위치 갱신부터는 카메라는 냅두고 마커(빨간 점)만 새 좌표로 이동시킴
                        myLocationLabel?.moveTo(myPosition)
                    }
                }
            }
        }

        // 위에서 설정한 조건대로 위치 업데이트 시작
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback!!, Looper.getMainLooper())
    }

    // 마커로 쓸 이미지를 파일에서 불러오지 않고 코드로 직접 그려서 성능 최적화
    private fun createSmallRedDot(): android.graphics.Bitmap {
        val size = 35
        // 투명한 도화지 생성
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        // 빨간색 원형 붓 세팅 (그림자 포함)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#FF4757")
            style = android.graphics.Paint.Style.FILL
            setShadowLayer(4f, 0f, 2f, android.graphics.Color.parseColor("#40000000"))
        }

        // 하얀색 테두리 붓 세팅
        val strokePaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 4f
        }

        // 여백 고려해서 반지름 계산 후 캔버스에 그리기
        val radius = size / 2f - 4f
        canvas.drawCircle(size / 2f, size / 2f, radius, paint)
        canvas.drawCircle(size / 2f, size / 2f, radius, strokePaint)

        return bitmap
    }

    // 앱 화면이 다시 사용자에게 보일 때
    override fun onResume() {
        super.onResume()
        // 권한이 정상이면 꺼뒀던 GPS 추적 다시 시작
        if (locationCallback != null && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startContinuousLocationTracking()
        }
    }

    // 사용자가 홈버튼을 누르거나 다른 앱으로 넘어갈 때
    override fun onPause() {
        super.onPause()
        // 보이지도 않는데 계속 GPS를 찾으면 배터리가 닳으므로 임시 중단
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
    }
}