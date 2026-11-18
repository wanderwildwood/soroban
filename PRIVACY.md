# Privacy

Abacus works everything out on the phone, and it cannot go online: it has no internet
permission. Its exchange rates come with it.

That is the whole policy. The rest of this page is the evidence for it.

## No permissions

`app/src/main/AndroidManifest.xml` declares none, and `INTERNET` in particular is not there,
so Android gives the app no way to reach the network.

The built APK lists only `com.wanderwildwood.soroban.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`,
which AndroidX adds so that the app's own internal broadcasts stay private to it; it lets
nothing in or out.

The exchange rates are one day's file from
[Fawaz Ahmed's currency API](https://github.com/fawazahmed0/exchange-api), CC0, the source
Unitto uses, taken when this version of the app was made and carried inside it
(`app/src/main/res/raw/rates_usd.json`). The converter shows their date. Nothing is fetched,
in the background or otherwise, and there is no scheduled job and no notification.

Earlier versions fetched that file while currency was open and kept it in the app's storage
(`rates/`); this version deletes that copy the first time the converter opens.

## What it keeps

All in the app's own storage, which no other app can read:

- the last hundred sums worked out with =, for the tape (`history.json`);
- the settings, the page last open, the units last used in each kind, the graph's functions
  and window, and the programmer's base and word size (`SharedPreferences`);
- whether dark mode is on, and Home's dark mode switch as last heard (`ui/DarkMode.kt`).

If Home (`com.wanderwildwood.iori`) is installed, Abacus asks it whether its dark mode switch
is on, a yes or no, and sends nothing to it.

While Settings → Turn sideways with the phone is on, the app reads the phone's accelerometer
while it is open, only to turn the graph. Nothing of it is kept.

Clearing the tape is the last row of the tape. Uninstalling the app removes all of it.

The app is excluded from Android backups (`allowBackup="false"`).

## What it does not have

No analytics, no crash reporting, no advertising, no account. Unitto, which this is built on,
has none of these either.

Copying or sharing a number hands it to the clipboard or the app you share it with, and only
when you ask.

<!-- shared-about privacy section -->
## Who we are

Abacus is made by wander wildwood, an independent developer: one person, not a company. wander wildwood is the data controller for anything the app itself handles, and that is very little. What you put in Abacus stays on your phone. The app sends nothing out: it has no internet permission. The developer receives nothing, so the developer holds no data about you.

## How to contact us

Write to email@wanderthe.dev about your data, this policy or anything else. The same address is in the app, under About.

## Your rights

If you are in the EU, the EEA or the UK, the GDPR gives you the right to:

- see the personal data held about you (access)
- have it corrected (rectification) or deleted (erasure)
- limit how it is used (restriction)
- object to its use (objection)
- get a copy to take elsewhere (portability)
- withdraw your consent at any time, where something rests on it
- complain to a data protection supervisory authority, for example the one where you live

Similar laws elsewhere, such as the CCPA in California, give you the right to know, delete and correct, and to opt out of the sale or sharing of your data. Nothing from Abacus is sold or shared.

Since Abacus keeps what it stores only on your phone, you can exercise most of these rights yourself: clear the tape in the app, clear the app's data in Android's settings, or uninstall the app, which deletes all of it. For anything you cannot do yourself, write to the address above.
<!-- /shared-about privacy section -->
