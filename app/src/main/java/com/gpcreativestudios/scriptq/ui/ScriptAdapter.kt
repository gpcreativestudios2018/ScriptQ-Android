package com.gpcreativestudios.scriptq.ui

import android.os.Build
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gpcreativestudios.scriptq.R
import com.gpcreativestudios.scriptq.data.Script

import android.widget.ImageButton
import com.google.android.material.button.MaterialButton

class ScriptAdapter(
    private val onItemClick: (Script) -> Unit,
    private val onItemLongClick: (View, Script) -> Boolean,
    private val onFavoriteClick: (Script) -> Unit,
    private val onPromptClick: (Script) -> Unit
) : ListAdapter<Script, ScriptAdapter.ScriptViewHolder>(ScriptsComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScriptViewHolder {
        val holder = ScriptViewHolder.create(parent)
        holder.itemView.setOnClickListener {
            val position = holder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onItemClick(getItem(position))
            }
        }
        holder.itemView.setOnLongClickListener {
            val position = holder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onItemLongClick(holder.itemView, getItem(position))
            } else {
                false
            }
        }
        holder.favoriteButton.setOnClickListener {
            val position = holder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onFavoriteClick(getItem(position))
            }
        }
        holder.promptButton.setOnClickListener {
            val position = holder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onPromptClick(getItem(position))
            }
        }
        holder.moreButton.setOnClickListener {
            val position = holder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onItemLongClick(holder.moreButton, getItem(position))
            }
        }
        return holder
    }

    override fun onBindViewHolder(holder: ScriptViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current)
    }

    class ScriptViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val scriptTitleView: TextView = itemView.findViewById(R.id.textViewTitle)
        private val scriptContentView: TextView = itemView.findViewById(R.id.textViewContent)
        val favoriteButton: ImageButton = itemView.findViewById(R.id.buttonFavorite)
        val moreButton: ImageButton = itemView.findViewById(R.id.buttonMore)
        val promptButton: MaterialButton = itemView.findViewById(R.id.buttonPrompt)

        fun bind(script: Script) {
            scriptTitleView.text = script.title

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                scriptContentView.text = Html.fromHtml(script.textContent, Html.FROM_HTML_MODE_COMPACT).toString()
            } else {
                @Suppress("DEPRECATION")
                scriptContentView.text = Html.fromHtml(script.textContent).toString()
            }

            if (script.isFavorite) {
                favoriteButton.setImageResource(android.R.drawable.star_on)
            } else {
                favoriteButton.setImageResource(android.R.drawable.star_off)
            }
        }

        companion object {
            fun create(parent: ViewGroup): ScriptViewHolder {
                val view: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_script, parent, false)
                return ScriptViewHolder(view)
            }
        }
    }

    class ScriptsComparator : DiffUtil.ItemCallback<Script>() {
        override fun areItemsTheSame(oldItem: Script, newItem: Script): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Script, newItem: Script): Boolean {
            return oldItem.title == newItem.title && 
                   oldItem.textContent == newItem.textContent && 
                   oldItem.isFavorite == newItem.isFavorite
        }
    }
}
