package li.cil.oc;

import com.mojang.authlib.GameProfile;
import li.cil.oc.common.Tier;
import li.cil.oc.integration.Mods;
import li.cil.oc.util.AddressValidator;
import li.cil.oc.util.DebugCardAccess;
import net.minecraft.tileentity.TileEntityCommandBlock;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings("WeakerAccess")
public final class Settings {
    public static final class Client extends Category {
        public static double screenTextFadeStartDistance = 15;
        public static double maxScreenTextRenderDistance = 20;
        public static boolean textLinearFiltering = false;
        public static boolean textAntiAlias = true;
        public static boolean robotLabels = true;
        public static double soundVolume = 1;
        public static double fontCharScale = 1.01;
        public static double hologramFadeStartDistance = 48;
        public static double hologramRenderDistance = 64;
        public static double hologramFlickerFrequency = 0.025;
        public static int monochromeColor = 0xFFFFFF;
        public static int beepSampleRate = 44100;
        public static int beepVolume = 32;
        public static double beepRadius = 16;
        public static double[] nanomachineHudPos = new double[]{-1, -1};
        public static boolean enableNanomachinePfx = true;

        Client() {
            super("Client side settings, presentation and performance related stuff.");
        }

        @Override
        void load(final ConfigAccess config) {
            screenTextFadeStartDistance = config.getDouble("screenTextFadeStartDistance", screenTextFadeStartDistance, 0, 64,
                    "The distance at which to start fading out the text on screens. This is\n"
                            + "purely cosmetic, to avoid text disappearing instantly when moving too\n"
                            + "far away from a screen. This should have no measurable impact on\n"
                            + "performance. Note that this needs OpenGL 1.4 to work, otherwise text\n"
                            + "will always just instantly disappear when moving away from the screen\n"
                            + "displaying it.");
            maxScreenTextRenderDistance = config.getDouble("maxScreenTextRenderDistance", maxScreenTextRenderDistance, 0, 64,
                    "The maximum distance at which to render text on screens. Rendering text\n"
                            + "can be pretty expensive, so if you have a lot of screens you'll want to\n"
                            + "avoid huge numbers here. Note that this setting is client-sided, and\n"
                            + "only has an impact on render performance on clients.");
            textLinearFiltering = config.getBoolean("textLinearFiltering", textLinearFiltering,
                    "Whether to apply linear filtering for text displayed on screens when the\n"
                            + "screen has to be scaled down - i.e. the text is rendered at a resolution\n"
                            + "lower than their native one, e.g. when the GUI scale is less than one or\n"
                            + "when looking at a far away screen. This leads to smoother text for\n"
                            + "scaled down text but results in characters not perfectly connecting\n"
                            + "anymore (for example for box drawing characters. Look it up on\n"
                            + "Wikipedia.)");
            textAntiAlias = config.getBoolean("textAntiAlias", textAntiAlias,
                    "If you prefer the text on the screens to be aliased (you know, *not*\n"
                            + "anti-aliased / smoothed) turn this option off.");
            robotLabels = config.getBoolean("robotLabels", robotLabels,
                    "Render robots' names as a label above them when near them"
            );
            soundVolume = config.getDouble("soundVolume", soundVolume, 0, 2,
                    "The volume multiplier applied to sounds from this mod like the computer\n"
                            + "running noise. Disable sounds by setting this to zero."
            );
            fontCharScale = config.getDouble("fontCharScale", fontCharScale, 0.5, 2,
                    "This is the scaling of the individual chars rendered on screens. This\n"
                            + "is set to slightly overscale per default, to avoid gaps between fully\n"
                            + "filled chars to appear (i.e. the block symbol that is used for cursor\n"
                            + "blinking for example) on less accurate hardware."
            );
            hologramFadeStartDistance = config.getDouble("hologramFadeStartDistance", hologramFadeStartDistance, 0, 64,
                    "The distance at which to start fading out the hologram (as with\n"
                            + "hologramRenderDistance). This is purely cosmetic, to avoid image\n"
                            + "disappearing instantly when moving too far away from a projector.\n"
                            + "It does not affect performance. Holograms are transparent anyway."
            );
            hologramRenderDistance = config.getDouble("hologramRenderDistance", hologramRenderDistance, 0, 64,
                    "The maximum render distance of a hologram projected by a highest tier\n"
                            + "hologram projector when at maximum scale. Render distance is scaled\n"
                            + "down with the actual scale of the hologram."
            );
            hologramFlickerFrequency = config.getDouble("hologramFlickerFrequency", hologramFlickerFrequency, 0, 1,
                    "This controls how often holograms 'flicker'. This is the chance that it\n"
                            + "flickers for *each frame*, meaning if you're running at high FPS you\n"
                            + "may want to lower this a bit, to avoid it flickering too much."
            );
            monochromeColor = Integer.decode(config.getString("monochromeColor", String.format("0x%06X", monochromeColor),
                    "The color of monochrome text (i.e. displayed when in 1-bit color depth,\n"
                            + "e.g. tier one screens / GPUs, or higher tier set to 1-bit color depth).\n"
                            + "Defaults to white, feel free to make it some other color, tho!"));

            beepSampleRate = config.getInt("beepSampleRate", beepSampleRate, 8000, 44100,
                    "The sample rate used for generating beeps of computers' internal\n"
                            + "speakers. Use custom values at your own responsibility here; if it\n"
                            + "breaks OC you'll get no support. Some potentially reasonable\n"
                            + "lower values are 16000 or even 8000 (which was the old default, but\n"
                            + "leads to artifacting on certain frequencies)."
            );
            beepVolume = config.getInt("beepVolume", beepVolume, 0, Byte.MAX_VALUE,
                    "The base volume of beeps generated by computers. This may be in a\n"
                            + "range of [0, 127], where 0 means mute (the sound will not even be\n"
                            + "generated), and 127 means maximum amplitude / volume."
            );
            beepRadius = config.getDouble("beepRadius", beepRadius, 1, 32,
                    "The radius in which computer beeps can be heard."
            );
            nanomachineHudPos = config.getDoubleList("nanomachineHudPos", nanomachineHudPos,
                    "Position of the power indicator for nanomachines, by default left to the\n"
                            + "player's health, specified by negative values. Values in [0, 1) will be\n"
                            + "treated as relative positions, values in [1, inf) will be treated as\n"
                            + "absolute positions.",
                    "Bad number of HUD coordinates, ignoring.");
            enableNanomachinePfx = config.getBoolean("enableNanomachinePfx", enableNanomachinePfx,
                    "Whether to emit particle effects around players via nanomachines. This\n"
                            + "includes the basic particles giving a rough indication of the current\n"
                            + "power level of the nanomachines as well as particles emitted by the\n"
                            + "particle effect behaviors."
            );
        }
    }

    public static final class Computer extends Category {
        public static int threads = 4;
        public static double timeout = 5;
        public static double startupDelay = 0.25;
        public static int eepromSize = 4096;
        public static int eepromDataSize = 256;
        public static int[] cpuComponentCount = new int[]{8, 12, 16};
        public static double[] callBudgets = new double[]{0.5, 1.0, 1.5};
        public static boolean eraseTmpOnReboot = false;
        public static int executionDelay = 12;

        Computer() {
            super("Computer related settings, concerns server performance and security.");
        }

        @Override
        void load(final ConfigAccess config) {
            threads = config.getInt("threads", threads, 1, 64,
                    "The overall number of threads to use to drive computers. Whenever a\n"
                            + "computer should run, for example because a signal should be processed or\n"
                            + "some sleep timer expired it is queued for execution by a worker thread.\n"
                            + "The higher the number of worker threads, the less likely it will be that\n"
                            + "computers block each other from running, but the higher the host\n"
                            + "system's load may become."
            );
            timeout = config.getDouble("timeout", timeout, 0, 60,
                    "The time in seconds a program may run without yielding before it is\n"
                            + "forcibly aborted. This is used to avoid stupidly written or malicious\n"
                            + "programs blocking other computers by locking down the executor threads.\n"
                            + "Note that changing this won't have any effect on computers that are\n"
                            + "already running - they'll have to be rebooted for this to take effect."
            );
            startupDelay = config.getDouble("startupDelay", startupDelay, 0.05, 60,
                    "The time in seconds to wait after a computer has been restored before it\n"
                            + "continues to run. This is meant to allow the world around the computer\n"
                            + "to settle, avoiding issues such as components in neighboring chunks\n"
                            + "being removed and then re-connected and other odd things that might\n"
                            + "happen."
            );
            eepromSize = config.getInt("eepromSize", eepromSize, 0, 65536,
                    "The maximum size of the byte array that can be stored on EEPROMs as executable data.."
            );
            eepromDataSize = config.getInt("eepromDataSize", eepromDataSize, 0, 65536,
                    "The maximum size of the byte array that can be stored on EEPROMs as configuration data."
            );
            cpuComponentCount = config.getIntList("cpuComponentCount", cpuComponentCount,
                    "The number of components the different CPU tiers support. This list\n"
                            + "must contain exactly three entries, or it will be ignored.",
                    "Bad number of CPU component counts, ignoring."
            );
            callBudgets = config.getDoubleList("callBudgets", callBudgets,
                    "The provided call budgets by the three tiers of CPU and memory. Higher\n"
                            + "budgets mean that more direct calls can be performed per tick. You can"
                            + "raise this to increase the \"speed\" of computers at the cost of higher\n"
                            + "real CPU time. Lower this to lower the load Lua executors put on your\n"
                            + "machine / server, at the cost of slower computers. This list must\n"
                            + "contain exactly three entries, or it will be ignored.",
                    "Bad number of call budgets, ignoring."
            );
            eraseTmpOnReboot = config.getBoolean("eraseTmpOnReboot", eraseTmpOnReboot,
                    "Whether to delete all contents in the /tmp file system when performing\n"
                            + "a 'soft' reboot (i.e. via `computer.shutdown(true)`). The tmpfs will\n"
                            + "always be erased when the computer is completely powered off, even if\n"
                            + "it crashed. This setting is purely for software-triggered reboots."
            );
            executionDelay = config.getInt("executionDelay", executionDelay, 0, 50,
                    "The time in milliseconds that scheduled computers are forced to wait\n"
                            + "before executing more code. This avoids computers to \"busy idle\",\n"
                            + "leading to artificially high CPU load. If you're worried about\n"
                            + "performance on your server, increase this number a little to reduce\n"
                            + "CPU load even more."
            );
        }

        public static final class Lua extends Category {
            public static boolean allowBytecode = false;
            public static boolean allowGC = false;
            public static boolean enableLua53 = true;
            public static int[] ramSizes = new int[]{192, 256, 384, 512, 768, 1024};
            public static double ramScaleFor64Bit = 1.8;
            public static int maxTotalRam = 64 * 1024 * 1024;

            Lua(final Category parent) {
                super(parent, "Settings specific to the Lua architecture.");
            }

