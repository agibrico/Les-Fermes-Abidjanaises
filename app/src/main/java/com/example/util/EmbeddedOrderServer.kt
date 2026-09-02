package com.example.util

import android.util.Log
import com.example.data.entity.Order
import com.example.ui.viewmodel.FarmViewModel
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

class EmbeddedOrderServer(
    private val viewModel: FarmViewModel,
    private val port: Int = 8080,
    private val securityToken: String = "FarmSecureToken2026"
) {
    private var server: HttpServer? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun start() {
        if (server != null) return
        try {
            server = HttpServer.create(InetSocketAddress(port), 0).apply {
                createContext("/api/orders", OrderHandler())
                createContext("/api/status", StatusHandler())
                executor = null // Use default executor
                start()
            }
            Log.d("EmbeddedOrderServer", "Server started on port $port")
        } catch (e: Exception) {
            Log.e("EmbeddedOrderServer", "Failed to start server on port $port", e)
        }
    }

    fun stop() {
        try {
            server?.stop(0)
            server = null
            Log.d("EmbeddedOrderServer", "Server stopped")
        } catch (e: Exception) {
            Log.e("EmbeddedOrderServer", "Failed to stop server", e)
        }
    }

    fun isRunning(): Boolean {
        return server != null
    }

    fun getPort(): Int {
        return port
    }

    fun getSecurityToken(): String {
        return securityToken
    }

    private inner class StatusHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            if ("GET" == exchange.requestMethod) {
                val response = JSONObject().apply {
                    put("status", "running")
                    put("port", port)
                    put("endpoints", JSONArray().apply {
                        put("POST /api/orders")
                        put("GET /api/status")
                    })
                }.toString()

                sendResponse(exchange, 200, response)
            } else {
                exchange.sendResponseHeaders(405, -1) // Method Not Allowed
            }
        }
    }

    private inner class OrderHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            try {
                if ("POST" != exchange.requestMethod) {
                    sendResponse(exchange, 405, "{\"error\": \"Method not allowed. Use POST.\"}")
                    return
                }

                // Verify authorization header (Bearer token)
                val authHeader = exchange.requestHeaders.getFirst("Authorization")
                if (authHeader == null || authHeader != "Bearer $securityToken") {
                    Log.w("EmbeddedOrderServer", "Unauthorized request received: $authHeader")
                    sendResponse(exchange, 401, "{\"error\": \"Unauthorized. Invalid or missing security token.\"}")
                    return
                }

                // Read request body
                val reader = BufferedReader(InputStreamReader(exchange.requestBody, StandardCharsets.UTF_8))
                val body = reader.readText()
                reader.close()

                Log.d("EmbeddedOrderServer", "Received body: $body")
                val json = JSONObject(body)

                val clientName = json.getString("clientName")
                val clientPhone = json.getString("clientPhone")
                val clientType = json.optString("clientType", "Particulier")
                val deliveryLocation = json.optString("deliveryLocation", "Abidjan")
                
                // Read items
                val itemsArray = json.optJSONArray("items")
                var totalQuantity = 0
                var totalAmount = 0.0
                var parsedItemsDescription = ""

                if (itemsArray != null && itemsArray.length() > 0) {
                    for (i in 0 until itemsArray.length()) {
                        val item = itemsArray.getJSONObject(i)
                        val name = item.getString("name")
                        val quantity = item.getInt("quantity")
                        val unitPrice = item.getDouble("unitPrice")
                        
                        totalQuantity += quantity
                        totalAmount += (quantity * unitPrice)
                        
                        if (parsedItemsDescription.isNotEmpty()) {
                            parsedItemsDescription += ", "
                        }
                        parsedItemsDescription += "$quantity x $name"
                    }
                } else {
                    // Fallback to flat fields if there's no nested list
                    totalQuantity = json.getInt("quantity")
                    val unitPrice = json.getDouble("unitPrice")
                    totalAmount = totalQuantity * unitPrice
                    parsedItemsDescription = "$totalQuantity x Poulets"
                }

                // Construct full client descriptive name with details
                val fullClientDescription = "$clientName ($clientType - $deliveryLocation) - $parsedItemsDescription"

                // Create the order inside ViewModel
                viewModel.createOrder(
                    clientName = fullClientDescription,
                    clientPhone = clientPhone,
                    quantity = totalQuantity,
                    unitPrice = if (totalQuantity > 0) totalAmount / totalQuantity else 0.0,
                    sellerName = "API Client Externe",
                    paymentStatus = "NON_PAYE",
                    deliveryDate = System.currentTimeMillis() + 24 * 60 * 60 * 1000 // 1 day from now
                )

                val successResponse = JSONObject().apply {
                    put("success", true)
                    put("message", "Commande reçue et enregistrée avec succès.")
                    put("totalAmount", totalAmount)
                }.toString()

                sendResponse(exchange, 200, successResponse)
            } catch (e: Exception) {
                Log.e("EmbeddedOrderServer", "Error parsing POST data", e)
                sendResponse(exchange, 400, "{\"error\": \"Invalid request data: ${e.message}\"}")
            }
        }
    }

    private fun sendResponse(exchange: HttpExchange, statusCode: Int, response: String) {
        val bytes = response.toByteArray(StandardCharsets.UTF_8)
        exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
        exchange.sendResponseHeaders(statusCode, bytes.size.toLong())
        val os: OutputStream = exchange.responseBody
        os.write(bytes)
        os.close()
    }
}
