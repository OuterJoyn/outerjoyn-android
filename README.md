# OuterJoyn SDK for Android (beta)

Let customers link their loyalty accounts, pay with points and more, from your Android app. Each widget opens in a Material 3 bottom sheet. Partner logins run in a Chrome Custom Tab.

> **Beta.** The API may change before 2.0. Tell us what you hit: support@outerjoyn.com.

Requires Android 7.0 (API 24)+ and Jetpack Compose 1.7+ with material3 1.3+. Compile against SDK 34 or newer.

## Install

Add JitPack to your repositories (`settings.gradle.kts`):

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

Then the dependency:

```kotlin
implementation("com.github.OuterJoyn:outerjoyn-android:1.5.0")
```

## Link an account

1. **On your server**, start a short-lived session for the signed-in customer with your secret API key (never put the key in the app):

   ```http
   POST https://api.outerjoyn.com/api/sdk/sessions
   x-api-key: <your secret key>
   content-type: application/json

   { "partner_member_id": "<your id for the customer>", "scopes": ["link"] }
   ```

   Send the returned `token` to the app.

2. **In the app**, show the link sheet:

   ```kotlin
   var showLink by remember { mutableStateOf(false) }

   Button(onClick = { showLink = true }) { Text("Link your account") }

   OuterJoynSheet(
       visible = showLink,
       widget = OJWidget.Connect,
       config = OJWidgetConfig(
           sessionToken = token,
           partners = listOf("<partner program id>"), // optional: only these partners; one skips the program list
       ),
       onDismiss = { showLink = false },
       onSuccess = { result -> /* linked */ },
   )
   ```

3. **Treat the server as the source of truth**: subscribe to the `account_link.completed` webhook.

No manifest entries are needed for partner logins; the SDK's return activity is merged into your app automatically.

## Offers

Show a promotion set's offers inline. The view grows to fit its offers. Buttons that go to a web page open the browser, "Open in the app" buttons open the brand's app, and link or rewards buttons open the kit's sheet.

```kotlin
// Compose
OuterJoynPromotions(
    setId = "<promotion set id>",
    layout = OJPromotionsLayout.Grid,                          // optional: instead of the set's own
    look = mapOf("buttonColor" to "#e11d48", "columns" to 2),  // optional: any saved set setting, for this spot only
    sessionToken = token,                                      // optional: null shows the offers everyone can see
)

// Views: OuterJoyn windows go to onOpenWidget (open an OuterJoynSheet there)
val offers = OuterJoynPromotionsView(context)
offers.onOpenWidget = { widget, partners -> /* show OuterJoynSheet(widget, OJWidgetConfig(token, partners = partners)) */ }
offers.show(setId = "<promotion set id>", sessionToken = token)
container.addView(offers)
```

`look` takes any setting from `GET /api/v1/promotion-sets/design-settings` (`accentColor`, `buttonColor`, `columns`, `bannerLook`, ...). Layouts: `Carousel`, `Grid`, `List`, `Hero`, `Strip`, `Banner`.

## Open in the app

An offer button can open a brand's app. When the button is tapped the kit opens the app if it is installed, or the button's web page in the browser if not. Nothing to set up in your app.

## Look

```kotlin
OuterJoynSDK.configure(OuterJoynAppearance(colors = OJColors(primary = Color(0xFF6750A4))))
```

## Docs

https://docs.outerjoyn.com/guides/retailer/ACCOUNT-LINKING-EMBED

## License

MIT — see [LICENSE](LICENSE).
