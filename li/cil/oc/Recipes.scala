package li.cil.oc

import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.registry.GameRegistry
import net.minecraft.block.Block
import net.minecraft.item.crafting.FurnaceRecipes
import net.minecraft.item.{Item, ItemStack}
import net.minecraftforge.oredict.{ShapelessOreRecipe, ShapedOreRecipe}

object Recipes {
  def init() {
    val blazeRod = new ItemStack(Item.blazeRod)
    val boneMeal = new ItemStack(Item.dyePowder, 1, 15)
    val cactusGreen = new ItemStack(Item.dyePowder, 1, 2)
    val clock = new ItemStack(Item.pocketSundial)
    val comparator = new ItemStack(Item.comparator)
    val craftingTable = new ItemStack(Block.workbench)
    val diamond = new ItemStack(Item.diamond)
    val dispenser = new ItemStack(Block.dispenser)
    val emerald = new ItemStack(Item.emerald)
    val enderPearl = new ItemStack(Item.enderPearl)
    val glass = new ItemStack(Block.glass)
    val glowstoneDust = new ItemStack(Item.glowstone)
    val goldIngot = new ItemStack(Item.ingotGold)
    val goldNugget = new ItemStack(Item.goldNugget)
    val ironBars = new ItemStack(Block.fenceIron)
    val ironIngot = new ItemStack(Item.ingotIron)
    val lapis = new ItemStack(Item.dyePowder, 1, 4)
    val lever = new ItemStack(Block.lever)
    val minecartHopper = new ItemStack(Item.minecartHopper)
    val netherQuartz = new ItemStack(Item.netherQuartz)
    val obsidian = new ItemStack(Block.obsidian)
    val paper = new ItemStack(Item.paper)
    val piston = new ItemStack(Block.pistonBase)
    val redstoneDust = new ItemStack(Item.redstone)
    val redstoneTorch = new ItemStack(Block.torchRedstoneActive)
    val repeater = new ItemStack(Item.redstoneRepeater)
    val roseRed = new ItemStack(Item.dyePowder, 1, 1)
    val slimeBall = new ItemStack(Item.slimeBall)
    val spiderEye = new ItemStack(Item.spiderEye)
    val stick = new ItemStack(Item.stick)
    val sugar = new ItemStack(Item.sugar)

    val acid = Items.acid.createItemStack()
    val alu = Items.alu.createItemStack()
    val cable = Blocks.cable.createItemStack()
    val card = Items.card.createItemStack()
    val chip1 = Items.chip1.createItemStack()
    val chip2 = Items.chip2.createItemStack()
    val chip3 = Items.chip3.createItemStack()
    val board = Items.circuitBoard.createItemStack()
    val cpu = Items.cpu.createItemStack()
    val cu = Items.cu.createItemStack()
    val disk = Items.disk.createItemStack()
    val floppy = Items.floppyDisk.createItemStack()
    val gpu1 = Items.gpu1.createItemStack()
    val gpu2 = Items.gpu2.createItemStack()
    val gpu3 = Items.gpu3.createItemStack()
    val hdd1 = Items.hdd1.createItemStack()
    val hdd2 = Items.hdd2.createItemStack()
    val hdd3 = Items.hdd3.createItemStack()
    val lanCard = Items.lan.createItemStack()
    val pcb = Items.pcb.createItemStack()
    val ram1 = Items.ram1.createItemStack()
    val ram2 = Items.ram2.createItemStack()
    val ram3 = Items.ram3.createItemStack()
    val rawBoard = Items.rawCircuitBoard.createItemStack()
    val redstoneCard = Items.rs.createItemStack()
    val transistor = Items.transistor.createItemStack()
    val wlanCard = Items.wlan.createItemStack()

    // ----------------------------------------------------------------------- //

    if (!Loader.isModLoaded("gregtech_addon")) {
      GameRegistry.addRecipe(new ShapelessOreRecipe(Items.ironNugget.createItemStack(9), ironIngot))
    }
    GameRegistry.addShapelessRecipe(rawBoard, Items.cuttingWire.createItemStack(), new ItemStack(Block.blockClay), cactusGreen)
    FurnaceRecipes.smelting().addSmelting(rawBoard.itemID, rawBoard.getItemDamage, board, 0)
    GameRegistry.addRecipe(new ShapelessOreRecipe(acid, Item.bucketWater, sugar, roseRed, slimeBall, spiderEye, boneMeal))
    GameRegistry.addRecipe(new ShapelessOreRecipe(pcb, acid, Item.goldNugget, board))

    addRecipe(ironIngot,
      "xxx",
      "xxx",
      "xxx",
      'x', "nuggetIron")

    addRecipe(disk,
      " i ",
      "i i",
      " i ",
      'i', "nuggetIron")

    addRecipe(transistor,
      "iii",
      "grg",
      " t ",
      'i', "nuggetIron",
      'g', goldNugget,
      'r', redstoneDust,
      't', redstoneTorch)

    addRecipe(chip1,
      "ibi",
      "rtr",
      "ibi",
      'i', "nuggetIron",
      'b', ironBars,
      'r', redstoneDust,
      't', transistor)

    addRecipe(chip2,
      "glg",
      "cdc",
      "glg",
      'g', goldNugget,
      'l', lapis,
      'c', chip1,
      'd', diamond)

    addRecipe(chip3,
      "dmd",
      "cec",
      "dmd",
      'd', glowstoneDust,
      'm', comparator,
      'c', chip2,
      'e', emerald)

    addRecipe(alu,
      "rtr",
      "sss",
      "idi",
      'r', repeater,
      's', transistor,
      't', redstoneTorch,
      'i', "nuggetIron",
      'd', redstoneDust)

    addRecipe(cu,
      "gtg",
      "scs",
      "gdg",
      'g', goldNugget,
      't', redstoneTorch,
      's', transistor,
      'c', clock,
      'd', redstoneDust)

    addRecipe(cpu,
      "cdc",
      "bub",
      "cac",
      'c', chip1,
      'd', redstoneDust,
      'b', ironBars,
      'u', cu,
      'a', alu)

    addRecipe(card,
      "ict",
      "ibb",
      "igg",
      'i', "nuggetIron",
      'c', chip1,
      't', transistor,
      'b', pcb,
      'g', goldNugget)

    addRecipe(Items.buttonGroup.createItemStack(),
      "bbb",
      "bbb",
      'b', new ItemStack(Block.stoneButton))

    addRecipe(Items.arrowKeys.createItemStack(),
      " b ",
      "bbb",
      'b', new ItemStack(Block.stoneButton))

    addRecipe(Items.numPad.createItemStack(),
      "bbb",
      "bbb",
      "bbb",
      'b', new ItemStack(Block.stoneButton))

    // ----------------------------------------------------------------------- //

    addRecipe(Blocks.case1.createItemStack(),
      "ipi",
      "bcb",
      "imi",
      'i', ironIngot,
      'p', pcb,
      'b', ironBars,
      'c', cpu,
      'm', chip1)

    addRecipe(Blocks.case2.createItemStack(),
      "gpg",
      "mcm",
      "gpg",
      'g', goldIngot,
      'p', pcb,
      'm', chip2,
      'c', Blocks.case1.createItemStack())

    addRecipe(Blocks.case3.createItemStack(),
      "mpm",
      "dcd",
      "mpm",
      'm', chip3,
      'p', pcb,
      'd', diamond,
      'c', Blocks.case2.createItemStack())

    addRecipe(Blocks.screen1.createItemStack(),
      "iig",
      "rtg",
      "iig",
      'i', ironIngot,
      'g', glass,
      'r', redstoneDust,
      't', transistor)

    addRecipe(Blocks.screen2.createItemStack(),
      "iri",
      "cgs",
      "ibi",
      'i', goldIngot,
      'r', roseRed,
      'c', chip2,
      'g', cactusGreen,
      's', Blocks.screen1.createItemStack(),
      'b', lapis)

    addRecipe(Blocks.screen3.createItemStack(),
      "opc",
      "bqs",
      "opc",
      'o', obsidian,
      'p', pcb,
      'c', chip3,
      'b', blazeRod,
      'q', netherQuartz,
      's', Blocks.screen2.createItemStack())

    addRecipe(Blocks.capacitor.createItemStack(),
      "iti",
      "gpg",
      "ibi",
      'i', ironIngot,
      't', transistor,
      'g', goldNugget,
      'p', paper,
      'b', pcb)

    addRecipe(Blocks.powerDistributor.createItemStack(),
      "ici",
      "wgw",
      "ibi",
      'i', ironIngot,
      'c', chip1,
      'w', cable,
      'g', goldIngot,
      'b', pcb)

    addRecipe(Blocks.powerConverter.createItemStack(),
      "iwi",
      "gcg",
      "ibi",
      'i', ironIngot,
      'c', chip1,
      'w', cable,
      'g', goldIngot,
      'b', pcb)

    addRecipe(Blocks.diskDrive.createItemStack(),
      "ici",
      "ps ",
      "ici",
      'i', ironIngot,
      'c', chip1,
      'p', piston,
      's', stick)

    addRecipe(Blocks.router.createItemStack(),
      "ini",
      "ncn",
      "ibi",
      'i', ironIngot,
      'n', lanCard,
      'c', chip1,
      'b', pcb)

    addRecipe(Blocks.adapter.createItemStack(),
      "iwi",
      "wcw",
      "ibi",
      'i', ironIngot,
      'w', cable,
      'c', chip1,
      'b', pcb)

    addRecipe(Blocks.charger.createItemStack(),
      "igi",
      "pcp",
      "ibi",
      'i', ironIngot,
      'g', goldIngot,
      'p', Blocks.capacitor.createItemStack(),
      'c', chip2,
      'b', pcb)

    addRecipe(Blocks.robotProxy.createItemStack(),
      "sgf",
      "dcr",
      "bmb",
      's', Blocks.screen1.createItemStack(),
      'g', gpu1,
      'f', Blocks.diskDrive.createItemStack(),
      'd', dispenser,
      'c', Blocks.case1.createItemStack(),
      'r', ram1,
      'b', Blocks.capacitor.createItemStack(),
      'm', minecartHopper)

    addRecipe(Blocks.keyboard.createItemStack(),
      "ggg",
      "gan",
      'g', Items.buttonGroup.createItemStack(),
      'a', Items.arrowKeys.createItemStack(),
      'n', Items.numPad.createItemStack())

    addRecipe(Blocks.cable.createItemStack(4),
      " i ",
      "iri",
      " i ",
      'i', "nuggetIron",
      'r', redstoneDust)

    // ----------------------------------------------------------------------- //

    addRecipe(Items.cuttingWire.createItemStack(),
      "sis",
      's', stick,
      'i', "nuggetIron")

    addRecipe(Items.analyzer.createItemStack(),
      " r ",
      "tcg",
      "tpg",
      'r', redstoneTorch,
      't', transistor,
      'c', chip1,
      'g', goldNugget,
      'p', pcb)

    addRecipe(ram1,
      "ccc",
      "bbb",
      'c', chip1,
      'b', pcb)

    addRecipe(ram2,
      "ccc",
      "rbr",
      'c', chip2,
      'r', ram1,
      'b', pcb)

    addRecipe(ram3,
      "ccc",
      "rbr",
      'c', chip3,
      'r', ram2,
      'b', pcb)

    addRecipe(floppy,
      "ili",
      "bdb",
      "ipi",
      'i', "nuggetIron",
      'l', lever,
      'b', board,
      'd', disk,
      'p', paper)

    addRecipe(hdd1,
      "cdi",
      "bdp",
      "cdi",
      'c', chip1,
      'd', disk,
      'i', ironIngot,
      'b', pcb,
      'p', piston)

    addRecipe(hdd2,
      "gdg",
      "cbc",
      "gdg",
      'g', goldIngot,
      'd', hdd1,
      'c', chip2,
      'b', pcb)

    addRecipe(hdd3,
      "cdc",
      "rbr",
      "cdc",
      'c', chip3,
      'd', hdd2,
      'r', ram1,
      'b', pcb)

    addRecipe(gpu1,
      "car",
      " b ",
      'c', chip1,
      'a', alu,
      'r', ram1,
      'b', card)

    addRecipe(gpu2,
      "ccr",
      " g ",
      'c', chip2,
      'r', ram2,
      'g', gpu1)

    addRecipe(gpu3,
      "ccr",
      " g ",
      'c', chip3,
      'r', ram3,
      'g', gpu2)

    addRecipe(redstoneCard,
      "tc ",
      " b ",
      't', redstoneTorch,
      'c', chip1,
      'b', card)

    addRecipe(lanCard,
      "wc ",
      " b ",
      'w', cable,
      'c', chip1,
      'b', card)

    addRecipe(wlanCard,
      "pc ",
      " b ",
      'p', enderPearl,
      'c', chip2,
      'b', lanCard)

    addRecipe(Items.generator.createItemStack(),
      "i i",
      "cpc",
      "bib",
      'i', ironIngot,
      'c', chip1,
      'p', piston,
      'b', pcb)

    addRecipe(Items.crafting.createItemStack(),
      "ipi",
      "cwc",
      "ibi",
      'i', ironIngot,
      'p', piston,
      'c', chip1,
      'w', craftingTable,
      'b', pcb)
  }

  private def addRecipe(output: ItemStack, args: Any*) = {
    GameRegistry.addRecipe(new ShapedOreRecipe(output, args.map(_.asInstanceOf[AnyRef]): _*))
  }
}
