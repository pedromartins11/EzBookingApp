package ipca.utility.bookinghousesapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ipca.utility.bookinghousesapp.databinding.ActivityAdminUsersListBinding
import ipca.utility.bookinghousesapp.databinding.ActivityHousedetailBinding
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.lifecycle.LifecycleCoroutineScope
import ipca.utility.bookinghousesapp.Backend
import ipca.utility.bookinghousesapp.databinding.ActivityCreateHouseBinding



class CreateHouse : AppCompatActivity() {

    private lateinit var binding: ActivityCreateHouseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateHouseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.imageViewBackH2.setOnClickListener{
            onBackPressed()
        }

        val buttonCreateHouse =
            binding.buttonCreateHouse

        setupCreateHouseButton(buttonCreateHouse, lifecycleScope)
    }

    fun setupCreateHouseButton(buttonCreateHouse :Button, lifecycleScope: LifecycleCoroutineScope) {
        buttonCreateHouse.setOnClickListener {
            createHouseObjectFromUI { body ->
                Backend.CreateHouse(body, this).observe(this) {
                    it.onError { error ->
                        Toast.makeText(
                            this@CreateHouse,
                            "${error.error}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    it.onNetworkError {
                        Toast.makeText(
                            this@CreateHouse,
                            "Sem Ligação à Internet",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    it.onSuccess {
                        Backend.fetchUserDetail(this, lifecycleScope) { user ->
                            Backend.logout(this, lifecycleScope) {
                                val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                                val userPassword = sharedPreferences.getString("password", "")
                                Backend.login(this, lifecycleScope, user.email!!, userPassword!!) { loginSuccess ->
                                    if (loginSuccess) {
                                        val intent = Intent(this, HouseImagesActivity::class.java)
                                        intent.putExtra("HOUSE_NAME", body.name)
                                        startActivity(intent)
                                        finish()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }




    private fun createHouseObjectFromUI(callback: (house: io.swagger.client.models.House) -> Unit) {


        val houseName = binding.editTextNameHouse.text.toString()
        val numbPessoas = binding.editTextNumberPHouse.text.toString().toInt()
        val district = binding.editTextDistrictHouse.text.toString()
        val concelho = binding.editTextConcelhoHouse.text.toString()
        val street = binding.editTextStreetHouse.text.toString()
        val postalCodeValue = binding.editTextPostalCodeHouse.text.toString().toInt()

        val postalCode = io.swagger.client.models.PostalCode(
            postalCode = postalCodeValue,
            concelho = concelho,
            district = district,
        )

        val numbRooms = binding.editTextNumberRoomsHouse.text.toString().toInt()
        val numbFloor = binding.editTextNumberFloorHouse.text.toString().toInt()
        val doorNumber = binding.editTextNumberDoorHouse.text.toString().toInt()
        val price = binding.editTextNumberPriceHouse.text.toString().toDouble()
        val propertyAssessment = binding.editTextpropertyAssessment.text.toString()

        val isAnnualPrice = binding.checkBoxAnualPrice.isChecked
        val isSharedRoom = binding.checkBoxSharedRoom.isChecked

        val sharedPreferences = this.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val id_user = sharedPreferences.getInt("user_id", 0)

        Backend.fetchUserDetail(this, lifecycleScope, id_user) { user ->

            val body = io.swagger.client.models.House(
                name = houseName,
                doorNumber = doorNumber,
                guestsNumber = numbPessoas,
                postalCode = postalCode,
                floorNumber = numbFloor,
                rooms = numbRooms,
                road = street,
                user = user,
                propertyAssessment = propertyAssessment,
                sharedRoom = isSharedRoom,
                price = if (isAnnualPrice) null else price,
                priceyear = if (isAnnualPrice) price else null,
            )

            println(body)
            callback(body)
        }
    }
}
