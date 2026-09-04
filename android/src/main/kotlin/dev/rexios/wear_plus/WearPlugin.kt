package dev.rexios.wear_plus

import android.content.pm.PackageManager
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding

class WearPlugin : FlutterPlugin, ActivityAware {
    private var delegate: WearPluginDelegateApi? = null
    private var activityBinding: ActivityPluginBinding? = null

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        if (!binding.applicationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)) {
            return
        }

        delegate = Class.forName(DELEGATE_CLASS_NAME)
            .getDeclaredConstructor()
            .newInstance() as WearPluginDelegateApi
        delegate?.onAttachedToEngine(binding)
        activityBinding?.let { delegate?.onAttachedToActivity(it) }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        delegate?.onDetachedFromEngine(binding)
        delegate = null
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activityBinding = binding
        delegate?.onAttachedToActivity(binding)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        delegate?.onDetachedFromActivityForConfigChanges()
        activityBinding = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activityBinding = binding
        delegate?.onReattachedToActivityForConfigChanges(binding)
    }

    override fun onDetachedFromActivity() {
        delegate?.onDetachedFromActivity()
        activityBinding = null
    }

    private companion object {
        const val DELEGATE_CLASS_NAME = "dev.rexios.wear_plus.WearPluginDelegate"
    }
}
