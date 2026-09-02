package com.example.data.repository

import com.example.data.dao.*
import com.example.data.entity.*
import kotlinx.coroutines.flow.Flow

class FarmRepository(
    private val userDao: UserDao,
    private val bandeDao: BandeDao,
    private val mortalityDao: MortalityDao,
    private val feedFormulaDao: FeedFormulaDao,
    private val transactionDao: TransactionDao,
    private val orderDao: OrderDao,
    private val volaillerTaskDao: VolaillerTaskDao,
    private val feedingAlarmDao: FeedingAlarmDao,
    private val inventoryDao: InventoryDao,
    private val interventionDao: InterventionDao
) {
    val allUsers: Flow<List<User>> = userDao.getAllUsers()
    val allBandes: Flow<List<Bande>> = bandeDao.getAllBandes()
    val activeBandes: Flow<List<Bande>> = bandeDao.getActiveBandes()
    val allMortalities: Flow<List<Mortality>> = mortalityDao.getAllMortalities()
    val allFeedFormulas: Flow<List<FeedFormula>> = feedFormulaDao.getAllFeedFormulas()
    val allTransactions: Flow<List<FarmTransaction>> = transactionDao.getAllTransactions()
    val allOrders: Flow<List<Order>> = orderDao.getAllOrders()
    val allTasks: Flow<List<VolaillerTask>> = volaillerTaskDao.getAllTasks()
    val allAlarms: Flow<List<FeedingAlarm>> = feedingAlarmDao.getAllAlarms()
    val allInventoryItems: Flow<List<InventoryItem>> = inventoryDao.getAllInventoryItems()
    val allInterventions: Flow<List<Intervention>> = interventionDao.getAllInterventions()

    fun getMortalitiesForBande(bandeId: Int): Flow<List<Mortality>> = mortalityDao.getMortalitiesForBande(bandeId)
    fun getFormulaByType(feedType: String): Flow<List<FeedFormula>> = feedFormulaDao.getFormulaByType(feedType)
    fun getTasksForBande(bandeId: Int): Flow<List<VolaillerTask>> = volaillerTaskDao.getTasksForBande(bandeId)
    fun getInterventionsForBande(bandeId: Int): Flow<List<Intervention>> = interventionDao.getInterventionsForBande(bandeId)

    suspend fun login(name: String, password: String): User? = userDao.login(name, password)

    suspend fun insertUser(user: User) = userDao.insertUser(user)
    suspend fun deleteUser(user: User) = userDao.deleteUser(user)

    suspend fun insertBande(bande: Bande) = bandeDao.insertBande(bande)
    suspend fun updateBande(bande: Bande) = bandeDao.updateBande(bande)
    suspend fun deleteBande(bande: Bande) = bandeDao.deleteBande(bande)

    suspend fun insertMortality(mortality: Mortality) = mortalityDao.insertMortality(mortality)
    suspend fun deleteMortality(mortality: Mortality) = mortalityDao.deleteMortality(mortality)

    suspend fun insertIngredient(formula: FeedFormula) = feedFormulaDao.insertIngredient(formula)
    suspend fun updateIngredient(formula: FeedFormula) = feedFormulaDao.updateIngredient(formula)
    suspend fun deleteIngredient(formula: FeedFormula) = feedFormulaDao.deleteIngredient(formula)
    suspend fun deleteIngredientByName(feedType: String, ingredientName: String) = 
        feedFormulaDao.deleteIngredientByName(feedType, ingredientName)

    suspend fun insertTransaction(transaction: FarmTransaction) = transactionDao.insertTransaction(transaction)
    suspend fun deleteTransaction(transaction: FarmTransaction) = transactionDao.deleteTransaction(transaction)

    suspend fun insertOrder(order: Order) = orderDao.insertOrder(order)
    suspend fun updateOrder(order: Order) = orderDao.updateOrder(order)
    suspend fun deleteOrder(order: Order) = orderDao.deleteOrder(order)

    suspend fun insertTask(task: VolaillerTask) = volaillerTaskDao.insertTask(task)
    suspend fun updateTask(task: VolaillerTask) = volaillerTaskDao.updateTask(task)
    suspend fun deleteTask(task: VolaillerTask) = volaillerTaskDao.deleteTask(task)

    suspend fun insertAlarm(alarm: FeedingAlarm) = feedingAlarmDao.insertAlarm(alarm)
    suspend fun updateAlarm(alarm: FeedingAlarm) = feedingAlarmDao.updateAlarm(alarm)
    suspend fun deleteAlarm(alarm: FeedingAlarm) = feedingAlarmDao.deleteAlarm(alarm)

    suspend fun insertInventoryItem(item: InventoryItem) = inventoryDao.insertInventoryItem(item)
    suspend fun updateInventoryItem(item: InventoryItem) = inventoryDao.updateInventoryItem(item)
    suspend fun deleteInventoryItem(item: InventoryItem) = inventoryDao.deleteInventoryItem(item)

    suspend fun insertIntervention(intervention: Intervention) = interventionDao.insertIntervention(intervention)
    suspend fun updateIntervention(intervention: Intervention) = interventionDao.updateIntervention(intervention)
    suspend fun deleteIntervention(intervention: Intervention) = interventionDao.deleteIntervention(intervention)

    // RESTORE DATABASE TO ANTERIOR DATE
    suspend fun restoreToDate(timestamp: Long) {
        // We will remove all transactions, orders, mortalities and tasks created after the timestamp.
        // Also if any Bande was created after the timestamp, we can delete or change it back.
        // Since we are offline, let's fetch lists and delete items whose date is > timestamp.
        
        // Note: For simplicity, the Dao doesn't have a direct query for time deletion,
        // but we can query them or do standard cleanups. Let's do a complete restoration
        // where we filter items newer than selected date. Let's let the DB layer handle it
        // or do it in Kotlin! Doing it in Kotlin is safe and reliable.
    }
}
