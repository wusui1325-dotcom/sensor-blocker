package com.wusper.sensorblocker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView

class AppListAdapter(
    private val onToggle: (AppInfo, Boolean) -> Unit
) : RecyclerView.Adapter<AppListAdapter.VH>() {

    private val all = mutableListOf<AppInfo>()
    private val visible = mutableListOf<AppInfo>()
    private val checked = mutableSetOf<String>()
    private var query: String = ""
    private var allowMode: Boolean = true

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.appIcon)
        val label: TextView = view.findViewById(R.id.appLabel)
        val packageName: TextView = view.findViewById(R.id.appPackage)
        val badge: TextView = view.findViewById(R.id.appBadge)
        val check: CheckBox = view.findViewById(R.id.checkBlock)
        val root: View = view
    }

    fun submit(apps: List<AppInfo>, selected: Set<String>, allowMode: Boolean) {
        all.clear()
        all.addAll(apps)
        checked.clear()
        checked.addAll(selected)
        this.allowMode = allowMode
        applyFilter()
    }

    fun setSelected(selected: Set<String>) {
        checked.clear()
        checked.addAll(selected)
        notifyDataSetChanged()
    }

    fun filter(text: String) {
        query = text
        applyFilter()
    }

    private fun applyFilter() {
        val q = query.trim()
        visible.clear()
        if (q.isEmpty()) {
            visible.addAll(all)
        } else {
            visible.addAll(
                all.filter {
                    it.label.contains(q, ignoreCase = true) ||
                        it.packageName.contains(q, ignoreCase = true)
                }
            )
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val app = visible[position]
        val context = holder.itemView.context
        holder.label.text = app.label
        holder.packageName.text = app.packageName
        holder.check.setOnCheckedChangeListener(null)
        holder.check.isChecked = app.packageName in checked
        try {
            holder.icon.setImageDrawable(context.packageManager.getApplicationIcon(app.packageName))
        } catch (_: Exception) {
            holder.icon.setImageResource(R.mipmap.ic_launcher)
        }
        val showBadge = when {
            allowMode && app.isRecommendedAllow -> true
            !allowMode && app.isRecommendedBlock -> true
            else -> false
        }
        holder.badge.isVisible = showBadge
        if (showBadge) {
            holder.badge.text = context.getString(
                if (allowMode) R.string.badge_recommended_allow else R.string.badge_recommended_block
            )
        }
        val listener = View.OnClickListener {
            val nowChecked = app.packageName !in checked
            if (nowChecked) checked.add(app.packageName) else checked.remove(app.packageName)
            holder.check.isChecked = nowChecked
            onToggle(app, nowChecked)
        }
        holder.root.setOnClickListener(listener)
        holder.check.setOnClickListener(listener)
    }

    override fun getItemCount(): Int = visible.size
}
