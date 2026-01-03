package com.bvr.projectjtcm.data

data class WasteData(
    val id: String? = null,
    val type: String = "",
    val weight: Int = 0,
    val income: Double = 0.0,
    val status: String = "Saved", // "Saved" or "Ordered"
    val date: String = "", // Tanggal pembuatan data
    val month: String = "", // Bulan pembuatan data
    val location: String = "", // Lokasi Bank Sampah
    val pickupDate: String = "", // Tanggal penjemputan/pengantaran
    val pickupTime: String = ""  // Jam penjemputan/pengantaran
)