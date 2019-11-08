package com.quakearts.symbolusclient

import android.app.AlertDialog
import android.content.Intent
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.Toast
import com.quakearts.symbolusclient.utils.*
import kotlinx.android.synthetic.main.activity_loader.*
import kotlinx.android.synthetic.main.activity_pin.*
import kotlinx.android.synthetic.main.activity_totp.*
import java.text.MessageFormat

class LoaderActivity : AppCompatActivity() {
    private val activityThreadHandler = Handler()
    private val hidePart2Runnable = Runnable {
        symbolus_logo.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LOW_PROFILE or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }

    private val showPart2Runnable = Runnable {
        supportActionBar?.show()
        fullscreen_content_controls.visibility = View.VISIBLE
        hide_controls.visibility = View.VISIBLE
    }
    private var fullScreenContentVisible: Boolean = false
    private val hideRunnable = Runnable { hide() }
    private val showRunnable = Runnable { show() }
    private val delayHideTouchListener = View.OnTouchListener { _, _ ->
        val bounceAnimation = AnimationUtils.loadAnimation(this,R.anim.bounce)
        bounceAnimation.interpolator = BounceInterpolator(0.2, 15.0)
        hide_controls.startAnimation(bounceAnimation)
        if (AUTO_HIDE) {
            delayedHide(AUTO_HIDE_DELAY_MILLIS)
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_loader)

        if(DeviceStorage.hasDeviceFile(this)){
            alias_text.visibility = View.GONE
            alias_helper_text.visibility = View.GONE
        }

        fullScreenContentVisible = true

        symbolus_logo.setOnClickListener { toggle() }
        hide_controls.setOnTouchListener(delayHideTouchListener)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        delayedHide(100)
        delayedShow(5000)
    }

    private fun toggle() {
        if (fullScreenContentVisible) {
            hide()
        } else {
            show()
        }
    }

