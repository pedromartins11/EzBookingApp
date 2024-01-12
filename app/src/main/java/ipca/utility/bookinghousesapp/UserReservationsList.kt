package ipca.utility.bookinghousesapp

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import io.swagger.client.apis.FeedbackApi
import ipca.utility.bookinghousesapp.Backend.BASE_API
import ipca.utility.bookinghousesapp.databinding.ActivityAdminReservationsListBinding
import ipca.utility.bookinghousesapp.databinding.ActivityUserReservationsListBinding

class UserReservationsList : AppCompatActivity() {
    private lateinit var binding : ActivityUserReservationsListBinding
    var reservations = arrayListOf<io.swagger.client.models.Reservation>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserReservationsListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.imageViewBack.setOnClickListener {
            onBackPressed()
        }

        Backend.GetUserReservations(this, lifecycleScope) { fetchedReservations ->
            reservations.addAll(fetchedReservations)
            if(!reservations.isEmpty()){
                binding.textViewNoReservations.visibility = View.GONE
                setupListView()
            }
        }
    }

    private fun setupListView() {
        val adapter = ReservationsListAdapter()
        binding.listViewReservations.adapter = adapter
    }

    inner class ReservationsListAdapter : BaseAdapter(){
        override fun getCount(): Int {
            return reservations.size
        }

        override fun getItem(position: Int): Any {
            return reservations[position]
        }

        override fun getItemId(position: Int): Long {
            return 0
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val rootView = layoutInflater.inflate(R.layout.row_reservation,parent, false)
            rootView.findViewById<TextView>(R.id.textViewReservationName).text = reservations[position].house?.name
            rootView.findViewById<TextView>(R.id.textViewReservationGuestsNumber).text = "${reservations[position].guestsNumber.toString()} pessoas"
            rootView.findViewById<TextView>(R.id.textViewReservationInitialDate).text = reservations[position].init_date.toString()
            rootView.findViewById<TextView>(R.id.textViewReservationFinalDate).text = reservations[position].end_date.toString()
            rootView.findViewById<TextView>(R.id.textViewReservationState).text = reservations[position].reservationStates?.state
            if (reservations[position].house?.images != null ) {
                val avatar = rootView.findViewById<ImageView>(R.id.imageView)

                val firstImage = reservations[position].house?.images?.firstOrNull()
                if (firstImage != null) {
                    val imageUrl = "${Backend.BASE_API}/Houses/${firstImage.image}${firstImage.formato}"
                    println(imageUrl)
                    Glide.with(this@UserReservationsList)
                        .asBitmap()
                        .load(imageUrl)
                        .transition(BitmapTransitionOptions.withCrossFade())
                        .transform(CircleCrop())
                        .into(avatar)
                }
            }
            val buttonCancel = rootView.findViewById<Button>(R.id.buttonCancelReservation)
            val buttonReact = rootView.findViewById<TextView>(R.id.textViewFeedback)

            if(reservations[position].reservationStates?.id == 1 || reservations[position].reservationStates?.id == 2){
                buttonCancel.visibility = View.VISIBLE
            }
            else
                buttonCancel.visibility = View.GONE



            if (reservations[position].reservationStates?.id == 3) {
                val reservationId = reservations[position].id_reservation
                Backend.GetReservationFeedbacks(lifecycleScope, reservationId.toString().toInt()) { feedbacks ->
                    println(feedbacks)
                    if (feedbacks) {
                        buttonReact.visibility = View.GONE
                    } else {
                        buttonReact.visibility = View.VISIBLE
                    }
                }
            } else {
                buttonReact.visibility = View.GONE
            }

            buttonCancel.setOnClickListener {
                val reservationToCancel = reservations[position].id_reservation
                Backend.CancelReservation(this@UserReservationsList, lifecycleScope,reservationToCancel.toString().toInt()) { isSuccess ->
                    if (isSuccess) {
                        reservations.clear()

                        Backend.GetUserReservations(this@UserReservationsList, lifecycleScope) { fetchedReservations ->
                            reservations.addAll(fetchedReservations)
                            if(!reservations.isEmpty()){
                                binding.textViewNoReservations.visibility = View.GONE
                                notifyDataSetChanged()
                            }
                        }
                    }
                }
            }

            val feedbackButton = rootView.findViewById<TextView>(R.id.textViewFeedback)
            feedbackButton.setOnClickListener {

                val intent = Intent(this@UserReservationsList,FeedbackActivity::class.java )
                intent.putExtra(FeedbackActivity.DATA_RESERVATION, reservations[position].id_reservation)
                startActivity(intent)
            }

            return rootView
        }

    }

}