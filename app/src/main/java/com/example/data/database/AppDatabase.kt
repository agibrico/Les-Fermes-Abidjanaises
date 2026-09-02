package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.*
import com.example.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        User::class,
        Bande::class,
        Mortality::class,
        FeedFormula::class,
        FarmTransaction::class,
        Order::class,
        VolaillerTask::class,
        FeedingAlarm::class,
        InventoryItem::class,
        Intervention::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun bandeDao(): BandeDao
    abstract fun mortalityDao(): MortalityDao
    abstract fun feedFormulaDao(): FeedFormulaDao
    abstract fun transactionDao(): TransactionDao
    abstract fun orderDao(): OrderDao
    abstract fun volaillerTaskDao(): VolaillerTaskDao
    abstract fun feedingAlarmDao(): FeedingAlarmDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun interventionDao(): InterventionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "farm_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database)
                }
            }
        }

        suspend fun populateDatabase(db: AppDatabase) {
            // 1. Initial Users
            val userDao = db.userDao()
            userDao.insertUser(User(name = "Administrateur", role = "ADMINISTRATEUR", password = "1234"))
            userDao.insertUser(User(name = "Partenaire", role = "PARTENAIRE", password = "1234"))
            userDao.insertUser(User(name = "Volailler", role = "VOLAILLER", password = "1234"))
            userDao.insertUser(User(name = "Vendeur", role = "VENDEUR", password = "1234"))

            // 2. Initial Bandes (arrival dates in past to simulate different ages)
            val bandeDao = db.bandeDao()
            val now = System.currentTimeMillis()
            val tenDaysMs = 10L * 24 * 60 * 60 * 1000
            val fifteenDaysMs = 15L * 24 * 60 * 60 * 1000
            val twentyFiveDaysMs = 25L * 24 * 60 * 60 * 1000

            bandeDao.insertBande(Bande(id = 1, name = "Lot A - Finition (J25)", arrivalDate = now - twentyFiveDaysMs, initialCount = 500))
            bandeDao.insertBande(Bande(id = 2, name = "Lot B - Croissance (J15)", arrivalDate = now - fifteenDaysMs, initialCount = 400))
            bandeDao.insertBande(Bande(id = 3, name = "Lot C - Prédémarrage (J5)", arrivalDate = now - tenDaysMs + (5 * 24 * 60 * 60 * 1000), initialCount = 600)) // arrived 5 days ago

            // 3. Initial Mortalities
            val mortalityDao = db.mortalityDao()
            mortalityDao.insertMortality(Mortality(bandeId = 1, count = 2, date = now - twentyFiveDaysMs + tenDaysMs))
            mortalityDao.insertMortality(Mortality(bandeId = 1, count = 3, date = now - twentyFiveDaysMs + fifteenDaysMs))
            mortalityDao.insertMortality(Mortality(bandeId = 2, count = 1, date = now - fifteenDaysMs + (5L * 24 * 60 * 60 * 1000)))

            // 4. Initial Feed Ingredients for formulas
            val formulaDao = db.feedFormulaDao()
            val types = listOf("Démarrage", "Croissance", "Finition")
            for (type in types) {
                formulaDao.insertIngredient(FeedFormula(feedType = type, ingredientName = "Maïs (Corn)", percentage = if (type == "Finition") 65.0 else if (type == "Croissance") 60.0 else 55.0, pricePerKg = 350.0))
                formulaDao.insertIngredient(FeedFormula(feedType = type, ingredientName = "Tourteau de soja", percentage = if (type == "Finition") 15.0 else if (type == "Croissance") 20.0 else 25.0, pricePerKg = 550.0))
                formulaDao.insertIngredient(FeedFormula(feedType = type, ingredientName = "Concentré", percentage = 15.0, pricePerKg = 700.0))
                formulaDao.insertIngredient(FeedFormula(feedType = type, ingredientName = "Prémix", percentage = 5.0, pricePerKg = 1500.0))
            }

            // 5. Initial Transactions
            val transactionDao = db.transactionDao()
            transactionDao.insertTransaction(FarmTransaction(type = "OUT", category = "Achat poussins", amount = 250000.0, date = now - twentyFiveDaysMs, description = "Achat 500 poussins d'un jour Lot A", bandeId = 1))
            transactionDao.insertTransaction(FarmTransaction(type = "OUT", category = "Aliment", amount = 120000.0, date = now - twentyFiveDaysMs + (2 * 24 * 60 * 60 * 1000), description = "Sack d'aliments prédémarrage Lot A", bandeId = 1))
            transactionDao.insertTransaction(FarmTransaction(type = "OUT", category = "Vétérinaire", amount = 15000.0, date = now - twentyFiveDaysMs + (11 * 24 * 60 * 60 * 1000), description = "Vaccins J11 Lot A", bandeId = 1))
            transactionDao.insertTransaction(FarmTransaction(type = "IN", category = "Vente", amount = 480000.0, date = now - tenDaysMs, description = "Vente partielle 150 poulets Lot A", bandeId = 1, isSale = true))

            transactionDao.insertTransaction(FarmTransaction(type = "OUT", category = "Achat poussins", amount = 200000.0, date = now - fifteenDaysMs, description = "Achat 400 poussins Lot B", bandeId = 2))
            transactionDao.insertTransaction(FarmTransaction(type = "OUT", category = "Aliment", amount = 95000.0, date = now - fifteenDaysMs + (2 * 24 * 60 * 60 * 1000), description = "Aliment démarrage Lot B", bandeId = 2))

            // General operating cash inflows/outflows
            transactionDao.insertTransaction(FarmTransaction(type = "OUT", category = "Autre", amount = 50000.0, date = now - (5L * 24 * 60 * 60 * 1000), description = "Salaire gardien de ferme"))

            // 6. Initial Alarms
            val alarmDao = db.feedingAlarmDao()
            alarmDao.insertAlarm(FeedingAlarm(hour = 7, minute = 0, isActive = true))
            alarmDao.insertAlarm(FeedingAlarm(hour = 12, minute = 30, isActive = true))
            alarmDao.insertAlarm(FeedingAlarm(hour = 18, minute = 0, isActive = true))

            // 7. Initial Orders
            val orderDao = db.orderDao()
            orderDao.insertOrder(Order(clientName = "Hôtel du Plateau", clientPhone = "+2250707070707", quantity = 100, unitPrice = 3000.0, totalAmount = 300000.0, status = "PENDING", sellerName = "Vendeur", paymentStatus = "NON_PAYE", deliveryDate = now + 2 * 24 * 60 * 60 * 1000))
            orderDao.insertOrder(Order(clientName = "Maquis Valy Cocody", clientPhone = "+2250505050505", quantity = 50, unitPrice = 3200.0, totalAmount = 160000.0, status = "PROCESSED", sellerName = "Vendeur", paymentStatus = "PAYE", deliveryDate = now + 1 * 24 * 60 * 60 * 1000))
            orderDao.insertOrder(Order(clientName = "Supermarché Abidjan Mall", clientPhone = "+2250101010101", quantity = 150, unitPrice = 2900.0, totalAmount = 435000.0, status = "PENDING", sellerName = "Administrateur", paymentStatus = "ACOMPTE", deliveryDate = now + 3 * 24 * 60 * 60 * 1000))

            // 8. Initial Tasks for Lot B (J15) and Lot A (J25)
            val taskDao = db.volaillerTaskDao()
            // Lot B is J15
            taskDao.insertTask(VolaillerTask(bandeId = 2, dayOfCycle = 11, title = "Vaccin Peste + Bronchite (H120)", description = "Matin: Vaccin H120 dans l'eau de boisson. Après-midi: Vitamines de soutien.", isCompleted = true, date = now - (4L * 24 * 60 * 60 * 1000)))
            taskDao.insertTask(VolaillerTask(bandeId = 2, dayOfCycle = 14, title = "Vaccin Gumboro Intermédiaire", description = "Matin: Gumboro. Après-midi: Vitamines.", isCompleted = true, date = now - (1L * 24 * 60 * 60 * 1000)))
            taskDao.insertTask(VolaillerTask(bandeId = 2, dayOfCycle = 15, title = "Transition Croissance & Vitamines", description = "Donner vitamines de transition pour soutenir la croissance.", isCompleted = false, date = now))

            // Lot A is J25
            taskDao.insertTask(VolaillerTask(bandeId = 1, dayOfCycle = 21, title = "Rappel Gumboro Forte", description = "Matin: Rappel Gumboro Forte. Après-midi: Vitamines.", isCompleted = true, date = now - (4L * 24 * 60 * 60 * 1000)))
            taskDao.insertTask(VolaillerTask(bandeId = 1, dayOfCycle = 24, title = "Protecteur hépatique", description = "Donner protecteur hépatique alternativement avec de l'eau claire pour détoxifier.", isCompleted = true, date = now - (1L * 24 * 60 * 60 * 1000)))
            taskDao.insertTask(VolaillerTask(bandeId = 1, dayOfCycle = 25, title = "Eau claire / Protecteur hépatique", description = "Alterner protecteur hépatique et eau claire.", isCompleted = false, date = now))

            // 9. Initial Inventory Items
            val inventoryDao = db.inventoryDao()
            inventoryDao.insertInventoryItem(InventoryItem(name = "Aliment Démarrage", category = "ALIMENT", quantity = 350.0, unit = "kg", threshold = 100.0, pricePerUnit = 400.0))
            inventoryDao.insertInventoryItem(InventoryItem(name = "Aliment Croissance", category = "ALIMENT", quantity = 80.0, unit = "kg", threshold = 150.0, pricePerUnit = 420.0)) // Trigger alert!
            inventoryDao.insertInventoryItem(InventoryItem(name = "Aliment Finition", category = "ALIMENT", quantity = 600.0, unit = "kg", threshold = 200.0, pricePerUnit = 450.0))
            inventoryDao.insertInventoryItem(InventoryItem(name = "Vaccin Gumboro", category = "VETERINAIRE", quantity = 15.0, unit = "flacons", threshold = 5.0, pricePerUnit = 2500.0))
            inventoryDao.insertInventoryItem(InventoryItem(name = "Vaccin H120 (Peste)", category = "VETERINAIRE", quantity = 2.0, unit = "flacons", threshold = 4.0, pricePerUnit = 3000.0)) // Trigger alert!
            inventoryDao.insertInventoryItem(InventoryItem(name = "Vitamines Oligo", category = "VETERINAIRE", quantity = 10.0, unit = "sachets", threshold = 3.0, pricePerUnit = 1500.0))

            // 10. Initial Scheduled Interventions
            val interventionDao = db.interventionDao()
            val dayInMs = 24L * 60 * 60 * 1000
            interventionDao.insertIntervention(Intervention(bandeId = 2, date = now + (2 * dayInMs), type = "VACCINATION", title = "Rappel Gumboro J17 (Lot B)", description = "Rappel du vaccin Gumboro intermédiaire pour le Lot B."))
            interventionDao.insertIntervention(Intervention(bandeId = 1, date = now + (3 * dayInMs), type = "VACCINATION", title = "Vaccin Lasota J28 (Lot A)", description = "Administration du vaccin Lasota contre la peste porcine / NewCastle."))
            interventionDao.insertIntervention(Intervention(bandeId = 3, date = now + (1 * dayInMs), type = "TRAITEMENT", title = "Vermifuge J6 (Lot C)", description = "Vermifugation préventive des poussins du Lot C."))
        }
    }
}
