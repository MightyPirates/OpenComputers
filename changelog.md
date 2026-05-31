## Fixes/improvements

* [#3784] Added three new robot names (airone01)
* Fixed Blood Altars (Blood Magic) returning the incorrect value for getSacrificeMultiplier. (hinyb)
* Fixed client leak in Sound.
* Fixed clipboard length not being validated on the server side.
* Fixed network filtering not protecting against NAT64 accesses by default.
* Fixed non-local addresses not being specifiable by the ip: filtering rule.
* Fixed server leaks in Player, SideTracker and StaticSimpleEnvironment. (Alexdoru)
* glGetError() is no longer called if logging GL errors is disabled. (mitchej123)
* Improved Polish translation.
* OpenComputers native libraries are now stored in an `opencomputers/natives` subdirectory. (CaitlynMainer)

## List of contributors

airone01, Alexdoru, asie, CaitlynMainer, hinyb, mitchej123
