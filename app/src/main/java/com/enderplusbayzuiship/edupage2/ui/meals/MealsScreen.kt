package com.enderplusbayzuiship.edupage2.ui.meals

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.edupage.api.model.Meal
import com.edupage.api.model.MealOrderInfo
import com.edupage.api.model.MealRating
import com.edupage.api.model.MenuChoice
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.ui.core.containers.RoundedCardContainer
import com.enderplusbayzuiship.edupage2.ui.util.rememberAppHaptics
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealsScreen(
    bottomPadding: PaddingValues,
    viewModel: MealsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val haptics = rememberAppHaptics()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    val isRefreshing = (uiState as? MealsUiState.Success)?.isRefreshing == true

    val infiniteTransition = rememberInfiniteTransition(label = "meals_refresh")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.meals_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    FilledTonalIconButton(onClick = { haptics.virtualKey(); viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.meals_refresh),
                            modifier = Modifier.rotate(if (isRefreshing) rotation else 0f),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            DateNavigationRow(
                selectedDate = selectedDate,
                onPrevDay = { haptics.virtualKey(); viewModel.prevDay() },
                onNextDay = { haptics.virtualKey(); viewModel.nextDay() },
                onToday = { haptics.virtualKey(); viewModel.goToToday() },
            )

            when (val state = uiState) {
                is MealsUiState.Loading -> MealsLoading()
                is MealsUiState.Error -> MealsError(
                    message = state.message,
                    onRetry = { haptics.virtualKey(); viewModel.refresh() },
                )
                is MealsUiState.Success -> MealList(
                    meals = state.meals,
                    credit = state.credit,
                    mealOrderInfo = state.mealOrderInfo,
                    bottomPadding = bottomPadding,
                    onOrder = viewModel::orderMeal,
                    onCancel = viewModel::cancelMeal,
                    onRate = viewModel::rateMeal,
                )
            }
        }
    }
}

@Composable
private fun DateNavigationRow(
    selectedDate: LocalDate,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit,
) {
    val isToday = selectedDate == LocalDate.now()
    val datePattern = stringResource(R.string.meals_date_format)
    val dateText = runCatching {
        selectedDate.format(DateTimeFormatter.ofPattern(datePattern))
    }.getOrElse {
        selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    RoundedCardContainer(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceBright,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledTonalIconButton(onClick = onPrevDay) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.meals_prev_day),
                        )
                    }

                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )

                    FilledTonalIconButton(onClick = onNextDay) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = stringResource(R.string.meals_next_day),
                        )
                    }
                }

                if (!isToday) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        FilledTonalButton(onClick = onToday) {
                            Text(stringResource(R.string.meals_today))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MealsLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.meals_loading),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MealsError(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text(
                text = stringResource(R.string.meals_retry),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun MealList(
    meals: List<Meal>,
    credit: String?,
    mealOrderInfo: List<MealOrderInfo>?,
    bottomPadding: PaddingValues,
    onOrder: (String, String) -> Unit,
    onCancel: (String) -> Unit,
    onRate: (String, Int, Int) -> Unit,
) {
    if (meals.isEmpty() && mealOrderInfo.isNullOrEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.meals_no_meals),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = bottomPadding.calculateBottomPadding() + 16.dp),
        ) {
            if (credit != null) {
                item(key = "credit") {
                    CreditRow(credit = credit)
                }
            }

            if (mealOrderInfo != null) {
                mealOrderInfo.forEach { info ->
                    item(key = "order_${info.mealTypeIndex}") {
                        MealOrderCard(
                            info = info,
                            onOrder = { choice -> onOrder(info.mealTypeIndex, choice) },
                            onCancel = { onCancel(info.mealTypeIndex) },
                            onRate = { quality, quantity ->
                                onRate(info.mealTypeIndex, quality, quantity)
                            },
                        )
                    }
                }
            }

            itemsIndexed(meals, key = { index, meal -> meal.mealId ?: "meal_$index" }) { _, meal ->
                MealCard(meal = meal)
            }
        }
    }
}

