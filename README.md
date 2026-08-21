# Int View

Int View is a fullscreen Android TV and phone **interdimensional cable** player built around curated public YouTube playlists. It is deliberately an experience, not a catalogue: open it and a random channel is already playing.

There is no Reddit integration, login, or server. It runs from curated public YouTube playlists by default.

## Remote controls

| Remote control | Action |
| --- | --- |
| Left / Right | Change channel |
| OK / centre | Pause or resume |
| Media play/pause/next/previous | Equivalent playback or channel action |

## Phone controls

| Touch gesture | Action |
| --- | --- |
| Swipe left / right | Change channel |
| Tap | Pause or resume |

There are no app controls, menus, or overlays.

## Build

Store your key once in the ignored `local.properties` file at the project root:

```properties
YOUTUBE_API_KEY=your-google-api-key
```

Then open the project in Android Studio, or run:

```bash
gradle wrapper --gradle-version 8.9
./gradlew assembleDebug
```

The API key is optional. With it, each channel switches to a themed YouTube Data API v3 search (random videos matching topics such as AI shorts, surreal animation, and analog horror). Without it, the app uses its curated playlist fallback. The key remains present for future local builds but is ignored by Git. The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Notes

Playback uses the standard YouTube IFrame Player API. YouTube and video owners control availability, embedding, and any YouTube-provided branding or playback notices. The API key is not secret once embedded in an APK: restrict it in Google Cloud to the YouTube Data API v3 and this Android app's package/signing certificate. Do not add it to this repository or a public Actions artifact.

Voice-over-translation is intentionally not bundled: it is a browser extension/userscript, not an Android TV player SDK. A reliable native translation feature would need a separately authorised translation provider and audio/subtitle pipeline.