            @Override
            void load(final ConfigAccess config) {
                allowBytecode = config.getBoolean("allowBytecode", allowBytecode,
                        "Whether to allow loading precompiled bytecode via Lua's `load`\n"
                                + "function, or related functions (`loadfile`, `dofile`). Enable this\n"
                                + "only if you absolutely trust all users on your server and all Lua\n"
                                + "code you run. This can be a MASSIVE SECURITY RISK, since precompiled\n"
                                + "code can easily be used for exploits, running arbitrary code on the\n"
                                + "real server! I cannot stress this enough: only enable this is you\n"
                                + "know what you're doing."
                );
                allowGC = config.getBoolean("allowGC", allowGC,
                        "Whether to allow user defined __gc callbacks, i.e. __gc callbacks\n"
                                + "defined *inside* the sandbox. Since garbage collection callbacks\n"
                                + "are not sandboxed (hooks are disabled while they run), this is not\n"
                                + "recommended."
                );
                enableLua53 = config.getBoolean("enableLua53", enableLua53,
                        "Whether to make the Lua 5.3 architecture available. If enabled, you\n"
                                + "can reconfigure any CPU to use the Lua 5.3 architecture."
                );
                ramSizes = config.getIntList("ramSizes", ramSizes,
                        "The sizes of the six levels of RAM, in kilobytes. This list must\n"
                                + "contain exactly six entries, or it will be ignored. Note that while\n"
                                + "there are six levels of RAM, they still fall into the three tiers of\n"
                                + "items (level 1, 2 = tier 1, level 3, 4 = tier 2, level 5, 6 = tier 3).",
                        "Bad number of RAM sizes, ignoring."
                );
                ramScaleFor64Bit = config.getDouble("ramScaleFor64Bit", ramScaleFor64Bit, 1, 2,
                        "This setting allows you to fine-tune how RAM sizes are scaled internally\n"
                                + "on 64 Bit machines (i.e. when the Minecraft server runs in a 64 Bit VM).\n"
                                + "Why is this even necessary? Because objects consume more memory in a 64\n"
                                + "Bit environment than in a 32 Bit one, due to pointers and possibly some\n"
                                + "integer types being twice as large. It's actually impossible to break\n"
                                + "this down to a single number, so this is really just a rough guess. If\n"
                                + "you notice this doesn't match what some Lua program would use on 32 bit,\n"
                                + "feel free to play with this and report your findings!\n"
                                + "Note that the values *displayed* to Lua via `computer.totalMemory` and\n"
                                + "`computer.freeMemory` will be scaled by the inverse, so that they always\n"
                                + "correspond to the \"apparent\" sizes of the installed memory modules. For\n"
                                + "example, when running a computer with a 64KB RAM module, even if it's\n"
                                + "scaled up to 96KB, `computer.totalMemory` will return 64KB, and if there\n"
                                + "are really 45KB free, `computer.freeMemory` will return 32KB."
                );
                maxTotalRam = config.getInt("maxTotalRam", maxTotalRam, 0, Integer.MAX_VALUE,
                        "The total maximum amount of memory a Lua machine may use for user\n"
                                + "programs. The total amount made available by components cannot\n"
                                + "exceed this. The default is 64*1024*1024. Keep in mind that this does\n"
                                + "not include memory reserved for built-in code such as `machine.lua`.\n"
                                + "IMPORTANT: DO NOT MESS WITH THIS UNLESS YOU KNOW WHAT YOU'RE DOING.\n"
                                + "IN PARTICULAR, DO NOT REPORT ISSUES AFTER MESSING WITH THIS!"
                );
            }
        }
    }

    public static final class Robot extends Category {
        public static boolean allowActivateBlocks = true;
        public static boolean allowUseItemsWithDuration = true;
        public static boolean canAttackPlayers = false;
        public static int limitFlightHeight = 8;
        public static boolean screwCobwebs = true;
        public static double swingRange = 0.49;
        public static double useAndPlaceRange = 0.65;
        public static double itemDamageRate = 0.1;
        public static String nameFormat = "$player$.robot";
        public static String uuidFormat = "$player$";
        public static int[] upgradeFlightHeight = new int[]{64, 256};

        Robot() {
            super("Robot related settings, what they may do and general balancing.");
        }

        @Override
        void load(final ConfigAccess config) {
            allowActivateBlocks = config.getBoolean("allowActivateBlocks", allowActivateBlocks,
                    "Whether robots may 'activate' blocks in the world. This includes\n"
                            + "pressing buttons and flipping levers, for example. Disable this if it\n"
                            + "causes problems with some mod (but let me know!) or if you think this\n"
                            + "feature is too over-powered."
            );
            allowUseItemsWithDuration = config.getBoolean("allowUseItemsWithDuration", allowUseItemsWithDuration,
                    "Whether robots may use items for a specifiable duration. This allows\n"
                            + "robots to use items such as bows, for which the right mouse button has\n"
                            + "to be held down for a longer period of time. For robots this works\n"
                            + "slightly different: the item is told it was used for the specified\n"
                            + "duration immediately, but the robot will not resume execution until the\n"
                            + "time that the item was supposedly being used has elapsed. This way\n"
                            + "robots cannot rapidly fire critical shots with a bow, for example."
            );
            canAttackPlayers = config.getBoolean("canAttackPlayers", canAttackPlayers,
                    "Whether robots may damage players if they get in their way. This\n"
                            + "includes all 'player' entities, which may be more than just real players\n"
                            + "in the game."
            );
            limitFlightHeight = config.getInt("limitFlightHeight", limitFlightHeight, 0, 256,
                    "Limit robot flight height, based on the following rules:\n"
                            + "- Robots may only move if the start or target position is valid (e.g.\n"
                            + "to allow building bridges).\n"
                            + "- The position below a robot is always valid (can always move down).\n"
                            + "- Positions up to <flightHeight> above a block are valid (limited\n"
                            + "flight capabilities).\n"
                            + "- Any position that has an adjacent block with a solid face towards the\n"
                            + "position is valid (robots can \"climb\").\n"
                            + "Set this to 256 to allow robots to fly wherever, as was the case\n"
                            + "before the 1.5 update. Consider using drones for cases where you need\n"
                            + "unlimited flight capabilities instead!"
            );
            upgradeFlightHeight = config.getIntList("upgradeFlightHeight", upgradeFlightHeight,
                    "The maximum flight height with upgrades, tier one and tier two of the\n"
                            + "hover upgrade, respectively.",
                    "Bad number of hover flight height counts, ignoring."
            );
            screwCobwebs = config.getBoolean("notAfraidOfSpiders", screwCobwebs,
                    "Determines whether robots are a pretty cool guy. Usually cobwebs are\n"
                            + "the bane of anything using a tool other than a sword or shears. This is\n"
                            + "an utter pain in the part you sit on, because it makes robots meant to\n"
                            + "dig holes utterly useless: the poor things couldn't break cobwebs in"
                            + "mining shafts with their golden pick axes. So, if this setting is true,\n"
                            + "we check for cobwebs and allow robots to break 'em anyway, no matter\n"
                            + "their current tool. After all, the hardness value of cobweb can only\n"
                            + "rationally explained by Steve's fear of spiders, anyway."
            );
            swingRange = config.getDouble("swingRange", swingRange, 0, 4,
                    "The 'range' of robots when swinging an equipped tool (left click). This\n"
                            + "is the distance to the center of block the robot swings the tool in to\n"
                            + "the side the tool is swung towards. I.e. for the collision check, which\n"
                            + "is performed via ray tracing, this determines the end point of the ray\n"
                            + "like so: `block_center + unit_vector_towards_side * swingRange`\n"
                            + "This defaults to a value just below 0.5 to ensure the robots will not\n"
                            + "hit anything that's actually outside said block."
            );
            useAndPlaceRange = config.getDouble("useAndPlaceRange", useAndPlaceRange, 0, 4,
                    "The 'range' of robots when using an equipped tool (right click) or when"
                            + "placing items from their inventory. See `robot.swingRange`. This\n"
                            + "defaults to a value large enough to allow robots to detect 'farmland',\n"
                            + "i.e. tilled dirt, so that they can plant seeds."
            );
            itemDamageRate = config.getDouble("itemDamageRate", itemDamageRate, 0, 1,
                    "The rate at which items used as tools by robots take damage. A value of\n"
                            + "one means that items lose durability as quickly as when they are used by\n"
                            + "a real player. A value of zero means they will not lose any durability\n"
                            + "at all. This only applies to items that can actually be damaged (such as\n"
                            + "swords, pickaxes, axes and shovels).\n"
                            + "Note that this actually is the *chance* of an item losing durability\n"
                            + "when it is used. Or in other words, it's the inverse chance that the\n"
                            + "item will be automatically repaired for the damage it just took\n"
                            + "immediately after it was used."
            );
            nameFormat = config.getString("nameFormat", nameFormat,
                    "The name format to use for robots. The substring '$player$' is\n"
                            + "replaced with the name of the player that owns the robot, so for the\n"
                            + "first robot placed this will be the name of the player that placed it.\n"
                            + "This is transitive, i.e. when a robot in turn places a robot, that\n"
                            + "robot's owner, too, will be the owner of the placing robot.\n"
                            + "The substring $random$ will be replaced with a random number in the\n"
                            + "interval [1, 0xFFFFFF], which may be useful if you need to differentiate\n"
                            + "individual robots.\n"
                            + "If a robot is placed by something that is not a player, e.g. by some\n"
                            + "block from another mod, the name will default to 'OpenComputers'."
            );
            uuidFormat = config.getString("uuidFormat", uuidFormat,
                    "Controls the UUID robots are given. You can either specify a fixed UUID\n"
                            + "here or use the two provided variables:\n"
                            + "- $random$, which will assign each robot a random UUID.\n"
                            + "- $player$, which assigns to each placed robot the UUID of the player\n"
                            + "that placed it (note: if robots are placed by fake players, i.e.\n"
                            + "other mods' blocks, they will get that mods' fake player's profile!)\n"
                            + "Note that if no player UUID is available this will be the same as\n"
                            + "$random$."
            );
        }

        public static final class Experience extends Category {
            public static double baseValue = 50;
            public static double constantGrowth = 8;
            public static double exponentialGrowth = 2;
            public static double actionXp = 0.05;
            public static double exhaustionXpRate = 1;
            public static double oreXpRate = 4;
            public static double bufferPerLevel = 5000;
            public static double toolEfficiencyPerLevel = 0.01;
            public static double harvestSpeedBoostPerLevel = 0.02;

            Experience(final Category parent) {
                super(parent, "This controls how fast robots gain experience, and how that experience alters the stats.");
            }

            @Override
            void load(final ConfigAccess config) {
                baseValue = config.getDouble("baseValue", baseValue, 0, 100,
                        "The required amount per level is computed like this:\n"
                                + "xp(level) = baseValue + (level * constantGrowth) ^ exponentialGrowth"
                );
                constantGrowth = config.getDouble("constantGrowth", constantGrowth, 1, 100,
                        "The required amount per level is computed like this:\n"
                                + "xp(level) = baseValue + (level * constantGrowth) ^ exponentialGrowth"
                );
                exponentialGrowth = config.getDouble("exponentialGrowth", exponentialGrowth, 1, 10,
                        "The required amount per level is computed like this:\n"
                                + "xp(level) = baseValue + (level * constantGrowth) ^ exponentialGrowth"
                );
                actionXp = config.getDouble("actionXp", actionXp, 0, 10,
                        "This controls how much experience a robot gains for each successful\n"
                                + "action it performs. \"Actions\" only include the following: swinging a\n"
                                + "tool and killing something or destroying a block and placing a block\n"
                                + "successfully. Note that a call to `swing` or `use` while \"bare handed\"\n"
                                + "will *not* gain a robot any experience."
                );
                exhaustionXpRate = config.getDouble("exhaustionXpRate", exhaustionXpRate, 0, 10,
                        "This determines how much \"exhaustion\" contributes to a robots\n"
                                + "experience. This is additive to the \"action\" xp, so digging a block\n"
                                + "will per default give 0.05 + 0.025 [exhaustion] * 1.0 = 0.075 XP."
                );
                oreXpRate = config.getDouble("oreXpRate", oreXpRate, 0, 10,
                        "This determines how much experience a robot gets for each real XP orb\n"
                                + "an ore it harvested would have dropped. For example, coal is worth\n"
                                + "two real experience points, redstone is worth 5."
                );
                bufferPerLevel = config.getDouble("bufferPerLevel", bufferPerLevel, 0, 50000,
                        "This is the amount of additional energy that fits into a robots\n"
                                + "internal buffer for each level it gains. So with the default values,\n"
                                + "at maximum level (30) a robot will have an internal buffer size of\n"
                                + "two hundred thousand."
                );
                toolEfficiencyPerLevel = config.getDouble("toolEfficiencyPerLevel", toolEfficiencyPerLevel, 0, 1,
                        "The additional \"efficiency\" a robot gains in using tools with each\n"
                                + "level. This basically increases the chances of a tool not losing\n"
                                + "durability when used, relative to the base rate. So for example, a\n"
                                + "robot with level 15 gets a 0.15 bonus, with the default damage rate\n"
                                + "that would lead to a damage rate of 0.1 * (1 - 0.15) = 0.085."
                );
                harvestSpeedBoostPerLevel = config.getDouble("harvestSpeedBoostPerLevel", harvestSpeedBoostPerLevel, 0, 1,
                        "The increase in block harvest speed a robot gains per level. The time\n"
                                + "it takes to break a block is computed as actualTime * (1 - bonus).\n"
                                + "For example at level 20, with a bonus of 0.4 instead of taking 0.3\n"
                                + "seconds to break a stone block with a diamond pick axe it only takes\n"
                                + "0.12 seconds."
                );
            }
        }

