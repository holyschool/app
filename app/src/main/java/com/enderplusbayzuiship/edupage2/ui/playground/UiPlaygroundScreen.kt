package com.enderplusbayzuiship.edupage2.ui.playground

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.ui.core.cards.FeatureCard
import com.enderplusbayzuiship.edupage2.ui.core.cards.IconToggleItem
import com.enderplusbayzuiship.edupage2.ui.core.cards.PastelIcon
import com.enderplusbayzuiship.edupage2.ui.core.cards.PastelInitials
import com.enderplusbayzuiship.edupage2.ui.core.cards.SettingsNavigationRow
import com.enderplusbayzuiship.edupage2.ui.core.cards.SettingsSelectRow
import com.enderplusbayzuiship.edupage2.ui.core.containers.RoundedCardContainer
import com.enderplusbayzuiship.edupage2.ui.core.containers.SectionLabel
import com.enderplusbayzuiship.edupage2.ui.core.sheets.AppBottomSheet
import com.enderplusbayzuiship.edupage2.ui.modifiers.MotionBlurGate
import com.enderplusbayzuiship.edupage2.ui.modifiers.scrollMotionBlur
import com.enderplusbayzuiship.edupage2.ui.util.rememberAppHaptics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UiPlaygroundScreen(
    onBack: () -> Unit,
    bottomPadding: PaddingValues = PaddingValues(),
) {
    val haptics = rememberAppHaptics()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showSheet by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    BackHandler { onBack() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.dev_test_ui),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { haptics.virtualKey(); onBack() }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
        androidx.compose.foundation.lazy.LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .scrollMotionBlur(
                    lazyListState = listState,
                    enabled = MotionBlurGate.enabled,
                ),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = bottomPadding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item { PlaygroundSection(text = "Headers") }
            item {
                RoundedCardContainer {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceBright,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            SectionLabel(text = "Primary section label")
                            SectionLabel(
                                text = "Settings section label",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                startPadding = 16.dp,
                                topPadding = 16.dp,
                            )
                        }
                    }
                }
            }

            item { PlaygroundSection(text = "Rows") }
            item {
                PlaygroundToggleRows()
            }
            item {
                RoundedCardContainer {
                    var picked by remember { mutableStateOf("b") }
                    SettingsSelectRow(
                        title = "Select row",
                        icon = Icons.Rounded.Settings,
                        options = listOf("a" to "Option A", "b" to "Option B"),
                        selectedOption = picked,
                        onOptionSelected = { picked = it },
                    )
                    SettingsNavigationRow(
                        title = "Navigation row",
                        description = "Leads somewhere",
                        icon = Icons.AutoMirrored.Rounded.ArrowForward,
                        onClick = {},
                    )
                    FeatureCard(
                        title = "Feature card",
                        description = "Pastel icon, chevron, tap me",
                        icon = Icons.Rounded.Star,
                        onClick = {},
                    )
                    FeatureCard(
                        title = "Feature toggle",
                        description = "With a switch",
                        icon = Icons.Rounded.Notifications,
                        showToggle = true,
                        checked = true,
                        onCheckedChange = {},
                        onClick = {},
                    )
                }
            }

            item { PlaygroundSection(text = "Icons") }
            item {
                RoundedCardContainer {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceBright,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp),
                        ) {
                            PastelIcon(icon = Icons.Rounded.School, key = "Mathematics")
                            PastelIcon(icon = Icons.Rounded.Email, key = "Messages")
                            PastelIcon(icon = Icons.Rounded.Schedule, key = "Timetable")
                            PastelInitials(text = "English")
                            PastelInitials(text = "Physical Education")
                        }
                    }
                }
            }

            item { PlaygroundSection(text = "Buttons") }
            item {
                RoundedCardContainer {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceBright,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(16.dp),
                        ) {
                            Button(
                                onClick = {},
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                            ) {
                                Text(
                                    text = "Primary CTA",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(
                                    onClick = {},
                                    modifier = Modifier.size(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(0.dp),
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "Back",
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                                FilledTonalButton(onClick = {}) { Text("Tonal") }
                                TextButton(onClick = {}) { Text("Text") }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                FilledTonalIconButton(onClick = {}) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Add")
                                }
                                IconButton(onClick = {}) {
                                    Icon(Icons.Rounded.Info, contentDescription = "Info")
                                }
                                BadgedBox(
                                    badge = {
                                        Badge { Text("3", style = MaterialTheme.typography.labelSmall) }
                                    },
                                ) {
                                    Icon(Icons.Rounded.Email, contentDescription = null)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                FloatingActionButton(onClick = {}) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Add")
                                }
                                ExtendedFloatingActionButton(
                                    onClick = {},
                                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                                    text = { Text("Extended") },
                                )
                            }
                        }
                    }
                }
            }

            item { PlaygroundSection(text = "Chips") }
            item {
                RoundedCardContainer {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceBright,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(16.dp),
                        ) {
                            var chipPicked by remember { mutableStateOf(true) }
                            var assistCount by remember { mutableIntStateOf(0) }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                androidx.compose.material3.FilterChip(
                                    selected = chipPicked,
                                    onClick = { chipPicked = !chipPicked },
                                    label = { Text("Filter") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    },
                                )
                                AssistChip(
                                    onClick = { assistCount++ },
                                    label = { Text("Assist ($assistCount)") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Rounded.Search,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    },
                                )
                            }
                            var segmented by remember { mutableStateOf("b") }
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                listOf("a" to "Day", "b" to "Week").forEachIndexed { index, (value, label) ->
                                    SegmentedButton(
                                        selected = segmented == value,
                                        onClick = { segmented = value },
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { PlaygroundSection(text = "Fields & controls") }
            item {
                var text by remember { mutableStateOf("") }
                var checked by remember { mutableStateOf(true) }
                var slider by remember { mutableFloatStateOf(0.6f) }
                var radio by remember { mutableStateOf("a") }
                RoundedCardContainer {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceBright,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(16.dp),
                        ) {
                            androidx.compose.material3.OutlinedTextField(
                                value = text,
                                onValueChange = { text = it },
                                label = { Text("Outlined field") },
                                singleLine = true,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Switch(checked = checked, onCheckedChange = { checked = it })
                                Checkbox(checked = checked, onCheckedChange = { checked = it })
                                RadioButton(selected = radio == "a", onClick = { radio = "a" })
                                RadioButton(selected = radio == "b", onClick = { radio = "b" })
                            }
                            Slider(value = slider, onValueChange = { slider = it })
                            LinearProgressIndicator(
                                progress = { slider },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                Text("Loading", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            item { PlaygroundSection(text = "Sheets & dialogs") }
            item {
                RoundedCardContainer {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceBright,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(16.dp),
                        ) {
                            FilledTonalButton(onClick = { showSheet = true }) { Text("Sheet") }
                            OutlinedButton(onClick = { showDialog = true }) { Text("Dialog") }
                        }
                    }
                }
            }

            item { PlaygroundSection(text = "Shapes") }
            item {
                RoundedCardContainer {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceBright,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        ) {
                            CookieDemo(sides = 6, label = "6")
                            CookieDemo(sides = 8, label = "8")
                            CookieDemo(sides = 12, label = "12")
                        }
                    }
                }
            }

            item { PlaygroundSection(text = "Typography") }
            item {
                RoundedCardContainer {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceBright,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(16.dp),
                        ) {
                            Text("Headline", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("Title", style = MaterialTheme.typography.titleMedium)
                            Text("Body", style = MaterialTheme.typography.bodyMedium)
                            Text("Label", style = MaterialTheme.typography.labelMedium)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TonalDot(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, "P")
                                TonalDot(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, "S")
                                TonalDot(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, "T")
                                TonalDot(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, "E")
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showSheet) {
        AppBottomSheet(onDismissRequest = { showSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
            ) {
                Text(
                    text = "Demo sheet",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
                RoundedCardContainer {
                    FeatureCard(
                        title = "Sheet row",
                        description = "surfaceContainerHigh container",
                        icon = Icons.Rounded.Info,
                        onClick = { showSheet = false },
                    )
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { showSheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    Text(
                        text = "Close",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Demo dialog") },
            text = { Text("AlertDialog with 20dp fields and tonal buttons.") },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text("Got it") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun PlaygroundSection(text: String) {
    SectionLabel(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        startPadding = 16.dp,
        topPadding = 16.dp,
    )
}

@Composable
private fun PlaygroundToggleRows() {
    var first by remember { mutableStateOf(true) }
    RoundedCardContainer {
        IconToggleItem(
            icon = Icons.Rounded.Person,
            title = "Toggle row",
            description = "Divider plus switch trailing",
            checked = first,
            onCheckedChange = { first = it },
        )
        IconToggleItem(
            icon = Icons.Rounded.Favorite,
            title = "Disabled row",
            description = "Shows disabled state",
            checked = false,
            enabled = false,
            onCheckedChange = {},
        )
        com.enderplusbayzuiship.edupage2.ui.core.cards.SettingsNavigationRow(
            title = "Navigation row",
            description = "Chevron trailing",
            icon = Icons.AutoMirrored.Rounded.ArrowForward,
            onClick = {},
        )
    }
}

@Composable
private fun TonalDot(
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    label: String,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(container, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = content,
        )
    }
}

@Composable
private fun CookieDemo(sides: Int, label: String) {
    val container = MaterialTheme.colorScheme.primaryContainer
    val content = MaterialTheme.colorScheme.onPrimaryContainer
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .drawWithCache {
                    val poly = RoundedPolygon(
                        numVertices = sides,
                        rounding = CornerRounding(0.4f),
                    )
                    val androidPath = poly.toPath()
                    val path = Path()
                    val matrix = Matrix()
                    onDrawBehind {
                        path.reset()
                        path.addPath(androidPath.asComposePath())
                        matrix.reset()
                        val s = size.minDimension / 2f * 0.98f
                        matrix.scale(s, s)
                        matrix.translate(1f, 1f)
                        path.transform(matrix)
                        val bounds = path.getBounds()
                        path.translate(
                            Offset(
                                (size.width - bounds.width) / 2f - bounds.left,
                                (size.height - bounds.height) / 2f - bounds.top,
                            )
                        )
                        drawPath(path, color = container)
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = content,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "$sides-sided",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

