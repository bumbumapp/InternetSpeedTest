# Product plan — Huawei rule 4.1 round 2 (2026-11-16)

## What exists (v2.1.0)
Speed test (download/upload/ping, server picker, history, share) + Tools hub with Network details, Latency monitor, History statistics/CSV.
Home = the speedometer screen with a small "Tools" pill; a reviewer sees "one speed test".

## Competitors (Speedtest by Ookla, Fing, WiFi Analyzer, NetSpot, Network Analyzer, My Data Manager)
Category users want: Wi-Fi channel analysis, "who is on my network", ping/traceroute/DNS/port utilities, data usage per day.

## New modules (all work offline-safe, off the main thread, cancellable)
1. Wi-Fi Analyzer — nearby networks, signal bars, security, channel graph 2.4/5 GHz, best-channel suggestion, live signal meter. Location permission requested only inside the tool (explained first); denied -> connected-network info still shown.
2. Device Scanner — sweeps the local /24 (TCP connect + reachability, bounded thread pool, cancel), lists IPs, hostnames, response time, marks gateway and this phone. No permission, no root.
3. Network Toolbox — Ping, Traceroute (TTL probes via system ping), DNS Lookup (A/AAAA), Port Check (TCP). One shared screen, four modes, each its own tile on Home.
4. Data Usage — per-day / 7 day / month totals Wi-Fi vs mobile via NetworkStatsManager. Usage-access permission requested only inside the tool; denied -> explanation + since-boot totals from TrafficStats still shown.

No new third-party endpoints. New permissions: ACCESS_FINE_LOCATION (optional, Wi-Fi scan results), CHANGE_WIFI_STATE not used, PACKAGE_USAGE_STATS (special access, optional, Data Usage).

## Home
New HomeActivity: greeting + title, hero tile "Speed test" (last result + Start), 2-column tile grid of 10 tools. Speed test screen unchanged behind the hero.
