package com.wiffles.edupage.ui.subjects

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wiffles.edupage.R
import com.wiffles.edupage.data.SubjectStyle
import com.wiffles.edupage.ui.core.sheets.AppBottomSheet

/**
 * Read-only overview of every subject the user has customized (icon and/or color).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectIconsOverviewSheet(
    onDismiss: () -> Unit,
    viewModel: SubjectIconsViewModel = hiltViewModel(),
) {
    val styles by viewModel.styles.collectAsState()
    val configured = remember(styles) { styles.entries.sortedBy { it.key } }

    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.subject_icons_view),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))

            if (configured.isEmpty()) {
                Text(
                    text = stringResource(R.string.subject_icons_view_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = stringResource(R.string.subject_icons_view_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    configured.forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SubjectAvatar(
                                subjectName = entry.key,
                                style = entry.value,
                                enabled = true,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.key,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = styleDetail(entry.value),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun styleDetail(style: SubjectStyle): String = when {
    style.iconKey != null && style.colorArgb != null -> stringResource(R.string.subject_icons_detail_both)
    style.iconKey != null -> stringResource(R.string.subject_icons_detail_icon)
    else -> stringResource(R.string.subject_icons_detail_color)
}
