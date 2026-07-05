# MinimapWorldId
<!-- ensure to update `gradle.properties` if we ever change this description -->
A Paper plugin that ensures Xaero's minimap sees the correct world id.

There is not currently any support for other minmaps.

## Credits
This plugin is largely based on [ChristopherHaws/mc-xaero-map-spigot],
but fixed so that world ids are per-world instead of per-server.
See issue [mc-xaero-map-spigot#15] for details.

[mc-xaero-map-spigot#15]: https://github.com/ChristopherHaws/mc-xaero-map-spigot/issues/15

I decided to make a new plugin instead of a fork because there
is no easy way to migrate the bugged per-server ids to per-world ids.
Creating a new plugin makes this incompatibility clear.

## See Also
- [Muspah/xaero-minimap-paper] - Has a lot more features and is more complex.
- [ChristopherHaws/mc-xaero-map-spigot] - Has the above-mentioned bug
- [funniray/minimap-control] - Place limitations on the ability of minimaps

[Muspah/xaero-minimap-paper]: https://github.com/Muspah/xaero-minimap-paper
[ChristopherHaws/mc-xaero-map-spigot]: https://github.com/ChristopherHaws/mc-xaero-map-spigot
[funniray/minimap-control]: https://github.com/funniray/minimap-control
