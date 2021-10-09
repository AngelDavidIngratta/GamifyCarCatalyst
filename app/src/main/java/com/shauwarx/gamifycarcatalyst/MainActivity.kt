package com.shauwarx.gamifycarcatalyst

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*

const val FAST_UPDATE_INTERVAL: Long = 5
const val DEFAULT_UPDATE_INTERVAL: Long = 30

const val TAG: String = "MainActivity"

class MainActivity : AppCompatActivity() {

    private lateinit var tv_latitude : TextView
    private lateinit var tv_longitude : TextView
    private lateinit var tv_altitude : TextView
    private lateinit var tv_accuracy : TextView
    private lateinit var tv_speed : TextView
    private lateinit var tv_sensor : TextView
    private lateinit var tv_updates : TextView
    private lateinit var tv_address : TextView

    private lateinit var sw_locationUpdates: Switch
    private lateinit var sw_gps: Switch

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private lateinit var locationRequest: LocationRequest

    private lateinit var locationCallback: LocationCallback

    private val requestPermissionLauncher =
        registerForActivityResult(
            RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
                updateGPS()
            } else {
                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied.
                Toast.makeText(this, getString(R.string.FeatureUnavailableBecauseNoPermissionGranted), Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tv_latitude = findViewById(R.id.tv_lat)
        tv_longitude = findViewById(R.id.tv_lon)
        tv_altitude = findViewById(R.id.tv_altitude)
        tv_accuracy = findViewById(R.id.tv_accuracy)
        tv_speed = findViewById(R.id.tv_speed)
        tv_sensor = findViewById(R.id.tv_sensor)
        tv_updates = findViewById(R.id.tv_updates)
        tv_address = findViewById(R.id.tv_address)

        sw_gps = findViewById(R.id.sw_gps)
        sw_locationUpdates = findViewById(R.id.sw_locationsupdates)

        locationRequest = LocationRequest.create()
        locationRequest.interval = 1000 * DEFAULT_UPDATE_INTERVAL
        locationRequest.fastestInterval = 1000 * FAST_UPDATE_INTERVAL

        locationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations){
                    Log.d(TAG, "On Location Result : ${location.toString()}")
                    updateUIValues(location)
                }
            }
        }

        sw_gps.setOnClickListener{
            if(sw_gps.isChecked) {
                locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                tv_sensor.text = getString(R.string.Sw_GPS_Checked_Text)
            } else {
                locationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
                tv_sensor.text = getString(R.string.Sw_GPS_Unchecked_Text)
            }
        }

        sw_locationUpdates.setOnClickListener{
            if (sw_locationUpdates.isChecked)
                startLocationUpdates()
            else
                stopLocationUpdates()
        }

        updateGPS()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        tv_updates.text = getString(R.string.TrakingEnabled)
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper() )
    }

    private fun stopLocationUpdates() {
        tv_updates.text = getString(R.string.TrackingDisabled)

        fusedLocationProviderClient.removeLocationUpdates(locationCallback)

        tv_latitude.text = getString(R.string.TrackingDisabled)
        tv_longitude.text = getString(R.string.TrackingDisabled)
        tv_accuracy.text = getString(R.string.TrackingDisabled)
        tv_altitude.text = getString(R.string.TrackingDisabled)
        tv_speed.text = getString(R.string.TrackingDisabled)
        tv_address.text = getString(R.string.TrackingDisabled)
    }


    private fun updateGPS() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        when {
            // The permission is already Granted
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                Toast.makeText(this, getString(R.string.PermissionAlreadyGranted), Toast.LENGTH_SHORT).show()
                // You can use the API that requires the permission.
                fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                    if(location != null) {
                        Log.d(TAG, "Last Location : ${location.toString()}")
                        updateUIValues(location)
                    }
                }
            }
            // You need to request permission
            shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                Toast.makeText(this, getString(R.string.PermissionAlreadyGranted), Toast.LENGTH_SHORT).show()
                requestPermissionLauncher.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION)
            }
            // the permission has not been asked yet
            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                requestPermissionLauncher.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun updateUIValues(location: Location) {
        tv_latitude.text = location.latitude.toString()
        tv_longitude.text = location.longitude.toString()
        tv_accuracy.text = location.accuracy.toString()
        if (location.hasAltitude())
            tv_altitude.text = location.altitude.toString()
        else
            tv_altitude.text = getString(R.string.NotAvailable)
        if (location.hasSpeed())
            tv_speed.text = location.speed.toString()
        else
            tv_speed.text = getString(R.string.NotAvailable)

        val geocoder : Geocoder =  Geocoder(this)
        try {
             val addresses: List<Address> = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            tv_address.text = addresses[0].getAddressLine(0)
        } catch (e: Exception ) {
            Log.e(TAG, "Last Location : ${e.stackTrace}")
        }

    }
}