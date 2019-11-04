package jp.co.dds.spicube

import android.system.Os.*
import java.io.File

import java.lang.Exception

fun workspaceinit() {
    // Create the named pipes
    val mode770 = 0x1f8
    val mode755 = 0x1ed
    try {
        mkfifo("/data/data/jp.co.dds.spicube/settings.json",mode770)
    }
    catch (e:Exception){
        // TODO ???
    }
    try {
        mkfifo("/data/data/jp.co.dds.spicube/rawimage.png",mode770)
    }
    catch (e:Exception){
        // TODO ???
    }
    try {
        mkfifo("/data/data/jp.co.dds.spicube/transimage.png",mode770)
    }
    catch (e:Exception){
        // TODO ???
    }

    // Create a runme script for convineince
    val script="#!/bin/sh\necho 0 > /proc/sys/kernel/printk\nam start -n jp.co.dds.spicube/jp.co.dds.spicube.MainActivity &\nspihello /dev/sup* -u -l\n"

    try {
            File("/data/data/jp.co.dds.spicube/runme.sh").printWriter().use { out -> out.println( script.toString())  }
            chmod("/data/data/jp.co.dds.spicube/runme.sh",mode755)
    }
    catch (e:Exception){
    }
}
