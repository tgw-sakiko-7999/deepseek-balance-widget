package com.dsbalance.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import java.io.File
import kotlin.math.ceil
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import java.io.IOException
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BalanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BalanceWidget

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        RefreshScheduler.schedule(context)
    }
}

object BalanceWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetSnapshot.read(BalanceStore(context))
        // 渲染时读系统深浅色，背景和文字位图用同一套配色；切主题后下次刷新（点击或≤30分钟）整体换装喵
        val palette = if (isNight(context)) DARK else LIGHT
        val balanceImage = snapshot.balance?.let {
            val text = "${currencySymbol(snapshot.currency)} ${it.toPlainString()}"
            val color = if (snapshot.belowThreshold) palette.red else palette.primary
            ImageProvider(renderTextBitmap(context, text, color, R.font.jetbrains_mono_bold, 22f))
        }
        val updatedImage = if (snapshot.balance != null && snapshot.error == null) {
            val text = "更新于 " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(snapshot.updated))
            ImageProvider(renderTextBitmap(context, text, palette.secondary, R.font.jetbrains_mono_regular, 10f))
        } else {
            null
        }
        val mascot = loadMascot(context)
        provideContent { WidgetContent(snapshot, balanceImage, updatedImage, palette, mascot) }
    }

    /** 通知桌面所有实例重渲染；返回桌面是否还有实例，供调度判断要不要继续跑闹钟链喵 */
    suspend fun updateAll(context: Context): Boolean {
        val manager = GlanceAppWidgetManager(context)
        val ids = manager.getGlanceIds(this@BalanceWidget::class.java)
        ids.forEach { update(context, it) }
        return ids.isNotEmpty()
    }

    /** 只查桌面还有没有本组件实例，不触发渲染喵 */
    suspend fun hasWidgets(context: Context): Boolean =
        GlanceAppWidgetManager(context).getGlanceIds(this@BalanceWidget::class.java).isNotEmpty()

    // 自定义表情包的解码结果按文件时间戳缓存，免得每分钟重渲染都重新解码一次喵
    private var mascotBitmap: Bitmap? = null
    private var mascotStamp: Long = 0

    private fun loadMascot(context: Context): ImageProvider {
        val file = mascotFile(context)
        if (!file.exists()) return ImageProvider(R.drawable.widget_mascot)
        val stamp = file.lastModified()
        val cached = mascotBitmap
        if (cached == null || stamp != mascotStamp) {
            mascotBitmap = BitmapFactory.decodeFile(file.absolutePath)
            mascotStamp = stamp
        }
        return mascotBitmap?.let { ImageProvider(it) } ?: ImageProvider(R.drawable.widget_mascot)
    }

    private fun isNight(context: Context): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    private fun renderTextBitmap(
        context: Context,
        text: String,
        color: Int,
        fontRes: Int,
        spSize: Float,
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = ResourcesCompat.getFont(context, fontRes) ?: Typeface.MONOSPACE
            textSize = spSize * density
            this.color = color
        }
        val metrics = paint.fontMetrics
        val width = ceil(paint.measureText(text) + 2f).toInt().coerceAtLeast(1)
        val height = ceil(metrics.descent - metrics.ascent + 2f).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawText(text, 1f, -metrics.ascent + 1f, paint)
        return bitmap
    }
}

// 深浅两套配色；红/灰在深色下提亮保证对比喵
private data class Palette(val bg: Int, val primary: Int, val secondary: Int, val red: Int)

private val LIGHT = Palette(
    bg = 0xFFFFFFFF.toInt(),
    primary = 0xFF1B1B1B.toInt(),
    secondary = 0xFF757575.toInt(),
    red = 0xFFC62828.toInt(),
)

private val DARK = Palette(
    bg = 0xFF1C1B1F.toInt(),
    primary = 0xFFEDEDED.toInt(),
    secondary = 0xFF9E9E9E.toInt(),
    red = 0xFFFF8A80.toInt(),
)

/** 组合进 UI 前的一次性快照，避免在 composable 里读 SharedPreferences 喵 */
data class WidgetSnapshot(
    val balance: BigDecimal?,
    val currency: String?,
    val updated: Long,
    val error: String?,
    val belowThreshold: Boolean,
) {
    companion object {
        fun read(store: BalanceStore): WidgetSnapshot {
            val balance = store.lastBalance
            return WidgetSnapshot(
                balance = balance,
                currency = store.lastCurrency,
                updated = store.lastUpdated,
                error = store.lastError,
                belowThreshold = balance != null &&
                    store.threshold > BigDecimal.ZERO && balance < store.threshold,
            )
        }
    }
}

