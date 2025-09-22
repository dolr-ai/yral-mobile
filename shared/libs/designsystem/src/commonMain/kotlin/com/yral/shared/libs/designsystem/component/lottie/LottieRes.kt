package com.yral.shared.libs.designsystem.component.lottie

@Suppress("ForbiddenComment")
enum class LottieRes(
    filename: String,
) {
    // TODO: Move lottie to feature folders
    LOADING("loading"),
    SPLASH("splash_lottie"),
    LIGHTNING("lightning_lottie"),
    CLAIM_SUCCESSFUL_WO_LOADING("claim_successful_wo_loading"),
    CLAIM_UNSUCCESSFUL_WO_LOADING("claim_unsuccessful_wo_loading"),
    COMMON_LOADING("common_loading"),
    SIGNUP_SCROLL("signup_scroll"),
    SMILEY_GAME_FIRE("smiley_game_fire"),
    SMILEY_GAME_HEART("smiley_game_heart"),
    SMILEY_GAME_LAUGH("smiley_game_laugh"),
    SMILEY_GAME_LOSE("smiley_game_lose"),
    SMILEY_GAME_PUKE("smiley_game_puke"),
    SMILEY_GAME_ROCKET("smiley_game_rocket"),
    SMILEY_GAME_SURPRISE("smiley_game_surprise"),
    SMILEY_GAME_WIN("smiley_game_win"),
    COLORFUL_CONFETTI_BRUST("colorful_confetti_brust"),
    LEADERBOARD_STAR("leaderboard_star"),
    YELLOW_RAYS("yellow_rays"),
    PURPLE_RAYS("purple_rays"),
    YRAL_LOADER("yral_loader"),
    WHITE_LOADER("white_loader"),
    READ_LOADER("read_loader"),
    ;

    val path: String = "files/lottie/$filename.json"
}
