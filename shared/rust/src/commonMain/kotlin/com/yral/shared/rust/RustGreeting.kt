package com.yral.shared.rust

import com.yral.shared.uniffi.generated.GetPostsOfUserProfileError
import com.yral.shared.uniffi.generated.IndividualUserService
import com.yral.shared.uniffi.generated.Result12
import kotlinx.serialization.json.Json

class RustGreeting {
    private var individualUserService: IndividualUserService = IndividualUserService(
        principal = principal,
        identityData = Json.encodeToString(identityData).toByteArray(Charsets.UTF_8),
    )

    fun greet(name: String): String {
        return "Hello, ${name}! from rustLib"
    }

    suspend fun getPostsOfThisUserProfileWithPaginationCursor(): Result12 {
        try {
            val page = 0UL
            val pageSize = 10UL
            return individualUserService.getPostsOfThisUserProfileWithPaginationCursor(
                page,
                pageSize,
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return Result12.Err(GetPostsOfUserProfileError.INVALID_BOUNDS_PASSED)
        }
    }

    companion object {
        val principal = "4cgid-nqaaa-aaaah-ql7za-cai"
        val identityData = KIdentityData(
            from_key = listOf(
                48, 86, 48, 16, 6, 7, 42, 134, 72, 206, 61, 2, 1, 6, 5, 43, 129, 4, 0, 10,
                3, 66, 0, 4, 41, 76, 202, 6, 126, 100, 246, 217, 245, 13, 113, 176, 101, 226,
                83, 71, 38, 203, 200, 39, 100, 144, 27, 89, 88, 132, 252, 211, 70, 13, 107,
                235, 39, 80, 103, 41, 1, 94, 209, 177, 172, 230, 206, 48, 233, 63, 62, 143,
                119, 18, 23, 20, 87, 126, 173, 252, 219, 81, 184, 91, 117, 4, 9, 46
            ).toIntArray(),
            to_secret = Secret(
                kty = "EC",
                crv = "secp256k1",
                x = "NBMz_7LJmLi2hL87Y-4DaYDKNnzil7nuO8elqonnP1c",
                y = "4NMjBgegSLiKalEMz2FroyJnHJPzyJuCz2Uo2FmxwYo",
                d = "HHDQbPzrbLoCwloB7EmXwJcOnczhhI9-gX4W8cXhuVo"
            ),
            delegation_chain = listOf(
                DelegationChain(
                    delegation = Delegation(
                        pubkey = listOf(
                            48, 86, 48, 16, 6, 7, 42, 134, 72, 206, 61, 2, 1, 6, 5, 43, 129,
                            4, 0, 10, 3, 66, 0, 4, 52, 19, 51, 255, 178, 201, 152, 184, 182,
                            132, 191, 59, 99, 238, 3, 105, 128, 202, 54, 124, 226, 151, 185,
                            238, 59, 199, 165, 170, 137, 231, 63, 87, 224, 211, 35, 6, 7,
                            160, 72, 184, 138, 106, 81, 12, 207, 97, 107, 163, 34, 103, 28,
                            147, 243, 200, 155, 130, 207, 101, 40, 216, 89, 177, 193, 138
                        ).toIntArray(),
                        expiration = 1743710406877828430
                    ),
                    signature = listOf(
                        4, 117, 29, 118, 87, 115, 143, 64, 81, 236, 175, 71, 101, 75, 214, 132,
                        200, 87, 4, 57, 73, 175, 129, 229, 143, 179, 11, 84, 176, 39, 224, 237,
                        39, 30, 102, 65, 217, 240, 165, 122, 182, 238, 67, 4, 40, 245, 121, 9,
                        87, 14, 157, 178, 153, 23, 118, 180, 193, 114, 152, 65, 60, 50, 201, 100
                    ).toIntArray()
                )
            )
        )
    }
}