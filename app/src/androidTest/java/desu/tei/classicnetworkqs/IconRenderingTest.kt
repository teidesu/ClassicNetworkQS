package desu.tei.classicnetworkqs

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class IconRenderingTest {
    @Test
    fun renderAllSignalStates() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val rows = listOf(
            intArrayOf(R.drawable.ic_qpr1_wifi_0, R.drawable.ic_qpr1_wifi_1, R.drawable.ic_qpr1_wifi_2, R.drawable.ic_qpr1_wifi_3, R.drawable.ic_qpr1_wifi_4),
            intArrayOf(R.drawable.ic_qpr1_wifi_0_no_internet, R.drawable.ic_qpr1_wifi_1_no_internet, R.drawable.ic_qpr1_wifi_2_no_internet, R.drawable.ic_qpr1_wifi_3_no_internet, R.drawable.ic_qpr1_wifi_4_no_internet),
            intArrayOf(R.drawable.ic_qpr1_cellular_0, R.drawable.ic_qpr1_cellular_1, R.drawable.ic_qpr1_cellular_2, R.drawable.ic_qpr1_cellular_3, R.drawable.ic_qpr1_cellular_4),
            intArrayOf(R.drawable.ic_qpr1_cellular_0_no_internet, R.drawable.ic_qpr1_cellular_1_no_internet, R.drawable.ic_qpr1_cellular_2_no_internet, R.drawable.ic_qpr1_cellular_3_no_internet, R.drawable.ic_qpr1_cellular_4_no_internet),
            intArrayOf(R.drawable.ic_wifi_0, R.drawable.ic_wifi_1, R.drawable.ic_wifi_2, R.drawable.ic_wifi_3, R.drawable.ic_wifi_4),
            intArrayOf(R.drawable.ic_wifi_0_no_internet, R.drawable.ic_wifi_1_no_internet, R.drawable.ic_wifi_2_no_internet, R.drawable.ic_wifi_3_no_internet, R.drawable.ic_wifi_4_no_internet),
            intArrayOf(R.drawable.ic_cellular_0, R.drawable.ic_cellular_1, R.drawable.ic_cellular_2, R.drawable.ic_cellular_3, R.drawable.ic_cellular_4),
            intArrayOf(R.drawable.ic_cellular_0_no_internet, R.drawable.ic_cellular_1_no_internet, R.drawable.ic_cellular_2_no_internet, R.drawable.ic_cellular_3_no_internet, R.drawable.ic_cellular_4_no_internet),
            intArrayOf(R.drawable.ic_networks_available, R.drawable.ic_disconnected, R.drawable.ic_airplane, R.drawable.ic_ethernet, R.drawable.ic_ethernet_no_internet),
        )
        val names = listOf("QPR1 Wi-Fi", "QPR1 Wi-Fi error", "QPR1 cellular", "QPR1 cellular error", "Classic Wi-Fi", "Classic Wi-Fi error", "Classic cellular", "Classic cellular error", "Other states")
        val sheet = Bitmap.createBitmap(920, rows.size * 130, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheet)
        canvas.drawColor(Color.rgb(20, 28, 23))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 22f }
        var previousRow = emptyList<Bitmap>()
        for ((row, icons) in rows.withIndex()) {
            canvas.drawText(names[row], 16f, row * 130 + 70f, paint)
            val renderedRow = mutableListOf<Bitmap>()
            for ((column, resource) in icons.withIndex()) {
                val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
                val drawable = context.getDrawable(resource)!!.mutate()
                drawable.setTint(Color.WHITE)
                val scale = 80f / maxOf(drawable.intrinsicWidth, drawable.intrinsicHeight)
                val width = (drawable.intrinsicWidth * scale).toInt()
                val height = (drawable.intrinsicHeight * scale).toInt()
                drawable.setBounds((96 - width) / 2, (96 - height) / 2, (96 + width) / 2, (96 + height) / 2)
                drawable.draw(Canvas(bitmap))
                val pixels = IntArray(96 * 96)
                bitmap.getPixels(pixels, 0, 96, 0, 0, 96, 96)
                assertTrue("${names[row]} level $column rendered empty", pixels.any { Color.alpha(it) > 0 })
                if (row % 2 == 1) {
                    assertTrue("${names[row]} level $column missing error indicator", !bitmap.sameAs(previousRow[column]))
                }
                renderedRow.add(bitmap)
                canvas.drawBitmap(bitmap, 280f + column * 125, row * 130 + 10f, null)
                canvas.drawText(column.toString(), 319f + column * 125, row * 130 + 124f, paint)
            }
            previousRow = renderedRow
        }
        File(context.getExternalFilesDir(null), "icon-states.png").outputStream().use {
            sheet.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
