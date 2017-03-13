package li.cil.oc.common;

import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.common.item.data.PrintData;
import li.cil.oc.common.template.AssemblerTemplates;
import li.cil.oc.integration.util.ItemCharge;
import li.cil.oc.integration.util.Wrench;
import li.cil.oc.server.driver.Registry;
import li.cil.oc.server.machine.ProgramLocations;
import li.cil.oc.util.Reflection;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.event.FMLInterModComms.IMCEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class IMC {
    public static final String REGISTER_ASSEMBLER_TEMPLATE = "registerAssemblerTemplate";
    public static final String REGISTER_TOOL_DURABILITY_PROVIDER = "registerToolDurabilityProvider";
    public static final String REGISTER_WRENCH_TOOL = "registerWrenchTool";
    public static final String REGISTER_WRENCH_TOOL_CHECK = "registerWrenchToolCheck";
    public static final String REGISTER_ITEM_CHARGE = "registerItemCharge";
    public static final String BLACKLIST_PERIPHERAL = "blacklistPeripheral";
    public static final String BLACKLIST_HOST = "blacklistHost";
    public static final String REGISTER_ASSEMBLER_FILTER = "registerAssemblerFilter";
    public static final String REGISTER_INK_PROVIDER = "registerInkProvider";
    public static final String REGISTER_CUSTOM_POWER_SYSTEM = "registerCustomPowerSystem";
    public static final String REGISTER_PROGRAM_DISK_LABEL = "registerProgramDiskLabel";

    public static final String TAG_NAME = "name";
    public static final String TAG_PROGRAM = "program";
    public static final String TAG_LABEL = "label";
    public static final String TAG_CAN_CHARGE = "canCharge";
    public static final String TAG_CHARGE = "charge";
    public static final String TAG_HOST = "host";
    public static final String TAG_ARCHITECTURES = "architectures";

    public static void handleEvent(final IMCEvent e) {
        for (final FMLInterModComms.IMCMessage message : e.getMessages()) {
            if (Objects.equals(message.key, REGISTER_ASSEMBLER_TEMPLATE) && message.isNBTMessage()) {
                if (message.getNBTValue().hasKey(TAG_NAME, NBT.TAG_STRING)) {
                    OpenComputers.log().info("Registering new assembler template '" + message.getNBTValue().getString(TAG_NAME) + "' from mod " + message.getSender() + ".");
                } else {
                    OpenComputers.log().info("Registering new, unnamed assembler template from mod " + message.getSender() + ".");
                }

                try {
                    AssemblerTemplates.add(message.getNBTValue());
                } catch (final Throwable t) {
                    OpenComputers.log().warn("Failed registering assembler template.", t);
                }
            } else if (Objects.equals(message.key, REGISTER_TOOL_DURABILITY_PROVIDER) && message.isStringMessage()) {
                OpenComputers.log().info("Registering new tool durability provider '" + message.getStringValue() + "' from mod " + message.getSender() + ".");

                try {
                    ToolDurabilityProviders.add(Reflection.getStaticMethod(message.getStringValue(), ToolDurabilityProviders.ToolDurabilityProvider.class));
                } catch (final Throwable t) {
                    OpenComputers.log().warn("Failed registering tool durability provider.", t);
                }
            } else if (Objects.equals(message.key, REGISTER_WRENCH_TOOL) && message.isStringMessage()) {
                OpenComputers.log().info("Registering new wrench usage '" + message.getStringValue() + "' from mod " + message.getSender() + ".");

                try {
                    Wrench.addUsage(Reflection.getStaticMethod(message.getStringValue(), Wrench.WrenchConsumer.class));
                } catch (final Throwable t) {
                    OpenComputers.log().warn("Failed registering wrench usage.", t);
                }
            } else if (Objects.equals(message.key, REGISTER_WRENCH_TOOL_CHECK) && message.isStringMessage()) {
                OpenComputers.log().info("Registering new wrench tool check '" + message.getStringValue() + "' from mod " + message.getSender() + ".");

                try {
                    Wrench.addValidator(Reflection.getStaticMethod(message.getStringValue(), Wrench.WrenchValidator.class));
                } catch (final Throwable t) {
                    OpenComputers.log().warn("Failed registering wrench check.", t);
                }
            } else if (Objects.equals(message.key, REGISTER_ITEM_CHARGE) && message.isNBTMessage()) {
                OpenComputers.log().info("Registering new item charge implementation '" + message.getNBTValue().getString(TAG_NAME) + "' from mod " + message.getSender() + ".");

                try {
                    ItemCharge.add(
                            Reflection.getStaticMethod(message.getNBTValue().getString(TAG_CAN_CHARGE), ItemCharge.ItemChargeValidator.class),
                            Reflection.getStaticMethod(message.getNBTValue().getString(TAG_CHARGE), ItemCharge.ItemCharger.class)
                    );
                } catch (final Throwable t) {
                    OpenComputers.log().warn("Failed registering item charge implementation.", t);
                }
            } else if (Objects.equals(message.key, BLACKLIST_PERIPHERAL) && message.isStringMessage()) {
                OpenComputers.log().info("Blacklisting CC peripheral '" + message.getStringValue() + "' as requested by mod " + message.getSender() + ".");

                if (!Settings.get().peripheralBlacklist.contains(message.getStringValue())) {
                    Settings.get().peripheralBlacklist.add(message.getStringValue());
                }
            } else if (Objects.equals(message.key, BLACKLIST_HOST) && message.isNBTMessage()) {
                OpenComputers.log().info("Blacklisting component '" + message.getNBTValue().getString(TAG_NAME) + "' for host '" + message.getNBTValue().getString(TAG_HOST) + "' as requested by mod " + message.getSender() + ".");

                try {
                    Registry.blacklistHost(new ItemStack(message.getNBTValue().getCompoundTag("item")), Class.forName(message.getNBTValue().getString(TAG_HOST)));
                } catch (final Throwable t) {
                    OpenComputers.log().warn("Failed blacklisting component.", t);
                }
            } else if (Objects.equals(message.key, REGISTER_ASSEMBLER_FILTER) && message.isStringMessage()) {
                OpenComputers.log().info("Registering new assembler template filter '" + message.getStringValue() + "' from mod " + message.getSender() + ".");

                try {
                    AssemblerTemplates.addFilter(message.getStringValue());
                } catch (final Throwable t) {
                    OpenComputers.log().warn("Failed registering assembler template filter.", t);
                }
            } else if (Objects.equals(message.key, REGISTER_INK_PROVIDER) && message.isStringMessage()) {
                OpenComputers.log().info("Registering new ink provider '" + message.getStringValue() + "' from mod " + message.getSender() + ".");

                try {
                    PrintData.addInkProvider(getStaticMethod(message.getStringValue(), ItemStack.class));
                } catch (final Throwable t) {
                    OpenComputers.log().warn("Failed registering ink provider.", t);
                }
            } else if (Objects.equals(message.key, REGISTER_CUSTOM_POWER_SYSTEM) && message.isStringMessage()) {
                OpenComputers.log().info("Was told there is an unknown power system present by mod " + message.getSender() + ".");

                Settings.get().is3rdPartyPowerSystemPresent = Objects.equals(message.getStringValue(), "true");
            } else if (Objects.equals(message.key, REGISTER_PROGRAM_DISK_LABEL) && message.isNBTMessage()) {
                final String program = message.getNBTValue().getString(TAG_PROGRAM);
                final String label = message.getNBTValue().getString(TAG_LABEL);
                final NBTTagList architecturesNbt = message.getNBTValue().getTagList(TAG_ARCHITECTURES, NBT.TAG_STRING);
                final List<String> architectures = new ArrayList<>();
                for (int tagIndex = 0; tagIndex < architecturesNbt.tagCount(); tagIndex++) {
                    final String architecture = architecturesNbt.getStringTagAt(tagIndex);
                    architectures.add(architecture);
                }

                OpenComputers.log().info("Registering new program location mapping for program '" + program + "' being on disk '" + label + "' from mod " + message.getSender() + ".");

                ProgramLocations.addMapping(program, label, architectures);
            } else {
                OpenComputers.log().warn("Got an unrecognized or invalid IMC message '" + message.key + "' from mod " + message.getSender() + ".");
            }
        }
    }
}
