package com.yral.shared.features.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yral.shared.features.auth.nav.countryselector.CountrySelectorComponent
import com.yral.shared.features.auth.viewModel.LoginViewModel
import com.yral.shared.libs.designsystem.component.LoaderSize
import com.yral.shared.libs.designsystem.component.YralAsyncImage
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.phonevalidation.countries.Country
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.auth.generated.resources.Res
import yral_mobile.shared.features.auth.generated.resources.country
import yral_mobile.shared.features.auth.generated.resources.search_by_country_name
import yral_mobile.shared.libs.designsystem.generated.resources.arrow_left
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Composable
fun CountrySelectorScreen(
    component: CountrySelectorComponent,
    loginViewModel: LoginViewModel,
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember { mutableStateOf("") }
    val countries =
        remember(searchQuery) {
            if (searchQuery.isBlank()) {
                loginViewModel.getAllCountries()
            } else {
                loginViewModel.searchCountries(searchQuery)
            }
        }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(YralColors.Neutral950),
    ) {
        // Header
        Header(component)

        // Search bar
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        // Country list
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
        ) {
            items(countries, key = { it.code }) { country ->
                CountryListItem(
                    country = country,
                    onClick = { component.onCountrySelected(country) },
                )
                HorizontalDivider(
                    color = YralColors.Divider,
                    thickness = 1.dp,
                )
            }
        }
    }
}

@Composable
private fun Header(component: CountrySelectorComponent) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(YralColors.Neutral950)
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        IconButton(
            onClick = component::onBack,
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Icon(
                painter = painterResource(DesignRes.drawable.arrow_left),
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }

        Text(
            text = stringResource(Res.string.country),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .height(44.dp)
                .background(
                    color = YralColors.Neutral900,
                    shape = RoundedCornerShape(8.dp),
                ).padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = LocalAppTopography.current.baseMedium.copy(color = YralColors.NeutralTextPrimary),
                cursorBrush = SolidColor(YralColors.Pink300),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.search_by_country_name),
                            style = LocalAppTopography.current.baseMedium,
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

@Composable
private fun CountryListItem(
    country: Country,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Flag image
        YralAsyncImage(
            imageUrl = country.flagUrl,
            modifier = Modifier.width(31.5.dp).height(22.5.dp),
            contentScale = ContentScale.FillBounds,
            shape = RoundedCornerShape(4.dp),
            loaderSize = LoaderSize.Dynamic(16.dp),
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Country name
        Text(
            text = country.name,
            style = LocalAppTopography.current.baseMedium,
            color = YralColors.NeutralTextPrimary,
            modifier = Modifier.weight(1f),
        )

        // Dial code
        Text(
            text = country.dialCode,
            style = LocalAppTopography.current.baseMedium,
            color = YralColors.Neutral600,
        )
    }
}
