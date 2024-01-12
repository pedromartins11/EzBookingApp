package ipca.utility.bookinghousesapp

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import androidx.lifecycle.lifecycleScope
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.util.Pair
import androidx.core.widget.addTextChangedListener
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.messaging.FirebaseMessaging
import io.swagger.client.models.House
import io.swagger.client.models.Reservation
import ipca.utility.bookinghousesapp.databinding.ActivityHousedetailBinding
import ipca.utility.bookinghousesapp.databinding.ActivityReservationDetailsBinding
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class ReservationDetailsActivity : AppCompatActivity() {

    private lateinit var binding : ActivityReservationDetailsBinding
    private var startDate: LocalDateTime? = null
    private var endDate: LocalDateTime? = null
    var valortotalReserva = 0.0
    var reservationId = 0
    var token = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReservationDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("user_id", 0)

        val houseId = intent.extras?.getInt("HOUSE_ID")?:-1
        val houseName = intent.extras?.getString("HOUSE_NAME")?:""
        val medclass = intent.extras?.getDouble("HOUSE_FEEDM")?:0.0
        val price = intent.extras?.getDouble("HOUSE_PRICE")?:null
        val imagelink = intent.extras?.getString("HOUSE_IMAGE")?:""

        val image = binding.imageViewHouse
        val date = binding.textViewCheckInOut2


        val db = FirebaseFirestore.getInstance()
        val tokenRef = db.collection("tokens").document(userId.toString())
        tokenRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    token = documentSnapshot.getString("token")!!
                }
            }

        binding.textViewName.text = houseName
        binding.textViewMFeed.text = "☆${medclass}"

        Glide.with(this)
            .load(imagelink)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(image)

        //updateReservationPrice()
        binding.editTextNumber.addTextChangedListener {
           updateReservationPrice()
        }

        date.setOnClickListener {
            val picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTheme(R.style.ThemeMaterialCalendar)
                .setTitleText("Selecione a Data de Estadia")
                .setSelection(Pair(null,null))
                .build()

            picker.show(this.supportFragmentManager, "TAG")

            picker.addOnPositiveButtonClickListener {
                date.setText(convertTimeToDate(it.first) + " - " +convertTimeToDate(it.second))
                startDate = convertTimeToDatee(it.first)
                endDate = convertTimeToDatee(it.second)
                updateReservationPrice()
            }
            picker.addOnNegativeButtonClickListener {
                picker.dismiss()
            }
        }

        binding.buttonBack2.setOnClickListener {
            onBackPressed()
        }

        binding.buttonConfirmtoPay.setOnClickListener {
            var reservation = Reservation(guestsNumber = binding.editTextNumber.text.toString().toInt(),init_date = startDate!!, end_date = endDate!!)
            Backend.CreateReservation(reservation, houseId = houseId, userId = userId,this ).observe(this){
                it.onError {error ->
                    Toast.makeText(
                        this@ReservationDetailsActivity,
                        "Erro:${error.error}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                it.onNetworkError {
                    Toast.makeText(
                        this@ReservationDetailsActivity,
                        "Sem Ligação à Internet",
                        Toast.LENGTH_LONG
                    ).show()
                }
                it.onSuccess {

                    val intent = Intent(this, PaymentActivity::class.java)
                    intent.putExtra("HOUSE_PRICET", valortotalReserva)
                    intent.putExtra("PAYMENT_CREATIONDATE", LocalDateTime.now().toString())
                    startActivity(intent)

                }


                }

        }


    }

    private fun convertTimeToDate(time: Long): String{
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utc.timeInMillis = time
        val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return format.format(utc.time)
    }

    private fun convertTimeToDatee(time: Long): LocalDateTime{
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneOffset.UTC)
    }

    private fun updateReservationPrice() {
        val pricePerNight = intent.extras?.getDouble("HOUSE_PRICE") ?: 0.0
        val sharedRoom = intent.extras?.getBoolean("HOUSE_SHARED")?:false
        val startDate = startDate
        val endDate = endDate
        var totalPrice = 0.0
        Log.d("tested",startDate.toString())
        val numGuests = binding.editTextNumber.text.toString().toIntOrNull() ?: 0
        var days = Duration.between(startDate, endDate).toDays()

        if (startDate == null || endDate == null) {
            days = 0
        }
        if (sharedRoom){
            totalPrice = pricePerNight * (numGuests) * days
        }
        else {
            totalPrice = pricePerNight * days
        }
        var totalTaxa = totalPrice * 0.05
        valortotalReserva = totalPrice+totalTaxa

        binding.textViewValorReserva.text = totalPrice.toString()
        binding.textViewTaxaEz.text = totalTaxa.toString()
        binding.textViewTotalHR.text = "${valortotalReserva}€"
    }

}