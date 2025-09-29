CURRENCY_YRAL = "YRAL"
CURRENCY_BTC = "BTC"
COUNTRY_CODE_INDIA = "IN"
COUNTRY_CODE_USA = "US"
SUPPORTED_REWARD_CURRENCIES = [CURRENCY_YRAL, CURRENCY_BTC]
DEFAULT_REWARD_CURRENCY = SUPPORTED_REWARD_CURRENCIES[1]

REWARDS_ENABLED = True

REWARD_AMOUNT = {
    CURRENCY_YRAL: {
        "1": 100,
        "2": 80,
        "3": 50,
        "4": 40,
        "5": 20,
    },
    CURRENCY_BTC: {
        COUNTRY_CODE_INDIA: {
            "1": 100,
            "2": 80,
            "3": 50,
            "4": 40,
            "5": 20,
        },
        COUNTRY_CODE_USA: {
            "1": 5,
            "2": 4,
            "3": 3,
            "4": 2,
            "5": 1,
        }
    }
}