package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val role: String, // "ADMINISTRATEUR", "PARTENAIRE", "VOLAILLER", "VENDEUR"
    val password: String = "1234"
)

@Entity(tableName = "bandes")
data class Bande(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val arrivalDate: Long = System.currentTimeMillis(),
    val initialCount: Int,
    val status: String = "ACTIVE", // "ACTIVE", "SOLD"
    val souche: String = "Cobb 500"
)

@Entity(tableName = "mortalities")
data class Mortality(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bandeId: Int,
    val count: Int,
    val date: Long = System.currentTimeMillis()
)

@Entity(tableName = "feed_formulas")
data class FeedFormula(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val feedType: String, // "Démarrage", "Croissance", "Finition"
    val ingredientName: String,
    val percentage: Double,
    val pricePerKg: Double
)

@Entity(tableName = "transactions")
data class FarmTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "IN" (Entrée), "OUT" (Sortie)
    val category: String, // "Achat poussins", "Aliment", "Vétérinaire", "Vente", "Autre"
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val description: String,
    val bandeId: Int? = null,
    val isSale: Boolean = false
)

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clientName: String,
    val clientPhone: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalAmount: Double,
    val date: Long = System.currentTimeMillis(),
    val status: String = "PENDING", // "PENDING" (En attente), "PROCESSED" (Traitée)
    val sellerName: String = "Administrateur",
    val paymentStatus: String = "NON_PAYE", // "NON_PAYE" (Non Payé), "PAYE" (Payé), "ACOMPTE" (Acompte)
    val deliveryDate: Long = System.currentTimeMillis() + 24 * 60 * 60 * 1000 // Date de livraison prévue
)

@Entity(tableName = "volailler_tasks")
data class VolaillerTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bandeId: Int,
    val dayOfCycle: Int,
    val title: String,
    val description: String,
    val isCompleted: Boolean = false,
    val date: Long = System.currentTimeMillis()
)

@Entity(tableName = "feeding_alarms")
data class FeedingAlarm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val isActive: Boolean = true
)

@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String, // "ALIMENT" or "VETERINAIRE"
    val quantity: Double, // in kg or units
    val unit: String, // "kg", "sacs", "flacons", "boîtes"
    val threshold: Double, // critical threshold for alerts
    val pricePerUnit: Double = 0.0
)

@Entity(tableName = "interventions")
data class Intervention(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bandeId: Int,
    val date: Long, // timestamp
    val type: String, // "VACCINATION", "TRAITEMENT", "VITAMINES"
    val title: String,
    val description: String,
    val isCompleted: Boolean = false
)
