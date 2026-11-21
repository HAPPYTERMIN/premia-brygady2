package com.premiabrygady.app.ui

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.premiabrygady.app.CalculatorState
import com.premiabrygady.app.HistoryEntry
import com.premiabrygady.app.HistoryPerson
import com.premiabrygady.app.PdfExporter
import com.premiabrygady.app.PdfRow
import com.premiabrygady.app.PremiumCalculator
import com.premiabrygady.app.Storage
import java.time.Instant
import java.util.Date

private data class PersonUi(var name: String, var hoursText: String)

@Composable
fun PremiaBrygadyApp() {
    val context = LocalContext.current
    val storage = remember { Storage(context) }
    var tab by rememberSaveable { mutableIntStateOf(0) }

    val initial = remember { storage.loadCalculator() }
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var amountText by rememberSaveable { mutableStateOf(initial?.amountText ?: "") }
    val persons = remember {
        mutableStateListOf<PersonUi>().apply {
            val loaded = initial?.persons
            if (loaded.isNullOrEmpty()) {
                add(PersonUi("", ""))
                add(PersonUi("", ""))
            } else {
                loaded.forEach { add(PersonUi(it.name, it.hoursText)) }
            }
        }
    }
    var history by remember { mutableStateOf(storage.loadHistory()) }

    // Automatyczny zapis stanu kalkulatora. Dzieki temu stan przetrwa
    // obrot ekranu (aktywnosc jest odtwarzana ze Storage) i ponowne
    // uruchomienie aplikacji. Zakladka trzymana jest w rememberSaveable.
    LaunchedEffect(name, amountText) {
        storage.saveCalculator(
            CalculatorState(
                name,
                amountText,
                persons.map { com.premiabrygady.app.PersonInput(it.name, it.hoursText) }
            )
        )
    }
    LaunchedEffect(persons) {
        snapshotFlow { persons.map { it.name to it.hoursText } }.collect { pairs ->
            storage.saveCalculator(
                CalculatorState(
                    name,
                    amountText,
                    pairs.map { com.premiabrygady.app.PersonInput(it.first, it.second) }
                )
            )
        }
    }
    LaunchedEffect(history) {
        storage.saveHistory(history)
    }

    Scaffold(
        containerColor = BgColor,
        topBar = {
            TopAppBar(
                title = { Text("Premia Brygady", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BgColor,
                    titleContentColor = TextColor
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = CardColor) {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.Calculate, contentDescription = "Kalkulator") },
                    label = { Text("Kalkulator") }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.History, contentDescription = "Historia") },
                    label = { Text("Historia") }
                )
            }
        },
        modifier = Modifier.imePadding()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (tab == 0) {
                CalculatorScreen(
                    name = name,
                    onName = { name = it },
                    amountText = amountText,
                    onAmount = { amountText = it },
                    persons = persons,
                    onSaveHistory = {
                        val amountCents = PremiumCalculator.parseAmountToCents(amountText)
                        val hours = persons.map { PremiumCalculator.parseHours(it.hoursText) }
                        val result = PremiumCalculator.calculate(amountCents, hours)
                        val entryPersons = persons.mapIndexed { index, p ->
                            HistoryPerson(
                                p.name.trim().ifBlank { "Osoba " + (index + 1) },
                                hours[index],
                                result.amountsCents[index]
                            )
                        }
                        val entry = HistoryEntry(
                            id = Instant.now().toString(),
                            name = name.trim().ifBlank { "Premia" },
                            dateIso = Instant.now().toString(),
                            amountCents = amountCents,
                            persons = entryPersons
                        )
                        history = listOf(entry) + history
                    }
                )
            } else {
                HistoryScreen(
                    history = history,
                    onDelete = { id -> history = history.filter { it.id != id } },
                    onDeleteAll = { history = emptyList() },
                    onLoad = { entry ->
                        name = entry.name
                        amountText = (entry.amountCents / 100.0).toString()
                        persons.clear()
                        entry.persons.forEach { persons.add(PersonUi(it.name, it.hours.toString())) }
                        tab = 0
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalculatorScreen(
    name: String,
    onName: (String) -> Unit,
    amountText: String,
    onAmount: (String) -> Unit,
    persons: List<PersonUi>,
    onSaveHistory: () -> Unit
) {
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    val amountCents = PremiumCalculator.parseAmountToCents(amountText)
    val hours = persons.map { PremiumCalculator.parseHours(it.hoursText) }
    val result = PremiumCalculator.calculate(amountCents, hours)
    val totalHours = result.totalHours
    val distributedCents = result.amountsCents.sum()
    val rateText = if (totalHours > 0L) {
        PremiumCalculator.formatPln(
            Math.round(amountCents * 100.0 / totalHours.toDouble())
        )
    } else {
        PremiumCalculator.formatPln(0L)
    }

    fun buildPdfRows(): List<PdfRow> {
        return persons.mapIndexed { index, p ->
            val share = if (totalHours > 0L) {
                (hours[index] * 100.0 / totalHours.toDouble())
            } else {
                0.0
            }
            PdfRow(
                p.name.trim().ifBlank { "Osoba " + (index + 1) },
                hours[index],
                String.format("%.2f %%", share).replace(".", ","),
                result.amountsCents[index]
            )
        }
    }

    fun sharePdf() {
        val file = PdfExporter.createPdf(
            context,
            name.trim().ifBlank { "Premia" },
            DateFormat.format("yyyy-MM-dd HH:mm", Date()).toString(),
            amountCents,
            totalHours,
            buildPdfRows()
        )
        PdfExporter.sharePdf(context, file)
    }

    fun validateOrRun(action: () -> Unit) {
        if (amountCents <= 0L || totalHours <= 0L) {
            message = "Wpisz kwote i godziny"
            isError = true
        } else {
            action()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = onName,
            label = { Text("Nazwa") },
            placeholder = { Text("np. budowa lub miesiac") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = amountText,
            onValueChange = onAmount,
            label = { Text("Kwota premii do podzialu (zl)") },
            placeholder = { Text("np. 3500") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        Text("Osoby", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextColor)

        persons.forEachIndexed { index, person ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CardColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = person.name,
                                onValueChange = {
                                    val list = persons as? MutableList<PersonUi>
                                    if (list != null) list[index] = PersonUi(it, person.hoursText)
                                },
                                label = { Text("Imie") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = person.hoursText,
                                onValueChange = {
                                    val list = persons as? MutableList<PersonUi>
                                    if (list != null) list[index] = PersonUi(person.name, it)
                                },
                                label = { Text("Godziny") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        IconButton(onClick = {
                            (persons as? MutableList<PersonUi>)?.removeAt(index)
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Usun osobe", tint = RedColor)
                        }
                    }
                    val share = if (totalHours > 0L) {
                        hours[index] * 100.0 / totalHours.toDouble()
                    } else {
                        0.0
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            String.format("%.2f %%", share).replace(".", ",") + "  ",
                            color = TextColor,
                            fontSize = 14.sp
                        )
                        Text(
                            PremiumCalculator.formatPln(result.amountsCents[index]),
                            color = GreenColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                (persons as? MutableList<PersonUi>)?.add(PersonUi("", ""))
            }) {
                Text("+ Dodaj osobe")
            }
            TextButton(onClick = {
                val list = persons as? MutableList<PersonUi>
                list?.clear()
                list?.add(PersonUi("", ""))
                list?.add(PersonUi("", ""))
            }) {
                Text("Wyczysc", color = RedColor)
            }
        }

        if (amountCents > 0L && totalHours == 0L) {
            Text(
                "Wpisz godziny przynajmniej jednej osoby",
                color = RedColor,
                fontWeight = FontWeight.Bold
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = CardColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Suma godzin: " + totalHours, color = TextColor)
                Text("Stawka za godzine: " + rateText, color = TextColor)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Rozdzielono " + PremiumCalculator.formatPln(distributedCents) +
                        " z " + PremiumCalculator.formatPln(amountCents),
                    color = GreenColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (message != null) {
            Text(
                message ?: "",
                color = if (isError) RedColor else GreenColor,
                fontWeight = FontWeight.Bold
            )
        }

        Button(
            onClick = {
                validateOrRun {
                    onSaveHistory()
                    message = "Zapisano do historii"
                    isError = false
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Zapisz do historii", fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = { validateOrRun { sharePdf() } },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("PDF")
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun HistoryScreen(
    history: List<HistoryEntry>,
    onDelete: (String) -> Unit,
    onDeleteAll: () -> Unit,
    onLoad: (HistoryEntry) -> Unit
) {
    val context = LocalContext.current
    var expandedId by remember { mutableStateOf<String?>(null) }
    var confirmDeleteAll by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (history.isEmpty()) {
            Text("Brak zapisanych premii.", color = TextColor, fontSize = 16.sp)
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(history) { entry ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedId = if (expandedId == entry.id) null else entry.id
                            }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(entry.name, fontWeight = FontWeight.Bold, color = TextColor)
                            Text(
                                entry.dateIso + "  |  Osoby: " + entry.persons.size,
                                color = TextColor,
                                fontSize = 13.sp
                            )
                            Text(
                                PremiumCalculator.formatPln(entry.amountCents),
                                color = GreenColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            if (expandedId == entry.id) {
                                Spacer(modifier = Modifier.height(10.dp))
                                val totalHours = entry.persons.sumOf { it.hours }
                                entry.persons.forEach { p ->
                                    val share = if (totalHours > 0L) {
                                        p.hours * 100.0 / totalHours.toDouble()
                                    } else {
                                        0.0
                                    }
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            p.name + " (" + p.hours + " h, " +
                                                String.format("%.2f %%", share).replace(".", ",") + ")",
                                            color = TextColor,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            PremiumCalculator.formatPln(p.amountCents),
                                            color = GreenColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = {
                                        val rows = entry.persons.map { p ->
                                            val share = if (totalHours > 0L) {
                                                p.hours * 100.0 / totalHours.toDouble()
                                            } else {
                                                0.0
                                            }
                                            PdfRow(
                                                p.name,
                                                p.hours,
                                                String.format("%.2f %%", share).replace(".", ","),
                                                p.amountCents
                                            )
                                        }
                                        val file = PdfExporter.createPdf(
                                            context,
                                            entry.name,
                                            entry.dateIso,
                                            entry.amountCents,
                                            totalHours,
                                            rows
                                        )
                                        PdfExporter.sharePdf(context, file)
                                    }) {
                                        Text("PDF")
                                    }
                                    OutlinedButton(onClick = { onLoad(entry) }) {
                                        Text("Wczytaj")
                                    }
                                    TextButton(onClick = { onDelete(entry.id) }) {
                                        Text("Usun", color = RedColor)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = { confirmDeleteAll = true }) {
                Text("Usun cala historie", color = RedColor)
            }
        }
    }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text("Usunac cala historie?") },
            text = { Text("Tej operacji nie mozna cofnac.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteAll()
                    confirmDeleteAll = false
                }) {
                    Text("Usun", color = RedColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAll = false }) {
                    Text("Anuluj")
                }
            }
        )
    }
}
