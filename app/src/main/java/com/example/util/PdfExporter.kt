package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.entity.Bande
import com.example.data.entity.FarmTransaction
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object PdfExporter {

    private fun formatFCFA(amount: Double): String {
        return NumberFormat.getInstance(Locale.FRANCE).format(amount) + " FCFA"
    }

    fun exportMonthlyReportPdf(
        context: Context,
        reportTitle: String, // e.g. "Bilan d'Exploitation - Juillet 2026"
        transactions: List<FarmTransaction>,
        bandes: List<Bande>
    ) {
        val pdfDocument = PdfDocument()
        // Page width 595, height 842 (A4 size in points)
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint()
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }

        // Header Rect
        paint.color = Color.parseColor("#2E7D32") // Farm Success Green
        canvas.drawRect(0f, 0f, 595f, 100f, paint)

        // Header Title
        textPaint.apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("LES FERMES ABIDJANAISES", 30f, 45f, textPaint)

        textPaint.apply {
            color = Color.argb(200, 255, 255, 255)
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText(reportTitle, 30f, 70f, textPaint)

        // Subtitle/Date right aligned
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val dateStr = "Généré le: " + sdf.format(Date())
        textPaint.textSize = 10f
        canvas.drawText(dateStr, 400f, 45f, textPaint)

        // Reset text paint
        textPaint.color = Color.BLACK
        textPaint.textSize = 12f

        var yPos = 140f

        // 1. FINANCIAL SUMMARY SECTION
        textPaint.apply {
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.parseColor("#1B5E20")
        }
        canvas.drawText("1. RÉSUMÉ DES FINANCES", 30f, yPos, textPaint)
        yPos += 8f
        paint.color = Color.parseColor("#1B5E20")
        canvas.drawRect(30f, yPos, 565f, yPos + 2f, paint)
        yPos += 24f

        val totalIn = transactions.filter { it.type == "IN" }.sumOf { it.amount }
        val totalOut = transactions.filter { it.type == "OUT" }.sumOf { it.amount }
        val netCash = totalIn - totalOut

        textPaint.apply {
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            color = Color.BLACK
        }

        canvas.drawText("Total des Recettes (Entrées) :", 50f, yPos, textPaint)
        textPaint.apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.parseColor("#2E7D32")
        }
        canvas.drawText(formatFCFA(totalIn), 350f, yPos, textPaint)
        yPos += 20f

        textPaint.apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            color = Color.BLACK
        }
        canvas.drawText("Total des Dépenses (Intrants/Charges) :", 50f, yPos, textPaint)
        textPaint.apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.parseColor("#C62828")
        }
        canvas.drawText("-" + formatFCFA(totalOut), 350f, yPos, textPaint)
        yPos += 20f

        paint.color = Color.parseColor("#EEEEEE")
        canvas.drawRect(40f, yPos, 550f, yPos + 25f, paint)

        textPaint.apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.BLACK
        }
        canvas.drawText("CASH FLOW NET :", 55f, yPos + 17f, textPaint)
        textPaint.color = if (netCash >= 0) Color.parseColor("#2E7D32") else Color.parseColor("#C62828")
        canvas.drawText(formatFCFA(netCash), 350f, yPos + 17f, textPaint)
        yPos += 45f

        // 2. BANDES / LOTS DE VOLAILLES SECTION
        textPaint.apply {
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.parseColor("#E65100")
        }
        canvas.drawText("2. ÉTAT DES BANDES DE VOLAILLES", 30f, yPos, textPaint)
        yPos += 8f
        paint.color = Color.parseColor("#E65100")
        canvas.drawRect(30f, yPos, 565f, yPos + 2f, paint)
        yPos += 24f

        textPaint.apply {
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.DKGRAY
        }
        canvas.drawText("Nom du Lot", 50f, yPos, textPaint)
        canvas.drawText("Date Arrivée", 200f, yPos, textPaint)
        canvas.drawText("Effectif Initial", 350f, yPos, textPaint)
        canvas.drawText("Statut", 480f, yPos, textPaint)
        yPos += 10f

        paint.color = Color.parseColor("#CCCCCC")
        canvas.drawRect(40f, yPos, 550f, yPos + 1f, paint)
        yPos += 18f

        textPaint.apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            color = Color.BLACK
        }

        val sdfArrival = SimpleDateFormat("dd MMM yyyy", Locale.FRENCH)

        if (bandes.isEmpty()) {
            canvas.drawText("Aucun lot enregistré sur cette période.", 50f, yPos, textPaint)
            yPos += 20f
        } else {
            bandes.forEach { b ->
                if (yPos > 760) return@forEach // Prevent vertical overflow
                canvas.drawText(b.name, 50f, yPos, textPaint)
                canvas.drawText(sdfArrival.format(Date(b.arrivalDate)), 200f, yPos, textPaint)
                canvas.drawText("${b.initialCount} sujets", 350f, yPos, textPaint)
                canvas.drawText(if (b.status == "ACTIVE") "Actif" else "Vendu", 480f, yPos, textPaint)
                yPos += 18f
            }
        }
        yPos += 20f

        // 3. DETAILED CATEGORIES BREAKDOWN
        if (yPos < 700) {
            textPaint.apply {
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.parseColor("#1565C0")
            }
            canvas.drawText("3. RÉPARTITION DES CHARGES", 30f, yPos, textPaint)
            yPos += 8f
            paint.color = Color.parseColor("#1565C0")
            canvas.drawRect(30f, yPos, 565f, yPos + 2f, paint)
            yPos += 24f

            textPaint.apply {
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                color = Color.BLACK
            }

            val categories = listOf("Achat poussins", "Aliment", "Vétérinaire", "Autre")
            categories.forEach { cat ->
                val amount = transactions.filter { it.type == "OUT" && it.category == cat }.sumOf { it.amount }
                canvas.drawText("Dépenses - $cat :", 50f, yPos, textPaint)
                canvas.drawText(formatFCFA(amount), 350f, yPos, textPaint)
                yPos += 18f
            }
        }

        // Page Footer
        paint.color = Color.parseColor("#9E9E9E")
        canvas.drawRect(30f, 800f, 565f, 801f, paint)
        textPaint.apply {
            textSize = 9f
            color = Color.GRAY
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        canvas.drawText("Les Fermes Abidjanaises - Document d'exploitation confidentiel et officiel", 30f, 815f, textPaint)
        canvas.drawText("Page 1/1", 520f, 815f, textPaint)

        pdfDocument.finishPage(page)

        // Save PDF to downloads or documents
        val path = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
        val file = File(path, "Rapport_Ferme_${System.currentTimeMillis()}.pdf")

        try {
            val fos = FileOutputStream(file)
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos.close()

            Toast.makeText(context, "PDF exporté : ${file.name}", Toast.LENGTH_LONG).show()

            // Open/Share PDF Intent
            try {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(Intent.createChooser(intent, "Partager le rapport PDF"))
            } catch (e: Exception) {
                // If provider fails, try view intent
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.fromFile(file), "application/pdf")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Erreur d'exportation PDF : ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
