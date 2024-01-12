package ipca.utility.bookinghousesapp

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import ipca.utility.bookinghousesapp.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone


class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var binding : ActivityMainBinding
    private lateinit var textShowData: TextView
    private lateinit var getData: TextView
    private lateinit var progressDialog: ProgressDialog
    var houses = arrayListOf<io.swagger.client.models.House>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.menu.findItem(R.id.home).isChecked = true
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> startActivity(Intent(this, MainActivity::class.java))
                R.id.notifications -> startActivity(Intent(this, NotificationsActivity::class.java))
                R.id.profile -> startActivity(Intent(this, ProfilePageActivity::class.java))
            }
            true
        }

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("A carregar...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        val recyclerView = binding.RecycleViewHouses
        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = HouseAdapter(houses)
        recyclerView.adapter = adapter

        var buttonSearch = binding.buttonSearchFilter
        var startDate: LocalDateTime? = null
        var endDate: LocalDateTime? = null



        textShowData = binding.textViewCheckInOut

        textShowData.setOnClickListener {
            val picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTheme(R.style.ThemeMaterialCalendar)
                .setTitleText("Selecione a Data de Estadia")
                .setSelection(Pair(null,null))
                .build()

            picker.show(this.supportFragmentManager, "TAG")

            picker.addOnPositiveButtonClickListener {
                textShowData.setText(convertTimeToDate(it.first) + " - " +convertTimeToDate(it.second))
                startDate = convertTimeToDatee(it.first)
                endDate = convertTimeToDatee(it.second)

            }
            picker.addOnNegativeButtonClickListener {
                picker.dismiss()
            }
        }

        Backend.fetchAllHouses().observe(this){
            it.onError {error ->
                Toast.makeText(
                    this@MainActivity,
                    "Erro:${error.error}",
                    Toast.LENGTH_LONG
                ).show()
            }
            it.onNetworkError {
                Toast.makeText(
                    this@MainActivity,
                    "Sem Ligação à Internet",
                    Toast.LENGTH_LONG
                ).show()
            }
            it.onSuccess {houses ->
                houses?.let {
                    adapter.updateData(houses.toList())
                }
                progressDialog.dismiss()
            }
        }



        buttonSearch.setOnClickListener {
            val editTextLocalidade = binding.editTextTextLocalidade.text.toString()
            val editTextGuests = binding.editTextTextGuests.text.toString().toIntOrNull()
            val radiobuttonRoom = binding.radioButtonRoom.isChecked

            Backend.filterHouses(editTextLocalidade,editTextGuests,radiobuttonRoom,startDate, endDate).observe(this){
                it.onError {error ->
                    Toast.makeText(
                        this@MainActivity,
                        "Erro:${error.error}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                it.onNetworkError {
                    Toast.makeText(
                        this@MainActivity,
                        "Sem Ligação à Internet",
                        Toast.LENGTH_LONG
                    ).show()
                }
                it.onSuccess { houses ->
                    houses?.let {
                        adapter.updateData(houses.toList())
                    }
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

    class HouseAdapter(private var houses: List<io.swagger.client.models.House>) : RecyclerView.Adapter<HouseAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val nameTextView: TextView = itemView.findViewById(R.id.textViewNameHouse)
            val DCTextView: TextView = itemView.findViewById(R.id.textViewDC)
            val FeedTextView: TextView = itemView.findViewById(R.id.textViewMedFeedbackH)
            val HouseRoomTextView: TextView = itemView.findViewById(R.id.textViewQH)
            val priceTextView: TextView = itemView.findViewById(R.id.textViewPNY)
            val viewPager: ViewPager2 = itemView.findViewById(R.id.viewPager)
            val tabLayout : TabLayout = itemView.findViewById(R.id.tabLayout)

            init {
                itemView.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val clickedHouse = houses[position]
                        val intent = Intent(itemView.context, HouseDetailActivity::class.java)
                        intent.putExtra("HOUSE_ID", clickedHouse.id_house)
                        itemView.context.startActivity(intent)
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.row_house, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val house = houses[position]
            var DC = "${house.postalCode?.district}, ${house.postalCode?.concelho}"
            val displayText = if (house.priceyear != null) {
                "${house.priceyear}€ / Ano"
            } else {
                "${house.price}€ / Noite"
            }
            var imageUrls = house.images?.map { imageName ->
                "${Backend.BASE_API}/Houses/${imageName.image}${imageName.formato}"
            } ?: emptyList()
            val averageFeedback = calculateAverageFeedback(house.reservations)


            holder.nameTextView.text = house.name
            holder.DCTextView.text = DC
            holder.FeedTextView.text = if (averageFeedback == 0.0) {""}
            else averageFeedback.toString()
            holder.HouseRoomTextView.text = if (house.sharedRoom== false) "Casa" else "Quarto"
            holder.priceTextView.text = displayText

            val imagePagerAdapter = ImagePagerAdapter(imageUrls, holder.itemView.context as FragmentActivity)
            holder.viewPager.adapter = imagePagerAdapter
            TabLayoutMediator(holder.tabLayout, holder.viewPager) { tab, position ->
            }.attach()

        }


        override fun getItemCount(): Int {
            return houses.size
        }

        fun updateData(newHouses: List<io.swagger.client.models.House>) {
            houses = newHouses
            notifyDataSetChanged()
        }

        fun calculateAverageFeedback(reservations: Array<io.swagger.client.models.Reservation>?): Double {
            var cont = 0
            if (reservations.isNullOrEmpty()) {
                return 0.0
            }

            var totalFeedback = 0.0
            reservations.forEach { reservation ->
                Log.d("teste",  reservation.toString())
                reservation.feedback?.let {
                    totalFeedback += it.classification!!
                    cont++
                }
            }

            return totalFeedback.toDouble() / cont
        }
    }

     class ImagePagerAdapter(private val imageUrls: List<String>, activity: FragmentActivity) :
        FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = imageUrls.size

        override fun createFragment(position: Int): Fragment {
            return HouseDetailActivity.ImageFragment(imageUrls[position])
        }
    }

    inner class ImageFragment(private val imageUrl: String) : Fragment() {

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            Log.d("ImageFragment", "onCreateView called")
            val view = inflater.inflate(R.layout.image_view_carrousel, container, false)
            val imageView = view.findViewById<ImageView>(R.id.idIVImage)

            Glide.with(this)
                .load(imageUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imageView)


            return view
        }
    }





}