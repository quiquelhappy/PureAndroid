package ovh.quiquelhappy.android.purevanilla

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_voting.*
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient


class voting : AppCompatActivity() {

    val PREFS_FILENAME = "ovh.quiquelhappy.android.purevanilla.prefs"
    var activity:Context?=null;

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voting)

        activity=this

        val prefs = this.getSharedPreferences(PREFS_FILENAME, 0)
        val username = prefs!!.getString("username", null)

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("username", username)
        clipboard.primaryClip = clip

        Handler().postDelayed({
            close.visibility= View.VISIBLE
        }, 60000)

        val url = intent.getStringExtra("url")
        browser.settings.javaScriptEnabled = true
        browser.settings.setSupportMultipleWindows(true)
        browser.loadUrl(url)
        var changecount = 0

        closebutton.setOnClickListener {
            finish()
        }

        browser.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                (activity as voting).finish()
                return false
            }
            override fun onPageFinished(view: WebView?, weburl: String?) {

                changecount+=1

                if(changecount>1){
                    (activity as voting).finish()
                    finish()
                }
            }
        }
    }
}
