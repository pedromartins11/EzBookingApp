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
import ipca.utility.bookinghousesapp.databinding.ActivityAdminPaymentsListBinding
import ipca.utility.bookinghousesapp.databinding.ActivityAdminUsersListBinding

class AdminPaymentsList : AppCompatActivity() {
    private lateinit var binding : ActivityAdminPaymentsListBinding
    private var payments = arrayListOf<io.swagger.client.models.Payment>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminPaymentsListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Backend.GetAllPayments(this, lifecycleScope) { fetchedPayments ->
            payments.addAll(fetchedPayments)
            setupListView()
        }

        binding.imageViewBack.setOnClickListener {
            onBackPressed()
        }
    }

    private fun setupListView() {
        val adapter = PaymentListAdapter()
        binding.listViewUsers.adapter = adapter
    }

    inner class PaymentListAdapter : BaseAdapter() {

        override fun getCount(): Int {
            return payments.size
        }

        override fun getItem(position: Int): Any {
            return payments[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val rootView = layoutInflater.inflate(R.layout.row_payment,parent, false)
            rootView.findViewById<TextView>(R.id.TextViewNomeUser).text = payments[position].reservation?.user?.name
            rootView.findViewById<TextView>(R.id.TextViewPaymentDate).text = payments[position].paymentDate.toString()
            rootView.findViewById<TextView>(R.id.TextViewPaymentMethod).text = payments[position].paymentMethod
            rootView.findViewById<TextView>(R.id.TextViewPaymentValue).text = "${payments[position].paymentValue.toString()}€"
            rootView.findViewById<TextView>(R.id.TextViewPaymentState).text = payments[position].state?.state


            val avatar = rootView.findViewById<ImageView>(R.id.imageView7)

            if(payments[position].reservation?.user?.image != null || payments[position].reservation?.user?.imageFormat != null){
                val imageUrl = "${Backend.BASE_API}/Users/${payments[position].reservation?.user?.image}${payments[position].reservation?.user?.imageFormat}"
                Glide.with(this@AdminPaymentsList)
                    .asBitmap()
                    .load(imageUrl)
                    .transition(BitmapTransitionOptions.withCrossFade())
                    .transform(CircleCrop())
                    .into(avatar)
            }
            else{
                Glide.with(this@AdminPaymentsList)
                    .asBitmap()
                    .load(R.drawable.icons8_person_64)
                    .transition(BitmapTransitionOptions.withCrossFade())
                    .transform(CircleCrop())
                    .into(avatar)
            }

            if(payments[position].paymentDate == null){
                rootView.findViewById<TextView>(R.id.TextViewPaymentDate).text = "Pagamento não efetuado"
            }

            return rootView
        }
    }
}