package dev.staticvar.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

val BreezeShape =
  Shapes(
    extraSmall = RoundedCornerShape(BreezeDimens.xsmall),
    small = RoundedCornerShape(BreezeDimens.small),
    medium = RoundedCornerShape(BreezeDimens.medium),
    large = RoundedCornerShape(BreezeDimens.large),
    extraLarge = RoundedCornerShape(BreezeDimens.xlarge),
  )
