# ATEF IPTV - Professional IPTV Media Player

A commercial-grade, multi-device IPTV media player for Android Phone, Tablet, and Android TV / Google TV. Built entirely with Kotlin, Jetpack Compose, and AndroidX Media3 ExoPlayer, featuring a native RTL Arabic interface, multi-format playlist parsing (M3U, Xtream Codes), per-channel playback profiles, Arabic metadata enrichment, and responsive D-pad / remote control navigation.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The architectural foundation and technical stack have been aligned with the user requirements:

- **Playback Engine**: AndroidX Media3 ExoPlayer with adaptive bitrate streaming (HLS, DASH, RTSP, progressive MP4/MKV), custom track selection, and multi-format audio/subtitle decoders.
- **UI & Device Mode**: Adaptive UI system with automatic device detection (Phone vs. Tablet vs. TV Leanback) plus a quick toggle switch in settings and on first launch.
- **Subscription & Security**: Local persistence via Room Database, storing credentials and sensitive playlist tokens securely in private app storage. No remote tracking, telemetry, or unsolicited data collection.
- **Content Policy**: Strict IPTV Player architecture—no pre-loaded playlists, channels, or streams. The user inputs their own legal content and credentials. Original media titles are preserved while rich Arabic metadata and descriptions are displayed.
- **Support & Identity**: Official support hotline (`0581283398`) and email (`eng.ibrahimabdalfatah@gmail.com`) integrated into Settings, Support, and About screens with one-tap dialing, emailing, and clipboard copying.

---

## 1. Overview & Core Concept

### What It Does
ATEF IPTV is a dedicated client player that enables users to connect their personal IPTV subscriptions via **Xtream Codes API**, **M3U/M3U8 URLs or files**, and **XMLTV EPG guides**. It organizes content dynamically into:
1. **Live TV (القنوات المباشرة)** with category filtering, channel surfing, EPG timeline progress, and fast channel switching.
2. **Movies (الأفلام)** with original titles, high-resolution posters, Arabic synopsis, ratings, and resume playback.
3. **Series (المسلسلات)** with structured season/episode breakdown, Arabic episode summaries, and automatic next-episode queuing.
4. **Offline Download & Storage Manager (إدارة التنزيلات والتخزين)** allowing offline playback where permitted by source streams.

### Target Audience & Persona
Arabic-speaking users on Android mobile devices, foldables, tablets, and living-room Android TV boxes seeking a fluid, premium streaming experience with instant channel flipping, clean RTL typography, and zero clutter.

### Key Value
- **Zero lag channel flipping**: Smart player instance reuse and seamless buffering strategies.
- **Per-Channel Playback Profiles**: Remembers aspect ratio (16:9, Fit, Zoom, etc.), audio track, subtitle font/timing, and video quality per individual channel.
- **100% Arabic Native UX**: Designed from the ground up for Arabic RTL orientation, high-contrast TV readability, and remote D-pad navigation.

---

## 2. User Experience & Visual Design

### Key User Flows

```
[ First Launch / Welcome Screen ]
               │
               ▼
   [ Device Profile Selection ] (Auto-detected / Phone / Tablet / TV)
               │
               ▼
     [ Add Subscription ] ────► [ M3U / M3U8 URL or File ]
               │          ────► [ Xtream Codes (Server, User, Pass) ]
               ▼
       [ Main Dashboard ] ────► [ متابعة المشاهدة (Continue Watching) ]
               │          ────► [ القنوات المفضلة (Favorite Channels) ]
               │          ────► [ أحدث الأفلام والمسلسلات (Recent VOD) ]
               │          ────► [ الاشتراكات النشطة (Active Subscriptions) ]
               ▼
┌───────────────────┬───────────────────┬───────────────────┐
│     Live TV       │      Movies       │      Series       │
│  (البث المباشر)   │     (الأفلام)     │   (المسلسلات)     │
└─────────┬─────────┴─────────┬─────────┴─────────┬─────────┘
          │                   │                   │
          ▼                   ▼                   ▼
  [ Video Player ]    [ Movie Details ]   [ Series Seasons ]
  (Controls, Audio,   (Play / Resume /    (Episodes / Resume)
   Subtitles, Aspect,  Favorite)                  │
   Channel Drawer)                                ▼
                                          [ Video Player ]
```

