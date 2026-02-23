# Mayank's Completed Issues - Mobile Team

> Issues assigned to mayank@gobazzinga.io and marked as **Done** in the Mobile team on Linear.
> Only includes issues that have a description and/or comments by Mayank.

---

## MOB-812 | Profile picture is displayed at 90 degrees

**Description:**
I changed my profile picture and it was getting displayed at 90 degrees even though the picture was uploaded correctly. This is reproducing in iOS as well as android.

**Mayank's Comments:**
- *2026-01-30:* Finding: raw image is being uploaded without any processing on client. Exif information is being lost on the received processed image. Resolution to be provided by backend team.

---

## MOB-157 | Upload video in background, to ensure user doesn't have to wait long on the uploading... state

**Description:**
Once the video is upload CTA by user is "Upload" there is progression screen. For some videos this take time and user feels that post the upload the video will be shown in my profile. Since, video isn't displayed to user unless its uploaded on the canister we should skip the screen.

---

## MOB-969 | sentry-2 server config

**Description:**
Redis consume too much memory.

**Mayank's Comments:**
- *2026-02-19:* (Shared screenshots of Sentry metrics dashboards)
- *2026-02-18:* metrics are holding fine
- *2026-02-17:* all lag cleared
- *2026-02-17:* all events are processed, lag is still observed in recordings. But it is gradually decreasing, should be cleared soon
- *2026-02-17:* limited max memory to 15gb
- *2026-02-17:* queued events are being processed, lag is being reduced
- *2026-02-17:* (Shared screenshots of Sentry dashboards)
- *2026-02-17:* limited max memory to 15gb

---

## MOB-874 | Edge functions

