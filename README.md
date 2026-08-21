# Int View

Int View is a fullscreen Android TV **interdimensional cable** player built around curated public YouTube playlists. It is deliberately an experience, not a catalogue: open it and a random channel is already playing.

There is no Reddit integration, login, server, or YouTube Data API key.

## Remote controls

| Remote control | Action |
| --- | --- |
| Left / Right | Change channel |
| OK / centre | Pause or resume |
| Media play/pause/next/previous | Equivalent playback or channel action |

There are no app controls, menus, or overlays.

## Build

Open the project in Android Studio, or run:

```bash
gradle wrapper --gradle-version 8.9
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions also produces it as the `int-view-debug-apk` artifact.

## Notes

Playback uses the standard YouTube IFrame Player API and public playlists. YouTube and video owners control availability, embedding, and any YouTube-provided branding or playback notices.

Voice-over-translation is intentionally not bundled: it is a browser extension/userscript, not an Android TV player SDK. A reliable native translation feature would need a separately authorised translation provider and audio/subtitle pipeline.
