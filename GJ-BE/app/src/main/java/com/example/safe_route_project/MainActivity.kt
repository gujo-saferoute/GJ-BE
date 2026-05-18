package com.example.safe_route_project

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PointF
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.safe_route_project.app.ServiceLocator
import com.example.safe_route_project.data.shelter.DisasterType
import com.example.safe_route_project.data.shelter.RouteResult
import com.example.safe_route_project.data.shelter.ShelterPin
import com.example.safe_route_project.data.shelter.ShelterRepository
import com.example.safe_route_project.home.HomeAlertBinder
import com.example.safe_route_project.home.HomeShelterBinder
import com.example.safe_route_project.main.MainScreenController
import com.example.safe_route_project.settings.AccountSectionController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.skt.tmap.TMapData
import com.skt.tmap.TMapPoint
import com.skt.tmap.TMapView
import com.skt.tmap.overlay.TMapMarkerItem
import com.skt.tmap.overlay.TMapPolyLine
import com.skt.tmap.poi.TMapPOIItem
import org.w3c.dom.Document
import org.w3c.dom.Element


class MainActivity : AppCompatActivity() {

    private lateinit var accountSectionController: AccountSectionController
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationManager: LocationManager
    private lateinit var mapContainer: FrameLayout
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var routeInfoCard: View
    private lateinit var routeInfoTitle: TextView
    private lateinit var routeInfoDetail: TextView
    private lateinit var routeInfoDistance: TextView
    private lateinit var filterBarrierFreeChip: TextView
    private lateinit var filterAllChip: TextView
    private lateinit var filterEarthquakeChip: TextView
    private lateinit var filterRainChip: TextView
    private lateinit var filterSnowChip: TextView

    private lateinit var shelterRepository: ShelterRepository
    private lateinit var homeAlertBinder: HomeAlertBinder
    private lateinit var homeShelterBinder: HomeShelterBinder
    private lateinit var mainScreenController: MainScreenController

    private var tMapView: TMapView? = null
    private var isMapStarted = false
    private var hasMovedToInitialLocation = false
    private var locationCallback: LocationCallback? = null
    private var currentTMapPoint: TMapPoint? = null
    private var selectedShelter: ShelterPin? = null
    private var lastRouteStartPoint: TMapPoint? = null
    private var lastRouteShelter: ShelterPin? = null
    private var lastRouteSummaryText: String? = null
    private var routeRequestVersion = 0
    private var activeDisasterType: DisasterType? = null
    private var barrierFreeOnly = false

    private var shelterPins: List<ShelterPin> = emptyList()
    private val selectedRouteSummaries = mutableMapOf<TMapData.TMapPathType, RouteResult>()
    private val selectedRouteRequestsFinished = mutableSetOf<TMapData.TMapPathType>()
    private val homeRouteDistances = mutableMapOf<String, Double>()
    private var lastHomeRouteStartPoint: TMapPoint? = null

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
        shelterRepository = ServiceLocator.shelterRepository()

        mapContainer = findViewById(R.id.map_container)
        bottomNav = findViewById(R.id.bottom_nav)
        routeInfoCard = findViewById(R.id.route_info_card)
        routeInfoTitle = findViewById(R.id.route_info_title)
        routeInfoDetail = findViewById(R.id.route_info_detail)
        routeInfoDistance = findViewById(R.id.route_info_distance)
        filterBarrierFreeChip = findViewById(R.id.filter_barrier_free_chip)
        filterAllChip = findViewById(R.id.filter_all_chip)
        filterEarthquakeChip = findViewById(R.id.filter_earthquake_chip)
        filterRainChip = findViewById(R.id.filter_rain_chip)
        filterSnowChip = findViewById(R.id.filter_snow_chip)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val homeLayout = findViewById<View>(R.id.home_layout)
        val mapScreen = findViewById<View>(R.id.map_screen)
        val settingsLayout = findViewById<View>(R.id.settings_layout)
        val btnMyLocation = findViewById<FloatingActionButton>(R.id.btn_my_location)
        val switchDarkMode = findViewById<SwitchCompat>(R.id.switch_dark_mode)
        val settingsAccountCard = findViewById<View>(R.id.settings_account_card)
        val accountNameView = findViewById<TextView>(R.id.tv_account_name)
        val accountEmailView = findViewById<TextView>(R.id.tv_account_email)

        homeAlertBinder = HomeAlertBinder(
            activity = this,
            titleView = findViewById(R.id.tv_alert_title),
            messageView = findViewById(R.id.tv_alert_message),
            sourceView = findViewById(R.id.tv_alert_source),
        )

