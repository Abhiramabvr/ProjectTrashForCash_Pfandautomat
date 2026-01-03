package com.bvr.projectjtcm.ui.user

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
// Hapus import AppCompatActivity, karena kita sudah punya BaseActivity di package yang sama
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bvr.projectjtcm.R
import com.bvr.projectjtcm.data.WastePrice
import com.bvr.projectjtcm.data.WasteData // Pastikan import data model aman
import com.bvr.projectjtcm.databinding.ActivityMainBinding
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// PERUBAHAN 1: Mewarisi BaseActivity
class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var categoryButtons: List<MaterialButton>
    private var currentWeight = 1
    private var currentIncome = 3500.0
    private var selectedType = "Plastic"

    private val categoryPrices = mutableMapOf(
        "Plastic" to 3500.0,
        "Glass" to 2500.0,
        "Organic" to 1500.0,
        "Metal" to 5000.0,
        "Paper" to 2000.0,
        "Other" to 1000.0
    )

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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cardParams = binding.cardInfo.layoutParams as ViewGroup.MarginLayoutParams
            cardParams.topMargin = systemBars.top + (40 * resources.displayMetrics.density).toInt()
            binding.cardInfo.layoutParams = cardParams
            binding.bottomNavContainer.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        setupCategoryButtons()
        setupSeekBar()

        // PERUBAHAN 2: Panggil fungsi dari BaseActivity (Hanya 1 Baris!)
        setupBottomNavigation(R.id.navHome)

        fetchPricesFromFirebase()

        updateButtonStates(binding.btnPlastic)
        updateWeightAndIncome(binding.seekBar.progress)

        binding.btnNext.setOnClickListener {
            val intent = Intent(this, OrderPickupActivity::class.java)
            intent.putExtra("TYPE", selectedType)
            intent.putExtra("WEIGHT", currentWeight)
            intent.putExtra("INCOME", currentIncome)
            startActivity(intent)
        }

        binding.btnSaveLater.setOnClickListener {
            saveToFirebase("Saved")
        }
    }

    private fun fetchPricesFromFirebase() {
        if (database == null) return

        database?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (dataSnapshot in snapshot.children) {
                        val priceData = dataSnapshot.getValue(WastePrice::class.java)
                        if (priceData != null) {
                            categoryPrices[priceData.category] = priceData.pricePerKg
                        }
                    }
                    updateWeightAndIncome(binding.seekBar.progress)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // PERUBAHAN 3: Fungsi setupDynamicBottomNavigation() SUDAH DIHAPUS.
    // Kode jadi jauh lebih bersih.

    private fun saveToFirebase(status: String) {
        try {
            val database = FirebaseDatabase.getInstance().getReference("waste_history")
            val id = database.push().key
            val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            val month = SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())

            val wasteData = WasteData(
                id,
                selectedType,
                currentWeight,
                currentIncome,
                status,
                date,
                month,
                location = "Saved Item"
            )

            if (id != null) {
                database.child(id).setValue(wasteData).addOnCompleteListener {
                    if (it.isSuccessful) {
                        Toast.makeText(this, "Data Saved Successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to save data", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Firebase Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupCategoryButtons() {
        categoryButtons = listOf(
            binding.btnPlastic, binding.btnGlass, binding.btnOrganic,
            binding.btnMetal, binding.btnPaper, binding.btnOther
        )

        categoryButtons.forEach { button ->
            button.setOnClickListener {
                updateButtonStates(button)
                selectedType = button.text.toString()
                binding.tvSelectedCategory.text = selectedType
                updateWeightAndIncome(binding.seekBar.progress)
            }
        }
    }

    private fun updateButtonStates(selectedButton: MaterialButton) {
        val activeColor = Color.parseColor("#328E6E")
        val inactiveColor = Color.parseColor("#FFFFFF")
        val activeTextColor = Color.parseColor("#FFFFFF")
        val inactiveTextColor = Color.parseColor("#1B211A")

        categoryButtons.forEach { button ->
            if (button.id == selectedButton.id) {
                button.setBackgroundColor(activeColor)
                button.setTextColor(activeTextColor)
                button.iconTint = ColorStateList.valueOf(Color.WHITE)
                button.elevation = 8f
            } else {
                button.setBackgroundColor(inactiveColor)
                button.setTextColor(inactiveTextColor)
                button.iconTint = ColorStateList.valueOf(Color.BLACK)
                button.elevation = 8f
            }
        }
    }

    private fun setupSeekBar() {
        binding.seekBar.max = 9
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateWeightAndIncome(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun updateWeightAndIncome(progress: Int) {
        currentWeight = progress + 1

        val pricePerKg = categoryPrices[selectedType] ?: 0.0
        currentIncome = (currentWeight * pricePerKg)

        binding.tvWeight.text = "$currentWeight kg"

        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        format.maximumFractionDigits = 0
        val formattedIncome = format.format(currentIncome)

        binding.tvIncome.text = formattedIncome
    }
}