package com.example.utils

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.data.model.PaymentTransaction
import com.example.data.model.RestaurantTenant
import com.example.data.model.StaffEmployee
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExporter {

    fun writePdfReport(
        context: Context,
        uri: Uri,
        reportTitle: String,
        lines: List<String>
    ): Result<Unit> {
        return try {
            val document = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val titlePaint = Paint().apply {
                color = Color.parseColor("#0F172A")
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val metaPaint = Paint().apply {
                color = Color.parseColor("#475569")
                textSize = 11f
            }
            val bodyPaint = Paint().apply {
                color = Color.parseColor("#1E293B")
                textSize = 11.5f
                typeface = Typeface.MONOSPACE
            }

            var pageNum = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas
            var y = 52f

            canvas.drawText("RestroAdmin • $reportTitle", 40f, y, titlePaint)
            y += 22f
            val stamp = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())
            canvas.drawText("Generated: $stamp", 40f, y, metaPaint)
            y += 24f

            lines.forEach { line ->
                if (y > pageHeight - 48) {
                    document.finishPage(page)
                    pageNum++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    y = 52f
                }
                canvas.drawText(line.take(86), 40f, y, bodyPaint)
                y += 18f
            }

            document.finishPage(page)
            context.contentResolver.openOutputStream(uri)?.use { out ->
                document.writeTo(out)
            } ?: error("Unable to open output stream for PDF.")
            document.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun writeCsvReport(
        context: Context,
        uri: Uri,
        headers: List<String>,
        rows: List<List<String>>
    ): Result<Unit> {
        return try {
            val sb = StringBuilder()
            sb.appendLine(headers.joinToString(",") { escapeCsv(it) })
            rows.forEach { row ->
                sb.appendLine(row.joinToString(",") { escapeCsv(it) })
            }
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(sb.toString().toByteArray(Charsets.UTF_8))
            } ?: error("Unable to open output stream for CSV.")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun writeJsonBackup(
        context: Context,
        uri: Uri,
        restaurants: List<RestaurantTenant>,
        payments: List<PaymentTransaction>,
        employees: List<StaffEmployee>
    ): Result<Unit> {
        return try {
            val root = JSONObject()
            root.put("exportedAt", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date()))
            root.put("restaurantCount", restaurants.size)
            val restArr = JSONArray()
            restaurants.forEach { r ->
                restArr.put(
                    JSONObject().apply {
                        put("userCode", r.userCode)
                        put("restaurantName", r.restaurantName)
                        put("ownerName", r.ownerName)
                        put("mobileNumber", r.mobileNumber)
                        put("email", r.email)
                        put("address", r.address)
                        put("plan", r.plan)
                        put("billingCycle", r.billingCycle)
                        put("subscriptionStatus", r.subscriptionStatus)
                        put("accountStatus", r.accountStatus)
                        put("demoAccount", r.demoAccount)
                        put("expiryEpochMillis", r.expiryEpochMillis)
                    }
                )
            }
            root.put("restaurants", restArr)
            root.put("paymentsCount", payments.size)
            root.put("employeesCount", employees.size)

            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(root.toString(2).toByteArray(Charsets.UTF_8))
            } ?: error("Unable to open output stream for JSON backup.")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun readTextFromUri(context: Context, uri: Uri): Result<String> {
        return try {
            val sb = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        sb.appendLine(line)
                        line = reader.readLine()
                    }
                }
            } ?: error("Unable to read selected backup file.")
            Result.success(sb.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun escapeCsv(value: String): String {
        val clean = value.replace("\"", "\"\"")
        return "\"$clean\""
    }
}
