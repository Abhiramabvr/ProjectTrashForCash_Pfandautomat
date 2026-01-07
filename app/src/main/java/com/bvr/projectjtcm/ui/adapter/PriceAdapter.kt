package com.bvr.projectjtcm.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bvr.projectjtcm.data.WastePrice
import com.bvr.projectjtcm.databinding.ItemWastePriceBinding
import java.text.NumberFormat
import java.util.Locale

class PriceAdapter(private val priceList: ArrayList<WastePrice>) : RecyclerView.Adapter<PriceAdapter.PriceViewHolder>() {

    private var listener: OnItemClickListener? = null

    interface OnItemClickListener {
        fun onItemClick(wastePrice: WastePrice)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    // --- FITUR BARU: Update Data Dinamis ---
    // Fungsi ini dipanggil saat data dari Firebase selesai didownload
    fun updateData(newList: List<WastePrice>) {
        priceList.clear()
        priceList.addAll(newList)
        notifyDataSetChanged() // Memberitahu RecyclerView untuk refresh tampilan
    }

    inner class PriceViewHolder(val binding: ItemWastePriceBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener?.onItemClick(priceList[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PriceViewHolder {
        val binding = ItemWastePriceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PriceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PriceViewHolder, position: Int) {
        val currentItem = priceList[position]

        holder.binding.tvCategory.text = currentItem.category

        // Format Mata Uang Rupiah (Indonesia)
        val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        format.maximumFractionDigits = 0
        val formattedPrice = format.format(currentItem.pricePerKg)

        // Tampilan: "+ Rp5.000 /kg"
        holder.binding.tvPrice.text = "+ $formattedPrice /kg"
    }

    override fun getItemCount(): Int {
        return priceList.size
    }
}