# Changelog

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
