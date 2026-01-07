package com.bvr.projectjtcm

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bvr.projectjtcm.databinding.ActivityHistoryBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.NumberFormat
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

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
        setupDynamicBottomNavigation(R.id.navHistory)
    }

    private fun setupRecyclerView() {
        binding.rvWasteHistory.layoutManager = LinearLayoutManager(this)
        adapter = WasteAdapter(wasteList)
        
        adapter.setOnItemClickListener(object : WasteAdapter.OnItemClickListener {
            override fun onItemClick(wasteData: WasteData) {
                // Logika Klik Berdasarkan Status
                when (wasteData.status) {
                    "Ordered" -> {
                        // Jika sudah diorder tapi belum selesai, tampilkan QR Code
                        val intent = Intent(this@HistoryActivity, QrDisplayActivity::class.java)
                        intent.putExtra("ORDER_ID", wasteData.id)
                        startActivity(intent)
                    }
                    "Saved" -> {
                        // Jika masih disimpan, lanjutkan order
                        val intent = Intent(this@HistoryActivity, OrderPickupActivity::class.java)
                        intent.putExtra("EXISTING_ID", wasteData.id)
                        intent.putExtra("TYPE", wasteData.type)
                        intent.putExtra("WEIGHT", wasteData.weight)
                        intent.putExtra("INCOME", wasteData.income)
                        startActivity(intent)
                    }
                    "Completed" -> {
                        // Jika sudah selesai, disable klik (hanya tampilkan pesan)
                        Toast.makeText(this@HistoryActivity, "✅ Transaksi ini sudah selesai & dicairkan.", Toast.LENGTH_SHORT).show()
                    }
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
                        wasteList.add(0, waste)
                        
                        // Hitung total hanya jika statusnya "Completed"
                        if (waste.status == "Completed") {
                            totalWeight += waste.weight
                            totalIncome += waste.income
                        }
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
        
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        format.maximumFractionDigits = 0
        binding.tvTotalIncome.text = format.format(income)

        // Progress bar logika sederhana (misal target 100kg)
        val progress = if (weight > 100) 100 else weight
        binding.progressBar.progress = progress
    }
    
    private fun setupDynamicBottomNavigation(activeId: Int) {
        val navHome = findViewById<ImageView>(R.id.navHome)
        val navHistory = findViewById<ImageView>(R.id.navHistory)
        val navOrder = findViewById<ImageView>(R.id.navOrder)
        val navProfile = findViewById<ImageView>(R.id.navProfile)

        val icons = listOf(navHome, navHistory, navOrder, navProfile)
        icons.forEach { it.alpha = 0.5f }

        findViewById<ImageView>(activeId)?.alpha = 1.0f

        navHome.setOnClickListener {
             if (activeId != R.id.navHome) {
                startActivity(Intent(this, MainActivity::class.java))
                overridePendingTransition(0, 0)
                finishAffinity()
            }
        }

        navHistory.setOnClickListener {
            // Already here
        }

        navOrder.setOnClickListener {
             if (activeId != R.id.navOrder) {
                startActivity(Intent(this, PriceListActivity::class.java))
                overridePendingTransition(0, 0)
                finish()
            }
        }

        navProfile.setOnClickListener {
             if (activeId != R.id.navProfile) {
                startActivity(Intent(this, ProfileActivity::class.java))
                overridePendingTransition(0, 0)
                finish()
            }
        }
    }
}