@Composable
private fun CreditRow(credit: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.AccountBalanceWallet,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(R.string.meals_credit, credit),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun MealOrderCard(
    info: MealOrderInfo,
    onOrder: (String) -> Unit,
    onCancel: () -> Unit,
    onRate: (Int, Int) -> Unit,
) {
    val haptics = rememberAppHaptics()
    var showRating by remember { mutableStateOf(false) }

    RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceBright,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Restaurant,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(8.dp).size(18.dp),
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = info.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        val subtitle = buildList {
                            if (!info.servedFrom.isNullOrBlank() && !info.servedTo.isNullOrBlank()) {
                                add("${info.servedFrom}\u2013${info.servedTo}")
                            }
                            info.amountOfFoods?.let {
                                add(
                                    androidx.compose.ui.res.stringResource(
                                        R.string.meals_foods, it
                                    )
                                )
                            }
                        }.joinToString(" · ")
                        if (subtitle.isNotBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (info.orderedChoice != null) {
                        IconButton(onClick = { haptics.virtualKey(); showRating = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = stringResource(R.string.meals_rate),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                info.availableChoices.forEach { choice ->
                    val isSelected = choice.letter == info.orderedChoice
                    ChoiceRow(
                        choice = choice,
                        isSelected = isSelected,
                        onOrder = { onOrder(choice.letter) },
                    )
                }

                if (info.orderedChoice != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        FilledTonalButton(
                            onClick = { haptics.virtualKey(); onCancel() },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.meals_cancel_order))
                        }
                    }
                }

                val changeUntil = info.canChangeUntil
                if (changeUntil != null) {
                    Text(
                        text = stringResource(R.string.meals_change_until, changeUntil),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showRating) {
        RatingDialog(
            title = info.title,
            onDismiss = { showRating = false },
            onSubmit = { quality, quantity ->
                showRating = false
                onRate(quality, quantity)
            },
        )
    }
}

@Composable
private fun RatingDialog(
    title: String,
    onDismiss: () -> Unit,
    onSubmit: (Int, Int) -> Unit,
) {
    var quality by remember { mutableIntStateOf(5) }
    var quantity by remember { mutableIntStateOf(5) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.meals_rate_title, title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                StarRatingRow(
                    label = stringResource(R.string.meals_rate_quality),
                    value = quality,
                    onChange = { quality = it },
                )
                StarRatingRow(
                    label = stringResource(R.string.meals_rate_quantity),
                    value = quantity,
                    onChange = { quantity = it },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(quality, quantity) }) {
                Text(stringResource(R.string.meals_rate_submit))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.meals_rate_cancel))
            }
        },
    )
}

@Composable
private fun StarRatingRow(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            for (star in 1..5) {
                IconButton(onClick = { onChange(star) }) {
                    Icon(
                        imageVector = if (star <= value) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = null,
                        tint = if (star <= value) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChoiceRow(
    choice: MenuChoice,
    isSelected: Boolean,
    onOrder: () -> Unit,
) {
    val haptics = rememberAppHaptics()
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${choice.letter}: ${choice.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val meta = buildList {
                    choice.allergens?.takeIf { it.isNotEmpty() }?.let {
                        add("${stringResource(R.string.meals_allergens)}: ${it.joinToString(", ")}")
                    }
                    choice.weight?.takeIf { it.isNotBlank() }?.let { add(it) }
                }.joinToString(" · ")
                if (meta.isNotBlank()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                choice.rating?.takeIf { it.hasRatings }?.let { rating ->
                    RatingSummary(rating)
                }
            }

            if (isSelected) {
                Text(
                    text = stringResource(R.string.meals_ordered),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                FilledTonalButton(onClick = { haptics.virtualKey(); onOrder() }) {
                    Text(stringResource(R.string.meals_order))
                }
            }
        }
    }
}

@Composable
private fun RatingSummary(rating: MealRating) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 2.dp),
    ) {
        rating.qualityAverage?.let { avg ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = formatRating(avg, rating.qualityCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        rating.quantityAverage?.let { avg ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Restaurant,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = formatRating(avg, rating.quantityCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatRating(average: Double, count: Int?): String {
    val rounded = (average * 10).roundToInt() / 10.0
    return if (count != null) "%.1f (%d)".format(rounded, count) else "%.1f".format(rounded)
}

@Composable
private fun MealCard(meal: Meal) {
    RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceBright,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = meal.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )

                    if (meal.canOrder) {
                        OrderBadge(isOrdered = meal.isOrdered)
                    }
                }

                val allergens = meal.allergens
                if (!allergens.isNullOrEmpty()) {
                    Text(
                        text = "${stringResource(R.string.meals_allergens)}: ${allergens.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                val weight = meal.weight
                if (!weight.isNullOrBlank()) {
                    Text(
                        text = stringResource(R.string.meals_weight, weight),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderBadge(isOrdered: Boolean) {
    val (text, color) = if (isOrdered) {
        stringResource(R.string.meals_ordered) to MaterialTheme.colorScheme.primary
    } else {
        stringResource(R.string.meals_not_ordered) to MaterialTheme.colorScheme.outline
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = color,
    )
}