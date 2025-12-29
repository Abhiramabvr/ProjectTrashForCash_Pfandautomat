package com.bvr.projectjtcm

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bvr.projectjtcm.databinding.ActivityPriceListBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PriceListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPriceListBinding
    private lateinit var adapter: PriceAdapter
    private val priceList = ArrayList<WastePrice>()
    
    private val database by lazy { 
        try {
            FirebaseDatabase.getInstance().getReference("waste_prices")
        } catch (e: Exception) {
            null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        binding = ActivityPriceListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            val titleParams = binding.tvTitle.layoutParams as android.view.ViewGroup.MarginLayoutParams
            titleParams.topMargin = systemBars.top + (40 * resources.displayMetrics.density).toInt()
            binding.tvTitle.layoutParams = titleParams

            binding.bottomNavContainer.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        setupRecyclerView()
        fetchPricesFromFirebase()
        
        // PERBAIKAN: Set menu aktif ke navOrder (Ikon ke-3)
        setupDynamicBottomNavigation(R.id.navOrder) 
    }

    private fun setupRecyclerView() {
        binding.rvPriceList.layoutManager = LinearLayoutManager(this)
        adapter = PriceAdapter(priceList)
        binding.rvPriceList.adapter = adapter
    }

    private fun fetchPricesFromFirebase() {
        if (database == null) {
            Toast.makeText(this, "Database connection error", Toast.LENGTH_SHORT).show()
            addDummyData()
            return
        }

        database?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                priceList.clear()
                if (snapshot.exists()) {
                    for (dataSnapshot in snapshot.children) {
                        val price = dataSnapshot.getValue(WastePrice::class.java)
                        if (price != null) {
                            priceList.add(price)
                        }
                    }
                } else {
                    addDefaultDataToFirebase()
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@PriceListActivity, "Failed to load prices: ${error.message}", Toast.LENGTH_SHORT).show()
                addDummyData()
            }
        })
    }
    
    private fun addDefaultDataToFirebase() {
        val defaultPrices = listOf(
            WastePrice("Plastic", 2500.0),
            WastePrice("Glass", 3500.0),
            WastePrice("Organic", 1750.0),
            WastePrice("Metal", 5000.0),
            WastePrice("Paper", 1500.0),
            WastePrice("Other", 1250.0)
        )
        
        defaultPrices.forEach { price ->
            database?.child(price.category)?.setValue(price)
        }
    }

    private fun addDummyData() {
        priceList.clear()
        priceList.add(WastePrice("Plastic", 2500.0))
        priceList.add(WastePrice("Glass", 3500.0))
        priceList.add(WastePrice("Organic", 1750.0))
        priceList.add(WastePrice("Metal", 5000.0))
        priceList.add(WastePrice("Paper", 1500.0))
        priceList.add(WastePrice("Other", 1250.0))
        adapter.notifyDataSetChanged()
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
            if (activeId != R.id.navHistory) {
                startActivity(Intent(this, HistoryActivity::class.java))
                overridePendingTransition(0, 0)
                finish()
            }
        }

        navOrder.setOnClickListener {
             // Sudah di sini
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