    private fun hide() {
        supportActionBar?.hide()
        fullscreen_content_controls.visibility = View.GONE
        hide_controls.visibility = View.GONE

        fullScreenContentVisible = false
        activityThreadHandler.removeCallbacks(showPart2Runnable)
        activityThreadHandler.postDelayed(hidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun show() {
        symbolus_logo.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        fullScreenContentVisible = true

        activityThreadHandler.removeCallbacks(hidePart2Runnable)
        activityThreadHandler.postDelayed(showPart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun delayedHide(delayMillis: Int) {
        activityThreadHandler.removeCallbacks(hideRunnable)
        activityThreadHandler.postDelayed(hideRunnable, delayMillis.toLong())
    }

    private fun delayedShow(delayMillis: Int){
        activityThreadHandler.removeCallbacks(showRunnable)
        activityThreadHandler.postDelayed(showRunnable, delayMillis.toLong())
    }

    fun onContinueClicked(view:View){
        val hasAlias = !DeviceStorage.hasDeviceFile(this)
        if(hasAlias && alias_text.text.isBlank()){
            alias_helper_text.setTextColor(resources.getColor(R.color.text_view_error))
            return
        }

        val pinActivity = Intent(this, PinActivity::class.java);
        if(hasAlias)
            pinActivity.putExtra(Options.aliasProperty, alias_text.text.toString())

        startActivity(pinActivity)
    }

    companion object {
        /**
         * Whether or not the system UI should be auto-hidden after
         * [AUTO_HIDE_DELAY_MILLIS] milliseconds.
         */
        private const val AUTO_HIDE = true

        /**
         * If [AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private const val AUTO_HIDE_DELAY_MILLIS = 1000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private const val UI_ANIMATION_DELAY = 300
    }
}

class PinActivity : AppCompatActivity() {
    private var alias = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin)
        if(!DeviceStorage.hasDeviceFile(this)
            && intent.extras != null
            && intent.extras!!.containsKey(Options.aliasProperty))
            alias = intent.extras?.get(Options.aliasProperty) as String
    }

    fun pinButtonClicked(view: View){
        val buttonText = (view as Button).text.toString()
        pin_text.setText(pin_text.text.toString()+buttonText)
    }

    fun deleteClicked(view: View){
        if(pin_text.text.length>1)
            pin_text.setText(pin_text.text.subSequence(0,pin_text.text.length-1))
        else
            pin_text.setText("")
    }

    fun nextClicked(view: View){
        if(pin_text.text.isBlank()){
            pin_helper_text.visibility = View.VISIBLE
            return
        }
        progress_overlay.visibility = View.VISIBLE
        continue_device_load.setClickable(false)
        LoadAndOrProvisionTask().execute(alias, pin_text.text.toString())
    }

    inner class LoadAndOrProvisionTask:AsyncTask<String,Unit,Boolean>(){
        override fun doInBackground(vararg params: String): Boolean{
            val alias = params[0]!!
            val pin = params[1]!!
            try {
                TOTPApplication.loadDevice(alias, pin, application)
            } catch (e:Throwable){
                Log.e("Device Error","Unable to load device", e)
                return false
            }
            return true
        }

        override fun onPostExecute(result: Boolean?) {
            progress_overlay.visibility = View.GONE
            continue_device_load.setClickable(true)
            if(result!! ) {
                startActivity(Intent(this@PinActivity, TOTPActivity::class.java))
            } else {
                Toast.makeText(this@PinActivity,getText(R.string.loading_failed), Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}

class TOTPActivity : AppCompatActivity() {
    private lateinit var counter : Counter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(!TOTPApplication.started()){
            startActivity(Intent(this,LoaderActivity::class.java))
        }

        setContentView(R.layout.activity_totp)
        setSupportActionBar(toolbar)

        counter = Counter({
            runOnUiThread {
                counter_view.text = it
            }
        },{
            runOnUiThread{
                pin_view.text = TOTPApplication.generateOtp()
            }
        })

        TOTPApplication.registerListeners(otpAuthorizationRequestListener = { onOk, onCancel, _->
            runOnUiThread{
                AlertDialog.Builder(this).setMessage(R.string.otp_dialog)
                    .setPositiveButton(R.string.otp_dialog_authorize) { dialog, _ ->
                        onOk()
                        dialog.dismiss()
                    }
                    .setNegativeButton(R.string.otp_dialog_reject) { dialog, _ ->
                        onCancel()
                        dialog.dismiss()
                    }.create().show()
            }
        }, otpSigningRequestListener = {onOk, onCancel, message ->
            val signingDetails = message.map { messagePart -> messagePart.key+":"+messagePart.value }
            val signingMessage = MessageFormat.format(getString(R.string.otp_signing_dialog), signingDetails)
            runOnUiThread {
                AlertDialog.Builder(this).setMessage(signingMessage)
                    .setPositiveButton(R.string.otp_dialog_authorize) { dialog, _ ->
                        onOk()
                        dialog.dismiss()
                    }
                    .setNegativeButton(R.string.otp_dialog_reject) { dialog, _ ->
                        onCancel()
                        dialog.dismiss()
                    }.create().show()
            }
        })

        stop_counter.setClickable(false)
    }

    fun onPinTextClicked(view:View){
        if(this::counter.isInitialized) {
            val bounceAnimation = AnimationUtils.loadAnimation(this, R.anim.bounce)
            bounceAnimation.interpolator = BounceInterpolator(0.2, 15.0)
            pin_view.startAnimation(bounceAnimation)
            CounterTask().execute(counter)
            stop_counter.setClickable(true)
            pin_view.setClickable(false)
            pin_view.text = TOTPApplication.generateOtp()
        }
    }

    fun onStopClicked(view: View){
        if(this::counter.isInitialized){
            val bounceAnimation = AnimationUtils.loadAnimation(this, R.anim.bounce)
            bounceAnimation.interpolator = BounceInterpolator(0.2, 15.0)
            stop_counter.startAnimation(bounceAnimation)
            stopCounter()
        }
    }

    override fun onPause() {
        super.onPause()
        stopCounter()
        unregisterListeners()
    }

    private fun stopCounter() {
        counter.stop()
        clearTextView()
        stop_counter.setClickable(false)
        pin_view.setClickable(true)
    }

    private fun clearTextView() {
        pin_view.text = getText(R.string.otp_at_rest)
        counter_view.text = getText(R.string.counter_at_rest)
    }

    private fun unregisterListeners(){
        TOTPApplication.unRegisterListener()
    }

    override fun onDestroy() {
        super.onDestroy()
        TOTPApplication.onCleared()
    }
}

class CounterTask: AsyncTask<Counter, Unit, Unit>() {
    override fun doInBackground(vararg counters: Counter) {
        val counter = counters[0]
        counter.start()
    }
}
