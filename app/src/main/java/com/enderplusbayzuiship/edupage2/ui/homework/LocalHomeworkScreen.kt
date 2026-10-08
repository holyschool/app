package com.enderplusbayzuiship.edupage2.ui.homework

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.data.HomeworkItem
import com.enderplusbayzuiship.edupage2.ui.core.cards.PastelIcon
import com.enderplusbayzuiship.edupage2.ui.core.containers.RoundedCardContainer
import com.enderplusbayzuiship.edupage2.ui.core.containers.SectionLabel
import com.enderplusbayzuiship.edupage2.ui.core.sheets.AppBottomSheet
import com.enderplusbayzuiship.edupage2.ui.util.rememberAppHaptics
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private enum class HomeworkBucket(val titleRes: Int) {
    OVERDUE(R.string.homework_overdue),
    TODAY(R.string.homework_today),
    TOMORROW(R.string.homework_tomorrow),
    WEEK(R.string.homework_this_week),
    LATER(R.string.homework_later),
    DONE(R.string.homework_done),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalHomeworkScreen(
    onBack: () -> Unit,
    viewModel: LocalHomeworkViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsState()
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<HomeworkItem?>(null) }
    val haptics = rememberAppHaptics()

    LaunchedEffect(Unit) {
        viewModel.reload()
    }

    val existingSubjects = remember(items) {
        items.map { it.subject.trim() }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val edupageSubjects by viewModel.subjects.collectAsState()

    BackHandler(enabled = editing != null) {
        editing = null
    }

    when {
        editing != null -> HomeworkEditorScreen(
            initial = editing,
            existingSubjects = existingSubjects,
            eduSubjects = edupageSubjects,
            onDismiss = { editing = null },
            onSave = { item ->
                viewModel.addOrUpdate(item)
                editing = null
            },
            onDelete = { id ->
                viewModel.remove(id)
                editing = null
            },
        )

        else -> {
            HomeworkListScreen(
                items = items,
                onBack = onBack,
                onCreate = {
                    haptics.virtualKey()
                    creating = true
                },
                onEdit = { editing = it },
                onToggle = { viewModel.toggleDone(it) },
                onDelete = { viewModel.remove(it) },
            )
            if (creating) {
                HomeworkCreateWizard(
                    existingSubjects = (edupageSubjects + existingSubjects).distinct().sorted(),
                    onDismiss = { creating = false },
                    onSave = { item ->
                        viewModel.addOrUpdate(item)
                        creating = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeworkListScreen(
    items: List<HomeworkItem>,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (HomeworkItem) -> Unit,
    onToggle: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    val haptics = rememberAppHaptics()
    val today = remember { LocalDate.now() }
    var filterTab by rememberSaveable { mutableStateOf(HomeworkTab.ACTIVE) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var detailItem by remember { mutableStateOf<HomeworkItem?>(null) }

    val visibleItems = remember(items, filterTab, searchQuery) {
        val byTab = when (filterTab) {
            HomeworkTab.ACTIVE -> items.filter { !it.done }
            HomeworkTab.DONE -> items.filter { it.done }
            HomeworkTab.ALL -> items
        }
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) {
            byTab
        } else {
            byTab.filter {
                it.title.lowercase().contains(q) ||
                    it.subject.lowercase().contains(q) ||
                    it.notes.lowercase().contains(q)
            }
        }
    }
    val buckets = remember(visibleItems, today) { bucketize(visibleItems, today) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.homework_title),
                        style = MaterialTheme.typography.titleLarge,
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
                actions = {
                    FilledTonalIconButton(onClick = {
                        haptics.virtualKey()
                        showSearch = !showSearch
                        if (!showSearch) searchQuery = ""
                    }) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = stringResource(R.string.messages_search),
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    FilledTonalIconButton(onClick = onCreate) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.homework_add),
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreate) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.homework_add))
            }
        },
    ) { paddingValues ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.homework_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    HomeworkTab.entries.forEachIndexed { index, tab ->
                        SegmentedButton(
                            selected = filterTab == tab,
                            onClick = {
                                haptics.virtualKey()
                                filterTab = tab
                            },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = HomeworkTab.entries.size,
                            ),
                            label = {
                                Text(
                                    text = stringResource(tab.labelRes),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            },
                        )
                    }
                }

                AnimatedVisibility(visible = showSearch) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(stringResource(R.string.messages_search_hint)) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = null)
                                }
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }

                if (visibleItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.messages_no_results),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        HomeworkBucket.entries.forEach { bucket ->
                            val bucketItems = buckets[bucket].orEmpty()
                            if (bucketItems.isNotEmpty()) {
                                item(key = "hw_header_${bucket.name}") {
                                    HomeworkGroupHeader(
                                        text = stringResource(bucket.titleRes),
                                        count = bucketItems.size,
                                        highlight = bucket == HomeworkBucket.OVERDUE,
                                    )
                                }
                                item(key = "hw_card_${bucket.name}") {
                                    RoundedCardContainer {
                                        bucketItems.forEach { item ->
                                            key(item.id) {
                                                HomeworkRow(
                                                    item = item,
                                                    dimmed = bucket == HomeworkBucket.DONE,
                                                    onToggle = { onToggle(item.id) },
                                                    onOpen = { detailItem = item },
                                                    onDelete = { onDelete(item.id) },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    detailItem?.let { detail ->
        HomeworkDetailSheet(
            item = detail,
            onDismiss = { detailItem = null },
            onToggle = {
                onToggle(detail.id)
                detailItem = null
            },
            onEdit = {
                detailItem = null
                onEdit(detail)
            },
            onDelete = {
                onDelete(detail.id)
                detailItem = null
            },
        )
    }
}

private enum class HomeworkTab(val labelRes: Int) {
    ACTIVE(R.string.homework_pending),
    DONE(R.string.homework_done),
    ALL(R.string.messages_filter_all),
}

private fun bucketize(
    items: List<HomeworkItem>,
    today: LocalDate,
): Map<HomeworkBucket, List<HomeworkItem>> {
    val pending = items.filter { !it.done }
    val done = items.filter { it.done }.sortedBy { it.date }
    fun dateOf(item: HomeworkItem): LocalDate? = runCatching { LocalDate.parse(item.date) }.getOrNull()
    return mapOf(
        HomeworkBucket.OVERDUE to pending.filter { (dateOf(it)?.isBefore(today)) == true }.sortedBy { it.date },
        HomeworkBucket.TODAY to pending.filter { dateOf(it) == today }.sortedBy { it.date },
        HomeworkBucket.TOMORROW to pending.filter { dateOf(it) == today.plusDays(1) }.sortedBy { it.date },
        HomeworkBucket.WEEK to pending.filter {
            val d = dateOf(it)
            d != null && d.isAfter(today.plusDays(1)) && !d.isAfter(today.plusDays(7))
        }.sortedBy { it.date },
        HomeworkBucket.LATER to pending.filter {
            val d = dateOf(it)
            d == null || d.isAfter(today.plusDays(7))
        }.sortedBy { it.date },
        HomeworkBucket.DONE to done,
    )
}

@Composable
private fun HomeworkGroupHeader(
    text: String,
    count: Int,
    highlight: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 16.dp, bottom = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            color = if (highlight) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HomeworkRow(
    item: HomeworkItem,
    dimmed: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val haptics = rememberAppHaptics()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (dimmed) {
            MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surfaceBright
        },
        onClick = {
            haptics.virtualKey()
            onOpen()
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {
                haptics.virtualKey()
                onToggle()
            }) {
                Icon(
                    imageVector = if (item.done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = stringResource(R.string.homework_toggle_done),
                    tint = if (item.done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PastelIcon(
                icon = homeworkIconVector(item.iconKey),
                key = item.subject.ifBlank { item.title },
                containerSize = 40.dp,
                iconSize = 20.dp,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 6.dp, horizontal = 10.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (item.done) TextDecoration.LineThrough else null,
                    color = if (item.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.subject.isNotBlank()) {
                        Text(
                            text = item.subject,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = " · ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    val overdue = remember(item.date, item.done) { isOverdue(item.date) && !item.done }
                    Text(
                        text = formatDate(item.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (overdue) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (overdue) FontWeight.SemiBold else null,
                    )
                }
                if (item.notes.isNotBlank()) {
                    Text(
                        text = item.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                item.sourceLabel?.let { label ->
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Email,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            IconButton(onClick = {
                haptics.virtualKey()
                onDelete()
            }) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.homework_delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private enum class HomeworkIcon(val key: String, val icon: ImageVector) {
    BOOK("book", Icons.Rounded.MenuBook),
    READING("reading", Icons.Rounded.AutoStories),
    MATH("math", Icons.Rounded.Calculate),
    SCIENCE("science", Icons.Rounded.Science),
    LANGUAGE("language", Icons.Rounded.Language),
    HISTORY("history", Icons.Rounded.HistoryEdu),
    ART("art", Icons.Rounded.Palette),
    MUSIC("music", Icons.Rounded.MusicNote),
    SPORT("sport", Icons.Rounded.SportsSoccer),
    COMPUTER("computer", Icons.Rounded.Computer),
    GEOGRAPHY("geography", Icons.Rounded.Public),
    WRITING("writing", Icons.Rounded.Edit);

    companion object {
        fun fromKey(key: String?): HomeworkIcon? = entries.firstOrNull { it.key == key }
    }
}

private fun homeworkIconVector(key: String?): ImageVector =
    HomeworkIcon.fromKey(key)?.icon ?: Icons.Rounded.MenuBook

@Composable
private fun HomeworkIconPicker(
    selectedKey: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberAppHaptics()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HomeworkIcon.entries.forEach { option ->
            val isSelected = selectedKey == option.key
            Surface(
                onClick = {
                    haptics.virtualKey()
                    onSelect(if (isSelected) null else option.key)
                },
                shape = CircleShape,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceBright
                },
                modifier = Modifier.size(52.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = null,
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

private enum class CreateStep(val questionRes: Int) {
    TITLE(R.string.homework_step_title),
    SUBJECT(R.string.homework_step_subject),
    DATE(R.string.homework_step_date),
    ICON(R.string.homework_step_icon),
    NOTES(R.string.homework_step_notes);

    val optional: Boolean get() = this != TITLE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeworkCreateWizard(
    existingSubjects: List<String>,
    onDismiss: () -> Unit,
    onSave: (HomeworkItem) -> Unit,
) {
    val haptics = rememberAppHaptics()
    val steps = CreateStep.entries
    var step by remember { mutableStateOf(CreateStep.TITLE) }
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var notes by remember { mutableStateOf("") }
    var iconKey by remember { mutableStateOf<String?>(null) }
    var ownSubject by remember { mutableStateOf(existingSubjects.isEmpty()) }
    var subjectMenuExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val untitledLabel = stringResource(R.string.homework_untitled)
    val today = remember { LocalDate.now() }
    val fieldShape = RoundedCornerShape(24.dp)
    val stepIndex = steps.indexOf(step)
    val isLast = stepIndex >= steps.lastIndex

    fun save() {
        onSave(
            HomeworkItem(
                title = title.trim().ifBlank { untitledLabel },
                date = date.trim().ifBlank { today.toString() },
                subject = subject.trim(),
                notes = notes.trim(),
                iconKey = iconKey,
            )
        )
    }

    fun advance() {
        if (isLast) save() else step = steps[stepIndex + 1]
    }

    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 4.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.homework_step_of, stepIndex + 1, steps.size),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                LinearProgressIndicator(
                    progress = { (stepIndex + 1).toFloat() / steps.size },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50)),
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
            }

            Text(
                text = stringResource(step.questionRes),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            when (step) {
                CreateStep.TITLE -> OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text(stringResource(R.string.homework_step_title_hint)) },
                    singleLine = true,
                    shape = fieldShape,
                    modifier = Modifier.fillMaxWidth(),
                )

                CreateStep.SUBJECT -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = subjectMenuExpanded,
                        onExpandedChange = { subjectMenuExpanded = !subjectMenuExpanded },
                    ) {
                        OutlinedTextField(
                            value = when {
                                ownSubject -> stringResource(R.string.homework_subject_own)
                                else -> subject
                            },
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text(stringResource(R.string.homework_subject_label)) },
                            singleLine = true,
                            shape = fieldShape,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectMenuExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = subjectMenuExpanded,
                            onDismissRequest = { subjectMenuExpanded = false },
                        ) {
                            existingSubjects.forEach { suggestion ->
                                DropdownMenuItem(
                                    text = { Text(suggestion) },
                                    onClick = {
                                        haptics.virtualKey()
                                        ownSubject = false
                                        subject = suggestion
                                        subjectMenuExpanded = false
                                    },
                                )
                            }
                            if (existingSubjects.isNotEmpty()) {
                                HorizontalDivider()
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.homework_subject_own)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    haptics.virtualKey()
                                    ownSubject = true
                                    subject = ""
                                    subjectMenuExpanded = false
                                },
                            )
                        }
                    }
                    if (ownSubject) {
                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            placeholder = { Text(stringResource(R.string.homework_subject_label)) },
                            singleLine = true,
                            shape = fieldShape,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                CreateStep.DATE -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DateQuickChip(
                            label = stringResource(R.string.homework_today),
                            selected = date == today.toString(),
                            onClick = {
                                haptics.virtualKey()
                                date = today.toString()
                            },
                        )
                        DateQuickChip(
                            label = stringResource(R.string.homework_tomorrow),
                            selected = date == today.plusDays(1).toString(),
                            onClick = {
                                haptics.virtualKey()
                                date = today.plusDays(1).toString()
                            },
                        )
                        DateQuickChip(
                            label = stringResource(R.string.homework_next_week),
                            selected = date == today.plusDays(7).toString(),
                            onClick = {
                                haptics.virtualKey()
                                date = today.plusDays(7).toString()
                            },
                        )
                    }
                    Surface(
                        onClick = {
                            haptics.virtualKey()
                            showDatePicker = true
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceBright,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        ) {
                            PastelIcon(icon = Icons.Rounded.CalendarMonth, key = date)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.homework_date_label),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = formatDate(date),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }

                CreateStep.ICON -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    HomeworkIconPicker(
                        selectedKey = iconKey,
                        onSelect = { iconKey = it },
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        PastelIcon(
                            icon = homeworkIconVector(iconKey),
                            key = subject.ifBlank { title },
                            containerSize = 44.dp,
                            iconSize = 22.dp,
                        )
                        Text(
                            text = title.ifBlank { untitledLabel },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                CreateStep.NOTES -> OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text(stringResource(R.string.homework_step_notes_hint)) },
                    shape = fieldShape,
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (stepIndex > 0) {
                    TextButton(onClick = {
                        haptics.virtualKey()
                        step = steps[stepIndex - 1]
                    }) {
                        Text(stringResource(R.string.homework_back))
                    }
                }
                Spacer(Modifier.weight(1f))
                if (step.optional) {
                    TextButton(onClick = {
                        haptics.virtualKey()
                        when (step) {
                            CreateStep.SUBJECT -> {
                                subject = ""
                                ownSubject = false
                            }
                            CreateStep.ICON -> iconKey = null
                            CreateStep.NOTES -> notes = ""
                            else -> Unit
                        }
                        advance()
                    }) {
                        Text(stringResource(R.string.homework_skip))
                    }
                }
                Button(
                    onClick = {
                        haptics.virtualKey()
                        advance()
                    },
                    enabled = step != CreateStep.TITLE || title.isNotBlank(),
                    modifier = Modifier.height(52.dp),
                ) {
                    Text(
                        text = stringResource(
                            if (isLast) R.string.homework_finish else R.string.homework_next
                        ),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = if (isLast) Icons.Rounded.CheckCircle
                        else Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = remember(date) {
                runCatching {
                    LocalDate.parse(date.ifBlank { today.toString() })
                        .atStartOfDay(java.time.ZoneOffset.UTC)
                        .toInstant()
                        .toEpochMilli()
                }.getOrNull()
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    haptics.virtualKey()
                    pickerState.selectedDateMillis?.let { millis ->
                        date = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneOffset.UTC)
                            .toLocalDate()
                            .toString()
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.homework_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.homework_cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeworkEditorScreen(
    initial: HomeworkItem?,
    existingSubjects: List<String>,
    eduSubjects: List<String>,
    onDismiss: () -> Unit,
    onSave: (HomeworkItem) -> Unit,
    onDelete: (String) -> Unit,
) {
    var title by remember(initial?.id) { mutableStateOf(initial?.title ?: "") }
    var date by remember(initial?.id) { mutableStateOf(initial?.date ?: LocalDate.now().toString()) }
    var subject by remember(initial?.id) { mutableStateOf(initial?.subject ?: "") }
    var notes by remember(initial?.id) { mutableStateOf(initial?.notes ?: "") }
    var iconKey by remember(initial?.id) { mutableStateOf(initial?.iconKey) }
    val allSubjects = remember(existingSubjects, eduSubjects) {
        (eduSubjects + existingSubjects).distinct().sorted()
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var subjectExpanded by remember { mutableStateOf(false) }
    val untitledLabel = stringResource(R.string.homework_untitled)
    val haptics = rememberAppHaptics()
    val fieldShape = RoundedCornerShape(20.dp)
    val today = remember { LocalDate.now() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (initial == null) R.string.homework_add else R.string.homework_edit
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        haptics.virtualKey()
                        onDismiss()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    if (initial != null) {
                        IconButton(onClick = {
                            haptics.virtualKey()
                            onDelete(initial.id)
                        }) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = stringResource(R.string.homework_delete),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    haptics.virtualKey()
                    onSave(
                        HomeworkItem(
                            id = initial?.id ?: java.util.UUID.randomUUID().toString(),
                            title = title.trim().ifBlank { untitledLabel },
                            date = date.trim().ifBlank { LocalDate.now().toString() },
                            subject = subject.trim(),
                            notes = notes.trim(),
                            done = initial?.done ?: false,
                            createdAtMs = initial?.createdAtMs ?: System.currentTimeMillis(),
                            sourceTimelineId = initial?.sourceTimelineId,
                            sourceLabel = initial?.sourceLabel,
                            iconKey = iconKey,
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(16.dp)
                    .height(56.dp),
            ) {
                Text(
                    text = stringResource(R.string.homework_save),
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
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.homework_title_label)) },
                singleLine = true,
                shape = fieldShape,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionLabel(
                text = stringResource(R.string.homework_subject_label),
                modifier = Modifier.padding(bottom = 0.dp),
            )
            if (allSubjects.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = subjectExpanded,
                    onExpandedChange = { subjectExpanded = !subjectExpanded },
                ) {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = {
                            subject = it
                            subjectExpanded = true
                        },
                        placeholder = { Text(stringResource(R.string.homework_subject_label)) },
                        singleLine = true,
                        shape = fieldShape,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = subjectExpanded,
                        onDismissRequest = { subjectExpanded = false },
                    ) {
                        allSubjects
                            .filter { it.contains(subject, ignoreCase = true) }
                            .take(6)
                            .forEach { suggestion ->
                                DropdownMenuItem(
                                    text = { Text(suggestion) },
                                    onClick = {
                                        haptics.virtualKey()
                                        subject = suggestion
                                        subjectExpanded = false
                                    },
                                )
                            }
                    }
                }
            } else {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    placeholder = { Text(stringResource(R.string.homework_subject_label)) },
                    singleLine = true,
                    shape = fieldShape,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SectionLabel(
                text = stringResource(R.string.homework_date_label),
                modifier = Modifier.padding(bottom = 0.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                DateQuickChip(
                    label = formatDate(today.toString()),
                    selected = date == today.toString(),
                    onClick = {
                        haptics.virtualKey()
                        date = today.toString()
                    },
                )
                DateQuickChip(
                    label = formatDate(today.plusDays(1).toString()),
                    selected = date == today.plusDays(1).toString(),
                    onClick = {
                        haptics.virtualKey()
                        date = today.plusDays(1).toString()
                    },
                )
                DateQuickChip(
                    label = formatDate(today.plusDays(7).toString()),
                    selected = date == today.plusDays(7).toString(),
                    onClick = {
                        haptics.virtualKey()
                        date = today.plusDays(7).toString()
                    },
                )
            }
            Surface(
                onClick = {
                    haptics.virtualKey()
                    showDatePicker = true
                },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceBright,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    PastelIcon(
                        icon = Icons.Rounded.CalendarMonth,
                        key = date,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.homework_date_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = formatDate(date.ifBlank { LocalDate.now().toString() }),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            SectionLabel(
                text = stringResource(R.string.homework_notes_label),
                modifier = Modifier.padding(bottom = 0.dp),
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                placeholder = { Text(stringResource(R.string.homework_notes_label)) },
                shape = fieldShape,
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionLabel(
                text = stringResource(R.string.homework_step_icon),
                modifier = Modifier.padding(bottom = 0.dp),
            )
            HomeworkIconPicker(
                selectedKey = iconKey,
                onSelect = { iconKey = it },
            )
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = remember(date) {
                runCatching {
                    LocalDate.parse(date.ifBlank { LocalDate.now().toString() })
                        .atStartOfDay(java.time.ZoneOffset.UTC)
                        .toInstant()
                        .toEpochMilli()
                }.getOrNull()
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    haptics.virtualKey()
                    pickerState.selectedDateMillis?.let { millis ->
                        date = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneOffset.UTC)
                            .toLocalDate()
                            .toString()
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.homework_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.homework_cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun DateQuickChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeworkDetailSheet(
    item: HomeworkItem,
    onDismiss: () -> Unit,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val haptics = rememberAppHaptics()
    val overdue = remember(item.date, item.done) { isOverdue(item.date) && !item.done }

    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PastelIcon(
                    icon = homeworkIconVector(item.iconKey),
                    key = item.subject.ifBlank { item.title },
                    containerSize = 56.dp,
                    iconSize = 28.dp,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (item.done) TextDecoration.LineThrough else null,
                        color = if (item.done) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val (statusText, statusColor) = when {
                            item.done -> stringResource(R.string.homework_done) to MaterialTheme.colorScheme.primary
                            overdue -> stringResource(R.string.homework_overdue) to MaterialTheme.colorScheme.error
                            else -> formatDate(item.date) to MaterialTheme.colorScheme.secondary
                        }
                        StatusPill(text = statusText, color = statusColor)
                        if (item.done) {
                            StatusPill(
                                text = formatDate(item.date),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            RoundedCardContainer {
                if (item.subject.isNotBlank()) {
                    DetailInfoRow(
                        icon = Icons.Rounded.MenuBook,
                        label = stringResource(R.string.homework_detail_subject),
                        value = item.subject,
                    )
                }
                DetailInfoRow(
                    icon = Icons.Rounded.CalendarMonth,
                    label = stringResource(R.string.homework_detail_date),
                    value = formatDate(item.date),
                )
                item.sourceLabel?.let { label ->
                    DetailInfoRow(
                        icon = Icons.Default.Search,
                        label = stringResource(R.string.homework_detail_source),
                        value = label,
                    )
                }
            }

            if (item.notes.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(
                        text = stringResource(R.string.homework_detail_notes),
                        modifier = Modifier.padding(bottom = 0.dp),
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceBright,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = item.notes,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = {
                        haptics.virtualKey()
                        onToggle()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                ) {
                    Icon(
                        imageVector = if (item.done) {
                            Icons.Rounded.RadioButtonUnchecked
                        } else {
                            Icons.Rounded.CheckCircle
                        },
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(
                            if (item.done) R.string.homework_detail_mark_undone
                            else R.string.homework_detail_mark_done
                        ),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                FilledTonalButton(
                    onClick = {
                        haptics.virtualKey()
                        onEdit()
                    },
                    modifier = Modifier.height(52.dp),
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null)
                }
                OutlinedButton(
                    onClick = {
                        haptics.virtualKey()
                        onDelete()
                    },
                    modifier = Modifier.height(52.dp),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.homework_delete))
                }
            }
        }
    }
}

@Composable
private fun StatusPill(text: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

@Composable
private fun DetailInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceBright,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PastelIcon(icon = icon, key = label, containerSize = 36.dp, iconSize = 18.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private fun isOverdue(iso: String): Boolean {
    return try {
        LocalDate.parse(iso).isBefore(LocalDate.now())
    } catch (e: Exception) {
        false
    }
}

private fun formatDate(iso: String): String {
    return try {
        val date = LocalDate.parse(iso)
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()))
    } catch (e: Exception) {
        iso
    }
}
