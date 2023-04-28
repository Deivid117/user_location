package com.dwh.userubication

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.accompanist.permissions.*
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun LocationScreen(viewModel: LocationViewModel = hiltViewModel()){
    Surface(Modifier.fillMaxSize()) {
        LocationContent(viewModel)
    }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun LocationContent(viewModel: LocationViewModel) {

    val context = LocalContext.current
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        scaffoldState = scaffoldState
    ){
        UserLocation(context, viewModel, scaffoldState, coroutineScope)
    }
}

/* Realiza solicitudes o actualizaciones de la ubicación del usuario
*  cada 3 segundos */
@SuppressLint("MissingPermission")
fun startLocationUpdates(viewModel: LocationViewModel) {

    val fusedLocationClient = viewModel.fusedLocationClient
    val locationCallback = viewModel.locationCallback

    locationCallback.let { callback ->
        /*LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }*/
       /* val request = LocationRequest()
        request.setInterval(1 * 60 * 1000)
        request.setFastestInterval(2 * 60 * 1000)
        request.priority = LocationRequest.PRIORITY_HIGH_ACCURACY*/
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000).build()
        callback?.let { it ->
            fusedLocationClient?.requestLocationUpdates(locationRequest, it, Looper.getMainLooper())
        }
    }
}

@Composable
fun UserLocation(
    context: Context,
    viewModel: LocationViewModel,
    scaffoldState: ScaffoldState,
    coroutineScope: CoroutineScope
) {

    val latitude by remember { viewModel.latitudeUser }
    val longitude by remember { viewModel.longitudeUser }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Tus coordenadas son: \nLongitud $latitude \nLatitud $longitude")

        Spacer(modifier = Modifier.height(10.dp))

        CheckPermissions(context, viewModel, scaffoldState, coroutineScope)

        Spacer(modifier = Modifier.height(20.dp))

        UserLocationMap(viewModel)
    }
}

@Composable
fun UserLocationMap(
    viewModel: LocationViewModel
) {
    var latitude by remember { mutableStateOf(0.0) }
    var longitude by remember { mutableStateOf(0.0) }
    val userPosition = LatLng(latitude, longitude)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userPosition, 5f)
    }

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            latitude =  viewModel.latitudeUser.value
            longitude = viewModel.longitudeUser.value
        }) {
            Text(text = "Mostrar mi ubicación")
        }
        Spacer(modifier = Modifier.height(10.dp))
        GoogleMap(
            Modifier
                .fillMaxWidth()
                .height(250.dp),
            cameraPositionState = cameraPositionState
        ) {
            Marker(
                state = MarkerState(position = userPosition),
                title = "Mi ubicación",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)
            )
        }
    }
}

/* Valida si se tiene permisos de ubicación precisa o no
*  en caso de no tenerlos los solicita */
@Composable
fun CheckPermissions(
    context: Context,
    viewModel: LocationViewModel,
    scaffoldState: ScaffoldState,
    coroutineScope: CoroutineScope
) {

    var locationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcherPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                locationPermission = isGranted
            } else {
                coroutineScope.launch {
                    // Snackbar de aviso al usuario
                    scaffoldState.snackbarHostState.showSnackbar(
                        message = "Debes aceptar el permiso para obtener tus coordenadas",
                        actionLabel = "Ok",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    )

    if(locationPermission) GetLastUserLocation(context, viewModel)

    Column() {
        Button(onClick = {
            if(locationPermission){
                viewModel.setRequestingLocationUpdates(true)
                startLocationUpdates(viewModel)
            } else {
                launcherPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }) {
            Text(text = "Obtener ubicación")
        }
    }
}

/* Obtiene la ubicación más reciente del usuario
*  Dependiendo del ciclo de vida solicitará o detendrá
*  las actualizaciones de la ubicación */
@Composable
fun GetLastUserLocation(context: Context, viewModel: LocationViewModel) {

    val requestingLocationUpdates by remember { viewModel.requestingLocationUpdates }
    val fusedLocationClient = remember { viewModel.fusedLocationClient }
    val locationCallback = remember { viewModel.locationCallback }
    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

    DisposableEffect(lifecycleOwner) {
        viewModel.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        // Devuelve un listado de todas las coordenadas que vaya obteniendo
        viewModel.locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location  in locationResult.locations) {
                    // Update UI with location data
                    viewModel.setLatitude(location.latitude)
                    viewModel.setLongitude(location.longitude)
                    Log.w("Ubicación", location.latitude.toString() + " " + location.longitude.toString())
                }
            }
        }
        val observer = LifecycleEventObserver { owner, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    locationCallback?.let { fusedLocationClient?.removeLocationUpdates(it) }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (requestingLocationUpdates) startLocationUpdates(viewModel)
                }
                Lifecycle.Event.ON_STOP -> {
                    locationCallback?.let { fusedLocationClient?.removeLocationUpdates(it) }
                }
                else -> {  }
            }
        }
        val lifecycle = lifecycleOwner.value.lifecycle
        lifecycle.addObserver(observer)
        onDispose {  lifecycle.removeObserver(observer) }
    }
}