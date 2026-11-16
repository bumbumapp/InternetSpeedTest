# Implementation vs mockup (2026-11-16, release hms build on emulator Play_API35, 1080x2400)

| Screen | Score | Comparison |
|---|---|---|
| Home toolkit | 8.5 | Matches V1: greeting + big title, gradient hero with last result, 10 tiles all visible on one screen (icon left of title, wrap-safe for German). Deviation: tile subtitles 2 lines instead of 1. |
| Wi-Fi Analyzer | 8 | Signal meter, 2.4/5 GHz segmented channel arcs, best-channel line, nearby list with signal bar. Deviation: no "Secure" badge / lock icon (security shown as text). Emulator exposes only one network so the graph shows one arc. |
| Device Scanner | 8 | Status card with progress + list; router/this phone labelled in text instead of badges; no device-type icons (not obtainable without root). |
| Network Toolbox | 8.5 | Segmented Ping/Traceroute/DNS/Port, input, run/stop, result card. Hop rows are plain lines (IP + ms) rather than numbered circles. |
| Data Usage | 8.5 | Total, Wi-Fi/Mobile split bar, stacked chart, permission-needed card with since-boot fallback. Invented "% vs last week" insight omitted. |

Fixed after first on-device look: white window background under the nav bar (all screens), light dialog on a dark app, clipped channel arcs, left-aligned Latin text in RTL, emulator ICMP garbage times, hanging system ping.
Checked: English, German (long), Arabic (RTL), landscape, airplane mode, permission denied paths. Symmetry: back glyph inset 16dp = content margin 16dp on both sides.
