# Reddit Provider Design

This document defines the intended behavior of the Reddit-only test build on the `reddit-test` branch. It is the product contract for the branch: implementation details may change, but the observable behavior and acceptance criteria below should remain stable.

## Purpose

The Reddit build turns `r/aivideo` into a fullscreen, interface-free “interdimensional cable” channel for Android TV and Android phones.

It discovers video posts from Reddit, resolves each post to playable media, randomizes the channel, remembers watched videos, and continuously recovers from bad posts or stalled playback. It must never expose the Reddit page or another provider's webpage to the viewer.

This branch is deliberately Reddit-only. The normal YouTube search and curated-playlist pipeline is excluded so Reddit behavior can be tested independently.

## Required user experience

- Launch directly into fullscreen playback with no catalog, menus, or permanent controls.
- Show no webpage, Reddit UI, provider UI, advertisements from scraped pages, or embedded browser chrome.
- Begin playback as quickly as practical instead of leaving an indefinite black screen.
- Randomize the order of resolved posts for every newly fetched batch.
- Never intentionally replay a video already watched, including after an app restart.
- Automatically advance when a video ends.
- Preserve the current video while the next item is resolving. A requested skip becomes pending and completes as soon as a replacement is ready.
- Do not apply the four-second TV tuning/switch mask on the Reddit build.
- Disable subtitles for Reddit-linked YouTube playback.

## Source and discovery

The initial source is the public `r/aivideo` listing, using Reddit's **Hot** ordering. The discovery layer should be designed so other native Reddit sorts can be added later:

- Hot
- New
- Top, with a time range
- Rising, where available

The app does not require a Reddit account, Reddit API client, third-party API key, or self-hosted server for this test mode. It reads a public, server-rendered listing in an invisible WebView and extracts post links and immediately available media metadata.

Discovery requirements:

1. Fetch a full server-rendered listing rather than relying on the few cards initially rendered by modern Reddit.
2. Extract all valid post candidates from the page.
3. Shuffle candidates before resolution.
4. Resolve progressively; playback must not wait for the entire listing.
5. Fetch another page or refresh the listing when the queue is running low or exhausted.
6. Skip deleted posts, text-only posts, malformed URLs, and unsupported media without blocking the queue.

Selectors and parsing rules are fragile by nature and should be isolated so Reddit markup changes can be repaired in one place.

## Media resolution order

A Reddit post may point to several kinds of media. Resolution should prefer the most direct and reliable path:

1. **Native Reddit video**
   - Resolve `v.redd.it` manifests or direct media.
   - Prefer an HLS or DASH representation that includes synchronized audio.
   - Play through Android Media3/ExoPlayer.

2. **YouTube**
   - Extract the canonical YouTube video ID.
   - Play through the app's local YouTube player path.
   - Keep captions/subtitles disabled.
   - Do not use the YouTube Data API for Reddit discovery.

3. **Streamable**
   - Resolve the Streamable ID to a validated direct MP4/HLS URL.
   - Play through Media3/ExoPlayer.

4. **Direct media**
   - Accept validated HTTPS MP4, WebM, HLS, or DASH URLs.
   - Play through Media3/ExoPlayer.

5. **Generic external host**
   - Inspect only long enough to extract a real video URL from supported metadata such as `og:video`, JSON-LD, `video`, or `source` elements.
   - Pass only the validated direct media URL to the player.
   - If no playable media is found, skip the post.
   - Never display the external webpage as content.

DRM-only streams, login-only pages, bot challenges, and hosts that conceal media behind unsupported scripts are allowed to fail and be skipped.

## Queue, randomization, and no-repeat rules

Randomization and deduplication are separate requirements: the queue must be shuffled, and every item must still be checked against history.

Each playable item needs a canonical identity:

| Provider | Canonical identity |
|---|---|
| Reddit post | Reddit post ID |
| Native Reddit video | Reddit media ID or normalized manifest identity |
| YouTube | YouTube video ID |
| Streamable | Streamable video ID |
| Direct/generic media | Stable provider ID, otherwise normalized media URL |

The app must deduplicate against:

- the current resolution queue;
- the current playback session;
- the previous-video back stack;
- a persistent watched-history set capped at approximately 500 entries.

