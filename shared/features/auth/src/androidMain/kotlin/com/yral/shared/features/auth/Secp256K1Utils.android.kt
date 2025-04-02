package com.yral.shared.features.auth

import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import kotlinx.serialization.json.Json
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey

actual fun createAuthPayload(): ByteArray {
    val keyPair = generateSecp256k1KeyPair()
    keyPair?.let {
        val jwk: JWK = ECKey.Builder(Curve.SECP256K1, keyPair.public as ECPublicKey)
            .privateKey(keyPair.private as ECPrivateKey)
            .build()
        val jwkJson = jwk.toJSONObject()
        val jwkMap = mapOf(
            "kty" to jwkJson["kty"].toString(),
            "crv" to jwkJson["crv"].toString(),
            "x" to jwkJson["x"].toString(),
            "y" to jwkJson["y"].toString(),
            "d" to jwkJson["d"].toString(),
        )
        val payload = mapOf("anonymous_identity" to jwkMap)
        return Json.encodeToString(payload).toByteArray()
    }
    throw Exception("Invalid key pair")
}

private fun generateSecp256k1KeyPair(): KeyPair? {
    val gen = KeyPairGenerator.getInstance("EC")
    gen.initialize(Curve.SECP256K1.toECParameterSpec())
    return gen.generateKeyPair()
}