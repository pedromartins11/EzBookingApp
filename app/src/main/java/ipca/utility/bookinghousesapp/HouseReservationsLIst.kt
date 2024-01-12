package ipca.utility.bookinghousesapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import ipca.utility.bookinghousesapp.databinding.ActivityHouseReservationsListBinding
import ipca.utility.bookinghousesapp.databinding.ActivityUserReservationsListBinding

class HouseReservationsLIst : AppCompatActivity() {
    private lateinit var binding : ActivityHouseReservationsListBinding
    var houseReservations = arrayListOf<io.swagger.client.models.Reservation>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHouseReservationsListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.imageViewBack.setOnClickListener {
            onBackPressed()
        }

        val houseId = intent.extras?.getInt(DATA_HOUSE) ?: -1

        Backend.GetHouseReservations(this, lifecycleScope, houseId) { fetchedReservations ->
            houseReservations.addAll(fetchedReservations)
            if(!houseReservations.isEmpty()){
                binding.textViewNoReservations.visibility = View.GONE
                setupListView()
            }
        }

    }
    private fun setupListView() {
        val adapter = HouseReservationsListAdapter()
        binding.listViewReservations.adapter = adapter
    }

    companion object {
        const val DATA_HOUSE = "data_house"
    }

    inner class HouseReservationsListAdapter : BaseAdapter(){
        override fun getCount(): Int {
            return houseReservations.size
        }

        override fun getItem(position: Int): Any {
            return houseReservations[position]
        }

        override fun getItemId(position: Int): Long {
            return 0
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val rootView = layoutInflater.inflate(R.layout.row_reservation,parent, false)
            rootView.findViewById<TextView>(R.id.textViewReservationName).text = houseReservations[position].house?.name
            rootView.findViewById<TextView>(R.id.textViewReservationGuestsNumber).text = "${houseReservations[position].guestsNumber.toString()} pessoas"
            rootView.findViewById<TextView>(R.id.textViewReservationInitialDate).text = houseReservations[position].init_date.toString()
            rootView.findViewById<TextView>(R.id.textViewReservationFinalDate).text = houseReservations[position].end_date.toString()
            rootView.findViewById<TextView>(R.id.textViewReservationState).text = houseReservations[position].reservationStates?.state
            if (houseReservations[position].house?.images != null ) {
                val avatar = rootView.findViewById<ImageView>(R.id.imageView)

                val firstImage = houseReservations[position].house?.images?.firstOrNull()
                if (firstImage != null) {
                    val imageUrl = "${Backend.BASE_API}/Houses/${firstImage.image}${firstImage.formato}"
                    println(imageUrl)
                    Glide.with(this@HouseReservationsLIst)
                        .asBitmap()
                        .load(imageUrl)
                        .transition(BitmapTransitionOptions.withCrossFade())
                        .transform(CircleCrop())
                        .into(avatar)
                }
            }

            rootView.findViewById<Button>(R.id.buttonCancelReservation).visibility = View.GONE
            rootView.findViewById<TextView>(R.id.textViewFeedback).visibility = View.GONE

            return rootView
        }

    }
}