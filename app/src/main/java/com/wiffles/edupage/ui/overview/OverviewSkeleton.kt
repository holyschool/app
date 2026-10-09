package com.wiffles.edupage.ui.overview

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wiffles.edupage.ui.core.containers.RoundedCardContainer
import com.wiffles.edupage.ui.util.ShimmerBox

@Composable
internal fun OverviewSkeleton(
    bottomPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp, end = 16.dp,
                top = 8.dp,
                bottom = 16.dp + bottomPadding.calculateBottomPadding(),
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TimetableCardSkeleton()
        SkeletonCard { GradesBodySkeleton() }
        SkeletonCard { MessagesBodySkeleton() }
        QuickActionsSkeleton()
    }
}

@Composable
private fun TimetableCardSkeleton() {
    SkeletonCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ShimmerBox(modifier = Modifier.size(18.dp), cornerRadius = 4.dp)
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.45f), height = 16.dp)
            }
            Spacer(Modifier.height(2.dp))
            CurrentLessonBannerSkeleton()
            repeat(3) { CompactLessonRowSkeleton() }
        }
    }
}

@Composable
internal fun CurrentLessonBannerSkeleton() {
    ShimmerBox(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        cornerRadius = 20.dp,
    )
}

@Composable
internal fun CompactLessonRowSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ShimmerBox(modifier = Modifier.size(width = 40.dp, height = 14.dp), cornerRadius = 4.dp)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.6f), height = 14.dp)
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.35f), height = 11.dp)
        }
        ShimmerBox(modifier = Modifier.size(width = 32.dp, height = 14.dp), cornerRadius = 4.dp)
    }
}

@Composable
private fun GradesBodySkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CardHeaderSkeleton()
        Spacer(Modifier.height(4.dp))
        repeat(3) { GradeRowSkeleton() }
    }
}

@Composable
private fun MessagesBodySkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CardHeaderSkeleton()
        Spacer(Modifier.height(4.dp))
        repeat(3) { MessageRowSkeleton() }
    }
}

@Composable
private fun CardHeaderSkeleton() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ShimmerBox(modifier = Modifier.size(18.dp), cornerRadius = 4.dp)
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.4f), height = 16.dp)
    }
}

@Composable
internal fun GradeRowSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ShimmerBox(modifier = Modifier.size(32.dp), cornerRadius = 8.dp)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.5f), height = 14.dp)
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.3f), height = 11.dp)
        }
        ShimmerBox(modifier = Modifier.size(width = 36.dp, height = 14.dp), cornerRadius = 4.dp)
    }
}

@Composable
internal fun MessageRowSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ShimmerBox(
            modifier = Modifier.size(36.dp),
            cornerRadius = 18.dp,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.5f), height = 14.dp)
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.7f), height = 11.dp)
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.4f), height = 11.dp)
        }
        ShimmerBox(modifier = Modifier.size(width = 40.dp, height = 11.dp), cornerRadius = 4.dp)
    }
}

@Composable
private fun QuickActionsSkeleton() {
    SkeletonCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.35f), height = 16.dp)
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                repeat(3) {
                    ShimmerBox(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        cornerRadius = 20.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun SkeletonCard(content: @Composable () -> Unit) {
    RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceBright,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

