CURRENCY_YRAL = "YRAL"
CURRENCY_BTC = "BTC"
COUNTRY_CODE_INDIA = "IN"
COUNTRY_CODE_USA = "US"
SUPPORTED_REWARD_CURRENCIES = [CURRENCY_YRAL, CURRENCY_BTC]
DEFAULT_REWARD_CURRENCY = SUPPORTED_REWARD_CURRENCIES[1]

# Disabled in staging
# Enabled in production
REWARDS_ENABLED = False

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
            "1": 400,
            "2": 250,
            "3": 200,
            "4": 150,
            "5": 120,
            "6": 100,
            "7": 90,
            "8": 80,
            "9": 60,
            "10": 50
        },
        COUNTRY_CODE_USA: {
            "1": 5,
            "2": 3,
            "3": 3,
            "4": 2,
            "5": 2,
            "6": 1,
            "7": 1,
            "8": 1,
            "9": 1,
            "10": 1
        }
    }
}