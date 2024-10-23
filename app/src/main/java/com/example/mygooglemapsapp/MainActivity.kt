package com.example.mygooglemapsapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import java.io.IOException
import java.util.Locale

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var currentMapType = GoogleMap.MAP_TYPE_NORMAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Obtener el fragmento del mapa y configurar el callback
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Configuración de campos y botones
        val editTextSearch = findViewById<EditText>(R.id.editTextSearch)
        val btnSearch = findViewById<Button>(R.id.btnSearch)
        val editTextSearchOrigin = findViewById<EditText>(R.id.editTextSearchOrigin)
        val editTextSearchDestination = findViewById<EditText>(R.id.editTextSearchDestination)
        val btnSearchRoute = findViewById<Button>(R.id.btnSearchRoute)
        val btnChangeMapType = findViewById<Button>(R.id.btnChangeMapType)

        // Inicialmente, solo mostrar el campo de búsqueda de una ubicación
        editTextSearchOrigin.visibility = View.GONE
        editTextSearchDestination.visibility = View.GONE
        btnSearchRoute.visibility = View.GONE

        // Configuración del botón para buscar una ubicación
        btnSearch.setOnClickListener {
            searchPlace(editTextSearch.text.toString())
            // Mostrar los campos de origen y destino después de la búsqueda inicial
            editTextSearch.visibility = View.GONE
            btnSearch.visibility = View.GONE
            editTextSearchOrigin.visibility = View.VISIBLE
            editTextSearchDestination.visibility = View.VISIBLE
            btnSearchRoute.visibility = View.VISIBLE
        }

        // Configuración del botón para buscar una ruta
        btnSearchRoute.setOnClickListener {
            searchRoute(editTextSearchOrigin.text.toString(), editTextSearchDestination.text.toString())
        }

        // Botón para cambiar el tipo de mapa
        btnChangeMapType.setOnClickListener {
            changeMapType()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Configuraciones iniciales del mapa
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 12f))

        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isCompassEnabled = true

        enableMyLocation()
        centerMapOnMyLocation()
    }

    private fun searchPlace(query: String) {
        if (query.isNotEmpty()) {
            val geocoder = Geocoder(this)
            try {
                val addresses = geocoder.getFromLocationName(query, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    val address = addresses[0]
                    val location = LatLng(address.latitude, address.longitude)

                    mMap.clear() // Limpiar los marcadores anteriores
                    mMap.addMarker(MarkerOptions().position(location).title(address.getAddressLine(0)))
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
                } else {
                    Toast.makeText(this, "No se encontró el lugar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Error al buscar la ubicación", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Por favor, ingrese un lugar para buscar", Toast.LENGTH_SHORT).show()
        }
    }

    private fun searchRoute(origin: String, destination: String) {
        val geocoder = Geocoder(this, Locale.getDefault())

        if (origin.isNotEmpty() && destination.isNotEmpty()) {
            try {
                val originAddress = geocoder.getFromLocationName(origin, 1)
                val destinationAddress = geocoder.getFromLocationName(destination, 1)

                if (!originAddress.isNullOrEmpty() && !destinationAddress.isNullOrEmpty()) {
                    val originLocation = LatLng(originAddress[0].latitude, originAddress[0].longitude)
                    val destinationLocation = LatLng(destinationAddress[0].latitude, destinationAddress[0].longitude)

                    mMap.addMarker(MarkerOptions().position(originLocation).title("Origen: $origin"))
                    mMap.addMarker(MarkerOptions().position(destinationLocation).title("Destino: $destination"))

                    drawRoute(originLocation, destinationLocation)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(originLocation, 10f))
                } else {
                    Toast.makeText(this, "No se encontraron coordenadas para uno de los lugares", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error al buscar la ruta: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Por favor, ingrese un origen y un destino", Toast.LENGTH_SHORT).show()
        }
    }

    private fun drawRoute(origin: LatLng, destination: LatLng) {
        val options = PolylineOptions()
            .add(origin)
            .add(destination)
            .color(android.graphics.Color.BLUE)
            .width(10f)
        mMap.addPolyline(options)
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == 1 && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation()
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun changeMapType() {
        if (::mMap.isInitialized) {
            currentMapType = (currentMapType + 1) % 4
            when (currentMapType) {
                0 -> mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                1 -> mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
                2 -> mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
                3 -> mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
            }
        }
    }

    private fun centerMapOnMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
            mMap.setOnMyLocationButtonClickListener {
                val locationManager = getSystemService(LOCATION_SERVICE) as android.location.LocationManager
                val location = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                location?.let {
                    val myLocation = LatLng(it.latitude, it.longitude)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 15f))
                }
                true
            }
        }
    }
}


