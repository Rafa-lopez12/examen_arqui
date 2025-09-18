package com.example.tienda_emprendedor

import android.app.Application
import com.stripe.android.PaymentConfiguration

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Inicializar Stripe con tu clave pública
        PaymentConfiguration.init(
            applicationContext,
            "pk_test_51RTUBMHIj82SmGbDobq1uVtN5RTTtJbmY81CiBDmxgDzIEDe1QB7NooKByfJnQGL3uUTKPiVkYxNeCcBID4vua9R00lSQRU395"
        )

        println("✅ Stripe inicializado correctamente")
    }
}