package com.enderplusbayzuiship.edupage2.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocalDining
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.data.AppLanguage
import com.enderplusbayzuiship.edupage2.ui.core.cards.IconToggleItem
import com.enderplusbayzuiship.edupage2.ui.core.containers.RoundedCardContainer
import com.enderplusbayzuiship.edupage2.ui.modifiers.BlurStepTransition
import com.enderplusbayzuiship.edupage2.ui.util.rememberAppHaptics

private enum class OnboardingStep {
    WELCOME,
    ACKNOWLEDGEMENT,
    PREFERENCES,
    FEATURE_INTRODUCTION,
}

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    var currentStep by remember { mutableStateOf(OnboardingStep.WELCOME) }
    val haptics = rememberAppHaptics()

    LaunchedEffect(Unit) {
        if (viewModel.isCompleted) onFinish()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        BlurStepTransition(
            targetState = currentStep,
            isForward = { from, to -> to.ordinal > from.ordinal },
        ) { step ->
            Box(modifier = Modifier.fillMaxSize()) {
                when (step) {
                    OnboardingStep.WELCOME -> WelcomeStepContent(
                        viewModel = viewModel,
                        onNext = {
                            haptics.virtualKey()
                            currentStep = OnboardingStep.ACKNOWLEDGEMENT
                        },
                    )
                    OnboardingStep.ACKNOWLEDGEMENT -> {
                        val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
                        AcknowledgementStepContent(
                            notificationsEnabled = notificationsEnabled,
                            onNotificationsChanged = viewModel::setNotificationsEnabled,
                            onBack = {
                                haptics.virtualKey()
                                currentStep = OnboardingStep.WELCOME
                            },
                            onNext = {
                                haptics.virtualKey()
                                currentStep = OnboardingStep.PREFERENCES
                            },
                        )
                    }
                    OnboardingStep.PREFERENCES -> PreferencesStepContent(
                        viewModel = viewModel,
                        onBack = {
                            haptics.virtualKey()
                            currentStep = OnboardingStep.ACKNOWLEDGEMENT
                        },
                        onNext = {
                            haptics.virtualKey()
                            currentStep = OnboardingStep.FEATURE_INTRODUCTION
                        },
                    )
                    OnboardingStep.FEATURE_INTRODUCTION -> FeatureIntroStepContent(
                        onBack = {
                            haptics.virtualKey()
                            currentStep = OnboardingStep.PREFERENCES
                        },
                        onFinish = {
                            haptics.virtualKey()
                            viewModel.completeOnboarding()
                            onFinish()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeStepContent(
    viewModel: OnboardingViewModel,
    onNext: () -> Unit,
) {
    val appLanguage by viewModel.appLanguage.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(modifier = Modifier.statusBarsPadding())

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(240.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(128.dp),
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = stringResource(R.string.onboarding_welcome_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )

            Text(
                text = stringResource(R.string.onboarding_welcome_subtitle),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = stringResource(R.string.onboarding_pill_text),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(end = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            RoundedCardContainer(modifier = Modifier.padding(horizontal = 16.dp)) {
                LanguagePicker(
                    selected = appLanguage,
                    onSelected = viewModel::setAppLanguage,
                )
            }

            Spacer(modifier = Modifier.height(2.dp))
        }

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
                .height(56.dp),
        ) {
            Text(
                text = stringResource(R.string.onboarding_welcome_begin),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.weight(1f))

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun LanguagePicker(
    selected: AppLanguage,
    onSelected: (AppLanguage) -> Unit,
) {
    val haptics = rememberAppHaptics()
    var expanded by remember { mutableStateOf(false) }

    fun labelFor(language: AppLanguage): String = when (language) {
        AppLanguage.SYSTEM -> "System"
        AppLanguage.ENGLISH -> "English"
        AppLanguage.CZECH -> "Čeština"
        AppLanguage.SLOVAK -> "Slovenčina"
    }

    Box {
        ListItem(
            headlineContent = {
                Text(
                    text = stringResource(R.string.settings_language_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            leadingContent = {
                Icon(
                    imageVector = Icons.Rounded.Language,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            trailingContent = {
                Surface(
                    onClick = {
                        haptics.virtualKey()
                        expanded = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = labelFor(selected),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceBright,
            ),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            AppLanguage.entries.forEach { language ->
                DropdownMenuItem(
                    text = { Text(labelFor(language)) },
                    onClick = {
                        haptics.virtualKey()
                        onSelected(language)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun AcknowledgementStepContent(
    notificationsEnabled: Boolean,
    onNotificationsChanged: (Boolean) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Spacer(modifier = Modifier.statusBarsPadding())

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.onboarding_ack_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.onboarding_ack_desc),
                    style = MaterialTheme.typography.bodyLarge,
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.onboarding_ack_warning),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = stringResource(R.string.onboarding_ack_footer),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        RoundedCardContainer {
            IconToggleItem(
                icon = Icons.Rounded.Notifications,
                title = stringResource(R.string.settings_notif_label),
                description = stringResource(R.string.settings_notif_desc_on),
                checked = notificationsEnabled,
                onCheckedChange = onNotificationsChanged,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OnboardingBottomBar(
            onBack = onBack,
            onNext = onNext,
            nextLabel = stringResource(R.string.action_i_understand),
            nextIcon = Icons.Rounded.Check,
        )
    }
}

@Composable
private fun PreferencesStepContent(
    viewModel: OnboardingViewModel,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val showWeekends by viewModel.showWeekends.collectAsState()
    val mealsEnabled by viewModel.mealsEnabled.collectAsState()
    val notifGrades by viewModel.notifGradesEnabled.collectAsState()
    val notifMessages by viewModel.notifMessagesEnabled.collectAsState()
    val notifSubs by viewModel.notifSubstitutionsEnabled.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.statusBarsPadding())

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.onboarding_prefs_title),
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.onboarding_prefs_desc),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(32.dp))

            SectionLabel(text = stringResource(R.string.onboarding_section_app))

            RoundedCardContainer {
                IconToggleItem(
                    icon = Icons.Rounded.Schedule,
                    title = stringResource(R.string.settings_show_weekends),
                    description = stringResource(R.string.settings_show_weekends_desc),
                    checked = showWeekends,
                    onCheckedChange = viewModel::setShowWeekends,
                )
                IconToggleItem(
                    icon = Icons.Rounded.Restaurant,
                    title = stringResource(R.string.onboarding_meals_enabled),
                    description = stringResource(R.string.settings_meals_enabled_desc),
                    checked = mealsEnabled,
                    onCheckedChange = viewModel::setMealsEnabled,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionLabel(text = stringResource(R.string.onboarding_section_notifs))

            RoundedCardContainer {
                IconToggleItem(
                    icon = Icons.Rounded.Notifications,
                    title = stringResource(R.string.settings_notif_grades),
                    description = stringResource(R.string.settings_notif_grades_desc),
                    checked = notifGrades,
                    onCheckedChange = viewModel::setNotifGradesEnabled,
                )
                IconToggleItem(
                    icon = Icons.Rounded.Notifications,
                    title = stringResource(R.string.settings_notif_messages),
                    description = stringResource(R.string.settings_notif_messages_desc),
                    checked = notifMessages,
                    onCheckedChange = viewModel::setNotifMessagesEnabled,
                )
                IconToggleItem(
                    icon = Icons.Rounded.Notifications,
                    title = stringResource(R.string.settings_notif_substitutions),
                    description = stringResource(R.string.settings_notif_substitutions_desc),
                    checked = notifSubs,
                    onCheckedChange = viewModel::setNotifSubstitutionsEnabled,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        OnboardingBottomBar(
            onBack = onBack,
            onNext = onNext,
            nextLabel = stringResource(R.string.action_all_set),
            nextIcon = Icons.Rounded.Check,
        )
    }
}

@Composable
private fun FeatureIntroStepContent(
    onBack: () -> Unit,
    onFinish: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.statusBarsPadding())

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.onboarding_features_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.onboarding_features_desc),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start,
            )

            Spacer(modifier = Modifier.height(24.dp))

            ExampleOverview()
            Spacer(modifier = Modifier.height(12.dp))
            ExampleTimetable()
            Spacer(modifier = Modifier.height(12.dp))
            ExampleNotification()
            Spacer(modifier = Modifier.height(12.dp))
            ExampleMeals()

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.onboarding_ack_footer),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        OnboardingBottomBar(
            onBack = onBack,
            onNext = onFinish,
            nextLabel = stringResource(R.string.action_let_me_in),
            nextIcon = Icons.Rounded.Check,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .padding(start = 12.dp, bottom = 8.dp)
            .fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Start,
    )
}

@Composable
private fun OnboardingBottomBar(
    onBack: (() -> Unit)?,
    onNext: () -> Unit,
    nextLabel: String,
    nextIcon: ImageVector,
) {
    val haptics = rememberAppHaptics()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onBack != null) {
            OutlinedButton(
                onClick = {
                    haptics.virtualKey()
                    onBack()
                },
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(0.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Button(
            onClick = {
                haptics.virtualKey()
                onNext()
            },
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
        ) {
            Text(
                text = nextLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.weight(1f))

            Icon(
                imageVector = nextIcon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun ExampleCard(
    label: String,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun ExampleOverview() {
    ExampleCard(stringResource(R.string.onboarding_example_overview_label)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LessonRow()
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            GradeRow()
        }
    }
}

@Composable
private fun LessonRow() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.onboarding_example_lesson_subject),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.onboarding_example_lesson_room),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.onboarding_example_lesson_time),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GradeRow() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Grade,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.onboarding_example_grade_subject),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.onboarding_example_grade),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Text(
                text = stringResource(R.string.onboarding_example_grade_value),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun ExampleTimetable() {
    ExampleCard(stringResource(R.string.onboarding_title_2)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LessonRow()
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            LessonRow()
        }
    }
}

@Composable
private fun ExampleNotification() {
    ExampleCard(stringResource(R.string.onboarding_example_notif_title)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Rounded.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.onboarding_example_notif_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.onboarding_example_notif_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ExampleMeals() {
    ExampleCard(stringResource(R.string.onboarding_example_meals_menu)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.onboarding_example_meals_credit),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.onboarding_example_meals_credit_value),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocalDining,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = stringResource(R.string.onboarding_example_meals_dish),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

