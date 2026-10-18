package com.outerjoyn.sdk

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import androidx.browser.customtabs.CustomTabsIntent
import org.json.JSONObject

/**
 * Runs a partner's login in a Chrome Custom Tab.
 *
 * The link widget opens a partner login with `window.open`. A WebView drops that
 * pop-up, and partners (Google among them) refuse logins in embedded views, so
 * the kit replaces `window.open` with a bridge: the login opens in a Custom Tab,
 * the API redirects to `outerjoyn://connect/callback` when it is done
 * ([OuterJoynLoginCallbackActivity] receives it), and the result is posted back
 * into the widget as the same message a web pop-up would send.
 */
internal object OuterJoynLoginTab {
    private const val PARTNER_LOGIN_PATH = "/api/v1/connect/oauth/authorize"
    // The API returns native logins only to its registered app schemes.
    private const val CALLBACK_URL = "outerjoyn://connect/callback"

    /** Installed before the SDK script so it is in place when the widget loads. */
    val windowOpenBridge = """
        (function () {
          var popups = {};
          window.__ojLoginFinished = function (id) {
            if (popups[id]) { popups[id].closed = true; delete popups[id]; }
          };
          window.open = function (url) {
            var id = String(Date.now()) + String(Math.random()).slice(2);
            var popup = { closed: false, focus: function () {}, close: function () {} };
            popups[id] = popup;
            OJBridge.postMessage(JSON.stringify({
              type: 'login', payload: { id: id, url: new URL(String(url), location.href).toString() }
            }));
            return popup;
          };
        })();
    """.trimIndent()

    private class Pending(val id: String, val webView: WebView, val host: Activity)

    private var pending: Pending? = null

    fun open(context: Context, webView: WebView, id: String, url: String) {
        val uri = Uri.parse(url)
        if (uri.scheme != "https") return
        if (uri.path != PARTNER_LOGIN_PATH) {
            // Any other link the widget opens (terms, privacy) belongs in the browser.
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            finish(webView, id, afterDelivery = false)
            return
        }
        val host = context as? Activity ?: return
        val loginUri = uri.buildUpon().clearQuery().apply {
            for (name in uri.queryParameterNames) {
                if (name == "platform" || name == "redirect_uri") continue
                for (value in uri.getQueryParameters(name)) appendQueryParameter(name, value)
            }
            appendQueryParameter("platform", "mobile")
            appendQueryParameter("redirect_uri", CALLBACK_URL)
        }.build()
        pending = Pending(id, webView, host)
        CustomTabsIntent.Builder().build().launchUrl(host, loginUri)
    }

    /** The host came back to the front. With a login still pending, the customer
     *  closed the tab: closing the pop-up puts the widget back on its confirm step. */
    fun onHostResumed() {
        val p = pending ?: return
        pending = null
        finish(p.webView, p.id, afterDelivery = false)
    }

    fun deliver(callback: Uri) {
        val p = pending ?: return
        pending = null
        val message = JSONObject()
        val linkId = callback.getQueryParameter("member_partner_link_id")
        if (callback.getQueryParameter("status") == "success" && linkId != null) {
            val payload = JSONObject().put("member_partner_link_id", linkId)
            callback.getQueryParameter("institution_id")?.let { payload.put("institution_id", it) }
            callback.getQueryParameter("consumer_ref")?.let { payload.put("consumer_ref", it) }
            message.put("type", "outerjoyn:connect:complete").put("payload", payload)
        } else {
            message.put("type", "outerjoyn:connect:error").put(
                "payload",
                JSONObject()
                    .put("error", callback.getQueryParameter("error") ?: "unknown_error")
                    .put("message", callback.getQueryParameter("error_description") ?: "Linking did not finish."),
            )
        }
        p.webView.evaluateJavascript("window.postMessage($message, '*');", null)
        finish(p.webView, p.id, afterDelivery = true)
        // Bring the host back over the Custom Tab, closing the tab.
        p.host.startActivity(
            Intent(p.host, p.host.javaClass).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
    }

    /** The widget treats a closed pop-up with no result as a cancel, so after a
     *  result it is closed only once the posted message has had time to land. */
    private fun finish(webView: WebView, id: String, afterDelivery: Boolean) {
        val script = "window.__ojLoginFinished(${JSONObject.quote(id)});"
        if (afterDelivery) webView.postDelayed({ webView.evaluateJavascript(script, null) }, 1000)
        else webView.evaluateJavascript(script, null)
    }
}

/** Receives the API's redirect at the end of a partner login. */
class OuterJoynLoginCallbackActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent?.data?.let { OuterJoynLoginTab.deliver(it) }
        finish()
    }
}
