package com.example.safe_route_project


import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ImageSpan
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.HorizontalScrollView
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.safe_route_project.app.ServiceLocator
import com.example.safe_route_project.data.shelter.RouteResult
import com.example.safe_route_project.data.shelter.ShelterPin
import com.example.safe_route_project.data.shelter.ShelterRepository
import com.example.safe_route_project.home.HomeAlertBinder
import com.example.safe_route_project.home.HomeShelterBinder
import com.example.safe_route_project.main.MainScreenController
import com.example.safe_route_project.settings.AccountSectionController
import com.example.safe_route_project.settings.AppThemeManager
import com.example.safe_route_project.settings.DisasterTestManager
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
import com.skt.tmap.TMapInsets
import com.skt.tmap.TMapPoint
import com.skt.tmap.TMapView
import com.skt.tmap.overlay.TMapMarkerItem
import com.skt.tmap.overlay.TMapPolyLine
import com.skt.tmap.poi.TMapPOIItem
import org.w3c.dom.Document
import org.w3c.dom.Element


class MainActivity : AppCompatActivity() {

    private lateinit var routeInfoBarrierScroll: HorizontalScrollView
    private lateinit var routeInfoBarrierFreeIcon: ImageView
    private lateinit var routeInfoDividerIcon: ImageView
    private lateinit var routeInfoElevatorIcon: ImageView
    private lateinit var routeInfoParkingIcon: ImageView
    private lateinit var routeInfoToiletIcon: ImageView
    private lateinit var routeInfoEntranceIcon: ImageView
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
    private lateinit var searchShelterInput: EditText
    private lateinit var searchSuggestionsContainer: LinearLayout

    private lateinit var shelterRepository: ShelterRepository
    private lateinit var homeAlertBinder: HomeAlertBinder
    private lateinit var homeShelterBinder: HomeShelterBinder
    private lateinit var mainScreenController: MainScreenController

    private var tMapView: TMapView? = null

