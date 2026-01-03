package com.bvr.projectjtcm.ui.user

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
// Hapus import AppCompatActivity karena kita pakai BaseActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bvr.projectjtcm.R
import com.bvr.projectjtcm.data.WasteData
import com.bvr.projectjtcm.databinding.ActivityHistoryBinding
import com.bvr.projectjtcm.ui.adapter.WasteAdapter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.NumberFormat
import java.util.Locale

// PERUBAHAN 1: Mewarisi BaseActivity
class HistoryActivity : BaseActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var adapter: WasteAdapter
    private val wasteList = ArrayList<WasteData>()

    private val database by lazy {
        try {
            FirebaseDatabase.getInstance().getReference("waste_history")
        } catch (e: Exception) {
            null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            val titleParams = binding.tvTitle.layoutParams as ViewGroup.MarginLayoutParams
            titleParams.topMargin = systemBars.top + (20 * resources.displayMetrics.density).toInt()
            binding.tvTitle.layoutParams = titleParams

            binding.bottomNavContainer.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        setupRecyclerView()
        fetchDataFromFirebase()

        // PERUBAHAN 2: Panggil fungsi sakti dari BaseActivity
        // Parameter R.id.navHistory membuat ikon "Jam" (History) menyala
        setupBottomNavigation(R.id.navHistory)
    }

    private fun setupRecyclerView() {
        binding.rvWasteHistory.layoutManager = LinearLayoutManager(this)
        adapter = WasteAdapter(wasteList)

        // IMPLEMENTASI KLIK PADA ITEM HISTORY
        adapter.setOnItemClickListener(object : WasteAdapter.OnItemClickListener {
            override fun onItemClick(wasteData: WasteData) {
                if (wasteData.status == "Ordered") {
                    // Jika sudah diorder, buka halaman QR Code
                    val intent = Intent(this@HistoryActivity, QrDisplayActivity::class.java)
                    intent.putExtra("ORDER_ID", wasteData.id)
                    startActivity(intent)
                } else {
                    // Jika masih "Saved", buka halaman Order untuk dilanjutkan
                    val intent = Intent(this@HistoryActivity, OrderPickupActivity::class.java)
                    intent.putExtra("EXISTING_ID", wasteData.id)
                    intent.putExtra("TYPE", wasteData.type)
                    intent.putExtra("WEIGHT", wasteData.weight)
                    intent.putExtra("INCOME", wasteData.income)
                    startActivity(intent)
                }
            }
        })

        binding.rvWasteHistory.adapter = adapter
    }

    private fun fetchDataFromFirebase() {
        if (database == null) {
            Toast.makeText(this, "Database connection error", Toast.LENGTH_SHORT).show()
            return
        }

        database?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                wasteList.clear()
                var totalWeight = 0
                var totalIncome = 0.0

                for (dataSnapshot in snapshot.children) {
                    val waste = dataSnapshot.getValue(WasteData::class.java)
                    if (waste != null) {
                        wasteList.add(0, waste) // Add to top
                        totalWeight += waste.weight
                        totalIncome += waste.income
                    }
                }
                adapter.notifyDataSetChanged()
                updateSummary(totalWeight, totalIncome)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@HistoryActivity, "Failed to load data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateSummary(weight: Int, income: Double) {
        binding.tvTotalWeight.text = "$weight kg"

        val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        format.maximumFractionDigits = 0
        binding.tvTotalIncome.text = format.format(income)

        val progress = if (weight > 100) 100 else weight
        binding.progressBar.progress = progress
    }

    // PERUBAHAN 3: Fungsi setupDynamicBottomNavigation() SUDAH DIHAPUS.
    // Kode jadi jauh lebih bersih!
}