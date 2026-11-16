# Review V1 (2026-11-16) — Codex mockups, rendered with headless Chrome (500x915)

| Screen | Score | Notes |
|---|---|---|
| Home (toolkit) | 8.5 | Hero + 10-tile grid reads as a toolkit at a glance; 8 tiles visible without scrolling. Defect: compact icon-left tile has little room for long German titles -> implement tiles with icon above title so text can wrap. |
| Wi-Fi Analyzer | 8.5 | Connected card, channel arcs and best-channel chip are clear. Defects: invents a "Secure" badge and an RT-AX58U-style data we cannot read; implement with real fields only (SSID, dBm, GHz/channel, link speed). |
| Device Scanner | 8 | Good list rhythm. Defects: mock shows device models ("Pixel 9 Pro", "Sonos One") — without root we only get reverse-DNS names, so unknown devices show "Unknown device". Per-device-type icons dropped. |
| Network Toolbox | 8.5 | Segmented modes + input + hop table works. Defect: hop labels ("ISP gateway") not obtainable — show IP + ms only. |
| Data Usage | 8.5 | Strong total + split bar + stacked chart. Defects: "12% from last week" insight is invented — dropped; mobile series colour violet in mock, implemented as violet for consistency. |

Cross-cutting: left/right content margin is 16px everywhere in the mockups; Plus Jakarta Sans scale 12/14/16/20/28. All screens >= 8 after the noted corrections, so no V2 round.
Implementation check against the mockup is in REVIEW_41_IMPL.md.
