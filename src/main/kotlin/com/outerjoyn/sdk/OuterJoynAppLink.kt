package com.outerjoyn.sdk

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

/**
 * Opens a brand's app from an "Open in the app" offer button.
 *
 * The widget posts `{ type: "open_app_link", payload: { appUrl, fallbackUrl } }`.
 * The app opens when it is installed; otherwise the fallback page opens in the
 * browser. An https link counts only when an app, not a browser, takes it. No
 * manifest `<queries>` entry is needed: the kit tries the link instead of asking
 * first, and only Android 10 and older are asked which apps take it.
 */
internal object OuterJoynAppLink {
    fun open(context: Context, appUrl: String, fallbackUrl: String) {
        val fallback = Uri.parse(fallbackUrl)
        if (fallback.scheme != "https") return
        val app = Uri.parse(appUrl)
        val scheme = app.scheme?.lowercase()
        val opened = when {
            scheme == null || scheme == "http" || scheme == "intent" -> false
            scheme == "https" -> openAppLink(context, app)
            else -> start(context, view(app))
        }
        if (!opened) start(context, view(fallback))
    }

    private fun view(uri: Uri) = Intent(Intent.ACTION_VIEW, uri)
        .addCategory(Intent.CATEGORY_BROWSABLE)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun openAppLink(context: Context, uri: Uri): Boolean {
        val intent = view(uri)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            intent.addFlags(Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER)
            return start(context, intent)
        }
        val pm = context.packageManager
        val browsers = pm.queryIntentActivities(view(Uri.parse("https://example.com/")), 0)
            .map { it.activityInfo.packageName }.toSet()
        val app = pm.queryIntentActivities(intent, 0)
            .firstOrNull { it.activityInfo.packageName !in browsers } ?: return false
        intent.setPackage(app.activityInfo.packageName)
        return start(context, intent)
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
