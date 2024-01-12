package ipca.utility.bookinghousesapp

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import ipca.utility.bookinghousesapp.databinding.ActivityMainBinding
import ipca.utility.bookinghousesapp.databinding.ActivityNotificationsBinding

class NotificationsActivity : AppCompatActivity() {

    private lateinit var binding : ActivityNotificationsBinding
    private lateinit var bottomNavigationView: BottomNavigationView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.menu.findItem(R.id.notifications).isChecked = true
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> startActivity(Intent(this, MainActivity::class.java))
                R.id.notifications -> startActivity(Intent(this, NotificationsActivity::class.java))
                R.id.profile -> startActivity(Intent(this, ProfilePageActivity::class.java))
            }

            true
        }

        val recyclerView: RecyclerView = binding.RecycleViewNotifications
        val notificationsList = showNotificationsFromStorage()
        Log.d("teste", notificationsList.toString())
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager

        val adapter = NotificationsAdapter(notificationsList)
        recyclerView.adapter = adapter

    }

    private fun showNotificationsFromStorage(): List<String> {
        val sharedPreferences = applicationContext.getSharedPreferences("notifications", Context.MODE_PRIVATE)
        val notificationCount = sharedPreferences.getInt("notification_count", 0)
        Log.d("teste", sharedPreferences.toString())
        Log.d("teste", notificationCount.toString())
        val notificationsList = mutableListOf<String>()

        for (i in 1..notificationCount) {
            val notification = sharedPreferences.getString("notification_$i", "")
            if (!notification.isNullOrBlank()) {
                notificationsList.add(notification)
            }
        }

        return notificationsList
    }

}

class NotificationsAdapter(private val notifications: List<String>) : RecyclerView.Adapter<NotificationsAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.textView3)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification = notifications[position]
        holder.titleTextView.text = notification
    }

    override fun getItemCount(): Int {
        return notifications.size
    }


}

