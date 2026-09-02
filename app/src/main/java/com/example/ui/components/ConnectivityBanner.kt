package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.FarmViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ConnectivityBanner(
    viewModel: FarmViewModel,
    modifier: Modifier = Modifier
) {
    val isOnline by viewModel.isOnline.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()

    AnimatedVisibility(
        visible = true,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            color = when {
                !isOnline -> FarmPendingOrange.copy(alpha = 0.1f)
                isSyncing -> FarmGreenPrimary.copy(alpha = 0.1f)
                else -> FarmSuccessGreen.copy(alpha = 0.08f)
            },
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = when {
                    !isOnline -> FarmPendingOrange.copy(alpha = 0.3f)
                    isSyncing -> FarmGreenPrimary.copy(alpha = 0.3f)
                    else -> FarmSuccessGreen.copy(alpha = 0.2f)
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = when {
                            !isOnline -> Icons.Default.Warning
                            isSyncing -> Icons.Default.Refresh
                            else -> Icons.Default.CheckCircle
                        },
                        contentDescription = "Status de connexion",
                        tint = when {
                            !isOnline -> FarmPendingOrange
                            isSyncing -> FarmGreenPrimary
                            else -> FarmSuccessGreen
                        },
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = when {
                                !isOnline -> "Mode Hors-ligne"
                                isSyncing -> "Synchronisation en cours..."
                                else -> "Données actualisées"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                !isOnline -> FarmPendingOrange
                                isSyncing -> FarmGreenPrimary
                                else -> FarmSuccessGreen
                            }
                        )

                        Text(
                            text = when {
                                !isOnline -> "Modifications locales sauvegardées."
                                isSyncing -> "Récupération des prix du marché d'Abidjan..."
                                else -> {
                                    val timeStr = lastSyncTime?.let {
                                        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                        sdf.format(Date(it))
                                    } ?: "Récemment"
                                    "Dernière synchronisation : $timeStr"
                                }
                            },
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                if (isOnline && !isSyncing) {
                    IconButton(
                        onClick = { viewModel.triggerSync() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualiser les données",
                            tint = FarmGreenPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
