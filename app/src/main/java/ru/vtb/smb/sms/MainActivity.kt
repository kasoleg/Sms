package ru.vtb.smb.sms

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.huawei.hms.support.sms.ReadSmsManager
import com.huawei.hms.support.sms.common.ReadSmsConstant.READ_SMS_BROADCAST_ACTION
import ru.vtb.smb.sms.databinding.ActivityMainBinding
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private lateinit var mainActivityViewModel: MainActivityViewModel
    private lateinit var binding : ActivityMainBinding

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainActivityViewModel = ViewModelProvider(this).get(MainActivityViewModel::class.java)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_main
        )

        binding.viewModel = mainActivityViewModel
        binding.lifecycleOwner = this@MainActivity

        observe()
    }

    private fun observe() {
        mainActivityViewModel.clickObserver.observe(this, Observer {

            when (it) {
                "generate_code" -> {
                    val permissionCheck =
                        ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                    if (permissionCheck == PackageManager.PERMISSION_GRANTED)
                        generateSmsCode()
                    else
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.SEND_SMS), 111
                        )
                }
            }
        })
    }

    private fun initSmsManager() {

        val task = ReadSmsManager.startConsent(this@MainActivity, mainActivityViewModel.mobileNumber.value)
        task.addOnCompleteListener {

            if (task.isSuccessful) {
                Toast.makeText(this, "Verification code was sent successfully", Toast.LENGTH_LONG).show()
            } else
                Toast.makeText(this, "The service failed to be enabled.", Toast.LENGTH_LONG).show()
        }
    }

    private fun generateSmsCode() {
        initSmsManager()
        registerSmsBroadcastReceiver()
        sendSms()
        registerOtpBroadcastReceiver()
    }

    private fun registerSmsBroadcastReceiver() {

        val intentFilter = IntentFilter(READ_SMS_BROADCAST_ACTION)
        registerReceiver(SmsBroadcastReceiver(), intentFilter)
    }

    private fun sendSms() {
        val otp = Random.nextInt(
            100000,
            999999
        ).toString()

        if (!mainActivityViewModel.mobileNumber.value.isNullOrEmpty()) {

            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(
                mainActivityViewModel.mobileNumber.value,
                null,
                "Your verification code is $otp",
                null,
                null
            )
        } else {
            Toast.makeText(this, "Please enter the phone number", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            111 -> {

                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    generateSmsCode()
                else
                    Toast.makeText(
                        this,
                        "Permissions has been refused",
                        Toast.LENGTH_LONG
                    ).show()
            }
        }
    }

    private fun registerOtpBroadcastReceiver() {

        val filter = IntentFilter()
        filter.addAction("service.to.activity.transfer")
        val updateUIReceiver = object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent
            ){
                intent.getStringExtra("sms")?.let {
                    mainActivityViewModel.otp.value = "Otp : " + it.split(" ")[4]
                }
            }
        }
        registerReceiver(updateUIReceiver, filter)
    }

}