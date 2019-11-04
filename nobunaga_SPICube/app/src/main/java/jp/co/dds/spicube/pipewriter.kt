package jp.co.dds.spicube

import org.json.JSONObject
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

object pipewriter {
    val mutex = ReentrantLock()

    fun write_json(o: JSONObject) : Boolean {
        if (mutex.isLocked()){
            return false
        }else{
            thread {
                mutex.withLock {
                    try {
                        File("/data/data/jp.co.dds.spicube/settings.json").printWriter().use { out -> out.println( o.toString() )  }
                    }
                    catch (e:Throwable) {
                        // Catch expecion sicilently
                    }
                }
            }

        }
        return true
    }
}
