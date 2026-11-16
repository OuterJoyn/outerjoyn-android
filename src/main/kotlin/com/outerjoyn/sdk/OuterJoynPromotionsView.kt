package com.outerjoyn.sdk

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject

/** How a promotion set lays out its offers. Leave it out to use the set's own. */
enum class OJPromotionsLayout(val value: String) {
    Carousel("carousel"), Grid("grid"), List("list"), Hero("hero"), Strip("strip"), Banner("banner")
}

/**
 * A promotion set's offers, inline in your screen. It grows to fit its offers.
 *
 * Web pages and app links from offer buttons open outside the view (the
 * browser, or the brand's app). Link, rewards and other OuterJoyn windows go to
 * [onOpenWidget]; the [OuterJoynPromotions] composable opens them in the kit's
 * sheet for you.
 *
 * ```kotlin
 * val offers = OuterJoynPromotionsView(context)
 * offers.show(setId = "380551304567336520", layout = OJPromotionsLayout.Grid,
 *             look = mapOf("buttonColor" to "#e11d48", "columns" to 2))
 * ```
 */
@SuppressLint("SetJavaScriptEnabled", "ViewConstructor")
class OuterJoynPromotionsView(context: Context) : WebView(context) {
    /** The height the offers need changed, in dp (also applied to the view's height). */
    var onHeightChange: ((Int) -> Unit)? = null
    /** An offer button set to "Run an action on your site" was pressed: its action name. */
    var onOfferEvent: ((String) -> Unit)? = null
    /** An offer opens an OuterJoyn window (link, rewards, ...): `partners` is set for a link offer. */
    var onOpenWidget: ((OJWidget, List<String>?) -> Unit)? = null
    /** The set couldn't be shown (unknown set, bad look setting, no network). */
    var onError: ((String) -> Unit)? = null

