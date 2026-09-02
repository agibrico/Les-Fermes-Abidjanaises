package com.example.data.dao

import androidx.room.*
import com.example.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE role = :role")
    fun getUsersByRole(role: String): Flow<List<User>>

    @Query("SELECT * FROM users WHERE name = :name AND password = :password LIMIT 1")
    suspend fun login(name: String, password: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)
}

@Dao
interface BandeDao {
    @Query("SELECT * FROM bandes ORDER BY arrivalDate DESC")
    fun getAllBandes(): Flow<List<Bande>>

    @Query("SELECT * FROM bandes WHERE status = 'ACTIVE' ORDER BY arrivalDate DESC")
    fun getActiveBandes(): Flow<List<Bande>>

    @Query("SELECT * FROM bandes WHERE id = :id LIMIT 1")
    suspend fun getBandeById(id: Int): Bande?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBande(bande: Bande)

    @Update
    suspend fun updateBande(bande: Bande)

    @Delete
    suspend fun deleteBande(bande: Bande)
}

@Dao
interface MortalityDao {
    @Query("SELECT * FROM mortalities ORDER BY date DESC")
    fun getAllMortalities(): Flow<List<Mortality>>

    @Query("SELECT * FROM mortalities WHERE bandeId = :bandeId ORDER BY date DESC")
    fun getMortalitiesForBande(bandeId: Int): Flow<List<Mortality>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMortality(mortality: Mortality)

    @Delete
    suspend fun deleteMortality(mortality: Mortality)
}

@Dao
interface FeedFormulaDao {
    @Query("SELECT * FROM feed_formulas")
    fun getAllFeedFormulas(): Flow<List<FeedFormula>>

    @Query("SELECT * FROM feed_formulas WHERE feedType = :feedType")
    fun getFormulaByType(feedType: String): Flow<List<FeedFormula>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredient(formula: FeedFormula)

    @Update
    suspend fun updateIngredient(formula: FeedFormula)

    @Delete
    suspend fun deleteIngredient(formula: FeedFormula)

    @Query("DELETE FROM feed_formulas WHERE feedType = :feedType AND ingredientName = :ingredientName")
    suspend fun deleteIngredientByName(feedType: String, ingredientName: String)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<FarmTransaction>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    fun getTransactionsByType(type: String): Flow<List<FarmTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: FarmTransaction)

    @Delete
    suspend fun deleteTransaction(transaction: FarmTransaction)
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY date DESC")
    fun getAllOrders(): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE status = :status ORDER BY date DESC")
    fun getOrdersByStatus(status: String): Flow<List<Order>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order)

    @Update
    suspend fun updateOrder(order: Order)

    @Delete
    suspend fun deleteOrder(order: Order)
}

@Dao
interface VolaillerTaskDao {
    @Query("SELECT * FROM volailler_tasks ORDER BY date DESC")
    fun getAllTasks(): Flow<List<VolaillerTask>>

    @Query("SELECT * FROM volailler_tasks WHERE bandeId = :bandeId ORDER BY dayOfCycle ASC")
    fun getTasksForBande(bandeId: Int): Flow<List<VolaillerTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: VolaillerTask)

    @Update
    suspend fun updateTask(task: VolaillerTask)

    @Delete
    suspend fun deleteTask(task: VolaillerTask)
}

@Dao
interface FeedingAlarmDao {
    @Query("SELECT * FROM feeding_alarms ORDER BY hour ASC, minute ASC")
    fun getAllAlarms(): Flow<List<FeedingAlarm>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: FeedingAlarm)

    @Update
    suspend fun updateAlarm(alarm: FeedingAlarm)

    @Delete
    suspend fun deleteAlarm(alarm: FeedingAlarm)
}

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items ORDER BY category ASC, name ASC")
    fun getAllInventoryItems(): Flow<List<InventoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItem(item: InventoryItem)

    @Update
    suspend fun updateInventoryItem(item: InventoryItem)

    @Delete
    suspend fun deleteInventoryItem(item: InventoryItem)
}

@Dao
interface InterventionDao {
    @Query("SELECT * FROM interventions ORDER BY date ASC")
    fun getAllInterventions(): Flow<List<Intervention>>

    @Query("SELECT * FROM interventions WHERE bandeId = :bandeId ORDER BY date ASC")
    fun getInterventionsForBande(bandeId: Int): Flow<List<Intervention>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntervention(intervention: Intervention)

    @Update
    suspend fun updateIntervention(intervention: Intervention)

    @Delete
    suspend fun deleteIntervention(intervention: Intervention)
}