### Visual Identity & Theme
- **Aesthetic Direction**: *Luxury Cinematic Dark* — deep obsidian backgrounds with warm amber/gold accents (`#E5A93C`) and vivid cyan indicators (`#22D3EE`) for live status.
- **Color Palette**:
  - `Background`: Deep Midnight Slate (`#0B0F19`)
  - `Surface`: Elevated Obsidian Glass (`#141B2D`)
  - `Surface Variant / Borders`: Subtle Indigo Border (`#232F4D`)
  - `Primary / Accent`: Golden Amber (`#E5A93C`)
  - `Live TV Indicator`: Emerald Glow (`#10B981`)
  - `Text Primary`: Pure White (`#F8FAFC`)
  - `Text Secondary`: Soft Slate (`#94A3B8`)
- **Typography & Hierarchy**:
  - Headings: Bold modern Arabic typography with crisp readability on both 6-inch OLED displays and 65-inch 4K TVs.
  - Subtitles & EPG: High-legibility monospaced time stamps and clear RTL Arabic paragraph formatting.
- **TV & D-Pad Ergonomics**:
  - 10-foot UI design: High-contrast focus halos, scale transforms on focus (1.05x zoom on active poster), and explicit directional key handling (Up/Down/Left/Right/OK/Back).

---

## 3. Key Product Decisions & Trade-Offs

### Decision 1: Media3 ExoPlayer Engine vs. Hybrid WebViews
- **Chosen Approach**: Native AndroidX Media3 (`androidx.media3.exoplayer`, `ui`, `hls`, `dash`, `session`).
- **Why**: Native hardware acceleration, lowest audio/video synchronization latency, robust HLS variant switching, custom subtitle rendering, and surface aspect-ratio scaling without web frame drop.
- **Alternatives Considered**: Web-based video players (rejected due to memory overhead and lack of low-level codec controls on TV devices).

### Decision 2: Multi-Platform Layout Strategy (Adaptive Compose)
- **Chosen Approach**: WindowSizeClass-driven layouts (`Compact`, `Medium`, `Expanded`) paired with a runtime `DeviceMode` state (`PHONE`, `TABLET`, `TV`).
- **Why**: Allows TV mode to use custom D-Pad focus grids and full-screen navigation drawers while Phone/Tablet utilize modern bottom bars and swipeable gesture sheets.

### Decision 3: Local SQLite/Room Database vs. Remote Sync
- **Chosen Approach**: Strictly on-device Room Database with encrypted preferences for credentials.
- **Why**: Zero external cloud dependency for credentials protects user privacy and adheres to standard media player security requirements. All cached playlists, favorite channels, per-channel playback profiles, and resume timestamps are stored locally.

### Decision 4: Preserving Original Content Names with Arabic Metadata Enrichment
- **Chosen Approach**: Retain exact original title strings in the database (crucial for IPTV server compatibility and stream matching), accompanied by an Arabic description and category translation layer.
- **Why**: Users frequently search for content by official Hollywood, European, or Arabic original titles without garbled automated translations.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              UI LAYER (Compose)                             │
├───────────────────────────────┬─────────────────────────────────────────────┤
│ Phone / Tablet Views          │ TV Leanback Views (D-Pad Focus)             │
│ - Bottom Navigation           │ - Side Navigation Rail                      │
│ - Touch Controls Overlay      │ - Grid Cards with Focus Highlights          │
│ - Swipeable Channel Drawer    │ - Remote OK / Back / Play-Pause Intercept   │
└───────────────────────────────┴─────────────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          VIEWMODELS & STATE HOLDERS                         │
│ - PlayerViewModel (Media3 lifecycle, track selection, per-channel profiles)│
│ - SubscriptionViewModel (M3U / Xtream API sync & management)                │
│ - LiveTvViewModel (Categories, EPG timeline, channel switching)             │
│ - VodViewModel (Movies & Series browsing, resume playback, favorites)       │
│ - StorageViewModel (Download manager, cache, disk quota metrics)            │
└─────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              DOMAIN & REPOSITORIES                          │
│ - SubscriptionRepository ──► M3uParser / XtreamApiClient / EpgParser        │
│ - ChannelRepository      ──► Room DB (Channels, Categories, Profiles)       │
│ - VodRepository          ──► Room DB (Movies, Series, Seasons, Episodes)    │
│ - PlaybackHistoryRepo    ──► Room DB (Resume state, Watch history)          │
│ - DownloadRepository     ──► Android DownloadManager / App Cache Storage    │
└─────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                             LOCAL PERSISTENCE                               │
│ - Room Database: `atef_iptv.db`                                             │
│ - Tables: Subscriptions, Channels, ChannelProfiles, VOD, Episodes, History  │
│ - Encrypted Shared Preferences: Server tokens, passwords, security keys    │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Core Data Models
1. **SubscriptionEntity**: `id`, `name`, `type` (M3U, XTREAM), `sourceUrl`, `username`, `password`, `expirationDate`, `isActive`, `createdAt`.
2. **ChannelEntity**: `id`, `subscriptionId`, `streamId`, `name`, `logoUrl`, `streamUrl`, `categoryName`, `epgChannelId`, `isFavorite`, `lastWatchedTimestamp`.
3. **ChannelProfileEntity**: `channelUniqueKey`, `aspectRatio` (ORIGINAL, FIT, FILL, 16_9, 4_3, ZOOM), `preferredQuality`, `audioTrackId`, `subtitleTrackId`, `subtitleSize`, `subtitleColor`.
4. **VodEntity**: `id`, `subscriptionId`, `streamId`, `originalTitle`, `arabicTitle`, `arabicOverview`, `posterUrl`, `backdropUrl`, `type` (MOVIE, SERIES), `rating`, `year`, `durationMs`.
5. **EpisodeEntity**: `id`, `seriesId`, `seasonNumber`, `episodeNumber`, `originalTitle`, `arabicOverview`, `streamUrl`, `durationMs`.
6. **PlaybackHistoryEntity**: `contentId`, `contentType` (MOVIE, EPISODE), `lastPositionMs`, `durationMs`, `updatedAt`.

