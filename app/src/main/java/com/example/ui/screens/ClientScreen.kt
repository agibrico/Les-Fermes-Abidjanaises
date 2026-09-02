package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.FarmViewModel
import java.text.SimpleDateFormat
import java.util.*

data class ClientProductItem(
    val id: String,
    val name: String,
    val category: String, // "Poulets Entiers", "Découpes", "Abats & Divers"
    val unitLabel: String, // "U" (Unité) or "kg"
    val priceDesc: String,
    val getUnitPrice: (Int) -> Double
)

val clientProductsList = listOf(
    // Poulets Entiers
    ClientProductItem(
        "poulet_1.8_1.9",
        "Poulet entier (1.8 à 1.9 kg)",
        "Poulets Entiers",
        "U",
        "3 500 FCFA l'unité",
        { 3500.0 }
    ),
    ClientProductItem(
        "poulet_2_2.2",
        "Poulet entier (2 à 2.2 kg)",
        "Poulets Entiers",
        "U",
        "4 000 FCFA l'unité",
        { 4000.0 }
    ),
    ClientProductItem(
        "poulet_2.3",
        "Poulet entier (à partir de 2.3 kg)",
        "Poulets Entiers",
        "U",
        "4 500 FCFA l'unité",
        { 4500.0 }
    ),
    // Découpes
    ClientProductItem(
        "escalopes",
        "Escalopes (Découpe)",
        "Découpes",
        "kg",
        "1-4 kg: 3500 | 5-9 kg: 3300 | 10kg+: 3000 FCFA",
        { qty ->
            when {
                qty < 1 -> 3500.0
                qty in 1..4 -> 3500.0
                qty in 5..9 -> 3300.0
                else -> 3000.0
            }
        }
    ),
    ClientProductItem(
        "cuisses",
        "Cuisses (Découpe)",
        "Découpes",
        "kg",
        "1-4 kg: 2500 | 5-9 kg: 2400 | 10kg+: 2200 FCFA",
        { qty ->
            when {
                qty < 1 -> 2500.0
                qty in 1..4 -> 2500.0
                qty in 5..9 -> 2400.0
                else -> 2200.0
            }
        }
    ),
    ClientProductItem(
        "ailes",
        "Ailes (Découpe)",
        "Découpes",
        "kg",
        "1-4 kg: 2800 | 5-9 kg: 2700 | 10kg+: 2600 FCFA",
        { qty ->
            when {
                qty < 1 -> 2800.0
                qty in 1..4 -> 2800.0
                qty in 5..9 -> 2700.0
                else -> 2600.0
            }
        }
    ),
    ClientProductItem(
        "tete_cou_dos",
        "Ensemble Tête-Cou-Dos",
        "Découpes",
        "kg",
        "1 000 FCFA le kg",
        { 1000.0 }
    ),
    // Abats & Divers
    ClientProductItem(
        "gesier",
        "Gésiers",
        "Abats & Divers",
        "U",
        "125 FCFA l'unité",
        { 125.0 }
    ),
    ClientProductItem(
        "pattes",
        "Pattes",
        "Abats & Divers",
        "U",
        "75 FCFA l'unité",
        { 75.0 }
    ),
    ClientProductItem(
        "foie",
        "Foie",
        "Abats & Divers",
        "kg",
        "1 000 FCFA le kg",
        { 1000.0 }
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientScreen(
    viewModel: FarmViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val sharedPrefs = remember { context.getSharedPreferences("FarmPrefs", Context.MODE_PRIVATE) }
    val savedName = remember { sharedPrefs.getString("client_name", "") ?: "" }
    val savedPhone = remember { sharedPrefs.getString("client_phone", "") ?: "" }
    val savedType = remember { sharedPrefs.getString("client_type", "Particulier") ?: "Particulier" }

    // Client Form State
    var clientName by remember { mutableStateOf(savedName) }
    var clientPhone by remember { mutableStateOf(savedPhone) }
    var clientType by remember { mutableStateOf(savedType) } // Dropdown: Particulier / Restaurant
    var deliveryLocation by remember { mutableStateOf("") }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    // Product quantities map (index to quantity)
    val quantities = remember { mutableStateMapOf<Int, Int>() }

    // Initialize quantities to 0
    LaunchedEffect(Unit) {
        clientProductsList.indices.forEach { index ->
            quantities[index] = 0
        }
    }

    // Success and Dialog state
    var showSuccessDialog by remember { mutableStateOf(false) }
    var generatedDeepLink by remember { mutableStateOf("") }
    var generatedWhatsAppMsg by remember { mutableStateOf("") }

    // Calculation values
    val totalAmount = clientProductsList.mapIndexed { index, product ->
        val qty = quantities[index] ?: 0
        qty * product.getUnitPrice(qty)
    }.sum()

    val totalItemsCount = clientProductsList.mapIndexed { index, _ ->
        quantities[index] ?: 0
    }.sum()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        FarmGreenPrimary.copy(alpha = 0.08f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Top App Bar/Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                }
                Text(
                    text = "Boutique en Ligne Client",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = FarmGreenPrimary
                )
                Icon(
                    Icons.Default.ShoppingBag,
                    contentDescription = null,
                    tint = FarmGreenPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scrollable Content
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Intro text
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = FarmGreenLight.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = FarmGreenDark)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Prise de commande simplifiée. Renseignez vos coordonnées, composez votre panier et validez.",
                                fontSize = 12.sp,
                                color = FarmGreenDark,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 1. Coordonnées Client Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = FarmGreenPrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Coordonnées de Livraison", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FarmGreenPrimary)
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            // Name & Surname
                            OutlinedTextField(
                                value = clientName,
                                onValueChange = { clientName = it },
                                label = { Text("Nom et Prénom", fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Type of Client (Dropdown)
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = clientType,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Vous êtes :", fontSize = 13.sp) },
                                    leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                    trailingIcon = { IconButton(onClick = { isDropdownExpanded = !isDropdownExpanded }) {
                                        Icon(if (isDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, contentDescription = null)
                                    }},
                                    modifier = Modifier.fillMaxWidth().clickable { isDropdownExpanded = !isDropdownExpanded },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                DropdownMenu(
                                    expanded = isDropdownExpanded,
                                    onDismissRequest = { isDropdownExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.85f)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Particulier") },
                                        onClick = { clientType = "Particulier"; isDropdownExpanded = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Restaurant") },
                                        onClick = { clientType = "Restaurant"; isDropdownExpanded = false }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Phone Number
                            OutlinedTextField(
                                value = clientPhone,
                                onValueChange = { clientPhone = it },
                                label = { Text("Numéro de Téléphone", fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Delivery location
                            OutlinedTextField(
                                value = deliveryLocation,
                                onValueChange = { deliveryLocation = it },
                                label = { Text("Lieu de Livraison", fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                // 2. Products List section header
                item {
                    Text(
                        text = "Nos Produits frais & Découpes",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                // Products list grouping
                val categories = clientProductsList.groupBy { it.category }
                categories.forEach { (catName, products) ->
                    item {
                        Text(
                            text = "✦ $catName",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = FarmGoldSecondary,
                            modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 2.dp)
                        )
                    }

                    itemsIndexed(products) { _, product ->
                        val globalIndex = clientProductsList.indexOf(product)
                        val qty = quantities[globalIndex] ?: 0
                        val currentUnitPrice = product.getUnitPrice(qty)
                        val itemSubtotal = qty * currentUnitPrice

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            border = if (qty > 0) BorderStroke(1.5.dp, FarmGreenPrimary.copy(alpha = 0.5f)) else null,
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Product details
                                Column(modifier = Modifier.weight(1.2f)) {
                                    Text(
                                        text = product.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = product.priceDesc,
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                    if (qty > 0) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Tarif: ${formatFCFA(currentUnitPrice)} / ${product.unitLabel} • Total: ${formatFCFA(itemSubtotal)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = FarmGreenPrimary
                                        )
                                    }
                                }

                                // Quantities Selector Button Controls
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Minus Button
                                    IconButton(
                                        onClick = {
                                            if (qty > 0) {
                                                quantities[globalIndex] = qty - 1
                                            }
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (qty > 0) FarmGreenPrimary.copy(alpha = 0.1f)
                                                else Color.LightGray.copy(alpha = 0.2f)
                                            )
                                    ) {
                                        Icon(
                                            Icons.Default.Remove,
                                            contentDescription = "Diminuer",
                                            modifier = Modifier.size(16.dp),
                                            tint = if (qty > 0) FarmGreenPrimary else Color.Gray
                                        )
                                    }

                                    // Counter Text
                                    Text(
                                        text = "$qty",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.widthIn(min = 20.dp),
                                        textAlign = TextAlign.Center
                                    )

                                    // Plus Button
                                    IconButton(
                                        onClick = {
                                            quantities[globalIndex] = qty + 1
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(FarmGreenPrimary.copy(alpha = 0.1f))
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Augmenter",
                                            modifier = Modifier.size(16.dp),
                                            tint = FarmGreenPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Navigation/Summary Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Commande", fontSize = 12.sp, color = Color.Gray)
                            Text(
                                text = formatFCFA(totalAmount),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = FarmGreenPrimary
                            )
                        }
                        Badge(
                            containerColor = FarmGoldSecondary,
                            contentColor = Color.Black,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text("$totalItemsCount articles", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (clientName.isBlank()) {
                                Toast.makeText(context, "Veuillez renseigner votre Nom et Prénom", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (clientPhone.isBlank()) {
                                Toast.makeText(context, "Veuillez renseigner votre numéro de téléphone", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (deliveryLocation.isBlank()) {
                                Toast.makeText(context, "Veuillez renseigner le lieu de livraison", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (totalAmount <= 0) {
                                Toast.makeText(context, "Votre panier est vide !", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            // 1. Simuler l'écriture directe dans l'application mère
                            clientProductsList.forEachIndexed { idx, prod ->
                                val qty = quantities[idx] ?: 0
                                if (qty > 0) {
                                    val finalUnitPrice = prod.getUnitPrice(qty)
                                    // Custom client name string format storing type & location info inside order
                                    viewModel.createOrder(
                                        clientName = "$clientName ($clientType - $deliveryLocation) - ${prod.name}",
                                        clientPhone = clientPhone,
                                        quantity = qty,
                                        unitPrice = finalUnitPrice,
                                        sellerName = "Client standalone",
                                        paymentStatus = "NON_PAYE",
                                        deliveryDate = System.currentTimeMillis() + 24L * 60 * 60 * 1000 // default 1 day
                                    )
                                }
                            }

                            // 2. Construire le texte du message de commande pour WhatsApp
                            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)
                            val orderDate = sdf.format(Date())
                            val messageBuilder = StringBuilder()
                            messageBuilder.append("*🍗 NOUVELLE COMMANDE - LES FERMES ABIDJANAISES*\n")
                            messageBuilder.append("---------------------------------------------------\n")
                            messageBuilder.append("📅 *Date:* $orderDate\n")
                            messageBuilder.append("👤 *Client:* $clientName ($clientType)\n")
                            messageBuilder.append("📞 *Tél:* $clientPhone\n")
                            messageBuilder.append("📍 *Lieu de livraison:* $deliveryLocation\n\n")
                            messageBuilder.append("*PRODUITS COMMANDÉS:*\n")

                            val itemDetailsUrlBuilder = StringBuilder()
                            clientProductsList.forEachIndexed { idx, prod ->
                                val qty = quantities[idx] ?: 0
                                if (qty > 0) {
                                    val up = prod.getUnitPrice(qty)
                                    val sub = qty * up
                                    messageBuilder.append("• *$qty* x ${prod.name} (")
                                    messageBuilder.append("${formatFCFA(up)} / ${prod.unitLabel}) -> *${formatFCFA(sub)}*\n")

                                    // For deep link param: name:qty:price
                                    val safeName = prod.name.replace(" ", "_").replace("(", "").replace(")", "")
                                    itemDetailsUrlBuilder.append("${safeName}:${qty}:${up.toInt()};")
                                }
                            }
                            messageBuilder.append("---------------------------------------------------\n")
                            messageBuilder.append("💰 *MONTANT TOTAL:* *${formatFCFA(totalAmount)}*\n")
                            messageBuilder.append("🚀 Merci pour votre confiance !")

                            generatedWhatsAppMsg = messageBuilder.toString()

                            // 3. Construire le Deep Link URI pour la communication locale d'APK à APK
                            // Scheme: lesfermesabidjanaises://order?name=...&phone=...&type=...&location=...&items=...
                            val safeItemsStr = Uri.encode(itemDetailsUrlBuilder.toString().removeSuffix(";"))
                            generatedDeepLink = "lesfermesabidjanaises://order?" +
                                    "name=${Uri.encode(clientName)}&" +
                                    "phone=${Uri.encode(clientPhone)}&" +
                                    "type=${Uri.encode(clientType)}&" +
                                    "location=${Uri.encode(deliveryLocation)}&" +
                                    "items=$safeItemsStr"

                            showSuccessDialog = true
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("VALIDER LA COMMANDE", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    // Success and Dev Integration Dialog
    if (showSuccessDialog) {
        Dialog(onDismissRequest = { showSuccessDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = FarmSuccessGreen,
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Commande Enregistrée !",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = FarmSuccessGreen,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Elle a été injectée dans l'application mère locale et est prête pour la transmission externe.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    // SECTION 1: TRANSMISSION VIA WHATSAPP (Different devices communication)
                    Text(
                        text = "📱 Transmission via WhatsApp",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = FarmGreenPrimary,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Text(
                        text = "Idéal si le client et le vendeur ont l'application sur des téléphones différents.",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.Start).padding(bottom = 6.dp)
                    )
                    Button(
                        onClick = {
                            sendWhatsAppInvoice(context, "+22507000000", generatedWhatsAppMsg) // fallback or default seller phone
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FarmSuccessGreen),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Envoyer via WhatsApp", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { showSuccessDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Fermer")
                    }
                }
            }
        }
    }
}
