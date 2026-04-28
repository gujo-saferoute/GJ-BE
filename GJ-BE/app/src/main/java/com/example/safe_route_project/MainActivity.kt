package com.example.safe_route_project

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.skt.tmap.TMapPoint
import com.skt.tmap.TMapView
import com.skt.tmap.overlay.TMapMarkerItem

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationManager: LocationManager
    private lateinit var mapContainer: FrameLayout

    private var tMapView: TMapView? = null
    private var isMapStarted = false
    private var hasMovedToInitialLocation = false
    private var locationCallback: LocationCallback? = null
    private var currentTMapPoint: TMapPoint? = null

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            startContinuousLocationTracking()
        } else {
            Toast.makeText(this, "위치 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        mapContainer = findViewById(R.id.map_container)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val homeLayout = findViewById<View>(R.id.home_layout)
        val mapScreen = findViewById<View>(R.id.map_screen)
        val settingsLayout = findViewById<View>(R.id.settings_layout)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        val btnMyLocation = findViewById<FloatingActionButton>(R.id.btn_my_location)

        btnMyLocation.setOnClickListener {
            moveToMyLocation()
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.tab_home -> {
                    homeLayout.visibility = View.VISIBLE
                    mapScreen.visibility = View.GONE
                    settingsLayout.visibility = View.GONE
                    btnMyLocation.visibility = View.GONE
                    true
                }

                R.id.tab_map -> {
                    homeLayout.visibility = View.GONE
                    mapScreen.visibility = View.VISIBLE
                    settingsLayout.visibility = View.GONE
                    btnMyLocation.visibility = View.VISIBLE

                    if (!isMapStarted) {
                        startTmap()
                    }
                    true
                }

                R.id.tab_settings -> {
                    homeLayout.visibility = View.GONE
                    mapScreen.visibility = View.GONE
                    settingsLayout.visibility = View.VISIBLE
                    btnMyLocation.visibility = View.GONE
                    true
                }

                else -> false
            }
        }

        bottomNav.selectedItemId = R.id.tab_home
    }

    private fun startTmap() {
        if (BuildConfig.TMAP_KEY.isBlank()) {
            Toast.makeText(this, "local.properties에 TMAP_KEY를 설정해야 합니다.", Toast.LENGTH_LONG).show()
            return
        }

        val mapView = TMapView(this)
        tMapView = mapView
        mapContainer.removeAllViews()
        mapContainer.addView(
            mapView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        mapView.setSKTMapApiKey(BuildConfig.TMAP_KEY)
        mapView.setZoomLevel(15)
        mapView.setCenterPoint(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
        mapView.setLocationPoint(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)

        mapView.setOnMapReadyListener(object : TMapView.OnMapReadyListener {
            override fun onMapReady() {
                checkLocationPermission()
            }
        })

        isMapStarted = true
    }

    private fun checkLocationPermission() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            startContinuousLocationTracking()
        } else {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun startContinuousLocationTracking() {
        if (locationCallback != null) return

        if (!hasLocationPermission()) return

        val locationRequest = LocationRequest.Builder(getLocationPriority(), 3000L)
            .setMinUpdateIntervalMillis(1500L)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                updateCurrentLocation(location)
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (exception: SecurityException) {
            locationCallback = null
            Toast.makeText(this, "위치 권한을 확인해주세요.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun moveToMyLocation() {
        if (!hasLocationPermission()) {
            checkLocationPermission()
            return
        }

        currentTMapPoint?.let { point ->
            moveMapTo(point.latitude, point.longitude, 16, true)
            return
        }

        Toast.makeText(this, "현재 위치를 확인하는 중입니다.", Toast.LENGTH_SHORT).show()

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    updateCurrentLocation(location)
                    moveMapTo(location.latitude, location.longitude, 16, true)
                } else {
                    requestSingleCurrentLocation()
                }
            }
            .addOnFailureListener {
                requestSingleCurrentLocation()
            }
    }

    private fun requestSingleCurrentLocation() {
        if (!hasLocationPermission()) return

        try {
            fusedLocationClient.getCurrentLocation(
                getLocationPriority(),
                CancellationTokenSource().token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    updateCurrentLocation(location)
                    moveMapTo(location.latitude, location.longitude, 16, true)
                } else {
                    requestSingleSystemLocation()
                }
            }.addOnFailureListener {
                requestSingleSystemLocation()
            }
        } catch (exception: SecurityException) {
            requestSingleSystemLocation()
        }
    }

    private fun requestSingleSystemLocation() {
        if (!hasLocationPermission()) return

        getBestLastKnownSystemLocation()?.let { location ->
            updateCurrentLocation(location)
            moveMapTo(location.latitude, location.longitude, 16, true)
            return
        }

        val provider = getEnabledLocationProvider()
        if (provider == null) {
            Toast.makeText(this, "기기의 위치 서비스를 켜주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                locationManager.removeUpdates(this)
                updateCurrentLocation(location)
                moveMapTo(location.latitude, location.longitude, 16, true)
            }
        }

        try {
            locationManager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
        } catch (exception: SecurityException) {
            Toast.makeText(this, "위치 권한을 확인해주세요.", Toast.LENGTH_SHORT).show()
        } catch (exception: IllegalArgumentException) {
            Toast.makeText(this, "현재 위치를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getBestLastKnownSystemLocation(): Location? {
        if (!hasLocationPermission()) return null

        return try {
            locationManager.getProviders(true)
                .mapNotNull { provider -> locationManager.getLastKnownLocation(provider) }
                .maxByOrNull { location -> location.time }
        } catch (exception: SecurityException) {
            null
        }
    }

    private fun getEnabledLocationProvider(): String? {
        return when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }
    }

    private fun hasLocationPermission(): Boolean {
        val hasFineLocation = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return hasFineLocation || hasCoarseLocation
    }

    private fun getLocationPriority(): Int {
        val hasFineLocation = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return if (hasFineLocation) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }
    }

    private fun updateCurrentLocation(location: Location) {
        val point = TMapPoint(location.latitude, location.longitude)
        currentTMapPoint = point

        val mapView = tMapView ?: return
        mapView.setLocationPoint(location.latitude, location.longitude)
        upsertCurrentLocationMarker(point)

        if (!hasMovedToInitialLocation) {
            moveMapTo(location.latitude, location.longitude, 16, false)
            hasMovedToInitialLocation = true
        }
    }

    private fun upsertCurrentLocationMarker(point: TMapPoint) {
        val mapView = tMapView ?: return
        mapView.removeTMapMarkerItem(MY_LOCATION_MARKER_ID)

        val markerItem = TMapMarkerItem().apply {
            id = MY_LOCATION_MARKER_ID
            tMapPoint = point
            icon = createSmallRedDot()
            setPosition(0.5f, 0.5f)
        }

        mapView.addTMapMarkerItem(markerItem)
    }

    private fun moveMapTo(latitude: Double, longitude: Double, zoom: Int, animate: Boolean) {
        val mapView = tMapView ?: return
        mapView.setCenterPoint(latitude, longitude, animate)
        mapView.setLocationPoint(latitude, longitude)
        mapView.setZoomLevel(zoom)
    }

    private fun createSmallRedDot(): android.graphics.Bitmap {
        val size = 35
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#FF4757")
            style = android.graphics.Paint.Style.FILL
            setShadowLayer(4f, 0f, 2f, android.graphics.Color.parseColor("#40000000"))
        }

        val strokePaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 4f
        }

        val radius = size / 2f - 4f
        canvas.drawCircle(size / 2f, size / 2f, radius, paint)
        canvas.drawCircle(size / 2f, size / 2f, radius, strokePaint)

        return bitmap
    }

    override fun onPause() {
        super.onPause()
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
    }

    override fun onResume() {
        super.onResume()
        if (tMapView != null) {
            startContinuousLocationTracking()
        }
    }

    override fun onDestroy() {
        tMapView?.onDestroy()
        super.onDestroy()
    }

    private companion object {
        private const val DEFAULT_LATITUDE = 37.566481622437934
        private const val DEFAULT_LONGITUDE = 126.98502302169841
        private const val MY_LOCATION_MARKER_ID = "myLocation"
    }
}
