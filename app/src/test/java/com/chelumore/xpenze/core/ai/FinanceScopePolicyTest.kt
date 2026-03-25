package com.chelumore.xpenze.core.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceScopePolicyTest {

    @Test
    fun `finance question is accepted`() {
        val accepted = FinanceScopePolicy.isFinanceQuestion(
            "How can I reduce my food expenses and stay under budget this month?"
        )
        assertTrue(accepted)
    }

    @Test
    fun `currency amount question is accepted`() {
        val accepted = FinanceScopePolicy.isFinanceQuestion(
            "I spent $120 today, can I still afford rent this week?"
        )
        assertTrue(accepted)
    }

    @Test
    fun `off topic question is rejected`() {
        val accepted = FinanceScopePolicy.isFinanceQuestion(
            "How do I cure malaria quickly?"
        )
        assertFalse(accepted)
    }

    @Test
    fun `model disclosure detection catches internal details`() {
        val hasDisclosure = FinanceScopePolicy.containsModelDisclosure(
            "I run on gemini-2.5-flash and use an API key."
        )
        assertTrue(hasDisclosure)
    }
}
