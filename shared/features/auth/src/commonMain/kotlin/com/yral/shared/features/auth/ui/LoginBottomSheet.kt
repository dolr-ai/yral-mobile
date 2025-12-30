package com.yral.shared.features.auth.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yral.shared.analytics.events.SignupPageName
import com.yral.shared.features.auth.ui.LoginBottomSheetConstants.BOTTOM_SHEET_SPACER_PERCENT_TO_SCREEN
import com.yral.shared.features.auth.ui.SignupViewConstants.DEFAULT_TOP_CONTENT_HEIGHT
import com.yral.shared.features.auth.ui.SignupViewConstants.DEFAULT_TOP_CONTENT_WIDTH
import com.yral.shared.features.auth.viewModel.LoginViewModel
import com.yral.shared.libs.arch.presentation.UiState
import com.yral.shared.libs.designsystem.component.YralAsyncImage
import com.yral.shared.libs.designsystem.component.YralBottomSheet
import com.yral.shared.libs.designsystem.component.YralErrorMessage
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralBrushes
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import yral_mobile.shared.features.auth.generated.resources.Res
import yral_mobile.shared.features.auth.generated.resources.continue_to_sign_up_for_free
import yral_mobile.shared.features.auth.generated.resources.create_ai_videos_earn_bitcoin
import yral_mobile.shared.features.auth.generated.resources.create_ai_videos_earn_bitcoin_dis
import yral_mobile.shared.features.auth.generated.resources.join_tournament
import yral_mobile.shared.features.auth.generated.resources.join_tournament_disclaimer
import yral_mobile.shared.features.auth.generated.resources.login_to_chat_with_influencer
import yral_mobile.shared.features.auth.generated.resources.login_to_get_25_tokens
import yral_mobile.shared.features.auth.generated.resources.login_to_join_tournament
import yral_mobile.shared.features.auth.generated.resources.upload_ai_videos_earn_bitcoin
import yral_mobile.shared.features.auth.generated.resources.upload_ai_videos_earn_bitcoin_dis
import yral_mobile.shared.libs.designsystem.generated.resources.btc_giftbox
import yral_mobile.shared.libs.designsystem.generated.resources.could_not_login
import yral_mobile.shared.libs.designsystem.generated.resources.could_not_login_desc
import yral_mobile.shared.libs.designsystem.generated.resources.ok
import yral_mobile.shared.libs.designsystem.generated.resources.victory_cup
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun LoginBottomSheet(
    pageName: SignupPageName,
    bottomSheetState: SheetState,
    onDismissRequest: () -> Unit,
    onLoginSuccess: (() -> Unit)? = null,
    openTerms: (terms: String) -> Unit,
    bottomSheetType: LoginBottomSheetType,
    loginViewModel: LoginViewModel = koinViewModel(),
) {
    val dismissRequest = remember(onDismissRequest) { createDismissCallback(onDismissRequest, loginViewModel) }
    val loginSuccess =
        remember(onLoginSuccess) {
            onLoginSuccess?.let { createDismissCallback(it, loginViewModel) }
        }
    val context = getContext()
    val state = loginViewModel.state.collectAsStateWithLifecycle()
    val termsLink = remember { loginViewModel.getTncLink() }
    when (state.value) {
        is UiState.Initial, is UiState.InProgress -> {
            YralBottomSheet(
                onDismissRequest = dismissRequest,
                bottomSheetState = bottomSheetState,
            ) {
                BoxWithConstraints {
                    val maxHeight = maxHeight
                    val adaptiveHeight = (maxHeight * BOTTOM_SHEET_SPACER_PERCENT_TO_SCREEN)
                    Column(
                        modifier =
                            Modifier
                                .padding(start = 16.dp, top = 45.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Top),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        SignupView(
                            pageName = pageName,
                            onSignupClicked = { provider -> loginViewModel.signInWithSocial(context, provider) },
                            termsLink = termsLink,
                            openTerms = { openTerms(termsLink) },
                            headlineText =
                                getHeaderText(
                                    type = bottomSheetType,
                                    initialBalanceReward = loginViewModel.getInitialBalanceReward(),
                                ),
                            disclaimerText = getDisclaimerText(bottomSheetType),
                            topIconContent = { TopIconContent(bottomSheetType) },
                        )
                        Spacer(modifier = Modifier.height(adaptiveHeight))
                    }
                }
            }
        }
        is UiState.Success<*> -> {
            LaunchedEffect(Unit) { loginSuccess?.invoke() ?: dismissRequest() }
        }
        is UiState.Failure -> {
            YralErrorMessage(
                title = stringResource(DesignRes.string.could_not_login),
                error = stringResource(DesignRes.string.could_not_login_desc),
                sheetState = bottomSheetState,
                cta = stringResource(DesignRes.string.ok),
                onClick = dismissRequest,
                onDismiss = dismissRequest,
            )
        }
    }
}

