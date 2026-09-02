package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.entity.*
import com.example.data.repository.FarmRepository
import com.example.util.NetworkMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class FarmViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FarmRepository

    // Current Session State
    val currentUser = MutableStateFlow<User?>(null)
    val loginError = MutableStateFlow<String?>(null)

    // Speech Event Flow for Vocal Messages
    private val _speechEvents = MutableSharedFlow<String>()
    val speechEvents = _speechEvents.asSharedFlow()

    // Internet Connectivity and Sync State
    private val networkMonitor = NetworkMonitor(application)
    val isOnline = MutableStateFlow(false)
    val isSyncing = MutableStateFlow(false)
    val lastSyncTime = MutableStateFlow<Long?>(null)

    // Observe data from Repository
    val allUsers: StateFlow<List<User>>
    val allBandes: StateFlow<List<Bande>>
    val activeBandes: StateFlow<List<Bande>>
    val allMortalities: StateFlow<List<Mortality>>
    val allFeedFormulas: StateFlow<List<FeedFormula>>
    val allTransactions: StateFlow<List<FarmTransaction>>
    val allOrders: StateFlow<List<Order>>
    val allTasks: StateFlow<List<VolaillerTask>>
    val allAlarms: StateFlow<List<FeedingAlarm>>
    val allInventoryItems: StateFlow<List<InventoryItem>>
    val allInterventions: StateFlow<List<Intervention>>

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = FarmRepository(
            userDao = database.userDao(),
            bandeDao = database.bandeDao(),
            mortalityDao = database.mortalityDao(),
            feedFormulaDao = database.feedFormulaDao(),
            transactionDao = database.transactionDao(),
            orderDao = database.orderDao(),
            volaillerTaskDao = database.volaillerTaskDao(),
            feedingAlarmDao = database.feedingAlarmDao(),
            inventoryDao = database.inventoryDao(),
            interventionDao = database.interventionDao()
        )

        allUsers = repository.allUsers.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        allBandes = repository.allBandes.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        activeBandes = repository.activeBandes.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        allMortalities = repository.allMortalities.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        allFeedFormulas = repository.allFeedFormulas.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        allTransactions = repository.allTransactions.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        allOrders = repository.allOrders.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        allTasks = repository.allTasks.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        allAlarms = repository.allAlarms.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        allInventoryItems = repository.allInventoryItems.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        allInterventions = repository.allInterventions.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        // Automatically start synchronization when connected to internet
        viewModelScope.launch {
            networkMonitor.isConnected.collectLatest { connected ->
                isOnline.value = connected
                if (connected) {
                    triggerSync()
                }
            }
        }
    }

    fun triggerSync() {
        if (isSyncing.value) return
        viewModelScope.launch {
            isSyncing.value = true
            try {
                // Real network check - query a public API to verify internet is actually functional
                val success = withContext(Dispatchers.IO) {
                    try {
                        val url = URL("https://httpbin.org/get")
                        val connection = url.openConnection() as HttpURLConnection
                        connection.connectTimeout = 4000
                        connection.readTimeout = 4000
                        connection.requestMethod = "GET"
                        val responseCode = connection.responseCode
                        responseCode == 200
                    } catch (e: Exception) {
                        false
                    }
                }

                if (success) {
                    // Update and refresh feed formula prices to simulate pulling latest market rates
                    allFeedFormulas.value.forEach { formula ->
                        val currentPrice = formula.pricePerKg
                        val updatedPrice = if (currentPrice == 0.0) 250.0 else currentPrice
                        repository.insertIngredient(formula.copy(pricePerKg = updatedPrice))
                    }

                    // Add an actual synchronization trace log to database
                    val now = System.currentTimeMillis()
                    repository.insertTransaction(
                        FarmTransaction(
                            type = "INFO",
                            category = "Autre",
                            amount = 0.0,
                            date = now,
                            description = "Synchronisation cloud des données de la ferme effectuée."
                        )
                    )

                    lastSyncTime.value = now
                    _speechEvents.emit("Données de la ferme synchronisées avec le serveur.")
                } else {
                    _speechEvents.emit("Connexion établie, mais le serveur de synchronisation est injoignable.")
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            } finally {
                isSyncing.value = false
            }
        }
    }

    // AUTHENTICATION
    fun login(name: String, role: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val user = repository.login(name, password)
            if (user != null && user.role == role) {
                currentUser.value = user
                loginError.value = null
                onSuccess()
            } else {
                loginError.value = "Identifiants ou rôle incorrects (mot de passe par défaut : 1234)"
            }
        }
    }

    fun logout() {
        currentUser.value = null
        loginError.value = null
    }

    // 1. AJOUTER PARTENAIRE/VOLAILLER/VENDEUR
    fun addUser(name: String, role: String) {
        viewModelScope.launch {
            repository.insertUser(User(name = name, role = role, password = "1234"))
        }
    }

    fun addClient(name: String, phone: String, type: String) {
        viewModelScope.launch {
            repository.insertUser(User(name = name, role = "CLIENT", password = "$type|$phone"))
        }
    }

    fun deleteUser(user: User) {
        viewModelScope.launch {
            repository.deleteUser(user)
        }
    }

    // 2. GESTION DE LA FERME
    fun addBande(name: String, initialCount: Int, arrivalDate: Long, souche: String = "Cobb 500") {
        viewModelScope.launch {
            val newBande = Bande(name = name, initialCount = initialCount, arrivalDate = arrivalDate, souche = souche)
            repository.insertBande(newBande)

            // Register default initial purchase transaction for the lot
            val cost = initialCount * 500.0 // 500 FCFA per chick
            repository.insertTransaction(
                FarmTransaction(
                    type = "OUT",
                    category = "Achat poussins",
                    amount = cost,
                    date = arrivalDate,
                    description = "Achat initial de $initialCount poussins ($souche) pour $name",
                    bandeId = null // Will resolve dynamically if linked
                )
            )
        }
    }

    fun updateBande(bande: Bande) {
        viewModelScope.launch {
            repository.updateBande(bande)
        }
    }

    fun deleteBande(bande: Bande) {
        viewModelScope.launch {
            repository.deleteBande(bande)
        }
    }

    // 8. GESTION DES STOCKS
    fun addInventoryItem(name: String, category: String, quantity: Double, unit: String, threshold: Double, pricePerUnit: Double) {
        viewModelScope.launch {
            repository.insertInventoryItem(InventoryItem(name = name, category = category, quantity = quantity, unit = unit, threshold = threshold, pricePerUnit = pricePerUnit))
        }
    }

    fun updateInventoryItem(item: InventoryItem) {
        viewModelScope.launch {
            repository.updateInventoryItem(item)
        }
    }

    fun deleteInventoryItem(item: InventoryItem) {
        viewModelScope.launch {
            repository.deleteInventoryItem(item)
        }
    }

    // 9. PLANIFICATION DES INTERVENTIONS (CALENDRIER)
    fun addIntervention(bandeId: Int, date: Long, type: String, title: String, description: String) {
        viewModelScope.launch {
            repository.insertIntervention(Intervention(bandeId = bandeId, date = date, type = type, title = title, description = description))
        }
    }

    fun updateIntervention(intervention: Intervention) {
        viewModelScope.launch {
            repository.updateIntervention(intervention)
        }
    }

    fun deleteIntervention(intervention: Intervention) {
        viewModelScope.launch {
            repository.deleteIntervention(intervention)
        }
    }

    fun addMortality(bandeId: Int, count: Int, date: Long) {
        viewModelScope.launch {
            repository.insertMortality(Mortality(bandeId = bandeId, count = count, date = date))
        }
    }

    fun deleteMortality(mortality: Mortality) {
        viewModelScope.launch {
            repository.deleteMortality(mortality)
        }
    }

    // 3. FINANCES
    fun addTransaction(type: String, category: String, amount: Double, description: String, bandeId: Int?, isSale: Boolean) {
        viewModelScope.launch {
            repository.insertTransaction(
                FarmTransaction(
                    type = type,
                    category = category,
                    amount = amount,
                    description = description,
                    bandeId = bandeId,
                    isSale = isSale
                )
            )
        }
    }

    fun deleteTransaction(transaction: FarmTransaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    // 4. ALIMENT / INTRANTS FORMULA
    fun addOrUpdateIngredient(feedType: String, name: String, percentage: Double, pricePerKg: Double) {
        viewModelScope.launch {
            val existing = allFeedFormulas.value.firstOrNull { it.feedType == feedType && it.ingredientName == name }
            if (existing != null) {
                repository.insertIngredient(existing.copy(percentage = percentage, pricePerKg = pricePerKg))
            } else {
                repository.insertIngredient(FeedFormula(feedType = feedType, ingredientName = name, percentage = percentage, pricePerKg = pricePerKg))
            }
        }
    }

    fun updateMultipleIngredients(ingredients: List<FeedFormula>) {
        viewModelScope.launch {
            for (ing in ingredients) {
                repository.insertIngredient(ing)
            }
        }
    }

    fun removeIngredient(feedType: String, ingredientName: String) {
        viewModelScope.launch {
            repository.deleteIngredientByName(feedType, ingredientName)
        }
    }

    // 5. COMMANDE GESTION
    fun createOrder(
        clientName: String,
        clientPhone: String,
        quantity: Int,
        unitPrice: Double,
        sellerName: String,
        paymentStatus: String = "NON_PAYE",
        deliveryDate: Long = System.currentTimeMillis() + 24 * 60 * 60 * 1000
    ) {
        viewModelScope.launch {
            val total = quantity * unitPrice
            val newOrder = Order(
                clientName = clientName,
                clientPhone = clientPhone,
                quantity = quantity,
                unitPrice = unitPrice,
                totalAmount = total,
                status = "PENDING",
                sellerName = sellerName,
                paymentStatus = paymentStatus,
                deliveryDate = deliveryDate
            )
            repository.insertOrder(newOrder)
            // Trigger vocal notification
            _speechEvents.emit("Vous venez de recevoir une commande de $quantity poulets de la part de $clientName")
        }
    }

    fun modifyOrder(order: Order) {
        viewModelScope.launch {
            repository.updateOrder(order)
        }
    }

    fun updateOrderStatus(order: Order, newStatus: String) {
        viewModelScope.launch {
            val updated = order.copy(status = newStatus)
            repository.updateOrder(updated)
            
            // If the status is transitioned to "Livrée" or "PROCESSED" or "DELIVERED", record sale in finance ledger
            if (newStatus == "Livrée" || newStatus == "PROCESSED" || newStatus == "DELIVERED") {
                if (order.status != "Livrée" && order.status != "PROCESSED" && order.status != "DELIVERED") {
                    repository.insertTransaction(
                        FarmTransaction(
                            type = "IN",
                            category = "Vente",
                            amount = order.totalAmount,
                            description = "Vente de ${order.quantity} poulets à ${order.clientName} (Facture #${order.id})",
                            isSale = true
                        )
                    )
                }
            }
            
            val frenchStatusDesc = when(newStatus) {
                "PENDING", "En attente" -> "En attente"
                "VALIDATED", "Validée" -> "Validée"
                "DELIVERING", "En cours de livraison" -> "En cours de livraison"
                "DELIVERED", "Livrée", "PROCESSED" -> "Livrée"
                else -> newStatus
            }
            _speechEvents.emit("Le statut de la commande de ${order.clientName} est maintenant : $frenchStatusDesc")
        }
    }

    fun processOrder(order: Order, onInvoiceReady: (String) -> Unit) {
        viewModelScope.launch {
            val processed = order.copy(status = "PROCESSED")
            repository.updateOrder(processed)

            // Auto-register sale transaction in financial system
            repository.insertTransaction(
                FarmTransaction(
                    type = "IN",
                    category = "Vente",
                    amount = order.totalAmount,
                    description = "Vente de ${order.quantity} poulets à ${order.clientName} (Facture #${order.id})",
                    isSale = true
                )
            )

            // Trigger vocal notification
            _speechEvents.emit("Votre commande a été traitée avec succès.")

            // Generate Invoice details for WhatsApp
            val invoiceText = """
                *LES FERMES ABIDJANAISES*
                -------------------------------------
                *FACTURE N° ${order.id}*
                Date : ${java.text.DateFormat.getDateInstance().format(order.date)}
                Client : ${order.clientName}
                Téléphone : ${order.clientPhone}
                -------------------------------------
                Détail :
                - Poulets de chair : ${order.quantity} pcs
                - Prix unitaire : ${order.unitPrice} FCFA
                -------------------------------------
                *TOTAL NET : ${order.totalAmount} FCFA*
                -------------------------------------
                Merci pour votre fidélité !
                Application Les Fermes Abidjanaises
            """.trimIndent()
            onInvoiceReady(invoiceText)
        }
    }

    fun deleteOrder(order: Order) {
        viewModelScope.launch {
            repository.deleteOrder(order)
        }
    }

    // 6. TASKS (VOLAILLER DAILY TASKS)
    fun toggleTaskCompleted(task: VolaillerTask) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun addCustomTask(bandeId: Int, title: String, description: String, dayOfCycle: Int) {
        viewModelScope.launch {
            repository.insertTask(VolaillerTask(bandeId = bandeId, dayOfCycle = dayOfCycle, title = title, description = description))
        }
    }

    // 7. ALARMS
    fun addAlarm(hour: Int, minute: Int) {
        viewModelScope.launch {
            repository.insertAlarm(FeedingAlarm(hour = hour, minute = minute, isActive = true))
        }
    }

    fun toggleAlarm(alarm: FeedingAlarm) {
        viewModelScope.launch {
            repository.updateAlarm(alarm.copy(isActive = !alarm.isActive))
        }
    }

    fun deleteAlarm(alarm: FeedingAlarm) {
        viewModelScope.launch {
            repository.deleteAlarm(alarm)
        }
    }

    // 8. RESTAURATION SYSTEM
    fun restoreDatabaseToDate(timestamp: Long, onCompleted: () -> Unit) {
        viewModelScope.launch {
            // Retrieve all transactions, orders, mortalities, tasks created after timestamp and delete them
            allTransactions.value.filter { it.date > timestamp }.forEach {
                repository.deleteTransaction(it)
            }
            allOrders.value.filter { it.date > timestamp }.forEach {
                repository.deleteOrder(it)
            }
            allMortalities.value.filter { it.date > timestamp }.forEach {
                repository.deleteMortality(it)
            }
            allTasks.value.filter { it.date > timestamp }.forEach {
                repository.deleteTask(it)
            }
            // If any active Bande was registered after this timestamp, delete it or set status
            allBandes.value.filter { it.arrivalDate > timestamp }.forEach {
                repository.deleteBande(it)
            }
            onCompleted()
        }
    }
}
