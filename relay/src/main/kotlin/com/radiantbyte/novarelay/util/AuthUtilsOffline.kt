package com.radiantbyte.novarelay.util

import org.jose4j.json.internal.json_simple.JSONObject
import org.jose4j.jws.JsonWebSignature
import org.jose4j.jwt.JwtClaims
import org.jose4j.jwx.HeaderParameterNames
import java.security.KeyPair
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

        // For offline mode, the server expects a single self-signed identity token (chain length = 1).
        // Sending any part of the original chain will cause validation to fail ("broken chain").
        return listOf(jws.compactSerialization)
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun fetchOfflineSkinData(keyPair: KeyPair, skinData: JSONObject): String {
        val publicKeyBase64: String = Base64.encode(keyPair.public.encoded)

        val jws = JsonWebSignature()
        jws.algorithmHeaderValue = "ES384"
        jws.setHeader(HeaderParameterNames.X509_URL, publicKeyBase64)
        jws.payload = skinData.toJSONString()
        jws.key = keyPair.private

        return jws.compactSerialization
    }

}
