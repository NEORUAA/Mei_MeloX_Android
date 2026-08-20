package com.ljyh.mei.ui.local

import androidx.compose.runtime.compositionLocalOf
import com.ljyh.mei.ui.navigation.MeiNavigator

val LocalNavController = compositionLocalOf<MeiNavigator> {
    error("Did not init yet!")
}
