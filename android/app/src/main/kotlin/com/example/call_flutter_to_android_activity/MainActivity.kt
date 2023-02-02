package com.example.call_flutter_to_android_activity

import android.app.Activity
import android.content.Intent
import android.util.Log
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val CHANNEL = "com.example/testChannel"
    private var result: MethodChannel.Result? = null

    private lateinit var channel: MethodChannel

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        channel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
        channel.setMethodCallHandler { call, result ->
            this.result = result
            if (call.method.equals("StartSecondActivity")) {
                //Log.e("calling","It's working")
                val intent = Intent(this, TestActivity::class.java)
                startActivityForResult(intent, 101)
                Log.e("end", "activity destroy")
                //result.success("ActivityStarted")
            }else if(call.method.equals("StartPdfActivity")){
                val intent = Intent(this, PdfListActivity::class.java)
                startActivityForResult(intent, 102)
                Log.e("end", "activity destroy")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == Activity.RESULT_OK) {
            val imagePath = data?.getStringExtra("imagePath")
            if (imagePath != null && imagePath != "")
                result?.success(imagePath)
            else
                result?.success("")
        } else {
            result?.success("")
        }
    }

    /*override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GeneratedPluginRegistrant.registerWith(this)

        MethodChannel(FlutterView,CHANNEL).setMethodCallHandler{ call, result ->
            if(call.method.equals("StartSecondActivity")){
               *//* val intent=Intent(this,KotlinActivity::class.java)
                startActivity(intent)
                result.success("ActivityStarted")*//*
            }
            else{
                result.notImplemented()
            }
        }
    }*/
}