        public static final class Delays extends Category {
            public static double turn = 0.4;
            public static double move = 0.4;
            public static double swing = 0.4;
            public static double use = 0.4;
            public static double place = 0.4;
            public static double drop = 0.5;
            public static double suck = 0.5;
            public static double harvestRatio = 1;

            public static double skipCurrentTick(final double delay) {
                return Math.max(0.05, delay - 0.06);
            }

            Delays(final Category parent) {
                super(parent, "Allows fine-tuning of delays for robot actions.");
            }

            @Override
            void load(final ConfigAccess config) {
                turn = config.getDouble("turn", turn, 0.05, 10,
                        "The time in seconds to pause execution after a robot turned either\n"
                                + "left or right. Note that this essentially determines hw fast robots\n"
                                + "can turn around, since this also determines the length of the turn"
                                + "animation."
                );
                move = config.getDouble("move", move, 0.05, 10,
                        "The time in seconds to pause execution after a robot issued a\n"
                                + "successful move command. Note that this essentially determines how\n"
                                + "fast robots can move around, since this also determines the length\n"
                                + "of the move animation."
                );
                swing = config.getDouble("swing", swing, 0.05, 10,
                        "The time in seconds to pause execution after a robot successfully\n"
                                + "swung a tool (or it's 'hands' if nothing is equipped). Successful in"
                                + "this case means that it hit something, i.e. it attacked an entity or\n"
                                + "extinguishing fires.\n"
                                + "When breaking blocks the normal harvest time scaled with the\n"
                                + "`harvestRatio` (see below) applies."
                );
                use = config.getDouble("use", use, 0.05, 10,
                        "The time in seconds to pause execution after a robot successfully\n"
                                + "used an equipped tool (or it's 'hands' if nothing is equipped).\n"
                                + "Successful in this case means that it either used the equipped item,\n"
                                + "for example a splash potion, or that it activated a block, for\n"
                                + "example by pushing a button.\n"
                                + "Note that if an item is used for a specific amount of time, like\n"
                                + "when shooting a bow, the maximum of this and the duration of the\n"
                                + "item use is taken."
                );
                place = config.getDouble("place", place, 0.05, 10,
                        "The time in seconds to pause execution after a robot successfully\n"
                                + "placed an item from its inventory."
                );
                drop = config.getDouble("drop", drop, 0.05, 10,
                        "The time in seconds to pause execution after an item was\n"
                                + "successfully dropped from a robot's inventory."
                );
                suck = config.getDouble("suck", suck, 0.05, 10,
                        "The time in seconds to pause execution after a robot successfully\n"
                                + "picked up an item after triggering a suck command."
                );
                harvestRatio = config.getDouble("harvestRatio", harvestRatio, 0.05, 10,
                        "This is the *ratio* of the time a player would require to harvest a\n"
                                + "block. Note that robots cannot break blocks they cannot harvest. So\n"
                                + "the time a robot is forced to sleep after harvesting a block is\n"
                                + "breakTime * harvestRatio\n"
                                + "Breaking a block will always at least take one tick, 0.05 seconds."
                );
            }
        }
    }

    public static final class Power extends Category {
        public static boolean ignorePower = false;
        public static double tickFrequency = 10;
        public static double chargeRateExternal = 100;
        public static double chargeRateTablet = 10;
        public static double generatorEfficiency = 0.8;
        public static double solarGeneratorEfficiency = 0.2;
        public static double assemblerTickAmount = 50;
        public static double printerTickAmount = 1;
        public static String[] powerModBlacklist = new String[]{};

        // Set via IMC. TODO unify with Mods.isPowerProvidingModPresent.
        public static boolean is3rdPartyPowerSystemPresent = false;

        public static boolean shouldIgnorePower() {
            return ignorePower || (!is3rdPartyPowerSystemPresent && !Mods.isPowerProvidingModPresent());
        }

        Power() {
            super("Power settings, buffer sizes and power consumption.");
        }

        @Override
        void load(final ConfigAccess config) {
            ignorePower = config.getBoolean("ignorePower", ignorePower,
                    "Whether to ignore any power requirements. Whenever something requires\n"
                            + "power to function, it will try to get the amount of energy it needs from\n"
                            + "the buffer of its connector node, and in case it fails it won't perform\n"
                            + "the action / trigger a shutdown / whatever. Setting this to `true` will\n"
                            + "simply make the check 'is there enough energy' succeed unconditionally.\n"
                            + "Note that buffers are still filled and emptied following the usual\n"
                            + "rules, there just is no failure case anymore. The converter will however\n"
                            + "not accept power from other mods."
            );
            tickFrequency = config.getDouble("tickFrequency", tickFrequency, 1, 200,
                    "This determines how often continuous power sinks try to actually try to\n"
                            + "consume energy from the network. This includes computers, robots and\n"
                            + "screens. This also controls how frequent distributors re-validate their\n"
                            + "global state and secondary distributors, as well as how often the power\n"
                            + "converter queries sources for energy. If set to 1, this would query\n"
                            + "every tick. The default queries every 10 ticks, or in other words twice\n"
                            + "per second.\n"
                            + "Lower values mean more responsive power consumption, but slightly more\n"
                            + "work per tick (shouldn't be that noticeable, though). Note that this\n"
                            + "has no influence on the actual amount of energy required by computers\n"
                            + "and screens. The power cost is directly scaled up accordingly:\n"
                            + "`tickFrequency * cost`."
            );
            chargeRateExternal = config.getDouble("chargerChargeRate", chargeRateExternal, 1, 100000,
                    "The amount of energy a Charger transfers to each adjacent robot per tick\n"
                            + "if a maximum strength redstone signal is set. Chargers load robots with\n"
                            + "a controllable speed, based on the maximum strength of redstone signals\n"
                            + "going into the block. So if a redstone signal of eight is set, it'll\n"
                            + "charge robots at roughly half speed."
            );
            chargeRateTablet = config.getDouble("chargerChargeRateTablet", chargeRateTablet, 1, 10000,
                    "The amount of energy a Charger transfers into a tablet, if present, per\n"
                            + "tick. This is also based on configured charge speed, as for robots."
            );
            generatorEfficiency = config.getDouble("generatorEfficiency", generatorEfficiency, 0.1, 1,
                    "The energy efficiency of the generator upgrade. At 1.0 this will\n"
                            + "generate as much energy as you'd get by burning the fuel in a BuildCraft\n"
                            + "Stirling Engine (1MJ per fuel value / burn ticks). To discourage fully\n"
                            + "autonomous robots the efficiency of generators is slightly reduced by\n"
                            + "default."
            );
            solarGeneratorEfficiency = config.getDouble("solarGeneratorEfficiency", solarGeneratorEfficiency, 0.1, 10,
                    "The energy efficiency of the solar generator upgrade. At 1.0 this will\n"
                            + "generate as much energy as you'd get by burning  fuel in a BuildCraft\n"
                            + "Stirling Engine. To discourage fully autonomous robots the efficiency\n"
                            + "of solar generators is greatly reduced by default."
            );
            assemblerTickAmount = config.getDouble("assemblerTickAmount", assemblerTickAmount, 1, 10000,
                    "The amount of energy the robot assembler can apply per tick. This\n"
                            + "controls the speed at which robots are assembled, basically."
            );
            printerTickAmount = config.getDouble("printerTickAmount", printerTickAmount, 1, 10000,
                    "The amount of energy the printer can apply per tick. This controls\n"
                            + "the speed at which prints are completed, basically."
            );
            powerModBlacklist = config.getStringList("modBlacklist", powerModBlacklist,
                    "If you don't want OpenComputers to accept power from one or more of the\n"
                            + "supported power mods, for example because it doesn't suit the vision"
                            + "of your mod pack, you can disable support for them here. To stop\n"
                            + "OpenComputers accepting power from a mod, enter its mod id here, e.g.\n"
                            + "`BuildCraftAPI|power`, `IC2`, `factorization`, ..."
            );
        }

        public static final class Buffer extends Category {
            public static double capacitor = 1600;
            public static double capacitorAdjacencyBonus = 800;
            public static double[] capacitorUpgrades = new double[]{10000, 15000, 20000};
            public static double computer = 500;
            public static double robot = 20000;
            public static double converter = 1000;
            public static double distributor = 500;
            public static double tablet = 10000;
            public static double drone = 5000;
            public static double microcontroller = 1000;
            public static double hoverBoots = 15000;
            public static double nanomachines = 100000;

            Buffer(final Category parent) {
                super(parent, "Default \"buffer\" sizes, i.e. how much energy certain blocks can store.");
            }

            @Override
            void load(final ConfigAccess config) {
                capacitor = config.getDouble("capacitor", capacitor, 0, 16000,
                        "The amount of energy a single capacitor can store."
                );
                capacitorAdjacencyBonus = config.getDouble("capacitorAdjacencyBonus", capacitorAdjacencyBonus, 0, 8000,
                        "The amount of bonus energy a capacitor can store for each other\n"
                                + "capacitor it shares a face with. This bonus applies to both of the\n"
                                + "involved capacitors. It reaches a total of two blocks, where the\n"
                                + "bonus is halved for the second neighbor. So three capacitors in a\n"
                                + "row will give a total of 8.8k storage with default values:\n"
                                + "(1.6 + 0.8 + 0.4)k + (0.8 + 1.6 + 0.8)k + (0.4 + 0.8 + 1.6)k"
                );
                capacitorUpgrades = config.getDoubleList("capacitorUpgrades", capacitorUpgrades,
                        "The amount of energy a capacitor can store when installed as an"
                                + "upgrade into a robot.",
                        "Bad number of battery upgrade buffer sizes, ignoring."
                );
                computer = config.getDouble("computer", computer, 0, 5000,
                        "The amount of energy a computer can store. This allows you to get a\n"
                                + "computer up and running without also having to build a capacitor."
                );
                robot = config.getDouble("robot", robot, 0, 200000,
                        "The amount of energy robots can store in their internal buffer."
                );
                converter = config.getDouble("converter", converter, 0, 10000,
                        "The amount of energy a converter can store. This allows directly\n"
                                + "connecting a converter to a distributor, without having to have a\n"
                                + "capacitor on the side of the converter."
                );
                distributor = config.getDouble("distributor", distributor, 0, 5000,
                        "The amount of energy each face of a distributor can store. This\n"
                                + "allows connecting two power distributors directly. If the buffer\n"
                                + "capacity between the two distributors is zero, they won't be able\n"
                                + "to exchange energy. This basically controls the bandwidth. You can"
                                + "add capacitors between two distributors to increase this bandwidth."
                );
                tablet = config.getDouble("tablet", tablet, 0, 100000,
                        "The amount a tablet can store in its internal buffer."
                );
                microcontroller = config.getDouble("microcontroller", microcontroller, 0, 10000,
                        "The amount of energy a microcontroller can store in its internal\n"
                                + "buffer."
                );
                drone = config.getDouble("drone", drone, 0, 50000,
                        "The amount of energy a drone can store in its internal buffer."
                );
                hoverBoots = config.getDouble("hoverBoots", hoverBoots, 1, 150000,
                        "The internal buffer size of the hover boots."
                );
                nanomachines = config.getDouble("nanomachines", nanomachines, 0, 1000000,
                        "Amount of energy stored by nanomachines. Yeah, I also don't know\n"
                                + "where all that energy is stored. It's quite fascinating."
                );
            }
        }

