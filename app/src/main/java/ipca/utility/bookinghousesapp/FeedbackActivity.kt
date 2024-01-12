package ipca.utility.bookinghousesapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import ipca.utility.bookinghousesapp.databinding.ActivityFeedbackBinding
import ipca.utility.bookinghousesapp.databinding.ActivityRegisterBinding

class FeedbackActivity : AppCompatActivity() {
    private lateinit var binding : ActivityFeedbackBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val reservationId = intent.extras?.getInt(DATA_RESERVATION) ?: -1

        val buttonBack = binding.imageViewBack
        buttonBack.setOnClickListener {
            onBackPressed()
        }


        binding.buttonReact.setOnClickListener {
            val comment = binding.editTextTextMultiLine.text.toString()
            val classification = binding.ratingBar.rating.toInt()

            Backend.CreateFeedback(lifecycleScope, classification, comment, reservationId) { createSuccessful ->
                if (createSuccessful) {
                    val intent = Intent(this,UserReservationsList::class.java )
                    startActivity(intent)
                    finish()
                }
            }
        }
    }


    companion object {
        const val DATA_RESERVATION = "data_reservation"
    }
}