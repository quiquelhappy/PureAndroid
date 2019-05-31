package ovh.quiquelhappy.android.purevanilla

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_profile.*
import android.graphics.ColorMatrixColorFilter
import android.graphics.ColorMatrix
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.google.gson.JsonParser
import de.mateware.snacky.Snacky
import kotlinx.android.synthetic.main.activity_profile.profile_image
import kotlinx.android.synthetic.main.activity_profile.toolbar
import org.jetbrains.anko.*
import android.content.Intent
import android.view.MenuItem


class profile : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val uuid = intent.getStringExtra("uuid")
        val username = intent.getStringExtra("username")

        setSupportActionBar(toolbar)

        val actionBar = supportActionBar
        actionBar!!.title = username
        actionBar.setDisplayHomeAsUpEnabled(true)

        if(uuid==null&&username!=null){
            Picasso.get().load("https://minotar.net/avatar/$username").into(profile_image)
            Picasso.get().load("https://minotar.net/avatar/$username").into(profile_image_blured)
            val matrix = ColorMatrix()
            matrix.setSaturation(0f)
            val filter = ColorMatrixColorFilter(matrix)
            profile_image_blured.colorFilter = filter
        } else {
            Picasso.get().load("https://minotar.net/avatar/$uuid").into(profile_image)
            Picasso.get().load("https://minotar.net/avatar/$uuid").into(profile_image_blured)
        }

        fun getUser(username:String){
            profilerefresh.isRefreshing=true
            "https://www.purevanilla.es/api/user/profile?username=$username"
                .httpGet()
                .responseString { request, response, result ->
                    when (result) {
                        is Result.Failure -> {
                            profilerefresh.isRefreshing=false
                            Snacky.builder().setActivity(this).setText(result.getException().message.toString()).setDuration(Snacky.LENGTH_SHORT).build().show()
                            finish()
                        }
                        is Result.Success -> {
                            profilerefresh.isRefreshing=false

                            val data = JsonParser().parse(result.get()).asJsonObject

                            if(!data.get("geolocation").asJsonObject.get("country").isJsonNull){
                                val country = data.get("geolocation").asJsonObject.get("country").asString
                                if(!data.get("geolocation").asJsonObject.get("state").isJsonNull){
                                    val state = data.get("geolocation").asJsonObject.get("state").asString
                                    location.text = "$state, $country"
                                } else {
                                    location.text = "$country"
                                }
                            } else {
                                location.visibility= View.GONE
                            }

                            if(!data.get("advancements").isJsonNull){
                                val advancements= data.get("advancements").asJsonArray

                                repeat(advancements.size()){
                                    val current= advancements.get(it).asJsonObject

                                    val server = current.get("server").asString
                                    val advancementcurrent = current.get("advancements").asJsonArray


                                    var count = 0
                                    var lastrow:LinearLayout

                                    val row = LinearLayout(this)
                                    row.orientation=LinearLayout.HORIZONTAL
                                    row.gravity=Gravity.CENTER_VERTICAL or Gravity.LEFT
                                    val rowlp = LinearLayout.LayoutParams(matchParent, wrapContent)
                                    row.layoutParams=rowlp
                                    lastrow=row

                                    if(server=="cool"){
                                        cooladvancements.removeAllViews()
                                        cooladvancements.addView(lastrow)
                                    } else {
                                        simplyadvancements.removeAllViews()
                                        simplyadvancements.addView(lastrow)
                                    }

                                    repeat(advancementcurrent.size()){selected->

                                        val advcurrent = advancementcurrent.get(selected).asJsonObject

                                        val advname = advcurrent.get("meta").asJsonObject.get("name").asString
                                        val advdesc =  advcurrent.get("meta").asJsonObject.get("desc").asString
                                        val pic =  advcurrent.get("meta").asJsonObject.get("pic").asString

                                        if(count>=4){

                                            count=0

                                            val row = LinearLayout(this)
                                            row.orientation=LinearLayout.HORIZONTAL
                                            row.gravity=Gravity.CENTER_VERTICAL or Gravity.LEFT
                                            val rowlp = LinearLayout.LayoutParams(matchParent, wrapContent)
                                            row.layoutParams=rowlp
                                            lastrow=row

                                            if(server=="cool"){
                                                cooladvancements.addView(lastrow)
                                            } else {
                                                simplyadvancements.addView(lastrow)
                                            }

                                        }

                                        count += 1

                                        lastrow.addView(getAdvCard(advname,advdesc,null,pic))

                                    }

                                }
                            }

                        }
                    }
                }
        }

        getUser(username)

        profilerefresh.setOnRefreshListener {
            getUser(username)
        }

    }

    private fun getAdvCard(name:String, dec:String, tech: String?, pic:String): ImageView {
        val imageview = ImageView(this)
        val imageviewlp = LinearLayout.LayoutParams(dip(70),dip(70))
        imageviewlp.setMargins(dip(10),dip(10),dip(10),dip(10))
        imageview.layoutParams=imageviewlp

        Picasso.get().load(pic).into(imageview)

        return imageview

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        finish()
        return true
    }
}