        public static final class Cost extends Category {
            public static double computer;
            public static double microcontroller;
            public static double robot;
            public static double drone;
            public static double sleepFactor;
            public static double screen;
            public static double hologram;
            public static double hddRead;
            public static double hddWrite;
            public static double gpuSet;
            public static double gpuFill;
            public static double gpuClear;
            public static double gpuCopy;
            public static double robotTurn;
            public static double robotMove;
            public static double robotExhaustion;
            public static double wirelessCostPerRange;
            public static double abstractBusPacket;
            public static double geolyzerScan;
            public static double robotBaseCost;
            public static double robotComplexityCost;
            public static double microcontrollerBaseCost;
            public static double microcontrollerComplexityCost;
            public static double tabletBaseCost;
            public static double tabletComplexityCost;
            public static double droneBaseCost;
            public static double droneComplexityCost;
            public static double disassemblerItemCost;
            public static double chunkloader;
            public static double pistonPush;
            public static double eepromWrite;
            public static double printerModel;
            public static double hoverBootJump;
            public static double hoverBootAbsorb;
            public static double hoverBootMove;
            public static double dataCardTrivial;
            public static double dataCardTrivialByte;
            public static double dataCardSimple;
            public static double dataCardSimpleByte;
            public static double dataCardComplex;
            public static double dataCardComplexByte;
            public static double dataCardAsymmetric;
            public static double transposer;
            public static double nanomachineInput;
            public static double nanomachineReconfigure;
            public static double mfuRelay;

            Cost(final Category parent) {
                super(parent, "Default \"costs\", i.e. how much energy certain operations consume.");
            }

            @Override
            void load(final ConfigAccess config) {
                computer = config.getDouble("computer", 0.5, 0, Long.MAX_VALUE,
                        "The amount of energy a computer consumes per tick when running."
                );
                microcontroller = config.getDouble("microcontroller", 0.1, 0, Long.MAX_VALUE,
                        "Amount of energy a microcontroller consumes per tick while running."
                );
                robot = config.getDouble("robot", 0.25, 0, Long.MAX_VALUE,
                        "The amount of energy a robot consumes per tick when running. This is\n"
                                + "per default less than a normal computer uses because... well... they\n"
                                + "are better optimized? It balances out due to the cost for movement,\n"
                                + "interaction and whatnot, and the fact that robots cannot connect to\n"
                                + "component networks directly, so they are no replacements for normal\n"
                                + "computers."
                );
                drone = config.getDouble("drone", 0.4, 0, Long.MAX_VALUE,
                        "The amount of energy a drone consumes per tick when running."
                );
                sleepFactor = config.getDouble("sleepFactor", 0.1, 0, Long.MAX_VALUE,
                        "The actual cost per tick for computers and robots is multiplied\n"
                                + "with this value if they are currently in a \"sleeping\" state. They\n"
                                + "enter this state either by calling `os.sleep()` or by pulling\n"
                                + "signals. Note that this does not apply in the tick they resume, so\n"
                                + "you can't fake sleep by calling `os.sleep(0)`."
                );
                screen = config.getDouble("screen", 0.05, 0, Long.MAX_VALUE,
                        "The amount of energy a screen consumes per tick. For each lit pixel\n"
                                + "(each character that is not blank) this cost increases linearly:\n"
                                + "for basic screens, if all pixels are lit the cost per tick will be\n"
                                + "this value. Higher tier screens can become even more expensive to\n"
                                + "run, due to their higher resolution. If a screen cannot consume the\n"
                                + "defined amount of energy it will stop rendering the text that\n"
                                + "should be displayed on it. It will *not* forget that text, however,\n"
                                + "so when enough power is available again it will restore the\n"
                                + "previously displayed text (with any changes possibly made in the\n"
                                + "meantime). Note that for multi-block screens *each* screen that is\n"
                                + "part of it will consume this amount of energy per tick."
                );
                hologram = config.getDouble("hologram", 0.2, 0, Long.MAX_VALUE,
                        "The amount of energy a hologram projetor consumes per tick. This\n"
                                + "is the cost if every column is lit. If not a single voxel is\n"
                                + "displayed the hologram projector will not drain energy."
                );
                hddRead = config.getDouble("hddRead", 0.1, 0, Long.MAX_VALUE,
                        "Energy it takes read one kilobyte from a file system. Note that non"
                                + "I/O operations on file systems such as `list` or `getFreeSpace` do\n"
                                + "*not* consume power. Note that this very much determines how much\n"
                                + "energy you need in store to start a computer, since you need enough\n"
                                + "to have the computer read all the libraries, which is around 60KB\n"
                                + "at the time of writing.\n"
                                + "Note: internally this is adjusted to a cost per byte, and applied\n"
                                + "as such. It's just specified per kilobyte to be more intuitive."
                ) / 1024;
                hddWrite = config.getDouble("hddWrite", 0.25, 0, Long.MAX_VALUE,
                        "Energy it takes to write one kilobyte to a file system.\n"
                                + "Note: internally this is adjusted to a cost per byte, and applied\n"
                                + "as such. It's just specified per kilobyte to be more intuitive."
                ) / 1024;
                gpuSet = config.getDouble("gpuSet", 2.0, 0, Long.MAX_VALUE,
                        "Energy it takes to change *every* 'pixel' via the set command of a\n"
                                + "basic screen via the `set` command.\n"
                                + "Note: internally this is adjusted to a cost per pixel, and applied\n"
                                + "as such, so this also implicitly defines the cost for higher tier\n"
                                + "screens."
                ) / Constants.getBasicScreenPixels();
                gpuFill = config.getDouble("gpuFill", 1.0, 0, Long.MAX_VALUE,
                        "Energy it takes to change a basic screen with the fill command.\n"
                                + "Note: internally this is adjusted to a cost per pixel, and applied\n"
                                + "as such, so this also implicitly defines the cost for higher tier\n"
                                + "screens."
                ) / Constants.getBasicScreenPixels();
                gpuClear = config.getDouble("gpuClear", 0.1, 0, Long.MAX_VALUE,
                        "Energy it takes to clear a basic screen using the fill command with\n"
                                + "'space' as the fill char.\n"
                                + "Note: internally this is adjusted to a cost per pixel, and applied\n"
                                + "as such, so this also implicitly defines the cost for higher tier\n"
                                + "screens."
                ) / Constants.getBasicScreenPixels();
                gpuCopy = config.getDouble("gpuCopy", 0.25, 0, Long.MAX_VALUE,
                        "Energy it takes to copy half of a basic screen via the copy command.\n"
                                + "Note: internally this is adjusted to a cost per pixel, and applied\n"
                                + "as such, so this also implicitly defines the cost for higher tier\n"
                                + "screens."
                ) / Constants.getBasicScreenPixels();
                robotTurn = config.getDouble("robotTurn", 2.5, 0, Long.MAX_VALUE,
                        "The amount of energy it takes a robot to perform a 90 degree turn."
                );
                robotMove = config.getDouble("robotMove", 15.0, 0, Long.MAX_VALUE,
                        "The amount of energy it takes a robot to move a single block."
                );
                robotExhaustion = config.getDouble("robotExhaustion", 10.0, 0, Long.MAX_VALUE,
                        "The conversion rate of exhaustion from using items to energy\n"
                                + "consumed. Zero means exhaustion does not require energy, one is a\n"
                                + "one to one conversion. For example, breaking a block generates 0.025\n"
                                + "exhaustion, attacking an entity generates 0.3 exhaustion."
                );
                wirelessCostPerRange = config.getDouble("wirelessCostPerRange", 0.05, 0, Long.MAX_VALUE,
                        "The amount of energy it costs to send a wireless message with signal\n"
                                + "strength one, which means the signal reaches one block. This is\n"
                                + "scaled up linearly, so for example to send a signal 400 blocks a\n"
                                + "signal strength of 400 is required, costing a total of\n"
                                + "400 * `wirelessCostPerRange`. In other words, the higher this value,\n"
                                + "the higher the cost of wireless messages.\n"
                                + "See also: `maxWirelessRange`."
                );
                abstractBusPacket = config.getDouble("abstractBusPacket", 1, 0, Long.MAX_VALUE,
                        "The cost of a single packet sent via StargateTech 2's abstract bus."
                );
                geolyzerScan = config.getDouble("geolyzerScan", 10, 0, Long.MAX_VALUE,
                        "How much energy is consumed when the Geolyzer scans a block."
                );
                robotBaseCost = config.getDouble("robotAssemblyBase", 50000, 0, Long.MAX_VALUE,
                        "The base energy cost for assembling a robot."
                );
                robotComplexityCost = config.getDouble("robotAssemblyComplexity", 10000, 0, Long.MAX_VALUE,
                        "The additional amount of energy required to assemble a robot for\n"
                                + "each point of complexity."
                );
                microcontrollerBaseCost = config.getDouble("microcontrollerAssemblyBase", 10000, 0, Long.MAX_VALUE,
                        "The base energy cost for assembling a microcontroller."
                );
                microcontrollerComplexityCost = config.getDouble("microcontrollerAssemblyComplexity", 10000, 0, Long.MAX_VALUE,
                        "The additional amount of energy required to assemble a\n"
                                + "microcontroller for each point of complexity."
                );
                tabletBaseCost = config.getDouble("tabletAssemblyBase", 20000, 0, Long.MAX_VALUE,
                        "The base energy cost for assembling a tablet."
                );
                tabletComplexityCost = config.getDouble("tabletAssemblyComplexity", 5000, 0, Long.MAX_VALUE,
                        "The additional amount of energy required to assemble a tablet for\n"
                                + "each point of complexity."
                );
                droneBaseCost = config.getDouble("droneAssemblyBase", 25000, 0, Long.MAX_VALUE,
                        "The base energy cost for assembling a drone."
                );
                droneComplexityCost = config.getDouble("droneAssemblyComplexity", 15000, 0, Long.MAX_VALUE,
                        "The additional amount of energy required to assemble a\n"
                                + "drone for each point of complexity."
                );
                disassemblerItemCost = config.getDouble("disassemblerPerItem", 2000, 0, Long.MAX_VALUE,
                        "The amount of energy it takes to extract one ingredient from an"
                                + "item that is being disassembled. For example, if an item that was\n"
                                + "crafted from three other items gets disassembled, a total of 15000\n"
                                + "energy will be required by default.\n"
                                + "Note that this is consumed over time, and each time this amount is\n"
                                + "reached *one* ingredient gets ejected (unless it breaks, see the\n"
                                + "disassemblerBreakChance setting)."
                );
                chunkloader = config.getDouble("chunkloader", 0.06, 0, Long.MAX_VALUE,
                        "The amount of energy the chunkloader upgrade draws per tick while\n"
                                + "it is enabled, i.e. actually loading a chunk."
                );
                pistonPush = config.getDouble("pistonPush", 20, 0, Long.MAX_VALUE,
                        "The amount of energy pushing blocks with the piston upgrade costs."
                );
                eepromWrite = config.getDouble("eepromWrite", 50, 0, Long.MAX_VALUE,
                        "Energy it costs to re-program an EEPROM. This is deliberately\n"
                                + "expensive, to discourage frequent re-writing of EEPROMs."
                );
                printerModel = config.getDouble("printerModel", 100, 0, Long.MAX_VALUE,
                        "How much energy is required for a single 3D print."
                );
                hoverBootJump = config.getDouble("hoverBootJump", 10, 0, Long.MAX_VALUE,
                        "The amount of energy consumed when jumping with the hover boots. Only\n"
                                + "applies when the jump boost is applied, i.e. when not sneaking."
                );
                hoverBootAbsorb = config.getDouble("hoverBootAbsorb", 10, 0, Long.MAX_VALUE,
                        "The amount of energy consumed when the hover boots absorb some fall\n"
                                + "velocity (i.e. when falling from something higher than three blocks)."
                );
                hoverBootMove = config.getDouble("hoverBootMove", 1, 0, Long.MAX_VALUE,
                        "The amount of energy consumed *per second* when moving around while\n"
                                + "wearing the hover boots. This is compensate for the step assist, which\n"
                                + "does not consume energy on a per-use basis. When standing still or\n"
                                + "moving very slowly this also does not trigger."
                );
                dataCardTrivial = config.getDouble("dataCardTrivial", 0.2, 0, Long.MAX_VALUE,
                        "Cost for trivial operations on the data card, such as CRC32 or Base64"
                );
                dataCardTrivialByte = config.getDouble("dataCardTrivialByte", 0.005, 0, Long.MAX_VALUE,
                        "Per-byte cost for trivial operations"
                );
                dataCardSimple = config.getDouble("dataCardSimple", 1.0, 0, Long.MAX_VALUE,
                        "Cost for simple operations on the data card, such as MD5 or AES"
                );
                dataCardSimpleByte = config.getDouble("dataCardSimpleByte", 0.01, 0, Long.MAX_VALUE,
                        "Per-byte cost for simple operations"
                );
                dataCardComplex = config.getDouble("dataCardComplex", 6.0, 0, Long.MAX_VALUE,
                        "Cost for complex operations on the data card, such as SHA256, inflate/deflate and SecureRandom."
                );
                dataCardComplexByte = config.getDouble("dataCardComplexByte", 0.1, 0, Long.MAX_VALUE,
                        "Per-byte cost for complex operations"
                );
                dataCardAsymmetric = config.getDouble("dataCardAsymmetric", 10.0, 0, Long.MAX_VALUE,
                        "Cost for asymmetric operations on the data card, such as ECDH and ECDSA\n"
                                + "Per-byte cost for ECDSA operation is controlled by `complex` value,\n"
                                + "because data is hashed with SHA256 before signing/verifying"
                );
                transposer = config.getDouble("transposer", 1, 0, Long.MAX_VALUE,
                        "Energy required for one transposer operation (regardless of the number\n"
                                + "of items / fluid volume moved)."
                );
                nanomachineInput = config.getDouble("nanomachineInput", 0.5, 0, Long.MAX_VALUE,
                        "Energy consumed per tick per active input node by nanomachines."
                );
                nanomachineReconfigure = config.getDouble("nanomachineReconfigure", 5000, 0, Long.MAX_VALUE,
                        "Energy consumed when reconfiguring nanomachines."
                );
                mfuRelay = config.getDouble("mfuRelay", 1, 0, Long.MAX_VALUE,
                        "Energy consumed by a MFU per tick while connected.\n"
                                + "Similarly to `wirelessCostPerRange`, this is multiplied with the distance to the bound block."
                );
            }
        }

