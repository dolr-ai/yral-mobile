package com.yral.shared.features.auth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.phonevalidation.countries.Country
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.auth.generated.resources.Res
import yral_mobile.shared.features.auth.generated.resources.enter_mobile_number

@Composable
fun PhoneInputField(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    selectedCountry: Country?,
    isError: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .height(44.dp)
                .background(
                    color = YralColors.Neutral900,
                    shape = RoundedCornerShape(8.dp),
                ).border(
                    width = 1.dp,
                    color = if (isError) YralColors.Pink300 else YralColors.Neutral700,
                    shape = RoundedCornerShape(8.dp),
                ).padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Dial code prefix
            Text(
                text = selectedCountry?.dialCode ?: "+1",
                style = LocalAppTopography.current.baseSemiBold,
                color = YralColors.NeutralTextPrimary,
            )

            // Phone number input
            BasicTextField(
                value = phoneNumber,
                onValueChange = { value ->
                    // Only allow digits
                    val filtered = value.filter { it.isDigit() }
                    onPhoneNumberChange(filtered)
                },
                textStyle = LocalAppTopography.current.baseSemiBold.copy(color = YralColors.NeutralTextPrimary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                cursorBrush = SolidColor(YralColors.Pink300),
                decorationBox = { innerTextField ->
                    if (phoneNumber.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.enter_mobile_number),
                            style = LocalAppTopography.current.baseSemiBold,
                            color = YralColors.NeutralTextTertiary,
                        )
                    }
                    innerTextField()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
