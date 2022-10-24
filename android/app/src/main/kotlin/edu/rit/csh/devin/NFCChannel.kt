package edu.rit.csh.devin

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import org.spongycastle.util.encoders.Hex


@RequiresApi(Build.VERSION_CODES.M)
class NFCChannel(val context: Context) : MethodChannel.MethodCallHandler {
    private var masterKeyAlias: MasterKey = MasterKey.Builder(context, "edu.rit.csh.devin.nfc")
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    private var sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "nfc",
        masterKeyAlias,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun applyAids() {
        for (realm in GatekeeperService.Realm.values()) {
            val aid = sharedPreferences.getString(realm.id, null)
            if (aid != null) {
                realm.associationId = Hex.decode(aid)
            }
        }
    }

    /**
     * Handles the specified method call received from Flutter.
     *
     *
     * Handler implementations must submit a result for all incoming calls, by making a single
     * call on the given [Result] callback. Failure to do so will result in lingering Flutter
     * result handlers. The result may be submitted asynchronously and on any thread. Calls to
     * unknown or unimplemented methods should be handled using [Result.notImplemented].
     *
     *
     * Any uncaught exception thrown by this method will be caught by the channel implementation
     * and logged, and an error result will be sent back to Flutter.
     *
     *
     * The handler is called on the platform thread (Android main thread). For more details see
     * [Threading in
 * the Flutter Engine](https://github.com/flutter/engine/wiki/Threading-in-the-Flutter-Engine).
     *
     * @param call A [MethodCall].
     * @param result A [Result] used for submitting the result of the call.
     */
    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when(call.method) {
            "updateAid" -> {
                val aids = call.arguments as HashMap<String, String>;
                val editor = sharedPreferences.edit()
                for ((realmId, aid) in aids) {
                    editor.putString(realmId, aid)

                    // val realm = GatekeeperService.Realm.values().first {it.id == realmId}
                    // realm.associationId = Hex.decode(aid)
                }
                editor.apply()
                applyAids()
                result.success(null)
            }
        }
    }

}