        public static final class Rate extends Category {
            public static double assembler = 100;
            public static double[] caseRate = new double[]{5, 10, 20};
            public static double charger = 200;
            public static double powerConverter = 500;
            public static double serverRack = 50;

            Rate(final Category parent) {
                super(parent, "The rate at which different blocks accept external power. All of these values are in OC energy / tick.");
            }

            @Override
            void load(final ConfigAccess config) {
                assembler = config.getDouble("assembler", 100.0, 0, 10000,
                        ""
                );
                caseRate = config.getDoubleList("case", caseRate,
                        "",
                        "Bad number of computer case conversion rates, ignoring."
                );
                charger = config.getDouble("charger", charger, 0, 10000,
                        ""
                );
                powerConverter = config.getDouble("powerConverter", powerConverter, 0, 10000,
                        ""
                );
                serverRack = config.getDouble("serverRack", serverRack, 0, 10000,
                        ""
                );
            }
        }

        public static final class Value extends Category {
            private static double RedstoneFlux;

            private static final double Internal = 1000;

            public static double ratioRedstoneFlux() {
                return RedstoneFlux / Internal;
            }

            Value(final Category parent) {
                super(parent,
                        "Power values for different power systems. For reference, the value of\n"
                                + "OC's internal energy type is 1000. I.e. the conversion ratios are the\n"
                                + "values here divided by 1000. This is mainly to avoid small floating\n"
                                + "point numbers in the config, due to potential loss of precision.");
            }

            @Override
            void load(final ConfigAccess config) {
                RedstoneFlux = config.getDouble("RedstoneFlux", 1000,
                        ""
                );
            }
        }
    }

    public static final class Filesystem extends Category {
        ;
        public static boolean bufferChanges = true;
        public static int fileCost = 512;
        public static int[] hddSizes = new int[]{1024, 2048, 4096};
        public static int[] hddPlatterCounts = new int[]{2, 4, 6};
        public static int floppySize = 512;
        public static int tmpSize = 64;
        public static int maxHandles = 16;
        public static int maxReadBuffer = 2048;
        public static int sectorSeekThreshold = 128;
        public static double sectorSeekTime = 0.1;

        Filesystem() {
            super("File system related settings, performance and and balancing.");
        }

        @Override
        void load(final ConfigAccess config) {
            bufferChanges = config.getBoolean("bufferChanges", bufferChanges,
                    "Whether persistent file systems such as disk drives should be\n"
                            + "'buffered', and only written to disk when the world is saved. This\n"
                            + "applies to all hard drives. The advantage of having this enabled is that\n"
                            + "data will never go 'out of sync' with the computer's state if the game\n"
                            + "crashes. The price is slightly higher memory consumption, since all\n"
                            + "loaded files have to be kept in memory (loaded as in when the hard drive\n"
                            + "is in a computer)."
            );
            fileCost = config.getInt("fileCost", fileCost, 0, 4096,
                    "The base 'cost' of a single file or directory on a limited file system,\n"
                            + "such as hard drives. When computing the used space we add this cost to\n"
                            + "the real size of each file (and folders, which are zero sized\n"
                            + "otherwise). This is to ensure that users cannot spam the file system\n"
                            + "with an infinite number of files and/or folders. Note that the size\n"
                            + "returned via the API will always be the real file size, however."
            );
            hddSizes = config.getIntList("hddSizes", hddSizes,
                    "The sizes of the three tiers of hard drives, in kilobytes. This list\n"
                            + "must contain exactly three entries, or it will be ignored.",
                    "Bad number of HDD sizes, ignoring."
            );
            hddPlatterCounts = config.getIntList("hddPlatterCounts", hddPlatterCounts,
                    "Number of physical platters to pretend a disk has in unmanaged mode. This\n"
                            + "controls seek times, in how it emulates sectors overlapping (thus sharing\n"
                            + "a common head position for access).",
                    "Bad number of HDD platter counts, ignoring."
            );
            floppySize = config.getInt("floppySize", floppySize, 0, 4096,
                    "The size of writable floppy disks, in kilobytes."
            );
            tmpSize = config.getInt("tmpSize", tmpSize, 0, 4096,
                    "The size of the /tmp filesystem that each computer gets for free. If\n"
                            + "set to a non-positive value the tmp file system will not be created."
            );
            maxHandles = config.getInt("maxHandles", maxHandles, 0, 128,
                    "The maximum number of file handles any single computer may have open at\n"
                            + "a time. Note that this is *per filesystem*. Also note that this is only\n"
                            + "enforced by the filesystem node - if an add-on decides to be fancy it\n"
                            + "may well ignore this. Since file systems are usually 'virtual' this will\n"
                            + "usually not have any real impact on performance and won't be noticeable\n"
                            + "on the host operating system."
            );
            maxReadBuffer = config.getInt("maxReadBuffer", maxReadBuffer, 0, 8192,
                    "The maximum block size that can be read in one 'read' call on a file\n"
                            + "system. This is used to limit the amount of memory a call from a user\n"
                            + "program can cause to be allocated on the host side: when 'read' is,\n"
                            + "called a byte array with the specified size has to be allocated. So if\n"
                            + "this weren't limited, a Lua program could trigger massive memory\n"
                            + "allocations regardless of the amount of RAM installed in the computer it\n"
                            + "runs on. As a side effect this pretty much determines the read\n"
                            + "performance of file systems."
            );
            sectorSeekThreshold = config.getInt("sectorSeekThreshold", sectorSeekThreshold, 1, 512,
                    "When skipping more than this number of sectors in unmanaged mode, the\n"
                            + "pause specified in sectorSeekTime will be enforced. We use this instead\n"
                            + "of linear scaling for movement because those values would have to be\n"
                            + "really small, which is hard to conceptualize and configure."
            );
            sectorSeekTime = config.getDouble("sectorSeekTime", sectorSeekTime, 0, 5,
                    "The time to pause when the head movement threshold is exceeded."
            );
        }
    }

    public static final class Internet extends Category {
        public static boolean enableHttp = true;
        public static boolean enableHttpHeaders = true;
        public static boolean enableTcp = true;
        public static List<AddressValidator> httpHostBlacklist;
        public static List<AddressValidator> httpHostWhitelist;
        public static int httpTimeout = 0;
        public static int maxTcpConnections = 4;
        public static int internetThreads = 4;

        Internet() {
            super("Internet settings, security related.");
        }

        @Override
        void load(final ConfigAccess config) {
            enableHttp = config.getBoolean("enableHttp", enableHttp,
                    "Whether to allow HTTP requests via internet cards. When enabled,\n"
                            + "the `request` method on internet card components becomes available."
            );
            enableHttpHeaders = config.getBoolean("enableHttpHeaders", enableHttpHeaders,
                    "Whether to allow adding custom headers to HTTP requests."
            );
            enableTcp = config.getBoolean("enableTcp", enableTcp,
                    "Whether to allow TCP connections via internet cards. When enabled,\n"
                            + "the `connect` method on internet card components becomes available."
            );
            httpHostBlacklist = Arrays.stream(config.getStringList("blacklist", new String[]{
                            "127.0.0.0/8",
                            "10.0.0.0/8",
                            "192.168.0.0/16",
                            "172.16.0.0/12"
                    },
                    "This is a list of blacklisted domain names. If an HTTP request is made\n"
                            + "or a socket connection is opened the target address will be compared\n"
                            + "to the addresses / adress ranges in this list. It it is present in this\n"
                            + "list, the request will be denied.\n"
                            + "Entries are either domain names (www.example.com) or IP addresses in"
                            + "string format (10.0.0.3), optionally in CIDR notation to make it easier\n"
                            + "to define address ranges (1.0.0.0/8). Domains are resolved to their\n"
                            + "actual IP once on startup, future requests are resolved and compared\n"
                            + "to the resolved addresses.\n"
                            + "By default all local addresses are blocked. This is only meant as a\n"
                            + "thin layer of security, to avoid average users hosting a game on their\n"
                            + "local machine having players access services in their local network.\n"
                            + "Server hosters are expected to configure their network outside of the\n"
                            + "mod's context in an appropriate manner, e.g. using a system firewall."
            )).map(AddressValidator::create).collect(Collectors.toList());
            httpHostWhitelist = Arrays.stream(config.getStringList("whitelist", new String[]{},
                    "This is a list of whitelisted domain names. Requests may only be made\n"
                            + "to addresses that are present in this list. If this list is empty,\n"
                            + "requests may be made to all addresses not blacklisted. Note that the\n"
                            + "blacklist is always applied, so if an entry is present in both the\n"
                            + "whitelist and the blacklist, the blacklist will win.\n"
                            + "Entries are of the same format as in the blacklist. Examples:\n"
                            + "\"gist.github.com\", \"www.pastebin.com\""
            )).map(AddressValidator::create).collect(Collectors.toList());
            httpTimeout = config.getInt("requestTimeout", httpTimeout, 0, 60,
                    "The time in seconds to wait for a response to a request before timing\n"
                            + "out and returning an error message. If this is zero (the default) the\n"
                            + "request will never time out."
            );
            maxTcpConnections = config.getInt("maxTcpConnections", maxTcpConnections, 0, 64,
                    "The maximum concurrent TCP connections *each* internet card can have\n"
                            + "open at a time."
            );
            internetThreads = config.getInt("threads", internetThreads, 1, 16,
                    "The number of threads used for processing host name look-ups and HTTP\n"
                            + "requests in the background. The more there are, the more concurrent\n"
                            + "connections can potentially be opened by computers, and the less likely\n"
                            + "they are to delay each other."
            );
        }
    }

    public static final class Relay extends Category {
        public static int defaultMaxQueueSize = 20;
        public static int defaultRelayAmount = 1;
        public static int defaultRelayDelay = 5;
        public static int queueSizeUpgrade = 10;
        public static int relayAmountUpgrade = 1;
        public static double relayDelayUpgrade = 1.5;

        Relay() {
            super("Relay network message forwarding logic related stuff.");
        }

