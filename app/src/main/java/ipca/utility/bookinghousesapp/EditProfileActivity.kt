package ipca.utility.bookinghousesapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import ipca.utility.bookinghousesapp.databinding.ActivityEditProfileBinding
import ipca.utility.bookinghousesapp.databinding.ActivityHousedetailBinding

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    var usersfeed = mutableListOf<io.swagger.client.models.User>()
    private var password: String = ""
    private var token: String = ""
    private var status: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Backend.fetchUserDetail(this, lifecycleScope) { user ->
            user?.let {
                binding.textViewUserName.text = it.name
                val nameEditable = Editable.Factory.getInstance().newEditable(it.name)
                val emailEditable = Editable.Factory.getInstance().newEditable(it.email)
                val phoneEditable = Editable.Factory.getInstance().newEditable(it.phone.toString())

                binding.EditTextUserEditName.text = nameEditable
                binding.EditTextUserEditEmail.text = emailEditable
                binding.EditTextUserEditPhone.text = phoneEditable

                password = it.password.toString()
                token = it.token.toString()
                status = it.status.toString().toBoolean()


                val imageUrl = "${Backend.BASE_API}/Users/${user.image}${user.imageFormat}"
                Glide.with(this@EditProfileActivity)
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

        binding.buttonUpdateUser.setOnClickListener {
            val newUserName = binding.EditTextUserEditName.text.toString()
            val newUserEmail = binding.EditTextUserEditEmail.text.toString()
            val newUserPhone = binding.EditTextUserEditPhone.text.toString().toIntOrNull() ?: 0

            Backend.UpdateUserProfile(this, newUserName, newUserEmail, newUserPhone).observe(this){
                it.onError {error ->
                    Toast.makeText(
                        this@EditProfileActivity,
                        "${error.error}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                it.onNetworkError {
                    Toast.makeText(
                        this@EditProfileActivity,
                        "Sem Ligação à Internet",
                        Toast.LENGTH_LONG
                    ).show()
                }
                it.onSuccess {
                    val intent = Intent(this, ProfilePageActivity::class.java)
                    startActivity(intent)
                    Log.d("UpdateActivity", "Update bem-sucedido!")
                }
            }
        }
    }
}