package com.wiffles.edupage.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.rounded.BeachAccess
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.edupage.api.model.EduEvent
import com.edupage.api.model.EduEventKind
import com.wiffles.edupage.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Shared school-event presentation. The Overview card, the timetable day banners and the
 * week strip all render the same "akcie" data, so the styling lives here in one place.
 */

private val eventDateFmt = DateTimeFormatter.ofPattern("d MMM")

/**
 * What belongs on the event boards: tests and exams already live in assignments, and a
 * cancelled event is not news, so both stay off the Overview card and the timetable.
 */
internal fun showOnBoards(event: EduEvent): Boolean =
    event.kind != EduEventKind.EXAM && !event.cancelled

/** True when [date] falls inside the event, including multi-day holidays already under way. */
internal fun EduEvent.covers(date: LocalDate): Boolean =
    !this.date.isAfter(date) && !(endDate ?: this.date).isBefore(date)

/** Colour family for an event kind. Deliberately theme-based so light/dark stay readable. */
@Composable
internal fun eventAccent(kind: EduEventKind): Color = when (kind) {
    EduEventKind.TRIP, EduEventKind.MEETING -> MaterialTheme.colorScheme.primary
    EduEventKind.CULTURE, EduEventKind.SPORT -> MaterialTheme.colorScheme.secondary
    EduEventKind.HOLIDAY -> MaterialTheme.colorScheme.tertiary
    EduEventKind.SAFETY -> MaterialTheme.colorScheme.error
    EduEventKind.EXAM, EduEventKind.SCHOOL_EVENT, EduEventKind.OTHER -> MaterialTheme.colorScheme.primary
}

internal fun eventIcon(kind: EduEventKind): ImageVector = when (kind) {
    EduEventKind.TRIP -> Icons.Rounded.DirectionsBus
    EduEventKind.CULTURE -> Icons.Rounded.TheaterComedy
    EduEventKind.SPORT -> Icons.Rounded.SportsSoccer
    EduEventKind.HOLIDAY -> Icons.Rounded.BeachAccess
    EduEventKind.MEETING -> Icons.Rounded.Groups
    EduEventKind.SAFETY -> Icons.Rounded.HealthAndSafety
    EduEventKind.EXAM, EduEventKind.SCHOOL_EVENT, EduEventKind.OTHER -> Icons.Default.Event
}

@Composable
internal fun eventWhenLabel(event: EduEvent): String {
    val endDate = event.endDate
    val days = if (endDate != null && endDate.isAfter(event.date)) {
        "${formatEventDay(event.date)} – ${formatEventDay(endDate)}"
    } else {
        formatEventDay(event.date)
    }
    val period = event.period
    val periods = event.periods
    val time = when {
        !event.startTime.isNullOrBlank() && !event.endTime.isNullOrBlank() ->
            "${event.startTime}–${event.endTime}"
        period != null && periods != null && periods > 1 ->
            stringResource(R.string.event_period_range, period, period + periods - 1)
        period != null -> stringResource(R.string.event_period_single, period)
        event.allDay -> stringResource(R.string.event_all_day)
        else -> ""
    }
    return listOf(days, time).filter { it.isNotBlank() }.joinToString(" · ")
}

@Composable
internal fun formatEventDay(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> stringResource(R.string.event_today)
        today.plusDays(1) -> stringResource(R.string.event_tomorrow)
        else -> date.format(eventDateFmt)
    }
}

/** Teacher/room/class summary shown as one muted line under an event title. */
internal fun eventDetailLine(event: EduEvent): String =
    (event.teacherNames.take(2) + event.roomNames.take(1) + event.classNames.take(1))
        .filter { it.isNotBlank() }
        .joinToString(" · ")

@Composable
internal fun EventTypeChip(label: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

/** One compact list row used by the Overview events card. */
@Composable
internal fun EventRow(event: EduEvent) {
    val accent = eventAccent(event.kind)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = accent.copy(alpha = 0.15f),
            modifier = Modifier.size(36.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = eventIcon(event.kind),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = accent,
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (event.cancelled) TextDecoration.LineThrough else null,
                    modifier = Modifier.weight(1f, fill = false),
                )
                event.typeName?.takeIf { it.isNotBlank() }?.let { EventTypeChip(it) }
            }
            Text(
                text = eventWhenLabel(event),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val detail = eventDetailLine(event)
            if (detail.isNotBlank()) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Compact one-line strip shown above the lessons of a day that has an event. */
@Composable
internal fun DayEventBanner(event: EduEvent, modifier: Modifier = Modifier) {
    val accent = eventAccent(event.kind)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = accent.copy(alpha = 0.14f),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = eventIcon(event.kind),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = accent,
            )
            Text(
                text = event.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (event.cancelsTimetable) {
                Text(
                    text = stringResource(R.string.event_cancels_lessons),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * The prominent card used by the timetable: a trip, holiday or school event gets its own
 * tile with the type, when it happens, who it involves and whether lessons are cancelled.
 */
@Composable
internal fun EventHighlightCard(event: EduEvent, modifier: Modifier = Modifier) {
    val accent = eventAccent(event.kind)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = accent.copy(alpha = 0.12f),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = 0.20f),
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = eventIcon(event.kind),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = accent,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = event.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (event.cancelled) TextDecoration.LineThrough else null,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    event.typeName?.takeIf { it.isNotBlank() }?.let { EventTypeChip(it) }
                }
                Text(
                    text = eventWhenLabel(event),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val detail = eventDetailLine(event)
                if (detail.isNotBlank()) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (event.cancelsTimetable) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EventBusy,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = accent,
                        )
                        Text(
                            text = stringResource(R.string.event_cancels_lessons),
                            style = MaterialTheme.typography.labelMedium,
                            color = accent,
                        )
                    }
                }
            }
        }
    }
}

/** Title above a group of events, matching the Overview card section header. */
@Composable
internal fun EventsSectionHeader(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(9.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Event,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Slim tappable chip used by the week-mode events strip. */
@Composable
internal fun EventWeekChip(event: EduEvent, dayLabel: String, onClick: () -> Unit) {
    val accent = eventAccent(event.kind)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = accent.copy(alpha = 0.14f),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = eventIcon(event.kind),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = accent,
            )
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Text(
                    text = dayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** A small "no school today" style line for days without events. */
@Composable
internal fun EventFreeDayHint(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Schedule,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}