        @Override
        void load(final ConfigAccess config) {
            defaultMaxQueueSize = config.getInt("defaultMaxQueueSize", defaultMaxQueueSize, 1, 100,
                    "This is the size of the queue of a not upgraded relay. Increasing it\n"
                            + "avoids packets being dropped when many messages are sent in a single\n"
                            + "burst."
            );
            defaultRelayAmount = config.getInt("defaultRelayAmount", defaultRelayAmount, 1, 10,
                    "The base number of packets that get relayed in one 'cycle'. The\n"
                            + "cooldown between cycles is determined by the delay."
            );
            defaultRelayDelay = config.getInt("defaultRelayDelay", defaultRelayDelay, 1, 10,
                    "The delay a relay has by default between relaying packets (in ticks).\n"
                            + "WARNING: lowering this value will result in higher maximum CPU load,\n"
                            + "and may in extreme cases cause server lag."
            );
            queueSizeUpgrade = config.getInt("queueSizeUpgrade", queueSizeUpgrade, 0, 50,
                    "This is the amount by which the queue size increases per tier of the\n"
                            + "hard drive installed in the relay."
            );
            relayAmountUpgrade = config.getInt("relayAmountUpgrade", relayAmountUpgrade, 0, 10,
                    "The number of additional packets that get relayed per cycle, based on"
                            + "the tier of RAM installed in the relay. For built-in RAM this\n"
                            + "increases by one per half-tier, for third-party ram this increases by\n"
                            + "two per item tier."
            );
            relayDelayUpgrade = config.getDouble("relayDelayUpgrade", relayDelayUpgrade, 0, 10,
                    "The amount of ticks the delay is *reduced* by per tier of the CPU\n"
                            + "inserted into a relay."
            );
        }
    }

    public static final class Nanomachines extends Category {
        public static double triggerQuota = 0.4;
        public static double connectorQuota = 0.2;
        public static int maxInputs = 2;
        public static int maxOutputs = 2;
        public static int safeInputsActive = 2;
        public static int maxInputsActive = 4;
        public static double commandDelay = 1;
        public static double commandRange = 2;
        public static double magnetRange = 8;
        public static int disintegrationRange = 1;
        public static String[] potionWhitelist = new String[]{
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
        };
        public static int hungryDamage = 5;
        public static int hungryEnergyRestored = 50;

        Nanomachines() {
            super(
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
                            + "increase their amplification value).");
        }

        @Override
        void load(final ConfigAccess config) {
            triggerQuota = config.getDouble("triggerQuota", triggerQuota, 0, 1,
                    "The relative amount of triggers available based on the number of\n"
                            + "available behaviors (such as different potion effects). For example,\n"
                            + "if there are a total of 10 behaviors available, 0.5 means there will\n"
                            + "be 5 trigger inputs, triggers being the inputs that can be activated\n"
                            + "via nanomachines."
            );
            connectorQuota = config.getDouble("connectorQuota", connectorQuota, 0, 1,
                    "The relative number of connectors based on the number of available\n"
                            + "behaviors (see triggerQuota)."
            );
            maxInputs = config.getInt("maxInputs", maxInputs, 1, 10,
                    "The maximum number of inputs for each node of the \"neural network\"\n"
                            + "nanomachines connect to. I.e. each behavior node and connector node\n"
                            + "may only have up to this many inputs."
            );
            maxOutputs = config.getInt("maxOutputs", maxOutputs, 1, 10,
                    "The maximum number of outputs for each node (see maxInputs)."
            );
            safeInputsActive = config.getInt("safeInputsActive", safeInputsActive, 0, 10,
                    "How many input nodes may be active at the same time before negative\n"
                            + "effects are applied to the player."
            );
            maxInputsActive = config.getInt("maxInputsActive", maxInputsActive, 0, 10,
                    "Hard maximum number of active inputs. This is mainly to avoid people\n"
                            + "bumping other nanomachines' inputs to max, killing them in a matter\n"
                            + "of (milli)seconds."
            );
            commandDelay = config.getDouble("commandDelay", commandDelay, 0, 10,
                    "Time in seconds it takes for the nanomachines to process a command\n"
                            + "and send a response."
            );
            commandRange = config.getDouble("commandRange", commandRange, 0, 64,
                    "The distance in blocks that nanomachines can communicate within. If\n"
                            + "a message comes from further away, it'll be ignored. When responding,\n"
                            + "the response will only be sent this far."
            );
            magnetRange = config.getDouble("magnetRange", magnetRange, 0, 16,
                    "Range of the item magnet behavior added for each active input."
            );
            disintegrationRange = config.getInt("disintegrationRange", disintegrationRange, 0, 4,
                    "Radius in blocks of the disintegration behavior for each active input."
            );
            potionWhitelist = config.getStringList("potionWhitelist", potionWhitelist,
                    "Whitelisted potions, i.e. potions that will be used for the potion"
                            + "behaviors nanomachines may trigger. This can contain strings or numbers.\n"
                            + "In the case of strings, it has to be the internal name of the potion,\n"
                            + "in case of a number it has to be the potion ID. Add any potion effects\n"
                            + "to make use of here, since they will all be disabled by default."
            );
            hungryDamage = config.getInt("hungryDamage", hungryDamage, 0, 100,
                    "How much damage the hungry behavior should deal to the player when the\n"
                            + "nanomachine controller runs out of energy."
            );
            hungryEnergyRestored = config.getInt("hungryEnergyRestored", hungryEnergyRestored, 0, 10000,
                    "How much energy the hungry behavior should restore when damaging the\n"
                            + "player."
            );
        }
    }

    public static final class Printer extends Category {
        public static int maxPrintComplexity = 24;
        public static double printRecycleRate = 0.75;
        public static boolean chameliumEdible = true;
        public static int maxPrintLightLevel = 8;
        public static int printCustomRedstone = 300;
        public static int printMaterialValue = 2000;
        public static int printInkValue = 50000;
        public static boolean printsHaveOpacity = false;
        public static double noclipMultiplier = 2;

        Printer() {
            super("3D printer related stuff.");
        }

        @Override
        void load(final ConfigAccess config) {
            maxPrintComplexity = config.getInt("maxShapes", maxPrintComplexity, 1, 256,
                    "The maximum number of shape for a state of a 3D print allowed. This is\n"
                            + "for the individual states (off and on), so it is possible to have up to\n"
                            + "this many shapes *per state* (the reasoning being that only one state\n"
                            + "will ever be visible at a time)."
            );
            printRecycleRate = config.getDouble("recycleRate", printRecycleRate, 0, 1,
                    "How much of the material used to print a model is refunded when using\n"
                            + "the model to refuel a printer. This the value the original material\n"
                            + "cost is multiplied with, so 1 is a full refund, 0 disables the\n"
                            + "functionality (won't be able to put prints into the material input)."
            );
            chameliumEdible = config.getBoolean("chameliumEdible", chameliumEdible,
                    "Whether Chamelium is edible or not. When eaten, it gives a (short)\n"
                            + "invisibility buff, and (slightly longer) blindness debuff."
            );
            maxPrintLightLevel = config.getInt("maxBaseLightLevel", maxPrintLightLevel, 0, 15,
                    "The maximum light level a printed block can emit. This defaults to\n"
                            + "a value similar to that of a redstone torch, because by default the\n"
                            + "material prints are made of contains redstone, but no glowstone.\n"
                            + "Prints' light level can further be boosted by crafting them with\n"
                            + "glowstone dust. This is merely the maximum light level that can be\n"
                            + "achieved directly when printing them."
            );
            printCustomRedstone = config.getInt("customRedstoneCost", printCustomRedstone, 0, 10000,
                    "The extra material cost involved for printing a model with a customized\n"
                            + "redstone output, i.e. something in [1, 14]."
            );
            printMaterialValue = config.getInt("materialValue", printMaterialValue, 0, 100000,
                    "The amount by which a printers material buffer gets filled for a single\n"
                            + "chamelium. Tweak this if you think printing is too cheap or expensive."
            );
            printInkValue = config.getInt("inkValue", printInkValue, 0, 100000,
                    "The amount by which a printers ink buffer gets filled for a single\n"
                            + "cartridge. Tweak this if you think printing is too cheap or expensive.\n"
                            + "Note: the amount a single dye adds is this divided by 10."
            );
            printsHaveOpacity = config.getBoolean("printsHaveOpacity", printsHaveOpacity,
                    "Whether to enable print opacity, i.e. make prints have shadows. If\n"
                            + "enabled, prints will have an opacity that is estimated from their\n"
                            + "sampled fill rate. This is disabled by default, because MC's lighting\n"
                            + "computation is apparently not very happy with multiple blocks with\n"
                            + "dynamic opacity sitting next to each other, and since all prints share\n"
                            + "the same block type, this can lead to weird shadows on prints. If you\n"
                            + "don't care about that and prefer them to be not totally shadowless,\n"
                            + "enable this."
            );
            noclipMultiplier = config.getDouble("noclipMultiplier", noclipMultiplier, 0, 100,
                    "By what (linear) factor the cost of a print increases if one or both of\n"
                            + "its states are non-collidable (i.e. entities can move through them).\n"
                            + "This only influences the chamelium cost."
            );
        }
    }

    public static final class Hologram extends Category {
        public static double[] maxScaleByTier = new double[]{3, 4};
        public static double[] maxTranslationByTier = new double[]{0.25, 0.5};
        public static double rawDelay = 0.2;
        public static boolean light = true;

        Hologram() {
            super("Hologram related stuff.");
        }

        @Override
        void load(final ConfigAccess config) {
            maxScaleByTier = config.getDoubleList("maxScale", maxScaleByTier,
                    "This controls the maximum scales of holograms, by tier.\n"
                            + "The size at scale 1 is 3x2x3 blocks, at scale 3 the hologram will\n"
                            + "span up to 9x6x9 blocks. Unlike most other `client' settings, this\n"
                            + "value is only used for validation *on the server*, with the effects\n"
                            + "only being visible on the client.\n"
                            + "Warning: very large values may lead to rendering and/or performance\n"
                            + "issues due to the high view distance! Increase at your own peril.",
                    "Bad number of hologram max scales, ignoring."
            );
            maxTranslationByTier = config.getDoubleList("maxTranslation", maxTranslationByTier,
                    "This controls the maximum translation of holograms, by tier.\n"
                            + "The scale is in \"hologram sizes\", i.e. scale 1 allows offsetting a\n"
                            + "hologram once by its own size.",
                    "Bad number of hologram max translations, ignoring."
            );
            rawDelay = config.getDouble("setRawDelay", rawDelay, 0, 10,
                    "The delay forced on computers between calls to `hologram.setRaw`, in"
                            + "seconds. Lower this if you want faster updates, raise this if you're\n"
                            + "worried about bandwidth use; in *normal* use-cases this will never be\n"
                            + "an issue. When abused, `setRaw` can be used to generate network traffic\n"
                            + "due to changed data being sent to clients. With the default settings,\n"
                            + "the *worst case* is ~30KB/s/client. Again, for normal use-cases this\n"
                            + "is actually barely noticeable."
            );
            light = config.getBoolean("emitLight", light,
                    "Whether the hologram block should provide light. It'll also emit light\n"
                            + "when off, because having state-based light in MC is... painful."
            );
        }
    }

    public static final class Misc extends Category {
        public static int maxScreenWidth = 8;
        public static int maxScreenHeight = 6;
        public static boolean inputUsername = true;
        public static int maxClipboard = 1024;
        public static int maxNetworkPacketSize = 8192;
        public static int maxNetworkPacketParts = 8;
        public static int maxOpenPorts = 16;
        public static double maxWirelessRange = 400;
        public static int terminalsPerServer = 4;
        public static int lootProbability = 5;
        public static boolean lootRecrafting = true;
        public static int geolyzerRange = 32;
        public static double geolyzerNoise = 2;
        public static boolean hideOwnPet = false;
        public static boolean allowItemStackInspection = true;
        public static double presentChance = 0.05;
        public static String[] assemblerBlacklist = new String[]{};
        public static int threadPriority = -1;
        public static boolean giveManualToNewPlayers = true;
        public static int dataCardSoftLimit = 8*1024;
        public static int dataCardHardLimit = 1024*1024;
        public static double dataCardTimeout = 1;
        public static int serverRackRelayTier = Tier.One();
        public static double redstoneDelay = 0.1;
        public static double tradingRange = 8;
        public static int mfuRange = 3;

