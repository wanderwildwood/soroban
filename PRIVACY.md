# Privacy

Abacus works everything out on the phone. The one thing it fetches from the internet is the
day's exchange rates, while currency is open in the converter, and it sends nothing but the
request for them.

That is the whole policy. The rest of this page is the evidence for it.

## One permission

`app/src/main/AndroidManifest.xml` declares exactly one:

```
android.permission.INTERNET
```

The built APK also lists `com.wanderwildwood.soroban.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`,
which AndroidX adds so that the app's own internal broadcasts stay private to it; it lets
nothing in or out.

`INTERNET` is used for the exchange rates and nothing else. Every connection the app makes is in
`convert/Rates.kt`: a plain request for one file,

```
https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.json
```

or, when that cannot be reached, the same file from the API's own mirror,

```
https://latest.currency-api.pages.dev/v1/currencies/usd.json
```

The rates are [Fawaz Ahmed's currency API](https://github.com/fawazahmed0/exchange-api), CC0,
the source Unitto uses, served by jsDelivr and by Cloudflare Pages. As with any web request,
they see the phone's IP address and the time. The request carries no amount, no currency
chosen, no identifier and no cookie, and it names itself only as `soroban` rather than with
the phone's model, which Android would otherwise send. The same file is fetched whatever is
being converted: every rate is worked out on the phone from the one list.

**It is fetched only while currency is open**, and once it has arrived, not again that day; a
fetch that fails is tried again the next time currency is opened, or when the line saying so
is pressed. There is no background work, no scheduled job and no notification. Calculating,
the programmer's calculator, graphs, every other kind of unit, number bases and dates never
touch the network.

## What it keeps

All in the app's own storage, which no other app can read:

- the last hundred sums worked out with =, for the tape (`history.json`);
- the last exchange rates fetched, and the day they were fetched (`rates/`);
- the settings, the page last open, the units last used in each kind, the graph's functions
  and window, and the programmer's base and word size (`SharedPreferences`).

Clearing the tape is the last row of the tape. Uninstalling the app removes all of it.

The app is excluded from Android backups (`allowBackup="false"`).

## What it does not have

No analytics, no crash reporting, no advertising, no account. Unitto, which this is built on,
has none of these either.

Copying or sharing a number hands it to the clipboard or the app you share it with, and only
when you ask.

<!-- shared-about privacy section -->
## Who we are

Abacus is made by wander wildwood, an independent developer: one person, not a company. wander wildwood is the data controller for anything the app itself handles, and that is very little. What you put in Abacus stays on your phone. The only thing that goes out is the request for exchange rates to jsDelivr or Cloudflare Pages described above, which carries no amount, no currency chosen and nothing you have written. The developer receives nothing, so the developer holds no data about you.

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

Abacus connects to jsDelivr and Cloudflare Pages. They are separate from this app and from its developer, and what they receive is covered by their own privacy policies; ask them about what they hold.
<!-- /shared-about privacy section -->
