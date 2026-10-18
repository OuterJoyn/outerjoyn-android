# OuterJoyn SDK for Android (beta)

Let customers link their loyalty accounts, pay with points and more, from your Android app. Each widget opens in a Material 3 bottom sheet. Partner logins run in a Chrome Custom Tab.

> **Beta.** The API may change before 2.0. Tell us what you hit: support@outerjoyn.com.

Requires Android 7.0 (API 24)+ and Jetpack Compose.

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
implementation("com.github.OuterJoyn:outerjoyn-android:1.2.0")
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
           extras = mapOf("preselectedBrandId" to "<partner program id>"), // optional: skip the program list
       ),
       onDismiss = { showLink = false },
       onSuccess = { result -> /* linked */ },
   )
   ```

3. **Treat the server as the source of truth**: subscribe to the `account_link.completed` webhook.

No manifest entries are needed for partner logins; the SDK's return activity is merged into your app automatically.

## Look

```kotlin
OuterJoynSDK.configure(OuterJoynAppearance(colors = OJColors(primary = Color(0xFF6750A4))))
```

## Docs

https://docs.outerjoyn.com/guides/retailer/ACCOUNT-LINKING-EMBED

## License

MIT — see [LICENSE](LICENSE).
