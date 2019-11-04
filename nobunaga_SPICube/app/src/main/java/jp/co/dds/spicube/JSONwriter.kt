package jp.co.dds.spicube

import android.system.Os.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread

object JSONwriter {
    var mystoredJSONobject = String()
    val mutex = ReentrantLock()

    fun setJSON(j:String){
        mutex.lock()
        mystoredJSONobject = j;
    }

    fun onCreate(){
        val mode755 = 0x1ed
        try {
            val filename = "/data/data/jp.co.dds.spicube/settings.json"
            var stats = lstat(filename)
            var x = stats.st_mode
            remove(filename)

            stats = lstat("/data/data/jp.co.dds.spicube/settings.json")
            x = stats.st_mode
        }
        catch (e:Exception){
            // TODO ???
        }


        thread{
            while (true){
                mutex.lock()
                if(!mystoredJSONobject.isEmpty()){
                    val j = mystoredJSONobject
                    mystoredJSONobject = ""
                    mutex.unlock()
                    mkfifo("/data/data/jp.co.dds.spicube/settings.json",mode755)
                }

            }


        }

    }

}
