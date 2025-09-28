package com.example.tienda_emprendedor.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class StripeService {
    companion object {
        private const val SECRET_KEY = "sk_test_51RTUBMHIj82SmGbDUOfWoYdywM9WJE2H73lXKaRyQxIg3lWiqxHaYlY0woSm8OPt9tvSo2NpFDpDzhMhGkgi25oj000QK3sXEc"
        private const val STRIPE_API_URL = "https://api.stripe.com/v1"
    }

    private val client = OkHttpClient.Builder()
        .build()

    suspend fun createPaymentIntent(
        amount: Long,
        currency: String = "usd",
        description: String = "Venta de Aires Acondicionados"
    ): Result<String> = withContext(Dispatchers.IO) {

        return@withContext suspendCoroutine { continuation ->
            val url = "$STRIPE_API_URL/payment_intents"

            val requestBody = FormBody.Builder()
                .add("amount", amount.toString())
                .add("currency", currency)
                .add("description", description)
                .add("automatic_payment_methods[enabled]", "true")
                .build()

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Authorization", "Bearer $SECRET_KEY")
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    println("❌ Error al crear PaymentIntent: ${e.message}")
                    continuation.resume(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        val responseBody = response.body?.string()
                        println("📝 Respuesta de Stripe: $responseBody")

                        if (response.isSuccessful && responseBody != null) {
                            val jsonResponse = JSONObject(responseBody)
                            val clientSecret = jsonResponse.getString("client_secret")

                            println("✅ PaymentIntent creado exitosamente")
                            println("🔑 Client Secret: ${clientSecret.substring(0, 10)}...")

                            continuation.resume(Result.success(clientSecret))
                        } else {
                            val errorMsg = "Error HTTP ${response.code}: ${response.message}"
                            println("$errorMsg")
                            if (responseBody != null) {
                                println("Error body: $responseBody")
                            }
                            continuation.resume(Result.failure(Exception(errorMsg)))
                        }
                    } catch (e: Exception) {
                        println("Error parseando respuesta: ${e.message}")
                        continuation.resume(Result.failure(e))
                    }
                }
            })
        }
    }

    fun dollarsToCents(dollars: Double): Long {
        return (dollars * 100).toLong()
    }


    fun centsToDollars(cents: Long): Double {
        return cents / 100.0
    }
}