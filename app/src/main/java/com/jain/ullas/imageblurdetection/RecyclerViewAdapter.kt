package com.jain.ullas.imageblurdetection

import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_row.view.*

class RecyclerViewAdapter(val items: ArrayList<Data>) : RecyclerView.Adapter<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, p1: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_row, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size
}

class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    fun bind(data: Data) {
        itemView.scannedImage.setImageBitmap(data.bitmap)
        val imageSharpness = data.score
        when (imageSharpness < MainActivity.BLUR_THRESHOLD) {
            true -> {
                itemView.status.text = imageSharpness.toString()
                itemView.status.setTextColor(Color.parseColor("#F70913"))
            }
            false -> {
                itemView.status.text = imageSharpness.toString()
                itemView.status.setTextColor(Color.parseColor("#34F709"))
            }
        }
    }

}