package com.sixlogs.smsapi

import android.R.attr
import android.accounts.AccountManager.get
import android.annotation.SuppressLint
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore



import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

import java.io.File
import com.klinker.android.logger.Log;

import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.os.Build

import android.preference.PreferenceManager
import android.text.TextUtils
import android.view.View
import android.widget.*
import com.klinker.android.send_message.ApnUtils.OnApnFinishedListener
import androidx.recyclerview.widget.RecyclerView
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import com.klinker.android.send_message.*
import android.provider.Telephony
import com.klinker.android.logger.Log.setDebug
import com.klinker.android.logger.Log.setLogListener

import com.klinker.android.logger.OnLogListener
import android.graphics.drawable.BitmapDrawable
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import android.R.attr.path
import android.renderscript.RenderScript
import java.lang.Exception
import java.net.URI


class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    private var settings: Settings? = null


    private var setDefaultAppButton: Button? = null
    private var selectApns: Button? = null
    private var fromField: EditText? = null
    private var toField: EditText? = null
    private var messageField: EditText? = null
    private var imageToSend: ImageView? = null
    private var sendButton: Button? = null
    private var log: RecyclerView? = null

    private var logAdapter: LogAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("request_permissions", true) &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        ) {
            startActivity(Intent(this, PermissionActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_main)


        initSettings()
        initViews()
        initActions()
        initLogging()

        BroadcastUtils.sendExplicitBroadcast(this, Intent(), "test action")


    }

    private fun initSettings() {
        settings = Settings.get(this)
        if (TextUtils.isEmpty(settings?.getMmsc()) &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP
        ) {
            initApns()
        }
    }

    private fun initApns() {
        ApnUtils.initDefaultApns(
            this
        ) { settings = Settings.get(this@MainActivity, true) }
    }


    private fun initViews() {
        setDefaultAppButton = findViewById<View>(R.id.set_as_default) as Button
        selectApns = findViewById<View>(R.id.apns) as Button
        fromField = findViewById<View>(R.id.from) as EditText
        toField = findViewById<View>(R.id.to) as EditText
        messageField = findViewById<View>(R.id.message) as EditText
        imageToSend = findViewById<View>(R.id.image) as ImageView
        sendButton = findViewById<View>(R.id.send) as Button
        log = findViewById<View>(R.id.log) as RecyclerView
    }


    private fun initActions() {
        if (Utils.isDefaultSmsApp(this)) {
            setDefaultAppButton!!.visibility = View.GONE
        } else {
            setDefaultAppButton!!.setOnClickListener { setDefaultSmsApp() }
        }
        selectApns!!.setOnClickListener { initApns() }
        fromField?.setText(Utils.getMyPhoneNumber(this))
        toField?.setText(Utils.getMyPhoneNumber(this))
        imageToSend!!.setOnClickListener { toggleSendImage() }
        sendButton!!.setOnClickListener { sendMessage() }
        log!!.setHasFixedSize(false)
        log!!.layoutManager = LinearLayoutManager(this)
        logAdapter = LogAdapter(ArrayList<String>())
        log!!.adapter = logAdapter
    }

    private fun initLogging() {
        Log.setDebug(true)
        Log.setPath("messenger_log.txt")
        Log.setLogListener(OnLogListener { tag, message ->
            //logAdapter.addItem(tag + ": " + message);
        })
    }

    private fun setDefaultSmsApp() {
        setDefaultAppButton!!.visibility = View.GONE
        val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
        intent.putExtra(
            Telephony.Sms.Intents.EXTRA_PACKAGE_NAME,
            packageName
        )
        startActivity(intent)
    }

    private fun toggleSendImage() {
        openAttachement()
   /*     if (imageToSend!!.isEnabled) {
            imageToSend!!.isEnabled = false
            imageToSend!!.alpha = 0.3f
        } else {
            imageToSend!!.isEnabled = true
            imageToSend!!.alpha = 1.0f
        }*/


    }

    private fun openAttachement() {

        val pickIntnet = Intent()
        pickIntnet.type = "audio/*"
        pickIntnet.action = Intent.ACTION_GET_CONTENT
        activityChooser.launch(pickIntnet)
    }
    var gelleryPath = ""
    var fileUri : Uri ?= null
    private var activityChooser: ActivityResultLauncher<Intent> =
        registerForActivityResult<Intent, ActivityResult>(
            ActivityResultContracts.StartActivityForResult(),
            ActivityResultCallback<ActivityResult> {
                // ToDO:
                if (it.resultCode == AppCompatActivity.RESULT_OK) {
                    val selectedImageUri: Uri? = it.data?.data
                    if (selectedImageUri != null) {
                        android.util.Log.e(TAG, ": $selectedImageUri ")
                        try {
                            fileUri = selectedImageUri
                            val actualImagePath = FileUtils.getPath(this, selectedImageUri)
                            val file = File(actualImagePath)
                            //  binding.etContent.setText(file.name)
                            if (actualImagePath != null) {
                                gelleryPath = actualImagePath
                            }
                        }catch (ex : Exception){
                            ex.printStackTrace()
                        }




                    } else {
                     //   toast("Can't find image ")
                    }

                }
            }
        ) as ActivityResultLauncher<Intent>

    @SuppressLint("Range")
    fun getPath(uri: Uri?): String? {
        var cursor: Cursor =
            getContentResolver().query(uri!!, null, null, null, null)!!
        cursor.moveToFirst()
        var document_id = cursor.getString(0)
        document_id = document_id.substring(document_id.lastIndexOf(":") + 1)
        cursor.close()
        cursor = getContentResolver().query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            null, MediaStore.Images.Media._ID + " = ? ", arrayOf(document_id), null
        )!!
        cursor.moveToFirst()
        val path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
        cursor.close()
        return path
    }


    /* fun onViewClick(view: android.view.View) {
        when(view.id){
            R.id.bPickImg -> {
                openAttachement()
            }
            R.id.bSendMMS -> {
                sendSMSViaMyApp()
             //   sendMMS()
            }
        }
    }
*/

    fun sendMessage() {
        Thread {
            val sendSettings = Settings()
            sendSettings.mmsc = settings!!.mmsc
            sendSettings.proxy = settings!!.mmsProxy
            sendSettings.port = settings!!.mmsPort
            sendSettings.useSystemSending = true
            val transaction = Transaction(this@MainActivity, sendSettings)
            val message = Message(messageField!!.text.toString(), toField!!.text.toString())


            val audioFileBytes = convertFileToByte(gelleryPath)
        //    val messagePart =  Message.Part(audioFileBytes)
               message.setAudio(audioFileBytes)
      //    message.messageUri = fileUri
//          if (imageToSend!!.isEnabled) {
//               val imagbBitMap = (imageToSend!!.drawable as BitmapDrawable).bitmap
//                message.setImage(imagbBitMap)*//*
//            }


            transaction.sendNewMessage(message, Transaction.NO_THREAD_ID)
        }.start()
    }

    private fun convertFileToByte(s: String): ByteArray {
        val file = File(s)
        val fis = FileInputStream(file)
        val bos = ByteArrayOutputStream()
        val b = ByteArray(1024)

        var readNum: Int
        while (fis.read(b).also { readNum = it } != -1) {
            bos.write(b, 0, readNum)
        }


        val bytes: ByteArray = bos.toByteArray()
        return bytes
    }


}