suspend fun fetchAndStore(context: Context): Unit = withContext(Dispatchers.IO) {
    val store = BalanceStore(context)
    if (store.apiKey.isBlank()) {
        store.lastError = "请先在应用里填写 API Key"
        return@withContext
    }
    try {
        val info = BalanceApi.fetch(store.apiKey)
        store.lastBalance = info.totalBalance
        store.lastCurrency = info.currency
        store.lastUpdated = System.currentTimeMillis()
        store.lastError = null
    } catch (e: ApiException) {
        store.lastError = e.message
    } catch (e: IOException) {
        store.lastError = "网络错误：${e.message?.take(60)}"
    } catch (e: Exception) {
        store.lastError = "${e.javaClass.simpleName}：${e.message?.take(60)}"
    }
}

class RefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        fetchAndStore(context)
        BalanceWidget.update(context, glanceId)
        // 手点刷新顺带把可能断掉的闹钟链接回来喵
        RefreshScheduler.schedule(context)
    }
}

// 自定义表情包存在应用私有目录，缺省用打包资源；贴图规则：高=组件高、贴右、CENTER_CROP 喵
internal fun mascotFile(context: Context): File = File(context.filesDir, "mascot.png")

// 自定义表情包统一存 WebP：同画质下比 PNG 小一个数量级，省用户存储；
// API 30 起 WEBP 被拆成 LOSSY/LOSSLESS，但老枚举在所有版本都还能用喵
@Suppress("DEPRECATION")
private val MASCOT_FORMAT = Bitmap.CompressFormat.WEBP

fun saveMascotFromUri(context: Context, uri: Uri): Boolean = try {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        false
    } else {
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 512) sample *= 2
        val src = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }
        if (src == null) {
            false
        } else {
            mascotFile(context).outputStream().use { out ->
                src.compress(MASCOT_FORMAT, 90, out)
            }
            true
        }
    }
} catch (e: Exception) {
    false
}

fun resetMascot(context: Context): Boolean = mascotFile(context).delete()

// 中文提示行用系统字体（JetBrains Mono 无中文字形），余额与时间两行走位图；表情包贴右下角喵
@Composable
private fun WidgetContent(
    s: WidgetSnapshot,
    balanceImage: ImageProvider?,
    updatedImage: ImageProvider?,
    palette: Palette,
    mascot: ImageProvider,
) {
    val size = LocalSize.current
    // 常规宽高比下表情包高度=组件高（上下右三边贴外沿）；极端窄卡按 0.6 宽兜底避免挤没文字喵
    val side = if (size.height > 0.dp && size.width > 0.dp) {
        minOf(size.height, size.width * 0.6f)
    } else {
        64.dp
    }
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(palette.bg)))
            .cornerRadius(14.dp)
            .clickable(actionRunCallback<RefreshAction>()),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Column(
            modifier = GlanceModifier.defaultWeight().padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            when {
                s.error != null -> Text(
                    "⚠ ${s.error}",
                    maxLines = 2,
                    style = TextStyle(fontSize = 13.sp, color = ColorProvider(Color(palette.red))),
                )
                balanceImage != null -> Image(
                    provider = balanceImage,
                    contentDescription = "当前余额",
                )
                else -> Text(
                    "点击拉取余额",
                    style = TextStyle(fontSize = 14.sp, color = ColorProvider(Color(palette.secondary))),
                )
            }
            if (updatedImage != null) {
                Image(provider = updatedImage, contentDescription = "更新时间")
            }
        }
        Image(
            provider = mascot,
            contentDescription = null,
            // 高度直接撑满行高，避免 LocalSize 与桌面实际尺寸的误差造成上下留白喵
            modifier = GlanceModifier.fillMaxHeight().width(side),
            contentScale = ContentScale.Crop,
        )
    }
}

internal fun currencySymbol(code: String?): String = when (code) {
    "CNY" -> "¥"
    "USD" -> "$"
    "EUR" -> "€"
    null -> "¥"
    else -> code
}
