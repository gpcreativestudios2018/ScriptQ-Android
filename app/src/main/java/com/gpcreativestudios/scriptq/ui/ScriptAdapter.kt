package com.gpcreativestudios.scriptq.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gpcreativestudios.scriptq.R
import com.gpcreativestudios.scriptq.data.Script

class ScriptAdapter(private val onItemClick: (com.gpcreativestudios.scriptq.data.Script) -> Unit) : ListAdapter<Script, ScriptAdapter.ScriptViewHolder>(ScriptsComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScriptViewHolder {
        val holder = ScriptViewHolder.create(parent)
        holder.itemView.setOnClickListener {
            val position = holder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onItemClick(getItem(position))
            }
        }
        return holder
    }

    override fun onBindViewHolder(holder: ScriptViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current.title, current.textContent)
    }

    class ScriptViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val scriptTitleView: TextView = itemView.findViewById(R.id.textViewTitle)
        private val scriptContentView: TextView = itemView.findViewById(R.id.textViewContent)

        fun bind(title: String?, content: String?) {
            scriptTitleView.text = title
            scriptContentView.text = content
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
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: Script, newItem: Script): Boolean {
            return oldItem.title == newItem.title && oldItem.textContent == newItem.textContent
        }
    }
}
