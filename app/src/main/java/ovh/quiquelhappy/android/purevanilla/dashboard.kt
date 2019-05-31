package ovh.quiquelhappy.android.purevanilla

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.google.gson.JsonParser
import de.mateware.snacky.Snacky
import kotlinx.android.synthetic.main.activity_dashboard.*
import org.jetbrains.anko.*
import com.getkeepsafe.taptargetview.TapTargetView
import com.getkeepsafe.taptargetview.TapTarget
import android.view.ViewGroup
import com.squareup.picasso.Picasso
import android.content.Intent
import android.graphics.Bitmap
import android.os.Handler
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.DecelerateInterpolator


class dashboard : AppCompatActivity() {

    val PREFS_FILENAME = "ovh.quiquelhappy.android.purevanilla.prefs"

    private lateinit var textMessage: TextView
    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                fadeAll()

                getVotingSites()

                textMessage.setText(R.string.title_home)

                Handler().postDelayed({
                    voting.visibility= View.VISIBLE
                    stats.visibility= View.GONE
                    store.visibility= View.GONE
                }, 200)

                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dashboard -> {
                fadeAll()

                textMessage.setText(R.string.title_dashboard)

                loadPopularityGraph()
                loadDonors()
                loadTotal()

                Handler().postDelayed({
                    voting.visibility= View.GONE
                    stats.visibility= View.VISIBLE
                    store.visibility= View.GONE

                    val prefs = this.getSharedPreferences(PREFS_FILENAME, 0)

                    if(!prefs.getBoolean("profiletutorial", false)){

                        val editor = prefs.edit()
                        editor.putBoolean("profiletutorial", true)
                        editor.apply()

                        TapTargetView.showFor(this, // `this` is an Activity
                            TapTarget.forView(profile_image,
                                "Click here to see your profile!",
                                "Here you can always see your achievements and statistics"
                            ).outerCircleColor(R.color.colorPrimary).drawShadow(false).cancelable(true).tintTarget(false))
                    }


                }, 200)

                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_notifications -> {
                fadeAll()

                textMessage.setText(R.string.title_notifications)

                loadStore()

                Handler().postDelayed({
                    voting.visibility= View.GONE
                    stats.visibility= View.GONE
                    store.visibility= View.VISIBLE
                }, 200)

                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onResume() {
        super.onResume()
        Handler().postDelayed({
            getVotingSites()
        }, 5000)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        setSupportActionBar(toolbar)

        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        getVotingSites()
        getProfilePicture(this)

        textMessage = tabname
        navView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)

        refresh.setOnRefreshListener {
            getVotingSites()
            loadPopularityGraph()
            loadDonors()
            loadTotal()
        }

        searchuser.setOnClickListener {
            alert("Coming soon! Keep the app updated!").show()
        }

        searchuser.setOnFocusChangeListener { v, hasFocus ->
            if(hasFocus){
                alert("Coming soon! Keep the app updated!").show()
            }
        }
    }

    private fun fadeAll(){
        val fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.interpolator = DecelerateInterpolator() //add this
        fadeIn.duration = 200

        val fadeOut = AlphaAnimation(1f, 0f)
        fadeOut.interpolator = AccelerateInterpolator() //and this
        fadeOut.duration = 200

        wrapscroll.startAnimation(fadeOut)
        Handler().postDelayed({
            wrapscroll.startAnimation(fadeIn)
        }, 200)
    }

    private fun getVotingSites(){

        val prefs = this.getSharedPreferences(PREFS_FILENAME, 0)
        val uuid = prefs!!.getString("uuid", null)

        refresh.isRefreshing=true

        "https://www.purevanilla.es/api/voting/last?uuid=$uuid"
            .httpGet()
            .responseString { request, response, result ->
                when (result) {
                    is Result.Failure -> {
                        Snacky.builder().setActivity(this).setText(result.getException().message.toString()).setDuration(Snacky.LENGTH_SHORT).build().show()
                        refresh.isRefreshing=false
                    }
                    is Result.Success -> {
                        refresh.isRefreshing=false
                        val data = result.get()
                        val jsonObject = JsonParser().parse(data).asJsonArray

                        voting.removeAllViewsInLayout()

                        repeat(jsonObject.size()){
                            val site=jsonObject[it].asJsonObject

                            var view: View
                            if(site.get("voted").asBoolean){
                                view=getVoteCard(site.get("name").asString, site.get("url").asString, true)
                                voting.addView(view)
                            } else {
                                view=getVoteCard(site.get("name").asString, site.get("url").asString, false)
                                voting.addView(view)
                            }
                            if(it==0&&!prefs.getBoolean("votingtutorial", false)){

                                val editor = prefs.edit()
                                editor.putBoolean("votingtutorial", true)
                                editor.apply()

                                TapTargetView.showFor(this, // `this` is an Activity
                                    TapTarget.forView(((view as ViewGroup).getChildAt(0) as ViewGroup).getChildAt(1),
                                        "Click on any card to vote for the server!",
                                        "You will see the pages in which you have already voted with a green icon"
                                    ).outerCircleColor(R.color.colorPrimary).drawShadow(false).cancelable(true))
                            }
                        }
                    }
                }
            }
    }

    private fun loadPopularityGraph(){

        popularitygraph.settings.javaScriptEnabled = true
        popularitygraph.settings.setSupportMultipleWindows(false)
        popularitygraph.loadUrl("https://www.purevanilla.es/api/stats/popularity/iframe/index.html")

        popularitygraph.setOnTouchListener({ v, event -> true })

    }

    private fun loadDonors(){
        refresh.isRefreshing=true
        "https://www.purevanilla.es/api/store/recent"
            .httpGet()
            .responseString { request, response, result ->
                when (result) {
                    is Result.Failure -> {
                        Snacky.builder().setActivity(this).setText(result.getException().message.toString()).setDuration(Snacky.LENGTH_SHORT).build().show()
                        refresh.isRefreshing=false
                    }
                    is Result.Success -> {
                        refresh.isRefreshing=false

                        recentdonors.removeAllViewsInLayout()

                        val data = result.get()
                        val jsonObject = JsonParser().parse(data).asJsonArray

                        repeat(15){
                            val username = jsonObject[it].asJsonObject.get("username").asString
                            val packagename = jsonObject[it].asJsonObject.get("package").asString
                            if(username!="invalid"){

                                val view = getDonorCard(username, packagename)
                                recentdonors.addView(view)

                                view.setOnClickListener {
                                    val intent = Intent(this, ovh.quiquelhappy.android.purevanilla.profile::class.java)
                                    intent.putExtra("username", username)
                                    startActivity(intent)
                                }

                            }
                        }
                    }
                }
            }
    }

    private fun loadTotal(){
        refresh.isRefreshing=true
        "https://www.purevanilla.es/api/stats/total"
            .httpGet()
            .responseString { request, response, result ->
                when (result) {
                    is Result.Failure -> {
                        Snacky.builder().setActivity(this).setText(result.getException().message.toString()).setDuration(Snacky.LENGTH_SHORT).build().show()
                        refresh.isRefreshing=false
                    }
                    is Result.Success -> {
                        refresh.isRefreshing=false

                        searchuser.hint="Search the statistics of ${JsonParser().parse(result.get()).asJsonObject.get("count").asInt} users"
                    }
                }
            }
    }

    private fun loadStore(){

        storeweb.settings.javaScriptEnabled = true
        storeweb.settings.setSupportMultipleWindows(false)
        storeweb.loadUrl("https://www.purevanilla.es/store?app=true")

        storeweb.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                storeweb.loadUrl(url)
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                refresh.isRefreshing=true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                view!!.scrollTo(0,0)
                wrapscroll.scrollTo(0,0)
                refresh.isRefreshing=false
            }
        }

    }

    private fun getProfilePicture(context:Context){
        val prefs = context.getSharedPreferences(PREFS_FILENAME, 0)
        val username = prefs!!.getString("username", null)
        if(username!=null){
            Picasso.get().load("https://minotar.net/avatar/$username").into(profile_image)
        }

        profile_image.setOnClickListener {
            val intent = Intent(this, ovh.quiquelhappy.android.purevanilla.profile::class.java)
            intent.putExtra("username", username)
            startActivity(intent)
        }
    }

    private fun getDonorCard(username:String, packagename:String):View{

        val container = LinearLayoutCompat(this)
        val containerlp = LinearLayoutCompat.LayoutParams(matchParent, wrapContent)
        containerlp.setMargins(0,0,0,dip(10))
        container.layoutParams=containerlp

        container.setPadding(dip(20),dip(20),dip(20),dip(20))
        container.orientation= LinearLayoutCompat.VERTICAL
        container.setBackgroundResource(R.drawable.card)

        val topcontainer = LinearLayoutCompat(this)
        val topcontainerlp = LinearLayoutCompat.LayoutParams(matchParent, wrapContent)
        topcontainer.layoutParams=topcontainerlp
        topcontainer.orientation= LinearLayoutCompat.HORIZONTAL
        topcontainer.gravity=Gravity.CENTER_VERTICAL

        val text = TextView(this)
        val textlp = LinearLayoutCompat.LayoutParams(wrapContent, wrapContent)
        textlp.weight=1f
        text.layoutParams=textlp
        text.text="$username, $packagename"

        val profilepic = ImageView(this)
        val profilepiclp = LinearLayoutCompat.LayoutParams(dip(25), dip(25))
        profilepic.layoutParams=profilepiclp

        Picasso.get().load("https://minotar.net/avatar/$username").into(profilepic)


        topcontainer.addView(text)
        topcontainer.addView(profilepic)
        container.addView(topcontainer)

        return container
    }

    private fun getVoteCard(site:String, link:String, voteddone:Boolean):View{

        val container = LinearLayoutCompat(this)
        val containerlp = LinearLayoutCompat.LayoutParams(matchParent, wrapContent)
        containerlp.setMargins(0,0,0,dip(10))
        container.layoutParams=containerlp

        container.setPadding(dip(20),dip(20),dip(20),dip(20))
        container.orientation= LinearLayoutCompat.VERTICAL
        container.setBackgroundResource(R.drawable.card)

        val topcontainer = LinearLayoutCompat(this)
        val topcontainerlp = LinearLayoutCompat.LayoutParams(matchParent, wrapContent)
        topcontainer.layoutParams=topcontainerlp
        topcontainer.orientation= LinearLayoutCompat.HORIZONTAL
        topcontainer.gravity=Gravity.CENTER_VERTICAL

        val text = TextView(this)
        val textlp = LinearLayoutCompat.LayoutParams(wrapContent, wrapContent)
        textlp.weight=1f
        text.layoutParams=textlp
        text.text=site

        val voted = ImageView(this)
        val votedlp = LinearLayoutCompat.LayoutParams(dip(25), dip(25))
        voted.layoutParams=votedlp
        voted.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.ic_voting))
        if(voteddone){
            voted.setColorFilter(this.resources.getColor(R.color.colorPrimary))
        }

        topcontainer.addView(text)
        topcontainer.addView(voted)
        container.addView(topcontainer)

        if(!voteddone){
            container.setOnClickListener {
                val intent = Intent(this, ovh.quiquelhappy.android.purevanilla.voting::class.java)
                intent.putExtra("url", link)
                startActivity(intent)
            }
        } else {
            container.setOnClickListener {
                Snacky.builder().setActivity(this).setText("You have already voted here!").setDuration(Snacky.LENGTH_SHORT).build().show()
            }
        }

        return container
    }
}