@Composable
internal expect fun getContext(): Any

private fun createDismissCallback(
    onDismiss: () -> Unit,
    loginViewModel: LoginViewModel,
): () -> Unit =
    {
        onDismiss()
        loginViewModel.sheetDismissed()
    }

private object LoginBottomSheetConstants {
    const val BOTTOM_SHEET_SPACER_PERCENT_TO_SCREEN = 0.3f
}

@Composable
private fun getHeaderText(
    type: LoginBottomSheetType,
    initialBalanceReward: Int,
): AnnotatedString? =
    when (type) {
        LoginBottomSheetType.FEED -> {
            val fullText = stringResource(Res.string.login_to_get_25_tokens, initialBalanceReward)
            getAnnotatedHeaderForLogin(fullText)
        }
        LoginBottomSheetType.UPLOAD_AI_VIDEO -> {
            val fullText = stringResource(Res.string.upload_ai_videos_earn_bitcoin)
            val maskedText = fullText.substringAfter(".")
            getAnnotatedHeaderForLogin(fullText, maskedText)
        }
        LoginBottomSheetType.CREATE_AI_VIDEO -> {
            val fullText = stringResource(Res.string.create_ai_videos_earn_bitcoin)
            val maskedText = fullText.substringAfter(".")
            getAnnotatedHeaderForLogin(fullText, maskedText)
        }
        LoginBottomSheetType.TOURNAMENT -> {
            val fullText = stringResource(Res.string.login_to_join_tournament)
            val maskedText = stringResource(Res.string.join_tournament)
            getAnnotatedHeaderForLogin(fullText, maskedText)
        }
        is LoginBottomSheetType.CONVERSATION -> {
            val name = type.influencerName
            val fullText = stringResource(Res.string.login_to_chat_with_influencer, name)
            getAnnotatedHeaderForLogin(fullText)
        }
        else -> getAnnotatedHeaderForLogin(stringResource(Res.string.continue_to_sign_up_for_free))
    }

@Composable
fun getAnnotatedHeaderForLogin(
    fullText: String,
    maskedText: String = "",
) = buildAnnotatedString {
    val maskedStart = fullText.indexOf(maskedText)
    val maskedEnd = maskedStart + maskedText.length
    val textStyle = LocalAppTopography.current.xlSemiBold
    val spanStyle =
        SpanStyle(
            fontSize = textStyle.fontSize,
            fontFamily = textStyle.fontFamily,
            fontWeight = textStyle.fontWeight,
        )
    if (maskedStart >= 0) {
        withStyle(
            style = spanStyle.copy(color = Color.White),
        ) { append(fullText.take(maskedStart)) }

        withStyle(
            style =
                spanStyle.copy(
                    brush = YralBrushes.GoldenTextBrush,
                    fontWeight = FontWeight.Bold,
                ),
        ) { append(fullText.substring(maskedStart, maskedEnd)) }

        if (maskedEnd < fullText.length) {
            withStyle(
                style = spanStyle.copy(color = Color.White),
            ) { append(fullText.substring(maskedEnd)) }
        }
    } else {
        withStyle(
            style = spanStyle.copy(color = Color.White),
        ) {
            append(fullText)
        }
    }
}

