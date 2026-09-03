## 2026-09-01 - [Regex Splitting Overhead in TTS Engine]
**Learning:** Regex splitting (`String.split(Regex)`) inside iteration loops over hundreds of TTS voices creates significant memory pressure from allocating intermediate `List<String>` structures. When finding substrings to look up in a map, do not replace the O(1) map lookup with an O(N) iteration over the map entries to save string allocations via `regionMatches`.
**Action:** For tight loops extracting tokens, use manual index traversal and `substring` to get the token, then use it in an O(1) map lookup, avoiding Regex entirely.
