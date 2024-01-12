package ipca.utility.bookinghousesapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import ipca.utility.bookinghousesapp.databinding.ActivityAdminReservationsListBinding
import java.time.LocalDateTime
import java.util.Date
import java.text.SimpleDateFormat
import java.util.*

class AdminReservationsList : AppCompatActivity() {

    private lateinit var binding : ActivityAdminReservationsListBinding
    var reservations = arrayListOf<io.swagger.client.models.Reservation>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminReservationsListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Backend.GetAllReservations(this, lifecycleScope) { fetchedReservations ->
            reservations.addAll(fetchedReservations)
            setupListView()
        }

        binding.imageViewBack.setOnClickListener{
            onBackPressed()
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
            val rootView = layoutInflater.inflate(R.layout.row_adminreservations,parent, false)
            rootView.findViewById<TextView>(R.id.textViewReservationName).text = reservations[position].house?.name
            rootView.findViewById<TextView>(R.id.textViewReservationGuestsNumber).text = "${reservations[position].guestsNumber.toString()} pessoas"
            rootView.findViewById<TextView>(R.id.textViewReservationInitialDate).text = reservations[position].init_date.toString()
            rootView.findViewById<TextView>(R.id.textViewReservationFinalDate).text = reservations[position].end_date.toString()

            if (reservations[position].house?.images != null ) {
                val avatar = rootView.findViewById<ImageView>(R.id.imageView)

                val firstImage = reservations[position].house?.images?.firstOrNull()
                if (firstImage != null) {
                    val imageUrl = "${Backend.BASE_API}/Houses/${firstImage.image}${firstImage.formato}"
                    println(imageUrl)
                    Glide.with(this@AdminReservationsList)
                        .asBitmap()
                        .load(imageUrl)
                        .transition(BitmapTransitionOptions.withCrossFade())
                        .transform(CircleCrop())
                        .into(avatar)
                }
            }


            return rootView
        }

    }
}