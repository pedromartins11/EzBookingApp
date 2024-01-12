package ipca.utility.bookinghousesapp

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
import ipca.utility.bookinghousesapp.databinding.ActivityAdminUsersListBinding

class AdminUsersList : AppCompatActivity() {

    private lateinit var binding : ActivityAdminUsersListBinding
    private var users = arrayListOf<io.swagger.client.models.User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminUsersListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Backend.GetAllUsers(this, lifecycleScope) { fetchedUsers ->
            users.addAll(fetchedUsers)
            setupListView()
        }

        binding.imageViewBack.setOnClickListener {
            onBackPressed()
        }

    }

    private fun setupListView() {
        val adapter = UserListAdapter()
        binding.listViewUsers.adapter = adapter
    }

    inner class UserListAdapter : BaseAdapter() {

        override fun getCount(): Int {
            return users.size
        }

        override fun getItem(position: Int): Any {
            return users[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val rootView = layoutInflater.inflate(R.layout.row_user,parent, false)
            rootView.findViewById<TextView>(R.id.TextViewNomeUser).text = users[position].name
            rootView.findViewById<TextView>(R.id.TextViewEmailUser).text = users[position].email
            rootView.findViewById<TextView>(R.id.TextViewPhoneUser).text = users[position].phone.toString()
            if(users[position].status==true){
                rootView.findViewById<TextView>(R.id.TextViewStateUser).text = "Ativo"
            }
            else if(users[position].status==false)
                rootView.findViewById<TextView>(R.id.TextViewStateUser).text = "Inativo"
            val avatar = rootView.findViewById<ImageView>(R.id.imageView7)

            if(users[position].image != null || users[position].imageFormat != null){
                val imageUrl = "${Backend.BASE_API}/Users/${users[position].image}${users[position].imageFormat}"
                Glide.with(this@AdminUsersList)
                    .asBitmap()
                    .load(imageUrl)
                    .transition(BitmapTransitionOptions.withCrossFade())
                    .transform(CircleCrop())
                    .into(avatar)
            }
            else{
                Glide.with(this@AdminUsersList)
                    .asBitmap()
                    .load(R.drawable.icons8_person_64)
                    .transition(BitmapTransitionOptions.withCrossFade())
                    .transform(CircleCrop())
                    .into(avatar)
            }


            val buttonDeactivate = rootView.findViewById<Button>(R.id.buttonRemove)
            if (users[position].status == true) {
                buttonDeactivate.visibility = View.VISIBLE
                buttonDeactivate.setBackgroundResource(R.drawable.icons8_scroll_down_50)
            } else {
                buttonDeactivate.setBackgroundResource(R.drawable.icons8_scroll_up_50)
            }



            buttonDeactivate.setOnClickListener{
                val userIdToRemove = users[position].id_user
                Backend.DeactivateUser(this@AdminUsersList, lifecycleScope,userIdToRemove.toString().toInt()) { isSuccess ->
                    if (isSuccess) {
                        users.clear()

                        Backend.GetAllUsers(this@AdminUsersList, lifecycleScope) { fetchedUsers ->
                            users.addAll(fetchedUsers)
                            notifyDataSetChanged()
                        }
                    }
                }
            }


            return rootView
        }
    }
}