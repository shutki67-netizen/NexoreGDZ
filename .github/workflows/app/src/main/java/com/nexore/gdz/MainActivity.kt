package com.nexore.gdz

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val Blue = Color(0xFF1677F2)
private val Cyan = Color(0xFF13BDEB)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NexoreApp() }
    }
}

@Composable
private fun NexoreApp() {
    var dark by remember { mutableStateOf(true) }
    var photo by remember { mutableStateOf<Uri?>(null) }
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var followUp by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Работает на устройстве; интернет для решателя не нужен.") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        photo = uri
        if (uri != null) {
            busy = true
            status = "Распознаём текст на фото…"
            scope.launch {
                try {
                    val image = InputImage.fromFilePath(context, uri)
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    val result = recognizer.process(image).await()
                    question = result.text
                    status = if (question.isBlank()) "Текст не найден. Попробуй фото чётче или введи задание вручную." else "Текст распознан — проверь его перед решением."
                    recognizer.close()
                } catch (e: Exception) {
                    status = "Не удалось прочитать фото. Введи задание вручную. (${e.localizedMessage ?: "ошибка"})"
                } finally { busy = false }
            }
        }
    }
    val colors = if (dark) darkColorScheme(primary = Cyan, secondary = Blue, background = Color(0xFF08111F),
        surface = Color(0xFF111D30), onBackground = Color(0xFFEAF3FF), onSurface = Color(0xFFEAF3FF))
    else lightColorScheme(primary = Blue, secondary = Cyan, background = Color(0xFFF4F8FF),
        surface = Color.White, onBackground = Color(0xFF12233D), onSurface = Color(0xFF12233D))

    MaterialTheme(colorScheme = colors) {
        Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(Blue), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        Column {
                            Text("NexoreГДЗ", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                            Text("Учись понимать, а не угадывать", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    IconButton(onClick = { dark = !dark }) { Icon(if (dark) Icons.Default.LightMode else Icons.Default.DarkMode, "Сменить тему") }
                }
                Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Разберём задание", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Выбери фото или введи задание. Распознавание текста и поддерживаемые решения работают локально.")
                        OutlinedButton(onClick = { picker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.AddAPhoto, null); Spacer(Modifier.width(8.dp)); Text("Выбрать фото")
                        }
                        OutlinedTextField(question, { question = it }, modifier = Modifier.fillMaxWidth(),
                            label = { Text("Текст задания") }, placeholder = { Text("Например: 2x + 5 = 17") }, minLines = 3)
                        Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        Button(onClick = { answer = OfflineSolver.solve(question) }, enabled = !busy && question.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().height(50.dp)) {
                            Icon(Icons.Default.Calculate, null); Spacer(Modifier.width(8.dp)); Text("Решить и объяснить")
                        }
                    }
                }
                if (answer.isNotBlank()) {
                    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Решение", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(answer)
                            HorizontalDivider()
                            Text("Уточни объяснение", fontWeight = FontWeight.Bold)
                            OutlinedTextField(followUp, { followUp = it }, modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Например: объясни проще") })
                            Button(onClick = { answer += "\n\nТвой вопрос: $followUp\n\n${OfflineSolver.explainFollowUp(followUp, question)}"; followUp = "" },
                                enabled = followUp.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Chat, null); Spacer(Modifier.width(8.dp)); Text("Объяснить")
                            }
                        }
                    }
                }
                Text("Офлайн-режим: решатель поддерживает арифметику, проценты и простые линейные уравнения. Не все задания распознаются или решаются автоматически.", style = MaterialTheme.typography.bodySmall)
                Text("Nexore • 0.2.0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .55f))
            }
        }
    }
}


