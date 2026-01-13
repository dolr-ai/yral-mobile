package com.yral.shared.features.auth.nav.countryselector

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.libs.phonevalidation.countries.Country

abstract class CountrySelectorComponent {
    abstract fun onCountrySelected(country: Country)
    abstract fun onBack()

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            onCountrySelected: (Country) -> Unit,
            onBack: () -> Unit,
        ): CountrySelectorComponent =
            DefaultCountrySelectorComponent(
                componentContext = componentContext,
                onCountrySelected = onCountrySelected,
                onBack = onBack,
            )
    }
}
