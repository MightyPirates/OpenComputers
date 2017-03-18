package li.cil.oc;

import com.google.common.net.InetAddresses;
import com.mojang.authlib.GameProfile;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.internal.TextBuffer.ColorDepth;
import li.cil.oc.common.Tier;
import li.cil.oc.integration.Mods;
import li.cil.oc.server.component.DebugCard;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Loader;
import org.apache.commons.codec.binary.Hex;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public enum Settings {
	Client("Client side settings, presentation and performance related stuff.") {
		public double screenTextFadeStartDistance;
		public double maxScreenTextRenderDistance;
		public boolean textLinearFiltering;
		public boolean textAntiAlias;
		public boolean robotLabels;
		public double soundVolume;
		public double fontCharScale;
		public double hologramFadeStartDistance;
		public double hologramRenderDistance;
		public double hologramFlickerFrequency;
		public int monochromeColor;
		public String fontRenderer;
		public int beepSampleRate;
		public int beepVolume;
		public double beepRadius;
		public double[] nanomachineHudPos;
		public boolean enableNanomachinePfx;

		@Override
		protected void load() {
			screenTextFadeStartDistance = getDouble("screenTextFadeStartDistance", 15.0,
				"The distance at which to start fading out the text on screens. This is\n"
					+ "purely cosmetic, to avoid text disappearing instantly when moving too\n"
					+ "far away from a screen. This should have no measurable impact on\n"
					+ "performance. Note that this needs OpenGL 1.4 to work, otherwise text\n"
					+ "will always just instantly disappear when moving away from the screen\n"
					+ "displaying it.");
			maxScreenTextRenderDistance = getDouble("maxScreenTextRenderDistance", 20.0,
				"The maximum distance at which to render text on screens. Rendering text\n"
					+ "can be pretty expensive, so if you have a lot of screens you'll want to\n"
					+ "avoid huge numbers here. Note that this setting is client-sided, and\n"
					+ "only has an impact on render performance on clients.");
			textLinearFiltering = getBoolean("textLinearFiltering", false,
				"Whether to apply linear filtering for text displayed on screens when the\n"
					+ "screen has to be scaled down - i.e. the text is rendered at a resolution\n"
					+ "lower than their native one, e.g. when the GUI scale is less than one or\n"
					+ "when looking at a far away screen. This leads to smoother text for\n"
					+ "scaled down text but results in characters not perfectly connecting\n"
					+ "anymore (for example for box drawing characters. Look it up on\n"
					+ "Wikipedia.)");
			textAntiAlias = getBoolean("textAntiAlias", true,
				"If you prefer the text on the screens to be aliased (you know, *not*\n"
					+ "anti-aliased / smoothed) turn this option off.");
			robotLabels = getBoolean("robotLabels", true,
				"Render robots' names as a label above them when near them"
			);
			soundVolume = getDouble("soundVolume", 1.0, 0, 2,
				"The volume multiplier applied to sounds from this mod like the computer"
					+ "running noise. Disable sounds by setting this to zero."
			);
			fontCharScale = getDouble("fontCharScale", 1.01, 0.5, 2,
				"This is the scaling of the individual chars rendered on screens. This"
					+ "is set to slightly overscale per default, to avoid gaps between fully"
					+ "filled chars to appear (i.e. the block symbol that is used for cursor"
					+ "blinking for example) on less accurate hardware."
			);
			hologramRenderDistance = getDouble("hologramRenderDistance", 64, 0, 65536,
				"The maximum render distance of a hologram projected by a highest tier"
					+ "hologram projector when at maximum scale. Render distance is scaled"
					+ "down with the actual scale of the hologram."
			);
			hologramFadeStartDistance = getDouble("hologramFadeStartDistance", 48, 0, 65536,
				"The distance at which to start fading out the hologram (as with"
					+ "hologramRenderDistance). This is purely cosmetic, to avoid image"
					+ "disappearing instantly when moving too far away from a projector."
					+ "It does not affect performance. Holograms are transparent anyway."
			);
			hologramFlickerFrequency = getDouble("hologramFlickerFrequency", 0.025, 0, 65536,
				"This controls how often holograms 'flicker'. This is the chance that it"
					+ "flickers for *each frame*, meaning if you're running at high FPS you"
					+ "may want to lower this a bit, to avoid it flickering too much."
			);

			monochromeColor = Integer.decode(getString("monochromeColor", "0xFFFFFF",
				"The color of monochrome text (i.e. displayed when in 1-bit color depth,"
					+ "e.g. tier one screens / GPUs, or higher tier set to 1-bit color depth)."
					+ "Defaults to white, feel free to make it some other color, tho!"));

			fontRenderer = getString("fontRenderer", "hexfont",
				"Which font renderer to use. Defaults to `hexfont` if invalid."
					+ "Possible values:"
					+ "- hexfont: the (since 1.3.2) default font renderer. Font in .hex format"
					+ "capable of rendering many unicode glyphs."
					+ "The used font data can be swapped out using resource packs,"
					+ "but is harder to work with, since it involves binary data."
					+ "- texture: the old, font-texture based font renderer that was used"
					+ "in OC versions prior to 1.3.2. This will allow overriding"
					+ "the font texture as before. Keep in mind that this renderer"
					+ "is slightly less efficient than the new one, and more"
					+ "importantly, can only render code page 437 (as opposed to..."
					+ "a *lot* of unicode).");

			beepSampleRate = getInt("beepSampleRate", 44100,
				"The sample rate used for generating beeps of computers' internal"
					+ "speakers. Use custom values at your own responsibility here; if it"
					+ "breaks OC you'll get no support. Some potentially reasonable"
					+ "lower values are 16000 or even 8000 (which was the old default, but"
					+ "leads to artifacting on certain frequencies)."
			);
			beepVolume = getInt("beepVolume", 32, 0, Byte.MAX_VALUE,
				"The base volume of beeps generated by computers. This may be in a"
					+ "range of [0, 127], where 0 means mute (the sound will not even be"
					+ "generated), and 127 means maximum amplitude / volume."
			);
			beepRadius = getDouble("beepRadius", 16, 1, 32,
				"The radius in which computer beeps can be heard."
			);
			nanomachineHudPos = getDoubleList("nanomeachineHudPos", new double[] { -1, -1 },
				"Position of the power indicator for nanomachines, by default left to the"
					+ "player's health, specified by negative values. Values in [0, 1) will be"
					+ "treated as relative positions, values in [1, inf) will be treated as"
					+ "absolute positions.",
				"Bad number of HUD coordiantes, ignoring.");

			enableNanomachinePfx = getBoolean("enableNanomachinePfx", true,
				"Whether to emit particle effects around players via nanomachines. This"
					+ "includes the basic particles giving a rough indication of the current"
					+ "power level of the nanomachines as well as particles emitted by the"
					+ "particle effect behaviors."
			);

		}
	},
	Computer("Computer related settings, concerns server performance and security.") {
		public int threads;
		public double timeout;
		public double startupDelay;
		public int eepromSize;
		public int eepromDataSize;
		public int[] cpuComponentCount;
		public double[] callBudgets;
		public boolean canComputersBeOwned;
		public int maxUsers;
		public int maxUsernameLength;
		public boolean eraseTmpOnReboot;
		public int executionDelay;

		@Override
		protected void load() {
			threads = getInt("threads", 4, 1, 65536,
				"The overall number of threads to use to drive computers. Whenever a"
					+ "computer should run, for example because a signal should be processed or"
					+ "some sleep timer expired it is queued for execution by a worker thread."
					+ "The higher the number of worker threads, the less likely it will be that"
					+ "computers block each other from running, but the higher the host"
					+ "system's load may become."
			);
			timeout = getDouble("timeout", 5.0, 0, 65536,
				"The time in seconds a program may run without yielding before it is"
					+ "forcibly aborted. This is used to avoid stupidly written or malicious"
					+ "programs blocking other computers by locking down the executor threads."
					+ "Note that changing this won't have any effect on computers that are"
					+ "already running - they'll have to be rebooted for this to take effect."
			);
			startupDelay = getDouble("startupDelay", 0.25, 0.05, 65536,
				"The time in seconds to wait after a computer has been restored before it"
					+ "continues to run. This is meant to allow the world around the computer"
					+ "to settle, avoiding issues such as components in neighboring chunks"
					+ "being removed and then re-connected and other odd things that might"
					+ "happen."
			);
			eepromSize = getInt("eepromSize", 4096, 0, 65536,
				"The maximum size of the byte array that can be stored on EEPROMs as executable data.."
			);
			eepromDataSize = getInt("eepromDataSize", 256, 0, 65536,
				"The maximum size of the byte array that can be stored on EEPROMs as configuration data."
			);
			cpuComponentCount = getIntList("cpuComponentCount", new int[] {
					8,
					12,
					16
				},
				"The number of components the different CPU tiers support. This list"
					+ "must contain exactly three entries, or it will be ignored.",
				"Bad number of CPU component counts, ignoring."
			);
			callBudgets = getDoubleList("callBudgets", new double[] {
					0.5,
					1.0,
					1.5
				},
				"The provided call budgets by the three tiers of CPU and memory. Higher"
					+ "budgets mean that more direct calls can be performed per tick. You can"
					+ "raise this to increase the \"speed\" of computers at the cost of higher"
					+ "real CPU time. Lower this to lower the load Lua executors put on your"
					+ "machine / server, at the cost of slower computers. This list must"
					+ "contain exactly three entries, or it will be ignored.",
				"Bad number of call budgets, ignoring."
			);
			canComputersBeOwned = getBoolean("canComputersBeOwned", true,
				"This determines whether computers can only be used by players that are"
					+ "registered as users on them. Per default a newly placed computer has no"
					+ "users. Whenever there are no users the computer is free for all. Users"
					+ "can be managed via the Lua API (computer.addUser, computer.removeUser,"
					+ "computer.users). If this is true, the following interactions are only"
					+ "possible for users:"
					+ "- input via the keyboard and touch screen."
					+ "- inventory management."
					+ "- breaking the computer block."
					+ "If this is set to false, all computers will always be usable by all"
					+ "players, no matter the contents of the user list. Note that operators"
					+ "are treated as if they were in the user list of every computer, i.e. no"
					+ "restrictions apply to them."
					+ "See also: `maxUsers` and `maxUsernameLength`."
			);
			maxUsers = getInt("maxUsers", 16, 0, 65536,
				"The maximum number of users that can be registered with a single"
					+ "computer. This is used to avoid computers allocating unchecked amounts"
					+ "of memory by registering an unlimited number of users. See also:"
					+ "`canComputersBeOwned`."
			);
			maxUsernameLength = getInt("maxUsernameLength", 32, 0, 65536,
				"Sanity check for username length for users registered with computers. We"
					+ "store the actual user names instead of a hash to allow iterating the"
					+ "list of registered users on the Lua side."
					+ "See also: `canComputersBeOwned`."
			);
			eraseTmpOnReboot = getBoolean("eraseTmpOnReboot", false,
				"Whether to delete all contents in the /tmp file system when performing"
					+ "a 'soft' reboot (i.e. via `computer.shutdown(true)`). The tmpfs will"
					+ "always be erased when the computer is completely powered off, even if"
					+ "it crashed. This setting is purely for software-triggered reboots."
			);
			executionDelay = getInt("executionDelay", 12, 0, 65536,
				"The time in milliseconds that scheduled computers are forced to wait"
					+ "before executing more code. This avoids computers to \"busy idle\","
					+ "leading to artificially high CPU load. If you're worried about"
					+ "performance on your server, increase this number a little (it should"
					+ "never exceed 50, a single tick, though) to reduce CPU load even more."
			);
		}
	},
	Lua(Computer, "Settings specific to the Lua architecture.") {
		public boolean allowBytecode;
		public boolean allowGC;
		public boolean enableLua53;
		public int[] ramSizes;
		public double ramScaleFor64Bit;
		public int maxTotalRam;

		@Override
		protected void load() {
			allowBytecode = getBoolean("allowBytecode", false,
				"Whether to allow loading precompiled bytecode via Lua's `load`"
					+ "function, or related functions (`loadfile`, `dofile`). Enable this"
					+ "only if you absolutely trust all users on your server and all Lua"
					+ "code you run. This can be a MASSIVE SECURITY RISK, since precompiled"
					+ "code can easily be used for exploits, running arbitrary code on the"
					+ "real server! I cannot stress this enough: only enable this is you"
					+ "know what you're doing."
			);
			allowGC = getBoolean("allowGC", false,
				"Whether to allow user defined __gc callbacks, i.e. __gc callbacks"
					+ "defined *inside* the sandbox. Since garbage collection callbacks"
					+ "are not sandboxed (hooks are disabled while they run), this is not"
					+ "recommended."
			);
			enableLua53 = getBoolean("enableLua53", true,
				"Whether to make the Lua 5.3 architecture available. If enabled, you"
					+ "can reconfigure any CPU to use the Lua 5.3 architecture."
			);
			ramSizes = getIntList("ramSizes", new int[] {
					192,
					256,
					384,
					512,
					768,
					1024
				},
				"The sizes of the six levels of RAM, in kilobytes. This list must"
					+ "contain exactly six entries, or it will be ignored. Note that while"
					+ "there are six levels of RAM, they still fall into the three tiers of"
					+ "items (level 1, 2 = tier 1, level 3, 4 = tier 2, level 5, 6 = tier 3).",
				"Bad number of RAM sizes, ignoring."
			);
			ramScaleFor64Bit = getDouble("ramScaleFor64Bit", 1.8, 1, 65536,
				"This setting allows you to fine-tune how RAM sizes are scaled internally"
					+ "on 64 Bit machines (i.e. when the Minecraft server runs in a 64 Bit VM)."
					+ "Why is this even necessary? Because objects consume more memory in a 64"
					+ "Bit environment than in a 32 Bit one, due to pointers and possibly some"
					+ "integer types being twice as large. It's actually impossible to break"
					+ "this down to a single number, so this is really just a rough guess. If"
					+ "you notice this doesn't match what some Lua program would use on 32 bit,"
					+ "feel free to play with this and report your findings!"
					+ "Note that the values *displayed* to Lua via `computer.totalMemory` and"
					+ "`computer.freeMemory` will be scaled by the inverse, so that they always"
					+ "correspond to the \"apparent\" sizes of the installed memory modules. For"
					+ "example, when running a computer with a 64KB RAM module, even if it's"
					+ "scaled up to 96KB, `computer.totalMemory` will return 64KB, and if there"
					+ "are really 45KB free, `computer.freeMemory` will return 32KB."
			);
			maxTotalRam = getInt("maxTotalRam", 67108864, 0, Integer.MAX_VALUE,
				"The total maximum amount of memory a Lua machine may use for user"
					+ "programs. The total amount made available by components cannot"
					+ "exceed this. The default is 64*1024*1024. Keep in mind that this does"
					+ "not include memory reserved for built-in code such as `machine.lua`."
					+ "IMPORTANT: DO NOT MESS WITH THIS UNLESS YOU KNOW WHAT YOU'RE DOING."
					+ "IN PARTICULAR, DO NOT REPORT ISSUES AFTER MESSING WITH THIS!"
			);
		}
	},
	Robot("Robot related settings, what they may do and general balancing.") {
		public boolean allowActivateBlocks;
		public boolean allowUseItemsWithDuration;
		public boolean canAttackPlayers;
		public int limitFlightHeight;
		public boolean screwCobwebs;
		public double swingRange;
		public double useAndPlaceRange;
		public double itemDamageRate;
		public String nameFormat;
		public String uuidFormat;
		public int[] upgradeFlightHeight;

		@Override
		protected void load() {
			allowActivateBlocks = getBoolean("allowActivateBlocks", true,
				"Whether robots may 'activate' blocks in the world. This includes"
					+ "pressing buttons and flipping levers, for example. Disable this if it"
					+ "causes problems with some mod (but let me know!) or if you think this"
					+ "feature is too over-powered."
			);
			allowUseItemsWithDuration = getBoolean("allowUseItemsWithDuration", true,
				"Whether robots may use items for a specifiable duration. This allows"
					+ "robots to use items such as bows, for which the right mouse button has"
					+ "to be held down for a longer period of time. For robots this works"
					+ "slightly different: the item is told it was used for the specified"
					+ "duration immediately, but the robot will not resume execution until the"
					+ "time that the item was supposedly being used has elapsed. This way"
					+ "robots cannot rapidly fire critical shots with a bow, for example."
			);
			canAttackPlayers = getBoolean("canAttackPlayers", false,
				"Whether robots may damage players if they get in their way. This"
					+ "includes all 'player' entities, which may be more than just real players"
					+ "in the game."
			);
			limitFlightHeight = getInt("limitFlightHeight", 8, 0, 65536,
				"Limit robot flight height, based on the following rules:"
					+ "- Robots may only move if the start or target position is valid (e.g."
					+ "to allow building bridges)."
					+ "- The position below a robot is always valid (can always move down)."
					+ "- Positions up to <flightHeight> above a block are valid (limited"
					+ "flight capabilities)."
					+ "- Any position that has an adjacent block with a solid face towards the"
					+ "position is valid (robots can \"climb\")."
					+ "Set this to 256 to allow robots to fly whereever, as was the case"
					+ "before the 1.5 update. Consider using drones for cases where you need"
					+ "unlimited flight capabilities instead!"
			);
			upgradeFlightHeight = getIntList("upgradeFlightHeight", new int[] { 64, 256 },
				"The maximum flight height with upgrades, tier one and tier two of the"
					+ "hover upgrade, respectively.",
				"Bad number of hover flight height counts, ignoring."
			);
			screwCobwebs = getBoolean("notAfraidOfSpiders", true,
				"Determines whether robots are a pretty cool guy. Ususally cobwebs are"
					+ "the bane of anything using a tool other than a sword or shears. This is"
					+ "an utter pain in the part you sit on, because it makes robots meant to"
					+ "dig holes utterly useless: the poor things couldn't break cobwebs in"
					+ "mining shafts with their golden pick axes. So, if this setting is true,"
					+ "we check for cobwebs and allow robots to break 'em anyway, no matter"
					+ "their current tool. After all, the hardness value of cobweb can only"
					+ "rationally explained by Steve's fear of spiders, anyway."
			);
			swingRange = getDouble("swingRange", 0.49,
				"The 'range' of robots when swinging an equipped tool (left click). This"
					+ "is the distance to the center of block the robot swings the tool in to"
					+ "the side the tool is swung towards. I.e. for the collision check, which"
					+ "is performed via ray tracing, this determines the end point of the ray"
					+ "like so: `block_center + unit_vector_towards_side * swingRange`"
					+ "This defaults to a value just below 0.5 to ensure the robots will not"
					+ "hit anything that's actually outside said block."
			);
			useAndPlaceRange = getDouble("useAndPlaceRange", 0.65,
				"The 'range' of robots when using an equipped tool (right click) or when"
					+ "placing items from their inventory. See `robot.swingRange`. This"
					+ "defaults to a value large enough to allow robots to detect 'farmland',"
					+ "i.e. tilled dirt, so that they can plant seeds."
			);
			itemDamageRate = getDouble("itemDamageRate", 0.1, 0, 1,
				"The rate at which items used as tools by robots take damage. A value of"
					+ "one means that items lose durability as quickly as when they are used by"
					+ "a real player. A value of zero means they will not lose any durability"
					+ "at all. This only applies to items that can actually be damaged (such as"
					+ "swords, pickaxes, axes and shovels)."
					+ "Note that this actually is the *chance* of an item losing durability"
					+ "when it is used. Or in other words, it's the inverse chance that the"
					+ "item will be automatically repaired for the damage it just took"
					+ "immediately after it was used."
			);
			nameFormat = getString("nameFormat", "$player$.robot",
				"The name format to use for robots. The substring '$player$' is"
					+ "replaced with the name of the player that owns the robot, so for the"
					+ "first robot placed this will be the name of the player that placed it."
					+ "This is transitive, i.e. when a robot in turn places a robot, that"
					+ "robot's owner, too, will be the owner of the placing robot."
					+ "The substring $random$ will be replaced with a random number in the"
					+ "interval [1, 0xFFFFFF], which may be useful if you need to differentiate"
					+ "individual robots."
					+ "If a robot is placed by something that is not a player, e.g. by some"
					+ "block from another mod, the name will default to 'OpenComputers'."
			);
			uuidFormat = getString("uuidFormat", "$player$",
				"Controls the UUID robots are given. You can either specify a fixed UUID"
					+ "here or use the two provided variables:"
					+ "- $random$, which will assign each robot a random UUID."
					+ "- $player$, which assigns to each placed robot the UUID of the player"
					+ "that placed it (note: if robots are placed by fake players, i.e."
					+ "other mods' blocks, they will get that mods' fake player's profile!)"
					+ "Note that if no player UUID is available this will be the same as"
					+ "$random$."
			);
		}
	},
	RobotXP(Robot, "This controls how fast robots gain experience, and how that experience alters the stats.") {
		public double baseValue;
		public double constantGrowth;
		public double exponentialGrowth;
		public double actionXp;
		public double exhaustionXpRate;
		public double oreXpRate;
		public double bufferPerLevel;
		public double toolEfficiencyPerLevel;
		public double harvestSpeedBoostPerLevel;

		@Override
		protected void load() {
			baseValue = getDouble("baseValue", 50, 0, Long.MAX_VALUE,
				"The required amount per level is computed like this:"
					+ "xp(level) = baseValue + (level * constantGrowth) ^ exponentialGrowth"
			);
			constantGrowth = getDouble("constantGrowth", 8, 1, Long.MAX_VALUE,
				"The required amount per level is computed like this:"
					+ "xp(level) = baseValue + (level * constantGrowth) ^ exponentialGrowth"
			);
			exponentialGrowth = getDouble("exponentialGrowth", 2, 1, Long.MAX_VALUE,
				"The required amount per level is computed like this:"
					+ "xp(level) = baseValue + (level * constantGrowth) ^ exponentialGrowth"
			);
			actionXp = getDouble("actionXp", 0.05, 0, Long.MAX_VALUE,
				"This controls how much experience a robot gains for each successful"
					+ "action it performs. \"Actions\" only include the following: swinging a"
					+ "tool and killing something or destroying a block and placing a block"
					+ "successfully. Note that a call to `swing` or `use` while \"bare handed\""
					+ "will *not* gain a robot any experience."
			);
			exhaustionXpRate = getDouble("exhaustionXpRate", 1.0, 0, Long.MAX_VALUE,
				"This determines how much \"exhaustion\" contributes to a robots"
					+ "experience. This is additive to the \"action\" xp, so digging a block"
					+ "will per default give 0.05 + 0.025 [exhaustion] * 1.0 = 0.075 XP."
			);
			oreXpRate = getDouble("oreXpRate", 4.0, 0, Long.MAX_VALUE,
				"This determines how much experience a robot gets for each real XP orb"
					+ "an ore it harvested would have dropped. For example, coal is worth"
					+ "two real experience points, redstone is worth 5."
			);
			bufferPerLevel = getDouble("bufferPerLevel", 5000, 0, Long.MAX_VALUE,
				"This is the amount of additional energy that fits into a robots"
					+ "internal buffer for each level it gains. So with the default values,"
					+ "at maximum level (30) a robot will have an internal buffer size of"
					+ "two hundred thousand."
			);
			toolEfficiencyPerLevel = getDouble("toolEfficiencyPerLevel", 0.01, 0, Long.MAX_VALUE,
				"The additional \"efficiency\" a robot gains in using tools with each"
					+ "level. This basically increases the chances of a tool not losing"
					+ "durability when used, relative to the base rate. So for example, a"
					+ "robot with level 15 gets a 0.15 bonus, with the default damage rate"
					+ "that would lead to a damage rate of 0.1 * (1 - 0.15) = 0.085."
			);
			harvestSpeedBoostPerLevel = getDouble("harvestSpeedBoostPerLevel", 0.02, 0, Long.MAX_VALUE,
				"The increase in block harvest speed a robot gains per level. The time"
					+ "it takes to break a block is computed as actualTime * (1 - bonus)."
					+ "For example at level 20, with a bonus of 0.4 instead of taking 0.3"
					+ "seconds to break a stone block with a diamond pick axe it only takes"
					+ "0.12 seconds."
			);
		}
	},
	RobotDelays(Robot, "Allows fine-tuning of delays for robot actions.") {
		// Note: all delays are reduced by one tick to account for the tick they are
		// performed in (since all actions are delegated to the server thread).
		public double turn;
		public double move;
		public double swing;
		public double use;
		public double place;
		public double drop;
		public double suck;
		public double harvestRatio;

		@Override
		protected void load() {
			turn = getDouble("turn", 0.4, 0.11, 500,
				"The time in seconds to pause execution after a robot turned either"
					+ "left or right. Note that this essentially determines hw fast robots"
					+ "can turn around, since this also determines the length of the turn"
					+ "animation."
			) - 0.06;
			move = getDouble("move", 0.4, 0.11, 500,
				"The time in seconds to pause execution after a robot issued a"
					+ "successful move command. Note that this essentially determines how"
					+ "fast robots can move around, since this also determines the length"
					+ "of the move animation."
			) - 0.06;
			swing = getDouble("swing", 0.4, 0.11, 500,
				"The time in seconds to pause execution after a robot successfully"
					+ "swung a tool (or it's 'hands' if nothing is equipped). Successful in"
					+ "this case means that it hit something, i.e. it attacked an entity or"
					+ "extinguishing fires."
					+ "When breaking blocks the normal harvest time scaled with the"
					+ "`harvestRatio` (see below) applies."
			) - 0.06;
			use = getDouble("use", 0.4, 0.11, 500,
				"The time in seconds to pause execution after a robot successfully"
					+ "used an equipped tool (or it's 'hands' if nothing is equipped)."
					+ "Successful in this case means that it either used the equipped item,"
					+ "for example a splash potion, or that it activated a block, for"
					+ "example by pushing a button."
					+ "Note that if an item is used for a specific amount of time, like"
					+ "when shooting a bow, the maximum of this and the duration of the"
					+ "item use is taken."
			) - 0.06;
			place = getDouble("place", 0.4, 0.11, 500,
				"The time in seconds to pause execution after a robot successfully"
					+ "placed an item from its inventory."
			) - 0.06;
			drop = getDouble("drop", 0.5, 0.11, 500,
				"The time in seconds to pause execution after an item was"
					+ "successfully dropped from a robot's inventory."
			) - 0.06;
			suck = getDouble("suck", 0.5, 0.11, 500,
				"The time in seconds to pause execution after a robot successfully"
					+ "picked up an item after triggering a suck command."
			) - 0.06;
			harvestRatio = getDouble("harvestRatio", 1.0, 0, 65536,
				"This is the *ratio* of the time a player would require to harvest a"
					+ "block. Note that robots cannot break blocks they cannot harvest. So"
					+ "the time a robot is forced to sleep after harvesting a block is"
					+ "breakTime * harvestRatio"
					+ "Breaking a block will always at least take one tick, 0.05 seconds."
			);
		}
	},
	Power("Power settings, buffer sizes and power consumption.") {
		public boolean is3rdPartyPowerSystemPresent = false;
		public boolean pureIgnorePower;

		public boolean shouldIgnorePower() {
			return pureIgnorePower || (!is3rdPartyPowerSystemPresent && !Mods.isPowerProvidingModPresent);
		}

		public double tickFrequency;
		public double chargeRateExternal;
		public double chargeRateTablet;
		public double generatorEfficiency;
		public double solarGeneratorEfficiency;
		public double assemblerTickAmount;
		public double disassemblerTickAmount;
		public double printerTickAmount;
		public String[] powerModBlacklist;

		@Override
		protected void load() {
			pureIgnorePower = getBoolean("ignorePower", false,
				"Whether to ignore any power requirements. Whenever something requires"
					+ "power to function, it will try to get the amount of energy it needs from"
					+ "the buffer of its connector node, and in case it fails it won't perform"
					+ "the action / trigger a shutdown / whatever. Setting this to `true` will"
					+ "simply make the check 'is there enough energy' succeed unconditionally."
					+ "Note that buffers are still filled and emptied following the usual"
					+ "rules, there just is no failure case anymore. The converter will however"
					+ "not accept power from other mods."
			);
			tickFrequency = getDouble("tickFrequency", 10, 1, Integer.MAX_VALUE,
				"This determines how often continuous power sinks try to actually try to"
					+ "consume energy from the network. This includes computers, robots and"
					+ "screens. This also controls how frequent distributors revalidate their"
					+ "global state and secondary distributors, as well as how often the power"
					+ "converter queries sources for energy (for now: only BuildCraft). If set"
					+ "to 1, this would query every tick. The default queries every 10 ticks,"
					+ "or in other words twice per second."
					+ "Higher values mean more responsive power consumption, but slightly more"
					+ "work per tick (shouldn't be that noticeable, though). Note that this"
					+ "has no influence on the actual amount of energy required by computers"
					+ "and screens. The power cost is directly scaled up accordingly:"
					+ "`tickFrequency * cost`."
			);
			chargeRateExternal = getDouble("chargerChargeRate", 100.0,
				"The amount of energy a Charger transfers to each adjacent robot per tick"
					+ "if a maximum strength redstone signal is set. Chargers load robots with"
					+ "a controllable speed, based on the maximum strength of redstone signals"
					+ "going into the block. So if a redstone signal of eight is set, it'll"
					+ "charge robots at roughly half speed."
			);
			chargeRateTablet = getDouble("chargerChargeRateTablet", 10.0,
				"The amount of energy a Charger transfers into a tablet, if present, per"
					+ "tick. This is also based on configured charge speed, as for robots."
			);
			generatorEfficiency = getDouble("generatorEfficiency", 0.8,
				"The energy efficiency of the generator upgrade. At 1.0 this will"
					+ "generate as much energy as you'd get by burning the fuel in a BuildCraft"
					+ "Stirling Engine (1MJ per fuel value / burn ticks). To discourage fully"
					+ "autonomous robots the efficiency of generators is slighly reduced by"
					+ "default."
			);
			solarGeneratorEfficiency = getDouble("solarGeneratorEfficiency", 0.2,
				"The energy efficiency of the solar generator upgrade. At 1.0 this will"
					+ "generate as much energy as you'd get by burning  fuel in a BuildCraft"
					+ "Stirling Engine . To discourage fully autonomous robots the efficiency"
					+ "of solar generators is greatly reduced by default."
			);
			assemblerTickAmount = getDouble("assemblerTickAmount", 50, 1, 65536,
				"The amount of energy the robot assembler can apply per tick. This"
					+ "controls the speed at which robots are assembled, basically."
			);
			disassemblerTickAmount = getDouble("disassemblerTickAmount", 25, 1, 65536,
				"The amount of energy the disassembler can apply per tick. This"
					+ "controls the speed at which items are disassembled, basically."
			);
			printerTickAmount = getDouble("printerTickAmount", 1, 1, 65536,
				"The amount of energy the printer can apply per tick. This controls"
					+ "the speed at which prints are completed, basically."
			);
			powerModBlacklist = getStringList("modBlacklist", new String[] {},
				"If you don't want OpenComputers to accept power from one or more of the"
					+ "supported power mods, for example because it doesn't suit the vision"
					+ "of your mod pack, you can disable support for them here. To stop"
					+ "OpenComputers accepting power from a mod, enter its mod id here, e.g."
					+ "`BuildCraftAPI|power`, `IC2`, `factorization`, ..."
			);
		}
	},
	PowerBuffer(Power, "Default \"buffer\" sizes, i.e. how much energy certain blocks can store.") {
		public double capacitor;
		public double capacitorAdjacencyBonus;
		public double computer;
		public double robot;
		public double converter;
		public double distributor;
		public double[] capacitorUpgrades;
		public double tablet;
		public double accessPoint;
		public double drone;
		public double microcontroller;
		public double hoverBoots;
		public double nanomachines;

		@Override
		protected void load() {
			capacitor = getDouble("capacitor", 1600.0, 0, Long.MAX_VALUE,
				"The amount of energy a single capacitor can store."
			);
			capacitorAdjacencyBonus = getDouble("capacitorAdjacencyBonus", 800.0, 0, Long.MAX_VALUE,
				"The amount of bonus energy a capacitor can store for each other"
					+ "capacitor it shares a face with. This bonus applies to both of the"
					+ "involved capacitors. It reaches a total of two blocks, where the"
					+ "bonus is halved for the second neighbor. So three capacitors in a"
					+ "row will give a total of 8.8k storage with default values:"
					+ "(1.6 + 0.8 + 0.4)k + (0.8 + 1.6 + 0.8)k + (0.4 + 0.8 + 1.6)k"
			);
			capacitorUpgrades = getDoubleList("capacitorUpgrades", new double[] {
					10000,
					15000,
					20000
				},
				"The amount of energy a capacitor can store when installed as an"
					+ "upgrade into a robot.",
				"Bad number of battery upgrade buffer sizes, ignoring."
			);
			computer = getDouble("computer", 500.0, 0, Long.MAX_VALUE,
				"The amount of energy a computer can store. This allows you to get a"
					+ "computer up and running without also having to build a capacitor."
			);
			microcontroller = getDouble("microcontroller", 1000.0, 0, Long.MAX_VALUE,
				"The amount of energy a microcontroller can store in its internal"
					+ "buffer."
			);
			tablet = getDouble("tablet", 10000, 0, Long.MAX_VALUE,
				"The amount a tablet can store in its internal buffer."
			);
			robot = getDouble("robot", 20000.0, 0, Long.MAX_VALUE,
				"The amount of energy robots can store in their internal buffer."
			);
			drone = getDouble("drone", 5000.0, 0, Long.MAX_VALUE,
				"The amount of energy a drone can store in its internal buffer."
			);
			converter = getDouble("converter", 1000.0, 0, Long.MAX_VALUE,
				"The amount of energy a converter can store. This allows directly"
					+ "connecting a converter to a distributor, without having to have a"
					+ "capacitor on the side of the converter."
			);
			distributor = getDouble("distributor", 500, 0, Long.MAX_VALUE,
				"The amount of energy each face of a distributor can store. This"
					+ "allows connecting two power distributors directly. If the buffer"
					+ "capacity between the two distributors is zero, they won't be able"
					+ "to exchange energy. This basically controls the bandwidth. You can"
					+ "add capacitors between two distributors to increase this bandwidth."
			);
			accessPoint = getDouble("accessPoint", 600.0, 0, Long.MAX_VALUE,
				"The amount of energy an access point can store."
			);
			hoverBoots = getDouble("hoverBoots", 15000.0, 1, Long.MAX_VALUE,
				"The internal buffer size of the hover boots."
			);
			nanomachines = getDouble("nanomachines", 100000, 0, Long.MAX_VALUE,
				"Amount of energy stored by nanomachines. Yeah, I also don't know"
					+ "where all that energy is stored. It's quite fascinating."
			);
		}
	},
	PowerCost(Power, "Default \"costs\", i.e. how much energy certain operations consume.") {
		public double computer;
		public double microcontroller;
		public double robot;
		public double drone;
		public double sleepFactor;
		public double screen;
		public double hologram;
		public double hddRead;
		public double hddWrite;
		public double gpuSet;
		public double gpuFill;
		public double gpuClear;
		public double gpuCopy;
		public double robotTurn;
		public double robotMove;
		public double robotExhaustion;
		public double wirelessCostPerRange;
		public double abstractBusPacket;
		public double geolyzerScan;
		public double robotBaseCost;
		public double robotComplexityCost;
		public double microcontrollerBaseCost;
		public double microcontrollerComplexityCost;
		public double tabletBaseCost;
		public double tabletComplexityCost;
		public double droneBaseCost;
		public double droneComplexityCost;
		public double disassemblerItemCost;
		public double chunkloader;
		public double pistonPush;
		public double eepromWrite;
		public double printerModel;
		public double hoverBootJump;
		public double hoverBootAbsorb;
		public double hoverBootMove;
		public double dataCardTrivial;
		public double dataCardTrivialByte;
		public double dataCardSimple;
		public double dataCardSimpleByte;
		public double dataCardComplex;
		public double dataCardComplexByte;
		public double dataCardAsymmetric;
		public double transposer;
		public double nanomachineInput;
		public double nanomachineReconfigure;
		public double mfuRelay;

		@Override
		protected void load() {
			computer = getDouble("computer", 0.5, 0, Long.MAX_VALUE,
				"The amount of energy a computer consumes per tick when running."
			);
			microcontroller = getDouble("microcontroller", 0.1, 0, Long.MAX_VALUE,
				"Amount of energy a microcontroller consumes per tick while running."
			);
			robot = getDouble("robot", 0.25, 0, Long.MAX_VALUE,
				"The amount of energy a robot consumes per tick when running. This is"
					+ "per default less than a normal computer uses because... well... they"
					+ "are better optimized? It balances out due to the cost for movement,"
					+ "interaction and whatnot, and the fact that robots cannot connect to"
					+ "component networks directly, so they are no replacements for normal"
					+ "computers."
			);
			drone = getDouble("drone", 0.4, 0, Long.MAX_VALUE,
				"The amount of energy a drone consumes per tick when running."
			);
			sleepFactor = getDouble("sleepFactor", 0.1, 0, Long.MAX_VALUE,
				"The actual cost per tick for computers and robots is multiplied"
					+ "with this value if they are currently in a \"sleeping\" state. They"
					+ "enter this state either by calling `os.sleep()` or by pulling"
					+ "signals. Note that this does not apply in the tick they resume, so"
					+ "you can't fake sleep by calling `os.sleep(0)`."
			);
			screen = getDouble("screen", 0.05, 0, Long.MAX_VALUE,
				"The amount of energy a screen consumes per tick. For each lit pixel"
					+ "(each character that is not blank) this cost increases linearly:"
					+ "for basic screens, if all pixels are lit the cost per tick will be"
					+ "this value. Higher tier screens can become even more expensive to"
					+ "run, due to their higher resolution. If a screen cannot consume the"
					+ "defined amount of energy it will stop rendering the text that"
					+ "should be displayed on it. It will *not* forget that text, however,"
					+ "so when enough power is available again it will restore the"
					+ "previously displayed text (with any changes possibly made in the"
					+ "meantime). Note that for multi-block screens *each* screen that is"
					+ "part of it will consume this amount of energy per tick."
			);
			hologram = getDouble("hologram", 0.2, 0, Long.MAX_VALUE,
				"The amount of energy a hologram projetor consumes per tick. This"
					+ "is the cost if every column is lit. If not a single voxel is"
					+ "displayed the hologram projector will not drain energy."
			);
			hddRead = getDouble("hddRead", 0.1, 0, Long.MAX_VALUE,
				"Energy it takes read one kilobyte from a file system. Note that non"
					+ "I/O operations on file systems such as `list` or `getFreeSpace` do"
					+ "*not* consume power. Note that this very much determines how much"
					+ "energy you need in store to start a computer, since you need enough"
					+ "to have the computer read all the libraries, which is around 60KB"
					+ "at the time of writing."
					+ "Note: internally this is adjusted to a cost per byte, and applied"
					+ "as such. It's just specified per kilobyte to be more intuitive."
			) / 1024;
			hddWrite = getDouble("hddWrite", 0.25, 0, Long.MAX_VALUE,
				"Energy it takes to write one kilobyte to a file system."
					+ "Note: internally this is adjusted to a cost per byte, and applied"
					+ "as such. It's just specified per kilobyte to be more intuitive."
			) / 1024;
			gpuSet = getDouble("gpuSet", 2.0, 0, Long.MAX_VALUE,
				"Energy it takes to change *every* 'pixel' via the set command of a"
					+ "basic screen via the `set` command."
					+ "Note: internally this is adjusted to a cost per pixel, and applied"
					+ "as such, so this also implicitly defines the cost for higher tier"
					+ "screens."
			) / Settings.getBasicScreenPixels();
			gpuFill = getDouble("gpuFill", 1.0, 0, Long.MAX_VALUE,
				"Energy it takes to change a basic screen with the fill command."
					+ "Note: internally this is adjusted to a cost per pixel, and applied"
					+ "as such, so this also implicitly defines the cost for higher tier"
					+ "screens."
			) / Settings.getBasicScreenPixels();
			gpuClear = getDouble("gpuClear", 0.1, 0, Long.MAX_VALUE,
				"Energy it takes to clear a basic screen using the fill command with"
					+ "'space' as the fill char."
					+ "Note: internally this is adjusted to a cost per pixel, and applied"
					+ "as such, so this also implicitly defines the cost for higher tier"
					+ "screens."
			) / Settings.getBasicScreenPixels();
			gpuCopy = getDouble("gpuCopy", 0.25, 0, Long.MAX_VALUE,
				"Energy it takes to copy half of a basic screen via the copy command."
					+ "Note: internally this is adjusted to a cost per pixel, and applied"
					+ "as such, so this also implicitly defines the cost for higher tier"
					+ "screens."
			) / Settings.getBasicScreenPixels();
			robotTurn = getDouble("robotTurn", 2.5, 0, Long.MAX_VALUE,
				"The amount of energy it takes a robot to perform a 90 degree turn."
			);
			robotMove = getDouble("robotMove", 15.0, 0, Long.MAX_VALUE,
				"The amount of energy it takes a robot to move a single block."
			);
			robotExhaustion = getDouble("robotExhaustion", 10.0, 0, Long.MAX_VALUE,
				"The conversion rate of exhaustion from using items to energy"
					+ "consumed. Zero means exhaustion does not require energy, one is a"
					+ "one to one conversion. For example, breaking a block generates 0.025"
					+ "exhaustion, attacking an entity generates 0.3 exhaustion."
			);
			wirelessCostPerRange = getDouble("wirelessCostPerRange", 0.05, 0, Long.MAX_VALUE,
				"The amount of energy it costs to send a wireless message with signal"
					+ "strength one, which means the signal reaches one block. This is"
					+ "scaled up linearly, so for example to send a signal 400 blocks a"
					+ "signal strength of 400 is required, costing a total of"
					+ "400 * `wirelessCostPerRange`. In other words, the higher this value,"
					+ "the higher the cost of wireless messages."
					+ "See also: `maxWirelessRange`."
			);
			abstractBusPacket = getDouble("abstractBusPacket", 1, 0, Long.MAX_VALUE,
				"The cost of a single packet sent via StargateTech 2's abstract bus."
			);
			geolyzerScan = getDouble("geolyzerScan", 10, 0, Long.MAX_VALUE,
				"How much energy is consumed when the Geolyzer scans a block."
			);
			robotBaseCost = getDouble("robotAssemblyBase", 50000, 0, Long.MAX_VALUE,
				"The base energy cost for assembling a robot."
			);
			robotComplexityCost = getDouble("robotAssemblyComplexity", 10000, 0, Long.MAX_VALUE,
				"The additional amount of energy required to assemble a robot for"
					+ "each point of complexity."
			);
			microcontrollerBaseCost = getDouble("microcontrollerAssemblyBase", 10000, 0, Long.MAX_VALUE,
				"The base energy cost for assembling a microcontroller."
			);
			microcontrollerComplexityCost = getDouble("microcontrollerAssemblyComplexity", 10000, 0, Long.MAX_VALUE,
				"The additional amount of energy required to assemble a"
					+ "microcontroller for each point of complexity."
			);
			tabletBaseCost = getDouble("tabletAssemblyBase", 20000, 0, Long.MAX_VALUE,
				"The base energy cost for assembling a tablet."
			);
			tabletComplexityCost = getDouble("tabletAssemblyComplexity", 5000, 0, Long.MAX_VALUE,
				"The additional amount of energy required to assemble a tablet for"
					+ "each point of complexity."
			);
			droneBaseCost = getDouble("droneAssemblyBase", 25000, 0, Long.MAX_VALUE,
				"The base energy cost for assembling a drone."
			);
			droneComplexityCost = getDouble("droneAssemblyComplexity", 15000, 0, Long.MAX_VALUE,
				"The additional amount of energy required to assemble a"
					+ "drone for each point of complexity."
			);
			disassemblerItemCost = getDouble("disassemblerPerItem", 2000, 0, Long.MAX_VALUE,
				"The amount of energy it takes to extract one ingredient from an"
					+ "item that is being disassembled. For example, if an item that was"
					+ "crafted from three other items gets disassembled, a total of 15000"
					+ "energy will be required by default."
					+ "Note that this is consumed over time, and each time this amount is"
					+ "reached *one* ingredient gets ejected (unless it breaks, see the"
					+ "disassemblerBreakChance setting)."
			);
			chunkloader = getDouble("chunkloader", 0.06, 0, Long.MAX_VALUE,
				"The amount of energy the chunkloader upgrade draws per tick while"
					+ "it is enabled, i.e. actually loading a chunk."
			);
			pistonPush = getDouble("pistonPush", 20, 0, Long.MAX_VALUE,
				"The amount of energy pushing blocks with the piston upgrade costs."
			);
			eepromWrite = getDouble("eepromWrite", 50, 0, Long.MAX_VALUE,
				"Energy it costs to re-program an EEPROM. This is deliberately"
					+ "expensive, to discourage frequent re-writing of EEPROMs."
			);
			printerModel = getDouble("printerModel", 100, 0, Long.MAX_VALUE,
				"How much energy is required for a single 3D print."
			);
			hoverBootJump = getDouble("hoverBootJump", 10, 0, Long.MAX_VALUE,
				"The amount of energy consumed when jumping with the hover boots. Only"
					+ "applies when the jump boost is applied, i.e. when not sneaking."
			);
			hoverBootAbsorb = getDouble("hoverBootAbsorb", 10, 0, Long.MAX_VALUE,
				"The amount of energy consumed when the hover boots absorb some fall"
					+ "velocity (i.e. when falling from something higher than three blocks)."
			);
			hoverBootMove = getDouble("hoverBootMove", 1, 0, Long.MAX_VALUE,
				"The amount of energy consumed *per second* when moving around while"
					+ "wearing the hover boots. This is compensate for the step assist, which"
					+ "does not consume energy on a per-use basis. When standing still or"
					+ "moving very slowly this also does not trigger."
			);
			dataCardTrivial = getDouble("dataCardTrivial", 0.2, 0, Long.MAX_VALUE,
				"Cost for trivial operations on the data card, such as CRC32 or Base64"
			);
			dataCardTrivialByte = getDouble("dataCardTrivialByte", 0.005, 0, Long.MAX_VALUE,
				"Per-byte cost for trivial operations"
			);
			dataCardSimple = getDouble("dataCardSimple", 1.0, 0, Long.MAX_VALUE,
				"Cost for simple operations on the data card, such as MD5 or AES"
			);
			dataCardSimpleByte = getDouble("dataCardSimpleByte", 0.01, 0, Long.MAX_VALUE,
				"Per-byte cost for simple operations"
			);
			dataCardComplex = getDouble("dataCardComplex", 6.0, 0, Long.MAX_VALUE,
				"Cost for complex operations on the data card, such as SHA256, inflate/deflate and SecureRandom."
			);
			dataCardComplexByte = getDouble("dataCardComplexByte", 0.1, 0, Long.MAX_VALUE,
				"Per-byte cost for complex operations"
			);
			dataCardAsymmetric = getDouble("dataCardAsymmetric", 10.0, 0, Long.MAX_VALUE,
				"Cost for asymmetric operations on the data card, such as ECDH and ECDSA"
					+ "Per-byte cost for ECDSA operation is controlled by `complex` value,"
					+ "because data is hashed with SHA256 before signing/verifying"
			);
			transposer = getDouble("transposer", 1, 0, Long.MAX_VALUE,
				"Energy required for one transposer operation (regardless of the number"
					+ "of items / fluid volume moved)."
			);
			nanomachineInput = getDouble("nanomachineInput", 0.5, 0, Long.MAX_VALUE,
				"Energy consumed per tick per active input node by nanomachines."
			);
			nanomachineReconfigure = getDouble("nanomachineReconfigure", 5000, 0, Long.MAX_VALUE,
				"Energy consumed when reconfiguring nanomachines."
			);
			mfuRelay = getDouble("mfuRelay", 1, 0, Long.MAX_VALUE,
				"Energy consumed by a MFU per tick while connected."
					+ "Similarly to `wirelessCostPerRange`, this is multiplied with the distance to the bound block."
			);
		}
	},
	PowerRates(Power, "The rate at which different blocks accept external power. All of these\nvalues are in OC energy / tick.") {
		public double accessPoint;
		public double assembler;
		public double[] caseRate;
		// Creative case.
		public double charger;
		public double disassembler;
		public double powerConverter;
		public double serverRack;

		@Override
		protected void load() {
			accessPoint = getDouble("accessPoint", 10.0, 0, Long.MAX_VALUE,
				""
			);
			assembler = getDouble("assembler", 100.0, 0, Long.MAX_VALUE,
				""
			);
			caseRate = getDoubleList("case", new double[] {
					5.0,
					10.0,
					20.0,
				},
				"",
				"Bad number of computer case conversion rates, ignoring."
			);
			charger = getDouble("charger", 200.0, 0, Long.MAX_VALUE,
				""
			);
			disassembler = getDouble("disassembler", 50.0, 0, Long.MAX_VALUE,
				""
			);
			powerConverter = getDouble("powerConverter", 500.0, 0, Long.MAX_VALUE,
				""
			);
			serverRack = getDouble("serverRack", 50.0, 0, Long.MAX_VALUE,
				""
			);
		}
	},
	ConversionRates(Power,
		"Power values for different power systems. For reference, the value of\n"
			+ "OC's internal energy type is 1000. I.e. the conversion ratios are the\n"
			+ "values here divided by 1000. This is mainly to avoid small floating\n"
			+ "point numbers in the config, due to potential loss of precision."
	) {
		private double AppliedEnergistics2;
		private double Factorization;
		private double Galacticraft;
		private double IndustrialCraft2;
		private double Mekanism;
		private double PowerAdvantage;
		private double RedstoneFlux;
		private double RotaryCraft;

		private double Internal = 1000;

		public double ratioAppliedEnergistics2() {
			return AppliedEnergistics2 / Internal;
		}

		public double ratioFactorization() {
			return Factorization / Internal;
		}

		public double ratioGalacticraft() {
			return Galacticraft / Internal;
		}

		public double ratioIndustrialCraft2() {
			return IndustrialCraft2 / Internal;
		}

		public double ratioMekanism() {
			return Mekanism / Internal;
		}

		public double ratioPowerAdvantage() {
			return PowerAdvantage / Internal;
		}

		public double ratioRedstoneFlux() {
			return RedstoneFlux / Internal;
		}

		public double ratioRotaryCraft() {
			return RotaryCraft / Internal;
		}

		@Override
		protected void load() {
			AppliedEnergistics2 = getDouble("AppliedEnergistics2", 200.0,
				""
			);
			Factorization = getDouble("Factorization", 13.0,
				""
			);
			Galacticraft = getDouble("Galacticraft", 48.0,
				""
			);
			IndustrialCraft2 = getDouble("IndustrialCraft2", 400.0,
				""
			);
			Mekanism = getDouble("Mekanism", 1333.33,
				""
			);
			PowerAdvantage = getDouble("PowerAdvantage", 31.25,
				""
			);
			RedstoneFlux = getDouble("RedstoneFlux", 100.0,
				""
			);
			RotaryCraft = getDouble("RotaryCraft", 200.0,
				""
			); // 11256, same as AE2
		}
	},
	Filesystem("File system related settings, performance and and balancing.") {
		public int fileCost;
		public boolean bufferChanges;
		public int[] hddSizes;
		public int[] hddPlatterCounts;
		public int floppySize;
		public int tmpSize;
		public int maxHandles;
		public int maxReadBuffer;
		public int sectorSeekThreshold;
		public double sectorSeekTime;

		@Override
		protected void load() {
			bufferChanges = getBoolean("bufferChanges", true,
				"Whether persistent file systems such as disk drives should be"
					+ "'buffered', and only written to disk when the world is saved. This"
					+ "applies to all hard drives. The advantage of having this enabled is that"
					+ "data will never go 'out of sync' with the computer's state if the game"
					+ "crashes. The price is slightly higher memory consumption, since all"
					+ "loaded files have to be kept in memory (loaded as in when the hard drive"
					+ "is in a computer)."
			);
			fileCost = getInt("fileCost", 512, 0, Integer.MAX_VALUE,
				"The base 'cost' of a single file or directory on a limited file system,"
					+ "such as hard drives. When computing the used space we add this cost to"
					+ "the real size of each file (and folders, which are zero sized"
					+ "otherwise). This is to ensure that users cannot spam the file system"
					+ "with an infinite number of files and/or folders. Note that the size"
					+ "returned via the API will always be the real file size, however."
			);
			hddSizes = getIntList("hddSizes", new int[] {
					1024,
					2048,
					4096
				},
				"The sizes of the three tiers of hard drives, in kilobytes. This list"
					+ "must contain exactly three entries, or it will be ignored.",
				"Bad number of HDD sizes, ignoring."
			);
			floppySize = getInt("floppySize", 512, 0, Integer.MAX_VALUE,
				"The size of writable floppy disks, in kilobytes."
			);
			tmpSize = getInt("tmpSize", 64, 0, Integer.MAX_VALUE,
				"The size of the /tmp filesystem that each computer gets for free. If"
					+ "set to a non-positive value the tmp file system will not be created."
			);
			maxHandles = getInt("maxHandles", 16, 0, Integer.MAX_VALUE,
				"The maximum number of file handles any single computer may have open at"
					+ "a time. Note that this is *per filesystem*. Also note that this is only"
					+ "enforced by the filesystem node - if an add-on decides to be fancy it"
					+ "may well ignore this. Since file systems are usually 'virtual' this will"
					+ "usually not have any real impact on performance and won't be noticeable"
					+ "on the host operating system."
			);
			maxReadBuffer = getInt("maxReadBuffer", 2048, 0, Integer.MAX_VALUE,
				"The maximum block size that can be read in one 'read' call on a file"
					+ "system. This is used to limit the amount of memory a call from a user"
					+ "program can cause to be allocated on the host side: when 'read' is,"
					+ "called a byte array with the specified size has to be allocated. So if"
					+ "this weren't limited, a Lua program could trigger massive memory"
					+ "allocations regardless of the amount of RAM installed in the computer it"
					+ "runs on. As a side effect this pretty much determines the read"
					+ "performance of file systems."
			);
			hddPlatterCounts = getIntList("hddPlatterCounts", new int[] { 2, 4, 6 },
				"Number of physical platters to pretend a disk has in unmanaged mode. This"
					+ "controls seek times, in how it emulates sectors overlapping (thus sharing"
					+ "a common head position for access).",
				"Bad number of HDD platter counts, ignoring."
			);
			sectorSeekThreshold = getInt("sectorSeekThreshold", 128,
				"When skipping more than this number of sectors in unmanaged mode, the"
					+ "pause specified in sectorSeekTime will be enforced. We use this instead"
					+ "of linear scaling for movement because those values would have to be"
					+ "really small, which is hard to conceptualize and configure."
			);
			sectorSeekTime = getDouble("sectorSeekTime", 0.1,
				"The time to pause when the head movement threshold is exceeded."
			);
		}
	},
	Internet("Internet settings, security related.") {
		public boolean enableHttp;
		public boolean enableHttpHeaders;
		public boolean enableTcp;
		public List<AddressValidator> httpHostBlacklist;
		public List<AddressValidator> httpHostWhitelist;
		public int httpTimeout;
		public int maxTcpConnections;
		public int internetThreads;

		@Override
		protected void load() {
			enableHttp = getBoolean("enableHttp", true,
				"Whether to allow HTTP requests via internet cards. When enabled,"
					+ "the `request` method on internet card components becomes available."
			);
			enableHttpHeaders = getBoolean("enableHttpHeaders", true,
				"Whether to allow adding custom headers to HTTP requests."
			);
			enableTcp = getBoolean("enableTcp", true,
				"Whether to allow TCP connections via internet cards. When enabled,"
					+ "the `connect` method on internet card components becomes available."
			);
			httpHostBlacklist = Arrays.stream(getStringList("blacklist", new String[] {
					"127.0.0.0/8",
					"10.0.0.0/8",
					"192.168.0.0/16",
					"172.16.0.0/12"
				},
				"This is a list of blacklisted domain names. If an HTTP request is made"
					+ "or a socket connection is opened the target address will be compared"
					+ "to the addresses / adress ranges in this list. It it is present in this"
					+ "list, the request will be denied."
					+ "Entries are either domain names (www.example.com) or IP addresses in"
					+ "string format (10.0.0.3), optionally in CIDR notation to make it easier"
					+ "to define address ranges (1.0.0.0/8). Domains are resolved to their"
					+ "actual IP once on startup, future requests are resolved and compared"
					+ "to the resolved addresses."
					+ "By default all local addresses are blocked. This is only meant as a"
					+ "thin layer of security, to avoid average users hosting a game on their"
					+ "local machine having players access services in their local network."
					+ "Server hosters are expected to configure their network outside of the"
					+ "mod's context in an appropriate manner, e.g. using a system firewall."
			)).map(AddressValidator::create).collect(Collectors.toList());
			httpHostWhitelist = Arrays.stream(getStringList("whitelist", new String[] {},
				"This is a list of whitelisted domain names. Requests may only be made"
					+ "to addresses that are present in this list. If this list is empty,"
					+ "requests may be made to all addresses not blacklisted. Note that the"
					+ "blacklist is always applied, so if an entry is present in both the"
					+ "whitelist and the blacklist, the blacklist will win."
					+ "Entries are of the same format as in the blacklist. Examples:"
					+ "\"gist.github.com\", \"www.pastebin.com\""
			)).map(AddressValidator::create).collect(Collectors.toList());
			;
			httpTimeout = getInt("requestTimeout", 0, 0, Integer.MAX_VALUE,
				"The time in seconds to wait for a response to a request before timing"
					+ "out and returning an error message. If this is zero (the default) the"
					+ "request will never time out."
			) * 1000;
			maxTcpConnections = getInt("maxTcpConnections", 4, 0, Integer.MAX_VALUE,
				"The maximum concurrent TCP connections *each* internet card can have"
					+ "open at a time."
			);
			internetThreads = getInt("threads", 4, 1, Integer.MAX_VALUE,
				"The number of threads used for processing host name lookups and HTTP"
					+ "requests in the background. The more there are, the more concurrent"
					+ "connections can potentially be opened by computers, and the less likely"
					+ "they are to delay each other."
			);
		}

	},
	Relay("Relay network message forwarding logic related stuff.") {
		public int defaultMaxQueueSize;
		public int queueSizeUpgrade;
		public int defaultRelayDelay;
		public double relayDelayUpgrade;
		public int defaultRelayAmount;
		public int relayAmountUpgrade;

		@Override
		protected void load() {
			defaultRelayDelay = getInt("defaultRelayDelay", 5, 1, Integer.MAX_VALUE,
				"The delay a relay has by default between relaying packets (in ticks)."
					+ "WARNING: lowering this value will result in higher maximum CPU load,"
					+ "and may in extreme cases cause server lag."
			);
			relayDelayUpgrade = getDouble("relayDelayUpgrade", 1.5, 0, Integer.MAX_VALUE,
				"The amount of ticks the delay is *reduced* by per tier of the CPU"
					+ "inserted into a relay."
			);
			defaultMaxQueueSize = getInt("defaultMaxQueueSize", 20, 1, Integer.MAX_VALUE,
				"This is the size of the queue of a not upgraded relay. Increasing it"
					+ "avoids packets being dropped when many messages are sent in a single"
					+ "burst."
			);
			queueSizeUpgrade = getInt("queueSizeUpgrade", 10, 0, Integer.MAX_VALUE,
				"This is the amount by which the queue size increases per tier of the"
					+ "hard drive installed in the relay."
			);
			defaultRelayAmount = getInt("defaultRelayAmount", 1, 1, Integer.MAX_VALUE,
				"The base number of packets that get relayed in one 'cycle'. The"
					+ "cooldown between cycles is determined by the delay."
			);
			relayAmountUpgrade = getInt("relayAmountUpgrade", 1, 0, Integer.MAX_VALUE,
				"The number of additional packets that get relayed per cycle, based on"
					+ "the tier of RAM installed in the relay. For built-in RAM this"
					+ "increases by one per half-tier, for third-party ram this increases by"
					+ "two per item tier."
			);
		}
	},
	Nanomachines(
		"Nanomachine related values. Note that most of these are relative, as\n"
			+ "they scale with the number of total effects controlled by nanomachines,\n"
			+ "which may very much vary depending on other mods used together with OC.\n"
			+ "To configure this, you'll need to know how this works a bit more in-\n"
			+ "depth, so here goes: there are three layers, the behavior layer, the\n"
			+ "connector layer, and the input layer. The behavior layer consists of\n"
			+ "one node for each behavior provided by registered providers (by default\n"
			+ "these will be potion effects and a few other things). The connector\n"
			+ "layer merely serves to mix things up a little. The input layer is made\n"
			+ "up from nodes that can be triggered by the nanomachines. Each connector\n"
			+ "node has behavior nodes it outputs to, and gets signals from input nodes.\n"
			+ "Behavior nodes get signals from both the connector and the input layers.\n"
			+ "Reconfiguring builds up random connections. Some behaviors change what\n"
			+ "they do based on the number of active inputs (e.g. potion effects will\n"
			+ "increase their amplification value)."
	) {
		public double triggerQuota;
		public double connectorQuota;
		public int maxInputs;
		public int maxOutputs;
		public int safeInputsActive;
		public int maxInputsActive;
		public double commandDelay;
		public double commandRange;
		public double magnetRange;
		public int disintegrationRange;
		public String[] potionWhitelist;
		public double hungryDamage;
		public double hungryEnergyRestored;

		@Override
		protected void load() {
			triggerQuota = getDouble("triggerQuota", 0.4, 0, Integer.MAX_VALUE,
				"The relative amount of triggers available based on the number of"
					+ "available behaviors (such as different potion effects). For example,"
					+ "if there are a total of 10 behaviors available, 0.5 means there will"
					+ "be 5 trigger inputs, triggers being the inputs that can be activated"
					+ "via nanomachines."
			);
			connectorQuota = getDouble("connectorQuota", 0.2, 0, Integer.MAX_VALUE,
				"The relative number of connectors based on the number of available"
					+ "behaviors (see triggerQuota)."
			);
			maxInputs = getInt("maxInputs", 2, 1, Integer.MAX_VALUE,
				"The maximum number of inputs for each node of the \"neural network\""
					+ "nanomachines connect to. I.e. each behavior node and connector node"
					+ "may only have up to this many inputs."
			);
			maxOutputs = getInt("maxOutputs", 2, 1, Integer.MAX_VALUE,
				"The maximum number of outputs for each node (see maxInputs)."
			);
			safeInputsActive = getInt("safeInputsActive", 2, 0, Integer.MAX_VALUE,
				"How many input nodes may be active at the same time before negative"
					+ "effects are applied to the player."
			);
			maxInputsActive = getInt("maxInputsActive", 4, 0, Integer.MAX_VALUE,
				"Hard maximum number of active inputs. This is mainly to avoid people"
					+ "bumping other nanomachines' inputs to max, killing them in a matter"
					+ "of (milli)seconds."
			);
			commandDelay = getDouble("commandDelay", 1, 0, Integer.MAX_VALUE,
				"Time in seconds it takes for the nanomachines to process a command"
					+ "and send a response."
			);
			commandRange = getDouble("commandRange", 2, 0, Integer.MAX_VALUE,
				"The distance in blocks that nanomachines can communicate within. If"
					+ "a message comes from further away, it'll be ignored. When responding,"
					+ "the response will only be sent this far."
			);
			magnetRange = getDouble("magnetRange", 8, 0, Integer.MAX_VALUE,
				"Range of the item magnet behavior added for each active input."
			);
			disintegrationRange = getInt("disintegrationRange", 1, 0, Integer.MAX_VALUE,
				"Radius in blocks of the disintegration behavior for each active input."
			);
			potionWhitelist = getStringList("potionWhitelist", new String[] {
					"speed",
					"haste",
					"strength",
					"jump_boost",
					"resistance",
					"fire_resistance",
					"water_breathing",
					"night_vision",
					"absorption",

					"blindness",
					"nausea",
					"mining_fatigue",
					"instant_damage",
					"hunger",
					"slowness",
					"poison",
					"weakness",
					"wither"
				},
				"Whitelisted potions, i.e. potions that will be used for the potion"
					+ "behaviors nanomachines may trigger. This can contain strings or numbers."
					+ "In the case of strings, it has to be the internal name of the potion,"
					+ "in case of a number it has to be the potion ID. Add any potion effects"
					+ "to make use of here, since they will all be disabled by default."
			);
			hungryDamage = getInt("hungryDamage", 5, 0, Integer.MAX_VALUE,
				"How much damage the hungry behavior should deal to the player when the"
					+ "nanomachine controller runs out of energy."
			);
			hungryEnergyRestored = getInt("hungryEnergyRestored", 50, 0, Integer.MAX_VALUE,
				"How much energy the hungry behavior should restore when damaging the"
					+ "player."
			);
		}
	},
	Printer("3D printer related stuff.") {
		public int maxPrintComplexity;
		public double printRecycleRate;
		public boolean chameliumEdible;
		public int maxPrintLightLevel;
		public int printCustomRedstone;
		public int printMaterialValue;
		public int printInkValue;
		public boolean printsHaveOpacity;
		public double noclipMultiplier;

		@Override
		protected void load() {
			maxPrintComplexity = getInt("maxShapes", 24,
				"The maximum number of shape for a state of a 3D print allowed. This is"
					+ "for the individual states (off and on), so it is possible to have up to"
					+ "this many shapes *per state* (the reasoning being that only one state"
					+ "will ever be visible at a time)."
			);
			printRecycleRate = getDouble("recycleRate", 0.75,
				"How much of the material used to print a model is refunded when using"
					+ "the model to refuel a printer. This the value the original material"
					+ "cost is multiplied with, so 1 is a full refund, 0 disables the"
					+ "functionality (won't be able to put prints into the material input)."
			);
			chameliumEdible = getBoolean("chameliumEdible", true,
				"Whether Chamelium is edible or not. When eaten, it gives a (short)"
					+ "invisibility buff, and (slightly longer) blindness debuff."
			);
			maxPrintLightLevel = getInt("maxBaseLightLevel", 8, 0, 15,
				"The maximum light level a printed block can emit. This defaults to"
					+ "a value similar to that of a redstone torch, because by default the"
					+ "material prints are made of contains redstone, but no glowstone."
					+ "Prints' light level can further be boosted by crafting them with"
					+ "glowstone dust. This is merely the maximum light level that can be"
					+ "achieved directly when printing them."
			);
			printCustomRedstone = getInt("customRedstoneCost", 300, 0, Integer.MAX_VALUE,
				"The extra material cost involved for printing a model with a customized"
					+ "redstone output, i.e. something in [1, 14]."
			);
			printMaterialValue = getInt("materialValue", 2000, 0, Integer.MAX_VALUE,
				"The amount by which a printers material buffer gets filled for a single"
					+ "chamelium. Tweak this if you think printing is too cheap or expensive."
			);
			printInkValue = getInt("inkValue", 50000,
				"The amount by which a printers ink buffer gets filled for a single"
					+ "cartridge. Tweak this if you think printing is too cheap or expensive."
					+ "Note: the amount a single dye adds is this divided by 10."
			);
			printsHaveOpacity = getBoolean("printsHaveOpacity", false,
				"Whether to enable print opacity, i.e. make prints have shadows. If"
					+ "enabled, prints will have an opacity that is estimated from their"
					+ "sampled fill rate. This is disabled by default, because MC's lighting"
					+ "computation is apparently not very happy with multiple blocks with"
					+ "dynamic opacity sitting next to each other, and since all prints share"
					+ "the same block type, this can lead to weird shadows on prints. If you"
					+ "don't care about that and prefer them to be not totally shadowless,"
					+ "enable this."
			);
			noclipMultiplier = getDouble("noclipMultiplier", 2, 0, Integer.MAX_VALUE,
				"By what (linear) factor the cost of a print increases if one or both of"
					+ "its states are non-collidable (i.e. entities can move through them)."
					+ "This only influences the chamelium cost."
			);
		}
	},
	Hologram("Hologram related stuff.") {

		public double[] maxScaleByTier;
		public double[] maxTranslationByTier;
		public double rawDelay;
		public boolean light;

		@Override
		protected void load() {
			maxScaleByTier = getDoubleList("maxScale", new double[] {
					3,
					4
				},
				"This controls the maximum scales of holograms, by tier."
					+ "The size at scale 1 is 3x2x3 blocks, at scale 3 the hologram will"
					+ "span up to 9x6x9 blocks. Unlike most other `client' settings, this"
					+ "value is only used for validation *on the server*, with the effects"
					+ "only being visible on the client."
					+ "Warning: very large values may lead to rendering and/or performance"
					+ "issues due to the high view distance! Increase at your own peril.",
				"Bad number of hologram max scales, ignoring."
			);
			maxTranslationByTier = getDoubleList("maxTranslation", new double[] {
					0.25,
					0.5
				},
				"This controls the maximum translation of holograms, by tier."
					+ "The scale is in \"hologram sizes\", i.e. scale 1 allows offsetting a"
					+ "hologram once by its own size.",
				"Bad number of hologram max translations, ignoring."
			);
			rawDelay = getDouble("setRawDelay", 0.2, 0, Integer.MAX_VALUE,
				"The delay forced on computers between calls to `hologram.setRaw`, in"
					+ "seconds. Lower this if you want faster updates, raise this if you're"
					+ "worried about bandwidth use; in *normal* use-cases this will never be"
					+ "an issue. When abused, `setRaw` can be used to generate network traffic"
					+ "due to changed data being sent to clients. With the default settings,"
					+ "the *worst case* is ~30KB/s/client. Again, for normal use-cases this"
					+ "is actually barely noticeable."
			);
			light = getBoolean("emitLight", true,
				"Whether the hologram block should provide light. It'll also emit light"
					+ "when off, because having state-based light in MC is... painful."
			);
		}
	},
	Misc("Other settings that you might find useful to tweak.") {
		public int maxScreenWidth;
		public int maxScreenHeight;
		public boolean inputUsername;
		public int maxClipboard;
		public int maxNetworkPacketSize;
		public int maxNetworkPacketParts;
		public int maxOpenPorts;
		public double maxWirelessRange;
		public int rTreeMaxEntries = 10;
		public int terminalsPerServer = 4;
		public boolean updateCheck;
		public int lootProbability;
		public boolean lootRecrafting;
		public int geolyzerRange;
		public double geolyzerNoise;
		public boolean disassembleAllTheThings;
		public double disassemblerBreakChance;
		public String[] disassemblerInputBlacklist;
		public boolean hideOwnPet;
		public boolean allowItemStackInspection;
		public int[] databaseEntriesPerTier = new int[] { 9, 25, 81 };
		public double presentChance;
		public String[] assemblerBlacklist;
		public int threadPriority;
		public boolean giveManualToNewPlayers;
		public int dataCardSoftLimit;
		public int dataCardHardLimit;
		public double dataCardTimeout;
		public int serverRackRelayTier;
		public double redstoneDelay;
		public double tradingRange;
		public int mfuRange;

		@Override
		protected void load() {
			maxScreenWidth = getInt("maxScreenWidth", 8, 1, Integer.MAX_VALUE,
				"The maximum width of multi-block screens, in blocks."
					+ "See also: `maxScreenHeight`."
			);
			maxScreenHeight = getInt("maxScreenHeight", 6, 1, Integer.MAX_VALUE,
				"The maximum height of multi-block screens, in blocks. This is limited to"
					+ "avoid excessive computations for merging screens. If you really need"
					+ "bigger screens it's probably safe to bump this quite a bit before you"
					+ "notice anything, since at least incremental updates should be very"
					+ "efficient (i.e. when adding/removing a single screen)."
			);
			inputUsername = getBoolean("inputUsername", true,
				"Whether to pass along the name of the user that caused an input signals"
					+ "to the computer (mouse and keyboard signals). If you feel this breaks"
					+ "the game's immersion, disable it."
					+ "Note: also applies to the motion sensor."
			);
			maxClipboard = getInt("maxClipboard", 1024, 0, Integer.MAX_VALUE,
				"The maximum length of a string that may be pasted. This is used to limit"
					+ "the size of the data sent to the server when the user tries to paste a"
					+ "string from the clipboard (Shift+Ins on a screen with a keyboard)."
			);
			maxNetworkPacketSize = getInt("maxNetworkPacketSize", 8192, 0, Integer.MAX_VALUE,
				"The maximum size of network packets to allow sending via network cards."
					+ "This has *nothing to do* with real network traffic, it's just a limit"
					+ "for the network cards, mostly to reduce the chance of computer with a"
					+ "lot of RAM killing those with less by sending huge packets. This does"
					+ "not apply to HTTP traffic."
			);
			// Need at least 4 for nanomachine protocol. Because I can!
			maxNetworkPacketParts = getInt("maxNetworkPacketParts", 8, 4, Integer.MAX_VALUE,
				"The maximum number of \"data parts\" a network packet is allowed to have."
					+ "When sending a network message, from Lua this may look like so:"
					+ "component.modem.broadcast(port, \"first\", true, \"third\", 123)"
					+ "This limits how many arguments can be passed and are wrapped into a"
					+ "packet. This limit mostly serves as a protection for lower-tier"
					+ "computers, to avoid them getting nuked by more powerful computers."
			);
			maxOpenPorts = getInt("maxOpenPorts", 16, 0, Integer.MAX_VALUE,
				"The maximum number of ports a single network card can have opened at"
					+ "any given time."
			);
			maxWirelessRange = getDouble("maxWirelessRange", 400, 0, Integer.MAX_VALUE,
				"The maximum distance a wireless message can be sent. In other words,"
					+ "this is the maximum signal strength a wireless network card supports."
					+ "This is used to limit the search range in which to check for modems,"
					+ "which may or may not lead to performance issues for ridiculous ranges -"
					+ "like, you know, more than the loaded area."
					+ "See also: `wirelessCostPerRange`."
			);
			updateCheck = getBoolean("updateCheck", true,
				"Whether to perform an update check and informing local players and OPs"
					+ "if a new version is available (contacts Github once the first player"
					+ "joins a server / the first map in single player is opened)."
			);
			lootProbability = getInt("lootProbability", 5,
				"The probability (or rather, weighted chance) that a program disk is"
					+ "spawned as loot in a treasure chest. For reference, iron ingots have"
					+ "a value of 10, gold ingots a value of 5 and and diamonds a value of 3."
					+ "This is the chance *that* a disk is created. Which disk that will be"
					+ "is decided in an extra roll of the dice."
			);
			lootRecrafting = getBoolean("lootRecrafting", true,
				"Whether to allow loot disk cycling by crafting them with a wrench."
			);
			geolyzerRange = getInt("geolyzerRange", 32,
				"The range, in blocks, in which the Geolyzer can scan blocks. Note that"
					+ "it uses the maximum-distance, not the euclidean one, i.e. it can scan"
					+ "in a cube surrounding it with twice this value as its edge length."
			);
			geolyzerNoise = getDouble("geolyzerNoise", 2, 0, Integer.MAX_VALUE,
				"Controls how noisy results from the Geolyzer are. This is the maximum"
					+ "deviation from the original value at the maximum vertical distance"
					+ "from the geolyzer. Noise increases linearly with the vertical distance"
					+ "to the Geolyzer. So yes, on the same height, the returned value are of"
					+ "equal 'quality', regardless of the real distance. This is a performance"
					+ "trade-off."
			);
			disassembleAllTheThings = getBoolean("disassembleAllTheThings", false,
				"By default the disassembler can only be used to disassemble items from"
					+ "OpenComputers itself (or objects whitelisted via the API). If you'd"
					+ "like to allow the disassembler to work on all kinds of items, even from"
					+ "other mods, set this to true."
			);
			disassemblerBreakChance = getDouble("disassemblerBreakChance", 0.05, 0, 1,
				"The probability that an item breaks when disassembled. This chance"
					+ "applies *per extracted item*. For example, if an item was crafted from"
					+ "three other items and gets disassembled, each of those three items has"
					+ "this chance of breaking in the process."
			);
			disassemblerInputBlacklist = getStringList("disassemblerInputBlacklist", new String[] {
					"minecraft:fire"
				},
				"Names of items / blocks that are blacklisted. Recipes containing these"
					+ "as inputs will be ignored by the disassembler."
			);
			hideOwnPet = getBoolean("hideOwnSpecial", false,
				"Whether to not show your special thinger (if you have one you know it)."
			);
			allowItemStackInspection = getBoolean("allowItemStackInspection", true,
				"Allow robots to get a table representation of item stacks using the"
					+ "inventory controller upgrade? (i.e. whether the getStackInSlot method"
					+ "of said upgrade is enabled or not). Also applies to tank controller"
					+ "upgrade and it's fluid getter method."
			);
			presentChance = getDouble("presentChance", 0.05, 0, 1,
				"Probablility that at certain celebratory times crafting an OC item will"
					+ "spawn a present in the crafting player's inventory. Set to zero to"
					+ "disable."
			);
			assemblerBlacklist = getStringList("assemblerBlacklist", new String[] {},
				"List of item descriptors of assembler template base items to blacklist,"
					+ "i.e. for disabling the assembler template for. Entries must be of the"
					+ "format 'itemid@damage', were the damage is optional."
					+ "Examples: 'OpenComputers:case3', 'minecraft:stonebrick@1'"
			);
			threadPriority = getInt("threadPriority", -1,
				"Override for the worker threads' thread priority. If set to a value"
					+ "lower than 1 it will use the default value, which is half-way between"
					+ "the system minimum and normal priority. Valid values may differ between"
					+ "Java versions, but usually the minimum value (lowest priority) is 1,"
					+ "the normal value is 5 and the maximum value is 10. If a manual value is"
					+ "given it is automatically capped at the maximum."
					+ "USE THIS WITH GREAT CARE. Using a high priority for worker threads may"
					+ "avoid issues with computers timing out, but can also lead to higher"
					+ "server load. AGAIN, USE WITH CARE!"
			);
			giveManualToNewPlayers = getBoolean("giveManualToNewPlayers", true,
				"Whether to give a new player a free copy of the manual. This will only"
					+ "happen one time per game, not per world, not per death. Once. If this"
					+ "is still too much for your taste, disable it here ;-)"
			);
			dataCardSoftLimit = getInt("dataCardSoftLimit", 8192, 0, Integer.MAX_VALUE,
				"Soft limit for size of byte arrays passed to data card callbacks. If this"
					+ "limit is exceeded, a longer sleep is enforced (see dataCardTimeout)."
			);
			dataCardHardLimit = getInt("dataCardHardLimit", 1048576, 0, Integer.MAX_VALUE,
				"Hard limit for size of byte arrays passed to data card callbacks. If this"
					+ "limit is exceeded, the call fails and does nothing."
			);
			dataCardTimeout = getDouble("dataCardTimeout", 1.0, 0, Integer.MAX_VALUE,
				"Time in seconds to pause a calling machine when the soft limit for a data"
					+ "card callback is exceeded."
			);
			serverRackRelayTier = getInt("serverRackRelayTier", 1, Tier.None() + 1, Tier.Three() + 1,
				"The general upgrade tier of the relay built into server racks, i.e. how"
					+ "upgraded server racks' relaying logic is. Prior to the introduction of"
					+ "this setting (1.5.15) this was always none. This applies to all"
					+ "properties, i.e. througput, frequency and buffer size."
					+ "Valid values are: 0 = none, 1 = tier 1, 2 = tier 2, 3 = tier 3."
			) - 1;
			redstoneDelay = getDouble("redstoneDelay", 0.1, 0, Integer.MAX_VALUE,
				"Enforced delay when changing a redstone emitting component's output,"
					+ "such as the redstone card and redstone I/O block. Lowering this can"
					+ "have very negative impact on server TPS, so beware."
			);
			tradingRange = getDouble("tradingRange", 8.0, 0, Integer.MAX_VALUE,
				"The maximum range between the drone/robot and a villager for a trade to"
					+ "be performed by the trading upgrade"
			);
			mfuRange = getInt("mfuRange", 3, 0, 128,
				"Radius the MFU is able to operate in"
			);
		}
	},
	Integration("Settings for mod integration (the mod previously known as OpenComponents).") {
		public String[] modBlacklist;
		public String[] peripheralBlacklist;
		public String fakePlayerUuid;
		public String fakePlayerName;
		public GameProfile fakePlayerProfile;
		public double buildcraftCostProgrammingTable;

		@Override
		protected void load() {
			modBlacklist = getStringList("modBlacklist", new String[] {
					"Thaumcraft",
					"thaumicenergistics"
				},
				"A list of mods (by mod id) for which support should NOT be enabled. Use"
					+ "this to disable support for mods you feel should not be controllable via"
					+ "computers (such as magic related mods, which is why Thaumcraft is on this"
					+ "list by default.)"
			);
			peripheralBlacklist = getStringList("peripheralBlacklist", new String[] {
					"net.minecraft.tileentity.TileEntityCommandBlock"
				},
				"A list of tile entities by class name that should NOT be accessible via"
					+ "the Adapter block. Add blocks here that can lead to crashes or deadlocks"
					+ "(and report them, please!)"
			);
			fakePlayerUuid = getString("fakePlayerUuid", "7e506b5d-2ccb-4ac4-a249-5624925b0c67",
				"The UUID to use for the global fake player needed for some mod"
					+ "interactions."
			);
			fakePlayerName = getString("fakePlayerName", "[OpenComputers]",
				"The name to use for the global fake player needed for some mod"
					+ "interactions."
			);
			fakePlayerProfile = new GameProfile(UUID.fromString(fakePlayerUuid), fakePlayerName);
			buildcraftCostProgrammingTable = getInt("programmingTableCost", 5000, 0, Integer.MAX_VALUE,
				"Cost to convert a loot disk to another in the BuildCraft programming table."
			);
		}

	},
	IntegrationVanilla(Integration, "Vanilla integration related settings.") {
		public boolean enableInventoryDriver;
		public boolean enableTankDriver;
		public boolean enableCommandBlockDriver;
		public boolean allowItemStackNBTTags;

		@Override
		protected void load() {
			enableInventoryDriver = getBoolean("enableInventoryDriver", false,
				"Whether to enable the inventory driver. This driver allows interacting"
					+ "with inventories adjacent to adapters in a way similar to what the"
					+ "inventory controller upgrade allows when built into a robot or placed"
					+ "inside an adapter. It is therefore considered to be somewhat cheaty by"
					+ "some, and disabled by default. If you don't care about that, feel free"
					+ "to enable this driver."
			);
			enableTankDriver = getBoolean("enableTankDriver", false,
				"Whether to enable the tank driver. This driver is like the inventory"
					+ "driver, just for fluid tanks, and is disabled by default with the same"
					+ "reasoning as the inventory driver - using a tank controller upgrade in"
					+ "an adapter has pretty much the same effect."
			);
			enableCommandBlockDriver = getBoolean("enableCommandBlockDriver", false,
				"Whether to enable the command block driver. Enabling this allows"
					+ "computers to set and execute commands via command blocks next to"
					+ "adapter blocks. The commands are run using OC's general fake player."
			);
			allowItemStackNBTTags = getBoolean("allowItemStackNBTTags", false,
				"Whether to allow the item stack converter to push NBT data in"
					+ "compressed format (GZIP'ed). This can be useful for pushing this"
					+ "data back to other callbacks. However, given a sophisticated"
					+ "enough software (Lua script) it is possible to decode this data,"
					+ "and get access to things that should be considered implementation"
					+ "detail / private (mods may keep \"secret\" data in such NBT tags)."
					+ "The recommended method is to use the database component instead."
			);
		}
	},
	Debug(
		"Settings that are intended for debugging issues, not for normal use.\n"
			+ "You usually don't want to touch these unless asked to do so by a developer."
	) {
		public boolean logLuaCallbackErrors;
		public boolean forceLuaJ;
		public boolean allowUserdata;
		public boolean allowPersistence;
		public boolean limitMemory;
		public boolean forceCaseInsensitiveFS;
		public boolean logFullLibLoadErrors;
		public String forceNativeLib;
		public boolean logOpenGLErrors;
		public boolean logHexFontErrors;
		public boolean alwaysTryNative;
		public boolean debugPersistence;
		public boolean nativeInTmpDir;
		public boolean periodicallyForceLightUpdate;
		public boolean insertIdsInConverters;

		public DebugCardAccess debugCardAccess;

		public boolean registerLuaJArchitecture;
		public boolean disableLocaleChanging;

		@Override
		protected void load() {
			forceLuaJ = getBoolean("forceLuaJ", false,
				"Forces the use of the LuaJ fallback instead of the native libraries."
					+ "Use this if you have concerns using native libraries or experience"
					+ "issues with the native library."
			);
			logLuaCallbackErrors = getBoolean("logCallbackErrors", false,
				"This setting is meant for debugging errors that occur in Lua callbacks."
					+ "Per default, if an error occurs and it has a message set, only the"
					+ "message is pushed back to Lua, and that's it. If you encounter weird"
					+ "errors or are developing an addon you'll want the stacktrace for those"
					+ "errors. Enabling this setting will log them to the game log. This is"
					+ "disabled per default to avoid spamming the log with inconsequentual"
					+ "exceptions such as IllegalArgumentExceptions and the like."
			);
			allowUserdata = !getBoolean("disableUserdata", true,
				"Disable user data support. This means any otherwise supported"
					+ "userdata (implementing the Value interface) will not be pushed"
					+ "to the Lua state."
			);
			allowPersistence = !getBoolean("disablePersistence", false,
				"Disable computer state persistence. This means that computers will"
					+ "automatically be rebooted when loaded after being unloaded, instead"
					+ "of resuming with their exection (it also means the state is not even"
					+ "saved). Only relevant when using the native library."
			);
			limitMemory = !getBoolean("disableMemoryLimit", false,
				"Disable memory limit enforcement. This means Lua states can"
					+ "theoretically use as much memory as they want. Only relevant when"
					+ "using the native library."
			);
			forceCaseInsensitiveFS = !getBoolean("forceCaseInsensitiveFS", false,
				"Force the buffered file system to be case insensitive. This makes it"
					+ "impossible to have multiple files whose names only differ in their"
					+ "capitalization, which is commonly the case on Windows, for example."
					+ "This only takes effect when bufferChanges is set to true."
			);
			logFullLibLoadErrors = getBoolean("logFullNativeLibLoadErrors", false,
				"Logs the full error when a native library fails to load. This is"
					+ "disabled by default to avoid spamming the log, since libraries are"
					+ "iterated until one works, so it's very likely for some to fail. Use"
					+ "this in case all libraries fail to load even though you'd expect one"
					+ "to work."
			);
			forceNativeLib = getString("forceNativeLibWithName", "",
				"Force loading one specific library, to avoid trying to load any"
					+ "others. Use this if you get warnings in the log or are told to do"
					+ "so for debugging purposes ;-)"
			);
			logOpenGLErrors = getBoolean("logOpenGLErrors", false,
				"Used to suppress log spam for OpenGL errors on derpy drivers. I'm"
					+ "quite certain the code in the font render is valid, display list"
					+ "compatible OpenGL, but it seems to cause 'invalid operation' errors"
					+ "when executed as a display list. I'd be happy to be proven wrong,"
					+ "since it'd restore some of my trust into AMD drivers..."
			);
			alwaysTryNative = getBoolean("alwaysTryNative", false,
				"On some platforms the native library can crash the game, so there are"
					+ "a few checks in place to avoid trying to load it in those cases. This"
					+ "is Windows XP and Windows Server 2003, right. If you think it might"
					+ "work nonetheless (newer builds of Server2k3 e.g.) you might want to"
					+ "try setting this to `true`. Use this at your own risk. If the game"
					+ "crashes as a result of setting this to `true` DO NOT REPORT IT."
			);
			debugPersistence = getBoolean("verbosePersistenceErrors", false,
				"This is meant for debugging errors. Enabling this has a high impact"
					+ "on computers' save and load performance, so you should not enable"
					+ "this unless you're asked to."
			);
			logHexFontErrors = getBoolean("logHexFontErrors", false,
				"Logs information about malformed glyphs (i.e. glyphs that deviate in"
					+ "width from what wcwidth says)."
			);
			nativeInTmpDir = getBoolean("nativeInTmpDir", false,
				"Extract the native library with Lua into the system's temporary"
					+ "directory instead of the game directory (e.g. /tmp on Linux). The"
					+ "default is to extract into the game directory, to avoid issues when"
					+ "the temporary directory is mounted as noexec (meaning the lib cannot"
					+ "be loaded). There is also less of a chance of conflicts when running"
					+ "multiple servers or server and client on the same machine."
			);
			periodicallyForceLightUpdate = getBoolean("periodicallyForceLightUpdate", false,
				"Due to a bug in Minecraft's lighting code there's an issue where"
					+ "lighting does not properly update near light emitting blocks that are"
					+ "fully solid - like screens, for example. This can be annoying when"
					+ "using other blocks that dynamically change their brightness (e.g. for"
					+ "the addon mod OpenLights). Enable this to force light emitting blocks"
					+ "in oc to periodically (every two seconds) do an update. This should"
					+ "not have an overly noticeable impact on performance, but it's disabled"
					+ "by default because it is unnecessary in *most* cases."
			);
			insertIdsInConverters = getBoolean("insertIdsInConverters", false,
				"Pass along IDs of items and fluids when converting them to a table"
					+ "representation for Lua."
			);
			switch(getString("debugCardAccess", "allow",
				"Enable debug card functionality. This may also be of use for custom"
					+ "maps, so it is enabled by default. If you run a server where people"
					+ "may cheat in items but should not have op/admin-like rights, you may"
					+ "want to set this to false or `deny`. Set this to `whitelist` if you"
					+ "want to enable whitelisting of debug card users (managed by command"
					+ "/oc_debugWhitelist). This will *not* remove the card, it will just"
					+ "make all functions it provides error out."
			)) {
				case "true":
				case "allow": {
					debugCardAccess = DebugCardAccess.Allowed;
					break;
				}
				case "false":
				case "deny": {
					debugCardAccess = DebugCardAccess.Forbidden;
					break;
				}
				case "whitelist": {
					File wlFile = new File(Loader.instance().getConfigDir() + File.separator + "opencomputers" + File.separator + "debug_card_whitelist.txt");
					debugCardAccess = new DebugCardAccess.Whitelist(wlFile);
				}
				default: {// Fallback to most secure configuration
					OpenComputers.log().warn("Unknown debug card access type, falling back to `deny`. Allowed values: `allow`, `deny`, `whitelist`.");
					debugCardAccess = DebugCardAccess.Forbidden;

				}
			}
			registerLuaJArchitecture = getBoolean("registerLuaJArchitecture", false,
				"Whether to always register the LuaJ architecture - even if the native"
					+ "library is available. In that case it is possible to switch between"
					+ "the two like any other registered architecture."
			);
			disableLocaleChanging = getBoolean("disableLocaleChanging", false,
				"Prevent OC calling Lua's os.setlocale method to ensure number"
					+ "formatting is the same on all systems it is run on. Use this if you"
					+ "suspect this might mess with some other part of Java (this affects"
					+ "the native C locale)."
			);
		}

	};

	@Nullable
	private final Settings parent;
	private final String comment;

	Settings(String comment) {
		this.parent = null;
		this.comment = comment;
	}

	Settings(Settings parent, String comment) {
		this.parent = parent;
		this.comment = comment;
	}

	protected String getCategory() {
		return (this.parent != null ? this.parent.getCategory() + "." : "") + this.name();
	}

	protected String getComment() {
		return this.comment;
	}

	protected abstract void load();

	protected final boolean getBoolean(String name, boolean def, String comment) {
		return config.getBoolean(name, getCategory(), def, comment);
	}

	protected final int getInt(String name, int def, int min, int max, String comment) {
		return config.getInt(name, getCategory(), def, min, max, comment);
	}

	protected final int getInt(String name, int def, String comment) {
		return config.get(getCategory(), name, def, comment).getInt();
	}

	protected final double getDouble(String name, double def, double min, double max, String comment) {
		return config.get(getCategory(), name, def, comment, min, max).getDouble();
	}

	protected final double getDouble(String name, double def, String comment) {
		return config.get(getCategory(), name, def, comment).getDouble();
	}

	protected final String getString(String name, String def, String comment) {
		return config.getString(name, getCategory(), def, comment);
	}

	protected final String[] getStringList(String name, String[] def, String comment) {
		return config.getStringList(name, getCategory(), def, comment);
	}

	protected final double[] getDoubleList(String name, double[] def, String comment, String errorMsg) {
		double[] list = config.get(getCategory(), name, def, comment).getDoubleList();
		if(list.length != def.length) {
			OpenComputers.log().warn(errorMsg);
			return def;
		}
		return list;
	}

	protected final int[] getIntList(String name, int[] def, String comment, String errorMsg) {
		int[] list = config.get(getCategory(), name, def, comment).getIntList();
		if(list.length != def.length) {
			OpenComputers.log().warn(errorMsg);
			return def;
		}
		return list;
	}

	protected final boolean[] getBooleanList(String name, boolean[] def, String comment, String errorMsg) {
		boolean[] list = config.get(getCategory(), name, def, comment).getBooleanList();
		if(list.length != def.length) {
			OpenComputers.log().warn(errorMsg);
			return def;
		}
		return list;
	}

	// Main values

	public static Configuration config;
	public static final Settings[] Categories = values();

	public static final String resourceDomain = "opencomputers";
	public static final String namespace = "oc:";
	public static final String savePath = "opencomputers/";
	public static final String scriptPath = "/assets/" + resourceDomain + "/lua/";
	public static final int[][] screenResolutionsByTier = new int[][] { new int[] { 50, 16 }, new int[] { 80, 25 }, new int[] { 160, 50 } };
	public static final ColorDepth[] screenDepthsByTier = new ColorDepth[] { TextBuffer.ColorDepth.OneBit, TextBuffer.ColorDepth.FourBit, ColorDepth.EightBit };
	public static final int[] deviceComplexityByTier = new int[] { 12, 24, 32, 9001 };
	public static boolean rTreeDebugRenderer = false;
	public static int blockRenderId = -1;

	public static int getBasicScreenPixels() {
		return screenResolutionsByTier[0][0] * screenResolutionsByTier[0][1];
	}

	public static void initialize(File file) {
		config = new Configuration(file, OpenComputers.Version());
		config.load();

		for(Settings category : Categories) {
			config.setCategoryComment(category.getCategory(), category.getComment());
			category.load();
		}

		config.save();
	}

	public static final Pattern cidrPattern = Pattern.compile("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})(?:/(\\d{1,2}))");

	@FunctionalInterface
	public interface AddressValidator {

		boolean isValid(InetAddress inetAddress, String host);

		static AddressValidator create(final String value) {
			try {
				Matcher matcher = cidrPattern.matcher(value);
				if(matcher.find()) {
					String address = matcher.group(1);
					String prefix = matcher.group(2);
					int addr = InetAddresses.coerceToInteger(InetAddresses.forString(address));
					int mask = 0xFFFFFFFF << (32 - Integer.valueOf(prefix));
					int min = addr & mask;
					int max = min | ~mask;
					return (inetAddress, host) -> {
						if(inetAddress instanceof Inet4Address) {
							int numeric = InetAddresses.coerceToInteger(inetAddress);
							return min <= numeric && numeric <= max;
						} else {
							return true; // Can't check IPv6 addresses so we pass them.
						}
					};
				} else {
					InetAddress address = InetAddress.getByName(value);
					return (inetAddress, host) -> Objects.equals(host, value) || inetAddress == address;
				}
			} catch(Throwable t) {
				OpenComputers.log().warn("Invalid entry in internet blacklist / whitelist: " + value, t);
				return (inetAddress, host) -> true;
			}
		}
	}

	@FunctionalInterface
	private interface DebugCardAccess {

		@Nullable
		String checkAccess(@Nullable DebugCard.AccessContext ctx);

		DebugCardAccess Forbidden = (ctx) -> "debug card is disabled";
		DebugCardAccess Allowed = (ctx) -> null;

		class Whitelist implements DebugCardAccess {

			private final File noncesFile;

			public Whitelist(File noncesFile) {
				this.rng = SecureRandom.getInstance("SHA1PRNG");
				this.noncesFile = noncesFile;
				load();
			}

			private HashMap<String, String> values = new HashMap<>();
			private SecureRandom rng;

			public void save() {
				File noncesDir = noncesFile.getParentFile();
				if(!noncesDir.exists() && !noncesDir.mkdirs()) {
					throw new IOException("Cannot create nonces directory: " + noncesDir.getCanonicalPath());
				}

				try(PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(noncesFile), StandardCharsets.UTF_8), false)) {
					values.forEach((p, n) -> writer.println(p + " " + n));
				}
			}

			public void load() {
				values.clear();

				if(!noncesFile.exists()) {
					return;
				}

				BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(noncesFile), StandardCharsets.UTF_8))
				for(String line = reader.readLine(); line != null; line = reader.readLine()) {
					String[] data = line.split(" ", 2);
					if(data.length == 2) {
						values.put(data[0], data[1]);
					}
				}
			}

			private String generateNonce() {
				byte[] buf = new byte[16];
				rng.nextBytes(buf);
				return new String(Hex.encodeHex(buf, true));
			}

			public String nonce(String player) {
				return values.get(player.toLowerCase(Locale.ROOT));
			}

			public boolean isWhitelisted(String player) {
				return values.containsKey(player.toLowerCase(Locale.ROOT));
			}

			public Set<String> whitelist() {
				return values.keySet();
			}

			public void add(String player) {
				if(!values.containsKey(player.toLowerCase(Locale.ROOT))) {
					values.put(player.toLowerCase(Locale.ROOT), generateNonce());
					save();
				}
			}

			public void remove(String player) {
				if(values.remove(player.toLowerCase(Locale.ROOT)) != null) {
					save();
				}
			}

			public void invalidate(String player) {
				if(values.containsKey(player.toLowerCase(Locale.ROOT))) {
					values.put(player.toLowerCase(Locale.ROOT), generateNonce());
					save();
				}
			}

			@Nullable
			@Override
			public String checkAccess(@Nullable DebugCard.AccessContext ctx) {
				if(ctx != null) {
					String x = values.get(ctx.player().toLowerCase(Locale.ROOT));
					if(x != null) {
						return x.equals(ctx.nonce()) ? null : "debug card is invalidated, please re-bind it to yourself";
					} else {
						return "you are not whitelisted to use debug card";
					}
				} else {
					return "debug card is whitelisted, Shift+Click with it to bind card to yourself";
				}
			}
		}
	}

	//  // ----------------------------------------------------------------------- //
	//  // client
	//  val screenTextFadeStartDistance = config.getDouble("client.screenTextFadeStartDistance")
	//  val maxScreenTextRenderDistance = config.getDouble("client.maxScreenTextRenderDistance")
	//  val textLinearFiltering = config.getBoolean("client.textLinearFiltering")
	//  val textAntiAlias = config.getBoolean("client.textAntiAlias")
	//  val robotLabels = config.getBoolean("client.robotLabels")
	//  val soundVolume = config.getDouble("client.soundVolume").toFloat max 0 min 2
	//  val fontCharScale = config.getDouble("client.fontCharScale") max 0.5 min 2
	//  val hologramFadeStartDistance = config.getDouble("client.hologramFadeStartDistance") max 0
	//  val hologramRenderDistance = config.getDouble("client.hologramRenderDistance") max 0
	//  val hologramFlickerFrequency = config.getDouble("client.hologramFlickerFrequency") max 0
	//  val monochromeColor = Integer.decode(config.getString("client.monochromeColor"))
	//  val fontRenderer = config.getString("client.fontRenderer")
	//  val beepSampleRate = config.getInt("client.beepSampleRate")
	//  val beepAmplitude = config.getInt("client.beepVolume") max 0 min Byte.MaxValue
	//  val beepRadius = config.getDouble("client.beepRadius").toFloat max 1 min 32
	//  val nanomachineHudPos = Array(config.getDoubleList("client.nanomachineHudPos"): _*) match {
	//    case Array(x, y) =>
	//      (x: Double, y: Double)
	//    case _ =>
	//      OpenComputers.log.warn("Bad number of HUD coordiantes, ignoring.")
	//      (-1.0, -1.0)
	//  }
	//  val enableNanomachinePfx = config.getBoolean("client.enableNanomachinePfx")
	//
	//  // ----------------------------------------------------------------------- //
	//  // computer
	//  val threads = config.getInt("computer.threads") max 1
	//  val timeout = config.getDouble("computer.timeout") max 0
	//  val startupDelay = config.getDouble("computer.startupDelay") max 0.05
	//  val eepromSize = config.getInt("computer.eepromSize") max 0
	//  val eepromDataSize = config.getInt("computer.eepromDataSize") max 0
	//  val cpuComponentSupport = Array(config.getIntList("computer.cpuComponentCount"): _*) match {
	//    case Array(tier1, tier2, tier3) =>
	//      Array(tier1: Int, tier2: Int, tier3: Int)
	//    case _ =>
	//      OpenComputers.log.warn("Bad number of CPU component counts, ignoring.")
	//      Array(8, 12, 16)
	//  }
	//  val callBudgets = Array(config.getDoubleList("computer.callBudgets"): _*) match {
	//    case Array(tier1, tier2, tier3) =>
	//      Array(tier1: Double, tier2: Double, tier3: Double)
	//    case _ =>
	//      OpenComputers.log.warn("Bad number of call budgets, ignoring.")
	//      Array(0.5, 1.0, 1.5)
	//  }
	//  val canComputersBeOwned = config.getBoolean("computer.canComputersBeOwned")
	//  val maxUsers = config.getInt("computer.maxUsers") max 0
	//  val maxUsernameLength = config.getInt("computer.maxUsernameLength") max 0
	//  val eraseTmpOnReboot = config.getBoolean("computer.eraseTmpOnReboot")
	//  val executionDelay = config.getInt("computer.executionDelay") max 0
	//
	//  // computer.lua
	//  val allowBytecode = config.getBoolean("computer.lua.allowBytecode")
	//  val allowGC = config.getBoolean("computer.lua.allowGC")
	//  val enableLua53 = config.getBoolean("computer.lua.enableLua53")
	//  val ramSizes = Array(config.getIntList("computer.lua.ramSizes"): _*) match {
	//    case Array(tier1, tier2, tier3, tier4, tier5, tier6) =>
	//      Array(tier1: Int, tier2: Int, tier3: Int, tier4: Int, tier5: Int, tier6: Int)
	//    case _ =>
	//      OpenComputers.log.warn("Bad number of RAM sizes, ignoring.")
	//      Array(192, 256, 384, 512, 768, 1024)
	//  }
	//  val ramScaleFor64Bit = config.getDouble("computer.lua.ramScaleFor64Bit") max 1
	//  val maxTotalRam = config.getInt("computer.lua.maxTotalRam") max 0
	//
	//  // ----------------------------------------------------------------------- //
	//  // robot
	//  val allowActivateBlocks = config.getBoolean("robot.allowActivateBlocks")
	//  val allowUseItemsWithDuration = config.getBoolean("robot.allowUseItemsWithDuration")
	//  val canAttackPlayers = config.getBoolean("robot.canAttackPlayers")
	//  val limitFlightHeight = config.getInt("robot.limitFlightHeight") max 0
	//  val screwCobwebs = config.getBoolean("robot.notAfraidOfSpiders")
	//  val swingRange = config.getDouble("robot.swingRange")
	//  val useAndPlaceRange = config.getDouble("robot.useAndPlaceRange")
	//  val itemDamageRate = config.getDouble("robot.itemDamageRate") max 0 min 1
	//  val nameFormat = config.getString("robot.nameFormat")
	//  val uuidFormat = config.getString("robot.uuidFormat")
	//  val upgradeFlightHeight = Array(config.getIntList("robot.upgradeFlightHeight"): _*) match {
	//    case Array(tier1, tier2) =>
	//      Array(tier1: Int, tier2: Int)
	//    case _ =>
	//      OpenComputers.log.warn("Bad number of hover flight height counts, ignoring.")
	//      Array(64, 256)
	//  }
	//
	//  // robot.xp
	//  val baseXpToLevel = config.getDouble("robot.xp.baseValue") max 0
	//  val constantXpGrowth = config.getDouble("robot.xp.constantGrowth") max 1
	//  val exponentialXpGrowth = config.getDouble("robot.xp.exponentialGrowth") max 1
	//  val robotActionXp = config.getDouble("robot.xp.actionXp") max 0
	//  val robotExhaustionXpRate = config.getDouble("robot.xp.exhaustionXpRate") max 0
	//  val robotOreXpRate = config.getDouble("robot.xp.oreXpRate") max 0
	//  val bufferPerLevel = config.getDouble("robot.xp.bufferPerLevel") max 0
	//  val toolEfficiencyPerLevel = config.getDouble("robot.xp.toolEfficiencyPerLevel") max 0
	//  val harvestSpeedBoostPerLevel = config.getDouble("robot.xp.harvestSpeedBoostPerLevel") max 0
	//
	//  // ----------------------------------------------------------------------- //
	//  // robot.delays
	//
	//  // Note: all delays are reduced by one tick to account for the tick they are
	//  // performed in (since all actions are delegated to the server thread).
	//  val turnDelay = (config.getDouble("robot.delays.turn") - 0.06) max 0.05
	//  val moveDelay = (config.getDouble("robot.delays.move") - 0.06) max 0.05
	//  val swingDelay = (config.getDouble("robot.delays.swing") - 0.06) max 0
	//  val useDelay = (config.getDouble("robot.delays.use") - 0.06) max 0
	//  val placeDelay = (config.getDouble("robot.delays.place") - 0.06) max 0
	//  val dropDelay = (config.getDouble("robot.delays.drop") - 0.06) max 0
	//  val suckDelay = (config.getDouble("robot.delays.suck") - 0.06) max 0
	//  val harvestRatio = config.getDouble("robot.delays.harvestRatio") max 0
	//
	//  // ----------------------------------------------------------------------- //
	//  // power
	//  var is3rdPartyPowerSystemPresent = false
	//  val pureIgnorePower = config.getBoolean("power.ignorePower")
	//  lazy val ignorePower = pureIgnorePower || (!is3rdPartyPowerSystemPresent && !Mods.isPowerProvidingModPresent)
	//  val tickFrequency = config.getDouble("power.tickFrequency") max 1
	//  val chargeRateExternal = config.getDouble("power.chargerChargeRate")
	//  val chargeRateTablet = config.getDouble("power.chargerChargeRateTablet")
	//  val generatorEfficiency = config.getDouble("power.generatorEfficiency")
	//  val solarGeneratorEfficiency = config.getDouble("power.solarGeneratorEfficiency")
	//  val assemblerTickAmount = config.getDouble("power.assemblerTickAmount") max 1
	//  val disassemblerTickAmount = config.getDouble("power.disassemblerTickAmount") max 1
	//  val printerTickAmount = config.getDouble("power.printerTickAmount") max 1
	//  val powerModBlacklist = config.getStringList("power.modBlacklist")
	//
	//  // power.buffer
	//  val bufferCapacitor = config.getDouble("power.buffer.capacitor") max 0
	//  val bufferCapacitorAdjacencyBonus = config.getDouble("power.buffer.capacitorAdjacencyBonus") max 0
	//  val bufferComputer = config.getDouble("power.buffer.computer") max 0
	//  val bufferRobot = config.getDouble("power.buffer.robot") max 0
	//  val bufferConverter = config.getDouble("power.buffer.converter") max 0
	//  val bufferDistributor = config.getDouble("power.buffer.distributor") max 0
	//  val bufferCapacitorUpgrades = Array(config.getDoubleList("power.buffer.batteryUpgrades"): _*) match {
	//    case Array(tier1, tier2, tier3) =>
	//      Array(tier1: Double, tier2: Double, tier3: Double)
	//    case _ =>
	//      OpenComputers.log.warn("Bad number of battery upgrade buffer sizes, ignoring.")
	//      Array(10000.0, 15000.0, 20000.0)
	//  }
	//  val bufferTablet = config.getDouble("power.buffer.tablet") max 0
	//  val bufferAccessPoint = config.getDouble("power.buffer.accessPoint") max 0
	//  val bufferDrone = config.getDouble("power.buffer.drone") max 0
	//  val bufferMicrocontroller = config.getDouble("power.buffer.mcu") max 0
	//  val bufferHoverBoots = config.getDouble("power.buffer.hoverBoots") max 1
	//  val bufferNanomachines = config.getDouble("power.buffer.nanomachines") max 0
	//
	//  // power.cost
	//  val computerCost = config.getDouble("power.cost.computer") max 0
	//  val microcontrollerCost = config.getDouble("power.cost.microcontroller") max 0
	//  val robotCost = config.getDouble("power.cost.robot") max 0
	//  val droneCost = config.getDouble("power.cost.drone") max 0
	//  val sleepCostFactor = config.getDouble("power.cost.sleepFactor") max 0
	//  val screenCost = config.getDouble("power.cost.screen") max 0
	//  val hologramCost = config.getDouble("power.cost.hologram") max 0
	//  val hddReadCost = (config.getDouble("power.cost.hddRead") max 0) / 1024
	//  val hddWriteCost = (config.getDouble("power.cost.hddWrite") max 0) / 1024
	//  val gpuSetCost = (config.getDouble("power.cost.gpuSet") max 0) / Settings.basicScreenPixels
	//  val gpuFillCost = (config.getDouble("power.cost.gpuFill") max 0) / Settings.basicScreenPixels
	//  val gpuClearCost = (config.getDouble("power.cost.gpuClear") max 0) / Settings.basicScreenPixels
	//  val gpuCopyCost = (config.getDouble("power.cost.gpuCopy") max 0) / Settings.basicScreenPixels
	//  val robotTurnCost = config.getDouble("power.cost.robotTurn") max 0
	//  val robotMoveCost = config.getDouble("power.cost.robotMove") max 0
	//  val robotExhaustionCost = config.getDouble("power.cost.robotExhaustion") max 0
	//  val wirelessCostPerRange = config.getDouble("power.cost.wirelessCostPerRange") max 0
	//  val abstractBusPacketCost = config.getDouble("power.cost.abstractBusPacket") max 0
	//  val geolyzerScanCost = config.getDouble("power.cost.geolyzerScan") max 0
	//  val robotBaseCost = config.getDouble("power.cost.robotAssemblyBase") max 0
	//  val robotComplexityCost = config.getDouble("power.cost.robotAssemblyComplexity") max 0
	//  val microcontrollerBaseCost = config.getDouble("power.cost.microcontrollerAssemblyBase") max 0
	//  val microcontrollerComplexityCost = config.getDouble("power.cost.microcontrollerAssemblyComplexity") max 0
	//  val tabletBaseCost = config.getDouble("power.cost.tabletAssemblyBase") max 0
	//  val tabletComplexityCost = config.getDouble("power.cost.tabletAssemblyComplexity") max 0
	//  val droneBaseCost = config.getDouble("power.cost.droneAssemblyBase") max 0
	//  val droneComplexityCost = config.getDouble("power.cost.droneAssemblyComplexity") max 0
	//  val disassemblerItemCost = config.getDouble("power.cost.disassemblerPerItem") max 0
	//  val chunkloaderCost = config.getDouble("power.cost.chunkloaderCost") max 0
	//  val pistonCost = config.getDouble("power.cost.pistonPush") max 0
	//  val eepromWriteCost = config.getDouble("power.cost.eepromWrite") max 0
	//  val printCost = config.getDouble("power.cost.printerModel") max 0
	//  val hoverBootJump = config.getDouble("power.cost.hoverBootJump") max 0
	//  val hoverBootAbsorb = config.getDouble("power.cost.hoverBootAbsorb") max 0
	//  val hoverBootMove = config.getDouble("power.cost.hoverBootMove") max 0
	//  val dataCardTrivial = config.getDouble("power.cost.dataCardTrivial") max 0
	//  val dataCardTrivialByte = config.getDouble("power.cost.dataCardTrivialByte") max 0
	//  val dataCardSimple = config.getDouble("power.cost.dataCardSimple") max 0
	//  val dataCardSimpleByte = config.getDouble("power.cost.dataCardSimpleByte") max 0
	//  val dataCardComplex = config.getDouble("power.cost.dataCardComplex") max 0
	//  val dataCardComplexByte = config.getDouble("power.cost.dataCardComplexByte") max 0
	//  val dataCardAsymmetric = config.getDouble("power.cost.dataCardAsymmetric") max 0
	//  val transposerCost = config.getDouble("power.cost.transposer") max 0
	//  val nanomachineCost = config.getDouble("power.cost.nanomachineInput") max 0
	//  val nanomachineReconfigureCost = config.getDouble("power.cost.nanomachinesReconfigure") max 0
	//  val mfuCost = config.getDouble("power.cost.mfuRelay") max 0
	//
	//  // power.rate
	//  val accessPointRate = config.getDouble("power.rate.accessPoint") max 0
	//  val assemblerRate = config.getDouble("power.rate.assembler") max 0
	//  val caseRate = (Array(config.getDoubleList("power.rate.case"): _*) match {
	//    case Array(tier1, tier2, tier3) =>
	//      Array(tier1: Double, tier2: Double, tier3: Double)
	//    case _ =>
	//      OpenComputers.log.warn("Bad number of computer case conversion rates, ignoring.")
	//      Array(5.0, 10.0, 20.0)
	//  }) ++ Array(9001.0)
	//  // Creative case.
	//  val chargerRate = config.getDouble("power.rate.charger") max 0
	//  val disassemblerRate = config.getDouble("power.rate.disassembler") max 0
	//  val powerConverterRate = config.getDouble("power.rate.powerConverter") max 0
	//  val serverRackRate = config.getDouble("power.rate.serverRack") max 0
	//
	//  // power.value
	//  private val valueAppliedEnergistics2 = config.getDouble("power.value.AppliedEnergistics2")
	//  private val valueFactorization = config.getDouble("power.value.Factorization")
	//  private val valueGalacticraft = config.getDouble("power.value.Galacticraft")
	//  private val valueIndustrialCraft2 = config.getDouble("power.value.IndustrialCraft2")
	//  private val valueMekanism = config.getDouble("power.value.Mekanism")
	//  private val valuePowerAdvantage = config.getDouble("power.value.PowerAdvantage")
	//  private val valueRedstoneFlux = config.getDouble("power.value.RedstoneFlux")
	//  private val valueRotaryCraft = config.getDouble("power.value.RotaryCraft") / 11256.0
	//
	//  private val valueInternal = 1000
	//
	//  val ratioAppliedEnergistics2 = valueAppliedEnergistics2 / valueInternal
	//  val ratioFactorization = valueFactorization / valueInternal
	//  val ratioGalacticraft = valueGalacticraft / valueInternal
	//  val ratioIndustrialCraft2 = valueIndustrialCraft2 / valueInternal
	//  val ratioMekanism = valueMekanism / valueInternal
	//  val ratioPowerAdvantage = valuePowerAdvantage / valueInternal
	//  val ratioRedstoneFlux = valueRedstoneFlux / valueInternal
	//  val ratioRotaryCraft = valueRotaryCraft / valueInternal
	//
	//  // ----------------------------------------------------------------------- //
	//  // filesystem
	//  val fileCost = config.getInt("filesystem.fileCost") max 0
	//  val bufferChanges = config.getBoolean("filesystem.bufferChanges")
	//  val hddSizes = Array(config.getIntList("filesystem.hddSizes"): _*) match {
	//    case Array(tier1, tier2, tier3) =>
	//      Array(tier1: Int, tier2: Int, tier3: Int)
	//    case _ =>
	//      OpenComputers.log.warn("Bad number of HDD sizes, ignoring.")
	//      Array(1024, 2048, 4096)
	//  }
	//  val hddPlatterCounts = Array(config.getIntList("filesystem.hddPlatterCounts"): _*) match {
	//    case Array(tier1, tier2, tier3) =>
	//      Array(tier1: Int, tier2: Int, tier3: Int)
	//    case _ =>
	//      OpenComputers.log.warn("Bad number of HDD platter counts, ignoring.")
	//      Array(2, 4, 6)
	//  }
	//  val floppySize = config.getInt("filesystem.floppySize") max 0
	//  val tmpSize = config.getInt("filesystem.tmpSize") max 0
	//  val maxHandles = config.getInt("filesystem.maxHandles") max 0
	//  val maxReadBuffer = config.getInt("filesystem.maxReadBuffer") max 0
	//  val sectorSeekThreshold = config.getInt("filesystem.sectorSeekThreshold")
	//  val sectorSeekTime = config.getDouble("filesystem.sectorSeekTime")
	//
	//  // ----------------------------------------------------------------------- //
	//  // internet
	//  val httpEnabled = config.getBoolean("internet.enableHttp")
	//  val httpHeadersEnabled = config.getBoolean("internet.enableHttpHeaders")
	//  val tcpEnabled = config.getBoolean("internet.enableTcp")
	//  val httpHostBlacklist = Array(config.getStringList("internet.blacklist").map(new Settings.AddressValidator(_)): _*)
	//  val httpHostWhitelist = Array(config.getStringList("internet.whitelist").map(new Settings.AddressValidator(_)): _*)
	//  val httpTimeout = (config.getInt("internet.requestTimeout") max 0) * 1000
	//  val maxConnections = config.getInt("internet.maxTcpConnections") max 0
	//  val internetThreads = config.getInt("internet.threads") max 1
	//
	//  // ----------------------------------------------------------------------- //
	//  // switch
	//  val switchDefaultMaxQueueSize = config.getInt("switch.defaultMaxQueueSize") max 1
	//  val switchQueueSizeUpgrade = config.getInt("switch.queueSizeUpgrade") max 0
	//  val switchDefaultRelayDelay = config.getInt("switch.defaultRelayDelay") max 1
	//  val switchRelayDelayUpgrade = config.getDouble("switch.relayDelayUpgrade") max 0
	//  val switchDefaultRelayAmount = config.getInt("switch.defaultRelayAmount") max 1
	//  val switchRelayAmountUpgrade = config.getInt("switch.relayAmountUpgrade") max 0
	//
	//  // ----------------------------------------------------------------------- //
	//  // hologram
	//  val hologramMaxScaleByTier = Array(config.getDoubleList("hologram.maxScale"): _*) match {
	//    case Array(tier1, tier2) =>
	//      Array((tier1: Double) max 1.0, (tier2: Double) max 1.0)
	//    case _ =>
	//      OpenComputers.log.warn("Bad number of hologram max scales, ignoring.")
	//      Array(3.0, 4.0)
	//  }
	//  val hologramMaxTranslationByTier = Array(config.getDoubleList("hologram.maxTranslation"): _*) match {
	//    case Array(tier1, tier2) =>
	//      Array((tier1: Double) max 0.0, (tier2: Double) max 0.0)
	//    case _ =>
	//      OpenComputers.log.warn("Bad number of hologram max translations, ignoring.")
	//      Array(0.25, 0.5)
	//  }
	//  val hologramSetRawDelay = config.getDouble("hologram.setRawDelay") max 0
	//  val hologramLight = config.getBoolean("hologram.emitLight")
	//
	//  // ----------------------------------------------------------------------- //
	//  // misc
	//  val maxScreenWidth = config.getInt("misc.maxScreenWidth") max 1
	//  val maxScreenHeight = config.getInt("misc.maxScreenHeight") max 1
	//  val inputUsername = config.getBoolean("misc.inputUsername")
	//  val maxClipboard = config.getInt("misc.maxClipboard") max 0
	//  val maxNetworkPacketSize = config.getInt("misc.maxNetworkPacketSize") max 0
	//  // Need at least 4 for nanomachine protocol. Because I can!
	//  val maxNetworkPacketParts = config.getInt("misc.maxNetworkPacketParts") max 4
	//  val maxOpenPorts = config.getInt("misc.maxOpenPorts") max 0
	//  val maxWirelessRange = config.getDouble("misc.maxWirelessRange") max 0
	//  val rTreeMaxEntries = 10
	//  val terminalsPerServer = 4
	//  val updateCheck = config.getBoolean("misc.updateCheck")
	//  val lootProbability = config.getInt("misc.lootProbability")
	//  val lootRecrafting = config.getBoolean("misc.lootRecrafting")
	//  val geolyzerRange = config.getInt("misc.geolyzerRange")
	//  val geolyzerNoise = config.getDouble("misc.geolyzerNoise").toFloat max 0
	//  val disassembleAllTheThings = config.getBoolean("misc.disassembleAllTheThings")
	//  val disassemblerBreakChance = config.getDouble("misc.disassemblerBreakChance") max 0 min 1
	//  val disassemblerInputBlacklist = config.getStringList("misc.disassemblerInputBlacklist")
	//  val hideOwnPet = config.getBoolean("misc.hideOwnSpecial")
	//  val allowItemStackInspection = config.getBoolean("misc.allowItemStackInspection")
	//  val databaseEntriesPerTier = Array(9, 25, 81)
	//  // Not configurable because of GUI design.
	//  val presentChance = config.getDouble("misc.presentChance") max 0 min 1
	//  val assemblerBlacklist = config.getStringList("misc.assemblerBlacklist")
	//  val threadPriority = config.getInt("misc.threadPriority")
	//  val giveManualToNewPlayers = config.getBoolean("misc.giveManualToNewPlayers")
	//  val dataCardSoftLimit = config.getInt("misc.dataCardSoftLimit") max 0
	//  val dataCardHardLimit = config.getInt("misc.dataCardHardLimit") max 0
	//  val dataCardTimeout = config.getDouble("misc.dataCardTimeout") max 0
	//  val serverRackSwitchTier = (config.getInt("misc.serverRackSwitchTier") - 1) max Tier.None min Tier.Three
	//  val redstoneDelay = config.getDouble("misc.redstoneDelay") max 0
	//  val tradingRange = config.getDouble("misc.tradingRange") max 0
	//  val mfuRange = config.getInt("misc.mfuRange") max 0 min 128
	//
	//  // ----------------------------------------------------------------------- //
	//  // nanomachines
	//  val nanomachineTriggerQuota = config.getDouble("nanomachines.triggerQuota") max 0
	//  val nanomachineConnectorQuota = config.getDouble("nanomachines.connectorQuota") max 0
	//  val nanomachineMaxInputs = config.getInt("nanomachines.maxInputs") max 1
	//  val nanomachineMaxOutputs = config.getInt("nanomachines.maxOutputs") max 1
	//  val nanomachinesSafeInputsActive = config.getInt("nanomachines.safeInputsActive") max 0
	//  val nanomachinesMaxInputsActive = config.getInt("nanomachines.maxInputsActive") max 0
	//  val nanomachinesCommandDelay = config.getDouble("nanomachines.commandDelay") max 0
	//  val nanomachinesCommandRange = config.getDouble("nanomachines.commandRange") max 0
	//  val nanomachineMagnetRange = config.getDouble("nanomachines.magnetRange") max 0
	//  val nanomachineDisintegrationRange = config.getInt("nanomachines.disintegrationRange") max 0
	//  val nanomachinePotionWhitelist = config.getAnyRefList("nanomachines.potionWhitelist")
	//  val nanomachinesHungryDamage = config.getDouble("nanomachines.hungryDamage").toFloat max 0
	//  val nanomachinesHungryEnergyRestored = config.getDouble("nanomachines.hungryEnergyRestored") max 0
	//
	//  // ----------------------------------------------------------------------- //
	//  // printer
	//  val maxPrintComplexity = config.getInt("printer.maxShapes")
	//  val printRecycleRate = config.getDouble("printer.recycleRate")
	//  val chameliumEdible = config.getBoolean("printer.chameliumEdible")
	//  val maxPrintLightLevel = config.getInt("printer.maxBaseLightLevel") max 0 min 15
	//  val printCustomRedstone = config.getInt("printer.customRedstoneCost") max 0
	//  val printMaterialValue = config.getInt("printer.materialValue") max 0
	//  val printInkValue = config.getInt("printer.inkValue") max 0
	//  val printsHaveOpacity = config.getBoolean("printer.printsHaveOpacity")
	//  val noclipMultiplier = config.getDouble("printer.noclipMultiplier") max 0
	//
	//  // ----------------------------------------------------------------------- //
	//  // integration
	//  val modBlacklist = config.getStringList("integration.modBlacklist")
	//  val peripheralBlacklist = config.getStringList("integration.peripheralBlacklist")
	//  val fakePlayerUuid = config.getString("integration.fakePlayerUuid")
	//  val fakePlayerName = config.getString("integration.fakePlayerName")
	//  val fakePlayerProfile = new GameProfile(UUID.fromString(fakePlayerUuid), fakePlayerName)
	//
	//  // integration.vanilla
	//  val enableInventoryDriver = config.getBoolean("integration.vanilla.enableInventoryDriver")
	//  val enableTankDriver = config.getBoolean("integration.vanilla.enableTankDriver")
	//  val enableCommandBlockDriver = config.getBoolean("integration.vanilla.enableCommandBlockDriver")
	//  val allowItemStackNBTTags = config.getBoolean("integration.vanilla.allowItemStackNBTTags")
	//
	//  // integration.buildcraft
	//  val costProgrammingTable = config.getDouble("integration.buildcraft.programmingTableCost") max 0
	//
	//  // ----------------------------------------------------------------------- //
	//  // debug
	//  val logLuaCallbackErrors = config.getBoolean("debug.logCallbackErrors")
	//  val forceLuaJ = config.getBoolean("debug.forceLuaJ")
	//  val allowUserdata = !config.getBoolean("debug.disableUserdata")
	//  val allowPersistence = !config.getBoolean("debug.disablePersistence")
	//  val limitMemory = !config.getBoolean("debug.disableMemoryLimit")
	//  val forceCaseInsensitive = config.getBoolean("debug.forceCaseInsensitiveFS")
	//  val logFullLibLoadErrors = config.getBoolean("debug.logFullNativeLibLoadErrors")
	//  val forceNativeLib = config.getString("debug.forceNativeLibWithName")
	//  val logOpenGLErrors = config.getBoolean("debug.logOpenGLErrors")
	//  val logHexFontErrors = config.getBoolean("debug.logHexFontErrors")
	//  val alwaysTryNative = config.getBoolean("debug.alwaysTryNative")
	//  val debugPersistence = config.getBoolean("debug.verbosePersistenceErrors")
	//  val nativeInTmpDir = config.getBoolean("debug.nativeInTmpDir")
	//  val periodicallyForceLightUpdate = config.getBoolean("debug.periodicallyForceLightUpdate")
	//  val insertIdsInConverters = config.getBoolean("debug.insertIdsInConverters")
	//
	//  val debugCardAccess = config.getValue("debug.debugCardAccess").unwrapped() match {
	//    case "true" | "allow" | java.lang.Boolean.TRUE => DebugCardAccess.Allowed
	//    case "false" | "deny" | java.lang.Boolean.FALSE => DebugCardAccess.Forbidden
	//    case "whitelist" =>
	//      val wlFile = new File(Loader.instance.getConfigDir + File.separator + "opencomputers" + File.separator +
	//                              "debug_card_whitelist.txt")
	//
	//      DebugCardAccess.Whitelist(wlFile)
	//
	//    case _ => // Fallback to most secure configuration
	//      OpenComputers.log.warn("Unknown debug card access type, falling back to `deny`. Allowed values: `allow`, `deny`, `whitelist`.")
	//      DebugCardAccess.Forbidden
	//  }
	//
	//  val registerLuaJArchitecture = config.getBoolean("debug.registerLuaJArchitecture")
	//  val disableLocaleChanging = config.getBoolean("debug.disableLocaleChanging")
}