        Misc() {
            super("Other settings that you might find useful to tweak.");
        }

        @Override
        void load(final ConfigAccess config) {
            maxScreenWidth = config.getInt("maxScreenWidth", maxScreenWidth, 1, 32,
                    "The maximum width of multi-block screens, in blocks.\n"
                            + "See also: `maxScreenHeight`."
            );
            maxScreenHeight = config.getInt("maxScreenHeight", maxScreenHeight, 1, 32,
                    "The maximum height of multi-block screens, in blocks. This is limited to\n"
                            + "avoid excessive computations for merging screens. If you really need\n"
                            + "bigger screens it's probably safe to bump this quite a bit before you\n"
                            + "notice anything, since at least incremental updates should be very\n"
                            + "efficient (i.e. when adding/removing a single screen)."
            );
            inputUsername = config.getBoolean("inputUsername", inputUsername,
                    "Whether to pass along the name of the user that caused an input signals\n"
                            + "to the computer (mouse and keyboard signals). If you feel this breaks\n"
                            + "the game's immersion, disable it.\n"
                            + "Note: also applies to the motion sensor."
            );
            maxClipboard = config.getInt("maxClipboard", maxClipboard, 0, 8192,
                    "The maximum length of a string that may be pasted. This is used to limit\n"
                            + "the size of the data sent to the server when the user tries to paste a\n"
                            + "string from the clipboard (Shift+Ins on a screen with a keyboard)."
            );
            maxNetworkPacketSize = config.getInt("maxNetworkPacketSize", maxNetworkPacketSize, 0, 65536,
                    "The maximum size of network packets to allow sending via network cards.\n"
                            + "This has *nothing to do* with real network traffic, it's just a limit\n"
                            + "for the network cards, mostly to reduce the chance of computer with a\n"
                            + "lot of RAM killing those with less by sending huge packets. This does\n"
                            + "not apply to HTTP traffic."
            );
            // Need at least 4 for nanomachine protocol.
            maxNetworkPacketParts = config.getInt("maxNetworkPacketParts", maxNetworkPacketParts, 4, 64,
                    "The maximum number of \"data parts\" a network packet is allowed to have.\n"
                            + "When sending a network message, from Lua this may look like so:\n"
                            + "component.modem.broadcast(port, \"first\", true, \"third\", 123)\n"
                            + "This limits how many arguments can be passed and are wrapped into a\n"
                            + "packet. This limit mostly serves as a protection for lower-tier\n"
                            + "computers, to avoid them getting nuked by more powerful computers."
            );
            maxOpenPorts = config.getInt("maxOpenPorts", maxOpenPorts, 0, 64,
                    "The maximum number of ports a single network card can have opened at\n"
                            + "any given time."
            );
            maxWirelessRange = config.getDouble("maxWirelessRange", maxWirelessRange, 0, 5000,
                    "The maximum distance a wireless message can be sent. In other words,\n"
                            + "this is the maximum signal strength a wireless network card supports.\n"
                            + "This is used to limit the search range in which to check for modems,\n"
                            + "which may or may not lead to performance issues for ridiculous ranges -\n"
                            + "like, you know, more than the loaded area.\n"
                            + "See also: `wirelessCostPerRange`."
            );
            lootProbability = config.getInt("lootProbability", lootProbability, 0, Integer.MAX_VALUE,
                    "The probability (or rather, weighted chance) that a program disk is\n"
                            + "spawned as loot in a treasure chest. For reference, iron ingots have\n"
                            + "a value of 10, gold ingots a value of 5 and and diamonds a value of 3.\n"
                            + "This is the chance *that* a disk is created. Which disk that will be\n"
                            + "is decided in an extra roll of the dice."
            );
            lootRecrafting = config.getBoolean("lootRecrafting", lootRecrafting,
                    "Whether to allow loot disk cycling by crafting them with a wrench."
            );
            geolyzerRange = config.getInt("geolyzerRange", geolyzerRange, 0, 128,
                    "The range, in blocks, in which the Geolyzer can scan blocks. Note that\n"
                            + "it uses the maximum-distance, not the euclidean one, i.e. it can scan"
                            + "in a cube surrounding it with twice this value as its edge length."
            );
            geolyzerNoise = config.getDouble("geolyzerNoise", geolyzerNoise, 0, 64,
                    "Controls how noisy results from the Geolyzer are. This is the maximum\n"
                            + "deviation from the original value at the maximum vertical distance\n"
                            + "from the geolyzer. Noise increases linearly with the vertical distance\n"
                            + "to the Geolyzer. So yes, on the same height, the returned value are of\n"
                            + "equal 'quality', regardless of the real distance. This is a performance\n"
                            + "trade-off."
            );
            hideOwnPet = config.getBoolean("hideOwnSpecial", hideOwnPet,
                    "Whether to not show your special thinger (if you have one you know it)."
            );
            allowItemStackInspection = config.getBoolean("allowItemStackInspection", allowItemStackInspection,
                    "Allow robots to get a table representation of item stacks using the\n"
                            + "inventory controller upgrade? (i.e. whether the getStackInSlot method\n"
                            + "of said upgrade is enabled or not). Also applies to tank controller\n"
                            + "upgrade and it's fluid getter method."
            );
            presentChance = config.getDouble("presentChance", presentChance, 0, 1,
                    "Probability that at certain celebratory times crafting an OC item will\n"
                            + "spawn a present in the crafting player's inventory. Set to zero to\n"
                            + "disable."
            );
            assemblerBlacklist = config.getStringList("assemblerBlacklist", assemblerBlacklist,
                    "List of item descriptors of assembler template base items to blacklist,\n"
                            + "i.e. for disabling the assembler template for. Entries must be of the\n"
                            + "format 'itemid@damage', were the damage is optional.\n"
                            + "Examples: 'OpenComputers:case3', 'minecraft:stonebrick@1'"
            );
            threadPriority = config.getInt("threadPriority", threadPriority,
                    "Override for the worker threads' thread priority. If set to a value\n"
                            + "lower than 1 it will use the default value, which is half-way between"
                            + "the system minimum and normal priority. Valid values may differ between"
                            + "Java versions, but usually the minimum value (lowest priority) is 1,\n"
                            + "the normal value is 5 and the maximum value is 10. If a manual value is\n"
                            + "given it is automatically capped at the maximum.\n"
                            + "USE THIS WITH GREAT CARE. Using a high priority for worker threads may\n"
                            + "avoid issues with computers timing out, but can also lead to higher\n"
                            + "server load. AGAIN, USE WITH CARE!"
            );
            giveManualToNewPlayers = config.getBoolean("giveManualToNewPlayers", giveManualToNewPlayers,
                    "Whether to give a new player a free copy of the manual. This will only\n"
                            + "happen one time per game, not per world, not per death. Once. If this\n"
                            + "is still too much for your taste, disable it here ;-)"
            );
            dataCardSoftLimit = config.getInt("dataCardSoftLimit", dataCardSoftLimit, 0, 16*1024,
                    "Soft limit for size of byte arrays passed to data card callbacks. If this\n"
                            + "limit is exceeded, a longer sleep is enforced (see dataCardTimeout)."
            );
            dataCardHardLimit = config.getInt("dataCardHardLimit", dataCardHardLimit, 0, 2048*1024,
                    "Hard limit for size of byte arrays passed to data card callbacks. If this\n"
                            + "limit is exceeded, the call fails and does nothing."
            );
            dataCardTimeout = config.getDouble("dataCardTimeout", dataCardTimeout, 0, 10,
                    "Time in seconds to pause a calling machine when the soft limit for a data\n"
                            + "card callback is exceeded."
            );
            serverRackRelayTier = config.getInt("serverRackRelayTier", serverRackRelayTier + 1, Tier.None() + 1, Tier.Three() + 1,
                    "The general upgrade tier of the relay built into server racks, i.e. how\n"
                            + "upgraded server racks' relaying logic is. Prior to the introduction of\n"
                            + "this setting (1.5.15) this was always none. This applies to all\n"
                            + "properties, i.e. throughput, frequency and buffer size.\n"
                            + "Valid values are: 0 = none, 1 = tier 1, 2 = tier 2, 3 = tier 3."
            ) - 1;
            redstoneDelay = config.getDouble("redstoneDelay", redstoneDelay, 0, 1,
                    "Enforced delay when changing a redstone emitting component's output,\n"
                            + "such as the redstone card and redstone I/O block. Lowering this can"
                            + "have very negative impact on server TPS, so beware."
            );
            tradingRange = config.getDouble("tradingRange", tradingRange, 0, 16,
                    "The maximum range between the drone/robot and a villager for a trade to\n"
                            + "be performed by the trading upgrade"
            );
            mfuRange = config.getInt("mfuRange", mfuRange, 0, 64,
                    "Radius the MFU is able to operate in"
            );
        }
    }

    public static final class Integration extends Category {
        public static String[] modBlacklist = new String[]{};
        public static String[] peripheralBlacklist = new String[]{TileEntityCommandBlock.class.getName()};
        public static String fakePlayerUuid = "7e506b5d-2ccb-4ac4-a249-5624925b0c67";
        public static String fakePlayerName = "[OpenComputers]";
        public static GameProfile fakePlayerProfile;

        Integration() {
            super("Settings for mod integration (the mod previously known as OpenComponents).");
        }

        @Override
        void load(final ConfigAccess config) {
            modBlacklist = config.getStringList("modBlacklist", modBlacklist,
                    "A list of mods (by mod id) for which support should NOT be enabled. Use\n"
                            + "this to disable support for mods you feel should not be controllable via\n"
                            + "computers (such as magic related mods, which is why Thaumcraft is on this\n"
                            + "list by default.)"
            );
            peripheralBlacklist = config.getStringList("peripheralBlacklist", peripheralBlacklist,
                    "A list of tile entities by class name that should NOT be accessible via\n"
                            + "the Adapter block. Add blocks here that can lead to crashes or deadlocks\n"
                            + "(and report them, please!)"
            );
            fakePlayerUuid = config.getString("fakePlayerUuid", fakePlayerUuid,
                    "The UUID to use for the global fake player needed for some mod\n"
                            + "interactions."
            );
            fakePlayerName = config.getString("fakePlayerName", fakePlayerName,
                    "The name to use for the global fake player needed for some mod\n"
                            + "interactions."
            );
            fakePlayerProfile = new GameProfile(UUID.fromString(fakePlayerUuid), fakePlayerName);
        }

        public static final class Minecraft extends Category {
            public static boolean enableCommandBlockDriver = false;
            public static boolean allowItemStackNBTTags = false;

            Minecraft(final Category parent) {
                super(parent, "Vanilla integration related settings.");
            }

            @Override
            void load(final ConfigAccess config) {
                enableCommandBlockDriver = config.getBoolean("enableCommandBlockDriver", enableCommandBlockDriver,
                        "Whether to enable the command block driver. Enabling this allows\n"
                                + "computers to set and execute commands via command blocks next to\n"
                                + "adapter blocks. The commands are run using OC's general fake player."
                );
                allowItemStackNBTTags = config.getBoolean("allowItemStackNBTTags", allowItemStackNBTTags,
                        "Whether to allow the item stack converter to push NBT data in"
                                + "compressed format (GZIP'ed). This can be useful for pushing this\n"
                                + "data back to other callbacks. However, given a sophisticated\n"
                                + "enough software (Lua script) it is possible to decode this data,\n"
                                + "and get access to things that should be considered implementation"
                                + "detail / private (mods may keep \"secret\" data in such NBT tags).\n"
                                + "The recommended method is to use the database component instead."
                );
            }
        }
    }

    public static final class Debug extends Category {
        public static boolean logLuaCallbackErrors = false;
        public static boolean forceLuaJ = false;
        public static boolean allowUserdata = true;
        public static boolean allowPersistence = true;
        public static boolean limitMemory = true;
        public static boolean forceCaseInsensitiveFS = false;
        public static boolean logFullLibLoadErrors = false;
        public static String forceNativeLib = "";
        public static boolean logOpenGLErrors = false;
        public static boolean logHexFontErrors = false;
        public static boolean alwaysTryNative = false;
        public static boolean debugPersistence = false;
        public static boolean nativeInTmpDir = false;
        public static boolean periodicallyForceLightUpdate = false;
        public static boolean insertIdsInConverters = false;
        public static boolean registerLuaJArchitecture = false;
        public static boolean disableLocaleChanging = false;

