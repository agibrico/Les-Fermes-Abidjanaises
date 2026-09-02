package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.FarmViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnerScreen(
    viewModel: FarmViewModel,
    onLogout: () -> Unit
) {
    val users by viewModel.allUsers.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()

    val totalSales = transactions.filter { it.isSale }.sumOf { it.amount }
    // Expenses related to feed & vaccines
    val saleExpenses = transactions.filter { it.type == "OUT" && (it.category == "Aliment" || it.category == "Vétérinaire") }.sumOf { it.amount }
    val netSalesProfit = totalSales - saleExpenses

    val partnersList = users.filter { it.role == "PARTENAIRE" }
    val partnerCount = partnersList.size + 1 // +1 for the Admin/Promoter
    val individualShare = if (partnerCount > 0) netSalesProfit / partnerCount else 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Espace Partenaire", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onLogout, modifier = Modifier.testTag("partner_logout")) {
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Dashboard Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = FarmGreenPrimary.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = FarmGreenPrimary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Bénéfices Globaux Répartissables", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        Text(formatFCFA(netSalesProfit), fontSize = 28.sp, fontWeight = FontWeight.Black, color = FarmSuccessGreen)
                    }
                }
            }

            // Financial Summary Breakdowns
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Rapport Financier Simplifié", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Chiffre d'affaires (Ventes) :", fontSize = 13.sp)
                            Text(formatFCFA(totalSales), fontWeight = FontWeight.Bold, color = FarmSuccessGreen)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Dépenses d'exploitation :", fontSize = 13.sp)
                            Text("-${formatFCFA(saleExpenses)}", fontWeight = FontWeight.Bold, color = FarmAlertRed)
                        }
                    }
                }
            }

            // Repartition Split Details
            item {
                Text(
                    text = "Répartition Générale (${partnerCount} Associés)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Votre part individuelle :", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(formatFCFA(individualShare), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = FarmSuccessGreen)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Note: Les bénéfices sont divisés à parts égales de manière transparente entre le promoteur et tous les associés de la ferme.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Détail par Associé",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            }

            // Admin Promoter Row
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Promoteur (Administrateur)", fontWeight = FontWeight.Bold)
                        Text(formatFCFA(individualShare), color = FarmSuccessGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Other partners list
            items(partnersList) { partner ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(partner.name, fontWeight = FontWeight.Bold)
                        Text(formatFCFA(individualShare), color = FarmSuccessGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
