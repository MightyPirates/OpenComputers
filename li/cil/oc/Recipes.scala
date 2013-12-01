package li.cil.oc

import cpw.mods.fml.common.registry.GameRegistry
import net.minecraft.block.Block
import net.minecraft.item.crafting.FurnaceRecipes
import net.minecraft.item.{Item, ItemStack}
import net.minecraftforge.oredict.{ShapelessOreRecipe, ShapedOreRecipe, OreDictionary}

object Recipes {
  def init() {
    val blazeRod = new ItemStack(Item.blazeRod)
    val cactusGreen = new ItemStack(Item.dyePowder, 1, 2)
    val clock = new ItemStack(Item.pocketSundial)
    val comparator = new ItemStack(Item.comparator)
    val diamond = new ItemStack(Item.diamond)
    val dirt = new ItemStack(Block.dirt)
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
    val netherQuarz = new ItemStack(Item.netherQuartz)
    val obsidian = new ItemStack(Block.obsidian)
    val paper = new ItemStack(Item.paper)
    val piston = new ItemStack(Block.pistonStickyBase)
    val redstoneDust = new ItemStack(Item.redstone)
    val redstoneTorch = new ItemStack(Block.torchRedstoneActive)
    val repeater = new ItemStack(Item.redstoneRepeater)
    val roseRed = new ItemStack(Item.dyePowder, 1, 1)

    val alu = Items.alu.createItemStack()
    val cable = Blocks.cable.createItemStack()
    val card = Items.card.createItemStack()
    val chip1 = Items.chip1.createItemStack()
    val chip2 = Items.chip2.createItemStack()
    val chip3 = Items.chip3.createItemStack()
    val circuitBoard = Items.circuitBoardBody.createItemStack()
    val cpu = Items.cpu.createItemStack()
    val cu = Items.cu.createItemStack()
    val disc = Items.disc.createItemStack()
    val floppy = Items.floppyDisk.createItemStack()
    val gpu1 = Items.gpu1.createItemStack()
    val gpu2 = Items.gpu2.createItemStack()
    val gpu3 = Items.gpu3.createItemStack()
    val hdd1 = Items.hdd1.createItemStack()
    val hdd2 = Items.hdd2.createItemStack()
    val hdd3 = Items.hdd3.createItemStack()
    val ironNugget = Items.ironNugget.createItemStack()
    val lanCard = Items.lan.createItemStack()
    val printedCircuitBoard = Items.printedCircuitBoard.createItemStack()
    val ram1 = Items.ram1.createItemStack()
    val ram2 = Items.ram2.createItemStack()
    val ram3 = Items.ram3.createItemStack()
    val rawCircuitBoard = Items.rawCircuitBoard.createItemStack()
    val redstoneCard = Items.rs.createItemStack()
    val transistor = Items.transistor.createItemStack()
    val wlanCard = Items.wlan.createItemStack()

    // ----------------------------------------------------------------------- //

    addRecipe(Blocks.adapter.createItemStack(),
      "x  ",
      "   ",
      "xxx",
      'x', dirt)

    addRecipe(Blocks.capacitor.createItemStack(),
      "x  ",
      "   ",
      "xxx",
      'x', dirt)

    addRecipe(Blocks.diskDrive.createItemStack(),
      "x  ",
      "   ",
      "xxx",
      'x', dirt)

    addRecipe(Blocks.powerDistributor.createItemStack(),
      "x  ",
      "   ",
      "xxx",
      'x', dirt)

    addRecipe(Blocks.powerSupply.createItemStack(),
      "x  ",
      "   ",
      "xxx",
      'x', dirt)

    addRecipe(Blocks.router.createItemStack(),
      "x  ",
      "   ",
      "xxx",
      'x', dirt)

    addRecipe(Blocks.cable.createItemStack(),
      "x  ",
      "   ",
      "xxx",
      'x', dirt)


    addRecipe(Blocks.robotProxy.createItemStack(),
      "x  ",
      "   ",
      "xxx",
      'x', dirt)

    // ----------------------------------------------------------------------- //

    GameRegistry.addShapelessRecipe(new ItemStack(Item.potion), Item.bucketWater, Item.glassBottle)
    GameRegistry.addRecipe(new ShapelessOreRecipe(Items.ironNugget.createItemStack(9), ironIngot))
    GameRegistry.addShapelessRecipe(Items.ironCutter.createItemStack(1), new ItemStack(Item.shears, 1, OreDictionary.WILDCARD_VALUE), ironNugget, new ItemStack(Item.stick))
    GameRegistry.addShapelessRecipe(rawCircuitBoard, Items.ironCutter.createItemStack(), new ItemStack(Block.blockClay), cactusGreen)
    FurnaceRecipes.smelting().addSmelting(rawCircuitBoard.itemID, rawCircuitBoard.getItemDamage, circuitBoard, 1)
    GameRegistry.addRecipe(new ShapelessOreRecipe(printedCircuitBoard, "potionPoison", Item.goldNugget, circuitBoard))

    addRecipe(ironIngot,
      "xxx",
      "xxx",
      "xxx",
      'x', "nuggetIron")

    addRecipe(disc,
      " i ",
      "i i",
      " i ",
      'i', ironNugget)

    addRecipe(transistor,
      "iii",
      "grg",
      " t ",
      'i', ironNugget,
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
      'i', ironNugget,
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
      'i', ironNugget,
      'c', chip1,
      't', transistor,
      'b', printedCircuitBoard,
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

    addRecipe(Items.analyzer.createItemStack(),
      " r ",
      "tcg",
      "tpg",
      'r', redstoneTorch,
      't', transistor,
      'c', chip1,
      'g', goldNugget,
      'p', printedCircuitBoard)

    addRecipe(Blocks.case1.createItemStack(),
      "ipi",
      "bcb",
      "imi",
      'i', ironIngot,
      'p', printedCircuitBoard,
      'b', ironBars,
      'c', cpu,
      'm', chip1)

    addRecipe(Blocks.case2.createItemStack(),
      "gpg",
      "mcm",
      "gpg",
      'g', goldIngot,
      'p', printedCircuitBoard,
      'm', chip2,
      'c', Blocks.case1.createItemStack())

    addRecipe(Blocks.case3.createItemStack(),
      "mpm",
      "dcd",
      "mpm",
      'm', chip3,
      'p', printedCircuitBoard,
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
      'p', printedCircuitBoard,
      'c', chip3,
      'b', blazeRod,
      'q', netherQuarz,
      's', Blocks.screen2.createItemStack())

    addRecipe(Blocks.keyboard.createItemStack(),
      "ggg",
      "gan",
      'g', Items.buttonGroup.createItemStack(),
      'a', Items.arrowKeys.createItemStack(),
      'n', Items.numPad.createItemStack())

    addRecipe(ram1,
      "ccc",
      "bbb",
      'c', chip1,
      'b', printedCircuitBoard)

    addRecipe(ram2,
      "ccc",
      "rbr",
      'c', chip2,
      'r', ram1,
      'b', printedCircuitBoard)

    addRecipe(ram3,
      "ccc",
      "rbr",
      'c', chip3,
      'r', ram2,
      'b', printedCircuitBoard)

    addRecipe(floppy,
      "ili",
      "bdb",
      "ipi",
      'i', ironNugget,
      'l', lever,
      'b', circuitBoard,
      'd', disc,
      'p', paper)

    addRecipe(hdd1,
      "cdi",
      "bdp",
      "cdi",
      'c', chip1,
      'd', disc,
      'i', ironIngot,
      'b', printedCircuitBoard,
      'p', piston)

    addRecipe(hdd2,
      "gdg",
      "cbc",
      "gdg",
      'g', goldIngot,
      'd', hdd1,
      'c', chip2,
      'b', printedCircuitBoard)

    addRecipe(hdd3,
      "cdc",
      "rbr",
      "cdc",
      'c', chip3,
      'd', hdd2,
      'r', ram1,
      'b', printedCircuitBoard)

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
  }

  private def addRecipe(output: ItemStack, args: Any*) = {
    GameRegistry.addRecipe(new ShapedOreRecipe(output, args.map(_.asInstanceOf[AnyRef]): _*))
  }
}
