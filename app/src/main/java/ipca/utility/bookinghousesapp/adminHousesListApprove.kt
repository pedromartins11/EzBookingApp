package ipca.utility.bookinghousesapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ipca.utility.bookinghousesapp.databinding.ActivityAdminUsersListBinding
import ipca.utility.bookinghousesapp.databinding.ActivityHousedetailBinding
import android.widget.Button
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import ipca.utility.bookinghousesapp.databinding.ActivityAdminHousesApproveListBinding


class AdminHousesListApprove : AppCompatActivity() {

    private lateinit var binding:ActivityAdminHousesApproveListBinding
    private var houses = arrayListOf<io.swagger.client.models.House>()
    var token = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminHousesApproveListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.imageViewBack.setOnClickListener{
            onBackPressed()
        }

        Backend.fetchAllHousesSusp(lifecycleScope) { fetchedHouses ->
            houses.addAll(fetchedHouses)
            if (!houses.isEmpty()) {
                binding.textViewHousesNotFound.visibility = View.GONE
                setupListView()
            }
        }
    }

    private fun setupListView() {
        val adapter = HouseListAdapter()
        binding.listViewHousesSusp.adapter = adapter
    }

    inner class HouseListAdapter : BaseAdapter() {

        override fun getCount(): Int {
            return houses.size
        }

        override fun getItem(position: Int): Any {
            return houses[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val rootView = layoutInflater.inflate(R.layout.row_house_approve,parent, false)
            rootView.findViewById<TextView>(R.id.textViewHouseName).text = houses[position].name
            rootView.findViewById<TextView>(R.id.textViewRuaHouse).text = houses[position].road
            rootView.findViewById<TextView>(R.id.textViewNumbP).text = "${houses[position].guestsNumber.toString()}  Pessoas"
            if (houses[position].images != null ) {
                val avatar = rootView.findViewById<ImageView>(R.id.imageViewHouse)

                val firstImage = houses[position].images?.firstOrNull()
                if (firstImage != null) {
                    val imageUrl = "${Backend.BASE_API}/Houses/${firstImage.image}${firstImage.formato}"

                    Glide.with(this@AdminHousesListApprove)
                        .asBitmap()
                        .load(imageUrl)
                        .transition(BitmapTransitionOptions.withCrossFade())
                        .into(avatar)
                }
            }

            val approveButton = rootView.findViewById<ImageView>(R.id.buttonAproveHouse)
            approveButton.setOnClickListener {
                val id_house = houses[position].id_house ?: -1

                print("ID DA HOUSE:")
                println(houses[position].id_house)
                println(id_house)


                val db = FirebaseFirestore.getInstance()
                val tokenRef = db.collection("tokens").document(houses[position].user?.id_user.toString())
                tokenRef.get()
                    .addOnSuccessListener { documentSnapshot ->
                        if (documentSnapshot.exists()) {
                            token = documentSnapshot.getString("token")!!
                            Log.d("testeeee",token.toString())
                        }
                    }

                Backend.sendNotitication(token, "Alojamento",
                    "Alojamento - O seu alojamento ${houses[position].name} foi Aprovado para Alugueres",lifecycleScope){
                }

                Backend.updateHouseStateApproved(id_house, lifecycleScope) {

                    houses.removeAt(position)
                    notifyDataSetChanged()
                }
            }

            val declineButton = rootView.findViewById<ImageView>(R.id.buttonDecHouse)
            declineButton.setOnClickListener {
                val id_house = houses[position].id_house ?: -1

                Backend.deleteHouseById(id_house, lifecycleScope) {

                    houses.removeAt(position)
                    notifyDataSetChanged()
                }
            }

            val constraintLayout = rootView.findViewById<ConstraintLayout>(R.id.constraintLayoutApprove)
            constraintLayout.setOnClickListener {
                val houseId = houses[position].id_house ?: -1

                val intent = Intent(rootView.context, HouseDetailActivity::class.java)
                intent.putExtra("HOUSE_ID", houseId)
                rootView.context.startActivity(intent)
            }




            return rootView
        }
    }

}
