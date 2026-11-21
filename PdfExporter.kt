package com.premiabrygady.app

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

data class PdfRow(val name: String, val hours: Long, val shareText: String, val amountCents: Long)

/**
 * Raport PDF na wbudowanym android.graphics.pdf.PdfDocument (bez bibliotek).
 * Strona A4, tabela: Osoba / Godziny / Udzial / Premia.
 * Naglowek: nazwa, data, kwota, suma godzin. Dluga lista -> kolejne strony.
 * Polskie znaki dzialaja, bo uzywany jest domyslny font systemowy.
 */
object PdfExporter {

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 40
    private const val ROW_H = 22

    fun createPdf(
        context: Context,
        title: String,
        dateText: String,
        amountCents: Long,
        totalHours: Long,
        rows: List<PdfRow>
    ): File {
        val document = PdfDocument()
        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 20f
            color = 0xFF111111.toInt()
        }
        val headerPaint = Paint().apply {
            typeface = Typeface.DEFAULT
            textSize = 12f
            color = 0xFF333333.toInt()
        }
        val cellPaint = Paint().apply {
            typeface = Typeface.DEFAULT
            textSize = 12f
            color = 0xFF111111.toInt()
        }
        val cellBold = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 12f
            color = 0xFF111111.toInt()
        }
        val linePaint = Paint().apply {
            color = 0xFF999999.toInt()
            strokeWidth = 1f
        }

        val rowsPerPage = 34
        var pageNumber = 1
        var pageInfo = document.startPage(
            PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNumber).create()
        )
        var canvas = pageInfo.canvas
        var y = MARGIN.toFloat()

        fun drawHeader() {
            canvas.drawText("Premia Brygady", MARGIN.toFloat(), y, titlePaint)
            y += 24f
            canvas.drawText("Nazwa: " + if (title.isBlank()) "-" else title, MARGIN.toFloat(), y, headerPaint)
            y += 18f
            canvas.drawText("Data: " + dateText, MARGIN.toFloat(), y, headerPaint)
            y += 18f
            canvas.drawText(
                "Kwota premii: " + PremiumCalculator.formatPln(amountCents) +
                    "    Suma godzin: " + totalHours,
                MARGIN.toFloat(), y, headerPaint
            )
            y += 16f
            canvas.drawLine(MARGIN.toFloat(), y, (PAGE_W - MARGIN).toFloat(), y, linePaint)
            y += 22f
            canvas.drawText("Osoba", MARGIN.toFloat(), y, cellBold)
            canvas.drawText("Godziny", 280f, y, cellBold)
            canvas.drawText("Udzial", 360f, y, cellBold)
            canvas.drawText("Premia", 450f, y, cellBold)
            y += 6f
            canvas.drawLine(MARGIN.toFloat(), y, (PAGE_W - MARGIN).toFloat(), y, linePaint)
            y += 20f
        }

        drawHeader()
        var rowIndex = 0
        for (row in rows) {
            if (rowIndex > 0 && rowIndex % rowsPerPage == 0) {
                document.finishPage(pageInfo)
                pageNumber += 1
                pageInfo = document.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNumber).create()
                )
                canvas = pageInfo.canvas
                y = MARGIN.toFloat()
                drawHeader()
            }
            canvas.drawText(row.name, MARGIN.toFloat(), y, cellPaint)
            canvas.drawText(row.hours.toString(), 280f, y, cellPaint)
            canvas.drawText(row.shareText, 360f, y, cellPaint)
            canvas.drawText(PremiumCalculator.formatPln(row.amountCents), 450f, y, cellPaint)
            y += ROW_H
            rowIndex += 1
        }
        document.finishPage(pageInfo)

        val dir = File(context.cacheDir, "pdfs")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "premia.pdf")
        FileOutputStream(file).use { out -> document.writeTo(out) }
        document.close()
        return file
    }

    fun sharePdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Wyslij PDF"))
    }
}
