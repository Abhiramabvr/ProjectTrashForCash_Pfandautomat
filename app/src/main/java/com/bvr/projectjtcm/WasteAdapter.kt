package com.bvr.projectjtcm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bvr.projectjtcm.databinding.ItemWasteHistoryBinding
import java.text.NumberFormat
import java.util.Locale

class WasteAdapter(private val wasteList: ArrayList<WasteData>) : RecyclerView.Adapter<WasteAdapter.WasteViewHolder>() {

    private var listener: OnItemClickListener? = null

    interface OnItemClickListener {
        fun onItemClick(wasteData: WasteData)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    inner class WasteViewHolder(val binding: ItemWasteHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener?.onItemClick(wasteList[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WasteViewHolder {
        val binding = ItemWasteHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WasteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WasteViewHolder, position: Int) {
        val currentItem = wasteList[position]

        // Set data utama
        holder.binding.tvDate.text = currentItem.month
        holder.binding.tvWasteType.text = currentItem.type
        holder.binding.tvWeight.text = "${currentItem.weight} kg"

        val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        format.maximumFractionDigits = 0
        holder.binding.tvIncome.text = format.format(currentItem.income)

        // Atur visibilitas dan data untuk detail jadwal
        if (currentItem.status == "Ordered" && currentItem.location != "Saved Item") {
            // Jika sudah diorder dan BUKAN item yang hanya di-save
            holder.binding.layoutScheduleDetails.visibility = View.VISIBLE
            holder.binding.tvLocation.text = "📍 ${currentItem.location}"
            holder.binding.tvScheduleTime.text = "🗓️ ${currentItem.pickupDate}, ${currentItem.pickupTime}"
            holder.binding.root.alpha = 1.0f // Tampil terang
            holder.binding.ivStatusIcon.setImageResource(android.R.drawable.ic_menu_send) // Ikon check/terkirim
        } else {
            // Jika hanya "Saved Item"
            holder.binding.layoutScheduleDetails.visibility = View.GONE
            holder.binding.root.alpha = 1.0f // Tetap terang agar bisa diklik
            holder.binding.ivStatusIcon.setImageResource(android.R.drawable.ic_menu_save) // Ikon simpan
        }
    }

    override fun getItemCount(): Int {
        return wasteList.size
    }
}
