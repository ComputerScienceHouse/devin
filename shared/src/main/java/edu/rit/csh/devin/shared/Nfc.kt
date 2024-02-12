package edu.rit.csh.devin.shared

import android.content.Context
import android.content.SharedPreferences
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.GsonBuilder
import com.okta.authfoundation.credential.Token
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import edu.rit.csh.devin.shared.model.AIDPack
import edu.rit.csh.devin.shared.model.GatekeperApi
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.bouncycastle.util.encoders.Hex
import org.bouncycastle.util.io.pem.PemReader
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.*
import java.security.KeyFactory
import java.security.Security
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.Cipher
import javax.inject.Inject
import javax.inject.Singleton

class Nfc(context: Context) {
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

  fun updateAid(aidPack: AIDPack) {
    val editor = sharedPreferences.edit()
    editor.putString(GatekeeperService.Realm.DOORS.id, aidPack.doorsId)
    editor.putString(GatekeeperService.Realm.DRINK.id, aidPack.drinkId)
    editor.putString(GatekeeperService.Realm.MEMBER_PROJECTS.id, aidPack.memberProjectsId)
    editor.apply()
    applyAids()
  }
}

@Module
@InstallIn(SingletonComponent::class)
class NfcProvider {
  @Provides
  @Singleton
  fun provideNfc(@ApplicationContext context: Context): Nfc = Nfc(context)
}

@HiltViewModel
class GatekeeperViewModel @Inject constructor(
  val token: StateFlow<Token?>,
  val nfc: Nfc
) : ViewModel() {
  val client = OkHttpClient.Builder()
    .addInterceptor { chain ->
      val tokenValue = token.value
      if (tokenValue == null) {
        chain.proceed(chain.request())
      } else {
        chain.proceed(
          chain.request().newBuilder()
            .header("Authorization", "Bearer ${tokenValue.accessToken}")
            .build()
        )
      }
    }
    .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
    .build()
  val retrofit = Retrofit.Builder()
    .baseUrl("https://gatekeeper-v2.csh.rit.edu")
    .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
    .client(client)
    .build()
  val api = retrofit.create(GatekeperApi::class.java)

  suspend fun provision() {
    val aidPack = try {
      api.provision()
    } catch(err: Exception) {
      err.printStackTrace()
      println("Failed to provision gatekeeper keys: $err")
      return
    }
    nfc.updateAid(aidPack)
  }
}

public class NfcConstants {
  companion object {
    val BASE_AID = byteArrayOf(0xF0.toByte(), 0x63, 0x73, 0x68, 0x72, 0x69, 0x74)
    val SPONGY_CASTLE = org.bouncycastle.jce.provider.BouncyCastleProvider()
    const val NONCE_SIZE = 8
    init {
      Security.insertProviderAt(
        SPONGY_CASTLE,
        1
      )
    }
    fun validateCrypto() {
      KeyFactory.getInstance("RSA", SPONGY_CASTLE)
      KeyFactory.getInstance("ECDSA", SPONGY_CASTLE)
      Cipher.getInstance("RSA/ECB/PKCS1Padding", SPONGY_CASTLE)
      Signature.getInstance("SHA384withECDSA", SPONGY_CASTLE)
      println("Validated all crypto methods we require are available!")
    }
  }
}

@AndroidEntryPoint
class GatekeeperService: HostApduService() {
  @Inject
  lateinit var nfc: Nfc

  private var aidApplied = false
  private var state: HandshakeState = HandshakeState.SELECT

  enum class HandshakeState {
    READER_VERIFICATION,
    SUCCESS,
    SELECT,
  }

