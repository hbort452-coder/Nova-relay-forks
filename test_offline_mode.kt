import com.radiantbyte.novarelay.NovaRelay
import com.radiantbyte.novarelay.listener.OfflineLoginPacketListener
import com.radiantbyte.novarelay.listener.AutoCodecPacketListener
import com.radiantbyte.novarelay.address.NovaAddress

fun main() {
    println("Starting NovaRelay in offline mode...")
    
    val relay = NovaRelay(
        localAddress = NovaAddress("0.0.0.0", 19132),
        serverConfig = com.radiantbyte.novarelay.config.EnhancedServerConfig.DEFAULT
    )
    
    relay.capture(
        remoteAddress = NovaAddress("localhost", 19133) // Test with local server
    ) {
        // Add offline login listener
        listeners.add(OfflineLoginPacketListener(this))
        // Add auto codec listener for protocol handling
        listeners.add(AutoCodecPacketListener(this))
        
        println("Offline mode configured. Relay is ready to accept connections.")
        println("Connect with a Minecraft Bedrock client to test offline mode.")
    }
    
    // Keep the program running
    Thread.sleep(Long.MAX_VALUE)
}