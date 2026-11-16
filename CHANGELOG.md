# Changelog

## 1.5.2 (beta)

- `OuterJoynPromotions` (Compose) and `OuterJoynPromotionsView` (a WebView) show a promotion set's offers inline, sized to fit. Pass `layout` and `look` (any saved set setting) for this spot only. Web pages and app links from offer buttons open outside the view; link and rewards buttons open the kit's sheet.

## 1.5.1 (beta)

- The appearance you provide (`LocalOJAppearance`) now styles the hosted widgets, the link window included, not only the sheet around them.
- The widgets follow the phone's light or dark mode when the brand's OuterJoyn Design > Appearance is System. Pass `followDeviceColorScheme = false` to keep the brand's look.

## 1.5.0 (beta)

- "Open in the app" offer buttons open the brand's app when it is installed, else the button's web page in the browser. An https link counts only when an app, not a browser, takes it. No manifest `<queries>` entry needed.
- The sheet uses your app's window colour: no white band or corners around the link window on dark apps.

## 1.4.0 (beta)

- `OJWidgetConfig(partners = ...)` shows only the partner programs you name in the link sheet. One opens straight on its sign-in; several are the whole list. An id your store can't link ends on an error with code `partner_not_linkable`.
- Lists and maps in `extras` now reach the widget as JSON lists and objects.

## 1.3.1 (beta)

- Each app returns from a partner login to its own address, so two apps with the kit on one phone no longer both offer to open the result.

## 1.3.0 (beta)

- When a brand needs a confirmation code to link an account, the link sheet asks for it and finishes the link.
- Built on Compose 1.7 and material3 1.3: the link sheet no longer crashes in apps using them.

## 1.2.0 (beta)

- First public release.
- Partner logins run in a Chrome Custom Tab; the result returns to the link sheet. No manifest setup needed.
- The link sheet opens at 85% of the screen and the link window fills it.
