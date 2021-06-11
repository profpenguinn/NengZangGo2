package com.example.nengzanggo2
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.Gravity
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.order.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

//음성인식 부분에 필요한거
private const val SPEECH_REQUEST_CODE = 0

class OrderActivity : Activity() {

    val stockHelper = stockDBHelper(this)
    var ingredientList = arrayListOf<ingredient>()
    lateinit var orderListView: ListView

    //D-Day계산 메소드드
    fun fewDay(beginDayYear:Int, beginDayMonth:Int, beginDayDate:Int, lastDayYear:Int, lastDayMonth:Int, lastDayDate:Int):Long{ //두 날짜간 차이 구하기
        //첫번째날
        val beginDay= Calendar.getInstance().apply{
            set(Calendar.YEAR, beginDayYear)
            set(Calendar.MONTH, beginDayMonth)
            set(Calendar.DAY_OF_MONTH, beginDayDate)
        }.timeInMillis

        //두번째날
        val lastDay= Calendar.getInstance().apply{
            set(Calendar.YEAR, lastDayYear)
            set(Calendar.MONTH, lastDayMonth)
            set(Calendar.DAY_OF_MONTH, lastDayDate)
        }.timeInMillis

        val fewDay = getIgnoredTimeDays(lastDay) - getIgnoredTimeDays(beginDay)
        return fewDay / (24*60*60*1000)
    }
    fun getIgnoredTimeDays(time: Long): Long{
        return Calendar.getInstance().apply{
            timeInMillis = time
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE,0)
            set(Calendar.SECOND,0)
            set(Calendar.MILLISECOND,0)
        }.timeInMillis
    }


    override fun onCreate(savedInstanceState: Bundle?) {//
        super.onCreate(savedInstanceState)
        setContentView(R.layout.order)

        title="주문!!"
        var Edit1 : EditText
        var btnSearch : Button
        var btnMic : Button
        var text : String

        Edit1 = findViewById(R.id.Edit1)
        btnSearch = findViewById(R.id.btnSearch)
        btnMic = findViewById(R.id.btnMic)

        btnSearch.setOnClickListener {
            text = Edit1.text.toString()
            var intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.coupang.com/np/search?component=&q=${text}&channel=user/"))
            startActivity(intent)
        }

        //음성인식 버튼
        btnMic.setOnClickListener {
            displaySpeechRecognizer()
        }


        orderListView = findViewById<ListView>(R.id.orderListView)
        val ingredientAdapter = CalendarListAdapter(this, ingredientList) //어댑터 생성
        orderListView.adapter = ingredientAdapter

        val stockDB = stockHelper.readableDatabase
        var cursor = stockDB.rawQuery("SELECT * FROM stockTBL",null)
        while(cursor.moveToNext())
        {
            var n_ingredient : ingredient = ingredient(cursor.getString(0),cursor.getString(1),cursor.getString(2))

            var exDateArray = n_ingredient.time.split(".")
            var exYEAR = exDateArray[0]
            var exMONTH = exDateArray[1]
            var exDAY = exDateArray[2]

            var now = LocalDate.now()
            var nowDate = now.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
            var nowDateArray = nowDate.split(".")
            var nowYEAR = nowDateArray[0]
            var nowMONTH = nowDateArray[1]
            var nowDAY = nowDateArray[2]

            var remaintime = fewDay(nowYEAR.toInt(),nowMONTH.toInt(),nowDAY.toInt(),exYEAR.toInt(),exMONTH.toInt(),exDAY.toInt())

            if(remaintime<= 3) {
                ingredientList.add(n_ingredient) //기한 임박 재료리스트에 추가
                ingredientAdapter.notifyDataSetChanged() //어댑터에 추가 확인시키기(싱크로나이즈)
            }

        }
















        val titleArr = arrayOf("된장찌개", "김치찌개", "순두부찌개", "제육볶음")
        var imageList = intArrayOf(
            R.drawable.img1,
            R.drawable.img2,
            R.drawable.img3,
            R.drawable.img4
        )

        var viewFlipper1 : ViewFlipper = findViewById(R.id.viewFlipper1)
        viewFlipper1.inAnimation = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
        viewFlipper1.outAnimation = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right)

        for (image in imageList) {
            val imageView = ImageView(this)
            val layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutParams.setMargins(30,30,30,0)
            layoutParams.gravity= Gravity.CENTER
            imageView.layoutParams=layoutParams
            imageView.setImageResource(image)
            viewFlipper1.addView(imageView)
        }

        /*for (String in titleArr) {
            var tempText : TextView = TextView(this)
            tempText.gravity = Gravity.CENTER_HORIZONTAL
            tempText.text = String
            viewFlipper1.addView(tempText)
        }*/

        viewFlipper1.flipInterval = 2000
        viewFlipper1.startFlipping()

        val bottomNavigation : BottomNavigationView = findViewById(R.id.btm_nav)
        bottomNavigation.selectedItemId =R.id.order
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                R.id.order -> {
                    startActivity(Intent(this, OrderActivity::class.java))
                    finish()
                }
                R.id.recipe -> {
                    startActivity(Intent(this,RecipeActivity::class.java))
                    finish()
                }
                R.id.calendar -> {
                    startActivity(Intent(this,CalendarActivity::class.java))
                    finish()
                }
            }
            true
        }
        
        
        //음성인식 부분
    }
    private fun displaySpeechRecognizer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        }
        // Start the activity, the intent will be populated with the speech text
        startActivityForResult(intent, SPEECH_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val spokenText: String? =
                data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).let { results ->
                    results?.get(0)
                }
            var intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.coupang.com/np/search?component=&q=${spokenText}&channel=user/"))
            startActivity(intent)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}