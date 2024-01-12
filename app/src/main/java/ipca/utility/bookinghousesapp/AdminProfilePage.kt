package ipca.utility.bookinghousesapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import ipca.utility.bookinghousesapp.databinding.ActivityAdminProfilePageBinding
import ipca.utility.bookinghousesapp.databinding.ActivityProfilePageBinding

class AdminProfilePage : AppCompatActivity() {
    private lateinit var binding : ActivityAdminProfilePageBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminProfilePageBinding.inflate(layoutInflater)
        setContentView(binding.root)


        Backend.fetchUserDetail(this, lifecycleScope) { user ->
            user?.let {
                binding.textViewUserName.text = it.name
                binding.textViewUserEmail.text = it.email

                val imageUrl = "${Backend.BASE_API}/Users/${user.image}${user.imageFormat}"
                Glide.with(this@AdminProfilePage)
                    .asBitmap()
                    .load(imageUrl)
                    .transition(BitmapTransitionOptions.withCrossFade())
                    .transform(CircleCrop())
                    .into(binding.imageView11)
            }
        }

        binding.imageViewBack.setOnClickListener{
            onBackPressed()
        }

        binding.constraintLayoutAllReservations.setOnClickListener{
            val intent = Intent(this,AdminReservationsList::class.java )
            startActivity(intent)
        }

        binding.constraintLayoutAllUsers.setOnClickListener{
            val intent = Intent(this,AdminUsersList::class.java )
            startActivity(intent)
        }

        binding.constraintLayoutAllHouses.setOnClickListener{
            val intent = Intent(this,AdminHousesList::class.java )
            startActivity(intent)
        }

        binding.constraintLayoutApproveHouses.setOnClickListener {
            val intent = Intent(this,AdminHousesListApprove::class.java)
            startActivity(intent)
        }

        binding.constraintLayoutAllPayments.setOnClickListener {
            val intent = Intent(this,AdminPaymentsList::class.java )
            startActivity(intent)
        }
    }
}