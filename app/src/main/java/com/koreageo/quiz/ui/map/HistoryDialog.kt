package com.koreageo.quiz.ui.map

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.koreageo.quiz.history.HistoryEntry
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryDialog(
    entries: List<HistoryEntry>,
    onDeleteEntry: (HistoryEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    var localEntries by remember(entries) { mutableStateOf(entries) }
    var entryPendingDelete by remember { mutableStateOf<HistoryEntry?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.widthIn(max = 360.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "완료 기록",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(12.dp))

                if (localEntries.isEmpty()) {
                    Text(
                        text = "아직 완료한 기록이 없습니다. 도전을 완료하면 여기에 남습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    Text(
                        text = "항목을 길게 누르면 삭제할 수 있습니다.",
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Spacer(Modifier.height(4.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items(localEntries, key = { it.completedAtMillis }) { entry ->
                            HistoryRow(entry, onLongPress = { entryPendingDelete = entry })
                            HorizontalDivider()
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("닫기")
                }
            }
        }
    }

    val target = entryPendingDelete
    if (target != null) {
        AlertDialog(
            onDismissRequest = { entryPendingDelete = null },
            title = { Text("기록 삭제") },
            text = { Text("\"${target.levelName} ${target.revealedCount}/${target.totalCount}\" 기록을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteEntry(target)
                    localEntries = localEntries.filterNot { it.completedAtMillis == target.completedAtMillis }
                    entryPendingDelete = null
                }) { Text("예") }
            },
            dismissButton = {
                TextButton(onClick = { entryPendingDelete = null }) { Text("아니오") }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryRow(entry: HistoryEntry, onLongPress: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = "${entry.levelName} ${entry.revealedCount}/${entry.totalCount}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = formatTimestamp(entry.completedAtMillis),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            text = formatDuration(entry.durationMillis),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

private val TIMESTAMP_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA)

private fun formatTimestamp(millis: Long): String = TIMESTAMP_FORMAT.format(millis)

fun formatDuration(durationMillis: Long): String {
    val totalSeconds = durationMillis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
