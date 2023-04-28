package com.dwh.userubication

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LocationViewModel@Inject constructor(
): ViewModel() {

    private val _requestingLocationUpdates = mutableStateOf(false)
    val requestingLocationUpdates = _requestingLocationUpdates

    fun setRequestingLocationUpdates(value: Boolean) {
        _requestingLocationUpdates.value = value
    }

    var locationCallback: LocationCallback? = null

    var fusedLocationClient: FusedLocationProviderClient? = null

    private val _latitudeUser = mutableStateOf(0.0)
    val latitudeUser = _latitudeUser

    fun setLatitude(value: Double) {
        _latitudeUser.value = value
    }

    private val _longitudeUser = mutableStateOf(0.0)
    val longitudeUser = _longitudeUser

    fun setLongitude(value: Double) {
        _longitudeUser.value = value
    }
}