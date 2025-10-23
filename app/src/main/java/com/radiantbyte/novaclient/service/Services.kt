package com.radiantbyte.novaclient.service

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import android.graphics.PixelFormat
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.radiantbyte.novaclient.game.AccountManager
import com.radiantbyte.novaclient.game.GameSession
import com.radiantbyte.novaclient.game.ModuleManager
import com.radiantbyte.novaclient.game.module.visual.ESPModule
import com.radiantbyte.novaclient.model.CaptureModeModel
import com.radiantbyte.novaclient.overlay.OverlayManager
import com.radiantbyte.novaclient.render.RenderOverlayView
import com.radiantbyte.novarelay.NovaRelay
import com.radiantbyte.novarelay.NovaRelaySession
import com.radiantbyte.novarelay.address.NovaAddress
import com.radiantbyte.novarelay.config.EnhancedServerConfig
import com.radiantbyte.novarelay.definition.Definitions
import com.radiantbyte.novarelay.listener.AutoCodecPacketListener
import com.radiantbyte.novarelay.listener.GamingPacketHandler
import com.radiantbyte.novarelay.listener.OfflineLoginPacketListener
import com.radiantbyte.novarelay.listener.OnlineLoginPacketListener
import com.radiantbyte.novarelay.listener.NovaRelayPacketListener
import com.radiantbyte.novarelay.util.captureGamePacket
import com.radiantbyte.novaclient.util.ServerCompatUtils
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

@Suppress("MemberVisibilityCanBePrivate")
object Services {

    private val handler = Handler(Looper.getMainLooper())

    private var novaRelay: NovaRelay? = null
    private var thread: Thread? = null

    private var renderView: RenderOverlayView? = null
    private var windowManager: WindowManager? = null

    var isActive by mutableStateOf(false)

    // Packet file logging
    private var packetLogWriter: BufferedWriter? = null
    private val packetLogLock = Any()

