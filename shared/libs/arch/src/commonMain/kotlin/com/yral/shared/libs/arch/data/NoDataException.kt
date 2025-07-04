package com.yral.shared.libs.arch.data

class NoDataException(
    message: String? = NO_SUCH_DATA,
) : NoSuchElementException(message) {
    companion object {
        const val NO_SUCH_DATA = "Data not found in the database"
    }
}
