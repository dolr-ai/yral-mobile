package com.yral.shared.rust.service.utils

import com.yral.shared.core.exceptions.YralException

class YralFfiException(
    cause: Throwable,
) : YralException(cause = cause)
