package com.bvr.projectjtcm.ui.user

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
// Hapus import AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bvr.projectjtcm.R
import com.bvr.projectjtcm.databinding.ActivityProfileBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

// PERUBAHAN 1: Mewarisi BaseActivity
class ProfileActivity : BaseActivity() {

    private lateinit var binding: ActivityProfileBinding

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            binding.ivProfileImage.setImageURI(it)
            saveUserData("PROFILE_IMAGE_URI", it.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val backParams = binding.btnBack.layoutParams as ViewGroup.MarginLayoutParams
            backParams.topMargin = systemBars.top + (20 * resources.displayMetrics.density).toInt()
            binding.btnBack.layoutParams = backParams

            binding.bottomNavContainer.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        loadUserData()
        fetchRecycledStats()
        setupClickListeners()

        // PERUBAHAN 2: Panggil Navigasi Otomatis (Set aktif di menu Profile)
        setupBottomNavigation(R.id.navProfile)
    }

    private fun loadUserData() {
        val sharedPref = getSharedPreferences("UserProfile", MODE_PRIVATE)

        binding.tvName.text = sharedPref.getString("NAME", "Abhirama")
        binding.tvRole.text = sharedPref.getString("ROLE", "Developer")
        binding.tvEmailValue.text = sharedPref.getString("EMAIL", "abhirama@gmail.com")
        binding.tvPhoneValue.text = sharedPref.getString("PHONE", "")
        binding.tvInstagramValue.text = sharedPref.getString("INSTAGRAM", "")

        val imageUriString = sharedPref.getString("PROFILE_IMAGE_URI", null)
        if (imageUriString != null) {
            binding.ivProfileImage.setImageURI(Uri.parse(imageUriString))
        }
    }

    private fun fetchRecycledStats() {
        val database = FirebaseDatabase.getInstance().getReference("waste_history")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalWeight = 0
                for (dataSnapshot in snapshot.children) {
                    val weight = dataSnapshot.child("weight").getValue(Int::class.java) ?: 0
                    totalWeight += weight
                }
                binding.tvRecycledTotal.text = "$totalWeight kg"
            }

            override fun onCancelled(error: DatabaseError) {
                binding.tvRecycledTotal.text = "0 kg"
            }
        })
    }

    private fun saveUserData(key: String, value: String) {
        val sharedPref = getSharedPreferences("UserProfile", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString(key, value)
            apply()
        }
        loadUserData()
        Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show()
    }

    private fun showEditDialog(title: String, key: String, currentValue: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null)
        val etValue = dialogView.findViewById<EditText>(R.id.etValue)
        etValue.setText(currentValue)

        AlertDialog.Builder(this)
            .setTitle("Edit $title")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newValue = etValue.text.toString()
                if (key != "NAME" || newValue.isNotEmpty()) {
                    saveUserData(key, newValue)
                } else {
                    Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.cvProfileImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.tvName.setOnClickListener {
            showEditDialog("Name", "NAME", binding.tvName.text.toString())
        }

        binding.tvRole.setOnClickListener {
            showEditDialog("Role/Job", "ROLE", binding.tvRole.text.toString())
        }

        binding.itemEmail.setOnClickListener {
            showEditDialog("Email", "EMAIL", binding.tvEmailValue.text.toString())
        }

        binding.itemPhone.setOnClickListener {
            showEditDialog("Phone", "PHONE", binding.tvPhoneValue.text.toString())
        }

        binding.itemInstagram.setOnClickListener {
            showEditDialog("Instagram", "INSTAGRAM", binding.tvInstagramValue.text.toString())
        }
    }

    // PERUBAHAN 3: Fungsi setupDynamicBottomNavigation() SUDAH DIHAPUS.
}