**Mayank's Comments:**
- *2026-02-03:* Although edge functions would not behave like edge functions in our case (self hosted on a single machine). The functions should be architected such that they are running on actual edge servers. One way to reduce cold starts and increase performance is to combine multiple actions into a single Edge Function.
- *2026-02-03:* Functions are simple `.ts` files that export a handler
- *2026-02-03:* Deno provides a basic web server. Some popular web frameworks are Express, Oak and Hono. Among these Hono would be the one we would be using. It is simple and fast. [Hono - Web framework built on Web Standards](https://hono.dev/)
- *2026-02-03:* Edge function can also be deployed on fly.io
- *2026-02-03:* deployment to server requires manually copy pasting the code or writing custom scripts. A GH Action or a custom supabase functions deploy command
- *2026-02-03:* Quickstart guide: [Getting Started with Edge Functions | Supabase Docs](https://supabase.com/docs/guides/functions/quickstart)
- *2026-02-03:* Supabase uses Deno and Typescript with wasm support

---

## MOB-873 | Self hosting exploration

**Mayank's Comments:**
- *2026-02-03:* Some things to keep in mind while setting up prod env: Creating and Managing secrets, Configuring S3 Storage, Configuring an email server if needed.
- *2026-02-03:* Supabase can be self hosted using docker compose. There are multiple services that get deployed as mentioned in their architecture: Studio, Kong (API gateway), Auth (JWT-based), PostgREST, Realtime (Elixir server), Storage (S3), imgproxy, postgres-meta, PostgreSQL, Edge Runtime (Deno), Logflare, Vector, Supavisor (connection pooler).

---

## MOB-872 | Local development setup

**Mayank's Comments:**
- *2026-02-03:* It will download ~8GB of images, grab a cup of coffee and wait patiently for the initial setup
- *2026-02-03:* Also install docker compose and compatibility extensions from podman desktop
- *2026-02-03:* If using podman, you might face docker related issues for which run this `export DOCKER_HOST=unix:///var/run/docker.sock`
- *2026-02-03:* Follow the Supabase Local Development & CLI guide. I used Podman, you can use docker desktop too but it is free for small businesses. Skip supabase login and link as it is required only for cloud hosting and does not work with self hosted solution.

---

## MOB-755 | Add a nudge for user when user taps on disabled state of the CTAs on the tournament card

**Description:**
Figma design link: https://www.figma.com/design/vos8GPHbm3BD5N02N2T6SO/Smily-tournament?node-id=1062-10458

---

## MOB-968 | update sentry-2

**Mayank's Comments:**
- *2026-02-17:* done

---

## MOB-833 | Video delay and stuck in ios with new architecture

**Mayank's Comments:**
- *2026-02-05:* **Video analysis:** The mp4 video files just has 1 keyframe at the beginning. There is only one GOP. GOP pattern is `I BBB P BBB P`, ~75% of frames are B-frames. This GOP pattern is more optimised for compression ratio rather than playback especially for short videos.
  - **How it matters:** Instant first frame because of 1 keyframe, but no recovery at all in case of errors. Mobile issues: Freezing, video tearing, buffer underruns, decoder reset. Transmitting issues: delayed data, stalled decode, dropped frames. Increase latency: B Frames need future frames to be decoded. Worst case severity: high because of high number of B-Frames being used.
  - **Resolution:** Add periodic I Frames (IDR) every 1-3 seconds. Reduce B frames to just 1 or preferably use just I and P frames. Use better formats like HEVC. Use same resolution and profile for all videos for better decoder reuse. Optimise for instant playback rather than pure compression.
- *2026-02-05:* **Optimisations:** Tuned playback parameters on iOS for better playback. Implemented a retry mechanism to solve for video stuck issue. **Observations:** Video playback startup greatly depends on network speed on both iOS and Android. Use of better codecs like HEVC can help reduce startup latency as well as data consumption. Estimated data savings: ~40%.
- *2026-02-02:* Need help from Sarvesh, waiting for some bandwidth availability

---

## MOB-827 | Sentry instance migration of FE events

**Description:**
Context: https://github.com/dolr-ai/product-roadmap/issues/1587#issue-3860053486

---

## MOB-821 | API feed latency on the home feed investigation

**Description:**
Rishi's device is facing a big latency issue. We are observing latency shift from 10 secs to 17 secs since start of January. We will need to investigate into what has changed and why?

**Mayank's Comments:**
- *2026-02-12:* verified the data on firebase performance monitoring, it closely matches the actual data with only a few millisecond difference
- *2026-02-12:* on slow network, response time is similar but error rate is higher. 'context deadline exceeded (Client.Timeout exceeded while awaiting headers)' 7/100
- *2026-02-11:* (Shared Google Chat link)
- *2026-02-11:* **API benchmarking from terminal (on 200 mbps connection):** Total: 145.0631 secs, Slowest: 19.7289 secs, Fastest: 4.0558 secs, Average: 9.8886 secs, Requests/sec: 0.6894. Latency distribution: 10% in 5.05 secs, 25% in 7.46 secs, 50% in 9.39 secs, 75% in 11.88 secs, 90% in 16.47 secs, 95% in 18.50 secs. Status code distribution: [200] 89 responses.

---

## MOB-832 | Rank 1 doesn't get the modal for BTC credited

**Description:**
Rank 1 users doesn't see the winning modal in the dynamic smiley game however I was able to see the modal on the prod app.

---

## MOB-885 | Something went wrong showing up on screen

**Description:**
On my profile this is coming. (Chat link shared)

**Mayank's Comments:**
- *2026-02-11:* Lots of DNS errors, probably some intermittent internet issue. Can reopen if issue comes again. (Shared screenshot)

---

## MOB-840 | Tournament bug when app went in background

**Description:**
While playing I got a call, I came back to the game but it didn't load so I killed the app and reopened it. Joined the tournament again but neither did my voting worked nor the rank was visible. I could only swipe and watch video. The click was working but nothing happened. I killed the app again and rejoined twice but nothing happened.

---

## MOB-933 | proper cache cleanup

**Description:**
tournament are short lived so cache should cleanup old entries.

---

## MOB-931 | feed position restore within same and across sessions

**Description:**
Scroll to last video on which user exited the tournament.

---

## MOB-930 | Tournament Local cache setup

**Description:**
save votes, diamonds and other user data to restore in next session.

---

## MOB-864 | Setup environment for writing Supabase APIs

**Mayank's Comments:**
- *2026-02-06:* deployed to staging environment. Saikat to host caddy and add dns

---

## MOB-879 | User reporting is not working

**Description:**
This is happening since we have made the new architecture live. Whenever user reports a video, the video is immediately filtered. However, now suddenly that video is not getting filtered in the feed.

---

## MOB-825 | Firebase libs collation for Supabase migration

**Description:**
Firebase event collation.

**Mayank's Comments:**
- *2026-01-30:*

| Firebase Service | Why It's Used | Supabase Equivalent | Notes |
|---|---|---|---|
| Auth | Issue ID tokens & map principal identity | Supabase Auth | "custom token" API is not present in supabase. Alternate is to use rust backend token or build some API to achieve similar functionality |
| Firestore | Core app data (users, leaderboards, tournaments, videos) & transactions | Supabase Postgres | Requires schema + RLS design to replace firebase nosql and permission rules |
| Storage | Media/assets delivery | Supabase Storage | Supabase provides Signed/public URLs and integration with coil library |
| Cloud Functions | Backend APIs + validation (primarily tournaments) | Edge Functions or Ktor | Heavy endpoints likely better in Ktor like tournament creation and video analysis |
| App Check | Abuse protection (not used anymore) | No equivalent | Needs attestation (Play Integrity/App Attest) or other anti-abuse |
| Messaging (FCM) | Push notifications | No equivalent | Keep FCM |
| Analytics | Event tracking | No equivalent | Keep Firebase analytics or completely rely on Mixpanel |
| Crashlytics | Crash reporting | No equivalent | Keep Firebase Crashlytics or completely rely on Sentry |
| Performance | Perf tracing | No equivalent | Keep performance monitoring and explore other services for metrics |
| Remote Config | Feature flags | No equivalent | Keep remote config for now and explore other solutions in future if needed |
| In-App Messaging | In app prompts | No equivalent | Seems unused |

---

## MOB-834 | Jank while swiping in hot or not game

**Description:**
It happens during transition of video and thumbnail.

---

## MOB-826 | Profile views API isn't working as expected

**Description:**
Profile view count which is shown to users on the profile section isn't getting updated. BE team checked and they said the API is working as expected however FE isn't updating it.

**Mayank's Comments:**
- *2026-01-29:* Finding: video started and video duration watched events are not being sent from profile screen. Action: send only video started event after discussion with Joel as video duration watched can be used to game the system.

---

## MOB-822 | Increase the batch size of the feed videos

**Description:**
We would want to increase the batch size of 10 videos to 50 videos to improve the latency being observed today in the home feed.

---

## MOB-640 | Splash screen timeout/app stuck

**Description:**
Smiley game coming up late.

---

## MOB-671 | [Bug] Videos stuck

**Description:**
(Chat link shared for reference)

---

## MOB-672 | Videos not loading

**Description:**
(Chat link shared for reference)

---

## MOB-643 | Video load time improvements

**Description:**
On fast scrolling videos are stuck.

---

## MOB-637 | Fire first app open event

**Description:**
Fire this event as soon as app opens.

---

## MOB-601 | feedback 1

**Description:**
From @sarvesh - Commonization build feedbacks:
1. Top and bottom safe area not calculating correctly
2. Bottom nav color should extend to the bottom
3. The video sound is linked to the phone being silent, if phone is silent, audio is not present
4. Entire bottom nav came up opening keyboard
5. The keyboard spacing is also incorrect
6. Feel there's some issue with main thread because the app feels sticky sometimes
7. Edit profile photo picker not opening
8. Status bar icons should become white on feed view
9. The entire profile isn't working, crashing with JSON error

---

## MOB-489 | OAuth

**Description:**
also verify IosOAuthUtilsHelper impl

---

## MOB-469 | videoplayer lib

**Description:**
all actual implementations

---

## MOB-432 | YralLottieAnimation

**Description:**
Options:
- https://github.com/alexzhirkevich/compottie
- https://github.com/ismai117/kottie

---

## MOB-427 | Branch NPE

**Description:**
Firebase Crashlytics link for Branch NPE crash.

---

## MOB-412 | Sharing module

**Description:**
Both profile and feed have share functionality that needs to be extracted in a single module.

---

## MOB-397 | internal link generation

**Mayank's Comments:**
- *2025-09-07:* Required some refactoring for deeplink parser to fix some issues like proper handling of query parameters and creating url without host. https://github.com/dolr-ai/yral-mobile/pull/459

---

## MOB-337 | Deeplink Parser

**Mayank's Comments:**
- *2025-08-29:* Resolved by https://github.com/dolr-ai/yral-mobile/pull/444

---

## MOB-297 | UI for app update for mandatory and non mandatory updates

**Description:**
Figma design: https://www.figma.com/design/2lZ7zWovZbiy6kofYqVXaa/Games---Yral?node-id=7850-2227

---

## MOB-265 | Feature Flag framework

**Description:**
A/B testing framework.

---

## MOB-198 | Tap to reload Sats balance on Android

**Description:**
Figma design: https://www.figma.com/design/2lZ7zWovZbiy6kofYqVXaa/Games---Yral?node-id=7631-4406

---

## MOB-199 | Add lottie animation on tapping

**Mayank's Comments:**
- *2025-07-27:* Moving MOB-199 and MOB-202 to cycle 2 as animation changes were required from design and was not ready to be picked up. Instead MOB-200 and MOB-201 were picked up in cycle 1. Implementation is complete, will be reviewed as a whole feature.

---

## MOB-203 | Deeplink for profile video

**Description:**
navigate to profile screen -> wait for refresh -> open video feed using deeplink. A deeplink framework would be better but skipping for now.

---

## MOB-187 | In app push notifications

**Description:**
Show in app toast if app is in foreground.

---

## MOB-195 | Toast component UI

**Description:**
With state support for:
- type: big, small
- cta: true, false
- status: success, warning, error, info
