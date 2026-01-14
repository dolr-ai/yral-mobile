package com.yral.shared.features.auth.nav.countryselector

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.libs.phonevalidation.countries.Country

internal class DefaultCountrySelectorComponent(
    componentContext: ComponentContext,
    private val onCountrySelected: (Country) -> Unit,
    private val onBack: () -> Unit,
) : CountrySelectorComponent(),
    ComponentContext by componentContext {
    override fun onCountrySelected(country: Country) {
        onCountrySelected.invoke(country)
    }

    override fun onBack() {
        onBack.invoke()
    }
}
