package ipca.utility.bookinghousesapp

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore
import io.swagger.client.models.Payment
import ipca.utility.bookinghousesapp.databinding.ActivityHousedetailBinding
import ipca.utility.bookinghousesapp.databinding.ActivityPaymentBinding
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Date
import java.util.Locale

class PaymentActivity : AppCompatActivity() {
    private lateinit var binding : ActivityPaymentBinding
    private var selectedPaymentMethod: String = ""
    var token = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("user_id", 0)
        var idReservation = 0



        val metodosPagamento = resources.getStringArray(R.array.metodos_pagamento)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, metodosPagamento)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinner.adapter = adapter

        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, selectedItemView: View?, position: Int, id: Long) {
                selectedPaymentMethod = parentView.getItemAtPosition(position).toString()

                binding.editTextEmail.visibility = View.GONE
                binding.editTextCardNumber.visibility = View.GONE
                binding.editTextCardExpiry.visibility = View.GONE
                binding.editTextCardCVV.visibility = View.GONE

                when (selectedPaymentMethod) {
                    "Paypal" -> {
                        binding.editTextEmail.visibility = View.VISIBLE
                    }
                    "Multibanco" -> {
                        binding.editTextCardNumber.visibility = View.VISIBLE
                        binding.editTextCardExpiry.visibility = View.VISIBLE
                        binding.editTextCardCVV.visibility = View.VISIBLE
                    }
                }
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {
            }
        }

        Backend.fetchReservationPayment(userId,this).observe(this) {
            it.onError { error ->
                Toast.makeText(
                    this@PaymentActivity,
                    "Erro:${error.error}",
                    Toast.LENGTH_LONG
                ).show()
            }
            it.onNetworkError {
                Toast.makeText(
                    this@PaymentActivity,
                    "Sem Ligação à Internet",
                    Toast.LENGTH_LONG
                ).show()
            }
            it.onSuccess {
                    reservation ->
                reservation?.let {
                    idReservation = reservation.id_reservation!!
                    Log.d("testeeeeu",idReservation.toString())
                    val db = FirebaseFirestore.getInstance()
                    val tokenRef = db.collection("tokens").document(reservation.house?.user?.id_user.toString())
                    tokenRef.get()
                        .addOnSuccessListener { documentSnapshot ->
                            if (documentSnapshot.exists()) {
                                token = documentSnapshot.getString("token")!!
                            }
                        }
                }
            }
        }


        binding.buttonPayment.setOnClickListener {

            val sharedPreferences = this.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            val userId = sharedPreferences.getInt("user_id", 0)

            Backend.GetLastPayment(lifecycleScope, userId) { payment ->
                val paymentId = payment.id_payment.toString().toInt()

                Backend.UpdatePayment(paymentId, lifecycleScope) { updateSuccessful ->
                    if (updateSuccessful) {
                        Backend.sendNotitication(token, "Reserva", "Reserva - O utilizador ${userId} efetuou uma reserva no seu alojamento",lifecycleScope){
                        }
                        val intent = Intent(this, UserReservationsList::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
            }
        }

        binding.buttonBack3.setOnClickListener {
            onBackPressed()
        }
    }
}