Provider IDs take precedence over raw URLs. A YouTube video discovered through Reddit must be considered the same video if it is later discovered by the normal YouTube pipeline on another branch.

An item should enter watched history only after playback has successfully started. Resolution failures and posts skipped before playback must not permanently consume their identity. Temporary failure caching is acceptable to avoid immediately retrying the same broken item.

## Navigation

### Android TV remote

- Right or Media Next: request the next playable video.
- Left or Media Previous: return to the previous successfully played video.
- Center/Enter or Media Play/Pause: pause or resume playback.

### Android phone

- Swipe left: request the next playable video.
- Swipe right: return to the previous video.
- Tap: pause or resume playback.

Maintain a session back stack of roughly 50 successfully played items. Back navigation must restore a known playable item directly; it should not re-run Reddit discovery.

Rapid repeated Next input must not strand the player. It may collapse into a pending forward request, but the app must eventually advance once a replacement is ready.

## Performance and recovery

Targets are behavioral guardrails rather than guarantees for every network:

- Cold launch should normally start valid playback within 5 seconds on a healthy connection.
- Maintain a ready or resolving buffer of at least 5 candidates, preferably 10.
- Every individual resolution path must time out within 8 seconds.
- A native player that remains buffering for 15 seconds must be abandoned and replaced.
- A failed item must never block later candidates.
- The currently playing video must not be paused or destroyed until the replacement is ready.
- End-of-media, player errors, resolver errors, empty pages, and exhausted queues must all trigger recovery.
- If a page yields too few candidates, paginate or refresh rather than replaying the same first three posts.

All player and resolver state transitions should be logged with a stable item identity and failure reason so device-specific freezes can be diagnosed through Logcat.

## Presentation

- Fullscreen immersive mode.
- No visible playback controls or provider pages.
- Preserve video aspect ratio; letterboxing is preferable to cropping important content.
- Audio and video must use the same playable representation where possible.
- The Reddit test application ID remains `com.intview.tv.reddittest` and the launcher name remains **Int View Reddit Test**, allowing installation beside the normal build.
- Support both Android TV remotes and touch phones.

## Security and privacy

- Do not expose a JavaScript bridge to arbitrary external sites.
- Only validated HTTPS media URLs may reach the native player.
- Limit WebView navigation to the resolver's explicit allowlist and cancel unrelated redirects.
- Do not collect Reddit credentials.
- Do not transmit viewing history off-device.
- Do not embed secrets in the Reddit discovery path.

## Non-goals for this branch

- Reddit login, voting, posting, comments, or authenticated/NSFW feeds.
- A visible feed or settings interface.
- The normal YouTube API search and curated-playlist pipeline.
- Guaranteed playback for every external host.
- Bypassing DRM, authentication, paywalls, or anti-bot systems.
- Guaranteed removal of ads inserted by YouTube or a media provider.
- Voice-over translation.

## Acceptance criteria

A release from `reddit-test` is acceptable when all of the following pass:

1. A cold launch reaches a valid randomized video without exposing a webpage.
2. The user can advance through at least 10 consecutive playable items; the third item is not a terminal state.
3. Fresh sessions produce a different shuffled order while persistent watched-history still prevents repeats.
4. Right/Next advances, Left/Previous returns, and Play/Pause works on an Android TV remote.
5. Swipe and tap equivalents work on a phone.
6. Native Reddit video plays with synchronized audio where Reddit provides audio.
7. A Reddit-linked YouTube video plays with subtitles disabled.
8. At least one supported Streamable or direct-media post plays natively.
9. An unsupported post is skipped within 8 seconds without stopping the current video.
10. A stream stalled in buffering is replaced within approximately 15 seconds.
11. Exhausting the initial listing loads more candidates instead of replaying or freezing.
12. No Reddit page, news site, generic provider page, or webpage advertisement is ever presented as the video.
13. The APK installs beside the normal application as **Int View Reddit Test**.

## Current implementation note

The branch currently uses a server-rendered Reddit listing, shuffles extracted candidates, prioritizes immediately resolvable media, applies per-path timeouts, keeps playback alive during pending forward navigation, and includes a native buffering watchdog. These are implementation choices, not substitutes for the acceptance criteria above. If Reddit changes or redirects the selected listing representation, discovery must fall back or be repaired without weakening the no-webpage and no-freeze guarantees.
