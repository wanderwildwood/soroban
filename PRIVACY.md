# Privacy

Calculator works everything out on the phone. The one thing it fetches from the internet is the
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
