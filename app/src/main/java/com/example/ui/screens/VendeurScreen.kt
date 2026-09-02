package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.Order
import com.example.ui.theme.*
import com.example.ui.viewmodel.FarmViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendeurScreen(
    viewModel: FarmViewModel,
    onLogout: () -> Unit
) {
    val orders by viewModel.allOrders.collectAsState()
    val context = LocalContext.current

    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var orderToEdit by remember { mutableStateOf<Order?>(null) }

    var clientName by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var pricePerChicken by remember { mutableStateOf("3000") } // default standard price in Abidjan
    var paymentStatus by remember { mutableStateOf("NON_PAYE") }
    var deliveryOption by remember { mutableStateOf(1) } // 0: Today, 1: Tomorrow, 2: 3 days, 3: 7 days

    var editClientName by remember { mutableStateOf("") }
    var editClientPhone by remember { mutableStateOf("") }
    var editQuantity by remember { mutableStateOf("") }
    var editPrice by remember { mutableStateOf("") }
    var editPaymentStatus by remember { mutableStateOf("NON_PAYE") }
    var editDeliveryOption by remember { mutableStateOf(1) }

    val sellerUser by viewModel.currentUser.collectAsState()
    val sellerName = sellerUser?.name ?: "Vendeur"

    // Filter orders placed by this seller or from client/external interfaces
    val sellerOrders = orders.filter { 
        it.sellerName == sellerName || 
        it.sellerName == "Vendeur" || 
        it.sellerName == "Client standalone" || 
        it.sellerName == "API Client Externe" ||
        it.sellerName == "CLIENT"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Espace Vendeur ($sellerName)", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                actions = {
                    IconButton(onClick = onLogout, modifier = Modifier.testTag("vendeur_logout")) {
                        Icon(Icons.Default.Logout, contentDescription = "Déconnexion", tint = FarmAlertRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = FarmGreenPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_order_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Créer une commande")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Summary Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Ventes traitées :", fontSize = 12.sp, color = Color.Gray)
                        Text(
                            text = formatFCFA(sellerOrders.filter { it.status == "PROCESSED" }.sumOf { it.totalAmount }),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = FarmSuccessGreen
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Commandes en attente :", fontSize = 12.sp, color = Color.Gray)
                        Text(
                            text = "${sellerOrders.count { it.status == "PENDING" }} en attente",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = FarmPendingOrange
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Mes Commandes Clients", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            if (sellerOrders.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Aucune commande enregistrée. Cliquez sur + pour ajouter.", fontSize = 14.sp, color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sellerOrders) { order ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(order.clientName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text("WhatsApp : ${order.clientPhone}", fontSize = 12.sp, color = Color.Gray)
                                        
                                        // Delivery Date
                                        val sdfDelivery = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.FRENCH)
                                        val deliveryDateStr = sdfDelivery.format(java.util.Date(order.deliveryDate))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Livraison : $deliveryDateStr", fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (order.sellerName == "Client standalone" || order.sellerName == "API Client Externe" || order.sellerName == "CLIENT") {
                                            Badge(
                                                containerColor = Color(0xFFE8F5E9),
                                                contentColor = Color(0xFF2E7D32)
                                            ) {
                                                Text("Espace Client", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Badge(
                                            containerColor = if (order.status == "PROCESSED" || order.status == "DELIVERED" || order.status == "Livrée") FarmSuccessGreen else FarmPendingOrange,
                                            contentColor = Color.White
                                        ) {
                                            Text(if (order.status == "PROCESSED" || order.status == "DELIVERED" || order.status == "Livrée") "Traitée" else "En Attente", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp)
                                        }
                                        Badge(
                                            containerColor = when(order.paymentStatus) {
                                                "PAYE" -> FarmSuccessGreen
                                                "ACOMPTE" -> FarmPendingOrange
                                                else -> FarmAlertRed
                                            },
                                            contentColor = Color.White
                                        ) {
                                            Text(
                                                text = when(order.paymentStatus) {
                                                    "PAYE" -> "Payé"
                                                    "ACOMPTE" -> "Acompte"
                                                    else -> "Non Payé"
                                                },
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${order.quantity} poulets x ${order.unitPrice} FCFA", fontSize = 13.sp)
                                    Text("Total : ${formatFCFA(order.totalAmount)}", fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                                }

                                Divider(modifier = Modifier.padding(vertical = 10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // MODIFIER COMMAND
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
                                        modifier = Modifier.weight(1f).padding(end = 4.dp).testTag("vendeur_modify_btn")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Modifier", fontSize = 11.sp)
                                    }

                                    // TRAITER COMMANDE
                                    if (order.status == "PENDING") {
                                        Button(
                                            onClick = {
                                                viewModel.processOrder(order) { invoice ->
                                                    sendWhatsAppInvoice(context, order.clientPhone, invoice)
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = FarmSuccessGreen),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1.2f).padding(start = 4.dp).testTag("vendeur_process_btn")
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Traiter", fontSize = 11.sp)
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                val invoiceText = """
                                                    *LES FERMES ABIDJANAISES*
                                                    -------------------------------------
                                                    *FACTURE N° ${order.id}*
                                                    Date : ${java.text.DateFormat.getDateInstance().format(order.date)}
                                                    Client : ${order.clientName}
                                                    -------------------------------------
                                                    Total : ${order.totalAmount} FCFA
                                                """.trimIndent()
                                                sendWhatsAppInvoice(context, order.clientPhone, invoiceText)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1.2f).padding(start = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Partager Facture", fontSize = 11.sp)
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

    // Dialogue Créer Commande
    if (showCreateDialog) {
        Dialog(onDismissRequest = { showCreateDialog = false }) {
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
                    Text("Nouvelle Commande Client", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Nom du client") },
                        modifier = Modifier.fillMaxWidth().testTag("client_name_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = clientPhone,
                        onValueChange = { clientPhone = it },
                        label = { Text("WhatsApp (ex: +22507070707)") },
                        modifier = Modifier.fillMaxWidth().testTag("client_phone_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it },
                            label = { Text("Qté poulets") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("quantity_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = pricePerChicken,
                            onValueChange = { pricePerChicken = it },
                            label = { Text("Prix Unit. (FCFA)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.2f).testTag("unit_price_input"),
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
                            val selected = paymentStatus == value
                            FilterChip(
                                selected = selected,
                                onClick = { paymentStatus = value },
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
                            val selected = deliveryOption == option
                            FilterChip(
                                selected = selected,
                                onClick = { deliveryOption = option },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(onClick = { showCreateDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("Annuler")
                        }
                        Button(
                            onClick = {
                                val qty = quantity.toIntOrNull()
                                val price = pricePerChicken.toDoubleOrNull()
                                if (clientName.isNotBlank() && qty != null && price != null) {
                                    val deliveryTimestamp = System.currentTimeMillis() + when (deliveryOption) {
                                        0 -> 0L
                                        1 -> 24L * 60 * 60 * 1000
                                        2 -> 3L * 24 * 60 * 60 * 1000
                                        else -> 7L * 24 * 60 * 60 * 1000
                                    }
                                    viewModel.createOrder(
                                        clientName = clientName,
                                        clientPhone = clientPhone,
                                        quantity = qty,
                                        unitPrice = price,
                                        sellerName = sellerName,
                                        paymentStatus = paymentStatus,
                                        deliveryDate = deliveryTimestamp
                                    )
                                    clientName = ""
                                    clientPhone = ""
                                    quantity = ""
                                    paymentStatus = "NON_PAYE"
                                    deliveryOption = 1
                                    showCreateDialog = false
                                }
                            },
                            modifier = Modifier.weight(1.2f).testTag("vendeur_submit_order_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary)
                        ) {
                            Text("Enregistrer")
                        }
                    }
                }
            }
        }
    }

    // Dialogue Modifier Commande
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
