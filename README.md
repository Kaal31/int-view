# Int View

An Android TV, fullscreen **interdimensional-cable** player for `r/ai_video`.

It is a single APK: it authenticates directly with your Reddit account, requests only the `read` scope, builds a queue from the selected Reddit listing, and plays native Reddit videos/direct MP4 posts continuously.

## Controls

| Remote control | Action |
| --- | --- |
| OK | Sign in / pause / play |
| Left / Right | Previous / next video |
| Menu | Cycle Hot → New → Top → Rising → Controversial |

## Build

1. Register a Reddit **installed app** and set its redirect URI to `intview://oauth`.
2. Build with its client ID (this is public; never put a client secret in an APK):

   ```bash
   ./gradlew assembleDebug -PREDDIT_CLIENT_ID=your-client-id
   ```

3. Install `app/build/outputs/apk/debug/app-debug.apk` on Android TV.

GitHub Actions can build the unsigned debug APK. Add a repository variable named `REDDIT_CLIENT_ID` before manually running it, or download a generic build and supply the client ID through a local Gradle build.

## Scope and limitations

This project is for personal use and uses Reddit OAuth/API access. It does not scrape Reddit or proxy media. It deliberately skips unsupported external video hosts; native `v.redd.it` video and direct MP4 posts are the first supported formats.
