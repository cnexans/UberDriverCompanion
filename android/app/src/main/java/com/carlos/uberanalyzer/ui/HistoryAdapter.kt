package com.carlos.uberanalyzer.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.carlos.uberanalyzer.R
import com.carlos.uberanalyzer.db.TripEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    private var trips: List<TripEntity> = emptyList()
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun submitList(list: List<TripEntity>) {
        trips = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_trip, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(trips[position])
    }

    override fun getItemCount() = trips.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTime: TextView = view.findViewById(R.id.tvItemTime)
        private val tvType: TextView = view.findViewById(R.id.tvItemType)
        private val tvPrice: TextView = view.findViewById(R.id.tvItemPrice)
        private val tvPorKm: TextView = view.findViewById(R.id.tvItemPorKm)
        private val tvPorMin: TextView = view.findViewById(R.id.tvItemPorMin)
        private val tvRatio: TextView = view.findViewById(R.id.tvItemRatio)
        private val tvRoute: TextView = view.findViewById(R.id.tvItemRoute)

        fun bind(trip: TripEntity) {
            tvTime.text = timeFormat.format(Date(trip.timestamp))
            tvType.text = "${trip.type} ${trip.subtype ?: ""}"
            tvPrice.text = "$ ${String.format("%,.0f", trip.price)}"
            tvPorKm.text = "$/km: ${String.format("%,.0f", trip.pesosPorKm)}"
            tvPorKm.setTextColor(if (trip.pesosPorKm >= 300) Color.GREEN else Color.RED)
            tvPorMin.text = "$/min: ${String.format("%,.0f", trip.pesosPorMin)}"
            tvRatio.text = "${String.format("%.0f", trip.pctDistancia)}%"
            tvRatio.setTextColor(
                when {
                    trip.pctDistancia >= 60.0 -> Color.GREEN
                    trip.pctDistancia >= 40.0 -> Color.YELLOW
                    else -> Color.RED
                }
            )
            tvRoute.text = "${trip.pickupAddress ?: "?"} -> ${trip.destination ?: "?"}"
        }
    }
}