  enum class Realm(val id: String, private val slot: Int, private val publicKey: Int, private val asymmetricPublicKey: Int, var associationId: ByteArray) {
    DOORS("doors", 0, R.raw.doors, R.raw.doors_asymmetric, byteArrayOf(0, 1, 2, 3, 4)),
    DRINK("drink", 1, R.raw.drink, R.raw.drink_asymmetric, byteArrayOf(0, 1, 2, 3, 4)),
    MEMBER_PROJECTS("memberProjects", 2, R.raw.member_projects, R.raw.member_projects_asymmetric, byteArrayOf(0, 1, 2, 3, 4));
    companion object {
      fun fromAid(aid: ByteArray): Realm? {
        if (aid.size != NfcConstants.BASE_AID.size) return null
        for (i in 0..(aid.size-2)) {
          if (aid[i] != NfcConstants.BASE_AID[i]) return null
        }
        val slotId = aid[aid.size-1] - NfcConstants.BASE_AID[aid.size-1]
        return values().first { it.slot == slotId }
      }
    }
    fun getPublicKey(context: Context): ECPublicKey {
      val keyStr = context.resources.openRawResource(publicKey)
      val spki = PemReader(InputStreamReader(keyStr)).readPemObject()
      val key = KeyFactory.getInstance("ECDSA", NfcConstants.SPONGY_CASTLE)
        .generatePublic(X509EncodedKeySpec(spki.content))
      return key as ECPublicKey
    }
    fun getPublicAsymmetricKey(context: Context): RSAPublicKey {
      val keyStr = context.resources.openRawResource(asymmetricPublicKey)
      val spki = PemReader(InputStreamReader(keyStr)).readPemObject()
      val key = KeyFactory.getInstance("RSA", NfcConstants.SPONGY_CASTLE)
        .generatePublic(X509EncodedKeySpec(spki.content))
      return key as RSAPublicKey
    }
  }

  /** Nonce generated by us */
  private val ourNonce: ByteArray = ByteArray(8)
  private val random = Random()
  private var realm: Realm = Realm.MEMBER_PROJECTS

  /**
   *
   * This method will be called when a command APDU has been received
   * from a remote device. A response APDU can be provided directly
   * by returning a byte-array in this method. Note that in general
   * response APDUs must be sent as quickly as possible, given the fact
   * that the user is likely holding their device over an NFC reader
   * when this method is called.
   *
   *
   * If there are multiple services that have registered for the same
   * AIDs in their meta-data entry, you will only get called if the user has
   * explicitly selected your service, either as a default or just for the next tap.
   *
   *
   * This method is running on the main thread of your application.
   * If you cannot return a response APDU immediately, return null
   * and use the [.sendResponseApdu] method later.
   *
   * @param commandApdu The APDU that was received from the remote device
   * @param extras A bundle containing extra data. May be null.
   * @return a byte-array containing the response APDU, or null if no
   * response APDU can be sent at this point.
   */
  override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray? {
    if (!aidApplied) {
      nfc.applyAids()
      aidApplied = true
    }
    if (commandApdu == null) return null
    val apdu = APDU(commandApdu)
    if (apdu.instructionId == 0xA4.toByte()) {
      state = HandshakeState.SELECT
    }
    return when (state) {
      HandshakeState.SELECT -> {
        if (apdu.instructionId != 0xA4.toByte() || apdu.p1 != 0x04.toByte() || apdu.p2 != 0x00.toByte()) {
          println("Wrong select?")
          return null
        }
        // println(apdu.data.map { value -> value.toString(16).padStart(2, '0') })
        if (apdu.data.size != NfcConstants.BASE_AID.size) {
          println("Wrong size?")
          return null
        }
        this.realm = Realm.fromAid(apdu.data) ?: return null
        this.random.nextBytes(this.ourNonce)
        this.state = HandshakeState.READER_VERIFICATION
        println("OK! Sending our nonce back!!!!")
        val bytes = NFCResponse(this.ourNonce, 0x9000U).toBytes()
        println(bytes.map { value -> value.toUByte().toString(16).padStart(2, '0') })
        bytes
      }
      HandshakeState.READER_VERIFICATION -> {
        println("Reader nonce is valid?")

        // Validate signature
        val publicKey = realm.getPublicKey(this.applicationContext)
        val signer = Signature.getInstance("SHA384withECDSA", NfcConstants.SPONGY_CASTLE)
        signer.initVerify(publicKey)
        val theirNonce =
          apdu.data.slice(IntRange(apdu.data.size - NfcConstants.NONCE_SIZE, apdu.data.size - 1))
        signer.update(ourNonce + theirNonce)
        val isValid = signer.verify(apdu.data, 0, apdu.data.size - NfcConstants.NONCE_SIZE)
        println("Okay, are we valid? $isValid")
        if (!isValid) {
          this.state = HandshakeState.SELECT
          return null
        }
        println("Looks like it's valid!!")

        assert(theirNonce.size == NfcConstants.NONCE_SIZE)
        val encodedValue = this.realm.associationId + theirNonce

        val publicAsymmetricKey = realm.getPublicAsymmetricKey(this.applicationContext)
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", NfcConstants.SPONGY_CASTLE)
        cipher.init(Cipher.ENCRYPT_MODE, publicAsymmetricKey)
        val encryptedValue = cipher.doFinal(encodedValue)
        // Send back encrypted `readerNonce` + association ID
        this.state = HandshakeState.SUCCESS
        NFCResponse(encryptedValue, 0x9069U).toBytes()
      }
      HandshakeState.SUCCESS -> {
        // Show a message to the user
        println("Success from reader!")
        null
      }
    }
  }