---

## Phased Implementation Roadmap

- **Phase 1: Project Architecture & Dependencies**
  - Configure `libs.versions.toml` and `app/build.gradle.kts` for AndroidX Media3 (`exoplayer`, `ui`, `hls`, `dash`, `session`), Room, Coil, OkHttp, and Navigation Compose.
  - Setup RTL layout direction in Theme, typography, and color tokens.
  - Configure strings and identity metadata (`ATEF IPTV`, support contact info).

- **Phase 2: Core Domain, Database & Playlist Parsers**
  - Implement Room entities, DAOs, and migrations for subscriptions, channels, profiles, VOD, and playback history.
  - Implement robust M3U/M3U8 parser handling extended tags (`tvg-id`, `tvg-name`, `tvg-logo`, `group-title`).
  - Implement Xtream Codes API client (Live streams, VOD, Series, User authentication and account expiry).
  - Implement EPG (XMLTV) stream parser.

- **Phase 3: AndroidX Media3 Video Player Engine**
  - Build persistent `ExoPlayer` controller with auto-reconnection and low-latency buffering.
  - Implement custom player controls overlay (Play/Pause, Seek, Aspect Ratio switcher, Quality selector, Track selector).
  - Implement Per-Channel Playback Profile persistence and automatic profile recall.
  - Subtitle manager with Arabic RTL font rendering, size adjustment, and sync offset.

- **Phase 4: Live TV, Channel Surfing & EPG**
  - Fast channel switching drawer during active playback without reloading navigation backstack.
  - Live TV category list, search, and favorites management.
  - EPG current and upcoming program timeline with dynamic time progress indicator.

- **Phase 5: VOD (Movies & Series) & Playback Resume**
  - Movie catalog with search, filter, and detail modal (Original title + Arabic overview, trailer/poster, watch/resume actions).
  - Series catalog with season tab selection, episode cards, and automatic next-episode continuity.
  - Continue Watching row on Dashboard with visual progress bars.

- **Phase 6: Multi-Device Adaptation (Mobile & Android TV)**
  - TV Leanback mode with D-Pad focus navigation, highlight animations, and remote control keys.
  - Device mode selection dialog on first run + Settings selector.
  - Complete Settings screen: Player options, Storage management, Subscriptions manager, and Support screen with direct dialing to `0581283398` and email to `eng.ibrahimabdalfatah@gmail.com`.

- **Phase 7: Verification, Compilation & Polish**
  - Verify complete compilation via `compile_applet`.
  - Validate edge-to-edge insets, error recovery when IPTV links are unreachable, and storage cleanup.

