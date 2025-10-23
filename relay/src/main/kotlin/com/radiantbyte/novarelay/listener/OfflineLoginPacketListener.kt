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
import java.util.Base64


@Suppress("MemberVisibilityCanBePrivate")
class OfflineLoginPacketListener(
    val novaRelaySession: NovaRelaySession,
    val keyPair: KeyPair = DefaultKeyPair,
    private val logger: ((String) -> Unit)? = null,
    private val passthroughLogin: Boolean = true
) : NovaRelayPacketListener {

    companion object {

        val DefaultKeyPair: KeyPair = EncryptionUtils.createKeyPair()

    }

    private var chain: List<String>? = null
    private var extraData: JSONObject? = null
    private var skinData: JSONObject? = null
    private var originalLoginPacket: LoginPacket? = null

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
                logger?.invoke("Handle offline login data")

                val jws = JsonWebSignature()
                jws.compactSerialization = packet.clientJwt

                skinData = JSONObject(JsonUtil.parseJson(jws.unverifiedPayload))

                // Store original login for passthrough
                originalLoginPacket = if (passthroughLogin) packet else null

                connectServer()

                // If passthrough, we will forward after receiving NetworkSettings from server
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
                val msg = "Compression threshold set to $threshold"
                println(msg)
                logger?.invoke(msg)
            } else {
                novaRelaySession.client!!.setCompression(PacketCompressionAlgorithm.NONE)
                val msg = "Compression threshold set to 0"
                println(msg)
                logger?.invoke(msg)
            }

            // If passthrough mode, forward client's original LoginPacket unchanged
            originalLoginPacket?.let { original ->
                runCatching {
                    novaRelaySession.serverBoundImmediately(original)
                    println("Forwarded original LoginPacket to server (passthrough)")
                    logger?.invoke("Forwarded original LoginPacket to server (passthrough)")
                }.onFailure { e ->
                    novaRelaySession.clientBound(DisconnectPacket().apply { kickMessage = e.toString() })
                    val err = "Failed to forward original LoginPacket: ${e.message}"
                    println(err)
                    logger?.invoke(err)
                }
                return true
            }

            // Fallback to offline generated login if passthrough not enabled or no original packet
            if (!passthroughLogin) {
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
                    logger?.invoke("Login success")
                } catch (e: Throwable) {
                    novaRelaySession.clientBound(DisconnectPacket().apply {
                        kickMessage = e.toString()
                    })
                    val err = "Login failed: $e"
                    println(err)
                    logger?.invoke(err)
                }
                return true
            }

            return false
        }
        if (packet is ServerToClientHandshakePacket) {
            // In passthrough mode, do not perform encryption here; let the client and server complete handshake.
            // This listener will not consume the packet.
            return false
        }
        return super.beforeServerBound(packet)
    }

    private fun connectServer() {
        novaRelaySession.novaRelay.connectToServer {
            println("Connected to server")
            logger?.invoke("Connected to server")

            val packet = RequestNetworkSettingsPacket()
            packet.protocolVersion = novaRelaySession.server.codec.protocolVersion
            novaRelaySession.serverBoundImmediately(packet)
        }
    }

}
