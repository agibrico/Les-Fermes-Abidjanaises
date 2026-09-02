package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    viewModel: FarmViewModel,
    onLogout: () -> Unit
) {
    var activePanel by remember { mutableStateOf<String?>(null) } // "USER", "FERME", "FINANCES", "ALIMENT", "VENDEURS", "COMMANDE", "RESTAURATION"

    val users by viewModel.allUsers.collectAsState()
    val bandes by viewModel.allBandes.collectAsState()
    val mortalities by viewModel.allMortalities.collectAsState()
    val formulas by viewModel.allFeedFormulas.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val orders by viewModel.allOrders.collectAsState()
    val tasks by viewModel.allTasks.collectAsState()
    val alarms by viewModel.allAlarms.collectAsState()
    val inventoryItems by viewModel.allInventoryItems.collectAsState()
    val interventions by viewModel.allInterventions.collectAsState()

    // Main Scaffold
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = activePanel ?: "Tableau de Bord Admin",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    if (activePanel != null) {
                        IconButton(onClick = { activePanel = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (activePanel == null) {
                // Main Dashboard & 7 Buttons Grid
                AdminDashboardMain(
                    users = users,
                    bandes = bandes,
                    mortalities = mortalities,
                    transactions = transactions,
                    orders = orders,
                    onPanelSelect = { activePanel = it }
                )
            } else {
                // Display the selected sub-panel
                when (activePanel) {
                    "USER" -> UserManagementPanel(viewModel, users)
                    "FERME" -> FarmManagementPanel(viewModel, bandes, mortalities, tasks, alarms, inventoryItems, interventions)
                    "FINANCES" -> FinancePanel(viewModel, transactions, users, bandes)
                    "ALIMENT" -> AlimentFormulaPanel(viewModel, formulas, bandes)
                    "VENDEURS" -> SellerManagementPanel(viewModel, users, orders, transactions)
                    "COMMANDE" -> OrderManagementPanel(viewModel, orders)
                    "RESTAURATION" -> RestorationPanel(viewModel)
                }
            }
        }
    }
}

// ==========================================================
// 0. MAIN DASHBOARD CONTENT
// ==========================================================
@Composable
fun AdminDashboardMain(
    users: List<User>,
    bandes: List<Bande>,
    mortalities: List<Mortality>,
    transactions: List<FarmTransaction>,
    orders: List<Order>,
    onPanelSelect: (String) -> Unit
) {
    val activeBandesCount = bandes.count { it.status == "ACTIVE" }
    val totalInitialSubjects = bandes.filter { it.status == "ACTIVE" }.sumOf { it.initialCount }
    val totalDeaths = mortalities.sumOf { it.count }
    val currentSubjectsCount = totalInitialSubjects - totalDeaths

    val totalIn = transactions.filter { it.type == "IN" }.sumOf { it.amount }
    val totalOut = transactions.filter { it.type == "OUT" }.sumOf { it.amount }
    val cashFlow = totalIn - totalOut

    val pendingOrdersCount = orders.count { it.status == "PENDING" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Équilibre d'Exploitation",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${formatFCFA(cashFlow)}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (cashFlow >= 0) FarmSuccessGreen else FarmAlertRed
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Sujets Actifs", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text("$currentSubjectsCount têtes", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Bandes Actives", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text("$activeBandesCount lots", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Cmds en attente", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text("$pendingOrdersCount commandes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = FarmPendingOrange)
                        }
                    }
                }
            }
        }

        // Section Title
        item {
            Text(
                text = "Actions de Gestion",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // Grid of 7 buttons as requested
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val actionButtons = listOf(
                    ActionBtnData("USER", "1. AJOUTER COMPTE", "Partenaires, Volaillers, Vendeurs", Icons.Default.PersonAdd, FarmGreenPrimary),
                    ActionBtnData("FERME", "2. GESTION DE LA FERME", "Bandes, vaccins, vitamines, mortalities, tâches, alarmes", Icons.Default.Agriculture, FarmSuccessGreen),
                    ActionBtnData("FINANCES", "3. BOUTON FINANCES", "Cash flow, revenus, prévisions, bénéfices", Icons.Default.AttachMoney, FarmGreenDark),
                    ActionBtnData("ALIMENT", "4. BOUTON ALIMENT", "Ages des lots, alimentation, ajout/retrait d'intrant", Icons.Default.Egg, FarmGoldSecondary),
                    ActionBtnData("VENDEURS", "5. PARTENAIRES & VENDEURS", "Suivi des transactions, parts, performances", Icons.Default.Storefront, FarmPendingOrange),
                    ActionBtnData("COMMANDE", "6. GESTION COMMANDE", "Traitement, modification, WhatsApp invoice", Icons.Default.ShoppingCart, FarmGreenPrimary),
                    ActionBtnData("RESTAURATION", "7. RESTAURATION", "Restaurer l'application à une date antérieure", Icons.Default.Restore, FarmAlertRed)
                )

                actionButtons.forEach { data ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPanelSelect(data.id) }
                            .testTag("admin_action_${data.id.lowercase()}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(data.color.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(data.icon, contentDescription = null, tint = data.color)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = data.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = data.subtitle,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
    }
}

data class ActionBtnData(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

// ==========================================================
// 1. AJOUTER PARTENAIRE/VOLAILLER/VENDEUR PANEL
// ==========================================================
@Composable
fun UserManagementPanel(viewModel: FarmViewModel, users: List<User>) {
    var showAddDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var roleInput by remember { mutableStateOf("PARTENAIRE") } // "PARTENAIRE", "VOLAILLER", "VENDEUR"
    
    // Tab State: 0 = Personnel / Partenaires, 1 = Clients
    var selectedTab by remember { mutableStateOf(0) }
    val context = LocalContext.current

    // Filtered lists
    val personnelUsers = users.filter { it.role != "CLIENT" }
    val clientUsers = users.filter { it.role == "CLIENT" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Title block
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (selectedTab == 0) "Comptes du Personnel" else "Base de données Clients",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (selectedTab == 0) {
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ajouter")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // TAB SWITCHER (Personnel vs Clients)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Personnel & Partenaires", "Annuaire des Clients").forEachIndexed { index, label ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) FarmGreenPrimary else Color.Transparent)
                        .clickable { selectedTab = index }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isSelected) Color.White else Color.DarkGray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTab == 0) {
            // ==========================================
            // PERSONNEL & PARTNERS LIST TAB
            // ==========================================
            if (personnelUsers.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Aucun compte personnel enregistré.", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(personnelUsers) { user ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = when (user.role) {
                                            "ADMINISTRATEUR" -> Icons.Default.AdminPanelSettings
                                            "PARTENAIRE" -> Icons.Default.Group
                                            "VOLAILLER" -> Icons.Default.Agriculture
                                            else -> Icons.Default.Storefront
                                        },
                                        contentDescription = null,
                                        tint = when (user.role) {
                                            "ADMINISTRATEUR" -> FarmGreenPrimary
                                            "PARTENAIRE" -> FarmGoldSecondary
                                            "VOLAILLER" -> FarmSuccessGreen
                                            else -> FarmPendingOrange
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(user.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("Rôle : ${user.role} | MDP : ${user.password}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                }
                                if (user.role != "ADMINISTRATEUR") {
                                    IconButton(onClick = { viewModel.deleteUser(user) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = FarmAlertRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ==========================================
            // CLIENTS DATABASE DIRECTORY TAB
            // ==========================================
            if (clientUsers.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Aucun client enregistré automatiquement pour le moment.", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(clientUsers) { user ->
                        val parts = user.password.split("|")
                        val clientType = parts.getOrNull(0) ?: "Particulier"
                        val clientPhone = parts.getOrNull(1) ?: "Inconnu"

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Visual Icon according to client type
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (clientType == "Particulier") FarmGreenPrimary.copy(alpha = 0.1f)
                                                else FarmGoldSecondary.copy(alpha = 0.15f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (clientType == "Particulier") Icons.Default.Person else Icons.Default.Storefront,
                                            contentDescription = null,
                                            tint = if (clientType == "Particulier") FarmGreenPrimary else Color(0xFFD4AF37)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column {
                                        Text(
                                            text = user.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (clientType == "Structure") "Restaurant / Vendeur" else "Particulier",
                                            fontSize = 11.sp,
                                            color = if (clientType == "Structure") FarmPendingOrange else FarmGreenDark,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(1.dp))
                                        Text(
                                            text = "📞 $clientPhone",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                // Interactive Call & WhatsApp & Delete actions
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Quick call button
                                    if (clientPhone != "Inconnu" && clientPhone.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                try {
                                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                                        data = Uri.parse("tel:${clientPhone.replace(" ", "")}")
                                                    }
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Impossible de passer l'appel", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Phone,
                                                contentDescription = "Appeler",
                                                tint = FarmGreenPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        // WhatsApp shortcut button
                                        IconButton(
                                            onClick = {
                                                try {
                                                    val cleanPhone = clientPhone.replace(" ", "").replace("+", "")
                                                    val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=Bonjour%20${user.name}"
                                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                                        data = Uri.parse(url)
                                                    }
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "WhatsApp non disponible", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "WhatsApp",
                                                tint = FarmSuccessGreen,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    // Delete client button
                                    IconButton(
                                        onClick = { viewModel.deleteUser(user) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Supprimer",
                                            tint = FarmAlertRed,
                                            modifier = Modifier.size(20.dp)
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

    if (showAddDialog) {
        Dialog(onDismissRequest = { showAddDialog = false }) {
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
                    Text("Nouvel Utilisateur", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Nom complet") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Sélectionner un Rôle :", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.align(Alignment.Start))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val roles = listOf("PARTENAIRE", "VOLAILLER", "VENDEUR")
                        roles.forEach { r ->
                            FilterChip(
                                selected = roleInput == r,
                                onClick = { roleInput = r },
                                label = { Text(r, fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Le mot de passe par défaut sera '1234'",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = FarmPendingOrange
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Annuler")
                        }
                        Button(
                            onClick = {
                                if (nameInput.isNotBlank()) {
                                    viewModel.addUser(nameInput, roleInput)
                                    nameInput = ""
                                    showAddDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                        ) {
                            Text("Créer")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================================
// 2. GESTION DE LA FERME PANEL
// ==========================================================
@Composable
fun FarmManagementPanel(
    viewModel: FarmViewModel,
    bandes: List<Bande>,
    mortalities: List<Mortality>,
    tasks: List<VolaillerTask>,
    alarms: List<FeedingAlarm>,
    inventoryItems: List<InventoryItem>,
    interventions: List<Intervention>
) {
    var activeTab by remember { mutableStateOf(0) } // 0: Bandes, 1: Calendrier, 2: Stocks & Alertes, 3: Alarmes

    // Lot Dialogs and states
    var showAddBandeDialog by remember { mutableStateOf(false) }
    var bandName by remember { mutableStateOf("") }
    var bandCount by remember { mutableStateOf("") }
    var bandSouche by remember { mutableStateOf("Cobb 500") }
    var bandDaysAgo by remember { mutableStateOf(0f) } // slider to select how many days ago it was set up

    var showMortalityDialog by remember { mutableStateOf(false) }
    var selectedBandeIdForMortality by remember { mutableStateOf(0) }
    var deathCount by remember { mutableStateOf("") }

    // Calendar states
    var selectedCalendarDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showAddInterventionDialog by remember { mutableStateOf(false) }
    var interventionBandeId by remember { mutableStateOf(0) }
    var interventionType by remember { mutableStateOf("VACCINATION") } // VACCINATION, TRAITEMENT, VITAMINES
    var interventionTitle by remember { mutableStateOf("") }
    var interventionDesc by remember { mutableStateOf("") }
    var interventionDaysOffset by remember { mutableStateOf(0f) } // days from today to schedule

    // Inventory states
    var showAddStockDialog by remember { mutableStateOf(false) }
    var stockName by remember { mutableStateOf("") }
    var stockCategory by remember { mutableStateOf("ALIMENT") } // ALIMENT, VETERINAIRE
    var stockQty by remember { mutableStateOf("") }
    var stockUnit by remember { mutableStateOf("kg") }
    var stockThreshold by remember { mutableStateOf("") }
    var stockPrice by remember { mutableStateOf("") }

    var showAdjustStockDialog by remember { mutableStateOf(false) }
    var selectedStockItem by remember { mutableStateOf<InventoryItem?>(null) }
    var adjustQty by remember { mutableStateOf("") }
    var isAddingStock by remember { mutableStateOf(true) } // true to add, false to consume

    // Alarm Dialog state
    var showAlarmDialog by remember { mutableStateOf(false) }
    var alarmHour by remember { mutableStateOf("07") }
    var alarmMinute by remember { mutableStateOf("00") }

    // State for expanded card details in lot view to see daily mortality logs
    var expandedBandeId by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ScrollableTabRow(
            selectedTabIndex = activeTab,
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = { Text("Bandes / Décès", fontSize = 13.sp) })
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = { Text("🗓️ Calendrier", fontSize = 13.sp) })
            Tab(selected = activeTab == 2, onClick = { activeTab = 2 }, text = { Text("📦 Stocks & Alertes", fontSize = 13.sp) })
            Tab(selected = activeTab == 3, onClick = { activeTab = 3 }, text = { Text("⏰ Distribution", fontSize = 13.sp) })
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (activeTab) {
            0 -> {
                // ==========================================
                // TAB 0: BANDES & DAILY MORTALITY LOG
                // ==========================================
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
                            Text("Nouvelle Bande", fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                if (bandes.isNotEmpty()) {
                                    selectedBandeIdForMortality = bandes.first().id
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
                            Text("Saisir Décès", fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Bandes Actuelles", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Cliquez sur un lot pour voir l'historique quotidien de la mortalité.", fontSize = 11.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(8.dp))

                    if (bandes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Aucune bande enregistrée. Créez-en une nouvelle !", color = Color.Gray, fontSize = 14.sp)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(bandes) { bande ->
                                val age = calculateAgeInDays(bande.arrivalDate)
                                val bMortalities = mortalities.filter { it.bandeId == bande.id }
                                val totalDeaths = bMortalities.sumOf { it.count }
                                val currentCount = bande.initialCount - totalDeaths
                                val isExpanded = expandedBandeId == bande.id

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            expandedBandeId = if (isExpanded) null else bande.id
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isExpanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                                    ),
                                    border = BorderStroke(
                                        width = if (isExpanded) 1.5.dp else 0.5.dp,
                                        color = if (isExpanded) FarmGreenPrimary else Color.LightGray.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(bande.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    SuggestionChip(
                                                        onClick = { },
                                                        label = { Text("Souche: ${bande.souche}", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                                        colors = SuggestionChipDefaults.suggestionChipColors(labelColor = FarmGreenDark)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Badge(
                                                        containerColor = if (bande.status == "ACTIVE") FarmSuccessGreen else Color.Gray,
                                                        contentColor = Color.White
                                                    ) {
                                                        Text(bande.status, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), fontSize = 9.sp)
                                                    }
                                                }
                                            }
                                            Text("J$age", fontSize = 18.sp, fontWeight = FontWeight.Black, color = FarmGreenPrimary)
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Date mise en place : ${formatDate(bande.arrivalDate)}", fontSize = 12.sp, color = Color.Gray)
                                            Text("Effectif initial : ${bande.initialCount} têtes", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Mortalité cumulée :", fontSize = 12.sp, color = Color.Gray)
                                            Text("$totalDeaths morts (${String.format("%.1f", (totalDeaths.toDouble() / bande.initialCount.toDouble() * 100))}% de perte)", fontSize = 12.sp, color = FarmAlertRed, fontWeight = FontWeight.Bold)
                                        }

                                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Sujets Vivants :", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("$currentCount têtes", fontSize = 16.sp, fontWeight = FontWeight.Black, color = FarmSuccessGreen)
                                        }

                                        // EXPANDED REGION: Daily mortality logs and quick action
                                        AnimatedVisibility(visible = isExpanded) {
                                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                                                Text("Suivi Quotidien de la Mortalité", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                                                Spacer(modifier = Modifier.height(6.dp))

                                                if (bMortalities.isEmpty()) {
                                                    Text("Aucun décès enregistré pour cette bande. Excellente gestion !", fontSize = 12.sp, color = FarmSuccessGreen, modifier = Modifier.padding(vertical = 4.dp))
                                                } else {
                                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        bMortalities.forEach { mort ->
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .background(Color.Gray.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text(formatDate(mort.date), fontSize = 12.sp)
                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                    Text("${mort.count} mort(s)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FarmAlertRed)
                                                                    Spacer(modifier = Modifier.width(8.dp))
                                                                    Icon(
                                                                        imageVector = Icons.Default.Delete,
                                                                        contentDescription = "Supprimer",
                                                                        tint = Color.Gray,
                                                                        modifier = Modifier
                                                                            .size(16.dp)
                                                                            .clickable { viewModel.deleteMortality(mort) }
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(10.dp))
                                                Button(
                                                    onClick = {
                                                        selectedBandeIdForMortality = bande.id
                                                        deathCount = ""
                                                        showMortalityDialog = true
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = FarmAlertRed.copy(alpha = 0.1f), contentColor = FarmAlertRed),
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Enregistrer un décès pour ce lot", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

            1 -> {
                // ==========================================
                // TAB 1: CALENDRIER & INTERVENTIONS INTERACTIF
                // ==========================================
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Vaccinations & Traitements", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Button(
                            onClick = {
                                if (bandes.isNotEmpty()) {
                                    interventionBandeId = bandes.first().id
                                    interventionTitle = ""
                                    interventionDesc = ""
                                    interventionDaysOffset = 0f
                                    showAddInterventionDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Planifier", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // INTERACTIVE CALENDAR DATE PICKER (Past 5 days to Next 9 days)
                    Text("Sélectionnez un jour pour voir ou planifier les interventions :", fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))

                    val today = System.currentTimeMillis()
                    val oneDayMs = 24L * 60 * 60 * 1000
                    val calendarOffsets = (-5..9).toList()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        calendarOffsets.forEach { offset ->
                            val targetTime = today + (offset * oneDayMs)
                            val targetDate = Date(targetTime)
                            val isSelected = formatDate(targetTime) == formatDate(selectedCalendarDate)

                            val dayFormat = SimpleDateFormat("dd", Locale.FRANCE)
                            val monthFormat = SimpleDateFormat("MMM", Locale.FRANCE)
                            val weekdayFormat = SimpleDateFormat("EE", Locale.FRANCE)

                            Card(
                                modifier = Modifier
                                    .width(55.dp)
                                    .clickable { selectedCalendarDate = targetTime },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) FarmGreenPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                ),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                    color = if (isSelected) FarmGreenPrimary else Color.LightGray.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(weekdayFormat.format(targetDate).uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(dayFormat.format(targetDate), fontSize = 16.sp, fontWeight = FontWeight.Black)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(monthFormat.format(targetDate).uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Interventions du : ${formatDate(selectedCalendarDate)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Filter interventions for selected date
                    val selectedDayStr = formatDate(selectedCalendarDate)
                    val daysInterventions = interventions.filter { formatDate(it.date) == selectedDayStr }

                    if (daysInterventions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Gray.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.DoneAll, contentDescription = null, tint = FarmSuccessGreen, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Aucune intervention planifiée pour ce jour !", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(daysInterventions) { intervention ->
                                val bande = bandes.find { it.id == intervention.bandeId }
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (intervention.isCompleted) FarmSuccessGreen.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
                                    ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (intervention.isCompleted) FarmSuccessGreen.copy(alpha = 0.3f) else Color.LightGray.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Completion checkbox
                                        IconButton(
                                            onClick = {
                                                viewModel.updateIntervention(intervention.copy(isCompleted = !intervention.isCompleted))
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (intervention.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                contentDescription = "Statut",
                                                tint = if (intervention.isCompleted) FarmSuccessGreen else FarmPendingOrange,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = intervention.title,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    textDecoration = if (intervention.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                                                    color = if (intervention.isCompleted) FarmSuccessGreen else MaterialTheme.colorScheme.onSurface
                                                )
                                                Badge(
                                                    containerColor = when (intervention.type) {
                                                        "VACCINATION" -> FarmAlertRed
                                                        "TRAITEMENT" -> FarmPendingOrange
                                                        else -> FarmGreenPrimary
                                                    },
                                                    contentColor = Color.White
                                                ) {
                                                    Text(intervention.type, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(intervention.description, fontSize = 12.sp, color = Color.Gray)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Concerne : ${bande?.name ?: "Lot Inconnu"}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FarmGreenDark)
                                        }

                                        IconButton(
                                            onClick = { viewModel.deleteIntervention(intervention) }
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = Color.Gray, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // ==========================================
                // TAB 2: SUIVI DES STOCKS & ALERTES CRITIQUES
                // ==========================================
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Stocks d'Aliments & Produits Vétos", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Button(
                            onClick = {
                                stockName = ""
                                stockQty = ""
                                stockThreshold = ""
                                stockPrice = ""
                                showAddStockDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ajouter Stock", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Critical Alerts section at top
                    val criticalItems = inventoryItems.filter { it.quantity <= it.threshold }
                    if (criticalItems.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = FarmAlertRed.copy(alpha = 0.08f)),
                            border = BorderStroke(1.5.dp, FarmAlertRed)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = FarmAlertRed, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("ALERTES STOCKS CRITIQUES (${criticalItems.size})", fontWeight = FontWeight.Black, color = FarmAlertRed, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Certains intrants sont en rupture ou sous le seuil critique d'approvisionnement :", fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                criticalItems.forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("• ${item.name}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FarmAlertRed)
                                        Text("Reste : ${item.quantity} ${item.unit} (Seuil: ${item.threshold})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FarmAlertRed)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Stock Tabs (Aliment vs Vétérinaire)
                    var selectedStockCategoryTab by remember { mutableStateOf("ALIMENT") }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("ALIMENT" to "Aliments", "VETERINAIRE" to "Produits Vétérinaires").forEach { (id, label) ->
                            val isSel = selectedStockCategoryTab == id
                            Button(
                                onClick = { selectedStockCategoryTab = id },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) FarmGreenPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val filteredStocks = inventoryItems.filter { it.category == selectedStockCategoryTab }

                    if (filteredStocks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Aucun produit enregistré sous cette catégorie.", color = Color.Gray, fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredStocks) { stock ->
                                val isCritical = stock.quantity <= stock.threshold
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isCritical) FarmAlertRed.copy(alpha = 0.02f) else MaterialTheme.colorScheme.surface
                                    ),
                                    border = BorderStroke(
                                        width = if (isCritical) 1.5.dp else 0.5.dp,
                                        color = if (isCritical) FarmAlertRed else Color.LightGray.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(stock.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                Text("Prix unit : ${formatFCFA(stock.pricePerUnit)} / ${stock.unit}", fontSize = 11.sp, color = Color.Gray)
                                            }

                                            // Highlight critical stock in red background
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        if (isCritical) FarmAlertRed else FarmSuccessGreen,
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "${stock.quantity} ${stock.unit}",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Progress visual indicator relative to critical threshold
                                        val totalCapacityRatio = (stock.quantity / (stock.threshold * 2.0)).coerceIn(0.0, 1.0).toFloat()
                                        LinearProgressIndicator(
                                            progress = { totalCapacityRatio },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = if (isCritical) FarmAlertRed else FarmSuccessGreen,
                                            trackColor = Color.LightGray.copy(alpha = 0.3f)
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isCritical) "⚠️ ALERTE : Stock critique (seuil: ${stock.threshold} ${stock.unit})" else "Niveau de stock correct (seuil: ${stock.threshold} ${stock.unit})",
                                                fontSize = 11.sp,
                                                color = if (isCritical) FarmAlertRed else FarmGreenDark,
                                                fontWeight = FontWeight.Bold
                                            )

                                            Row {
                                                IconButton(
                                                    onClick = {
                                                        selectedStockItem = stock
                                                        adjustQty = ""
                                                        isAddingStock = true
                                                        showAdjustStockDialog = true
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.AddCircle, contentDescription = "Réapprovisionner", tint = FarmSuccessGreen)
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                IconButton(
                                                    onClick = {
                                                        selectedStockItem = stock
                                                        adjustQty = ""
                                                        isAddingStock = false
                                                        showAdjustStockDialog = true
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.RemoveCircle, contentDescription = "Consommer", tint = FarmPendingOrange)
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                IconButton(
                                                    onClick = { viewModel.deleteInventoryItem(stock) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = Color.Gray)
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

            3 -> {
                // ==========================================
                // TAB 3: ALARMES DE DISTRIBUTION (ALIMENT)
                // ==========================================
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Heures d'Alimentation & Alarmes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Button(
                            onClick = { showAlarmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Text("Alarme")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val calendar = Calendar.getInstance()
                    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                    val isFeedingHour = alarms.any { it.isActive && it.hour == currentHour }

                    if (isFeedingHour) {
                        var isVisible by remember { mutableStateOf(true) }
                        LaunchedEffect(Unit) {
                            while (true) {
                                kotlinx.coroutines.delay(500)
                                isVisible = !isVisible
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isVisible) FarmAlertRed.copy(alpha = 0.2f) else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .border(1.dp, FarmAlertRed, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = FarmAlertRed, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("ALERTE : HEURE D'ALIMENTATION DES SUJETS !", color = FarmAlertRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(alarms) { alarm ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = FarmGreenPrimary)
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(
                                            text = String.format("%02d:%02d", alarm.hour, alarm.minute),
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Switch(
                                            checked = alarm.isActive,
                                            onCheckedChange = { viewModel.toggleAlarm(alarm) }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(onClick = { viewModel.deleteAlarm(alarm) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = FarmAlertRed)
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

    // Dialogue Nouvelle Bande
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
                    Text("Nouvelle Bande", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = bandName,
                        onValueChange = { bandName = it },
                        label = { Text("Nom du Lot (ex: Bande D)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = bandCount,
                        onValueChange = { bandCount = it },
                        label = { Text("Nombre de sujets (ex: 500)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Strain/Souche Choice
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Souche (Race de volaille) :", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Cobb 500", "Ross 308", "Hubbard").forEach { s ->
                                FilterChip(
                                    selected = bandSouche == s,
                                    onClick = { bandSouche = s },
                                    label = { Text(s, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Date offset slider (so they can create e.g. J20 bands for testing easily)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Mise en place :", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (bandDaysAgo.toInt() == 0) "Aujourd'hui" else "Il y a ${bandDaysAgo.toInt()} jours",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = FarmGreenPrimary
                            )
                        }
                        Slider(
                            value = bandDaysAgo,
                            onValueChange = { bandDaysAgo = it },
                            valueRange = 0f..45f,
                            steps = 45,
                            colors = SliderDefaults.colors(thumbColor = FarmGreenPrimary, activeTrackColor = FarmGreenPrimary)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

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
                                    val setupTime = System.currentTimeMillis() - (bandDaysAgo.toLong() * 24 * 60 * 60 * 1000)
                                    viewModel.addBande(bandName, count, setupTime, bandSouche)
                                    bandName = ""
                                    bandCount = ""
                                    bandDaysAgo = 0f
                                    showAddBandeDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                        ) {
                            Text("Créer")
                        }
                    }
                }
            }
        }
    }

    // Dialogue Saisie Décès
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
                    Text("Enregistrer un Décès", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = FarmAlertRed)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Sélectionner la Bande :", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.align(Alignment.Start))
                    var expanded by remember { mutableStateOf(false) }
                    val currentSelectedBande = bandes.find { it.id == selectedBandeIdForMortality } ?: bandes.firstOrNull()

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
                            Text(currentSelectedBande?.name ?: "Choisir une bande", fontWeight = FontWeight.Medium)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            bandes.forEach { b ->
                                DropdownMenuItem(
                                    text = { Text(b.name) },
                                    onClick = {
                                        selectedBandeIdForMortality = b.id
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
                        label = { Text("Nombre de décès") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "La date sera enregistrée automatiquement à aujourd'hui.",
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
                                val targetBandeId = if (selectedBandeIdForMortality != 0) selectedBandeIdForMortality else (currentSelectedBande?.id ?: 0)
                                if (count != null && targetBandeId != 0) {
                                    viewModel.addMortality(targetBandeId, count, System.currentTimeMillis())
                                    // Also register a small negative veterinary loss/expense as description
                                    viewModel.addTransaction(
                                        type = "OUT",
                                        category = "Vétérinaire",
                                        amount = 0.0, // Just record loss
                                        description = "Perte de $count sujets dans ${currentSelectedBande?.name}",
                                        bandeId = targetBandeId,
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

    // Dialogue Nouvelle Alarme
    if (showAlarmDialog) {
        Dialog(onDismissRequest = { showAlarmDialog = false }) {
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
                    Text("Nouvelle Alarme", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = alarmHour,
                            onValueChange = { alarmHour = it },
                            label = { Text("Heure (00-23)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = alarmMinute,
                            onValueChange = { alarmMinute = it },
                            label = { Text("Min (00-59)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(onClick = { showAlarmDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("Annuler")
                        }
                        Button(
                            onClick = {
                                val hr = alarmHour.toIntOrNull()
                                val min = alarmMinute.toIntOrNull()
                                if (hr != null && min != null && hr in 0..23 && min in 0..59) {
                                    viewModel.addAlarm(hr, min)
                                    showAlarmDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                        ) {
                            Text("Activer")
                        }
                    }
                }
            }
        }
    }

    // Dialogue Nouvelle Intervention
    if (showAddInterventionDialog) {
        Dialog(onDismissRequest = { showAddInterventionDialog = false }) {
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
                    Text("Planifier Intervention", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Sélectionner la Bande :", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.align(Alignment.Start))
                    var bandExpanded by remember { mutableStateOf(false) }
                    val currentSelectedBande = bandes.find { it.id == interventionBandeId } ?: bandes.firstOrNull()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .clickable { bandExpanded = true }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(currentSelectedBande?.name ?: "Choisir une bande", fontWeight = FontWeight.Medium)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = bandExpanded, onDismissRequest = { bandExpanded = false }) {
                            bandes.forEach { b ->
                                DropdownMenuItem(
                                    text = { Text(b.name) },
                                    onClick = {
                                        interventionBandeId = b.id
                                        bandExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Type d'intervention :", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.align(Alignment.Start))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("VACCINATION", "TRAITEMENT", "VITAMINES").forEach { t ->
                            FilterChip(
                                selected = interventionType == t,
                                onClick = { interventionType = t },
                                label = { Text(t, fontSize = 9.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = interventionTitle,
                        onValueChange = { interventionTitle = it },
                        label = { Text("Titre (ex: Vaccin Gumboro)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = interventionDesc,
                        onValueChange = { interventionDesc = it },
                        label = { Text("Instructions/Détails") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Planifié pour :", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            val targetTime = System.currentTimeMillis() + (interventionDaysOffset.toLong() * 24 * 60 * 60 * 1000)
                            Text(
                                text = if (interventionDaysOffset.toInt() == 0) "Aujourd'hui" else "Dans ${interventionDaysOffset.toInt()} jours (${formatDate(targetTime)})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = FarmGreenPrimary
                            )
                        }
                        Slider(
                            value = interventionDaysOffset,
                            onValueChange = { interventionDaysOffset = it },
                            valueRange = -3f..10f,
                            steps = 13,
                            colors = SliderDefaults.colors(thumbColor = FarmGreenPrimary, activeTrackColor = FarmGreenPrimary)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(onClick = { showAddInterventionDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("Annuler")
                        }
                        Button(
                            onClick = {
                                val targetBandeId = if (interventionBandeId != 0) interventionBandeId else (currentSelectedBande?.id ?: 0)
                                if (interventionTitle.isNotBlank() && targetBandeId != 0) {
                                    val targetTime = System.currentTimeMillis() + (interventionDaysOffset.toLong() * 24 * 60 * 60 * 1000)
                                    viewModel.addIntervention(
                                        bandeId = targetBandeId,
                                        type = interventionType,
                                        title = interventionTitle,
                                        description = interventionDesc,
                                        date = targetTime
                                    )
                                    interventionTitle = ""
                                    interventionDesc = ""
                                    interventionDaysOffset = 0f
                                    showAddInterventionDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                        ) {
                            Text("Planifier")
                        }
                    }
                }
            }
        }
    }

    // Dialogue Nouveau Stock
    if (showAddStockDialog) {
        Dialog(onDismissRequest = { showAddStockDialog = false }) {
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
                    Text("Nouvel Intrant", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = stockName,
                        onValueChange = { stockName = it },
                        label = { Text("Nom du produit (ex: Aliment Démarrage)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Catégorie :", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.align(Alignment.Start))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("ALIMENT" to "Aliment", "VETERINAIRE" to "Vétérinaire").forEach { (catId, label) ->
                            FilterChip(
                                selected = stockCategory == catId,
                                onClick = { stockCategory = catId },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = stockQty,
                            onValueChange = { stockQty = it },
                            label = { Text("Quantité") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.2f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = stockUnit,
                            onValueChange = { stockUnit = it },
                            label = { Text("Unité") },
                            modifier = Modifier.weight(0.8f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = stockThreshold,
                        onValueChange = { stockThreshold = it },
                        label = { Text("Seuil critique (Alerte)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = stockPrice,
                        onValueChange = { stockPrice = it },
                        label = { Text("Prix unitaire (FCFA)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(onClick = { showAddStockDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("Annuler")
                        }
                        Button(
                            onClick = {
                                val qty = stockQty.toDoubleOrNull()
                                val thresh = stockThreshold.toDoubleOrNull()
                                val price = stockPrice.toDoubleOrNull()
                                if (stockName.isNotBlank() && qty != null && thresh != null && price != null) {
                                    viewModel.addInventoryItem(
                                        name = stockName,
                                        category = stockCategory,
                                        quantity = qty,
                                        unit = stockUnit,
                                        threshold = thresh,
                                        pricePerUnit = price
                                    )
                                    // Record stock purchase expense in finances automatically!
                                    viewModel.addTransaction(
                                        type = "OUT",
                                        category = if (stockCategory == "ALIMENT") "Aliments" else "Vétérinaire",
                                        amount = qty * price,
                                        description = "Achat de $qty $stockUnit de $stockName",
                                        bandeId = null,
                                        isSale = false
                                    )
                                    showAddStockDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                        ) {
                            Text("Ajouter")
                        }
                    }
                }
            }
        }
    }

    // Dialogue Ajuster Stock
    if (showAdjustStockDialog && selectedStockItem != null) {
        val item = selectedStockItem!!
        Dialog(onDismissRequest = { showAdjustStockDialog = false }) {
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
                    Text(
                        text = if (isAddingStock) "Réapprovisionner" else "Consommer Intrant",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAddingStock) FarmSuccessGreen else FarmPendingOrange
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${item.name} (${item.quantity} ${item.unit} restants)", fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = adjustQty,
                        onValueChange = { adjustQty = it },
                        label = { Text("Quantité à ${if (isAddingStock) "ajouter" else "consommer"} (${item.unit})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(onClick = { showAdjustStockDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("Annuler")
                        }
                        Button(
                            onClick = {
                                val adj = adjustQty.toDoubleOrNull()
                                if (adj != null && adj > 0) {
                                    val newQty = if (isAddingStock) {
                                        item.quantity + adj
                                    } else {
                                        (item.quantity - adj).coerceAtLeast(0.0)
                                    }
                                    viewModel.updateInventoryItem(item.copy(quantity = newQty))

                                    // If we are restocking, let's record the financial transaction
                                    if (isAddingStock) {
                                        viewModel.addTransaction(
                                            type = "OUT",
                                            category = if (item.category == "ALIMENT") "Aliments" else "Vétérinaire",
                                            amount = adj * item.pricePerUnit,
                                            description = "Réapprovisionnement : +$adj ${item.unit} de ${item.name}",
                                            bandeId = null,
                                            isSale = false
                                        )
                                    }
                                    showAdjustStockDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAddingStock) FarmSuccessGreen else FarmPendingOrange
                            )
                        ) {
                            Text("Confirmer")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================================
// 3. BOUTON FINANCES PANEL
// ==========================================================
@Composable
fun FinancePanel(
    viewModel: FarmViewModel,
    transactions: List<FarmTransaction>,
    users: List<User>,
    bandes: List<Bande>
) {
    var showAddTxDialog by remember { mutableStateOf(false) }
    var txType by remember { mutableStateOf("IN") } // "IN", "OUT"
    var txCategory by remember { mutableStateOf("Vente") }
    var txAmount by remember { mutableStateOf("") }
    var txDescription by remember { mutableStateOf("") }

    var selectedTab by remember { mutableStateOf(0) } // 0: Flux & Cashflow, 1: Répartition & Ventes

    val totalIn = transactions.filter { it.type == "IN" }.sumOf { it.amount }
    val totalOut = transactions.filter { it.type == "OUT" }.sumOf { it.amount }
    val netCashFlow = totalIn - totalOut

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Caisse & Prevision", fontSize = 11.sp, maxLines = 1) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Revenus & Intrants", fontSize = 11.sp, maxLines = 1) })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Bénéfices & Parts", fontSize = 11.sp, maxLines = 1) })
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTab == 0) {
            // Cash Flow and Previsions
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Résumé de Caisse", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Entrées", fontSize = 12.sp, color = Color.Gray)
                            Text("+${formatFCFA(totalIn)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = FarmSuccessGreen)
                        }
                        Column {
                            Text("Total Sorties", fontSize = 12.sp, color = Color.Gray)
                            Text("-${formatFCFA(totalOut)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = FarmAlertRed)
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Solde Cash Flow :", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(formatFCFA(netCashFlow), fontWeight = FontWeight.Black, fontSize = 20.sp, color = if (netCashFlow >= 0) FarmSuccessGreen else FarmAlertRed)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Forecasts (Prévisions de dépenses)
            Text("Prévisions de Dépenses (Lots Actifs)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            val activeLots = bandes.filter { it.status == "ACTIVE" }
            if (activeLots.isEmpty()) {
                Text("Aucun lot actif pour calculer les prévisions.", fontSize = 12.sp, color = Color.Gray)
            } else {
                activeLots.forEach { lot ->
                    val age = calculateAgeInDays(lot.arrivalDate)
                    // Estimate feed consumption based on age: 
                    // J1-J10: ~0.5kg/chicken, J11-J20: ~1kg/chicken, J21-J35: ~2kg/chicken
                    val feedPerChicken = when {
                        age <= 10 -> 0.5
                        age <= 20 -> 1.2
                        else -> 2.5
                    }
                    val estimatedFeedCost = lot.initialCount * feedPerChicken * 450.0 // average 450 FCFA per kg
                    val vaccineCost = lot.initialCount * 80.0 // approx 80 FCFA vaccine per chick

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(lot.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Âge : J$age", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Est. Aliment (${feedPerChicken} kg/sujet) :", fontSize = 12.sp)
                                Text(formatFCFA(estimatedFeedCost), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Est. Vétérinaire (Vaccins/Vits) :", fontSize = 12.sp)
                                Text(formatFCFA(vaccineCost), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Divider(modifier = Modifier.padding(vertical = 6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Prévisionnel :", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(formatFCFA(estimatedFeedCost + vaccineCost), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FarmAlertRed)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add manual transaction button
            Button(
                onClick = { showAddTxDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AddCard, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Saisir un Flux Financier")
            }
        } else if (selectedTab == 1) {
            // Detailed Revenues & Expenses Breakdown
            val salesRevenues = transactions.filter { it.type == "IN" && (it.isSale || it.category == "Vente") }.sumOf { it.amount }
            val otherRevenues = transactions.filter { it.type == "IN" && !(it.isSale || it.category == "Vente") }.sumOf { it.amount }

            val foodExpenses = transactions.filter { it.type == "OUT" && (it.category == "Aliment" || it.category == "Alidents" || it.category == "Aliments") }.sumOf { it.amount }
            val vetExpenses = transactions.filter { it.type == "OUT" && it.category == "Vétérinaire" }.sumOf { it.amount }
            val chickExpenses = transactions.filter { it.type == "OUT" && it.category == "Achat poussins" }.sumOf { it.amount }
            val otherExpenses = transactions.filter { it.type == "OUT" && !listOf("Aliment", "Alidents", "Aliments", "Vétérinaire", "Achat poussins").contains(it.category) }.sumOf { it.amount }

            val totalAllExpenses = foodExpenses + vetExpenses + chickExpenses + otherExpenses

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Rentabilité de l'exploitation", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = if (totalAllExpenses > 0) {
                                    String.format("Ratio Ventes/Intrants : %.1f x", salesRevenues / totalAllExpenses.coerceAtLeast(1.0))
                                } else "Aucun intrant acheté",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    }
                }

                val context = LocalContext.current
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // D3.JS COMPARATIVE GRAPH CARD
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Évolution Mensuelle (D3.js)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(8.dp).background(FarmSuccessGreen, CircleShape))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Revenus", fontSize = 10.sp, color = Color.Gray)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(modifier = Modifier.size(8.dp).background(FarmAlertRed, CircleShape))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Charges", fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                MonthlyProfitChartWebView(transactions = transactions)
                            }
                        }
                    }

                    // PDF EXPORT BUTTON
                    item {
                        Button(
                            onClick = {
                                val currentMonth = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.FRENCH).format(java.util.Date())
                                com.example.util.PdfExporter.exportMonthlyReportPdf(
                                    context = context,
                                    reportTitle = "Bilan d'Exploitation - $currentMonth",
                                    transactions = transactions,
                                    bandes = bandes
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenDark),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Exporter le Bilan Mensuel (PDF)")
                        }
                    }

                    // 1. REVENUES SECTION
                    item {
                        Text("Revenus (Vente de Poulets)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier.size(8.dp).background(FarmSuccessGreen, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Ventes de Poulets de Chair", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    }
                                    Text(formatFCFA(salesRevenues), fontWeight = FontWeight.Bold, color = FarmSuccessGreen, fontSize = 14.sp)
                                }
                                if (otherRevenues > 0.0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier.size(8.dp).background(Color.Gray, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Autres entrées", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                        }
                                        Text(formatFCFA(otherRevenues), fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }

                    // 2. EXPENSES (INTRANTS) SECTION
                    item {
                        Text("Dépenses (Intrants & Charges)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FarmAlertRed)
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                // Food/Aliment
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(8.dp).background(FarmPendingOrange, CircleShape))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Aliments & Nutrition", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    }
                                    Text(formatFCFA(foodExpenses), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                // Veterinary
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(8.dp).background(FarmGreenPrimary, CircleShape))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Produits Vétérinaires", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    }
                                    Text(formatFCFA(vetExpenses), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                // Chicks/Poussins
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(8.dp).background(Color.Magenta, CircleShape))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Achats de Poussins", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    }
                                    Text(formatFCFA(chickExpenses), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                if (otherExpenses > 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(8.dp).background(Color.Gray, CircleShape))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Autres intrants / frais", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                        }
                                        Text(formatFCFA(otherExpenses), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }

                                Divider(modifier = Modifier.padding(vertical = 10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Total des Charges :", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(formatFCFA(totalAllExpenses), fontWeight = FontWeight.Bold, color = FarmAlertRed, fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    // 3. TRANSACTION HISTORY SECTION
                    item {
                        Text("Journal des Transactions", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    val relevantTxs = transactions.filter { it.type == "IN" || it.type == "OUT" }
                    if (relevantTxs.isEmpty()) {
                        item {
                            Text("Aucune transaction enregistrée.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(4.dp))
                        }
                    } else {
                        items(relevantTxs.sortedByDescending { it.date }) { tx ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(tx.description, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (tx.type == "IN") Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                                contentDescription = null,
                                                tint = if (tx.type == "IN") FarmSuccessGreen else FarmAlertRed,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("${tx.category} • ${formatDate(tx.date)}", fontSize = 10.sp, color = Color.Gray)
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = (if (tx.type == "IN") "+" else "-") + formatFCFA(tx.amount),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (tx.type == "IN") FarmSuccessGreen else FarmAlertRed
                                        )
                                        IconButton(
                                            onClick = { viewModel.deleteTransaction(tx) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = { showAddTxDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saisir un Flux Financier", fontSize = 13.sp)
                }
            }
        } else {
            // Profit splits & partner distribution
            val totalSales = transactions.filter { it.isSale }.sumOf { it.amount }
            val saleExpenses = transactions.filter { it.type == "OUT" && it.category == "Aliment" }.sumOf { it.amount } + 
                               transactions.filter { it.type == "OUT" && it.category == "Vétérinaire" }.sumOf { it.amount }
            val netSalesProfit = totalSales - saleExpenses

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Tableau des Ventes & Bénéfices", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Volume des ventes :", fontSize = 13.sp)
                        Text(formatFCFA(totalSales), fontWeight = FontWeight.Bold, color = FarmSuccessGreen)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Dépenses d'élevage liées :", fontSize = 13.sp)
                        Text("-${formatFCFA(saleExpenses)}", fontWeight = FontWeight.Bold, color = FarmAlertRed)
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Bénéfice Net Répartissable :", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(formatFCFA(netSalesProfit), fontSize = 16.sp, fontWeight = FontWeight.Black, color = FarmSuccessGreen)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Share calculation among partners
            val partnersList = users.filter { it.role == "PARTENAIRE" }
            Text("Répartition des bénéfices (${partnersList.size + 1} associés)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            if (partnersList.isEmpty()) {
                // Default share split
                val share = netSalesProfit / 2
                Text("Aucun autre partenaire créé. Partage 50% Promoteur / 50% Réserve :", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Part Administrateur (50%) :", fontWeight = FontWeight.Medium)
                            Text(formatFCFA(share), color = FarmSuccessGreen)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Réserve d'exploitation (50%) :", fontWeight = FontWeight.Medium)
                            Text(formatFCFA(share), color = FarmSuccessGreen)
                        }
                    }
                }
            } else {
                // Share evenly split among Admin + all partners
                val count = partnersList.size + 1
                val share = netSalesProfit / count
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Administrateur (Promoteur) :", fontWeight = FontWeight.Bold)
                                Text(formatFCFA(share), fontWeight = FontWeight.Bold, color = FarmSuccessGreen)
                            }
                        }
                    }
                    items(partnersList) { partner ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${partner.name} (Partenaire) :", fontWeight = FontWeight.Bold)
                                Text(formatFCFA(share), fontWeight = FontWeight.Bold, color = FarmSuccessGreen)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddTxDialog) {
        Dialog(onDismissRequest = { showAddTxDialog = false }) {
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
                    Text("Saisir un Flux", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { txType = "IN" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (txType == "IN") FarmSuccessGreen else Color.LightGray
                            )
                        ) {
                            Text("ENTRÉE (+)")
                        }
                        Button(
                            onClick = { txType = "OUT" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (txType == "OUT") FarmAlertRed else Color.LightGray
                            )
                        ) {
                            Text("SORTIE (-)")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = txAmount,
                        onValueChange = { txAmount = it },
                        label = { Text("Montant en FCFA") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = txDescription,
                        onValueChange = { txDescription = it },
                        label = { Text("Libellé / Description") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Catégorie :", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.align(Alignment.Start))
                    val categories = if (txType == "IN") listOf("Vente", "Autre") else listOf("Achat poussins", "Aliment", "Vétérinaire", "Autre")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = txCategory == cat,
                                onClick = { txCategory = cat },
                                label = { Text(cat, fontSize = 10.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(onClick = { showAddTxDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("Annuler")
                        }
                        Button(
                            onClick = {
                                val amt = txAmount.toDoubleOrNull()
                                if (amt != null && txDescription.isNotBlank()) {
                                    viewModel.addTransaction(
                                        type = txType,
                                        category = txCategory,
                                        amount = amt,
                                        description = txDescription,
                                        bandeId = null,
                                        isSale = (txCategory == "Vente")
                                    )
                                    txAmount = ""
                                    txDescription = ""
                                    showAddTxDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                        ) {
                            Text("Enregistrer")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================================
// 4. BOUTON ALIMENT PANEL
// ==========================================================
@Composable
fun AlimentFormulaPanel(
    viewModel: FarmViewModel,
    formulas: List<FeedFormula>,
    bandes: List<Bande>
) {
    var selectedFeedType by remember { mutableStateOf("Démarrage") } // "Démarrage", "Croissance", "Finition"
    
    var showAddIntrant by remember { mutableStateOf(false) }
    var intrantName by remember { mutableStateOf("") }
    var intrantPrice by remember { mutableStateOf("") }
    var intrantPercent by remember { mutableStateOf("") }

    var showRemoveIntrant by remember { mutableStateOf(false) }
    var intrantToRemoveName by remember { mutableStateOf("") }

    val activeLots = bandes.filter { it.status == "ACTIVE" }
    val typeIngredients = formulas.filter { it.feedType == selectedFeedType }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Dropdown to select feed type
        Text("Type d'aliment / Formule :", fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Démarrage", "Croissance", "Finition").forEach { type ->
                FilterChip(
                    selected = selectedFeedType == type,
                    onClick = { selectedFeedType = type },
                    label = { Text(type) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lots consuming this feed
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Lots consommant cet aliment :", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                val matchingBandes = activeLots.filter { b ->
                    val age = calculateAgeInDays(b.arrivalDate)
                    val expectedType = when {
                        age <= 10 -> "Démarrage"
                        age <= 20 -> "Croissance"
                        else -> "Finition"
                    }
                    expectedType == selectedFeedType
                }
                if (matchingBandes.isEmpty()) {
                    Text("Aucun lot actif n'est à cet âge actuellement.", fontSize = 12.sp, color = Color.Gray)
                } else {
                    matchingBandes.forEach { b ->
                        Text("- ${b.name} (Âge: J${calculateAgeInDays(b.arrivalDate)})", fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Ingredients Column and adjustments
        Text("Composition Actuelle (Total : ${typeIngredients.sumOf { it.percentage }}%)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(typeIngredients) { ing ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(ing.ingredientName, fontWeight = FontWeight.Bold)
                            Text("Prix : ${ing.pricePerKg} FCFA/kg", fontSize = 12.sp, color = Color.Gray)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${ing.percentage}%", fontSize = 16.sp, fontWeight = FontWeight.Black, color = FarmGreenPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            // Small adjustment buttons
                            IconButton(
                                onClick = {
                                    if (ing.percentage > 1) {
                                        viewModel.addOrUpdateIngredient(selectedFeedType, ing.ingredientName, ing.percentage - 1, ing.pricePerKg)
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = FarmAlertRed)
                            }
                            IconButton(
                                onClick = {
                                    if (ing.percentage < 99) {
                                        viewModel.addOrUpdateIngredient(selectedFeedType, ing.ingredientName, ing.percentage + 1, ing.pricePerKg)
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = FarmSuccessGreen)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Buttons Ajouter / Retirer intrant as requested
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { showAddIntrant = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = FarmSuccessGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ajouter Intrant", fontSize = 13.sp)
            }

            Button(
                onClick = {
                    if (typeIngredients.isNotEmpty()) {
                        intrantToRemoveName = typeIngredients.first().ingredientName
                        showRemoveIntrant = true
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = FarmAlertRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Retirer Intrant", fontSize = 13.sp)
            }
        }
    }

    // Dialog Ajouter Intrant
    if (showAddIntrant) {
        Dialog(onDismissRequest = { showAddIntrant = false }) {
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
                    Text("Ajouter un Intrant", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = FarmSuccessGreen)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = intrantName,
                        onValueChange = { intrantName = it },
                        label = { Text("Nom de l'intrant") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = intrantPrice,
                        onValueChange = { intrantPrice = it },
                        label = { Text("Prix par kg (FCFA)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = intrantPercent,
                        onValueChange = { intrantPercent = it },
                        label = { Text("Pourcentage d'incorporation (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Les autres intrants s'afficheront pour ajuster la formule à 100%.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(onClick = { showAddIntrant = false }, modifier = Modifier.weight(1f)) {
                            Text("Annuler")
                        }
                        Button(
                            onClick = {
                                val price = intrantPrice.toDoubleOrNull()
                                val percent = intrantPercent.toDoubleOrNull()
                                if (intrantName.isNotBlank() && price != null && percent != null) {
                                    viewModel.addOrUpdateIngredient(selectedFeedType, intrantName, percent, price)
                                    // Auto-adjust other ingredients if total > 100% (mock or let user click and adjust)
                                    intrantName = ""
                                    intrantPrice = ""
                                    intrantPercent = ""
                                    showAddIntrant = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = FarmSuccessGreen)
                        ) {
                            Text("Ajouter")
                        }
                    }
                }
            }
        }
    }

    // Dialog Retirer Intrant
    if (showRemoveIntrant) {
        Dialog(onDismissRequest = { showRemoveIntrant = false }) {
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
                    Text("Retirer un Intrant", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = FarmAlertRed)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Intrant à retirer :", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.align(Alignment.Start))
                    var expanded by remember { mutableStateOf(false) }

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
                            Text(intrantToRemoveName, fontWeight = FontWeight.Medium)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            typeIngredients.forEach { ing ->
                                DropdownMenuItem(
                                    text = { Text(ing.ingredientName) },
                                    onClick = {
                                        intrantToRemoveName = ing.ingredientName
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(onClick = { showRemoveIntrant = false }, modifier = Modifier.weight(1f)) {
                            Text("Annuler")
                        }
                        Button(
                            onClick = {
                                if (intrantToRemoveName.isNotBlank()) {
                                    viewModel.removeIngredient(selectedFeedType, intrantToRemoveName)
                                    showRemoveIntrant = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = FarmAlertRed)
                        ) {
                            Text("Supprimer")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================================
// 5. GESTION DES VENDEURS PANEL
// ==========================================================
@Composable
fun SellerManagementPanel(
    viewModel: FarmViewModel,
    users: List<User>,
    orders: List<Order>,
    transactions: List<FarmTransaction>
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Vendeurs, 1: Partenaires
    val context = LocalContext.current

    // Dialog state for adding a collaborator transaction
    var showTxDialog by remember { mutableStateOf(false) }
    var selectedCollabName by remember { mutableStateOf("") }
    var collabTxType by remember { mutableStateOf("OUT") } // "IN" (Investment), "OUT" (Payout/Commission)
    var collabTxAmount by remember { mutableStateOf("") }
    var collabTxDesc by remember { mutableStateOf("") }
    var isPartnerTx by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storefront, contentDescription = null, tint = if (selectedTab == 0) FarmPendingOrange else Color.Gray)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Vendeurs", fontWeight = FontWeight.Bold)
                }
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Group, contentDescription = null, tint = if (selectedTab == 1) FarmGreenPrimary else Color.Gray)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Partenaires Externes", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTab == 0) {
            // VENDEURS TAB
            val sellers = users.filter { it.role == "VENDEUR" }
            if (sellers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aucun vendeur actif dans la base de données.", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(sellers) { seller ->
                        val sellerOrders = orders.filter { it.sellerName == seller.name }
                        val totalSalesValue = sellerOrders.filter { it.status == "PROCESSED" }.sumOf { it.totalAmount }
                        val totalSalesCount = sellerOrders.count { it.status == "PROCESSED" }
                        val uniqueClients = sellerOrders.map { it.clientName }.distinct().size

                        // Find matching payout transactions for this seller (e.g. description contains their name)
                        val sellerPayments = transactions.filter {
                            it.type == "OUT" && it.description.contains(seller.name, ignoreCase = true)
                        }
                        val totalPaid = sellerPayments.sumOf { it.amount }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Storefront, contentDescription = null, tint = FarmPendingOrange)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(seller.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                    Text("Ventes : ${formatFCFA(totalSalesValue)}", fontWeight = FontWeight.Black, color = FarmSuccessGreen, fontSize = 14.sp)
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Clients gérés : $uniqueClients", fontSize = 12.sp, color = Color.Gray)
                                    Text("Commandes traitées : $totalSalesCount", fontSize = 12.sp, color = Color.Gray)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Commissions payées :", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(formatFCFA(totalPaid), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FarmAlertRed)
                                }

                                if (sellerOrders.isNotEmpty()) {
                                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                                    Text("Clients :", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        sellerOrders.map { it.clientName }.distinct().forEach { client ->
                                            SuggestionChip(onClick = {}, label = { Text(client, fontSize = 10.sp) })
                                        }
                                    }
                                }

                                if (sellerPayments.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Historique des règlements :", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    sellerPayments.take(3).forEach { pay ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(pay.description, fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                                            Text("-${formatFCFA(pay.amount)}", fontSize = 11.sp, color = FarmAlertRed, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        selectedCollabName = seller.name
                                        collabTxType = "OUT"
                                        collabTxAmount = ""
                                        collabTxDesc = "Règlement commission Vendeur: ${seller.name}"
                                        isPartnerTx = false
                                        showTxDialog = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Enregistrer un Règlement", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // PARTENAIRES TAB
            val partners = users.filter { it.role == "PARTENAIRE" }
            if (partners.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aucun partenaire externe enregistré.", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(partners) { partner ->
                        val partnerTransactions = transactions.filter {
                            it.description.contains(partner.name, ignoreCase = true)
                        }
                        val totalInvested = partnerTransactions.filter { it.type == "IN" }.sumOf { it.amount }
                        val totalPayouts = partnerTransactions.filter { it.type == "OUT" }.sumOf { it.amount }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Group, contentDescription = null, tint = FarmGreenPrimary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(partner.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                    Text("Parts", fontWeight = FontWeight.SemiBold, color = Color.Gray, fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Total Apports (Investis)", fontSize = 11.sp, color = Color.Gray)
                                        Text(formatFCFA(totalInvested), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FarmSuccessGreen)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Total Retours (Revenus/Bénéfices)", fontSize = 11.sp, color = Color.Gray)
                                        Text(formatFCFA(totalPayouts), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                                    }
                                }

                                if (partnerTransactions.isNotEmpty()) {
                                    Divider(modifier = Modifier.padding(vertical = 10.dp))
                                    Text("Transactions récentes :", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    partnerTransactions.take(3).forEach { tx ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (tx.type == "IN") Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                                    contentDescription = null,
                                                    tint = if (tx.type == "IN") FarmSuccessGreen else FarmAlertRed,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(tx.description, fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                                            }
                                            Text(
                                                text = (if (tx.type == "IN") "+" else "-") + formatFCFA(tx.amount),
                                                fontSize = 11.sp,
                                                color = if (tx.type == "IN") FarmSuccessGreen else FarmAlertRed,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            selectedCollabName = partner.name
                                            collabTxType = "OUT"
                                            collabTxAmount = ""
                                            collabTxDesc = "Versement dividendes Partenaire: ${partner.name}"
                                            isPartnerTx = true
                                            showTxDialog = true
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Distribuer Parts", fontSize = 11.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            selectedCollabName = partner.name
                                            collabTxType = "IN"
                                            collabTxAmount = ""
                                            collabTxDesc = "Apport de capital Partenaire: ${partner.name}"
                                            isPartnerTx = true
                                            showTxDialog = true
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Saisir Apport", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTxDialog) {
        Dialog(onDismissRequest = { showTxDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isPartnerTx) "Transaction Partenaire" else "Règlement Vendeur",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Collaborateur : $selectedCollabName", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(16.dp))

                    if (isPartnerTx) {
                        Text("Type de Flux", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(
                                selected = collabTxType == "IN",
                                onClick = {
                                    collabTxType = "IN"
                                    collabTxDesc = "Apport de capital Partenaire: $selectedCollabName"
                                },
                                label = { Text("Apport (+)") }
                            )
                            FilterChip(
                                selected = collabTxType == "OUT",
                                onClick = {
                                    collabTxType = "OUT"
                                    collabTxDesc = "Versement dividendes Partenaire: $selectedCollabName"
                                },
                                label = { Text("Versement (-)") }
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = collabTxAmount,
                        onValueChange = { collabTxAmount = it },
                        label = { Text("Montant (FCFA)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = collabTxDesc,
                        onValueChange = { collabTxDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(onClick = { showTxDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("Annuler")
                        }
                        Button(
                            onClick = {
                                val amt = collabTxAmount.toDoubleOrNull()
                                if (amt != null && collabTxDesc.isNotBlank()) {
                                    viewModel.addTransaction(
                                        type = collabTxType,
                                        category = "Autre",
                                        amount = amt,
                                        description = collabTxDesc,
                                        bandeId = null,
                                        isSale = collabTxType == "IN" && !isPartnerTx
                                    )
                                    showTxDialog = false
                                }
                            },
                            modifier = Modifier.weight(1.2f),
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                        ) {
                            Text("Enregistrer")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================================
// 6. GESTION DES COMMANDES PANEL (WITH WORKFLOWS, API DASHBOARD, & NOTIFICATIONS)
// ==========================================================
@Composable
fun OrderManagementPanel(
    viewModel: FarmViewModel,
    orders: List<Order>
) {
    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }
    var orderToEdit by remember { mutableStateOf<Order?>(null) }

    var editClientName by remember { mutableStateOf("") }
    var editClientPhone by remember { mutableStateOf("") }
    var editQuantity by remember { mutableStateOf("") }
    var editPrice by remember { mutableStateOf("") }
    var editPaymentStatus by remember { mutableStateOf("NON_PAYE") }
    var editDeliveryOption by remember { mutableStateOf(1) }

    // State for API integration dashboard & notifications
    var lastKnownOrdersCount by remember { mutableStateOf(orders.size) }
    var showNewOrderNotification by remember { mutableStateOf(false) }
    var notificationMessage by remember { mutableStateOf("") }

    // Listen to changes in orders size to trigger visual in-app notifications
    LaunchedEffect(orders.size) {
        if (orders.size > lastKnownOrdersCount) {
            val newOrders = orders.size - lastKnownOrdersCount
            val latestOrder = orders.lastOrNull()
            if (latestOrder != null && (latestOrder.sellerName == "API Client Externe" || latestOrder.sellerName == "Client standalone" || latestOrder.sellerName == "CLIENT")) {
                notificationMessage = "Nouvelle commande client de ${latestOrder.clientName} !"
                showNewOrderNotification = true
            }
        }
        lastKnownOrdersCount = orders.size
    }

    // Active status filter: "Tous", "En attente", "Validée", "En cours de livraison", "Livrée"
    var selectedFilterStatus by remember { mutableStateOf("Tous") }

    // Filter orders list based on status keys mapping
    val filteredOrders = remember(orders, selectedFilterStatus) {
        if (selectedFilterStatus == "Tous") {
            orders
        } else {
            orders.filter { order ->
                when (selectedFilterStatus) {
                    "En attente" -> order.status == "PENDING" || order.status == "En attente"
                    "Validée" -> order.status == "VALIDATED" || order.status == "Validée"
                    "En cours de livraison" -> order.status == "DELIVERING" || order.status == "En cours de livraison"
                    "Livrée" -> order.status == "DELIVERED" || order.status == "Livrée" || order.status == "PROCESSED"
                    else -> false
                }
            }
        }
    }

    // Count client orders (API & standalone boutique)
    val apiOrdersCount = remember(orders) {
        orders.count { it.sellerName == "API Client Externe" || it.sellerName == "Client standalone" || it.sellerName == "CLIENT" }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Real-time API Integration & Stats Banner (MOTHER APP ENDPOINT STATUS)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("API RECEPTEUR CLIENT : ACTIF", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FarmGreenDark)
                    }
                    Badge(containerColor = FarmGreenPrimary, contentColor = Color.White) {
                        Text("$apiOrdersCount Commande(s) Client", fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = "Endpoint : POST http://localhost:8080/api/orders",
                    fontSize = 11.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Token requis : Bearer FarmSecureToken2026",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Simulation Button
                Button(
                    onClick = {
                        // Directly simulate a REST POST arrival on background
                        val names = listOf("Restaurant Le Repère", "Maquis du Val", "Supermarché Proxi Cocody", "Amandine Traiteur")
                        val places = listOf("Angré", "Deux Plateaux", "Marcory", "Zone 4")
                        val simulatedClient = names.random()
                        val simulatedPlace = places.random()
                        val simulatedQty = (10..150).random()
                        
                        viewModel.createOrder(
                            clientName = "$simulatedClient ($simulatedPlace) - $simulatedQty x Poulets rôtis",
                            clientPhone = "+225070" + (1000000..9999999).random().toString(),
                            quantity = simulatedQty,
                            unitPrice = 3000.0,
                            sellerName = "API Client Externe",
                            paymentStatus = "NON_PAYE",
                            deliveryDate = System.currentTimeMillis() + 24 * 60 * 60 * 1000
                        )
                        Toast.makeText(context, "Simulation de Commande Client Externe reçue !", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SIMULER COMMANDE CLIENT EXTERNE (API POST)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Heads-up visual notification banner
        AnimatedVisibility(
            visible = showNewOrderNotification,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                border = BorderStroke(1.5.dp, Color(0xFFFF9800))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Nouvelle Commande !", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFE65100))
                            Text(notificationMessage, fontSize = 12.sp, color = Color(0xFFE65100))
                        }
                    }
                    IconButton(onClick = { showNewOrderNotification = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color(0xFFE65100))
                    }
                }
            }
        }

        // Title and filters layout
        Text("Suivi Général & Flux des Commandes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal status chips/tabs filter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Tous", "En attente", "Validée", "En cours de livraison", "Livrée").forEach { statusLabel ->
                val isSelected = selectedFilterStatus == statusLabel
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilterStatus = statusLabel },
                    label = { Text(statusLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FarmGreenPrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredOrders.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Aucune commande dans cet état", fontSize = 14.sp, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredOrders) { order ->
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("order_item_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Top row with Client Name and Type/Integration Badges
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(order.clientName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Tel : ${order.clientPhone}", fontSize = 12.sp, color = Color.Gray)
                                    
                                    // Delivery date formatted
                                    val sdfDelivery = SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH)
                                    val deliveryDateStr = sdfDelivery.format(Date(order.deliveryDate))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Livraison : $deliveryDateStr", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                                
                                // Status & Origin Badges Column
                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Origin Badge for External Clients
                                    if (order.sellerName == "API Client Externe") {
                                        Badge(containerColor = Color(0xFFE3F2FD), contentColor = Color(0xFF0D47A1)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                                                Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color(0xFF0D47A1))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("API Externe", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    } else if (order.sellerName == "Client standalone" || order.sellerName == "CLIENT") {
                                        Badge(containerColor = Color(0xFFE8F5E9), contentColor = Color(0xFF2E7D32)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                                                Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color(0xFF2E7D32))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("Espace Client", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    // Material 3 Workflow Status Badge
                                    val (statusText, statusBg, statusFg) = when (order.status) {
                                        "PENDING", "En attente" -> Triple("En attente", Color(0xFFFFF3E0), Color(0xFFE65100))
                                        "VALIDATED", "Validée" -> Triple("Validée", Color(0xFFE0F7FA), Color(0xFF006064))
                                        "DELIVERING", "En cours de livraison" -> Triple("En cours", Color(0xFFF3E5F5), Color(0xFF4A148C))
                                        "DELIVERED", "Livrée", "PROCESSED" -> Triple("Livrée", Color(0xFFE8F5E9), Color(0xFF1B5E20))
                                        else -> Triple(order.status, Color(0xFFF5F5F5), Color.DarkGray)
                                    }
                                    
                                    Badge(containerColor = statusBg, contentColor = statusFg) {
                                        Text(statusText, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Quantité : ${order.quantity} poulets", fontSize = 13.sp)
                                Text("Prix Unitaire : ${order.unitPrice} FCFA", fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Vendeur/Canal : ${order.sellerName}", fontSize = 12.sp, color = Color.Gray)
                                Text("Total : ${formatFCFA(order.totalAmount)}", fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                            }

                            // Interactive Status Transition Chips (DIRECT WORKFLOW TRANSITIONS)
                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(modifier = Modifier.padding(vertical = 4.dp).alpha(0.5f))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Workflow :", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                
                                listOf(
                                    "PENDING" to "En attente",
                                    "VALIDATED" to "Validée",
                                    "DELIVERING" to "Livraison",
                                    "DELIVERED" to "Livrée"
                                ).forEach { (statKey, statLabel) ->
                                    val isCurrent = order.status == statKey || 
                                                    (statKey == "PENDING" && order.status == "En attente") ||
                                                    (statKey == "VALIDATED" && order.status == "Validée") ||
                                                    (statKey == "DELIVERING" && order.status == "En cours de livraison") ||
                                                    (statKey == "DELIVERED" && (order.status == "Livrée" || order.status == "PROCESSED"))

                                    val chipBg = if (isCurrent) {
                                        when (statKey) {
                                            "PENDING" -> Color(0xFFFF9800)
                                            "VALIDATED" -> Color(0xFF00ACC1)
                                            "DELIVERING" -> Color(0xFF8E24AA)
                                            "DELIVERED" -> Color(0xFF4CAF50)
                                            else -> Color.Gray
                                        }
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    }

                                    val chipContentColor = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(chipBg)
                                            .clickable {
                                                viewModel.updateOrderStatus(order, statKey)
                                            }
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Text(statLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = chipContentColor)
                                    }
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // MODIFIER LA COMMANDE Button
                                OutlinedButton(
                                    onClick = {
                                        orderToEdit = order
                                        editClientName = order.clientName
                                        editClientPhone = order.clientPhone
                                        editQuantity = order.quantity.toString()
                                        editPrice = order.unitPrice.toString()
                                        editPaymentStatus = order.paymentStatus
                                        editDeliveryOption = when {
                                            order.deliveryDate - order.date < 12 * 60 * 60 * 1000 -> 0
                                            order.deliveryDate - order.date < 36 * 60 * 60 * 1000 -> 1
                                            order.deliveryDate - order.date < 5 * 24 * 60 * 60 * 1000 -> 2
                                            else -> 3
                                        }
                                        showEditDialog = true
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 4.dp)
                                        .testTag("modify_order_btn")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Modifier", fontSize = 11.sp)
                                }

                                // WhatsApp Invoice Action
                                Button(
                                    onClick = {
                                        val invoice = """
                                            *LES FERMES ABIDJANAISES*
                                            -------------------------------------
                                            *FACTURE N° ${order.id}*
                                            Date : ${java.text.DateFormat.getDateInstance().format(order.date)}
                                            Client : ${order.clientName}
                                            Statut de traitement : ${if (order.status == "DELIVERED" || order.status == "PROCESSED") "Livrée" else "En attente de livraison"}
                                            -------------------------------------
                                            Total : ${order.totalAmount} FCFA
                                        """.trimIndent()
                                        sendWhatsAppInvoice(context, order.clientPhone, invoice)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .padding(start = 4.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Facture WhatsApp", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modifier Commande Dialog
    if (showEditDialog && orderToEdit != null) {
        Dialog(onDismissRequest = { showEditDialog = false }) {
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
                    Text("Modifier la Commande", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = editClientName,
                        onValueChange = { editClientName = it },
                        label = { Text("Nom du client") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editClientPhone,
                        onValueChange = { editClientPhone = it },
                        label = { Text("Téléphone WhatsApp") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editQuantity,
                            onValueChange = { editQuantity = it },
                            label = { Text("Quantité") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = editPrice,
                            onValueChange = { editPrice = it },
                            label = { Text("P.U. (FCFA)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.2f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Statut de Paiement", fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("NON_PAYE" to "Non Payé", "PAYE" to "Payé", "ACOMPTE" to "Acompte").forEach { (value, label) ->
                            val selected = editPaymentStatus == value
                            FilterChip(
                                selected = selected,
                                onClick = { editPaymentStatus = value },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Date de Livraison Prévue", fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0 to "Aujourd'hui", 1 to "Demain", 2 to "3 Jours", 3 to "7 Jours").forEach { (option, label) ->
                            val selected = editDeliveryOption == option
                            FilterChip(
                                selected = selected,
                                onClick = { editDeliveryOption = option },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(onClick = { showEditDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("Annuler")
                        }
                        Button(
                            onClick = {
                                val qty = editQuantity.toIntOrNull()
                                val prc = editPrice.toDoubleOrNull()
                                if (editClientName.isNotBlank() && qty != null && prc != null) {
                                    val deliveryTimestamp = orderToEdit!!.date + when (editDeliveryOption) {
                                        0 -> 0L
                                        1 -> 24L * 60 * 60 * 1000
                                        2 -> 3L * 24 * 60 * 60 * 1000
                                        else -> 7L * 24 * 60 * 60 * 1000
                                    }
                                    val updated = orderToEdit!!.copy(
                                        clientName = editClientName,
                                        clientPhone = editClientPhone,
                                        quantity = qty,
                                        unitPrice = prc,
                                        totalAmount = qty * prc,
                                        paymentStatus = editPaymentStatus,
                                        deliveryDate = deliveryTimestamp
                                    )
                                    viewModel.modifyOrder(updated)
                                    showEditDialog = false
                                }
                            },
                            modifier = Modifier.weight(1.2f),
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                        ) {
                            Text("Sauvegarder")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================================
// 7. BOUTON RESTAURATION PANEL
// ==========================================================
@Composable
fun RestorationPanel(viewModel: FarmViewModel) {
    var restorationDate by remember { mutableStateOf("") }
    var isRestoredSuccessfully by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!isRestoredSuccessfully) {
            Icon(Icons.Default.Restore, contentDescription = null, tint = FarmAlertRed, modifier = Modifier.size(72.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Restauration de la base de données", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Cette fonctionnalité permet de restaurer l'application à un état antérieur en cas de BUG. Toutes les transactions, commandes et mortalities créées après la date spécifiée seront effacées pour retrouver l'état exact de ce jour.",
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = restorationDate,
                onValueChange = { restorationDate = it },
                label = { Text("Date de restauration (JJ/MM/AAAA)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("ex: 10/07/2026") }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (restorationDate.isNotBlank()) {
                        try {
                            val format = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
                            val dateObj = format.parse(restorationDate)
                            dateObj?.let {
                                viewModel.restoreDatabaseToDate(it.time) {
                                    isRestoredSuccessfully = true
                                }
                            }
                        } catch (e: Exception) {
                            // Simple fallback
                            viewModel.restoreDatabaseToDate(System.currentTimeMillis() - 3L*24*60*60*1000) {
                                isRestoredSuccessfully = true
                            }
                        }
                    } else {
                        // Restore 3 days ago by default
                        viewModel.restoreDatabaseToDate(System.currentTimeMillis() - 3L*24*60*60*1000) {
                            isRestoredSuccessfully = true
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = FarmAlertRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("confirm_restore_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("RESTAURER MAINTENANT", fontWeight = FontWeight.Bold)
            }
        } else {
            // Success view
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = FarmSuccessGreen, modifier = Modifier.size(80.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Base de Données Restaurée !", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = FarmSuccessGreen)
            Spacer(modifier = Modifier.height(8.dp))
            Text("L'application a été ramenée à l'état antérieur demandé avec succès.", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { isRestoredSuccessfully = false; restorationDate = "" },
                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Retour")
            }
        }
    }
}

// ==========================================================
// UTILITY FUNCTIONS
// ==========================================================
fun formatFCFA(amount: Double): String {
    return String.format("%,d", amount.toLong()) + " FCFA"
}

fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
    return sdf.format(date)
}

fun calculateAgeInDays(arrivalTimestamp: Long): Int {
    val now = System.currentTimeMillis()
    val diff = now - arrivalTimestamp
    val days = diff / (1000 * 60 * 60 * 24)
    return days.toInt().coerceAtLeast(1)
}

fun getProphylaxisInstruction(age: Int): String {
    return when (age) {
        in 1..10 -> {
            if (age in 4..5) "Vitamines en continu + Ajouter un antibiotique large spectre (infections ombilicales)"
            else "Vitamines en continu dans l'eau de boisson (Prédémarrage)"
        }
        11 -> "Vaccin Peste + Bronchite Infectieuse (H120) le matin. Vitamines l'après-midi."
        12, 13 -> "Vitamines de soutien pour contrer le stress thermique et vaccinal."
        14 -> "Vaccin Gumboro Intermédiaire le matin. Vitamines l'après-midi."
        15, 16 -> "Vitamines (Transition de croissance)."
        in 17..19 -> "Anticoccidien dans l'eau de boisson STRICTEMENT SANS VITAMINES."
        21 -> "Rappel Vaccin Gumboro Forte le matin. Vitamines l'après-midi."
        22, 23 -> "Vitamines d'accompagnement."
        in 24..27 -> "Protecteur hépatique alternativement avec de l'eau claire (détoxification foie/reins)."
        28 -> "Vaccin Lasota (Peste) le matin. Vitamines l'après-midi."
        29, 30 -> "Vitamines d'accompagnement de finition."
        in 31..35 -> "EAU CLAIRE STRICTEMENT UNIQUEMENT. Aucune vitamine ni médicament (Viande saine, sans résidus)."
        else -> "EAU CLAIRE STRICTEMENT UNIQUEMENT (Période d'attente / Vente en cours)."
    }
}

fun sendWhatsAppInvoice(context: android.content.Context, phone: String, message: String) {
    val cleanPhone = phone.replace("+", "").replace(" ", "")
    val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=" + Uri.encode(message))
    val intent = Intent(Intent.ACTION_VIEW, uri)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback: regular text share sheet
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, message)
        context.startActivity(Intent.createChooser(shareIntent, "Envoyer la facture via..."))
    }
}
