package com.bvr.projectjtcm.ui.collector

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bvr.projectjtcm.R
import com.bvr.projectjtcm.data.WastePrice
import com.bvr.projectjtcm.databinding.ActivityCollectorPriceListBinding
import com.bvr.projectjtcm.ui.adapter.PriceAdapter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CollectorPriceListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCollectorPriceListBinding
    private lateinit var adapter: PriceAdapter

    // Kita gunakan list kosong awal, nanti diisi oleh Firebase
    private val priceList = ArrayList<WastePrice>()

    private val database by lazy {
        try {
            // Path ini AMAN (Global), tidak perlu User ID
            FirebaseDatabase.getInstance().getReference("waste_prices")
        } catch (e: Exception) {
            null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCollectorPriceListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        fetchPricesFromFirebase()
        setupBottomNavigation()
    }

    private fun setupRecyclerView() {
        binding.rvPriceList.layoutManager = LinearLayoutManager(this)
        adapter = PriceAdapter(priceList)
        binding.rvPriceList.adapter = adapter

        adapter.setOnItemClickListener(object : PriceAdapter.OnItemClickListener {
            override fun onItemClick(wastePrice: WastePrice) {
                showEditPriceDialog(wastePrice)
            }
        })
    }

    private fun showEditPriceDialog(wastePrice: WastePrice) {
        val editText = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            // Tampilkan harga saat ini agar mudah diedit
            setText(wastePrice.pricePerKg.toString())
            hint = "Masukkan harga baru per kg"
        }

        AlertDialog.Builder(this)
            .setTitle("Edit Harga: ${wastePrice.category}")
            .setMessage("Ubah harga per kilogram:")
            .setView(editText)
            .setPositiveButton("Simpan") { _, _ ->
                val input = editText.text.toString()
                val newPrice = input.toDoubleOrNull()

                if (newPrice != null && newPrice >= 0) {
                    updatePriceInFirebase(wastePrice.category, newPrice)
                } else {
                    Toast.makeText(this, "Harga tidak valid", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun updatePriceInFirebase(category: String, newPrice: Double) {
        // Asumsi: Key database sama dengan nama kategori
        database?.child(category)?.child("pricePerKg")?.setValue(newPrice)
            ?.addOnSuccessListener {
                Toast.makeText(this, "Harga $category berhasil diupdate", Toast.LENGTH_SHORT).show()
            }
            ?.addOnFailureListener {
                Toast.makeText(this, "Gagal mengupdate harga: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchPricesFromFirebase() {
        if (database == null) return

        database?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempList = ArrayList<WastePrice>()

                if (snapshot.exists()) {
                    for (dataSnapshot in snapshot.children) {
                        val price = dataSnapshot.getValue(WastePrice::class.java)
                        if (price != null) {
                            tempList.add(price)
                        }
                    }
                }

                // PENTING: Gunakan fungsi updateData yang tadi kita buat di Adapter
                // agar animasi refresh-nya lebih mulus
                adapter.updateData(tempList)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CollectorPriceListActivity, "Gagal memuat harga", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupBottomNavigation() {
        val navScan = binding.bottomNavContainer.findViewById<ImageView>(R.id.navScan)
        val navEditPrices = binding.bottomNavContainer.findViewById<ImageView>(R.id.navEditPrices)

        // Efek Visual: Ikon Scan redup, Ikon Harga terang (karena sedang aktif)
        navScan.alpha = 0.5f
        navEditPrices.alpha = 1.0f

        navScan.setOnClickListener {
            // Kembali ke halaman Scanner
            finish()
            // Opsional: hilangkan animasi agar transisi terasa instan seperti tab bar asli
            overridePendingTransition(0, 0)
        }

        navEditPrices.setOnClickListener {
            // Sedang di halaman ini, tidak perlu aksi apa-apa
        }
    }
}