private object OfflineSolver {
    fun solve(raw: String): String {
        val s = raw.lowercase().replace(" ", "").replace(",", ".").replace("×", "*").replace("÷", "/")
        val equation = Regex("""([+-]?\d*\.?\d*)x([+-]\d+(?:\.\d+)?)=([+-]?\d+(?:\.\d+)?)""").find(s)
        if (equation != null) {
            val aText = equation.groupValues[1]
            val a = when (aText) { "", "+" -> 1.0; "-" -> -1.0; else -> aText.toDoubleOrNull() }
            val b = equation.groupValues[2].toDoubleOrNull()
            val c = equation.groupValues[3].toDoubleOrNull()
            if (a != null && b != null && c != null && a != 0.0) {
                val x = (c - b) / a
                return "Уравнение: $raw\n\n1. Переносим свободное число в правую часть: ${fmt(a)}x = ${fmt(c - b)}.\n2. Делим обе части на коэффициент при x: x = ${fmt(c - b)} / ${fmt(a)}.\n\nОтвет: x = ${fmt(x)}\n\nПроверка: подставь найденное значение в исходное уравнение. Это решатель простых уравнений вида ax+b=c."
            }
        }
        val percent = Regex("""(\d+(?:\.\d+)?)%от(\d+(?:\.\d+)?)""").find(s)
        if (percent != null) {
            val p = percent.groupValues[1].toDouble(); val n = percent.groupValues[2].toDouble()
            return "Найти $p% от $n.\n\n1. Переводим процент в дробь: $p / 100.\n2. Умножаем на число: ($p / 100) × $n.\n\nОтвет: ${fmt(p / 100 * n)}"
        }
        val arithmetic = Regex("""^[+-]?\d+(?:\.\d+)?[+\-*/][+-]?\d+(?:\.\d+)?$""").matchEntire(s)
        if (arithmetic != null) {
            val opMatch = Regex("""(?<!^)[+\-*/]""").find(s) ?: return unsupported()
            val op = opMatch.value
            val left = s.substring(0, opMatch.range.first).toDoubleOrNull() ?: return unsupported()
            val right = s.substring(opMatch.range.last + 1).toDoubleOrNull() ?: return unsupported()
            val result = when (op) { "+" -> left + right; "-" -> left - right; "*" -> left * right; "/" -> if (right != 0.0) left / right else null; else -> null }
            return if (result == null) "На ноль делить нельзя." else "Пример: $raw\n\n1. Определяем действие: ${when(op){"+"->"сложение";"-"->"вычитание";"*"->"умножение";else->"деление"}}.\n2. Вычисляем: ${fmt(left)} $op ${fmt(right)} = ${fmt(result)}.\n\nОтвет: ${fmt(result)}"
        }
        return unsupported()
    }
    fun explainFollowUp(q: String, task: String): String {
        val t = q.lowercase()
        return when {
            "проще" in t || "понят" in t -> "Разбей задание на маленькие шаги. Сначала определи, что требуется найти, затем выбери правило и выполни одно действие за раз. Я пока поддерживаю только несколько типов задач без ИИ."
            "провер" in t -> "Проверка зависит от типа задания. Для уравнения подставь найденное значение обратно в исходную запись; для арифметики повтори вычисление обратным действием."
            "друг" in t || "пример" in t -> "Попробуй похожий пример: 3x + 4 = 19. Вычти 4 с обеих сторон, затем раздели обе части на 3."
            "почему" in t -> "Это правило используется, чтобы сохранить равенство: одно и то же действие выполняется с обеими частями. Автоматические уточнения пока основаны на простых правилах, а не на свободном ИИ-диалоге."
            else -> "В офлайн-версии ответы на уточнения формируются по ограниченному набору правил. Для произвольных вопросов понадобится ИИ-модель или более широкая база объяснений.\nТекущее задание: $task"
        }
    }
    private fun fmt(n: Double) = if (n.isFinite() && n == n.toLong().toDouble()) n.toLong().toString() else "%.4f".format(java.util.Locale.US, n).trimEnd('0').trimEnd('.')
    private fun unsupported() = "Пока не могу надёжно решить такой формат автоматически.\n\nПопробуй ввести задание проще, например:\n• 2x+5=17\n• 15%от200\n• 12+8\n\nЭто версия без ИИ: новые типы заданий нужно добавлять отдельными алгоритмами."
}
