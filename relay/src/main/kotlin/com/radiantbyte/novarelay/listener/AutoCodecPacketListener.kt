package com.radiantbyte.novarelay.listener

import com.radiantbyte.novarelay.NovaRelay
import com.radiantbyte.novarelay.NovaRelaySession
import com.radiantbyte.novarelay.definition.Definitions
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec
import org.cloudburstmc.protocol.bedrock.codec.v729.serializer.InventoryContentSerializer_v729
import org.cloudburstmc.protocol.bedrock.codec.v729.serializer.InventorySlotSerializer_v729
import org.cloudburstmc.protocol.bedrock.data.EncodingSettings
import org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm
import org.cloudburstmc.protocol.bedrock.packet.*

@Suppress("MemberVisibilityCanBePrivate")
class AutoCodecPacketListener(
    val novaRelaySession: NovaRelaySession,
    val patchCodec: Boolean = true,
    private val logger: ((String) -> Unit)? = null
) : NovaRelayPacketListener {

    companion object {

        private val protocols = run {
            val stream = AutoCodecPacketListener::class.java.getResourceAsStream("/protocol_mapping.txt")
                ?: AutoCodecPacketListener::class.java.classLoader.getResourceAsStream("protocol_mapping.txt")
            if (stream != null) {
                stream.bufferedReader().use { reader ->
                    reader.readLines().map { version -> version.toInt() }
                }
            } else {
                emptyList()
            }
        }

        private fun fetchCodecIfClosest(
            protocolVersion: Int
        ): BedrockCodec {
            val closestProtocolVersion = protocols.findLast { currentProtocolVersion ->
                protocolVersion >= currentProtocolVersion
            } ?: NovaRelay.DefaultCodec.protocolVersion

            val bedrockCodecClass = Class.forName("org.cloudburstmc.protocol.bedrock.codec.v$closestProtocolVersion.Bedrock_v$closestProtocolVersion")
            val bedrockCodecField = bedrockCodecClass.getDeclaredField("CODEC")
            bedrockCodecField.isAccessible = true

            return bedrockCodecField.get(null) as BedrockCodec
        }

    }

    private fun patchCodecIfNeeded(codec: BedrockCodec): BedrockCodec {
        return if (patchCodec && codec.protocolVersion > 729) {
            codec.toBuilder()
                .updateSerializer(InventoryContentPacket::class.java, InventoryContentSerializer_v729.INSTANCE)
                .updateSerializer(InventorySlotPacket::class.java, InventorySlotSerializer_v729.INSTANCE)
                .build()
        } else {
            codec
        }
    }

    private var serverConnectStarted: Boolean = false

    override fun beforeClientBound(packet: BedrockPacket): Boolean {
        if (packet is RequestNetworkSettingsPacket) {
            try {
                val protocolVersion = packet.protocolVersion
                val bedrockCodec = patchCodecIfNeeded(fetchCodecIfClosest(protocolVersion))
                val msgCodec = "Fetched bedrock codec: ${bedrockCodec.protocolVersion} for protocol: $protocolVersion"
                println(msgCodec)
                logger?.invoke(msgCodec)

                novaRelaySession.server.codec = bedrockCodec
                novaRelaySession.server.peer.codecHelper.apply {
                    itemDefinitions = Definitions.itemDefinitions
                    blockDefinitions = Definitions.blockDefinitions
                    cameraPresetDefinitions = Definitions.cameraPresetDefinitions
                    encodingSettings = EncodingSettings.builder()
                        .maxListSize(Int.MAX_VALUE)
                        .maxByteArraySize(Int.MAX_VALUE)
                        .maxNetworkNBTSize(Int.MAX_VALUE)
                        .maxItemNBTSize(Int.MAX_VALUE)
                        .maxStringLength(Int.MAX_VALUE)
                        .build()
                }

                val networkSettingsPacket = NetworkSettingsPacket()
                networkSettingsPacket.compressionThreshold = 0
                networkSettingsPacket.compressionAlgorithm = PacketCompressionAlgorithm.ZLIB

                novaRelaySession.clientBoundImmediately(networkSettingsPacket)
                novaRelaySession.server.setCompression(PacketCompressionAlgorithm.ZLIB)
                val msgCompress = "Sent NetworkSettings(ZLIB, threshold=0) and enabled server compression"
                println(msgCompress)
                logger?.invoke(msgCompress)

                // MuCuteRelay approach: do not initiate upstream here; let login listeners drive it.
            } catch (e: Exception) {
                val err = "Failed to process network settings: ${e.message}"
                println(err)
                logger?.invoke(err)
                e.printStackTrace()
                novaRelaySession.server.disconnect("Failed to setup network settings: ${e.message}")
                return true
            }
            return true
        }
        return false
    }

}