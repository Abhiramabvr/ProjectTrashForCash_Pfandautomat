package com.bvr.projectjtcm.ui.user

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bvr.projectjtcm.ui.user.QrDisplayActivity
import com.bvr.projectjtcm.data.WasteData
import com.bvr.projectjtcm.databinding.ActivityOrderPickupBinding
import com.google.firebase.database.FirebaseDatabase
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.OnMapReadyCallback
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class OrderPickupActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityOrderPickupBinding
    private lateinit var mapLibreMap: MapLibreMap

    private var wasteType = ""
    private var wasteWeight = 0
    private var wasteIncome = 0.0
    private var existingId: String? = null

    private var selectedLocation = "Belum dipilih"
    private var selectedPickupDate = ""
    private var selectedPickupTime = ""

    // Data Bank Sampah di Yogyakarta
    private val wasteBanks = listOf(
        Pair("Bank Sampah Gemah Ripah", LatLng(-7.8211, 110.3236)),
        Pair("Bank Sampah Brama", LatLng(-7.7550, 110.3967)),
        Pair("Bank Sampah Waras", LatLng(-7.7925, 110.3660)),
        Pair("Bank Sampah Sukunan", LatLng(-7.7634, 110.3444)),
        Pair("Bank Sampah Budi Luhur", LatLng(-7.8100, 110.3800))
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        try {
            MapLibre.getInstance(this)
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal memuat MapLibre: ${e.message}", Toast.LENGTH_LONG).show()
        }

        binding = ActivityOrderPickupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            val titleParams = binding.tvTitle.layoutParams as ViewGroup.MarginLayoutParams
            titleParams.topMargin = systemBars.top + (20 * resources.displayMetrics.density).toInt()
            binding.tvTitle.layoutParams = titleParams

            binding.cardDetails.setPadding(0, 0, 0, systemBars.bottom)

            insets
        }

        wasteType = intent.getStringExtra("TYPE") ?: "General"
        wasteWeight = intent.getIntExtra("WEIGHT", 0)
        wasteIncome = intent.getDoubleExtra("INCOME", 0.0)
        existingId = intent.getStringExtra("EXISTING_ID")

        binding.tvTitle.text = if (existingId != null) "Lanjutkan Order" else "Drop Off Location"
        binding.tvAddressValue.text = "Pilih lokasi di peta"

        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnCancel.setOnClickListener { finish() }

        binding.btnOrder.setOnClickListener {
            if (selectedLocation == "Belum dipilih") {
                Toast.makeText(this, "⚠️ Pilih lokasi bank sampah dulu!", Toast.LENGTH_SHORT).show()
            } else if (selectedPickupDate.isEmpty() || selectedPickupTime.isEmpty()) {
                Toast.makeText(this, "⚠️ Pilih tanggal dan jam drop off!", Toast.LENGTH_SHORT).show()
            } else {
                saveToFirebase("Ordered")
            }
        }

        binding.layoutAddress.setOnClickListener {
            Toast.makeText(this, "Klik marker hijau di peta untuk memilih lokasi", Toast.LENGTH_SHORT).show()
        }

        binding.layoutDay.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                selectedPickupDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                binding.tvDayValue.text = selectedPickupDate
            }, year, month, day).show()
        }

        binding.layoutTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                selectedPickupTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                binding.tvTimeValue.text = selectedPickupTime
            }, hour, minute, true).show()
        }
    }

    private fun saveToFirebase(status: String) {
         try {
            val database = FirebaseDatabase.getInstance().getReference("waste_history")

            val id = existingId ?: database.push().key
            val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            val month = SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())

            val wasteData = WasteData(
                id,
                wasteType,
                wasteWeight,
                wasteIncome,
                status,
                date,
                month,
                selectedLocation,
                selectedPickupDate,
                selectedPickupTime
            )

            if (id != null) {
                database.child(id).setValue(wasteData).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Buka Halaman QR setelah berhasil menyimpan
                        val intent = Intent(this, QrDisplayActivity::class.java)
                        intent.putExtra("ORDER_ID", id)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "❌ Gagal menyimpan order: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error Database: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onMapReady(map: MapLibreMap) {
        this.mapLibreMap = map
        val styleUrl = "https://api.maptiler.com/maps/streets-v2/style.json?key=DhaZkQmsFvw7N7WHe394"

        map.setStyle(styleUrl) { style ->
            // Pindahkan fokus kamera ke Yogyakarta
            val yogyakarta = LatLng(-7.7925, 110.3660)
            map.cameraPosition = CameraPosition.Builder()
                .target(yogyakarta)
                .zoom(12.0) // Zoom yang sesuai untuk melihat beberapa lokasi
                .build()

            addWasteBankMarkers()

            map.setOnMarkerClickListener { marker ->
                selectedLocation = marker.title ?: "Lokasi Tidak Diketahui"
                binding.tvAddressValue.text = selectedLocation
                Toast.makeText(this, "📍 Lokasi dipilih: $selectedLocation", Toast.LENGTH_SHORT).show()
                true
            }
        }
    }

    private fun addWasteBankMarkers() {
        for ((name, latLng) in wasteBanks) {
            mapLibreMap.addMarker(MarkerOptions().position(latLng).title(name).snippet("Klik untuk memilih"))
        }
    }

    override fun onStart() { super.onStart(); binding.mapView.onStart() }
    override fun onResume() { super.onResume(); binding.mapView.onResume() }
    override fun onPause() { super.onPause(); binding.mapView.onPause() }
    override fun onStop() { super.onStop(); binding.mapView.onStop() }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); binding.mapView.onSaveInstanceState(outState) }
    override fun onDestroy() { super.onDestroy(); binding.mapView.onDestroy() }
    override fun onLowMemory() { super.onLowMemory(); binding.mapView.onLowMemory() }
}