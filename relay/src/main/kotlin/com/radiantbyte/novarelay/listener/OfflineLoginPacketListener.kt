package com.radiantbyte.novarelay.listener

import com.radiantbyte.novarelay.NovaRelaySession
import com.radiantbyte.novarelay.util.AuthUtilsOffline
import org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm
import org.cloudburstmc.protocol.bedrock.data.auth.AuthType
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
                try {
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
                println("Chain length: ${chain.size}")
                println("ExtraData keys: ${extraData.keys}")
                println("SkinData keys: ${skinData.keys}")

                    val jws = JsonWebSignature()
                    jws.compactSerialization = packet.clientJwt

                    skinData = JSONObject(JsonUtil.parseJson(jws.unverifiedPayload))
                    connectServer()
                    return true
                } catch (e: Exception) {
                    println("Failed to process offline login: ${e.message}")
                    e.printStackTrace()
                    novaRelaySession.server.disconnect("Failed to process offline login: ${e.message}")
                    return true
                }
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
            
            println("S->C NetworkSettingsPacket NetworkSettingsPacket(compressionThreshold=${packet.compressionThreshold}, compressionAlgorithm=${packet.compressionAlgorithm}, clientThrottleEnabled=${packet.clientThrottleEnabled}, clientThrottleThreshold=${packet.clientThrottleThreshold}, clientThrottleScalar=${packet.clientThrottleScalar})")
            
            // Log if compression settings don't match expected values
            if (packet.compressionThreshold != 0) {
                println("WARNING: Expected compression threshold 0, got ${packet.compressionThreshold}")
            }

            try {
                val extraDataValue = extraData
                val chainValue = chain
                val skinDataValue = skinData
                
                if (extraDataValue == null || chainValue == null || skinDataValue == null) {
                    throw Exception("Missing authentication data for offline mode")
                }

                val chain = AuthUtilsOffline.fetchOfflineChain(keyPair, extraDataValue, chainValue)
                val skinData = AuthUtilsOffline.fetchOfflineSkinData(keyPair, skinDataValue)

                val loginPacket = LoginPacket()
                loginPacket.protocolVersion = novaRelaySession.server.codec.protocolVersion
                val authPayload = CertificateChainPayload(chain, AuthType.SELF_SIGNED)
                loginPacket.authPayload = authPayload
                loginPacket.clientJwt = skinData
                novaRelaySession.serverBoundImmediately(loginPacket)

                println("Login success")
            } catch (e: Throwable) {
                println("Login failed: ${e.message}")
                e.printStackTrace()
                novaRelaySession.clientBound(DisconnectPacket().apply {
                    kickMessage = "Offline authentication failed: ${e.message}"
                })
            }

            return true
        }
        if (packet is ServerToClientHandshakePacket) {
            try {
                val parts = packet.jwt.split(".")
                if (parts.size != 3) {
                    throw Exception("Invalid JWT format")
                }

                val headerJson = String(java.util.Base64.getUrlDecoder().decode(parts[0]))
                val payloadJson = String(java.util.Base64.getUrlDecoder().decode(parts[1]))

                val header = JSONObject(JsonUtil.parseJson(headerJson))
                val payload = JSONObject(JsonUtil.parseJson(payloadJson))

                val x5u = header.get("x5u") as? String ?: throw Exception("Missing x5u in header")
                val serverKey = EncryptionUtils.parseKey(x5u)

                val saltString = payload.get("salt") as? String ?: throw Exception("Missing salt in payload")
                val salt = java.util.Base64.getDecoder().decode(saltString)

                val key = EncryptionUtils.getSecretKey(
                    keyPair.private,
                    serverKey,
                    salt
                )

                novaRelaySession.client!!.enableEncryption(key)
                println("Encryption enabled successfully (offline)")

                novaRelaySession.serverBoundImmediately(ClientToServerHandshakePacket())
            } catch (e: Exception) {
                println("Handshake failed (offline): ${e.message}")
                e.printStackTrace()
                novaRelaySession.server.disconnect("Handshake failed: ${e.message}")
                return true
            }
            return true
        }
        return super.beforeServerBound(packet)
    }

    private fun connectServer() {
        try {
            novaRelaySession.novaRelay.connectToServer {
                println("Connected to server")

                val packet = RequestNetworkSettingsPacket()
                packet.protocolVersion = novaRelaySession.server.codec.protocolVersion
                novaRelaySession.serverBoundImmediately(packet)
                println("Forwarded RequestNetworkSettings to upstream with protocol=${packet.protocolVersion}")
            }
        } catch (e: Exception) {
            println("Failed to connect to server: ${e.message}")
            e.printStackTrace()
            novaRelaySession.server.disconnect("Failed to connect to server: ${e.message}")
        }
    }

}
