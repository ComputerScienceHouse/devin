package edu.rit.csh.devin

import android.os.Build
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

private const val NFC_CHANNEL = "edu.rit.csh.devin/nfc"
class MainActivity: FlutterActivity() {
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            MethodChannel(flutterEngine.dartExecutor.binaryMessenger, NFC_CHANNEL).setMethodCallHandler(NFCChannel(activity))
        }
    }
}
