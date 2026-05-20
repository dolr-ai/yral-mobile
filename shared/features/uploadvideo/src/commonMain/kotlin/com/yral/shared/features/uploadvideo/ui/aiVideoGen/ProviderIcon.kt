package com.yral.shared.features.uploadvideo.ui.aiVideoGen

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.yral.shared.features.uploadvideo.domain.models.Provider
import com.yral.shared.libs.designsystem.component.YralAsyncImage
import com.yral.shared.libs.designsystem.component.getSVGImageModel
import org.jetbrains.compose.resources.painterResource
import yral_mobile.shared.features.uploadvideo.generated.resources.Res
import yral_mobile.shared.features.uploadvideo.generated.resources.ic_ltx_provider

@Composable
internal fun ProviderIcon(
    provider: Provider,
    modifier: Modifier = Modifier,
) {
    if (provider.usesLtxProviderIcon()) {
        Image(
            painter = painterResource(Res.drawable.ic_ltx_provider),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier,
        )
        return
    }

    provider.modelIcon?.takeIf { it.isNotBlank() }?.let { url ->
        YralAsyncImage(
            imageUrl = getSVGImageModel(url),
            modifier = modifier,
        )
    }
}

internal fun Provider.usesLtxProviderIcon(): Boolean {
    val normalizedId = id.trim().lowercase()
    val normalizedName = name.trim().lowercase()
    return normalizedId == LTX_PROVIDER_ID ||
        normalizedId.startsWith("$LTX_PROVIDER_ID-") ||
        normalizedId.startsWith("${LTX_PROVIDER_ID}_") ||
        normalizedName.startsWith(LTX_PROVIDER_ID)
}

private const val LTX_PROVIDER_ID = "ltx"
