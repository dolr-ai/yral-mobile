package com.yral.shared.rust.service.utils

import com.yral.shared.uniffi.generated.ServiceCanistersDetails

fun getUserInfoServiceCanister(): String = ServiceCanistersDetails().getUserInfoServiceCanisterId()