@Composable
private fun getDisclaimerText(type: LoginBottomSheetType) =
    when (type) {
        LoginBottomSheetType.UPLOAD_AI_VIDEO -> stringResource(Res.string.upload_ai_videos_earn_bitcoin_dis)
        LoginBottomSheetType.CREATE_AI_VIDEO -> stringResource(Res.string.create_ai_videos_earn_bitcoin_dis)
        LoginBottomSheetType.TOURNAMENT -> stringResource(Res.string.join_tournament_disclaimer)
        else -> null
    }

@Composable
private fun getTopIcon(type: LoginBottomSheetType) =
    when (type) {
        LoginBottomSheetType.UPLOAD_AI_VIDEO -> painterResource(DesignRes.drawable.btc_giftbox)
        LoginBottomSheetType.CREATE_AI_VIDEO -> painterResource(DesignRes.drawable.btc_giftbox)
        LoginBottomSheetType.TOURNAMENT -> painterResource(DesignRes.drawable.victory_cup)
        is LoginBottomSheetType.CONVERSATION -> painterResource(DesignRes.drawable.victory_cup)
        else -> null
    }

@Composable
private fun TopIconContent(type: LoginBottomSheetType) {
    val topIconSize = getTopIconSize(type)
    val topIconModifier =
        Modifier
            .padding(0.dp)
            .width(topIconSize?.width ?: DEFAULT_TOP_CONTENT_WIDTH)
            .height(topIconSize?.height ?: DEFAULT_TOP_CONTENT_HEIGHT)
    when (type) {
        is LoginBottomSheetType.CONVERSATION ->
            type.influencerAvatarUrl
                .takeIf { it.isNotBlank() }
                ?.let { avatarUrl ->
                    YralAsyncImage(
                        imageUrl = avatarUrl,
                        modifier = topIconModifier,
                    )
                }
        else -> {
            val topIcon = getTopIcon(type)
            topIcon?.let {
                Image(
                    painter = topIcon,
                    contentDescription = "",
                    modifier = topIconModifier,
                )
            } ?: DefaultTopContent()
        }
    }
}

@Composable
private fun getTopIconSize(type: LoginBottomSheetType) =
    when (type) {
        LoginBottomSheetType.UPLOAD_AI_VIDEO -> DpSize(AI_VIDEO_TOP_ICON_WIDTH.dp, AI_VIDEO_TOP_ICON_HEIGHT.dp)
        LoginBottomSheetType.CREATE_AI_VIDEO -> DpSize(AI_VIDEO_TOP_ICON_WIDTH.dp, AI_VIDEO_TOP_ICON_HEIGHT.dp)
        LoginBottomSheetType.TOURNAMENT -> DpSize(TOURNAMENT_TOP_ICON_WIDTH.dp, TOURNAMENT_TOP_ICON_HEIGHT.dp)
        is LoginBottomSheetType.CONVERSATION -> DpSize(CONVERSATION_TOP_ICON_WIDTH.dp, CONVERSATION_TOP_ICON_HEIGHT.dp)
        else -> null
    }

@Serializable
@Suppress("ClassName")
sealed interface LoginBottomSheetType {
    @Serializable
    data object DEFAULT : LoginBottomSheetType

    @Serializable
    data object FEED : LoginBottomSheetType

    @Serializable
    data object CREATE_AI_VIDEO : LoginBottomSheetType

    @Serializable
    data object UPLOAD_AI_VIDEO : LoginBottomSheetType

    @Serializable
    data object TOURNAMENT : LoginBottomSheetType

    @Serializable
    data class CONVERSATION(
        val influencerName: String,
        val influencerAvatarUrl: String,
    ) : LoginBottomSheetType
}

private const val AI_VIDEO_TOP_ICON_WIDTH = 200f
private const val AI_VIDEO_TOP_ICON_HEIGHT = 165f
private const val TOURNAMENT_TOP_ICON_WIDTH = 176f
private const val TOURNAMENT_TOP_ICON_HEIGHT = 156f
private const val CONVERSATION_TOP_ICON_WIDTH = 120f
private const val CONVERSATION_TOP_ICON_HEIGHT = 120f
