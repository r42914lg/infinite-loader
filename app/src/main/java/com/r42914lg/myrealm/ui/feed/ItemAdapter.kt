package com.r42914lg.myrealm.ui.feed

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.r42914lg.myrealm.R
import com.r42914lg.myrealm.domain.Item

class ItemAdapter : RecyclerView.Adapter<ItemAdapter.ViewHolder>() {

    private var _items: List<Item> = listOf()

    @SuppressLint("NotifyDataSetChanged")
    fun setItems(items: List<Item>) {
        _items = items
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val idTv: TextView = view.findViewById(R.id.item_id)
        val prop1Tv: TextView = view.findViewById(R.id.prop1)
        val prop2Tv: TextView = view.findViewById(R.id.prop2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.feed_item, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.idTv.text = _items[position].id.toString()
        holder.prop1Tv.text = _items[position].prop1
        holder.prop2Tv.text = _items[position].prop2
    }

    override fun getItemCount(): Int =
        _items.size
}