    private fun startPacketLogging(context: Context) {
        try {
            val dir = context.getExternalFilesDir(null) ?: File(context.filesDir, "")
            val file = File(dir, "log.txt")
            packetLogWriter = BufferedWriter(FileWriter(file, true)).also { writer ->
                writer.write("=== Session start ${timestamp()} ===\n")
                writer.flush()
            }
            Log.d("Services", "Packet logging to: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("Services", "Failed to start packet logging: ${e.message}")
        }
    }

    private fun stopPacketLogging() {
        try {
            synchronized(packetLogLock) {
                packetLogWriter?.apply {
                    write("=== Session end ${timestamp()} ===\n")
                    flush()
                    close()
                }
                packetLogWriter = null
            }
        } catch (e: Exception) {
            Log.e("Services", "Failed to stop packet logging: ${e.message}")
        }
    }

    private fun logPacket(direction: String, packet: BedrockPacket) {
        val line = "[${timestamp()}] $direction ${packet.javaClass.simpleName} ${safeToString(packet)}"
        try {
            synchronized(packetLogLock) {
                packetLogWriter?.let { writer ->
                    writer.write(line)
                    writer.newLine()
                    writer.flush()
                }
            }
        } catch (e: Exception) {
            Log.e("Services", "Failed to write packet log: ${e.message}")
        }
    }

    private fun logLine(message: String) {
        val line = "[${timestamp()}] $message"
        try {
            synchronized(packetLogLock) {
                packetLogWriter?.let { writer ->
                    writer.write(line)
                    writer.newLine()
                    writer.flush()
                }
            }
        } catch (e: Exception) {
            Log.e("Services", "Failed to write log line: ${e.message}")
        }
    }

    private fun timestamp(): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
    private fun safeToString(packet: BedrockPacket): String = runCatching { packet.toString() }.getOrElse { "" }

    fun toggle(context: Context, captureModeModel: CaptureModeModel) {
        if (!isActive) {
            on(context, captureModeModel)
            return
        }

        off()
    }

    private fun clearNetworkCaches() {
        try {
            val inetAddressClass = Class.forName("java.net.InetAddress")
            val cacheField = inetAddressClass.getDeclaredField("addressCache")
            cacheField.isAccessible = true
            val cache = cacheField.get(null)
            val cacheMapField = cache.javaClass.getDeclaredField("cache")
            cacheMapField.isAccessible = true
            val cacheMap = cacheMapField.get(cache) as? java.util.Map<*, *>
            cacheMap?.clear()

            val negativeCacheField = inetAddressClass.getDeclaredField("negativeCache")
            negativeCacheField.isAccessible = true
            val negativeCache = negativeCacheField.get(null)
            val negativeCacheMapField = negativeCache.javaClass.getDeclaredField("cache")
            negativeCacheMapField.isAccessible = true
            val negativeCacheMap = negativeCacheMapField.get(negativeCache) as? java.util.Map<*, *>
            negativeCacheMap?.clear()

            System.gc()
            System.runFinalization()

            Log.d("Services", "Network caches cleared")
        } catch (e: Exception) {
            Log.e("Services", "Error clearing network caches: ${e.message}")
        }
    }

    private fun on(context: Context, captureModeModel: CaptureModeModel) {
        if (thread != null) {
            return
        }

        novaRelay?.let { relay ->
            try {
                if (relay.javaClass.methods.any { it.name == "stop" }) {
                    relay.javaClass.getMethod("stop").invoke(relay)
                }
            } catch (e: Exception) {
                Log.e("Services", "Error stopping existing NovaRelay: ${e.message}")
            }
        }
        novaRelay = null

        File(context.cacheDir, "token_cache.json")

        isActive = true
        handler.post {
            OverlayManager.show(context)
        }

        setupOverlay(context)

        // Start packet logging to Android/data/.../files/log.txt
        startPacketLogging(context)

        thread = thread(
            name = "NovaRelayThread",
            priority = Thread.MAX_PRIORITY
        ) {
            runCatching {
                ModuleManager.loadConfig()
            }.exceptionOrNull()?.let {
                it.printStackTrace()
                context.toast("Load configuration error: ${it.message}")
            }

            runCatching {
                Definitions.loadBlockPalette()
            }.exceptionOrNull()?.let {
                it.printStackTrace()
                context.toast("Load block palette error: ${it.message}")
            }

            val selectedAccount = AccountManager.selectedAccount

            runCatching {
                clearNetworkCaches()
                
                val remoteAddress = NovaAddress(
                    captureModeModel.serverHostName,
                    captureModeModel.serverPort
                )
                
                val serverConfig = getServerConfig(captureModeModel)
                novaRelay = if (captureModeModel.isProtectedServer() && captureModeModel.enableServerOptimizations) {
                    NovaRelay(
                        localAddress = NovaAddress("0.0.0.0", 19132),
                        serverConfig = serverConfig
                    ).capture(remoteAddress = remoteAddress) {
                        initModules(this)
                        // Packet logging listener first to capture packets before others possibly consume them
                        listeners.add(object : NovaRelayPacketListener {
                            override fun beforeClientBound(packet: BedrockPacket): Boolean {
                                logPacket("C->S", packet)
                                return false
                            }

                            override fun beforeServerBound(packet: BedrockPacket): Boolean {
                                logPacket("S->C", packet)
                                return false
                            }

                            override fun onDisconnect(reason: String) {
                                logLine("Disconnected: $reason")
                            }
                        })
                        listeners.add(AutoCodecPacketListener(this))
                        if (selectedAccount == null) {
                            listeners.add(OfflineLoginPacketListener(this))
                        } else {
                            listeners.add(OnlineLoginPacketListener(this, selectedAccount))
                        }
                        listeners.add(GamingPacketHandler(this))
                    }
                } else {
                    captureGamePacket(
                        localAddress = NovaAddress("0.0.0.0", 19132),
                        remoteAddress = remoteAddress
                    ) {
                        initModules(this)
                        // Packet logging listener first to capture packets before others possibly consume them
                        listeners.add(object : NovaRelayPacketListener {
                            override fun beforeClientBound(packet: BedrockPacket): Boolean {
                                logPacket("C->S", packet)
                                return false
                            }

                            override fun beforeServerBound(packet: BedrockPacket): Boolean {
                                logPacket("S->C", packet)
                                return false
                            }

                            override fun onDisconnect(reason: String) {
                                logLine("Disconnected: $reason")
                            }
                        })
                        listeners.add(AutoCodecPacketListener(this))
                        if (selectedAccount == null) {
                            listeners.add(OfflineLoginPacketListener(this))
                        } else {
                            listeners.add(OnlineLoginPacketListener(this, selectedAccount))
                        }
                        listeners.add(GamingPacketHandler(this))
                    }
                }
            }.exceptionOrNull()?.let {
                it.printStackTrace()
                context.toast("Start NovaRelay error: ${it.message}")
            }
        }
    }

    private fun off() {
        thread(name = "NovaRelayThread") {
            ModuleManager.saveConfig()

            novaRelay?.let { relay ->
                try {
                    relay.novaRelaySession?.client?.disconnect()
                    relay.novaRelaySession?.server?.disconnect()
                    
                    if (relay.javaClass.methods.any { it.name == "stop" }) {
                        relay.javaClass.getMethod("stop").invoke(relay)
                        Log.d("Services", "NovaRelay connection stopped successfully")
                    }
                } catch (e: Exception) {
                    Log.e("Services", "Error stopping NovaRelay: ${e.message}")
                    e.printStackTrace()
                }
            }
            novaRelay = null

            clearNetworkCaches()

            try {
                Thread.sleep(500)
            } catch (e: Exception) {
                Log.e("Services", "Error during cleanup delay: ${e.message}")
            }

            handler.post {
                OverlayManager.dismiss()
            }
            removeOverlay()
            isActive = false
            thread?.interrupt()
            thread = null

            Log.d("Services", "NovaRelay service stopped and cleaned up")
            stopPacketLogging()
        }
    }

    private fun Context.toast(message: String) {
        handler.post {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun initModules(novaRelaySession: NovaRelaySession) {
        val session = GameSession(novaRelaySession)
        novaRelaySession.listeners.add(session)

        for (module in ModuleManager.modules) {
            module.session = session
        }
        Log.e("Services", "Init session")
    }

    private fun setupOverlay(context: Context) {
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            }
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            format = PixelFormat.TRANSLUCENT

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alpha = 0.8f
                flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                setFitInsetsTypes(0)
                setFitInsetsSides(0)
            }
        }

        renderView = RenderOverlayView(context)
        ESPModule.setRenderView(renderView!!)

        handler.post {
            try {
                windowManager?.addView(renderView, params)
            } catch (e: Exception) {
                e.printStackTrace()
                context.toast("Failed to add overlay view: ${e.message}")
            }
        }
    }

    private fun removeOverlay() {
        renderView?.let { view ->
            windowManager?.removeView(view)
            renderView = null
        }
    }

    private fun getServerConfig(captureModeModel: CaptureModeModel): EnhancedServerConfig {
        return when (captureModeModel.serverConfigType) {
            ServerCompatUtils.ServerConfigType.FAST -> EnhancedServerConfig.FAST
            ServerCompatUtils.ServerConfigType.DEFAULT -> EnhancedServerConfig.DEFAULT
            ServerCompatUtils.ServerConfigType.AGGRESSIVE -> EnhancedServerConfig.AGGRESSIVE
            ServerCompatUtils.ServerConfigType.STANDARD -> EnhancedServerConfig.DEFAULT
        }
    }
}
