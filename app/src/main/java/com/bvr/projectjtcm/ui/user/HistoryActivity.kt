package com.bvr.projectjtcm.ui.user

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bvr.projectjtcm.R
import com.bvr.projectjtcm.data.WasteData
import com.bvr.projectjtcm.databinding.ActivityHistoryBinding
import com.bvr.projectjtcm.ui.adapter.WasteAdapter
import com.bvr.projectjtcm.ui.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.NumberFormat
import java.util.Locale

class HistoryActivity : BaseActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var adapter: WasteAdapter

    // Kita inisialisasi list kosong, nanti diisi oleh Adapter
    private val wasteList = ArrayList<WasteData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()

        // 1. Cek Login Session (Wajib)
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            // Jika tidak ada user login, tendang ke Login Page
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        setupRecyclerView()

        // 2. Ambil data dengan parameter User UID
        fetchDataFromFirebase(currentUser.uid)

        // 3. Setup Navigasi Bawah
        setupBottomNavigation(R.id.navHistory)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val titleParams = binding.tvTitle.layoutParams as ViewGroup.MarginLayoutParams
            titleParams.topMargin = systemBars.top + (20 * resources.displayMetrics.density).toInt()
            binding.tvTitle.layoutParams = titleParams
            binding.bottomNavContainer.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
    }

    private fun setupRecyclerView() {
        binding.rvWasteHistory.layoutManager = LinearLayoutManager(this)
        adapter = WasteAdapter(wasteList) // List awal kosong

        // Handle Klik Item
        adapter.setOnItemClickListener(object : WasteAdapter.OnItemClickListener {
            override fun onItemClick(wasteData: WasteData) {
                if (wasteData.status == "Ordered" || wasteData.status == "Completed") {
                    // Jika status Ordered/Completed -> Buka QR Code
                    val intent = Intent(this@HistoryActivity, QrDisplayActivity::class.java)
                    intent.putExtra("ORDER_ID", wasteData.id)
                    startActivity(intent)
                } else {
                    // Jika status Saved -> Lanjutkan Edit Order
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

    // --- LOGIC UTAMA: AMBIL DATA PRIBADI ---
    private fun fetchDataFromFirebase(userId: String) {
        // Arahkan ke folder: waste_history -> USER_ID
        val databaseRef = FirebaseDatabase.getInstance().getReference("waste_history").child(userId)

        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempList = ArrayList<WasteData>()
                var totalWeight = 0
                var totalIncome = 0.0

                for (dataSnapshot in snapshot.children) {
                    val waste = dataSnapshot.getValue(WasteData::class.java)
                    if (waste != null) {
                        // Add(0, waste) agar data terbaru muncul di paling atas
                        tempList.add(0, waste)

                        // Hitung Total untuk Summary Header
                        totalWeight += waste.weight
                        totalIncome += waste.income
                    }
                }

                // Update Adapter dengan cara modern (List update)
                adapter.updateData(tempList)

                // Update Header Summary
                updateSummary(totalWeight, totalIncome)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@HistoryActivity, "Gagal memuat data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateSummary(weight: Int, income: Double) {
        binding.tvTotalWeight.text = "$weight kg"

        val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        format.maximumFractionDigits = 0
        binding.tvTotalIncome.text = format.format(income)

        // Progress bar max 100kg (hanya visual target)
        val progress = if (weight > 100) 100 else weight
        binding.progressBar.progress = progress
    }
}