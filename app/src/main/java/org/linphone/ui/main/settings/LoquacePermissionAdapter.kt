package org.linphone.ui.main.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.linphone.R
import org.linphone.databinding.LoquacePermissionItemBinding

data class PermissionItem(
    val name: String,
    val isGranted: Boolean,
    val onClickRequest: () -> Unit
)

class LoquacePermissionsAdapter(
    private val items: List<PermissionItem>
) : RecyclerView.Adapter<LoquacePermissionsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = LoquacePermissionItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(val binding: LoquacePermissionItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PermissionItem) {
            binding.permissionName.text = item.name
            binding.permissionStatus.setImageResource(
                if (item.isGranted) R.drawable.ic_permission_granted
                else R.drawable.ic_permission_denied
            )
            // Click to request if not granted
            binding.root.setOnClickListener {
                if (!item.isGranted) {
                    item.onClickRequest()
                }
            }
        }
    }
}