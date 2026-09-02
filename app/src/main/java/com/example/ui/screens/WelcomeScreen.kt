package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.R
import com.example.data.entity.User
import com.example.ui.theme.*
import com.example.ui.viewmodel.FarmViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    viewModel: FarmViewModel,
    users: List<User>,
    onRoleSelected: (String) -> Unit
) {
    var selectedRoleForLogin by remember { mutableStateOf<String?>(null) }
    var selectedUser by remember { mutableStateOf<User?>(null) }
    var passwordInput by remember { mutableStateOf("") }
    var showErrorDialog by remember { mutableStateOf(false) }
    val loginError by viewModel.loginError.collectAsState()

    // Main entrance portal selection: false = shows 2 buttons, true = shows admin roles
    var showAdminPortal by remember { mutableStateOf(false) }
    var showClientRegistrationDialog by remember { mutableStateOf(false) }

    // Client registration form fields
    var clientRegisterType by remember { mutableStateOf("Particulier") } // "Particulier" or "Structure"
    var clientNameInput by remember { mutableStateOf("") }
    var clientStructureInput by remember { mutableStateOf("") }
    var clientPhoneInput by remember { mutableStateOf("") }

    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("FarmPrefs", Context.MODE_PRIVATE) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Section with Logo
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                // Application Logo - using the generated img_farm_logo
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(4.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_farm_logo),
                        contentDescription = "Logo Les Fermes Abidjanaises",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "LES FERMES ABIDJANAISES",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "Gestion Agro-Pastorale Intelligente",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }

            // Central Interactive Portal Panel
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!showAdminPortal) {
                    // ==========================================
                    // TWO BUTTONS PORTAL SELECTION (MAIN STARTUP SCREEN)
                    // ==========================================
                    Text(
                        text = "Bienvenue ! Choisissez votre espace",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. CLIENT / BOUTIQUE PORTAL BUTTON
                        Button(
                            onClick = {
                                val isRegistered = sharedPrefs.getBoolean("is_client_registered", false)
                                if (isRegistered) {
                                    onRoleSelected("CLIENT")
                                } else {
                                    showClientRegistrationDialog = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(68.dp)
                                .testTag("client_portal_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FarmGoldSecondary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingBag,
                                    contentDescription = null,
                                    modifier = Modifier.size(26.dp),
                                    tint = Color.Black
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "ESPACE CLIENT / BOUTIQUE",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        // 2. ADMINISTRATION PORTAL BUTTON
                        Button(
                            onClick = { showAdminPortal = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(68.dp)
                                .testTag("admin_portal_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FarmGreenPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    modifier = Modifier.size(26.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "ESPACE ADMINISTRATION",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                } else {
                    // ==========================================
                    // ADMINISTRATION ROLES SELECTION PANEL
                    // ==========================================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showAdminPortal = false }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Retour", tint = FarmGreenPrimary)
                        }
                        Text(
                            text = "Portails d'administration",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = FarmGreenPrimary
                        )
                        Spacer(modifier = Modifier.size(48.dp)) // spacer to balance
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val roles = listOf(
                            Triple("ADMINISTRATEUR", Icons.Default.AdminPanelSettings, FarmGreenPrimary),
                            Triple("PARTENAIRE", Icons.Default.Group, FarmGoldSecondary),
                            Triple("VOLAILLER", Icons.Default.Agriculture, FarmSuccessGreen),
                            Triple("VENDEUR", Icons.Default.Storefront, FarmPendingOrange)
                        )

                        roles.forEach { (role, icon, color) ->
                            Button(
                                onClick = {
                                    selectedRoleForLogin = role
                                    val roleUsers = users.filter { it.role == role }
                                    selectedUser = roleUsers.firstOrNull()
                                    passwordInput = ""
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("role_btn_${role.lowercase()}"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = color,
                                    contentColor = if (color == FarmGoldSecondary) Color.Black else Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 2.dp,
                                    pressedElevation = 1.dp
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = role,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Footer info
        Text(
            text = "Version 1.0 • Côte d'Ivoire",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
        )

        // ==========================================
        // CLIENT REGISTRATION PROFILE SETUP DIALOG
        // ==========================================
        if (showClientRegistrationDialog) {
            Dialog(onDismissRequest = { showClientRegistrationDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Créer votre Profil Client",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = FarmGreenPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Veuillez renseigner vos coordonnées pour accéder à l'interface boutique.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))

                        // Client Type Selection (Particulier vs Structure)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("Particulier", "Structure").forEach { type ->
                                val isSelected = clientRegisterType == type
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) FarmGreenPrimary else Color.Transparent)
                                        .clickable { clientRegisterType = type }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (type == "Structure") "Restaurant / Vendeur" else "Particulier",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isSelected) Color.White else Color.DarkGray
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Dynamic Name Input
                        if (clientRegisterType == "Particulier") {
                            OutlinedTextField(
                                value = clientNameInput,
                                onValueChange = { clientNameInput = it },
                                label = { Text("Votre Nom complet", fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        } else {
                            OutlinedTextField(
                                value = clientStructureInput,
                                onValueChange = { clientStructureInput = it },
                                label = { Text("Nom de la Structure", fontSize = 13.sp) },
                                placeholder = { Text("Ex: Maquis Le Repère, Restaurant...") },
                                leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Phone Number
                        OutlinedTextField(
                            value = clientPhoneInput,
                            onValueChange = { clientPhoneInput = it },
                            label = { Text("Numéro de Téléphone", fontSize = 13.sp) },
                            placeholder = { Text("Ex: +225 07070707") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showClientRegistrationDialog = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Annuler")
                            }

                            Button(
                                onClick = {
                                    val finalName = if (clientRegisterType == "Particulier") clientNameInput.trim() else clientStructureInput.trim()
                                    val phone = clientPhoneInput.trim()

                                    if (finalName.isEmpty()) {
                                        Toast.makeText(context, "Veuillez renseigner le nom.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (phone.isEmpty()) {
                                        Toast.makeText(context, "Veuillez renseigner votre numéro de téléphone.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    // Save in SharedPreferences
                                    sharedPrefs.edit()
                                        .putBoolean("is_client_registered", true)
                                        .putString("client_name", finalName)
                                        .putString("client_phone", phone)
                                        .putString("client_type", clientRegisterType)
                                        .apply()

                                    // Automatically register in local SQLite Client User Database
                                    viewModel.addClient(name = finalName, phone = phone, type = clientRegisterType)

                                    Toast.makeText(context, "Inscription enregistrée !", Toast.LENGTH_SHORT).show()
                                    
                                    showClientRegistrationDialog = false
                                    onRoleSelected("CLIENT")
                                },
                                modifier = Modifier.weight(1.2f).testTag("client_register_submit"),
                                colors = ButtonDefaults.buttonColors(containerColor = FarmGreenPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Valider", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Login Password Dialog
        selectedRoleForLogin?.let { role ->
            val roleUsers = users.filter { it.role == role }

            Dialog(onDismissRequest = { selectedRoleForLogin = null }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Title
                        Text(
                            text = "Connexion $role",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // User selection dropdown if there are multiple users, otherwise show static name
                        if (roleUsers.size > 1) {
                            var expanded by remember { mutableStateOf(false) }
                            Text(
                                text = "Choisir un compte :",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { expanded = true }
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedUser?.name ?: "Sélectionner un utilisateur",
                                        fontWeight = FontWeight.Medium
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    roleUsers.forEach { u ->
                                        DropdownMenuItem(
                                            text = { Text(u.name) },
                                            onClick = {
                                                selectedUser = u
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            // Only 1 user or fallback
                            val nameToDisplay = selectedUser?.name ?: if (role == "ADMINISTRATEUR") "Administrateur" else role
                            Text(
                                text = "Utilisateur : $nameToDisplay",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Password Input
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Code d'accès") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        if (loginError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = loginError ?: "",
                                color = FarmAlertRed,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Actions Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { selectedRoleForLogin = null },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Annuler")
                            }

                            Button(
                                onClick = {
                                    val userName = selectedUser?.name ?: if (role == "ADMINISTRATEUR") "Administrateur" else role
                                    viewModel.login(userName, role, passwordInput) {
                                        selectedRoleForLogin = null
                                        onRoleSelected(role)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("login_submit_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Valider")
                            }
                        }
                    }
                }
            }
        }
    }
}