        homeShelterBinder = HomeShelterBinder(
            shelterOneName = findViewById(R.id.home_shelter_one_name),
            shelterOneDetail = findViewById(R.id.home_shelter_one_detail),
            shelterOneDistance = findViewById(R.id.home_shelter_one_distance),
            shelterTwoName = findViewById(R.id.home_shelter_two_name),
            shelterTwoDetail = findViewById(R.id.home_shelter_two_detail),
            shelterTwoDistance = findViewById(R.id.home_shelter_two_distance),
        )

        mainScreenController = MainScreenController(
            activity = this,
            bottomNav = bottomNav,
            homeLayout = homeLayout,
            mapScreen = mapScreen,
            settingsLayout = settingsLayout,
            btnMyLocation = btnMyLocation,
            switchDarkMode = switchDarkMode,
        )

        accountSectionController = AccountSectionController(
            activity = this,
            accountCard = settingsAccountCard,
            accountNameView = accountNameView,
            accountEmailView = accountEmailView,
        )


        homeAlertBinder.bind()
        accountSectionController.bind()
        observeShelters()
        setupDisasterFilterChips()
        renderHomeShelters()

        btnMyLocation.setOnClickListener {
            moveToMyLocation()
        }

        mainScreenController.bind(savedInstanceState) {
            if (!isMapStarted) {
                startTmap()
            } else {
                refreshShelterMarkers()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        mainScreenController.handleNewIntent(intent)
    }

    private fun observeShelters() {
        shelterRepository.fetchShelters(
            onSuccess = { shelters ->
                shelterPins = shelters
                renderHomeShelters()

                if (isMapStarted) {
                    refreshShelterMarkers()
                }

                val selected = selectedShelter
                if (selected != null && shelterPins.none { it.markerId == selected.markerId }) {
                    clearSelectedRoute()
                }
            },
            onFailure = {
                Toast.makeText(this, "대피소 데이터를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        )
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
                refreshShelterMarkers()
                checkLocationPermission()
            }
        })

        mapView.setOnClickListenerCallback(object : TMapView.OnClickListenerCallback {
            override fun onPressDown(
                markerItemList: ArrayList<TMapMarkerItem>,
                poiItemList: ArrayList<TMapPOIItem>,
                point: TMapPoint,
                pointF: PointF
            ) = Unit

            override fun onPressUp(
                markerItemList: ArrayList<TMapMarkerItem>,
                poiItemList: ArrayList<TMapPOIItem>,
                point: TMapPoint,
                pointF: PointF
            ) {
                val shelter = markerItemList
                    .mapNotNull { marker -> shelterPins.firstOrNull { it.markerId == marker.id } }
                    .firstOrNull()
                    ?: return

                showSelectedShelterRoute(shelter)
            }
        })

        isMapStarted = true
    }

    private fun setupDisasterFilterChips() {
        filterBarrierFreeChip.setOnClickListener { toggleBarrierFreeFilter() }
        filterAllChip.setOnClickListener { setActiveDisasterFilter(null) }
        filterEarthquakeChip.setOnClickListener { setActiveDisasterFilter(DisasterType.EARTHQUAKE) }
        filterRainChip.setOnClickListener { setActiveDisasterFilter(DisasterType.CIVIL_DEFENSE) }
        filterSnowChip.setOnClickListener { setActiveDisasterFilter(DisasterType.LANDSLIDE) }
        updateDisasterFilterChipStyle()
    }

    private fun toggleBarrierFreeFilter() {
        barrierFreeOnly = !barrierFreeOnly
        updateDisasterFilterChipStyle()
        refreshShelterMarkers()

        val selected = selectedShelter
        if (selected != null && !shelterMatchesActiveFilters(selected)) {
            clearSelectedRoute()
        }
    }

    private fun setActiveDisasterFilter(disasterType: DisasterType?) {
        activeDisasterType = disasterType
        updateDisasterFilterChipStyle()
        refreshShelterMarkers()

        val selected = selectedShelter
        if (selected != null && !shelterMatchesActiveFilters(selected)) {
            clearSelectedRoute()
        }
    }

    private fun clearSelectedRoute() {
        selectedShelter = null
        routeInfoCard.visibility = View.GONE
        tMapView?.removeTMapPolyLine(SHELTER_ROUTE_LINE_ID)
        lastRouteShelter = null
        lastRouteSummaryText = null
        selectedRouteSummaries.clear()
        selectedRouteRequestsFinished.clear()
    }

    private fun updateDisasterFilterChipStyle() {
        val selectedTextColor = ContextCompat.getColor(this, android.R.color.white)
        val normalTextColor = ContextCompat.getColor(this, R.color.sr_text_primary)
        val barrierFreeIcon = ContextCompat.getDrawable(this, R.drawable.ic_barrier_free_24)
        barrierFreeIcon?.setTint(if (barrierFreeOnly) selectedTextColor else normalTextColor)

        filterBarrierFreeChip.setBackgroundResource(
            if (barrierFreeOnly) R.drawable.bg_chip_blue else R.drawable.bg_chip_light
        )
        filterBarrierFreeChip.setTextColor(if (barrierFreeOnly) selectedTextColor else normalTextColor)
        filterBarrierFreeChip.setCompoundDrawablesWithIntrinsicBounds(barrierFreeIcon, null, null, null)

        listOf(
            filterAllChip to (activeDisasterType == null),
            filterEarthquakeChip to (activeDisasterType == DisasterType.EARTHQUAKE),
            filterRainChip to (activeDisasterType == DisasterType.CIVIL_DEFENSE),
            filterSnowChip to (activeDisasterType == DisasterType.LANDSLIDE)
        ).forEach { (chip, selected) ->
            chip.setBackgroundResource(if (selected) R.drawable.bg_chip_dark else R.drawable.bg_chip_light)
            chip.setTextColor(if (selected) selectedTextColor else normalTextColor)
        }
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
        } catch (_: SecurityException) {
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
        } catch (_: SecurityException) {
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
        } catch (_: SecurityException) {
            Toast.makeText(this, "위치 권한을 확인해주세요.", Toast.LENGTH_SHORT).show()
        } catch (_: IllegalArgumentException) {
            Toast.makeText(this, "현재 위치를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getBestLastKnownSystemLocation(): Location? {
        if (!hasLocationPermission()) return null

        return try {
            locationManager.getProviders(true)
                .mapNotNull { provider -> locationManager.getLastKnownLocation(provider) }
                .maxByOrNull { location -> location.time }
        } catch (_: SecurityException) {
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
        renderHomeShelters()
        requestHomeShelterRouteDistances(point)

        val mapView = tMapView ?: return
        mapView.setLocationPoint(location.latitude, location.longitude)
        upsertCurrentLocationMarker(point)

        if (!hasMovedToInitialLocation) {
            moveMapTo(location.latitude, location.longitude, 16, false)
            hasMovedToInitialLocation = true
        }

        selectedShelter?.let { shelter ->
            requestRouteToShelter(point, shelter, force = false)
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

    private fun refreshShelterMarkers() {
        val mapView = tMapView ?: return
        mapView.removeTMapMarkerItem(MY_LOCATION_MARKER_ID)
        shelterPins.forEach { shelter ->
            mapView.removeTMapMarkerItem(shelter.markerId)
        }

        currentTMapPoint?.let { point ->
            upsertCurrentLocationMarker(point)
        }

        shelterPins.forEach { shelter ->
            if (shelterMatchesActiveFilters(shelter)) {
                val markerItem = TMapMarkerItem().apply {
                    id = shelter.markerId
                    tMapPoint = shelter.point
                    name = shelter.name
                    icon = createShelterPin(shelter.barrierFree)
                    setPosition(0.5f, 0.5f)
                    setCalloutTitle(shelter.name)
                    setCalloutSubTitle(shelter.disasterLabels())
                    canShowCallout = true
                    autoCallloutVisible = true
                }
                mapView.addTMapMarkerItem(markerItem)
            }
        }
    }

    private fun shelterMatchesActiveFilters(shelter: ShelterPin): Boolean {
        return shelter.matchesDisasterFilter(activeDisasterType) &&
                (!barrierFreeOnly || shelter.barrierFree)
    }

    private fun renderHomeShelters() {
        val basePoint = currentTMapPoint ?: TMapPoint(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
        homeShelterBinder.render(
            shelters = shelterPins,
            basePoint = basePoint,
            routeDistances = homeRouteDistances
        )
    }

    private fun requestHomeShelterRouteDistances(startPoint: TMapPoint) {
        val previousStartPoint = lastHomeRouteStartPoint
        if (previousStartPoint != null &&
            distanceBetween(previousStartPoint, startPoint) < ROUTE_REFRESH_DISTANCE_METERS
        ) {
            return
        }

        lastHomeRouteStartPoint = startPoint
        homeRouteDistances.clear()
        shelterPins.forEach { shelter ->
            homeRouteDistances[shelter.markerId] = distanceBetween(startPoint, shelter.point).toDouble()
        }
        renderHomeShelters()
    }

    private fun showSelectedShelterRoute(shelter: ShelterPin) {
        selectedShelter = shelter
        routeInfoCard.visibility = View.VISIBLE
        routeInfoTitle.text = shelter.name

        val currentPoint = currentTMapPoint
        if (currentPoint == null) {
            routeInfoDetail.text = "${shelter.address}\n현재 위치를 확인하는 중입니다"
            routeInfoDistance.text = "경로 정보 계산 대기"
            return
        }

        requestRouteToShelter(currentPoint, shelter, force = true)
    }

    private fun requestRouteToShelter(startPoint: TMapPoint, shelter: ShelterPin, force: Boolean) {
        if (tMapView == null) return

        if (!force && lastRouteShelter == shelter) {
            val previousStartPoint = lastRouteStartPoint
            if (previousStartPoint != null &&
                distanceBetween(previousStartPoint, startPoint) < ROUTE_REFRESH_DISTANCE_METERS
            ) {
                lastRouteSummaryText?.let { routeInfoDistance.text = it }
                return
            }
        }

        lastRouteStartPoint = startPoint
        lastRouteShelter = shelter
        val requestVersion = ++routeRequestVersion
        selectedRouteSummaries.clear()
        selectedRouteRequestsFinished.clear()

        routeInfoDetail.text = "${shelter.disasterLabels()} 대피소까지의 경로를 계산하는 중입니다"
        routeInfoDistance.text = "자동차 및 보행 경로 계산 중"

        requestSingleRouteSummary(requestVersion, shelter, startPoint, TMapData.TMapPathType.CAR_PATH)
        requestSingleRouteSummary(requestVersion, shelter, startPoint, TMapData.TMapPathType.PEDESTRIAN_PATH)
    }

    private fun requestSingleRouteSummary(
        requestVersion: Int,
        shelter: ShelterPin,
        startPoint: TMapPoint,
        pathType: TMapData.TMapPathType
    ) {
        TMapData().findPathDataAllType(
            pathType,
            startPoint,
            shelter.point,
            object : TMapData.OnFindPathDataAllTypeListener {
                override fun onFindPathDataAllType(document: Document?) {
                    runOnUiThread {
                        if (requestVersion != routeRequestVersion || selectedShelter != shelter) return@runOnUiThread
                        selectedRouteRequestsFinished.add(pathType)

                        val routeResult = document?.let { parseRouteResult(it) }
                        if (routeResult != null && routeResult.points.isNotEmpty()) {
                            selectedRouteSummaries[pathType] = routeResult

                            if (pathType == TMapData.TMapPathType.PEDESTRIAN_PATH) {
                                val mapView = tMapView ?: return@runOnUiThread
                                mapView.removeTMapPolyLine(SHELTER_ROUTE_LINE_ID)
                                mapView.addTMapPolyLine(createRoutePolyLine(routeResult.points))
                                fitRouteToScreen(routeResult.points)
                            }
                        }

                        updateRouteSummaryCard(shelter)
                    }
                }
            }
        )
    }

    private fun updateRouteSummaryCard(shelter: ShelterPin) {
        val carRoute = selectedRouteSummaries[TMapData.TMapPathType.CAR_PATH]
        val pedestrianRoute = selectedRouteSummaries[TMapData.TMapPathType.PEDESTRIAN_PATH]

        if (carRoute == null && pedestrianRoute == null) {
            if (selectedRouteRequestsFinished.size >= 2) {
                showRouteFailure()
            } else {
                routeInfoDetail.text = "${shelter.disasterLabels()} 대피소까지의 경로 응답을 기다리는 중입니다"
                routeInfoDistance.text = "자동차 및 보행 경로 계산 중"
            }
            return
        }

        val evalText = if (shelter.barrierFree && shelter.evalInfo.isNotEmpty()) {
            val cleanedEval = shelter.evalInfo.split(",").map { it.trim() }.distinct().joinToString(", ")
            "\n♿ $cleanedEval"
        } else {
            ""
        }

        routeInfoDetail.text = "${shelter.address}\n${shelter.disasterLabels()}$evalText"

        val displayDistance = pedestrianRoute?.distanceMeters ?: carRoute?.distanceMeters
        val routeSummaryText = listOfNotNull(
            displayDistance?.let { "거리 ${formatDistance(it)}" },
            carRoute?.let { "자동차 예상 ${formatDuration(it.durationSeconds)}" },
            pedestrianRoute?.let { "보행 예상 ${formatDuration(it.durationSeconds)}" }
        ).joinToString("\n")

        lastRouteSummaryText = routeSummaryText
        routeInfoDistance.text = routeSummaryText
    }

    private fun showRouteFailure() {
        val mapView = tMapView ?: return
        mapView.removeTMapPolyLine(SHELTER_ROUTE_LINE_ID)
        lastRouteSummaryText = null
        val shelter = selectedShelter

        if (shelter != null) {
            val evalText = if (shelter.barrierFree && shelter.evalInfo.isNotEmpty()) {
                val cleanedEval = shelter.evalInfo.split(",").map { it.trim() }.distinct().joinToString(", ")
                "\n♿ $cleanedEval"
            } else {
                ""
            }
            routeInfoDetail.text = "${shelter.address}\n${shelter.disasterLabels()}$evalText"
            routeInfoDistance.text = "경로 계산 실패 (네트워크 오류)"
        } else {
            routeInfoDetail.text = "경로 응답이 없어 거리와 시간을 계산할 수 없습니다"
            routeInfoDistance.text = "경로 계산 실패"
        }
    }

    private fun createRoutePolyLine(points: ArrayList<TMapPoint>): TMapPolyLine {
        return TMapPolyLine().apply {
            setID(SHELTER_ROUTE_LINE_ID)
            points.forEach { point -> addLinePoint(point) }
            setLineColor(android.graphics.Color.parseColor("#0A84FF"))
            setLineWidth(10f)
            setOutLineColor(android.graphics.Color.WHITE)
            setOutLineWidth(14f)
            setLineAlpha(255)
        }
    }

    private fun parseRouteResult(document: Document): RouteResult? {
        val points = parseRoutePoints(document)
        val distanceMeters = findRouteNumber(document, "totalDistance")
        val durationSeconds = findRouteNumber(document, "totalTime")

        if (points.isEmpty() || distanceMeters == null || durationSeconds == null) return null

        return RouteResult(
            points = points,
            distanceMeters = distanceMeters,
            durationSeconds = durationSeconds
        )
    }

    private fun parseRoutePoints(document: Document): ArrayList<TMapPoint> {
        val points = arrayListOf<TMapPoint>()
        val nodes = document.getElementsByTagName("*")

        for (index in 0 until nodes.length) {
            val element = nodes.item(index) as? Element ?: continue
            if (!element.matchesTagName("coordinates")) continue

            element.textContent
                .trim()
                .split(Regex("\\s+"))
                .forEach { coordinate ->
                    val parts = coordinate.split(",")
                    val longitude = parts.getOrNull(0)?.toDoubleOrNull()
                    val latitude = parts.getOrNull(1)?.toDoubleOrNull()
                    if (latitude != null && longitude != null) {
                        points.add(TMapPoint(latitude, longitude))
                    }
                }
        }

        return points
    }

    private fun findRouteNumber(document: Document, tagName: String): Double? {
        val nodes = document.getElementsByTagName("*")

        for (index in 0 until nodes.length) {
            val element = nodes.item(index) as? Element ?: continue
            if (!element.matchesTagName(tagName)) continue
            return element.textContent.trim().toDoubleOrNull()
        }

        return null
    }

    private fun Element.matchesTagName(expectedName: String): Boolean {
        return localName == expectedName ||
                tagName == expectedName ||
                tagName.endsWith(":$expectedName")
    }

    private fun fitRouteToScreen(points: ArrayList<TMapPoint>) {
        val mapView = tMapView ?: return
        if (points.isEmpty()) return

        try {
            mapView.fitBounds(mapView.getBoundsFromPoints(points))
        } catch (_: RuntimeException) {
            val shelter = selectedShelter ?: return
            mapView.zoomToTMapPoint(currentTMapPoint ?: shelter.point, shelter.point)
        }
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

    private fun createShelterPin(barrierFree: Boolean): android.graphics.Bitmap {
        return if (barrierFree) createBarrierFreeShelterPin() else createDefaultShelterPin()
    }

    private fun createDefaultShelterPin(): android.graphics.Bitmap {
        val width = 62
        val height = 62
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        val fillPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            style = android.graphics.Paint.Style.FILL
            setShadowLayer(6f, 0f, 3f, android.graphics.Color.parseColor("#30000000"))
        }

        val strokePaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#11000000")
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 1.5f
        }

        val iconPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#111111")
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 3f
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
        }

        val radius = width / 2f - 6f
        canvas.drawCircle(width / 2f, height / 2f, radius, fillPaint)
        canvas.drawCircle(width / 2f, height / 2f, radius, strokePaint)
        drawMapPinIcon(canvas, iconPaint, width / 2f, height / 2f)
        return bitmap
    }

    private fun createBarrierFreeShelterPin(): android.graphics.Bitmap {
        val width = 62
        val height = 62
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        val fillPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#0A84FF")
            style = android.graphics.Paint.Style.FILL
            setShadowLayer(6f, 0f, 3f, android.graphics.Color.parseColor("#30000000"))
        }

        val strokePaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 3f
        }

        val iconPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 3.3f
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
        }

        val radius = width / 2f - 6f
        canvas.drawCircle(width / 2f, height / 2f, radius, fillPaint)
        canvas.drawCircle(width / 2f, height / 2f, radius, strokePaint)
        drawWheelchairIcon(canvas, iconPaint, width / 2f, height / 2f)
        return bitmap
    }

    private fun drawMapPinIcon(
        canvas: android.graphics.Canvas,
        paint: android.graphics.Paint,
        centerX: Float,
        centerY: Float
    ) {
        val markerPath = android.graphics.Path().apply {
            moveTo(centerX, centerY + 12f)
            cubicTo(centerX - 9f, centerY + 2f, centerX - 10f, centerY - 3f, centerX - 10f, centerY - 7f)
            cubicTo(centerX - 10f, centerY - 13f, centerX - 6f, centerY - 17f, centerX, centerY - 17f)
            cubicTo(centerX + 6f, centerY - 17f, centerX + 10f, centerY - 13f, centerX + 10f, centerY - 7f)
            cubicTo(centerX + 10f, centerY - 3f, centerX + 9f, centerY + 2f, centerX, centerY + 12f)
            close()
        }
        canvas.drawPath(markerPath, paint)
        canvas.drawCircle(centerX, centerY - 7f, 3.2f, paint)
    }

    private fun drawWheelchairIcon(
        canvas: android.graphics.Canvas,
        paint: android.graphics.Paint,
        centerX: Float,
        centerY: Float
    ) {
        canvas.drawCircle(centerX - 6f, centerY - 12f, 3.2f, paint)
        canvas.drawLine(centerX - 6f, centerY - 6f, centerX - 6f, centerY + 1f, paint)
        canvas.drawLine(centerX - 6f, centerY + 1f, centerX + 4f, centerY + 1f, paint)
        canvas.drawLine(centerX + 4f, centerY + 1f, centerX + 10f, centerY + 10f, paint)
        canvas.drawLine(centerX - 6f, centerY - 5f, centerX + 1f, centerY - 5f, paint)
        canvas.drawCircle(centerX - 5f, centerY + 8f, 8f, paint)
    }

    private fun distanceBetween(startPoint: TMapPoint, endPoint: TMapPoint): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            startPoint.latitude,
            startPoint.longitude,
            endPoint.latitude,
            endPoint.longitude,
            results
        )
        return results[0]
    }

    private fun formatDistance(distanceMeters: Double): String {
        return if (distanceMeters >= 1000.0) {
            String.format("%.1fkm", distanceMeters / 1000.0)
        } else {
            "${distanceMeters.toInt()}m"
        }
    }

    private fun formatDuration(durationSeconds: Double): String {
        val totalMinutes = kotlin.math.ceil(durationSeconds / 60.0).toInt().coerceAtLeast(1)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return if (hours > 0) {
            "${hours}시간 ${minutes}분"
        } else {
            "${minutes}분"
        }
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
        accountSectionController.refresh()
        if (tMapView != null) {
            startContinuousLocationTracking()
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        mainScreenController.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        tMapView?.onDestroy()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_OPEN_TAB = "open_tab"
        const val TAB_HOME = "home"
        const val TAB_MAP = "map"

        private const val DEFAULT_LATITUDE = 37.3082
        private const val DEFAULT_LONGITUDE = 127.9135
        private const val MY_LOCATION_MARKER_ID = "myLocation"
        private const val SHELTER_ROUTE_LINE_ID = "selectedShelterRoute"
        private const val ROUTE_REFRESH_DISTANCE_METERS = 30f
    }
}