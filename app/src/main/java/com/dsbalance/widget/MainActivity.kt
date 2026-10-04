package com.dsbalance.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import java.math.BigDecimal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 保存流程要在设置页关掉之后照样跑完刷新，所以用进程级作用域，不随页面销毁取消喵
private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // targetSdk 35 起系统强制 edge-to-edge，布局靠 fitsSystemWindows 躲开状态栏喵
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        val store = BalanceStore(this)
        // 排一次闹钟做自愈：桌面有组件就接回可能断掉的刷新链，没有的话链条首环会自己停喵
        RefreshScheduler.schedule(this)
        // 从组件长按菜单（重新配置）或初次添加进来时带 widget id，保存后要回传结果喵
        val appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val apiKeyInput = findViewById<EditText>(R.id.api_key)
        val thresholdInput = findViewById<EditText>(R.id.threshold)
        val changeMascotButton = findViewById<Button>(R.id.change_mascot)
        val resetMascotButton = findViewById<Button>(R.id.reset_mascot)

        apiKeyInput.setText(store.apiKey)
        thresholdInput.setText(
            store.threshold.takeIf { it > BigDecimal.ZERO }?.toPlainString().orEmpty(),
        )
        var hasCustomMascot = mascotFile(this).exists()
        resetMascotButton.visibility = if (hasCustomMascot) View.VISIBLE else View.GONE

        val pickMascot = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                appScope.launch {
                    val ok = withContext(Dispatchers.IO) { saveMascotFromUri(this@MainActivity, uri) }
                    Toast.makeText(
                        this@MainActivity,
                        if (ok) "表情包已更换" else "图片处理失败",
                        Toast.LENGTH_SHORT,
                    ).show()
                    if (ok) {
                        hasCustomMascot = true
                        resetMascotButton.visibility = View.VISIBLE
                        BalanceWidget.updateAll(this@MainActivity)
                    }
                }
            }
        }
        changeMascotButton.setOnClickListener {
            pickMascot.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        }
        resetMascotButton.setOnClickListener {
            appScope.launch {
                val ok = withContext(Dispatchers.IO) { resetMascot(this@MainActivity) }
                if (ok) {
                    hasCustomMascot = false
                    resetMascotButton.visibility = View.GONE
                    BalanceWidget.updateAll(this@MainActivity)
                    Toast.makeText(this@MainActivity, "已恢复默认", Toast.LENGTH_SHORT).show()
                }
            }
        }

        findViewById<Button>(R.id.save).setOnClickListener {
            store.apiKey = apiKeyInput.text.toString()
            store.threshold = thresholdInput.text.toString().toBigDecimalOrNull() ?: BigDecimal.ZERO
            appScope.launch {
                try {
                    fetchAndStore(this@MainActivity)
                    if (BalanceWidget.updateAll(this@MainActivity)) {
                        RefreshScheduler.schedule(this@MainActivity)
                    }
                    Toast.makeText(this@MainActivity, "已保存并刷新", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@MainActivity,
                        "刷新失败：${e.message}",
                        Toast.LENGTH_SHORT,
                    ).show()
                } finally {
                    // 从组件配置/编辑流程进来时要等刷新真正跑完再结束页面，
                    // 否则协程会被随页面一起取消，组件根本来不及更新喵
                    if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                        setResult(
                            RESULT_OK,
                            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
                        )
                        finish()
                    }
                }
            }
        }
    }
}
