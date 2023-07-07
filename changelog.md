## Fixes/improvements

* Reworked Internet Card filtering rules.
  * Implemented a new, more powerful system and improved default configuration.
  * Internet Card rules are now stored in the "internet.filteringRules" configuration key.
  * The old keys ("internet.whitelist", "internet.blacklist") are no longer used; an automatic migration is done upon upgrading the mod.
* [#3635] ArrayIndexOutOfBoundsException when using servers with 3 network cards
* [#3634] Internet card selector update logic erroneously drops non-ready keys

## List of contributors

asie, Fingercomp
