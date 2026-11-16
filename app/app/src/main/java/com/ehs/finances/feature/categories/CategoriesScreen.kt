package com.ehs.finances.feature.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ehs.finances.core.i18n.LocalStrings
import com.ehs.finances.core.ui.MessageEffect
import com.ehs.finances.core.ui.categoryLabel
import com.ehs.finances.core.ui.classificationLabel
import com.ehs.finances.core.ui.components.AppDropdown
import com.ehs.finances.core.ui.components.AppTextField
import com.ehs.finances.core.ui.components.ColorSwatches
import com.ehs.finances.core.ui.components.ConfirmDialog
import com.ehs.finances.core.ui.components.ContentCard
import com.ehs.finances.core.ui.components.DropdownOption
import com.ehs.finances.core.ui.components.EmptyNote
import com.ehs.finances.core.ui.components.ErrorBanner
import com.ehs.finances.core.ui.components.FormDialog
import com.ehs.finances.core.ui.components.FormError
import com.ehs.finances.core.ui.components.LoadingScreen
import com.ehs.finances.core.ui.components.PrimaryButton
import com.ehs.finances.core.ui.components.ScreenTitle
import com.ehs.finances.core.ui.components.SwitchRow
import com.ehs.finances.core.ui.components.Tag
import com.ehs.finances.core.ui.containerViewModel
import com.ehs.finances.core.ui.theme.FinancesTheme
import com.ehs.finances.data.model.Category
import com.ehs.finances.data.model.CategoryInput

private val palette = listOf(
    "#0F766E", "#0EA5E9", "#6366F1", "#A855F7", "#EC4899",
    "#EF4444", "#F59E0B", "#10B981", "#64748B", "#111827",
)

@Composable
fun CategoriesScreen() {
    val vm: CategoriesViewModel = containerViewModel { CategoriesViewModel(it.financesRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val strings = LocalStrings.current

    MessageEffect(message, onConsumed = vm::consumeMessage)

    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Category?>(null) }
    var deleteTarget by remember { mutableStateOf<Category?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenTitle(
            title = strings["categories"],
            subtitle = strings["categoriesSubtitle"],
            action = {
                PrimaryButton(
                    text = strings["newCategory"],
                    onClick = {
                        editing = null
                        showForm = true
                    },
                )
            },
        )

        OutlinedTextField(
            value = state.search,
            onValueChange = vm::setSearch,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(strings["filterCategories"]) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
        )
        AppDropdown(
            label = strings["filterCategoryType"],
            options = listOf(
                DropdownOption(CategoriesUiState.TYPE_ALL, strings["all"]),
                DropdownOption(CategoriesUiState.TYPE_INCOME, strings["income"]),
                DropdownOption(CategoriesUiState.TYPE_EXPENSE, strings["expense"]),
            ),
            selectedValue = state.typeFilter,
            onSelect = vm::setTypeFilter,
        )

        when {
            state.loading && state.categories.isEmpty() -> LoadingScreen(strings["loading"])
            state.error != null -> ErrorBanner(
                message = state.error.orEmpty(),
                onRetry = vm::load,
                retryLabel = strings["retry"],
            )
            state.filtered.isEmpty() -> EmptyNote(strings["noCategories"])
            else -> ContentCard {
                state.filtered.forEachIndexed { index, category ->
                    if (index > 0) HorizontalDivider(Modifier.padding(vertical = 10.dp))
                    CategoryRow(
                        category = category,
                        deleting = state.deletingId == category.id,
                        onEdit = {
                            editing = category
                            showForm = true
                        },
                        onDelete = { deleteTarget = category },
                    )
                }
            }
        }
    }

    if (showForm) {
        CategoryFormDialog(
            category = editing,
            saving = state.saving,
            onDismiss = { showForm = false },
            onSave = { input ->
                vm.save(editing?.id, input)
                showForm = false
            },
        )
    }

    deleteTarget?.let { category ->
        ConfirmDialog(
            title = strings["confirmDeleteTitle"],
            text = strings["deleteCategoryConfirm"],
            confirmLabel = strings["delete"],
            dismissLabel = strings["cancel"],
            onConfirm = { vm.delete(category.id) },
            onDismiss = { deleteTarget = null },
        )
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    deleting: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val strings = LocalStrings.current
    var menuOpen by remember { mutableStateOf(false) }
    val color = runCatching { Color(android.graphics.Color.parseColor(category.color)) }
        .getOrDefault(MaterialTheme.colorScheme.primary)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(color, CircleShape),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    categoryLabel(category, strings),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Tag(
                    text = strings[if (category.income) "income" else "expense"],
                    color = if (category.income) FinancesTheme.colors.success else FinancesTheme.colors.danger,
                    background = if (category.income) FinancesTheme.colors.successSoft else FinancesTheme.colors.dangerSoft,
                )
            }
            Text(
                listOfNotNull(
                    "${strings["goalLabel"]}: ${category.goal}%",
                    classificationLabel(category.classification, strings),
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (deleting) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = strings["actions"])
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(strings["edit"]) },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(strings["delete"], color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryFormDialog(
    category: Category?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (CategoryInput) -> Unit,
) {
    val strings = LocalStrings.current
    var title by remember { mutableStateOf(category?.title.orEmpty()) }
    var color by remember { mutableStateOf(category?.color ?: palette.first()) }
    var goal by remember { mutableStateOf(category?.goal?.toString() ?: "0") }
    var classification by remember { mutableStateOf(category?.classification ?: "ESSENTIAL") }
    var income by remember { mutableStateOf(category?.income ?: false) }
    var error by remember { mutableStateOf<String?>(null) }

    FormDialog(
        title = strings[if (category == null) "newCategory" else "editCategory"],
        onDismiss = onDismiss,
        confirmLabel = strings["save"],
        loading = saving,
        onConfirm = {
            val goalValue = goal.trim().toIntOrNull()
            error = when {
                title.isBlank() -> "titleRequired"
                goalValue == null || goalValue !in 0..100 -> "goalInvalid"
                else -> null
            }
            if (error == null) {
                onSave(
                    CategoryInput(
                        title = title,
                        color = color,
                        goal = goalValue!!,
                        classification = classification,
                        income = income,
                    ),
                )
            }
        },
    ) {
        AppTextField(
            value = title,
            onValueChange = { title = it },
            label = strings["categoryTitleLabel"],
            placeholder = strings["categoryTitlePlaceholder"],
        )
        Text(strings["colorLabel"], style = MaterialTheme.typography.labelMedium)
        ColorSwatches(colors = palette, selected = color, onSelect = { color = it })
        AppTextField(
            value = goal,
            onValueChange = { goal = it.filter { char -> char.isDigit() }.take(3) },
            label = strings["goalLabel"],
            supportingText = strings["goalCaption"],
            keyboardType = KeyboardType.Number,
        )
        AppDropdown(
            label = strings["classificationLabel"],
            options = listOf(
                DropdownOption("ESSENTIAL", strings["essential"]),
                DropdownOption("RECREATION", strings["recreation"]),
            ),
            selectedValue = classification,
            onSelect = { classification = it },
        )
        SwitchRow(
            title = strings["categoryTypeLabel"],
            subtitle = strings[if (income) "income" else "expense"],
            checked = income,
            onCheckedChange = { income = it },
        )
        FormError(error?.let { strings[it] })
    }
}
