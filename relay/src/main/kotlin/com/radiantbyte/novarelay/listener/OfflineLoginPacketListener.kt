package com.radiantbyte.novarelay.listener

import com.radiantbyte.novarelay.NovaRelaySession
import com.radiantbyte.novarelay.util.AuthUtilsOffline
import org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm
import org.cloudburstmc.protocol.bedrock.data.auth.CertificateChainPayload
import org.cloudburstmc.protocol.bedrock.packet.*
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils
import org.cloudburstmc.protocol.bedrock.util.JsonUtils
import org.jose4j.json.JsonUtil
import org.jose4j.json.internal.json_simple.JSONObject
import org.jose4j.jws.JsonWebSignature
import java.security.KeyPair


@Suppress("MemberVisibilityCanBePrivate")
class OfflineLoginPacketListener(
    val novaRelaySession: NovaRelaySession,
    val keyPair: KeyPair = DefaultKeyPair
) : NovaRelayPacketListener {

    companion object {

        val DefaultKeyPair: KeyPair = EncryptionUtils.createKeyPair()

    }

    private var chain: List<String>? = null

    private var extraData: JSONObject? = null

    private var skinData: JSONObject? = null

    override fun beforeClientBound(packet: BedrockPacket): Boolean {
        if (packet is LoginPacket) {
            val authPayload = packet.authPayload
            if (authPayload is CertificateChainPayload) {
                chain = authPayload.chain
                extraData =
                    JSONObject(
                        JsonUtils.childAsType(
                            EncryptionUtils.validateChain(chain).rawIdentityClaims(),
                            "extraData",
                            Map::class.java
                        )
                    )

                println("Handle offline login data")

                val jws = JsonWebSignature()
                jws.compactSerialization = packet.clientJwt

                skinData = JSONObject(JsonUtil.parseJson(jws.unverifiedPayload))
                connectServer()
                return true
            }
        }
        return false
    }

    override fun beforeServerBound(packet: BedrockPacket): Boolean {
        if (packet is NetworkSettingsPacket) {
            val threshold = packet.compressionThreshold
            if (threshold > 0) {
                novaRelaySession.client!!.setCompression(packet.compressionAlgorithm)
                println("Compression threshold set to $threshold")
            } else {
                novaRelaySession.client!!.setCompression(PacketCompressionAlgorithm.NONE)
                println("Compression threshold set to 0")
            }

            try {
                val chain = AuthUtilsOffline.fetchOfflineChain(keyPair, extraData!!, chain!!)
                val skinData = AuthUtilsOffline.fetchOfflineSkinData(keyPair, skinData!!)

                val loginPacket = LoginPacket()
                loginPacket.protocolVersion = novaRelaySession.server.codec.protocolVersion
                val authPayload = CertificateChainPayload(chain)
                loginPacket.authPayload = authPayload
                loginPacket.clientJwt = skinData
                novaRelaySession.serverBoundImmediately(loginPacket)

                println("Login success")
            } catch (e: Throwable) {
                novaRelaySession.clientBound(DisconnectPacket().apply {
                    kickMessage = e.toString()
                })
                println("Login failed: $e")
            }

            return true
        }
        return super.beforeServerBound(packet)
    }

    private fun connectServer() {
        novaRelaySession.novaRelay.connectToServer {
            println("Connected to server")

            val packet = RequestNetworkSettingsPacket()
            packet.protocolVersion = novaRelaySession.server.codec.protocolVersion
            novaRelaySession.serverBoundImmediately(packet)
        }
    }

}
