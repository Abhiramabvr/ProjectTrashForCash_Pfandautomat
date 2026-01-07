package com.bvr.projectjtcm.ui.collector

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
import com.bvr.projectjtcm.databinding.ActivityPriceListBinding
import com.bvr.projectjtcm.ui.adapter.PriceAdapter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class EditPriceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPriceListBinding
    private lateinit var adapter: PriceAdapter
    private val priceList = ArrayList<WastePrice>()

    private val database by lazy {
        try {
            // Path Database Global (Aman)
            FirebaseDatabase.getInstance().getReference("waste_prices")
        } catch (e: Exception) {
            null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPriceListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ubah Judul agar terlihat beda dengan versi User
        binding.tvTitle.text = "Edit Harga Sampah (Kolektor)"

        setupRecyclerView()
        fetchPricesFromFirebase()
        setupBottomNavigation()
    }

    private fun setupRecyclerView() {
        binding.rvPriceList.layoutManager = LinearLayoutManager(this)
        adapter = PriceAdapter(priceList)
        binding.rvPriceList.adapter = adapter

        // Listener Klik untuk Edit
        adapter.setOnItemClickListener(object : PriceAdapter.OnItemClickListener {
            override fun onItemClick(wastePrice: WastePrice) {
                showEditPriceDialog(wastePrice)
            }
        })
    }

    private fun showEditPriceDialog(wastePrice: WastePrice) {
        val editText = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(wastePrice.pricePerKg.toString())
            hint = "Masukkan harga baru"
        }

        AlertDialog.Builder(this)
            .setTitle("Edit: ${wastePrice.category}")
            .setMessage("Masukkan harga per kg:")
            .setView(editText)
            .setPositiveButton("Simpan") { _, _ ->
                val newPrice = editText.text.toString().toDoubleOrNull()

                // Validasi: Harga tidak boleh kosong dan tidak boleh minus
                if (newPrice != null && newPrice >= 0) {
                    updatePriceInFirebase(wastePrice.category, newPrice)
                } else {
                    Toast.makeText(this, "Harga tidak valid!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun updatePriceInFirebase(category: String, newPrice: Double) {
        database?.child(category)?.child("pricePerKg")?.setValue(newPrice)
            ?.addOnSuccessListener {
                Toast.makeText(this, "✅ Harga $category diperbarui", Toast.LENGTH_SHORT).show()
            }
            ?.addOnFailureListener {
                Toast.makeText(this, "❌ Gagal update: ${it.message}", Toast.LENGTH_SHORT).show()
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
                // Pakai updateData agar refresh lebih mulus
                adapter.updateData(tempList)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@EditPriceActivity, "Gagal memuat harga", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupBottomNavigation() {
        // PERHATIAN: Karena kamu memakai layout 'ActivityPriceListBinding' (punya User),
        // ID tombolnya adalah navHome, navHistory, dll.
        // Di sini kita "mengakali" agar navHome jadi tombol kembali (ke Scanner).

        val btnBackToScan = binding.bottomNavContainer.findViewById<ImageView>(R.id.navHome) // Ikon pertama
        val btnCurrentPage = binding.bottomNavContainer.findViewById<ImageView>(R.id.navHistory) // Ikon kedua

        // Efek Visual: Tombol aktif terang, tombol lain redup
        btnBackToScan.alpha = 0.5f
        btnCurrentPage.alpha = 1.0f

        // Tombol Home (User) -> Berfungsi sebagai Back ke Scanner (Kolektor)
        btnBackToScan.setOnClickListener {
            finish()
        }

        // Tombol History (User) -> Berfungsi sebagai halaman Edit Harga saat ini
        btnCurrentPage.setOnClickListener {
            // Tidak melakukan apa-apa karena sudah di halaman ini
            Toast.makeText(this, "Anda sedang di mode Edit Harga", Toast.LENGTH_SHORT).show()
        }
    }
}