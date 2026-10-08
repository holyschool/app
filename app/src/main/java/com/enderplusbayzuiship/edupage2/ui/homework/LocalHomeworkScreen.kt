package com.enderplusbayzuiship.edupage2.ui.homework

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val editorOpen = creating || editing != null

    BackHandler(enabled = editorOpen) {
        creating = false
        editing = null
    }

    if (editorOpen) {
        HomeworkEditorScreen(
            initial = editing,
            existingSubjects = remember(items) {
                items.map { it.subject.trim() }.filter { it.isNotBlank() }.distinct().sorted()
            },
            onDismiss = {
                creating = false
                editing = null
            },
            onSave = { item ->
                viewModel.addOrUpdate(item)
                creating = false
                editing = null
            },
            onDelete = { id ->
                viewModel.remove(id)
                creating = false
                editing = null
            },
        )
    } else {
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
    val buckets = remember(items, today) { bucketize(items, today) }

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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
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
                                            onEdit = { onEdit(item) },
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    onEdit: () -> Unit,
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
            onEdit()
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
                icon = Icons.Rounded.MenuBook,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeworkEditorScreen(
    initial: HomeworkItem?,
    existingSubjects: List<String>,
    onDismiss: () -> Unit,
    onSave: (HomeworkItem) -> Unit,
    onDelete: (String) -> Unit,
) {
    var title by remember(initial?.id) { mutableStateOf(initial?.title ?: "") }
    var date by remember(initial?.id) { mutableStateOf(initial?.date ?: LocalDate.now().toString()) }
    var subject by remember(initial?.id) { mutableStateOf(initial?.subject ?: "") }
    var notes by remember(initial?.id) { mutableStateOf(initial?.notes ?: "") }
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
            if (existingSubjects.isNotEmpty()) {
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
                        existingSubjects
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
