package dev.rexios.wear_plus

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.wearable.compat.WearableActivityController
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.embedding.engine.plugins.lifecycle.HiddenLifecycleReference
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

internal class WearPluginDelegate :
    WearPluginDelegateApi,
    MethodCallHandler,
    LifecycleEventObserver {
    private var ambientCallback = WearableAmbientCallback()
    private var methodChannel: MethodChannel? = null
    private var activityBinding: ActivityPluginBinding? = null
    private var ambientController: WearableActivityController? = null

    companion object {
        const val TAG = "WearPlugin"
        const val BURN_IN_PROTECTION = WearableActivityController.EXTRA_BURN_IN_PROTECTION
        const val LOW_BIT_AMBIENT = WearableActivityController.EXTRA_LOWBIT_AMBIENT
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel = MethodChannel(binding.binaryMessenger, "wear")
        methodChannel?.setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel?.setMethodCallHandler(null)
        methodChannel = null
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        attachAmbientController(binding)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        detachAmbientController()
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        attachAmbientController(binding)
    }

    override fun onDetachedFromActivity() {
        detachAmbientController()
    }

    private fun attachAmbientController(binding: ActivityPluginBinding) {
        activityBinding = binding
        ambientController = WearableActivityController(TAG, binding.activity, ambientCallback)
        ambientController?.setAmbientEnabled()
        val reference = binding.lifecycle as HiddenLifecycleReference
        reference.lifecycle.addObserver(this)
    }

    private fun detachAmbientController() {
        activityBinding?.let {
            val reference = it.lifecycle as HiddenLifecycleReference
            reference.lifecycle.removeObserver(this)
        }
        activityBinding = null
        ambientController = null
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "getShape" -> {
                val activity = activityBinding?.activity
                when {
                    activity == null -> result.error("no-activity", "No android activity available.", null)
                    activity.resources.configuration.isScreenRound -> result.success("round")
                    else -> result.success("square")
                }
            }
            "isAmbient" -> result.success(ambientController?.isAmbient ?: false)
            "setAutoResumeEnabled" -> {
                val enabled = call.argument<Boolean>("enabled")
                if (ambientController == null || enabled == null) {
                    result.error("not-ready", "Ambient mode controller not ready", null)
                } else {
                    ambientController?.setAutoResumeEnabled(enabled)
                    result.success(null)
                }
            }
            "setAmbientOffloadEnabled" -> {
                val enabled = call.argument<Boolean>("enabled")
                if (ambientController == null || enabled == null) {
                    result.error("not-ready", "Ambient mode controller not ready", null)
                } else {
                    ambientController?.setAmbientOffloadEnabled(enabled)
                    result.success(null)
                }
            }
            else -> result.notImplemented()
        }
    }

    private inner class WearableAmbientCallback : WearableActivityController.AmbientCallback() {
        override fun onEnterAmbient(ambientDetails: Bundle) {
            methodChannel?.invokeMethod(
                "onEnterAmbient",
                mapOf(
                    "burnInProtection" to ambientDetails.getBoolean(BURN_IN_PROTECTION, false),
                    "lowBitAmbient" to ambientDetails.getBoolean(LOW_BIT_AMBIENT, false),
                ),
            )
        }

        override fun onExitAmbient() {
            methodChannel?.invokeMethod("onExitAmbient", null)
        }

        override fun onUpdateAmbient() {
            methodChannel?.invokeMethod("onUpdateAmbient", null)
        }

        override fun onInvalidateAmbientOffload() {
            methodChannel?.invokeMethod("onInvalidateAmbientOffload", null)
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> ambientController?.onCreate()
            Lifecycle.Event.ON_RESUME -> ambientController?.onResume()
            Lifecycle.Event.ON_PAUSE -> ambientController?.onPause()
            Lifecycle.Event.ON_STOP -> ambientController?.onStop()
            Lifecycle.Event.ON_DESTROY -> ambientController?.onDestroy()
            else -> Unit
        }
    }
}
