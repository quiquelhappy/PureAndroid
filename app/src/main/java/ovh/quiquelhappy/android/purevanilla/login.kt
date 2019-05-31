package ovh.quiquelhappy.android.purevanilla

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import kotlinx.android.synthetic.main.activity_login.*
import de.mateware.snacky.Snacky
import com.google.gson.JsonParser
import com.squareup.picasso.Picasso


class login : AppCompatActivity() {

    val PREFS_FILENAME = "ovh.quiquelhappy.android.purevanilla.prefs"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        var uuid:String?=null
        var hash:String?=null
        var username:String?=null

        val prefs = this.getSharedPreferences(PREFS_FILENAME, 0)

        fun getSession(){
            login_gif.setImageResource(R.drawable.loading)

            try_login.visibility=View.VISIBLE
            finish_login.visibility=View.GONE
            login_gif.visibility= View.VISIBLE

            "https://www.purevanilla.es/api/sessions/get"
                .httpGet()
                .responseString { request, response, result ->
                    when (result) {
                        is Result.Failure -> {
                            login_gif.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.no_conn))
                            Snacky.builder().setActivity(this).setText(result.getException().message.toString()).setDuration(Snacky.LENGTH_SHORT).build().show()
                        }
                        is Result.Success -> {
                            val data = result.get()
                            val jsonObject = JsonParser().parse(data).asJsonArray
                            if(jsonObject.size()>0){
                                val player = jsonObject[0].asJsonObject.get("name").asString
                                Snacky.builder().setActivity(this).setText("Welcome back $player!").setDuration(Snacky.LENGTH_SHORT).build().show()
                                Picasso.get().load("https://minotar.net/avatar/$player").into(pic)

                                try_login.visibility=View.GONE
                                finish_login.visibility=View.VISIBLE
                                login_gif.visibility= View.GONE

                                uuid=jsonObject[0].asJsonObject.get("uuid").asString
                                hash=jsonObject[0].asJsonObject.get("hash").asString
                                username=jsonObject[0].asJsonObject.get("name").asString

                            } else {
                                Snacky.builder().setActivity(this).setText("There isn't any active sessions with this IP. Make sure you have played the server recently and use the same router as your computer.").setDuration(Snacky.LENGTH_LONG).build().show()
                                login_gif.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.no_conn))
                            }
                        }
                    }
                }
        }

        fun saveSession(){

            if(uuid!=null&&hash!=null){
                "https://www.purevanilla.es/api/sessions/check?uuid=$uuid&hash=$hash"
                    .httpGet()
                    .responseString { request, response, result ->
                        when (result) {
                            is Result.Failure -> {
                                Snacky.builder().setActivity(this).setText(result.getException().message.toString()).setDuration(Snacky.LENGTH_SHORT).build().show()
                            }
                            is Result.Success -> {
                                val data = result.get()
                                val jsonObject = JsonParser().parse(data).asJsonObject
                                if(!jsonObject.get("success").isJsonNull){
                                    Snacky.builder().setActivity(this).setText("Storing your secure account hash").setDuration(Snacky.LENGTH_SHORT).build().show()

                                    val editor = prefs!!.edit()
                                    editor.putString("hash", hash)
                                    editor.putString("uuid", uuid)
                                    editor.putString("username", username)
                                    editor.apply()

                                    val intent = Intent(this, autoload::class.java)
                                    startActivity(intent)
                                    finish()

                                } else {
                                    if(!jsonObject.get("error").isJsonNull){
                                        Snacky.builder().setActivity(this).setText(jsonObject.get("error").asString).setDuration(Snacky.LENGTH_SHORT).build().show()
                                    } else {
                                        Snacky.builder().setActivity(this).setText("Unknown error").setDuration(Snacky.LENGTH_SHORT).build().show()
                                    }
                                }
                            }
                        }
                    }
            } else {
                Snacky.builder().setActivity(this).setText("Invalid local hash").setDuration(Snacky.LENGTH_SHORT).build().show()
            }

        }

        getSession()

        try_login.setOnClickListener {
            getSession()
        }

        finish_login.setOnClickListener {
            saveSession()
        }

    }
}