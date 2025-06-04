package com.example.stripe_poc_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.example.stripe_poc_android.ui.theme.DefaultExampleTheme
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.json.responseJson
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.rememberPaymentSheet
import com.github.kittinunf.result.Result


class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DefaultExampleTheme{
                Checkout()
            }
        }
    }
}


@OptIn(UnstableApi::class)
@Composable
fun Checkout() {
    val paymentSheet = rememberPaymentSheet(::onPaymentSheetResult)
    val context = LocalContext.current
    var customerConfig by remember { mutableStateOf<PaymentSheet.CustomerConfiguration?>(null) }
    var paymentIntentClientSecret by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(context) {
        "http://10.0.2.2:3000/checkout".httpPost().responseJson { _, _, result ->
            Log.d("debug", result.toString())
            if (result is Result.Success) {
                val responseJson = result.get().obj()
                Log.d("debug", responseJson.toString())
                paymentIntentClientSecret = responseJson.getString("paymentIntent")
                customerConfig = PaymentSheet.CustomerConfiguration(
                    id = responseJson.getString("customer"),
                    ephemeralKeySecret = responseJson.getString("ephemeralKey")
                )
                val publishableKey = responseJson.getString("publishableKey")
                PaymentConfiguration.init(context, publishableKey)
            }
        }
    }

    Button(
        onClick = {
            val currentConfig = customerConfig
            val currentClientSecret = paymentIntentClientSecret
            Log.d("debug", currentConfig.toString() + "  " + currentClientSecret)

            if (currentConfig != null && currentClientSecret != null) {
                presentPaymentSheet(paymentSheet, currentConfig, currentClientSecret)
            }
        },
        modifier = Modifier.padding(top = 400.dp, start = 140.dp)
    ) {
        Text("Checkout")
    }
}

private fun presentPaymentSheet(
    paymentSheet: PaymentSheet,
    customerConfig: PaymentSheet.CustomerConfiguration,
    paymentIntentClientSecret: String
) {
    val googlePayConfiguration = PaymentSheet.GooglePayConfiguration(
        environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
        countryCode = "US",
        currencyCode = "USD" // Required for Setup Intents, optional for Payment Intents
    )
    paymentSheet.presentWithPaymentIntent(
        paymentIntentClientSecret,
        PaymentSheet.Configuration(
            merchantDisplayName = "Marlo",
            customer = customerConfig,
            // Set `allowsDelayedPaymentMethods` to true if your business handles
            // delayed notification payment methods like US bank accounts.
            allowsDelayedPaymentMethods = true,
            googlePay = googlePayConfiguration
        )
    )
}

private fun onPaymentSheetResult(paymentSheetResult: PaymentSheetResult) {
    when(paymentSheetResult) {
        is PaymentSheetResult.Canceled -> {
            print("Canceled")
        }
        is PaymentSheetResult.Failed -> {
            print("Error: ${paymentSheetResult.error}")
        }
        is PaymentSheetResult.Completed -> {
            // Display for example, an order confirmation screen
            print("Completed")
        }
    }
}
