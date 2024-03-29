![GitHub Release](https://img.shields.io/github/v/release/static-var/vlr-gg) ![GitHub Repo stars](https://img.shields.io/github/stars/static-var/vlr-gg) [![Beta Play store release](https://github.com/static-var/vlr-gg/actions/workflows/internal_play_store_release_dispatcher.yml/badge.svg?branch=trunk)](https://github.com/static-var/vlr-gg/actions/workflows/internal_play_store_release_dispatcher.yml)

# 🚧 VLR-GG (Unofficial application) 🚧

<a href='https://play.google.com/store/apps/details?id=dev.staticvar.vlr&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png'/></a>


An unofficial android app for vlr.gg, written in Jetpack compose, which uses REST API that parses vlr.gg (Code for the scrapper can be found [here](https://github.com/akhilnarang/vlrgg-scraper), thanks to [@akhilnarang](https://github.com/akhilnarang) for hosting and maintaining the scrapping code.)

The application is not listed on Play Store, but you will receive in app updates for every release (you can choose to install the update or ignore it)

## 📸 Preview 📸
There were no designs / mock ups during the development of this app, this was developed as a side project / hobby.

| Light                                                                                 | Dark                                                                                 |
|---------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------|
| ![](https://github.com/static-var/vlr-gg/blob/trunk/art/NEWS_LIGHT_VLR.jpg)           | ![](https://github.com/static-var/vlr-gg/blob/trunk/art/NEWS_DARK_VLR.jpg)           |
| ![](https://github.com/static-var/vlr-gg/blob/trunk/art/BLOG_LIGHT_VLR.jpg)           | ![](https://github.com/static-var/vlr-gg/blob/trunk/art/BLOG_DARK_VLR.jpg)           |
| ![](https://github.com/static-var/vlr-gg/blob/trunk/art/MATCH_OVERVIEW_LIGHT_VLR.jpg) | ![](https://github.com/static-var/vlr-gg/blob/trunk/art/MATCH_OVERVIEW_DARK_VLR.jpg) |
| ![](https://github.com/static-var/vlr-gg/blob/trunk/art/MATCH_DETAILS_LIGHT_VLR.jpg)  | ![](https://github.com/static-var/vlr-gg/blob/trunk/art/MATCH_DETAILS_DARK_VLR.jpg)  |
| ![](https://github.com/static-var/vlr-gg/blob/trunk/art/EVENT_OVERVIEW_LIGHT_VLR.jpg) | ![](https://github.com/static-var/vlr-gg/blob/trunk/art/EVENT_OVERVIEW_DARK_VLR.jpg) |
| ![](https://github.com/static-var/vlr-gg/blob/trunk/art/EVENT_LIGHT_VLR.jpg)          | ![](https://github.com/static-var/vlr-gg/blob/trunk/art/EVENT_DARK_VLR.jpg)          |
| ![](https://github.com/static-var/vlr-gg/blob/trunk/art/SQUAD_LIGHT_VLR.jpg)          | ![](https://github.com/static-var/vlr-gg/blob/trunk/art/SQUAD_DARK_VLR.jpg)          |
| ![](https://github.com/static-var/vlr-gg/blob/trunk/art/ABOUT_LIGHT_VLR.jpg)          | ![](https://github.com/static-var/vlr-gg/blob/trunk/art/ABOUT_DARK_VLR.jpg)          |


## ✨ App Overview ✨

The app currently shows the following information
- News and news articles
- (Ongoing, upcoming and completed) matches and matche details
- (Ongoing, upcoming and completed) events and events details
- Team details
- Team ranks per region

#### Additional features
- Users can opt in to get notified for their choice of matches a few minutes before it starts.
- Users can opt in to get notified for their favorite teams' match a few minutes before it starts.
- Widget to see scores and updates on your home screen.
- App uses Material 3 theming.

## 🧑‍💻 Dev Overview 🧑‍💻

The application has been written completely in [Kotlin](https://kotlinlang.org/) and [Jetpack Compose](https://developer.android.com/jetpack/compose)

#### Libraries used:
- [Material3](https://developer.android.com/jetpack/androidx/releases/compose-material3) - M3 opens up new possibilities for both brand colors and individual color preferences to converge in one-of-a-kind experiences. The color system embraces the need for color to reflect an app’s design sensibility, while also honoring the settings that individuals choose for themselves. 
- [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) - Kotlin solves Asynchronous or non-blocking programming problem in a flexible way by providing coroutine support at the language level and delegating most of the functionality to libraries.
- [Hilt](https://developer.android.com/training/dependency-injection/hilt-android) - Hilt is a dependency injection library for Android that reduces the boilerplate of doing manual dependency injection in your project.
- [Room](https://developer.android.com/jetpack/androidx/releases/room) - The Room persistence library provides an abstraction layer over SQLite to allow for more robust database access while harnessing the full power of SQLite.
- [Firebase Analytics](https://firebase.google.com/docs/analytics) - Analytics is an app measurement solution, available at no charge, that provides insight on app usage and user engagement.
- [Firebase Messaging](https://firebase.google.com/docs/cloud-messaging) - Firebase Cloud Messaging (FCM) is a cross-platform messaging solution that lets you reliably send messages at no cost.
- [Ktor-client](https://ktor.io/docs/create-client.html) - Ktor is a framework to easily build connected applications
- [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization) - Kotlin serialization consists of a compiler plugin, that generates visitor code for serializable classes, runtime library with core serialization API and support libraries with various serialization formats.
- [Navigation Compose](https://developer.android.com/jetpack/compose/navigation) - Navigation Compose enables you to navigate between composables while taking advantage of the Navigation component’s infrastructure and features.
- [ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel) - The ViewModel class is designed to store and manage UI-related data in a lifecycle conscious way. The ViewModel class allows data to survive configuration changes such as screen rotations.
- [Glance](https://developer.android.com/reference/kotlin/androidx/glance/package-summary) - Build layouts for remote surfaces using a Jetpack Compose-style API.
- [SplashScreen](https://developer.android.com/guide/topics/ui/splash-screen) - The SplashScreen API enables a new app launch animation for all apps when running on a device with Android 12 or higher. This includes an into-app motion at launch, a splash screen showing your app icon, and a transition to your app itself.
- [Material Icons](https://developer.android.com/reference/kotlin/androidx/compose/material/icons/Icons) - Material Design system icons as seen on Google Fonts.
- [Webkit](https://developer.android.com/reference/android/webkit/package-summary) - Provides tools for browsing the web.
- [Accompanist](https://google.github.io/accompanist/) - Accompanist is a group of libraries that aim to supplement Jetpack Compose with features that are commonly required by developers but not yet available.
  - System Ui Controller
  - Navigation Animation
  - Pager & Pager Indicators
  - Webview
- [Jsoup](https://jsoup.org/) - jsoup is a Java library for working with real-world HTML. It provides a very convenient API for fetching URLs and extracting and manipulating data, using the best of HTML5 DOM methods and CSS selectors.
- [Landscapist-glide](https://github.com/skydoves/landscapist) - Jetpack Compose image loading library which fetches and displays network images.

## License

```
Copyright (c) 2022 Shreyansh Lodha

Permission is hereby granted, free of charge, to any
person obtaining a copy of this software and associated
documentation files (the "Software"), to deal in the
Software without restriction, including without
limitation the rights to use, copy, modify, merge,
publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software
is furnished to do so, subject to the following
conditions:

The above copyright notice and this permission notice
shall be included in all copies or substantial portions
of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF
ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT
SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
DEALINGS IN THE SOFTWARE.
```
