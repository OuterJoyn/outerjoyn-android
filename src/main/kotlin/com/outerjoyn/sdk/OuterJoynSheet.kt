package com.outerjoyn.sdk

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.os.Handler
import android.os.Looper
import org.json.JSONObject

/**
 * Material 3 bottom sheet that presents an OuterJoyn SDK widget.
 *
 * Loads widget content from sdk.outerjoyn.com CDN inside a WebView,
 * with CSS custom properties injected from OuterJoynAppearance.
 *
 * @param visible Whether the sheet is shown
 * @param widget Which widget to present
 * @param config Widget configuration
 * @param onDismiss Called when sheet is dismissed
 * @param onSuccess Called when widget action completes
 * @param onError Called on error
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OuterJoynSheet(
    visible: Boolean,
    widget: OJWidget,
    config: OJWidgetConfig,
    onDismiss: () -> Unit,
    onSuccess: ((Map<String, Any?>) -> Unit)? = null,
    onError: ((String) -> Unit)? = null
) {
    if (!visible) return

    val appearance = LocalOJAppearance.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = (appearance.shapes.cornerRadiusDp + 8).dp,
            topEnd = (appearance.shapes.cornerRadiusDp + 8).dp
        ),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        var isLoading by remember { mutableStateOf(true) }

        // A concrete height: the sheet measures content with an unbounded height,
        // where a fraction resolves to nothing and the page would size to zero.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((LocalConfiguration.current.screenHeightDp * 0.85f).dp)
        ) {
            // WebView with SDK widget
            OJWebView(
                widget = widget,
                config = config,
                appearance = appearance,
                onFinishLoading = { isLoading = false },
                onMessage = { type, payload ->
                    when (type) {
                        "success" -> {
                            onSuccess?.invoke(payload)
                            onDismiss()
                        }
                        "error" -> {
                            onError?.invoke(payload["message"] as? String ?: "Unknown error")
                        }
                        "close" -> onDismiss()
                    }
                }
            )

            // Loading overlay
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = appearance.colors.primary
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun OJWebView(
    widget: OJWidget,
    config: OJWidgetConfig,
    appearance: OuterJoynAppearance,
    onFinishLoading: () -> Unit,
    onMessage: (String, Map<String, Any?>) -> Unit
) {
    val cssVars = appearance.toCSSVariables()
    val widgetName = widget.value

    val optionsObj = JSONObject().apply {
        put("sessionToken", config.sessionToken)
        put("apiBaseUrl", config.apiBaseUrl)
        config.orderTotalCents?.let { put("orderTotal", it) }
        // The redeemable basis: a 0 is sent (nothing charged for that part); null
        // is left out entirely.
        config.merchandiseSubtotalCents?.let { put("merchandiseSubtotalCents", it) }
        config.taxCents?.let { put("taxCents", it) }
        config.shippingCents?.let { put("shippingCents", it) }
        config.direction?.let { put("direction", it) }
        config.lockTargetToBrand?.let { put("lockTargetToBrand", it) }
        // Connect fills the sheet edge to edge instead of drawing its own card.
        if (widget == OJWidget.Connect) put("presentation", "sheet")
        // Extras merge last, so they override the typed fields (as on iOS).
        for ((key, value) in config.extras) {
            put(key, value)
        }
    }
    // Escape </script> in serialized JSON to prevent HTML injection
    val optionsJson = optionsObj.toString().replace("</script>", "<\\/script>")

    val html = """
    <!DOCTYPE html>
    <html>
    <head>
      <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
      <style>
        :root { $cssVars }
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: system-ui, sans-serif; background: transparent; }
        html, body, #oj-widget { height: 100%; }
      </style>
    </head>
    <body>
      <div id="oj-widget"></div>
      <script>${OuterJoynLoginTab.windowOpenBridge}</script>
      <script src="https://sdk.outerjoyn.com/v1/outerjoyn.js"></script>
      <script>
        (function() {
          var opts = $optionsJson;
          opts.container = '#oj-widget';
          opts.onSuccess = function(result) {
            OJBridge.postMessage(JSON.stringify({ type: 'success', payload: result || {} }));
          };
          opts.onClose = function() {
            OJBridge.postMessage(JSON.stringify({ type: 'close' }));
          };
          opts.onError = function(err) {
            OJBridge.postMessage(JSON.stringify({ type: 'error', payload: { message: err.message || String(err) } }));
          };
          var _tryCount = 0;
          function tryOpen() {
            if (window.OuterJoyn && window.OuterJoyn.$widgetName) {
              window.OuterJoyn.$widgetName.open(opts);
            } else if (++_tryCount < 10) {
              setTimeout(tryOpen, 200);
            } else {
              opts.onError({ message: 'OuterJoyn SDK failed to load after 2s' });
            }
          }
          tryOpen();
        })();
      </script>
    </body>
    </html>
    """.trimIndent()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) OuterJoynLoginTab.onHostResumed()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                val webView = this
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun postMessage(json: String) {
                        try {
                            val obj = JSONObject(json)
                            val type = obj.getString("type")
                            val payload = obj.optJSONObject("payload")
                            val payloadMap = mutableMapOf<String, Any?>()
                            payload?.keys()?.forEach { key ->
                                payloadMap[key] = payload.opt(key)
                            }
                            Handler(Looper.getMainLooper()).post {
                                if (type == "login") {
                                    OuterJoynLoginTab.open(
                                        context,
                                        webView,
                                        payloadMap["id"] as? String ?: return@post,
                                        payloadMap["url"] as? String ?: return@post,
                                    )
                                } else {
                                    onMessage(type, payloadMap)
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }, "OJBridge")

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        onFinishLoading()
                    }
                }

                loadDataWithBaseURL(
                    "https://sdk.outerjoyn.com",
                    html,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
