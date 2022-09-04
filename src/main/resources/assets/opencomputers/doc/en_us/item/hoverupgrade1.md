# Hover Upgrade

![Float like a feather.](oredict:opencomputers:hoverUpgrade1)

The hover upgrade allows [robots](../block/robot.md) to fly much higher above the ground than they normally could. Unlike [drones](drone.md), they are by default limited to a flight height of 8. This is usually not a big problem, because they can still move up or along walls. Their general movement rules can be summarized like this:
- Robots may only move if the start or target position is valid (e.g. to allow building bridges).
- The position below a robot is always valid (can always move down).
- Positions up to `flightHeight` above a solid block are valid (limited flight capabilities).
- Any position that has an adjacent block with a solid face towards the position is valid (robots can "climb").

These rules, excluding rule 2 for clarity (can always move down), can be visualized like this:
![Robot movement rules visualized.](opencomputers:doc/img/robotMovement.png)

If you don't want to worry about flight height limitations, these upgrades are what you're looking for.