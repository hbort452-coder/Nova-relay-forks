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
    private val logger: ((String) -> Unit)? = null
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
                logger?.invoke("Handle offline login data")

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
                val msg = "Compression threshold set to $threshold"
                println(msg)
                logger?.invoke(msg)
            } else {
                novaRelaySession.client!!.setCompression(PacketCompressionAlgorithm.NONE)
                val msg = "Compression threshold set to 0"
                println(msg)
                logger?.invoke(msg)
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
        if (packet is ServerToClientHandshakePacket) {
            try {
                val parts = packet.jwt.split(".")
                if (parts.size != 3) {
                    throw Exception("Invalid JWT format")
                }

                val headerJson = String(Base64.getUrlDecoder().decode(parts[0]))
                val payloadJson = String(Base64.getUrlDecoder().decode(parts[1]))

                val header = JSONObject(JsonUtil.parseJson(headerJson))
                val payload = JSONObject(JsonUtil.parseJson(payloadJson))

                val x5u = header.get("x5u") as? String ?: throw Exception("Missing x5u in header")
                val serverKey = EncryptionUtils.parseKey(x5u)

                val saltString = payload.get("salt") as? String ?: throw Exception("Missing salt in payload")
                val salt = Base64.getDecoder().decode(saltString)

                val key = EncryptionUtils.getSecretKey(
                    keyPair.private,
                    serverKey,
                    salt
                )

                novaRelaySession.client!!.enableEncryption(key)
                val msg = "Encryption enabled successfully (offline)"
                println(msg)
                logger?.invoke(msg)

                novaRelaySession.serverBoundImmediately(ClientToServerHandshakePacket())
            } catch (e: Exception) {
                val err = "Handshake failed (offline): ${e.message}"
                println(err)
                logger?.invoke(err)
                e.printStackTrace()
                novaRelaySession.server.disconnect("Handshake failed: ${e.message}")
                return true
            }
            return true
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
