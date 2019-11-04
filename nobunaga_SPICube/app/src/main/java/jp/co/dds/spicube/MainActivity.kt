package jp.co.dds.spicube

import android.graphics.BitmapFactory;
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.annotation.UiThread
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SeekBar

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.json.JSONArray
import java.lang.Exception
import java.util.concurrent.locks.ReentrantLock
import org.json.JSONObject
import java.io.File
import java.util.*

import kotlin.concurrent.thread
import kotlin.concurrent.timerTask
import kotlin.concurrent.withLock

// Message constants

// val MSG_READ_FILE:Int = 1
val MSG_LOAD_IMAGE:Int = 2
val MSG_TICK:Int = 3
val MSG_READ_FILE1 = 4
val MSG_READ_FILE2 = 5



class MainActivity : AppCompatActivity() {
    val mutex = ReentrantLock()
    var stored_transimage = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    var stored_rawimage = stored_transimage

    @UiThread
    override fun onCreate(savedInstanceState: Bundle?) {
        var clickcnt:Int = 1
        var watchdog:Int = 0
        var framesshown:Long = 0
        var framesrecived:Int = 0

        val gainsetting = JSONObject().put("page",1).put("addr",0x24)
        val expsetting = JSONObject().put("addrH",0x3).put("addrL",0x4).put("page",1)
        val local_epoc = System.currentTimeMillis()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        workspaceinit()
        val settingsJSON = JSONObject().put("shortregister",JSONArray().
                                                put(gainsetting)).
                                        put("16register",JSONArray().
                                                put(expsetting)).
                                        put("settings",JSONObject())


        fun write_json(o: JSONObject ){
            // for debug
            gettingstarted.setText(o.toString(4))
            if( pipewriter.write_json(o)){
                messagearea.visibility = View.GONE
                messagearea.setText("")
            }else{
                messagearea.setText("named pipe locked data dropped")
                messagearea.visibility = View.VISIBLE
                watchdog = 10
            }
        }

        // Suppress warning
        // https://stackoverflow.com/questions/11407943/this-handler-class-should-be-static-or-leaks-might-occur-incominghandler
        //
        // Conclusion is if messages do not stay in que long tine this is not an problem

        val handler = @SuppressLint("HandlerLeak")
        object :Handler(){
            var frametime:Long = 0
            override fun handleMessage(msg: Message) {
                when(msg.what) {
                    MSG_READ_FILE1 ->{
                        mutex.withLock {
                            imagearea.setImageBitmap(stored_rawimage)
                            imagearea.visibility = View.VISIBLE
                            gettingstarted.visibility = View.GONE
                        }
                    }
                    MSG_READ_FILE2 ->{
                        mutex.withLock {
                            transformedimage.setImageBitmap(stored_transimage)
                            transformedimage.visibility = View.VISIBLE
                            gettingstarted.visibility = View.GONE
                        }
                    }

                    MSG_TICK -> {
                        if( watchdog > 0) {
                            watchdog-=1
                            when (watchdog ){
                                7 -> if (textViewfps.visibility != View.GONE) textViewfps.visibility = View.INVISIBLE
                                3 -> textViewfps.visibility = View.GONE
                                0 -> mutex.withLock{
                                    textViewfps.visibility = View.GONE
                                    messagearea.visibility = View.GONE
                                }
                            }
                        }
                    }
                }
            }
        }
        saveImgCheckBox.setOnCheckedChangeListener{ buttonView,checked ->
            settingsJSON.getJSONObject("settings")?.put("numsavedimagesmax",if (checked) 100 else 0)
            write_json(settingsJSON)
        }
        gainSeekBar.setOnSeekBarChangeListener (object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                // Display the current progress of SeekBar
                messagearea.text = "Gain :0x" + i.toString(16)+ " ($i) "
                //TODO it would be nicer to find this object in JSON and modify it
                gainsetting.put("data",i)
                watchdog = 10

            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                messagearea.visibility = View.VISIBLE
                watchdog = 10

            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                messagearea.visibility = View.GONE
                write_json(settingsJSON)
            }
        })

        expSeekBar.setOnSeekBarChangeListener (object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                // Display the current progress of SeekBar
                messagearea.text = "Exposure time :0x" + i.toString(16)+ " ($i) "
                watchdog = 10
                expsetting.put("data",i)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                messagearea.visibility = View.VISIBLE
                watchdog = 10

            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                messagearea.visibility = View.GONE
                write_json(settingsJSON)
            }
        })


        airgapSeekBar.setOnSeekBarChangeListener (object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                // Display the current progress of SeekBar
                val gap = i.toFloat()/10;
                messagearea.text = "Air gap $gap"
                //TODO it would be nicer to find this object in JSON and modify it
                settingsJSON.getJSONObject("settings")?.put("airgap",gap)
                watchdog = 10

            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                messagearea.visibility = View.VISIBLE
                watchdog = 10

            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                messagearea.visibility = View.GONE
                write_json(settingsJSON)
            }
        })

        // Receive images in separate threads with a shared image objects

        thread {
            while (true) {
                try {
                    val bm = BitmapFactory.decodeFile("/data/data/jp.co.dds.spicube/rawimage.png")
                    if (bm != null) {
                        mutex.withLock {
                            stored_rawimage = bm;

                        }
                        handler.obtainMessage(MSG_READ_FILE1).sendToTarget()
                    } else {
                        Thread.sleep(500)
                    }
                }
                catch (e:Exception){
                    print ("Raw Image Unexpected exeception E "+e.toString())
                    Thread.sleep(1500)

                }
            }
        }

        val guiWatchDogtimer = Timer("Gui time out ticks",true)
        .scheduleAtFixedRate(timerTask{
            handler.obtainMessage(MSG_TICK).sendToTarget()
        },1000,1000)

        thread {
            while (true) {
                try {
                    val bm = BitmapFactory.decodeFile("/data/data/jp.co.dds.spicube/transimage.png")
                    if (bm != null) {
                        mutex.withLock {
                            stored_transimage = bm;
                        }
                        handler.obtainMessage(MSG_READ_FILE2).sendToTarget()
                    } else {
                        Thread.sleep(500)
                    }
                }
                catch (e: Exception) {
                    print ("Trans Image Unexpected exeception E "+e.toString())
                    Thread.sleep(1500)

                }
            }
        }
    }



override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
