package com.example.mapssample

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.maps.android.PolyUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        private const val REQUEST_CODE_LOCATION = 11
        private const val DISTANCE_MODE = "driving"
    }

    private val api: Api = NetworkBuilder.create()

    private lateinit var map: GoogleMap

    private lateinit var fusedLocationProvider: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient

    private var currentLocation: Location? = null
    private var polyline: Polyline? = null
    private var destinationMarker: Marker? = null
    private var disposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationProvider = LocationServices.getFusedLocationProviderClient(this)
        placesClient = Places.createClient(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        trackMyLocation()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.firstOrNull() == PackageManager.PERMISSION_DENIED) {
            showPermissionDialog()
            return
        }

        trackMyLocation()
    }

    private fun showPermissionDialog() {
        AlertDialog.Builder(this)
            .setPositiveButton("Настройки") { _, _ ->
                val intent = Intent(ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Закрыть") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setTitle("Необходимо дать разрешение на локацию")
            .create()
            .show()
    }

    private fun trackMyLocation() {
        if (!isLocationGranted()) return

        fusedLocationProvider.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLocation = location
                val latLng = LatLng(location.latitude, location.longitude)
                val options = MarkerOptions().position(latLng)
                map.addMarker(options)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12F))
            }
        }

        map.setOnMapLongClickListener { destination ->
            if (currentLocation == null) return@setOnMapLongClickListener

            fetchPlaceData(
                LatLng(currentLocation!!.latitude, currentLocation!!.longitude),
                destination
            )
        }

        map.setOnMarkerClickListener {
            it.showInfoWindow()
            return@setOnMarkerClickListener false
        }
    }

    private fun insertMarker(
        destination: LatLng,
        bitmap: Bitmap?
    ) {
        destinationMarker?.remove()
        destinationMarker = null

        val latLng = LatLng(destination.latitude, destination.longitude)
        val options = MarkerOptions()
            .position(latLng)
            .title("Your destination!")

        if (bitmap != null) {
            val rounded = ImageUtils.getRoundedCornerBitmap(bitmap)
            val result = BitmapDescriptorFactory.fromBitmap(rounded)

            rounded.recycle()
            bitmap.recycle()

            options.icon(result)
        } else {
            val drawable =
                resources.getDrawable(R.drawable.ic_location_city_black_24dp)
            val default = BitmapUtils.drawableToBitmap(drawable)
            options.icon(BitmapDescriptorFactory.fromBitmap(default))
        }

        destinationMarker = map.addMarker(options)
    }

    private fun insertPolyline(direction: Direction) {
        polyline?.remove()
        polyline = null

        val route = direction.routes.firstOrNull() ?: return
        val decoded = PolyUtil.decode(route.polyline.points)
        val options = PolylineOptions().addAll(decoded)
        polyline = map.addPolyline(options)
    }

    private fun showDistance(direction: Direction) {
        val route = direction.routes.firstOrNull() ?: return
        val leg = route.legs.firstOrNull() ?: return
        showToast("Distance: ${leg.distance.text}")
    }

    private fun isLocationGranted(): Boolean {
        val permissionResult =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val isGranted = permissionResult == PackageManager.PERMISSION_GRANTED

        if (!isGranted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_CODE_LOCATION
            )
        }

        return isGranted
    }

    private fun fetchPlaceData(origin: LatLng, destination: LatLng) {
        val strOrigin = "${origin.latitude}, ${origin.longitude}"
        val strDestination = "${destination.latitude}, ${destination.longitude}"
        val key = getString(R.string.google_maps_key)

        disposable = api.getPolyline(strOrigin, strDestination, false, DISTANCE_MODE, key)
            .zipWith(api.getGeocode(key, "${destination.latitude},${destination.longitude}"))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { (direction, geocode) ->
                    showDistance(direction)
                    insertPolyline(direction)
                    insertMarker(destination, null)
                    executePlaceRequest(destination, geocode)
                },
                onError = { showToast("Error getting place data") }
            )
    }

    private fun executePlaceRequest(destination: LatLng, geocode: Geocode) {
        val placeId = geocode.results.firstOrNull()?.placeId ?: return
        val fields = listOf(Place.Field.PHOTO_METADATAS)
        val request = FetchPlaceRequest.newInstance(placeId, fields)

        placesClient.fetchPlace(request).addOnSuccessListener { response ->
            val place = response.place
            val metadata = place.photoMetadatas?.firstOrNull()

            if (metadata == null) {
                Log.e("Photo", "No photo found for this place")
                return@addOnSuccessListener
            }

            val photoRequest = FetchPhotoRequest.builder(metadata)
                .setMaxWidth(100)
                .setMaxHeight(100)
                .build()

            placesClient.fetchPhoto(photoRequest)
                .addOnSuccessListener { photoResponse ->
                    val bitmap = photoResponse.bitmap
                    insertMarker(destination, bitmap)
                }
                .addOnFailureListener { e ->
                    Log.e("Photo", "Place not found", e)
                }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
