package org.linphone.loquace_integration.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.content.ContextCompat
import org.linphone.loquace_integration.R
import java.io.File

object GroupIconUtils {

    private const val ICON_SIZE = 256
    private var cachedIconPath: String? = null

    fun getOrCreateGroupIconFile(context: Context): String? {
        cachedIconPath?.let { if (File(it).exists()) return it }

        return try {
            val drawable = ContextCompat.getDrawable(context, R.drawable.loquace_group_icon)
                ?: return null

            val bitmap = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ContextCompat.getColor(context, R.color.login_box_bg)
            }
            canvas.drawCircle(ICON_SIZE / 2f, ICON_SIZE / 2f, ICON_SIZE / 2f, paint)

            val padding = ICON_SIZE / 5
            drawable.setBounds(padding, padding, ICON_SIZE - padding, ICON_SIZE - padding)
            drawable.draw(canvas)

            val iconFile = File(context.cacheDir, "loquace_group_icon.png")
            iconFile.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            cachedIconPath = iconFile.absolutePath
            iconFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}