package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.FarmViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolaillerScreen(
    viewModel: FarmViewModel,
    onLogout: () -> Unit
) {
    val bandes by viewModel.allBandes.collectAsState()
    val mortalities by viewModel.allMortalities.collectAsState()
    val tasks by viewModel.allTasks.collectAsState()
    val alarms by viewModel.allAlarms.collectAsState()
    val formulas by viewModel.allFeedFormulas.collectAsState()

    var showAddBandeDialog by remember { mutableStateOf(false) }
    var bandName by remember { mutableStateOf("") }
    var bandCount by remember { mutableStateOf("") }

    var showMortalityDialog by remember { mutableStateOf(false) }
    var selectedBandeId by remember { mutableStateOf(0) }
    var deathCount by remember { mutableStateOf("") }

    var selectedFeedTypeForView by remember { mutableStateOf("Démarrage") }

    var activeTab by remember { mutableStateOf(0) } // 0: Suivi de Vie, 1: Prophylaxie & Tâches, 2: Alimentation & Alarme

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Espace Volailler", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onLogout, modifier = Modifier.testTag("volailler_logout")) {
                        Icon(Icons.Default.Logout, contentDescription = "Déconnexion", tint = FarmAlertRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Horizontal Navigation Tabs for simple workflow
            TabRow(selectedTabIndex = activeTab) {
                Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = { Text("Lots & Décès") })
                Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = { Text("Prophylaxie") })
                Tab(selected = activeTab == 2, onClick = { activeTab = 2 }, text = { Text("Aliment & Alarme") })
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                when (activeTab) {
                    0 -> {
                        // TAB 0: LOTS AND MORTALITIES
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { showAddBandeDialog = true },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Nouveau Lot", fontSize = 13.sp)
                                }

                                Button(
                                    onClick = {
                                        if (bandes.isNotEmpty()) {
                                            selectedBandeId = bandes.first().id
                                            deathCount = ""
                                            showMortalityDialog = true
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = FarmAlertRed),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Dangerous, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Déclarer Décès", fontSize = 13.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Suivi des Bandes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(bandes) { bande ->
                                    val age = calculateAgeInDays(bande.arrivalDate)
                                    val bMortalities = mortalities.filter { it.bandeId == bande.id }.sumOf { it.count }
                                    val liveCount = bande.initialCount - bMortalities

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(bande.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                Text("J$age", fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Initiaux : ${bande.initialCount} poussins", fontSize = 12.sp)
                                                Text("Morts : $bMortalities", fontSize = 12.sp, color = FarmAlertRed)
                                            }
                                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Sujets Vivants :", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                                Text("$liveCount têtes", fontWeight = FontWeight.Bold, color = FarmSuccessGreen, fontSize = 16.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // TAB 1: PROPHYLAXIS AND TASKS
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text("Vaccins & Vitamines du Jour", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(bandes.filter { it.status == "ACTIVE" }) { lot ->
                                    val age = calculateAgeInDays(lot.arrivalDate)
                                    val instruction = getProphylaxisInstruction(age)
                                    val lotTasks = tasks.filter { it.bandeId == lot.id }

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, FarmGreenPrimary.copy(alpha = 0.2f))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(lot.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = FarmGreenPrimary)
                                                Text("Âge : J$age", fontWeight = FontWeight.Bold, color = FarmGoldSecondary)
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Visual medicine box
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        if (instruction.contains("Vaccin")) FarmAlertRed.copy(alpha = 0.08f)
                                                        else FarmSuccessGreen.copy(alpha = 0.08f),
                                                    RoundedCornerShape(10.dp)
                                                    )
                                                    .padding(10.dp)
                                            ) {
                                                Text(instruction, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            Text("Tâches à valider :", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            if (lotTasks.isEmpty()) {
                                                Text("Aucune tâche de prophylaxie requise aujourd'hui.", fontSize = 12.sp, color = Color.Gray)
                                            } else {
                                                lotTasks.forEach { task ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 4.dp)
                                                            .clickable { viewModel.toggleTaskCompleted(task) },
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                            contentDescription = null,
                                                            tint = if (task.isCompleted) FarmSuccessGreen else FarmPendingOrange,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = task.title,
                                                            fontSize = 13.sp,
                                                            textDecoration = if (task.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                                                            color = if (task.isCompleted) FarmSuccessGreen else MaterialTheme.colorScheme.onSurface
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

                    2 -> {
                        // TAB 2: FEED FORMULA AND ALARMS
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Alarm flashing check
                            val calendar = Calendar.getInstance()
                            val curHour = calendar.get(Calendar.HOUR_OF_DAY)
                            val isFeedAlert = alarms.any { it.isActive && it.hour == curHour }

                            if (isFeedAlert) {
                                var toggle by remember { mutableStateOf(true) }
                                LaunchedEffect(Unit) {
                                    while (true) {
                                        kotlinx.coroutines.delay(500)
                                        toggle = !toggle
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (toggle) FarmAlertRed.copy(alpha = 0.2f) else Color.Transparent,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .border(1.dp, FarmAlertRed, RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("⚠️ ALARME D'ALIMENTATION EN COURS !", color = FarmAlertRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // Alarm list
                            Text("Planning des distributions", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                alarms.forEach { alarm ->
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (alarm.isActive) FarmSuccessGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (alarm.isActive) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                                contentDescription = null,
                                                tint = if (alarm.isActive) FarmSuccessGreen else Color.Gray,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = String.format("%02d:%02d", alarm.hour, alarm.minute),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Formula Check
                            Text("Formules d'aliments de la ferme", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Démarrage", "Croissance", "Finition").forEach { type ->
                                    FilterChip(
                                        selected = selectedFeedTypeForView == type,
                                        onClick = { selectedFeedTypeForView = type },
                                        label = { Text(type, fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            val selectedTypeIngredients = formulas.filter { it.feedType == selectedFeedTypeForView }
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(selectedTypeIngredients) { ing ->
                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(ing.ingredientName, fontWeight = FontWeight.Bold)
                                            Text("${ing.percentage}%", color = FarmGreenPrimary, fontWeight = FontWeight.Black)
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

    // Dialog Create Bande (Rotation 10 jours)
    if (showAddBandeDialog) {
        Dialog(onDismissRequest = { showAddBandeDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Ajouter une Bande", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = bandName,
                        onValueChange = { bandName = it },
                        label = { Text("Nom du lot (ex: Bande D)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = bandCount,
                        onValueChange = { bandCount = it },
                        label = { Text("Nombre de poussins") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Une rotation toutes les 10 jours garantit un flux constant de sujets prêts pour l'abattage.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(onClick = { showAddBandeDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("Annuler")
                        }
                        Button(
                            onClick = {
                                val count = bandCount.toIntOrNull()
                                if (bandName.isNotBlank() && count != null) {
                                    viewModel.addBande(bandName, count, System.currentTimeMillis())
                                    bandName = ""
                                    bandCount = ""
                                    showAddBandeDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                        ) {
                            Text("Valider")
                        }
                    }
                }
            }
        }
    }

    // Dialog Record Mortality
    if (showMortalityDialog) {
        Dialog(onDismissRequest = { showMortalityDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Saisir Décès", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = FarmAlertRed)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Lot concerné :", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.align(Alignment.Start))
                    var expanded by remember { mutableStateOf(false) }
                    val currentSelectedBande = bandes.find { it.id == selectedBandeId } ?: bandes.firstOrNull()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .clickable { expanded = true }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(currentSelectedBande?.name ?: "Choisir le lot", fontWeight = FontWeight.Medium)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            bandes.forEach { b ->
                                DropdownMenuItem(
                                    text = { Text(b.name) },
                                    onClick = {
                                        selectedBandeId = b.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = deathCount,
                        onValueChange = { deathCount = it },
                        label = { Text("Nombre de poulets morts") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "La date sera automatiquement enregistrée avec la date d'aujourd'hui.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(onClick = { showMortalityDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("Annuler")
                        }
                        Button(
                            onClick = {
                                val count = deathCount.toIntOrNull()
                                if (count != null && selectedBandeId != 0) {
                                    viewModel.addMortality(selectedBandeId, count, System.currentTimeMillis())
                                    // Record veterinary notification
                                    viewModel.addTransaction(
                                        type = "OUT",
                                        category = "Vétérinaire",
                                        amount = 0.0,
                                        description = "Saisie Volailler : perte de $count sujets (${currentSelectedBande?.name})",
                                        bandeId = selectedBandeId,
                                        isSale = false
                                    )
                                    deathCount = ""
                                    showMortalityDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = FarmAlertRed)
                        ) {
                            Text("Enregistrer")
                        }
                    }
                }
            }
        }
    }
}
