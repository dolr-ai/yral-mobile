package com.yral.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.ApplicationProductFlavor
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.ProductFlavor

@Suppress("EnumEntryName")
enum class FlavorDimension {
    contentType
}

@Suppress("EnumEntryName")
enum class YralFlavor(val dimension: FlavorDimension, val applicationIdSuffix: String? = null) {
    staging(FlavorDimension.contentType, applicationIdSuffix = ".staging"),
    prod(FlavorDimension.contentType),
}

fun configureFlavors(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
    flavorConfigurationBlock: ProductFlavor.(flavor: YralFlavor) -> Unit = {}
) {
    commonExtension.apply {
        FlavorDimension.values().forEach { flavorDimension ->
            flavorDimensions += flavorDimension.name
        }

        productFlavors {
            YralFlavor.values().forEach { bkpFlavor ->
                register(bkpFlavor.name) {
                    dimension = bkpFlavor.dimension.name
                    flavorConfigurationBlock(this, bkpFlavor)
                    if (this@apply is ApplicationExtension && this is ApplicationProductFlavor) {
                        if (bkpFlavor.applicationIdSuffix != null) {
                            applicationIdSuffix = bkpFlavor.applicationIdSuffix
                        }
                    }
                }
            }
        }
    }
}