  /**
   * This method will be called in two possible scenarios:
   *  * The NFC link has been deactivated or lost
   *  * A different AID has been selected and was resolved to a different
   * service component
   * @param reason Either [.DEACTIVATION_LINK_LOSS] or [.DEACTIVATION_DESELECTED]
   */
  override fun onDeactivated(reason: Int) {
    println("Deactivated!!!")
    this.state = HandshakeState.SELECT
  }
}

class NFCResponse(private val payload: ByteArray, private val status: UShort) {
  fun toBytes(): ByteArray {
    val response: ByteArray = payload.copyOf(payload.size + 2)
    response[payload.size] = this.status.rotateRight(8).toByte()
    response[payload.size + 1] = this.status.and(0xffU).toByte()
    return response
  }
}

class APDU(val classId: Byte, val instructionId: Byte, val p1: Byte, val p2: Byte, val data: ByteArray, val responseLength: UShort) {
  override fun equals(other: Any?): Boolean {
    if (other !is APDU) {
      return false
    }
    return classId == other.classId &&
        instructionId == other.instructionId &&
        p1 == other.p1 &&
        p2 == other.p2 &&
        data.contentEquals(
          other.data
        ) &&
        responseLength == other.responseLength
  }

  override fun hashCode(): Int {
    var result: Int = this.classId.toInt()
    result = 31 * result + instructionId
    result = 31 * result + p1
    result = 31 * result + p2
    result = 31 * result + data.contentHashCode()
    result = 31 * result + responseLength.toInt()
    return result
  }

  constructor(data: ByteArray) : this(data[0], data[1], data[2], data[3], parseData(data).first, parseData(data).second)

  companion object {
    private fun parseData(data: ByteArray): Pair<ByteArray, UShort> {
      var index = 0
      var numBytes = 0.toUShort()
      if (data.size == 4) return Pair(byteArrayOf(), 0U)
      if (data.size == 5) return Pair(byteArrayOf(), data[4].toUShort())
      // [4] should be LC! :)
      if (data[4] > 1) {
        numBytes = data[4].toUShort()
        index = 5
      } else if (data[4] == 0.toByte()) {
        // Cringe as heck. Big(?) endian
        numBytes = data[5].toUShort().rotateLeft(8) or data[6].toUShort()
        index = 7
      }
      var responseLength = 0.toUShort()
      val responseLengthIndex = index + numBytes.toInt()
      if (responseLengthIndex < (data.size)-1) {
        responseLength = data[responseLengthIndex].toUShort().rotateLeft(8) or
            data[responseLengthIndex+1].toUShort()
      } else if (responseLengthIndex < data.size) {
        responseLength = data[responseLengthIndex].toUShort()
      }
      return Pair(data.sliceArray(IntRange(index, responseLengthIndex - 1)), responseLength)
    }
  }
}
