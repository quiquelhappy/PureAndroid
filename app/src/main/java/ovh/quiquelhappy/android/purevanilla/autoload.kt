package ovh.quiquelhappy.android.purevanilla

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class autoload : AppCompatActivity() {

    val PREFS_FILENAME = "ovh.quiquelhappy.android.purevanilla.prefs"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = this.getSharedPreferences(PREFS_FILENAME, 0)
        val hash = prefs!!.getString("hash", null)

        var intent: Intent

        if(hash.isNullOrEmpty()){
            intent = Intent(this, login::class.java)
        } else {
            intent = Intent(this, dashboard::class.java)
        }


        startActivity(intent)
        finish()

    }
}
