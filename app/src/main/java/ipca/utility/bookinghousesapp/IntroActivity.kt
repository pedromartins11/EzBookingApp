package ipca.utility.bookinghousesapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView

class IntroActivity : AppCompatActivity() {
    private lateinit var topAnim: Animation
    private lateinit var bottomAnim: Animation
    private lateinit var logo: ImageView
    private lateinit var slogan: TextView
    val INTRO : Long  = 5000
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_intro)

        topAnim = AnimationUtils.loadAnimation(this,R.anim.top_animation)
        bottomAnim = AnimationUtils.loadAnimation(this,R.anim.bottom_animation)

        logo = findViewById(R.id.imageViewLogo)
        slogan = findViewById(R.id.textViewSlogan)

        logo.startAnimation(topAnim)
        slogan.startAnimation(bottomAnim)

        Handler().postDelayed({
            val intent = Intent(this,LoginActivity::class.java )
            startActivity(intent)
            finish()
        }, INTRO)
    }
}