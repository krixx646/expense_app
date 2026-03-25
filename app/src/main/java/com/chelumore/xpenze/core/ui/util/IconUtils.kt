package com.chelumore.xpenze.core.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.ui.graphics.vector.ImageVector
import com.chelumore.xpenze.domain.model.AccountType

/**
 * Maps string icon names (stored in DB) to Material icons.
 *
 * Keep this list small + stable; unknown names fall back to a sensible default.
 */
fun iconForName(name: String): ImageVector {
    return when (name) {
        // Categories
        "restaurant" -> Icons.Default.Restaurant
        "shopping_bag" -> Icons.Default.ShoppingBag
        "home" -> Icons.Default.Home
        "directions_car" -> Icons.Default.DirectionsCar
        "movie" -> Icons.Default.Movie
        "medical_services" -> Icons.Default.MedicalServices
        "school" -> Icons.Default.School
        "payments" -> Icons.Default.Payments
        "more_horiz" -> Icons.Default.MoreHoriz
        "category" -> Icons.Default.Category

        // Accounts
        "wallet" -> Icons.Default.AccountBalanceWallet
        "bank" -> Icons.Default.AccountBalance
        "credit_card" -> Icons.Default.CreditCard
        "savings" -> Icons.Default.Savings
        else -> Icons.AutoMirrored.Filled.HelpOutline
    }
}

fun iconForAccountType(type: AccountType): ImageVector {
    return when (type) {
        AccountType.CASH -> Icons.Default.AccountBalanceWallet
        AccountType.BANK -> Icons.Default.AccountBalance
        AccountType.CREDIT_CARD -> Icons.Default.CreditCard
        AccountType.SAVINGS -> Icons.Default.Savings
        AccountType.INVESTMENT -> Icons.Default.Savings
        AccountType.OTHER -> Icons.Default.AccountBalanceWallet
    }
}




