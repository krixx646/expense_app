package com.chelumore.xpenze.domain.usecase.account

import com.chelumore.xpenze.domain.model.Account
import com.chelumore.xpenze.domain.model.AccountType
import com.chelumore.xpenze.domain.repository.AccountRepository
import java.util.UUID
import javax.inject.Inject

class AddAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    /**
     * Creates an account. Returns the generated account id.
     */
    suspend operator fun invoke(
        name: String,
        type: AccountType,
        currency: String,
        initialBalanceCents: Long = 0L,
        colorHex: String = "#81C784",
        iconName: String = "wallet",
        isDefault: Boolean = false,
        isArchived: Boolean = false,
        sortOrder: Int = 0
    ): String {
        require(name.isNotBlank()) { "name is required" }
        require(currency.isNotBlank()) { "currency is required" }

        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()

        val account = Account(
            id = id,
            name = name,
            type = type,
            currency = currency,
            initialBalanceCents = initialBalanceCents,
            currentBalanceCents = initialBalanceCents,
            colorHex = colorHex,
            iconName = iconName,
            isDefault = isDefault,
            isArchived = isArchived,
            sortOrder = sortOrder,
            synced = false,
            deleted = false,
            createdAt = now,
            updatedAt = now
        )

        accountRepository.insert(account)
        if (isDefault) {
            accountRepository.setDefault(id)
        }
        return id
    }
}


