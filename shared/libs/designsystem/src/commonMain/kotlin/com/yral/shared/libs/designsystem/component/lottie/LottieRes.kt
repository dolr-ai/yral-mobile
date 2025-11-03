package com.yral.shared.libs.designsystem.component.lottie

import yral_mobile.shared.libs.designsystem.generated.resources.Res

@Suppress("ForbiddenComment")
enum class LottieRes(
    filename: String,
) {
    // TODO: Move lottie to feature folders
    SPLASH("splash_lottie.json"),
    LIGHTNING("lightning_lottie.json"),
    CLAIM_SUCCESSFUL_WO_LOADING("claim_successful_wo_loading.lottie"),
    CLAIM_UNSUCCESSFUL_WO_LOADING("claim_unsuccessful_wo_loading.lottie"),
    COMMON_LOADING("common_loading.lottie"),
    SIGNUP_SCROLL("signup_scroll.json"),
    SMILEY_GAME_FIRE("smiley_game_fire.json"),
    SMILEY_GAME_HEART("smiley_game_heart.json"),
    SMILEY_GAME_LAUGH("smiley_game_laugh.json"),
    SMILEY_GAME_LOSE("smiley_game_lose.json"),
    SMILEY_GAME_PUKE("smiley_game_puke.json"),
    SMILEY_GAME_ROCKET("smiley_game_rocket.json"),
    SMILEY_GAME_SURPRISE("smiley_game_surprise.json"),
    SMILEY_GAME_WIN("smiley_game_win.json"),
    COLORFUL_CONFETTI_BRUST("colorful_confetti_brust.json"),
    LEADERBOARD_STAR("leaderboard_star.json"),
    YELLOW_RAYS("yellow_rays.json"),
    PURPLE_RAYS("purple_rays.json"),
    YRAL_LOADER("yral_loader.json"),
    WHITE_LOADER("white_loader.json"),
    READ_LOADER("read_loader.json"),
    BTC_REWARDS_COINS_ANIMATION("btc_rewards_coins.json"),
    BTC_CREDITED("btc_credited.json"),
    ;

    val path: String = "files/lottie/$filename"

    val assetPath = Res.getUri(path).removePrefix("file:///android_asset/")
}
