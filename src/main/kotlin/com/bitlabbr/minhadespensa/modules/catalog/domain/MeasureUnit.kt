package com.bitlabbr.minhadespensa.modules.catalog.domain

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Unidade de medida do produto")
enum class MeasureUnit {
    UNIT,
    KILOGRAM,
    LITER,
    PACKAGE,
    GRAM,
    MILLILITER
}
