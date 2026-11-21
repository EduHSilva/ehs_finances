package com.ehs.finances.feature.categories

import com.ehs.finances.data.model.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoriesUiStateTest {
    @Test
    fun `groups expenses by classification in fixed order with goal subtotal`() {
        val state = CategoriesUiState(
            categories = listOf(
                category("Lazer", 20, "RECREATION"),
                category("Mercado", 30, "ESSENTIAL"),
                category("Aporte", 10, "INVESTMENT"),
                category("Luz", 15, "ESSENTIAL"),
                category("Salário", 100, "ESSENTIAL", income = true),
            ),
        )

        val groups = state.expenseGroups

        assertEquals(listOf("ESSENTIAL", "INVESTMENT", "RECREATION"), groups.map { it.classification })
        assertEquals(listOf("Mercado", "Luz"), groups[0].categories.map { it.title })
        assertEquals(45, groups[0].goalTotal)
        assertEquals(10, groups[1].goalTotal)
        assertEquals(20, groups[2].goalTotal)
        assertEquals(listOf("Salário"), state.income.map { it.title })
    }

    @Test
    fun `omits groups without categories`() {
        val state = CategoriesUiState(categories = listOf(category("Aporte", 10, "INVESTMENT")))

        assertEquals(listOf("INVESTMENT"), state.expenseGroups.map { it.classification })
    }

    @Test
    fun `unknown classification falls back to essential`() {
        val state = CategoriesUiState(categories = listOf(category("Outros", 5, "OTHER")))

        assertEquals(listOf("ESSENTIAL"), state.expenseGroups.map { it.classification })
        assertTrue(state.expenseGroups.single().categories.any { it.title == "Outros" })
    }

    private fun category(title: String, goal: Int, classification: String, income: Boolean = false) =
        Category(
            id = title,
            title = title,
            color = "#000000",
            goal = goal,
            classification = classification,
            income = income,
        )
}