    private var html = ""
    private var pageLoaded = false
    private val main = Handler(Looper.getMainLooper())

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0)
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        isVerticalScrollBarEnabled = false
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        addJavascriptInterface(Bridge(), "OJBridge")
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                // The page itself and anything inside frames load here; a page the shopper goes to opens outside.
                if (!pageLoaded || !request.isForMainFrame) return false
                openOutside(request.url)
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                pageLoaded = true
            }
        }
    }

    /**
     * Show a set. Nothing reloads when nothing changed.
     *
     * @param setId The promotion set's id (the set designer's **Use on your site** window shows it).
     * @param layout Show the set in this layout instead of its own.
     * @param look Any saved set setting for this spot only (`accentColor`, `buttonColor`, `columns`,
     *   `bannerLook`, ...; the list is `GET /api/v1/promotion-sets/design-settings`). Values are
     *   strings, numbers or booleans.
     * @param sessionToken The signed-in customer's session, for joining, linking and rewards.
     *   `null` shows the offers everyone can see.
     */
    fun show(
        setId: String,
        layout: OJPromotionsLayout? = null,
        look: Map<String, Any> = emptyMap(),
        sessionToken: String? = null,
        apiBaseUrl: String = OuterJoynSDK.apiBaseUrl,
    ) {
        for ((key, value) in look) {
            require(value is String || value is Number || value is Boolean) {
                "OuterJoynPromotionsView look[$key]: values must be strings, numbers or booleans"
            }
        }
        val options = JSONObject().apply {
            put("arrayId", setId)
            put("theme", JSONObject(look))
            layout?.let { put("layout", it.value) }
        }
        val configure = JSONObject().apply {
            put("apiBaseUrl", apiBaseUrl)
            sessionToken?.let { put("sessionToken", it) }
        }
        val next = page(options, configure)
        if (next == html) return
        html = next
        pageLoaded = false
        loadDataWithBaseURL("https://sdk.outerjoyn.com", next, "text/html", "UTF-8", null)
    }

    /** Redraws the set, e.g. after a link changed which offers the customer has. */
    fun refresh() {
        evaluateJavascript("window.ojRenderPromotions && window.ojRenderPromotions()", null)
    }

    private fun openOutside(uri: Uri) {
        val scheme = uri.scheme?.lowercase() ?: return
        if (scheme == "javascript" || scheme == "data" || scheme == "about" || scheme == "intent") return
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: android.content.ActivityNotFoundException) {
            onError?.invoke("Nothing on this phone opens $uri")
        }
    }

    private inner class Bridge {
        @JavascriptInterface
        fun postMessage(json: String) {
            val obj = try { JSONObject(json) } catch (_: Exception) { return }
            val type = obj.optString("type")
            val payload = obj.optJSONObject("payload") ?: JSONObject()
            main.post { handle(type, payload) }
        }
    }

    private fun handle(type: String, payload: JSONObject) {
        when (type) {
            "height" -> {
                val dp = Math.ceil(payload.optDouble("height", 0.0)).toInt()
                val px = (dp * resources.displayMetrics.density).toInt()
                if (layoutParams.height != px) {
                    layoutParams = layoutParams.apply { height = px }
                    onHeightChange?.invoke(dp)
                }
            }
            "open_url" -> payload.optString("url").takeIf { it.isNotEmpty() }?.let { openOutside(Uri.parse(it)) }
            "open_app_link" -> {
                val appUrl = payload.optString("appUrl").takeIf { it.isNotEmpty() } ?: return
                val fallbackUrl = payload.optString("fallbackUrl").takeIf { it.isNotEmpty() } ?: return
                OuterJoynAppLink.open(context, appUrl, fallbackUrl)
            }
            "open_widget" -> {
                val widget = OJWidget.entries.firstOrNull { it.value == payload.optString("widget") } ?: return
                val partners = payload.optJSONArray("partners")?.let { a -> List(a.length()) { a.getString(it) } }
                val handler = onOpenWidget
                if (handler == null) onError?.invoke("Set onOpenWidget to open the ${widget.value} window")
                else handler(widget, partners)
            }
            "offer_event" -> payload.optString("eventId").takeIf { it.isNotEmpty() }?.let { onOfferEvent?.invoke(it) }
            "error" -> onError?.invoke(payload.optString("message").ifEmpty { "The offers could not be shown." })
        }
    }

    private companion object {
        // Kept the same as the iOS kit's PromotionsPage.
        fun page(options: JSONObject, configure: JSONObject): String {
            val opts = options.toString().replace("</", "<\\/")
            val conf = configure.toString().replace("</", "<\\/")
            return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
              <style>
                * { box-sizing: border-box; }
                html, body { margin: 0; padding: 0; background: transparent; font-family: system-ui, sans-serif; }
                #oj-promos { display: flow-root; }
              </style>
            </head>
            <body>
              <div id="oj-promos"></div>
              <script src="https://sdk.outerjoyn.com/v1/outerjoyn.js"></script>
              <script>
                (function() {
                  function send(type, payload) { OJBridge.postMessage(JSON.stringify({ type: type, payload: payload || {} })); }
                  var opts = $opts;
                  var configure = $conf;
                  var box = document.getElementById('oj-promos');

                  // The page's height, so the app sizes the view to the offers.
                  // An open details window counts too, so it is never cut off.
                  var last = -1;
                  function measure() {
                    var h = box.getBoundingClientRect().height;
                    document.querySelectorAll('[role="dialog"]').forEach(function(d) {
                      var c = d.firstElementChild || d;
                      h = Math.max(h, c.scrollHeight + 32);
                    });
                    h = Math.ceil(h);
                    if (h !== last) { last = h; send('height', { height: h }); }
                  }
                  new ResizeObserver(measure).observe(box);
                  new MutationObserver(measure).observe(document.body, { childList: true, subtree: true, attributes: true });

                  // Pages and app links leave the view; OuterJoyn windows go to the app.
                  window.open = function(url) { send('open_url', { url: String(url) }); return null; };
                  window.addEventListener('outerjoyn:offer', function(e) { send('offer_event', { eventId: e.detail && e.detail.eventId }); });

                  var tries = 0;
                  function start() {
                    var oj = window.OuterJoyn;
                    if (!oj || !oj.promotions) {
                      if (++tries < 25) return setTimeout(start, 200);
                      return send('error', { message: 'The OuterJoyn SDK did not load.' });
                    }
                    ['connect', 'redeem', 'pay', 'account', 'exchange'].forEach(function(name) {
                      var w = oj[name];
                      if (w && typeof w.open === 'function') {
                        w.open = function(o) { send('open_widget', { widget: name, partners: (o && o.partners) || null }); };
                      }
                    });
                    oj.promotions.configure(configure);
                    window.ojRenderPromotions = function() {
                      oj.promotions.render(Object.assign({}, opts, {
                        container: box,
                        onError: function(err) { send('error', { message: (err && err.message) || String(err) }); }
                      }));
                    };
                    window.ojRenderPromotions();
                  }
                  start();
                })();
              </script>
            </body>
            </html>
            """.trimIndent()
        }
    }
}

/**
 * Compose: a promotion set's offers, as tall as its offers need. Link, rewards
 * and other OuterJoyn windows open in the kit's [OuterJoynSheet].
 *
 * ```kotlin
 * OuterJoynPromotions(setId = "380551304567336520", layout = OJPromotionsLayout.Grid,
 *                     look = mapOf("buttonColor" to "#e11d48"), sessionToken = token)
 * ```
 */
@Composable
fun OuterJoynPromotions(
    setId: String,
    modifier: Modifier = Modifier,
    layout: OJPromotionsLayout? = null,
    look: Map<String, Any> = emptyMap(),
    sessionToken: String? = null,
    apiBaseUrl: String = OuterJoynSDK.apiBaseUrl,
    onOfferEvent: ((String) -> Unit)? = null,
    onError: ((String) -> Unit)? = null,
) {
    var heightDp by remember { mutableIntStateOf(0) }
    var sheet by remember { mutableStateOf<Pair<OJWidget, List<String>?>?>(null) }
    var view by remember { mutableStateOf<OuterJoynPromotionsView?>(null) }

    AndroidView(
        factory = { context -> OuterJoynPromotionsView(context).also { view = it } },
        update = { v ->
            v.onHeightChange = { heightDp = it }
            v.onOfferEvent = onOfferEvent
            v.onError = onError
            v.onOpenWidget = { widget, partners ->
                if (sessionToken == null) onError?.invoke("This offer needs a sessionToken.")
                else sheet = widget to partners
            }
            v.show(setId, layout, look, sessionToken, apiBaseUrl)
        },
        modifier = modifier.fillMaxWidth().height(heightDp.dp),
    )

    val open = sheet
    if (open != null && sessionToken != null) {
        OuterJoynSheet(
            visible = true,
            widget = open.first,
            config = OJWidgetConfig(sessionToken = sessionToken, apiBaseUrl = apiBaseUrl, partners = open.second),
            onDismiss = { sheet = null },
            onSuccess = { view?.refresh() },
            onError = onError,
        )
    }
}