        public static DebugCardAccess debugCardAccess;

        Debug() {
            super("Settings that are intended for debugging issues, not for normal use.\n"
                    + "You usually don't want to touch these unless asked to do so by a developer.");
        }

        @Override
        void load(final ConfigAccess config) {
            forceLuaJ = config.getBoolean("forceLuaJ", forceLuaJ,
                    "Forces the use of the LuaJ fallback instead of the native libraries.\n"
                            + "Use this if you have concerns using native libraries or experience\n"
                            + "issues with the native library."
            );
            logLuaCallbackErrors = config.getBoolean("logCallbackErrors", logLuaCallbackErrors,
                    "This setting is meant for debugging errors that occur in Lua callbacks.\n"
                            + "Per default, if an error occurs and it has a message set, only the\n"
                            + "message is pushed back to Lua, and that's it. If you encounter weird\n"
                            + "errors or are developing an addon you'll want the stacktrace for those\n"
                            + "errors. Enabling this setting will log them to the game log. This is\n"
                            + "disabled per default to avoid spamming the log with inconsequential\n"
                            + "exceptions such as IllegalArgumentExceptions and the like."
            );
            allowUserdata = config.getBoolean("allowUserdata", allowUserdata,
                    "Allows disabling userdata support. This means any otherwise supported\n"
                            + "userdata (implementing the Value interface) will not be pushed\n"
                            + "to the Lua state."
            );
            allowPersistence = config.getBoolean("allowPersistence", allowPersistence,
                    "Allows disabling computer state persistence. This means that computers\n"
                            + "will automatically be rebooted when loaded after being unloaded, instead\n"
                            + "of resuming with their exection (it also means the state is not even"
                            + "saved). Only relevant when using the native library."
            );
            limitMemory = config.getBoolean("limitMemory", limitMemory,
                    "Allows disabling memory limit enforcement. This means Lua states can"
                            + "theoretically use as much memory as they want. Only relevant when"
                            + "using the native library."
            );
            forceCaseInsensitiveFS = config.getBoolean("forceCaseInsensitiveFS", forceCaseInsensitiveFS,
                    "Force the buffered file system to be case insensitive. This makes it\n"
                            + "impossible to have multiple files whose names only differ in their\n"
                            + "capitalization, which is commonly the case on Windows, for example.\n"
                            + "This only takes effect when bufferChanges is set to true."
            );
            logFullLibLoadErrors = config.getBoolean("logFullNativeLibLoadErrors", logFullLibLoadErrors,
                    "Logs the full error when a native library fails to load. This is\n"
                            + "disabled by default to avoid spamming the log, since libraries are\n"
                            + "iterated until one works, so it's very likely for some to fail. Use\n"
                            + "this in case all libraries fail to load even though you'd expect one\n"
                            + "to work."
            );
            forceNativeLib = config.getString("forceNativeLibWithName", forceNativeLib,
                    "Force loading one specific library, to avoid trying to load any\n"
                            + "others. Use this if you get warnings in the log or are told to do\n"
                            + "so for debugging purposes ;-)"
            );
            logOpenGLErrors = config.getBoolean("logOpenGLErrors", logOpenGLErrors,
                    "Used to suppress log spam for OpenGL errors on derpy drivers. I'm\n"
                            + "quite certain the code in the font render is valid, display list\n"
                            + "compatible OpenGL, but it seems to cause 'invalid operation' errors\n"
                            + "when executed as a display list. I'd be happy to be proven wrong,\n"
                            + "since it'd restore some of my trust into AMD drivers..."
            );
            alwaysTryNative = config.getBoolean("alwaysTryNative", alwaysTryNative,
                    "On some platforms the native library can crash the game, so there are\n"
                            + "a few checks in place to avoid trying to load it in those cases. This\n"
                            + "is Windows XP and Windows Server 2003, right. If you think it might\n"
                            + "work nonetheless (newer builds of Server2k3 e.g.) you might want to\n"
                            + "try setting this to `true`. Use this at your own risk. If the game\n"
                            + "crashes as a result of setting this to `true` DO NOT REPORT IT."
            );
            debugPersistence = config.getBoolean("verbosePersistenceErrors", debugPersistence,
                    "This is meant for debugging errors. Enabling this has a high impact\n"
                            + "on computers' save and load performance, so you should not enable\n"
                            + "this unless you're asked to."
            );
            logHexFontErrors = config.getBoolean("logHexFontErrors", logHexFontErrors,
                    "Logs information about malformed glyphs (i.e. glyphs that deviate in"
                            + "width from what wcwidth says)."
            );
            nativeInTmpDir = config.getBoolean("nativeInTmpDir", nativeInTmpDir,
                    "Extract the native library with Lua into the system's temporary\n"
                            + "directory instead of the game directory (e.g. /tmp on Linux). The\n"
                            + "default is to extract into the game directory, to avoid issues when"
                            + "the temporary directory is mounted as noexec (meaning the lib cannot\n"
                            + "be loaded). There is also less of a chance of conflicts when running\n"
                            + "multiple servers or server and client on the same machine."
            );
            periodicallyForceLightUpdate = config.getBoolean("periodicallyForceLightUpdate", periodicallyForceLightUpdate,
                    "Due to a bug in Minecraft's lighting code there's an issue where\n"
                            + "lighting does not properly update near light emitting blocks that are\n"
                            + "fully solid - like screens, for example. This can be annoying when"
                            + "using other blocks that dynamically change their brightness (e.g. for\n"
                            + "the addon mod OpenLights). Enable this to force light emitting blocks\n"
                            + "in oc to periodically (every two seconds) do an update. This should\n"
                            + "not have an overly noticeable impact on performance, but it's disabled\n"
                            + "by default because it is unnecessary in *most* cases."
            );
            insertIdsInConverters = config.getBoolean("insertIdsInConverters", insertIdsInConverters,
                    "Pass along IDs of items and fluids when converting them to a table\n"
                            + "representation for Lua."
            );
            registerLuaJArchitecture = config.getBoolean("registerLuaJArchitecture", registerLuaJArchitecture,
                    "Whether to always register the LuaJ architecture - even if the native\n"
                            + "library is available. In that case it is possible to switch between"
                            + "the two like any other registered architecture."
            );
            disableLocaleChanging = config.getBoolean("disableLocaleChanging", disableLocaleChanging,
                    "Prevent OC calling Lua's os.setlocale method to ensure number\n"
                            + "formatting is the same on all systems it is run on. Use this if you\n"
                            + "suspect this might mess with some other part of Java (this affects\n"
                            + "the native C locale)."
            );
            switch (config.getString("debugCardAccess", "allow",
                    "Enable debug card functionality. This may also be of use for custom\n"
                            + "maps, so it is enabled by default. If you run a server where people\n"
                            + "may cheat in items but should not have op/admin-like rights, you may\n"
                            + "want to set this to false or `deny`. Set this to `whitelist` if you\n"
                            + "want to enable whitelisting of debug card users (managed by command\n"
                            + "/oc_debugWhitelist). This will *not* remove the card, it will just\n"
                            + "make all functions it provides error out."
            )) {
                case "allow": {
                    debugCardAccess = DebugCardAccess.Allowed;
                    break;
                }
                case "deny": {
                    debugCardAccess = DebugCardAccess.Forbidden;
                    break;
                }
                case "whitelist": {
                    final File wlFile = new File(Loader.instance().getConfigDir() + File.separator + "opencomputers" + File.separator + "debug_card_whitelist.txt");
                    debugCardAccess = new DebugCardAccess.Whitelist(wlFile);
                }
                default: {// Fallback to most secure configuration
                    OpenComputers.log().warn("Unknown debug card access type, falling back to `deny`. Allowed values: `allow`, `deny`, `whitelist`.");
                    debugCardAccess = DebugCardAccess.Forbidden;

                }
            }
        }
    }

    // ----------------------------------------------------------------------- //

    public static void initialize(final File file) {
        final Configuration config = new Configuration(file, OpenComputers.Version());
        config.load();

        for (final Category category : CATEGORIES) {
            final String categoryName = category.getCategory();
            config.setCategoryComment(categoryName, category.comment);
            category.load(new ConfigAccess(config, categoryName));
        }

        config.save();
    }

    // ----------------------------------------------------------------------- //

    private static final List<Category> CATEGORIES = new ArrayList<>();

    static {
        CATEGORIES.add(new Client());
        final Category computer = new Computer();
        CATEGORIES.add(computer);
        CATEGORIES.add(new Computer.Lua(computer));
        final Category robot = new Robot();
        CATEGORIES.add(robot);
        CATEGORIES.add(new Robot.Experience(robot));
        CATEGORIES.add(new Robot.Delays(robot));
        final Category power = new Power();
        CATEGORIES.add(power);
        CATEGORIES.add(new Power.Buffer(power));
        CATEGORIES.add(new Power.Cost(power));
        CATEGORIES.add(new Power.Rate(power));
        CATEGORIES.add(new Power.Value(power));
        CATEGORIES.add(new Filesystem());
        CATEGORIES.add(new Internet());
        CATEGORIES.add(new Relay());
        CATEGORIES.add(new Nanomachines());
        CATEGORIES.add(new Printer());
        CATEGORIES.add(new Hologram());
        CATEGORIES.add(new Misc());
        final Category integration = new Integration();
        CATEGORIES.add(integration);
        CATEGORIES.add(new Integration.Minecraft(integration));
        CATEGORIES.add(new Debug());
    }

    // ----------------------------------------------------------------------- //

    private static abstract class Category {
        @Nullable
        private final Category parent;
        private final String comment;

        // ----------------------------------------------------------------------- //

        protected Category(final String comment) {
            this(null, comment);
        }

        protected Category(@Nullable final Category parent, final String comment) {
            this.parent = parent;
            this.comment = comment;
        }

        // ----------------------------------------------------------------------- //

        String getCategory() {
            return (parent != null ? parent.getCategory() + "." : "") + this.getClass().getSimpleName();
        }

        abstract void load(final ConfigAccess config);
    }

    private static final class ConfigAccess {
        private final Configuration config;
        private final String category;

        public ConfigAccess(final Configuration config, final String category) {
            this.config = config;
            this.category = category;
        }

        public boolean getBoolean(final String name, final boolean def, final String comment) {
            return config.getBoolean(name, category, def, comment);
        }

        public int getInt(final String name, final int def, final int min, final int max, final String comment) {
            return config.getInt(name, category, def, min, max, comment);
        }

        public int getInt(final String name, final int def, final String comment) {
            return config.get(category, name, def, comment).getInt();
        }

        public double getDouble(final String name, final double def, final double min, final double max, final String comment) {
            return config.get(category, name, def, comment, min, max).getDouble();
        }

        public double getDouble(final String name, final double def, final String comment) {
            return config.get(category, name, def, comment).getDouble();
        }

        public String getString(final String name, final String def, final String comment) {
            return config.getString(name, category, def, comment);
        }

        public String[] getStringList(final String name, final String[] def, final String comment) {
            return config.getStringList(name, category, def, comment);
        }

        public double[] getDoubleList(final String name, final double[] def, final String comment, final String errorMsg) {
            final double[] list = config.get(category, name, def, comment).getDoubleList();
            if (list.length != def.length) {
                OpenComputers.log().warn(errorMsg);
                return def;
            }
            return list;
        }

        public int[] getIntList(final String name, final int[] def, final String comment, final String errorMsg) {
            final int[] list = config.get(category, name, def, comment).getIntList();
            if (list.length != def.length) {
                OpenComputers.log().warn(errorMsg);
                return def;
            }
            return list;
        }

        public boolean[] getBooleanList(final String name, final boolean[] def, final String comment, final String errorMsg) {
            final boolean[] list = config.get(category, name, def, comment).getBooleanList();
            if (list.length != def.length) {
                OpenComputers.log().warn(errorMsg);
                return def;
            }
            return list;
        }
    }
}