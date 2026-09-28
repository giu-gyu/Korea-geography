package com.koreageo.quiz.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.koreageo.quiz.BuildConfig
import com.koreageo.quiz.quiz.QuizSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: QuizSettings,
    onSettingsChange: (QuizSettings) -> Unit,
    onClose: () -> Unit,
) {
    var showLicenses by rememberSaveable { mutableStateOf(false) }

    BackHandler {
        if (showLicenses) showLicenses = false else onClose()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (showLicenses) "오픈소스 고지" else "설정") },
                    navigationIcon = {
                        IconButton(onClick = { if (showLicenses) showLicenses = false else onClose() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                        }
                    },
                )
            },
        ) { padding ->
            if (showLicenses) {
                LicensesPage(modifier = Modifier.padding(padding))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState()),
                ) {
                    SectionTitle("힌트")
                    SwitchRow(
                        title = "글자수 힌트",
                        description = "정답을 입력하다 막히면 정답의 글자수를 알려줍니다.",
                        checked = settings.characterCountHintEnabled,
                        onCheckedChange = { onSettingsChange(settings.copy(characterCountHintEnabled = it)) },
                    )
                    SwitchRow(
                        title = "초성 힌트",
                        description = "정답을 입력하다 막히면 정답의 초성을 알려줍니다.",
                        checked = settings.choseongHintEnabled,
                        onCheckedChange = { onSettingsChange(settings.copy(choseongHintEnabled = it)) },
                    )
                    Text(
                        text = "정답 글자수보다 길게 입력하거나, 백스페이스로 2번 이상 지우면 켜둔 힌트가 자동으로 나타납니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    SectionTitle("정보")
                    NavRow(title = "오픈소스 고지", onClick = { showLicenses = true })
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("앱 버전", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            BuildConfig.VERSION_NAME,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
    )
}

@Composable
private fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun NavRow(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
}

@Composable
private fun LicensesPage(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val text = remember {
        runCatching {
            context.assets.open("licenses.txt").bufferedReader().use { it.readText() }
        }.getOrDefault("")
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    )
}