//object Settings {
//  val resourceDomain = "opencomputers"
//  val namespace = "oc:"
//  val savePath = "opencomputers/"
//  val scriptPath = "/assets/" + resourceDomain + "/lua/"
//  val screenResolutionsByTier = Array((50, 16), (80, 25), (160, 50))
//  val screenDepthsByTier = Array(api.internal.TextBuffer.ColorDepth.OneBit, api.internal.TextBuffer.ColorDepth.FourBit, api.internal.TextBuffer.ColorDepth.EightBit)
//  val deviceComplexityByTier = Array(12, 24, 32, 9001)
//  var rTreeDebugRenderer = false
//  var blockRenderId = -1
//
//  def basicScreenPixels = screenResolutionsByTier(0)._1 * screenResolutionsByTier(0)._2
//
//  private var settings: Settings = _
//
//  def get = settings
//
//  def load(file: File) = {
//    import scala.compat.Platform.EOL
//    // typesafe config's internal method for loading the reference.conf file
//    // seems to fail on some systems (as does their parseResource method), so
//    // we'll have to load the default config manually. This was reported on the
//    // Minecraft Forums, I could not reproduce the issue, but this version has
//    // reportedly fixed the problem.
//    val defaults = {
//      val in = classOf[Settings].getResourceAsStream("/application.conf")
//      val config = Source.fromInputStream(in)(Codec.UTF8).getLines().mkString("", EOL, EOL)
//      in.close()
//      ConfigFactory.parseString(config)
//    }
//    val config =
//      try {
//        val plain = Source.fromFile(file)(Codec.UTF8).getLines().mkString("", EOL, EOL)
//        val config = patchConfig(ConfigFactory.parseString(plain), defaults).withFallback(defaults)
//        settings = new Settings(config.getConfig("opencomputers"))
//        config
//      }
//      catch {
//        case e: Throwable =>
//          if (file.exists()) {
//            OpenComputers.log.warn("Failed loading config, using defaults.", e)
//          }
//          settings = new Settings(defaults.getConfig("opencomputers"))
//          defaults
//      }
//    try {
//      val renderSettings = ConfigRenderOptions.defaults.setJson(false).setOriginComments(false)
//      val nl = sys.props("line.separator")
//      val nle = StringEscapeUtils.escapeJava(nl)
//      file.getParentFile.mkdirs()
//      val out = new PrintWriter(file)
//      out.write(config.root.render(renderSettings).lines.
//        // Indent two spaces instead of four.
//        map(line => """^(\s*)""".r.replaceAllIn(line, m => Regex.quoteReplacement(m.group(1).replace("  ", " ")))).
//        // Finalize the string.
//        filter(_ != "").mkString(nl).
//        // Newline after values.
//        replaceAll(s"((?:\\s*#.*$nle)(?:\\s*[^#\\s].*$nle)+)", "$1" + nl))
//      out.close()
//    }
//    catch {
//      case e: Throwable =>
//        OpenComputers.log.warn("Failed saving config.", e)
//    }
//  }
//
//  // Usage: VersionRange.createFromVersionSpec("[0.0,1.5)") -> Array("computer.ramSizes") will
//  // re-set the value of `computer.ramSizes` if a config saved with a version < 1.5 is loaded.
//  private val configPatches = Array[(VersionRange, Array[String])](
//    // Upgrading to version 1.5.20, changed relay delay default.
//    VersionRange.createFromVersionSpec("[0.0, 1.5.20)") -> Array(
//      "switch.relayDelayUpgrade"
//    ),
//    // Potion whitelist was fixed in 1.6.2.
//    VersionRange.createFromVersionSpec("[0.0, 1.6.2)") -> Array(
//      "nanomachines.potionWhitelist"
//    )
//  )
//
//  // Checks the config version (i.e. the version of the mod the config was
//  // created by) against the current version to see if some hard changes
//  // were made. If so, the new default values are copied over.
//  private def patchConfig(config: Config, defaults: Config) = {
//    val mod = Loader.instance.activeModContainer
//    val prefix = "opencomputers."
//    val configVersion = new DefaultArtifactVersion(if (config.hasPath(prefix + "version")) config.getString(prefix + "version") else "0.0.0")
//    var patched = config
//    if (configVersion.compareTo(mod.getProcessedVersion) != 0) {
//      OpenComputers.log.info(s"Updating config from version '${configVersion.getVersionString}' to '${defaults.getString(prefix + "version")}'.")
//      patched = patched.withValue(prefix + "version", defaults.getValue(prefix + "version"))
//      for ((version, paths) <- configPatches if version.containsVersion(configVersion)) {
//        for (path <- paths) {
//          val fullPath = prefix + path
//          OpenComputers.log.info(s"Updating setting '$fullPath'. ")
//          if (defaults.hasPath(fullPath)) {
//            patched = patched.withValue(fullPath, defaults.getValue(fullPath))
//          }
//          else {
//            patched = patched.withoutPath(fullPath)
//          }
//        }
//      }
//    }
//    patched
//  }
//
//  val cidrPattern = """(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})(?:/(\d{1,2}))""".r
//
//  class AddressValidator(val value: String) {
//    val validator = try cidrPattern.findFirstIn(value) match {
//      case Some(cidrPattern(address, prefix)) =>
//        val addr = InetAddresses.coerceToInteger(InetAddresses.forString(address))
//        val mask = 0xFFFFFFFF << (32 - prefix.toInt)
//        val min = addr & mask
//        val max = min | ~mask
//        (inetAddress: InetAddress, host: String) => inetAddress match {
//          case v4: Inet4Address =>
//            val numeric = InetAddresses.coerceToInteger(v4)
//            min <= numeric && numeric <= max
//          case _ => true // Can't check IPv6 addresses so we pass them.
//        }
//      case _ =>
//        val address = InetAddress.getByName(value)
//        (inetAddress: InetAddress, host: String) => host == value || inetAddress == address
//    } catch {
//      case t: Throwable =>
//        OpenComputers.log.warn("Invalid entry in internet blacklist / whitelist: " + value, t)
//        (inetAddress: InetAddress, host: String) => true
//    }
//
//    def apply(inetAddress: InetAddress, host: String) = validator(inetAddress, host)
//  }
//
//  sealed trait DebugCardAccess {
//    def checkAccess(ctx: Option[DebugCard.AccessContext]): Option[String]
//  }
//
//  object DebugCardAccess {
//    case object Forbidden extends DebugCardAccess {
//      override def checkAccess(ctx: Option[AccessContext]): Option[String] =
//        Some("debug card is disabled")
//    }
//
//    case object Allowed extends DebugCardAccess {
//      override def checkAccess(ctx: Option[AccessContext]): Option[String] = None
//    }
//
//    case class Whitelist(noncesFile: File) extends DebugCardAccess {
//      private val values = mutable.Map.empty[String, String]
//      private val rng = SecureRandom.getInstance("SHA1PRNG")
//
//      load()
//
//      def save(): Unit = {
//        val noncesDir = noncesFile.getParentFile
//        if (!noncesDir.exists() && !noncesDir.mkdirs())
//          throw new IOException(s"Cannot create nonces directory: ${noncesDir.getCanonicalPath}")
//
//        val writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(noncesFile), StandardCharsets.UTF_8), false)
//        try {
//          for ((p, n) <- values)
//            writer.println(s"$p $n")
//        } finally writer.close()
//      }
//
//      def load(): Unit = {
//        values.clear()
//
//        if (!noncesFile.exists())
//          return
//
//        val reader = new BufferedReader(new InputStreamReader(new FileInputStream(noncesFile), StandardCharsets.UTF_8))
//        Iterator.continually(reader.readLine())
//          .takeWhile(_ != null)
//          .map(_.split(" ", 2))
//          .flatMap {
//            case Array(p, n) => Seq(p -> n)
//            case _ => Nil
//          }.foreach(values += _)
//      }
//
//      private def generateNonce(): String = {
//        val buf = new Array[Byte](16)
//        rng.nextBytes(buf)
//        new String(Hex.encodeHex(buf, true))
//      }
//
//      def nonce(player: String) = values.get(player.toLowerCase)
//
//      def isWhitelisted(player: String) = values.contains(player.toLowerCase)
//
//      def whitelist: collection.Set[String] = values.keySet
//
//      def add(player: String): Unit = {
//        if (!values.contains(player.toLowerCase)) {
//          values.put(player.toLowerCase, generateNonce())
//          save()
//        }
//      }
//
//      def remove(player: String): Unit = {
//        if (values.remove(player.toLowerCase).isDefined)
//          save()
//      }
//
//      def invalidate(player: String): Unit = {
//        if (values.contains(player.toLowerCase)) {
//          values.put(player.toLowerCase, generateNonce())
//          save()
//        }
//      }
//
//      def checkAccess(ctxOpt: Option[DebugCard.AccessContext]): Option[String] = ctxOpt match {
//        case Some(ctx) => values.get(ctx.player.toLowerCase) match {
//          case Some(x) =>
//            if (x == ctx.nonce) None
//            else Some("debug card is invalidated, please re-bind it to yourself")
//          case None => Some("you are not whitelisted to use debug card")
//        }
//
//        case None => Some("debug card is whitelisted, Shift+Click with it to bind card to yourself")
//      }
//    }
//  }
//}
