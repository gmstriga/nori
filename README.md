[![GitHub license](https://img.shields.io/badge/license-GPLv2-blue.svg)](https://raw.githubusercontent.com/tjg1/nori/master/LICENSE)
[![GitHub stars](https://img.shields.io/github/stars/tjg1/nori.svg)](https://github.com/tjg1/nori/stargazers)
[![Codacy grade](https://img.shields.io/codacy/grade/116eaec4502d4a88acf6eeb60ad98577.svg?maxAge=2592000)](https://www.codacy.com/app/tjg1/nori)
[![Twitter URL](https://img.shields.io/twitter/url/http/shields.io.svg?style=social&maxAge=2592000)](https://twitter.com/Nori_Android)

Nori is a free and open source Android client for tag-based image archives and galleries. Work is currently underway to make a version that does not violate the Google Play Store [Developer Content Policy](https://play.google.com/about/developer-content-policy/).

If you're interested in contributing to Nori, have any questions or just want to hang out, join us in the `#nori` IRC channel on Freenode.

### Download ###

Nori can be downloaded from [F-Droid](https://f-droid.org/repository/browse/?fdid=io.github.tjg1.nori), the Free and Open Source Android App Repository.

We also publish APKs for each release, on [GitHub](https://github.com/tjg1/nori/releases).

### Build ###

To build Nori, first make sure you have cloned the repository recursively to also get a copy of [norilib](https://github.com/tjg1/norilib), our API client library:

```bash
$ git clone --recursive https://github.com/tjg1/nori
```

Providing you have the Java JDK and Android SDK installed on your computer, you should now be able to use the Gradle wrapper to build Nori:

```bash
$ cd nori
$ ./gradlew build
```

You can also use [Android Studio](https://developer.android.com/studio/index.html) to build Nori.

### Donations ###

If you would like to help keep Nori free (and free of ads) by supporting its continued development, you can do so by making a monthly pledge to [my Patreon account](https://www.patreon.com/user?u=3696048). 

Alternatively, you can make a one-off donation to [my PayPal account](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=FSVJZBNKMVZ9J).
