package com.skiy.sf.hook

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.skiy.sf.nmhooklib.utils.afterMethodCalled
import com.skiy.sf.nmhooklib.utils.findMethodByName
import com.skiy.sf.nmhooklib.utils.replaceMethodReturnTo

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MainActivity::class.java.findMethodByName("test")!! afterMethodCalled {
            println(it.getResult())
        }

        Log.d("Test", test())

    }

    fun test(): String {
        return "hook me"
    }
}