    private var shouldRefreshHomeAlertOnResume = false
    private var isMapStarted = false
    private var hasMovedToInitialLocation = false
    private var locationCallback: LocationCallback? = null
    private var currentTMapPoint: TMapPoint? = null
    private var selectedShelter: ShelterPin? = null
    private var lastRouteStartPoint: TMapPoint? = null
    private var lastRouteShelter: ShelterPin? = null
    private var lastRouteSummaryText: String? = null
    private var routeRequestVersion = 0
    private var barrierFreeOnly = false
    private var isApplyingSearchSuggestion = false

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
        routeInfoBarrierScroll = findViewById(R.id.route_info_barrier_scroll)
        routeInfoBarrierFreeIcon = findViewById(R.id.route_info_barrierfree_icon)
        routeInfoDividerIcon = findViewById(R.id.route_info_divider_icon)
        routeInfoElevatorIcon = findViewById(R.id.route_info_elevator_icon)
        routeInfoParkingIcon = findViewById(R.id.route_info_parking_icon)
        routeInfoToiletIcon = findViewById(R.id.route_info_toilet_icon)
        routeInfoEntranceIcon = findViewById(R.id.route_info_entrance_icon)
        filterBarrierFreeChip = findViewById(R.id.filter_barrier_free_chip)
        searchShelterInput = findViewById(R.id.search_shelter_input)
        searchSuggestionsContainer = findViewById(R.id.search_suggestions_container)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)

            insets
        }

        val homeLayout = findViewById<View>(R.id.home_layout)
        val mapScreen = findViewById<View>(R.id.map_screen)
        val settingsLayout = findViewById<View>(R.id.settings_layout)
        val btnViewAllShelters = findViewById<TextView>(R.id.btn_view_all_shelters)
        val btnMyLocation = findViewById<FloatingActionButton>(R.id.btn_my_location)
        val switchDarkMode = findViewById<SwitchCompat>(R.id.switch_dark_mode)
        val switchDisasterTest = findViewById<SwitchCompat>(R.id.switch_disaster_test)
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
            shelterOneBarrierScroll = findViewById(R.id.home_shelter_one_barrier_scroll),
            shelterOneBarrierFreeIcon = findViewById(R.id.home_shelter_one_barrierfree_icon),
            shelterOneDividerIcon = findViewById(R.id.home_shelter_one_divider_icon),
            shelterOneElevatorIcon = findViewById(R.id.home_shelter_one_elevator_icon),
            shelterOneParkingIcon = findViewById(R.id.home_shelter_one_parking_icon),
            shelterOneToiletIcon = findViewById(R.id.home_shelter_one_toilet_icon),
            shelterOneEntranceIcon = findViewById(R.id.home_shelter_one_entrance_icon),
            shelterOneDetail = findViewById(R.id.home_shelter_one_detail),
            shelterOneDistanceText = findViewById(R.id.home_shelter_one_distance_text),
            shelterOneAction = findViewById(R.id.home_shelter_one_distance),

            shelterTwoName = findViewById(R.id.home_shelter_two_name),
            shelterTwoBarrierScroll = findViewById(R.id.home_shelter_two_barrier_scroll),
            shelterTwoBarrierFreeIcon = findViewById(R.id.home_shelter_two_barrierfree_icon),
            shelterTwoDividerIcon = findViewById(R.id.home_shelter_two_divider_icon),
            shelterTwoElevatorIcon = findViewById(R.id.home_shelter_two_elevator_icon),
            shelterTwoParkingIcon = findViewById(R.id.home_shelter_two_parking_icon),
            shelterTwoToiletIcon = findViewById(R.id.home_shelter_two_toilet_icon),
            shelterTwoEntranceIcon = findViewById(R.id.home_shelter_two_entrance_icon),
            shelterTwoDetail = findViewById(R.id.home_shelter_two_detail),
            shelterTwoDistanceText = findViewById(R.id.home_shelter_two_distance_text),
            shelterTwoAction = findViewById(R.id.home_shelter_two_distance),
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

        DisasterTestManager.bind(this, switchDisasterTest) { enabled ->
            homeAlertBinder.refresh(sendTestNotification = enabled)
        }

        observeShelters()
        setupShelterFilterChips()
        setupShelterSearch()
        renderHomeShelters()

        btnMyLocation.setOnClickListener {
            moveToMyLocation()
        }

        btnViewAllShelters.setOnClickListener {
            showNearbySheltersDialog()
        }

        mainScreenController.bind(savedInstanceState) {
            if (!isMapStarted) {
                startTmap()
            } else {
                refreshShelterMarkers()
            }
        }
        shouldRefreshHomeAlertOnResume = false
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

    private fun barrierDividerDrawableRes(): Int {
        return if (AppThemeManager.isDarkModeEnabled(this)) {
            R.drawable.ic_divider_dark
        } else {
            R.drawable.ic_divider
        }
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

    private fun setupShelterFilterChips() {
        filterBarrierFreeChip.setOnClickListener { toggleBarrierFreeFilter() }
        updateShelterFilterChipStyle()
    }

    private fun setupShelterSearch() {
        searchShelterInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isApplyingSearchSuggestion) return
                updateSearchSuggestions(s?.toString().orEmpty())
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })

        searchShelterInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = searchShelterInput.text?.toString().orEmpty().trim()
                val matchedShelter = findAddressMatchedShelters(query).firstOrNull()
                if (matchedShelter != null) {
                    selectSearchSuggestion(matchedShelter)
                } else {
                    Toast.makeText(this, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show()
                }
                true
            } else {
                false
            }
        }
    }

    private fun updateSearchSuggestions(rawQuery: String) {
        val query = rawQuery.trim()
        searchSuggestionsContainer.removeAllViews()

        if (query.isBlank()) {
            searchSuggestionsContainer.visibility = View.GONE
            return
        }

        val suggestions = findAddressMatchedShelters(query).take(5)
        if (suggestions.isEmpty()) {
            searchSuggestionsContainer.visibility = View.GONE
            return
        }

        suggestions.forEachIndexed { index, shelter ->
            searchSuggestionsContainer.addView(createSearchSuggestionRow(shelter))

            if (index != suggestions.lastIndex) {
                searchSuggestionsContainer.addView(View(this).apply {
                    setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.sr_separator))
                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)))
            }
        }

        searchSuggestionsContainer.visibility = View.VISIBLE
    }

    private fun findAddressMatchedShelters(query: String): List<ShelterPin> {
        if (query.isBlank()) return emptyList()
        val normalizedQuery = query.lowercase()

        return shelterPins
            .filter { shelter -> shelter.address.lowercase().contains(normalizedQuery) }
            .sortedWith(
                compareBy<ShelterPin> { shelter ->
                    shelter.address.lowercase().indexOf(normalizedQuery).let { if (it < 0) Int.MAX_VALUE else it }
                }.thenBy { shelter -> shelter.address.length }
            )
    }

    private fun createSearchSuggestionRow(shelter: ShelterPin): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(14), dpToPx(10), dpToPx(14), dpToPx(10))
            isClickable = true
            isFocusable = true
            setOnClickListener { selectSearchSuggestion(shelter) }

            addView(TextView(this@MainActivity).apply {
                text = shelter.address
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.sr_text_primary))
                textSize = 14f
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            })

            addView(TextView(this@MainActivity).apply {
                text = shelter.name
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.sr_text_muted))
                textSize = 12f
                setPadding(0, dpToPx(3), 0, 0)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            })
        }
    }

    private fun selectSearchSuggestion(shelter: ShelterPin) {
        isApplyingSearchSuggestion = true
        searchShelterInput.setText(shelter.address)
        searchShelterInput.setSelection(searchShelterInput.text?.length ?: 0)
        isApplyingSearchSuggestion = false
        searchSuggestionsContainer.visibility = View.GONE
        hideKeyboard()
        moveMapTo(shelter.point.latitude, shelter.point.longitude, 16, false)
        showSelectedShelterRoute(shelter)
    }

    private fun hideKeyboard() {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(searchShelterInput.windowToken, 0)
        searchShelterInput.clearFocus()
    }

    private fun toggleBarrierFreeFilter() {
        barrierFreeOnly = !barrierFreeOnly
        updateShelterFilterChipStyle()
        refreshShelterMarkers()

        val selected = selectedShelter
        if (selected != null && !shelterMatchesActiveFilters(selected)) {
            clearSelectedRoute()
        }
    }

    private fun clearSelectedRoute() {
        selectedShelter = null
        routeInfoCard.visibility = View.GONE
        hideRouteBarrierIcons()
        tMapView?.removeTMapPolyLine(SHELTER_ROUTE_LINE_ID)
        lastRouteShelter = null
        lastRouteSummaryText = null
        selectedRouteSummaries.clear()
        selectedRouteRequestsFinished.clear()
    }

    private fun updateShelterFilterChipStyle() {
        val selectedTextColor = ContextCompat.getColor(this, android.R.color.white)
        val normalTextColor = ContextCompat.getColor(this, R.color.sr_text_primary)
        val barrierFreeIcon = ContextCompat.getDrawable(this, R.drawable.ic_barrier_free_24)
        barrierFreeIcon?.setTint(if (barrierFreeOnly) selectedTextColor else normalTextColor)

        filterBarrierFreeChip.setBackgroundResource(
            if (barrierFreeOnly) R.drawable.bg_chip_blue else R.drawable.bg_chip_light
        )
        filterBarrierFreeChip.setTextColor(if (barrierFreeOnly) selectedTextColor else normalTextColor)
        filterBarrierFreeChip.setCompoundDrawablesWithIntrinsicBounds(barrierFreeIcon, null, null, null)
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
                    setCalloutSubTitle(shelter.address)
                    canShowCallout = true
                    autoCallloutVisible = true
                }
                mapView.addTMapMarkerItem(markerItem)
            }
        }
    }

    private fun shelterMatchesActiveFilters(shelter: ShelterPin): Boolean {
        return !barrierFreeOnly || shelter.barrierFree
    }

    private fun renderHomeShelters() {
        val basePoint = currentTMapPoint ?: TMapPoint(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
        homeShelterBinder.render(
            shelters = shelterPins,
            basePoint = basePoint,
            routeDistances = homeRouteDistances,
            onRouteShortcutClick = ::openShelterRouteFromHome
        )
    }

    private fun showNearbySheltersDialog() {
        val basePoint = currentTMapPoint ?: TMapPoint(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
        val nearestShelters = shelterPins
            .map { shelter ->
                val distance = homeRouteDistances[shelter.markerId]
                    ?: distanceBetween(basePoint, shelter.point).toDouble()
                shelter to distance
            }
            .sortedBy { (_, distance) -> distance }
            .take(10)

        if (nearestShelters.isEmpty()) {
            Toast.makeText(this, "표시할 대피소가 없습니다", Toast.LENGTH_SHORT).show()
            return
        }

        val listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(4), dpToPx(8), dpToPx(4), dpToPx(8))
        }

        var dialog: AlertDialog? = null
        nearestShelters.forEachIndexed { index, (shelter, distanceMeters) ->
            listContainer.addView(createNearbyShelterDialogRow(index + 1, shelter, distanceMeters) {
                dialog?.dismiss()
                openShelterRouteFromHome(shelter)
            })

            if (index != nearestShelters.lastIndex) {
                listContainer.addView(View(this).apply {
                    setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.sr_separator))
                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)))
            }
        }

        val scrollView = ScrollView(this).apply {
            addView(listContainer)
        }

        dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.nearby_shelters_top_10))
            .setView(scrollView)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        dialog.show()
    }

    private fun createNearbyShelterDialogRow(
        rank: Int,
        shelter: ShelterPin,
        distanceMeters: Double,
        onClick: () -> Unit
    ): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }

        val textColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val nameView = TextView(this).apply {
            text = "$rank. ${shelter.name}"
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.sr_text_primary))
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        val detailView = TextView(this).apply {
            text = barrierFacilityText(shelter)
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.sr_text_muted))
            textSize = 13f
            setPadding(0, dpToPx(4), 0, 0)
        }

        val distanceView = TextView(this).apply {
            text = formatDistance(distanceMeters)
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.sr_text_muted))
            textSize = 12f
            setPadding(0, dpToPx(3), 0, 0)
        }

        textColumn.addView(nameView)
        createBarrierIconStrip(shelter, iconSizeDp = 18, dividerSizeDp = 14)?.let { iconRow ->
            textColumn.addView(iconRow)
        }
        textColumn.addView(detailView)
        textColumn.addView(distanceView)
        row.addView(
            textColumn,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )

        row.addView(TextView(this).apply {
            text = getString(R.string.route_shortcut)
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.sr_blue))
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })

        return row
    }

    private fun buildNearbyShelterSummary(shelter: ShelterPin): String {
        return barrierFacilityText(shelter)
    }

    private fun openShelterRouteFromHome(shelter: ShelterPin) {
        bottomNav.selectedItemId = R.id.tab_map
        mapContainer.post {
            showSelectedShelterRoute(shelter)
            moveMapTo(shelter.point.latitude, shelter.point.longitude, 15, true)
        }
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
            updateRouteBarrierIcons(shelter)
            routeInfoDetail.text = buildRouteDetailText(shelter)
            routeInfoDistance.text = "현재 위치를 확인하는 중입니다"
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

        updateRouteBarrierIcons(shelter)
        routeInfoDetail.text = buildRouteDetailText(shelter)
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
                                fitRouteToScreen(routeResult.points, startPoint, shelter.point)
                            }
                        }

                        updateRouteSummaryCard(shelter)
                    }
                }
            }
        )
    }
    private fun updateRouteBarrierIcons(shelter: ShelterPin?) {
        if (shelter == null) {
            hideRouteBarrierIcons()
            return
        }

        val state = getBarrierFacilityState(shelter)
        if (!state.hasAny) {
            hideRouteBarrierIcons()
            return
        }

        routeInfoBarrierScroll.visibility = View.VISIBLE
        routeInfoBarrierFreeIcon.visibility = View.VISIBLE
        routeInfoDividerIcon.setImageResource(barrierDividerDrawableRes())
        routeInfoDividerIcon.visibility = View.VISIBLE
        routeInfoElevatorIcon.visibility = if (state.hasElevator) View.VISIBLE else View.GONE
        routeInfoParkingIcon.visibility = if (state.hasParking) View.VISIBLE else View.GONE
        routeInfoToiletIcon.visibility = if (state.hasToilet) View.VISIBLE else View.GONE
        routeInfoEntranceIcon.visibility = if (state.hasEntrance) View.VISIBLE else View.GONE
    }

    private fun hideRouteBarrierIcons() {
        routeInfoBarrierScroll.visibility = View.GONE
        routeInfoBarrierFreeIcon.visibility = View.GONE
        routeInfoDividerIcon.visibility = View.GONE
        routeInfoElevatorIcon.visibility = View.GONE
        routeInfoParkingIcon.visibility = View.GONE
        routeInfoToiletIcon.visibility = View.GONE
        routeInfoEntranceIcon.visibility = View.GONE
    }

    private fun createBarrierIconStrip(
        shelter: ShelterPin,
        iconSizeDp: Int,
        dividerSizeDp: Int
    ): View? {
        val state = getBarrierFacilityState(shelter)
        if (!state.hasAny) return null

        val scrollView = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            setPadding(0, dpToPx(6), 0, 0)
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        fun addIcon(drawableRes: Int, sizeDp: Int, marginEndDp: Int = 6) {
            row.addView(
                ImageView(this).apply {
                    setImageResource(drawableRes)
                    adjustViewBounds = true
                },
                LinearLayout.LayoutParams(dpToPx(sizeDp), dpToPx(sizeDp)).apply {
                    marginEnd = dpToPx(marginEndDp)
                }
            )
        }

        addIcon(R.drawable.ic_barrierfree, iconSizeDp, marginEndDp = 3)
        addIcon(barrierDividerDrawableRes(), dividerSizeDp, marginEndDp = 3)
        if (state.hasElevator) addIcon(R.drawable.ic_barrier_elevator, iconSizeDp)
        if (state.hasParking) addIcon(R.drawable.ic_barrier_parking, iconSizeDp)
        if (state.hasToilet) addIcon(R.drawable.ic_barrier_toilet, iconSizeDp)
        if (state.hasEntrance) addIcon(R.drawable.ic_barrier_entrance, iconSizeDp, marginEndDp = 0)

        scrollView.addView(row)
        return scrollView
    }

    private fun getBarrierFacilityState(shelter: ShelterPin): BarrierFacilityState {
        val tags = shelter.evalInfo
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        val hasElevator = tags.contains("승강기")
        val hasParking = tags.contains("장애인전용주차구역")
        val hasToilet = tags.contains("장애인사용가능화장실")
        val hasEntrance = entranceFacilityLabels(tags).isNotEmpty()

        return BarrierFacilityState(
            hasElevator = hasElevator,
            hasParking = hasParking,
            hasToilet = hasToilet,
            hasEntrance = hasEntrance,
            hasAny = shelter.barrierFree && (hasElevator || hasParking || hasToilet || hasEntrance)
        )
    }

    private fun barrierFacilityText(shelter: ShelterPin): String {
        if (!shelter.barrierFree) {
            return getString(R.string.barrier_free_facility_none)
        }

        val tags = shelter.evalInfo
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        val labels = mutableListOf<String>()
        if (tags.contains("승강기")) labels.add("승강기")
        if (tags.contains("장애인전용주차구역")) labels.add("장애인전용주차구역")
        if (tags.contains("장애인사용가능화장실")) labels.add("장애인사용가능화장실")
        labels.addAll(entranceFacilityLabels(tags))

        return labels.ifEmpty {
            listOf(getString(R.string.barrier_free_facility_none))
        }.joinToString(", ")
    }

    private fun entranceFacilityLabels(tags: List<String>): List<String> {
        return listOf(
            "주출입구 높이차이 제거",
            "주출입구 접근로",
            "주출입구(문)"
        ).filter { tags.contains(it) }
    }

    private fun updateRouteSummaryCard(shelter: ShelterPin) {
        val carRoute = selectedRouteSummaries[TMapData.TMapPathType.CAR_PATH]
        val pedestrianRoute = selectedRouteSummaries[TMapData.TMapPathType.PEDESTRIAN_PATH]

        if (carRoute == null && pedestrianRoute == null) {
            if (selectedRouteRequestsFinished.size >= 2) {
                showRouteFailure()
            } else {
                updateRouteBarrierIcons(shelter)
                routeInfoDetail.text = buildRouteDetailText(shelter)
                routeInfoDistance.text = "자동차 및 보행 경로 계산 중"
            }
            return
        }

        updateRouteBarrierIcons(shelter)
        routeInfoDetail.text = buildRouteDetailText(shelter)

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
            updateRouteBarrierIcons(shelter)
            routeInfoDetail.text = barrierFacilityText(shelter)
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

    private fun fitRouteToScreen(
        points: ArrayList<TMapPoint>,
        startPoint: TMapPoint,
        destinationPoint: TMapPoint
    ) {
        val mapView = tMapView ?: return
        if (points.isEmpty()) return

        val boundsPoints = ArrayList<TMapPoint>(points.size + 2).apply {
            add(startPoint)
            addAll(points)
            add(destinationPoint)
        }

        try {
            mapView.fitBounds(
                mapView.getBoundsFromPoints(boundsPoints),
                TMapInsets.of(dpToPx(24), dpToPx(120), dpToPx(24), dpToPx(190))
            )
        } catch (_: RuntimeException) {
            val shelter = selectedShelter ?: return
            mapView.zoomToTMapPoint(currentTMapPoint ?: shelter.point, shelter.point)
        }
    }

    private fun moveMapTo(latitude: Double, longitude: Double, zoom: Int, animate: Boolean) {
        val mapView = tMapView ?: return
        mapView.setCenterPoint(latitude, longitude, animate)
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

    private fun buildRouteDetailText(shelter: ShelterPin): CharSequence {
        return "${shelter.address}\n${barrierFacilityText(shelter)}"
    }

    private fun appendBarrierFreeFacilities(
        builder: SpannableStringBuilder,
        shelter: ShelterPin,
        useCompactEntranceLabel: Boolean
    ) {
        builder.append(barrierFacilityText(shelter))
    }

    private fun appendFacilityWithIcon(
        builder: SpannableStringBuilder,
        drawableRes: Int,
        text: String,
        addComma: Boolean
    ) {
        if (addComma) builder.append(", ")
        builder.append(text)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
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

        if (shouldRefreshHomeAlertOnResume) {
            homeAlertBinder.refresh(sendTestNotification = false)
        } else {
            shouldRefreshHomeAlertOnResume = true
        }

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

    private data class BarrierFacilityState(
        val hasElevator: Boolean,
        val hasParking: Boolean,
        val hasToilet: Boolean,
        val hasEntrance: Boolean,
        val hasAny: Boolean
    )

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
