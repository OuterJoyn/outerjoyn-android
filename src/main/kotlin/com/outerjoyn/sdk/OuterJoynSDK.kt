package com.outerjoyn.sdk


/**
 * OuterJoyn SDK — Native Android integration for loyalty point widgets.
 *
 * Presents loyalty widgets in a Material 3 bottom sheet with web content
 * loaded from the OuterJoyn CDN. Partner logins run in a Chrome Custom Tab
 * (OuterJoynLoginTab); the return activity is merged into the host app's
 * manifest, so there is no setup for it.
 *
 * ## Usage
 * ```kotlin
 * // Optional: match your app's look
 * OuterJoynSDK.configure(OuterJoynAppearance(colors = OJColors(primary = Color(0xFF6750A4))))
 *
 * // In a Composable
 * OuterJoynSheet(
 *     visible = showLink,
 *     widget = OJWidget.Connect,
 *     config = OJWidgetConfig(sessionToken = tokenFromYourServer),
 *     onDismiss = { showLink = false },
 * )
 * ```
 */
object OuterJoynSDK {
    // Keep equal to the publication version in build.gradle.kts.
    const val VERSION = "1.2.0"

    var appearance = OuterJoynAppearance()
        private set

    var apiBaseUrl = "https://api.outerjoyn.com"
        private set

    fun configure(appearance: OuterJoynAppearance) {
        this.appearance = appearance
    }

    fun configure(apiBaseUrl: String) {
        this.apiBaseUrl = apiBaseUrl
    }
}

/**
 * Widget types supported by the SDK.
 */
enum class OJWidget(val value: String) {
    Pay("pay"),
    Exchange("exchange"),
    Redeem("redeem"),
    Connect("connect"),
    Account("account")
}

/**
 * Configuration for presenting a widget.
 *
 * THE ORDER BREAKDOWN ([merchandiseSubtotalCents], [taxCents], [shippingCents])
 * tells the server how much of the order points may cover — the "redeemable
 * basis".
 *
 * Pass all three on every pay checkout. WHICH of them the server requires is a
 * per-storefront setting the retailer can change without any app update, so a
 * checkout that always reports its whole breakdown is correct under every
 * configuration, while one that reports a subset starts failing with 422
 * (`merchandise_subtotal_required` / `tax_amount_required` /
 * `shipping_amount_required`) the moment that setting changes — and a shipped
 * mobile binary cannot be hotfixed.
 *
 * `0` is a value; omitting a field is not the same thing. Free shipping is
 * `shippingCents = 0`. `null` means "this checkout cannot report that
 * component" and is rejected — deliberately, so an unreported component can
 * never silently under-report the basis. The corollary: do NOT pass `0` for a
 * figure you merely don't have, since that asserts the component IS zero and
 * caps redemption accordingly.
 */
data class OJWidgetConfig(
    val sessionToken: String,
    val apiBaseUrl: String = OuterJoynSDK.apiBaseUrl,
    val orderTotalCents: Int? = null,
    val merchandiseSubtotalCents: Int? = null,
    val taxCents: Int? = null,
    val shippingCents: Int? = null,
    val direction: String? = null,
    val lockTargetToBrand: Boolean? = null,
    /**
     * Arbitrary extra widget options, merged over the typed fields above.
     *
     * NOTE: this was declared here but NEVER serialized — `OuterJoynSheet`
     * built its options object from the typed fields only, so anything passed
     * here was silently dropped on Android while the identical iOS API worked.
     * Now honored; see the merge in OuterJoynSheet.kt.
     */
    val extras: Map<String, Any> = emptyMap()
)
