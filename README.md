<div align="center">

# MediaSwipe

**A Tinder-style swipe app for cleaning up your phone's photo and video library — swipe to keep or stage for deletion, review before anything is touched, then send it through a Recently Deleted screen before it's gone for good.**

[![Kotlin](https://img.shields.io/badge/kotlin-drew?style=flat-square&logo=kotlin&logoColor=white&color=7F52FF)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/jetpack%20compose-drew?style=flat-square&logo=jetpackcompose&logoColor=white&color=4285F4)](https://developer.android.com/jetpack/compose)
[![Android](https://img.shields.io/badge/android-drew?style=flat-square&logo=android&logoColor=white&color=3DDC84)](https://developer.android.com/)
[![License](https://img.shields.io/badge/license-MIT-green?style=flat-square)](LICENSE)

</div>

---

## Overview

MediaSwipe is a native Android app (Kotlin + Jetpack Compose) for quickly
sorting through a phone's photo and video library. Pick how many items you
want to go through, then swipe: left stages something for deletion, right
keeps it. Nothing is ever touched on your device until you explicitly
confirm — deletion is a deliberate two-step process, not an accident one
swipe away.

An iOS (`.ipa`) version may follow at some point.

---

## How It Works

```
Grant photo/video access
        |
Pick a session size (10 / 25 / 50 / 100 / all unsorted)
        |
Swipe deck  ->  left = stage for deletion, right = keep
        |
Review screen  ->  unstage anything you change your mind on
        |
"Move to Recently Deleted"  ->  local staging only, nothing deleted yet
        |
Recently Deleted screen  ->  restore items, or "Delete Forever"
        |
Delete Forever  ->  real system consent dialog, then permanent removal
```

Sorting progress (what's been swiped) and pending deletions both persist
locally across app restarts, so a session can be picked back up later.

---

## Features

- **Session sizing** — choose how many unsorted photos/videos to review in
  one sitting instead of committing to the whole library at once.
- **Two-step deletion** — swiping stages, it doesn't delete. A dedicated
  Recently Deleted screen holds everything until you either restore it or
  permanently delete it via Android's real system consent dialog
  (`MediaStore.createDeleteRequest`) — no reliance on any particular
  gallery app's own trash view.
- **Favouriting** — integrates with the real system Photos "favourite"
  flag (`MediaStore.createFavoriteRequest`), not just an in-app tag.
- **Video support** — videos are mixed into the same swipe deck as photos,
  sorted by recency. Tap a video's thumbnail to play it inline (via Media3
  ExoPlayer) before deciding, with proper transport controls.
- **Sorted/unsorted tracker** — a persistent count of how much of the
  library has been reviewed, with a one-tap reset if you want to go through
  everything again.
- **Share** — send the current photo or video to any other app.
- **Undo** — step back through your last few swipes if you change your mind.

---

## Permissions

| Permission | Why |
|---|---|
| `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` (API 33+) or `READ_EXTERNAL_STORAGE` (below) | Read the photo/video library to build the swipe deck |
| `READ_MEDIA_VISUAL_USER_SELECTED` (API 34+) | Support partial library access grants |

No other permissions are requested. Deletion and favouriting both go
through Android's standard `MediaStore` consent-dialog APIs rather than
any broader storage access.

---

## Build

```bash
git clone https://github.com/drew-codes-things/MediaSwipe.git
cd MediaSwipe
./gradlew assembleDebug
```

The built APK lands at `app/build/outputs/apk/debug/app-debug.apk`. Minimum
supported Android version is 10 (API 29).

## Download

Grab the latest built APK from the [Releases](../../releases) page instead
of building it yourself.

---

## Get the Code

Clone with git:

```bash
git clone https://github.com/drew-codes-things/MediaSwipe.git
```

Or with the [GitHub CLI](https://cli.github.com/):

```bash
gh repo clone drew-codes-things/MediaSwipe
```

## License

MIT - made by [Drew](https://github.com/drew-codes-things)
