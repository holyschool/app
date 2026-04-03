package com.enderplusbayzuiship.edupage2.ui.messages

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.enderplusbayzuiship.edupage2.ui.util.ShimmerBox

@Composable
internal fun MessagesSkeleton(
    bottomPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = 8.dp,
            bottom = 16.dp + bottomPadding.calculateBottomPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        userScrollEnabled = false,
    ) {
        items(List(8) { it }) {
            MessageItemSkeleton()
        }
    }
}

@Composable
private fun MessageItemSkeleton() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {

            ShimmerBox(
                modifier = Modifier.size(36.dp),
                height = 36.dp,
                cornerRadius = 18.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {

                ShimmerBox(modifier = Modifier.fillMaxWidth(0.65f), height = 14.dp)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShimmerBox(modifier = Modifier.width(100.dp), height = 11.dp)
                    ShimmerBox(modifier = Modifier.width(56.dp), height = 18.dp, cornerRadius = 50.dp)
                }

                ShimmerBox(modifier = Modifier.fillMaxWidth(), height = 11.dp)
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.8f), height = 11.dp)
            }
            Spacer(Modifier.width(8.dp))

            ShimmerBox(modifier = Modifier.width(44.dp), height = 11.dp)
        }
    }
}
