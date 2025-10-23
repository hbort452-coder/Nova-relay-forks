package com.radiantbyte.novarelay.util

import com.radiantbyte.novarelay.address.NovaAddress
import org.jose4j.json.internal.json_simple.JSONObject
import org.jose4j.jws.JsonWebSignature
import org.jose4j.jwt.JwtClaims
import org.jose4j.jwx.HeaderParameterNames
import java.security.KeyPair
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import org.jose4j.jwt.NumericDate

object AuthUtilsOffline {

    @OptIn(ExperimentalEncodingApi::class)
    fun fetchOfflineChain(keyPair: KeyPair, extraData: JSONObject, chain: List<String>): List<String> {
        val publicKeyBase64: String = Base64.encode(keyPair.public.encoded)

        val timestamp = System.currentTimeMillis()
        val nbf = java.util.Date(timestamp - TimeUnit.SECONDS.toMillis(1))
        val exp = java.util.Date(timestamp + TimeUnit.DAYS.toMillis(1))

        val claimsSet = JwtClaims()
        claimsSet.notBefore = NumericDate.fromMilliseconds(nbf.time)
        claimsSet.expirationTime = NumericDate.fromMilliseconds(exp.time)
        // Use the current time for issuedAt, not expiration
        claimsSet.issuedAt = NumericDate.fromMilliseconds(timestamp)
        claimsSet.issuer = "self"
        claimsSet.setClaim("certificateAuthority", true)
        claimsSet.setClaim("extraData", extraData)
        claimsSet.setClaim("identityPublicKey", publicKeyBase64)

        val jws = JsonWebSignature()
        jws.payload = claimsSet.toJson()
        jws.key = keyPair.private
        jws.algorithmHeaderValue = "ES384"
        jws.setHeader(HeaderParameterNames.X509_URL, publicKeyBase64)

        // Prefer sending the self-signed CA token first. If the client's identity JWT is present,
        // append it to help servers that expect it; omit Mojang/XBL links for offline mode.
        return listOf(jws.compactSerialization)
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun fetchOfflineSkinData(
        keyPair: KeyPair,
        skinData: JSONObject,
        displayName: String?,
        serverAddress: NovaAddress?
    ): String {
        val publicKeyBase64: String = Base64.encode(keyPair.public.encoded)

        val overrides = HashMap<String, Any>()
        overrides["DeviceOS"] = 1 // Android
        overrides["DeviceId"] = UUID.randomUUID().toString()
        displayName?.let { overrides["ThirdPartyName"] = it }
        serverAddress?.let { overrides["ServerAddress"] = "${it.hostName}:${it.port}" }
        skinData.putAll(overrides)

        val jws = JsonWebSignature()
        jws.algorithmHeaderValue = "ES384"
        jws.setHeader(HeaderParameterNames.X509_URL, publicKeyBase64)
        jws.payload = skinData.toJSONString()
        jws.key = keyPair.private

        return jws.compactSerialization
    }

}