- **Phase 8: Advanced Subscription Refresh, TV Navigation Redesign, Screen Containment & AI Synopsis**
  - **Subscription Refresh System**: Built live re-sync logic (`IptvRepository.refreshSubscription`) updating Live channels, Movies, and Series from Xtream API / M3U playlists without data loss or re-entering credentials. Available via Header, TV Rail, Dashboard Hero Card, and Subscriptions manager.
  - **TV Side Navigation Rail Redesign**: Relocated cluttered top menus to pop out directly from `AtefTvNavRail`, with dedicated shortcuts for Subscriptions, Support, Settings, and Refresh with D-Pad focus indicators.
  - **Screen Containment (Phone & TV)**: Added TV overscan compensation padding (`20.dp` horizontal, `12.dp` vertical) to prevent physical TV bezel clipping, and `.safeDrawingPadding()` on mobile Scaffold to guarantee zero notch/cutout collision.
  - **Intelligent Arabic Synopsis & Full Translation**: Created `AiMetadataEngine` providing instant Arabic summaries for any movie or series with missing or English plots, preserving original names while ensuring 100% Arabic narrative understanding.
  - **Gemini AI Synopsis & Comfortable Reading**: Integrated Firebase AI Logic (`gemini-3.8-flash`) with one-tap generation for deep synopses and key theme tags, displayed inside dedicated, scrollable, comfortable Arabic reading cards (`lineHeight = 22.sp`).

- **Phase 9: Precise Film Search, Non-Spoiler Synopsis, Cast Members, In-App Downloads, Player Display Modes & Long-Press Favorites**
  - **Accurate Real Film/Series Search & Cast (`AiMetadataEngine`)**: Cleaned raw IPTV titles from stream artifacts to identify the actual work with 100% accuracy via Gemini 3.8 Flash. Generates non-spoiler summaries (ملخص بدون حرق), preserves existing Arabic plots verbatim, translates foreign plots faithfully, and extracts/displays real cast members (أبطال العمل وطاقم التمثيل).
  - **In-App Media Downloads (Offline Viewing)**: Integrated `DownloadRepository.getInstance(context)` into `MovieDetailDialog` and `SeriesDetailDialog` with live status tracking (Downloading %, Completed checkmark) and offline playback.
  - **Player Display Modes & Aspect Ratio (نسخة العرض)**: Prominent interactive button `[نسخة العرض: وضع العرض]` in player controls overlay, HUD animated Toast on mode switch, and comprehensive Arabic modal with detailed screen ratio explanations (Fit, Fill, Zoom, 16:9, 4:3, Original).
  - **Global Favorites & Long-Press Action (المفضلة والضغط المستمر)**: Added favorite filter buttons and "المفضلة ⭐" category chips in Live TV, Movies, and Series lists. Added `combinedClickable` long-press on movie/series/channel cards to instantly add or remove items to/from favorites.

- **Phase 10: Android TV Focus Effects & Remote Control (D-pad) Navigation (تأثيرات التركيز والتنقل السلس بريموت التلفاز)**
  - **Dynamic TV Focus Effects Modifier (`tvFocusable`)**: Created high-performance `tvFocusable` modifier with smooth spring/tween scale animation (1.06x–1.08x), luminous elevated golden border (`3.dp` `GoldPrimary`), z-index elevation (`zIndex(10f)`) to prevent adjacent card clipping, and drop shadows (`shadowElevation = 24f`) for optimal 10-foot TV viewing.
  - **Comprehensive Screen Application**: Applied `tvFocusable` to all cards, poster tiles, channel items, navigation rail shortcuts, dialog choices, and dashboard hero buttons across `AtefTvNavRail`, `MoviesScreen`, `SeriesScreen`, `LiveTvScreen`, `DashboardScreen`, and dialogs.
  - **Video Player D-pad Key Navigation**: Equipped `VideoPlayerScreen` with `FocusRequester` and hardware `onKeyEvent` intercept supporting all Android TV remote control keys:
    - `DPAD_CENTER`, `ENTER`, `NUMPAD_ENTER`: Instant toggle of controls overlay or playback.
    - `DPAD_LEFT`: 10-second rewind on VOD, previous channel on Live TV.
    - `DPAD_RIGHT`: 10-second forward on VOD, next channel on Live TV.
    - `DPAD_UP` / `DPAD_DOWN`: Reveal player HUD controls overlay and fast channel drawer.
    - `MEDIA_PLAY`, `MEDIA_PAUSE`, `MEDIA_PLAY_PAUSE`, `MEDIA_FAST_FORWARD`, `MEDIA_REWIND`, `CHANNEL_UP`, `CHANNEL_DOWN`: Direct hardware remote media actions.
  - **Focusable Player HUD & Dialog Elements**: Center play/pause, rewind/forward, aspect ratio display mode chip, quality & subtitle dialog options, and channel drawer items all support high-visibility focus states.
