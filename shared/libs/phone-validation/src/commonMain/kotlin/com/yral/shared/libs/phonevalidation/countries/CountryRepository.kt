package com.yral.shared.libs.phonevalidation.countries

class CountryRepository {
    private val countries = CountriesDataSource.getAllCountries()

    fun getAllCountries(): List<Country> = countries

    fun searchCountries(query: String): List<Country> {
        if (query.isBlank()) return countries

        val lowerCaseQuery = query.lowercase().trim()
        return countries.filter { country ->
            country.name.lowercase().contains(lowerCaseQuery) ||
                country.code.lowercase().contains(lowerCaseQuery) ||
                country.dialCode.contains(lowerCaseQuery)
        }
    }

    fun getCountryByCode(code: String): Country? = countries.firstOrNull { it.code.equals(code, ignoreCase = true) }

    fun getCountryByDialCode(dialCode: String): List<Country> {
        val normalizedDialCode = dialCode.removePrefix("+")
        return countries.filter { country ->
            country.dialCode.removePrefix("+").startsWith(normalizedDialCode)
        }
    }
}
