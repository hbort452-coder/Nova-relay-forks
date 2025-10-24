package com.radiantbyte.novarelay.listener

import com.radiantbyte.novarelay.NovaRelaySession
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition
import org.cloudburstmc.protocol.bedrock.data.definitions.SimpleNamedDefinition
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
import org.cloudburstmc.protocol.bedrock.packet.CameraPresetsPacket
import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket
import org.cloudburstmc.protocol.common.NamedDefinition
import org.cloudburstmc.protocol.common.SimpleDefinitionRegistry

@Suppress("MemberVisibilityCanBePrivate")
class GamingPacketHandler(
    val novaRelaySession: NovaRelaySession
) : NovaRelayPacketListener {

    override fun beforeServerBound(packet: BedrockPacket): Boolean {
        if (packet is StartGamePacket) {
            println("Start game, setting definitions")
            // Update definitions from server
            novaRelaySession.client!!.peer.codecHelper.itemDefinitions = SimpleDefinitionRegistry.builder<ItemDefinition>()
                .addAll(packet.itemDefinitions)
                .build()

            if (packet.isBlockNetworkIdsHashed) {
                // Use hashed block definitions if needed
                println("Using hashed block definitions")
            } else {
                // Use regular block definitions
                println("Using regular block definitions")
            }
        }
        if (packet is CameraPresetsPacket) {
            println("Camera presets")
            val cameraDefinitions =
                SimpleDefinitionRegistry.builder<NamedDefinition>()
                    .addAll(List(packet.presets.size) {
                        SimpleNamedDefinition(packet.presets[it].identifier, it)
                    })
                    .build()

            novaRelaySession.client!!.peer.codecHelper.cameraPresetDefinitions = cameraDefinitions
        }
        return false
    }
}