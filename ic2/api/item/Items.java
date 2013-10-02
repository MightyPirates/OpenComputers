package ic2.api.item;

import net.minecraft.item.ItemStack;

/**
 * Provides access to IC2 blocks and items.
 * 
 * Some items can be acquired through the ore dictionary which is the recommended way.
 * The items are initialized while IC2 is being loaded - try to use ModsLoaded() or load your mod after IC2.
 * Some blocks/items can be disabled by a config setting, so it's recommended to check if they're null first.
 * 
 * Getting the associated Block/Item for an ItemStack x:
 *   Blocks: Block.blocksList[x.itemID]
 *   Items: x.getItem()
 */
public final class Items {
	/**
	 * Get an ItemStack for a specific item name, example: Items.getItem("resin")
	 * See the list below for item names.
	 * Make sure to copy() the ItemStack if you want to modify it.
	 *
	 * @param name item name
	 * @return The item or null if the item does not exist or an error occurred
	 */
	public static ItemStack getItem(String name) {
		try {
			if (Ic2Items == null) Ic2Items = Class.forName(getPackage() + ".core.Ic2Items");

			Object ret = Ic2Items.getField(name).get(null);

			if (ret instanceof ItemStack) {
				return (ItemStack) ret;
			} else {
				return null;
			}
		} catch (Exception e) {
			System.out.println("IC2 API: Call getItem failed for "+name);

			return null;
		}
	}

	/* Possible values:

	----- blocks -----

	ores
		copperOre 				Copper Ore block, currently not meta sensitive, meta in ItemStack set to 0, ore dictionary: oreCopper, null with enableWorldGenOreCopper=false
		tinOre 					Tin Ore block, currently not meta sensitive, meta in ItemStack set to 0, ore dictionary: oreTin, null with enableWorldGenOreTin=false
		uraniumOre 				Tin Ore block, currently not meta sensitive, meta in ItemStack set to 0, ore dictionary: oreUranium, null with enableWorldGenOreUranium=false

	rubber related
		Rubber wood block, meta reflects the state, meta in ItemStack set to 0, ore dictionary: woodRubber (with meta 0), null with enableWorldGenTreeRubber=false
		dropped (as an item)  -> metadata 0
		block, no resin spot  -> metadata 0 or 1
		block, wet resin spot -> metadata 2-5 (according to the side)
		block, dry resin spot -> metadata 8-11 (wet state + 6)

		rubberWood
		rubberLeaves 			Rubber Leaves block, currently not meta sensitive, meta in ItemStack set to 0, null with enableWorldGenTreeRubber=false
		rubberSapling 			Rubber Sapling block, currently not meta sensitive, meta in ItemStack set to 0, null with enableWorldGenTreeRubber=false
		resinSheet 				Resin Sheet block, currently not meta sensitive
		rubberTrampoline 		Rubber Trampoline block, meta reflects internal state, meta in ItemStack set to 0

	building/storage
		ironFence 				Iron Fence block, currently not meta sensitive

		reinforcedStone 		Reinforced Stone block, currently not meta sensitive
		reinforcedGlass 		Reinforced Glass block, currently not meta sensitive
		reinforcedDoorBlock 	Reinforced Door block, meta reflects the state (see vanilla doors), meta in ItemStack set to 0

		constructionFoam 		Construction Foam block, currently not meta sensitive
		constructionFoamWall 	Construction Foam Wall block, meta = color, implements IPaintableBlock
		scaffold 				Scaffold block, meta reflects internal physical model data

		bronzeBlock 			Bronze block, meta sensitive
		copperBlock 			Copper block, meta sensitive
		tinBlock 				Tin block, meta sensitive
		uraniumBlock 			Uranium block, meta sensitive

	cables (when placed as a block, inventory items are different TE implements IEnergyConductor)
		copperCableBlock 				Copper Cable block, meta sensitive
		insulatedCopperCableBlock 		Insulated Copper Cable block, meta sensitive

		goldCableBlock 					Gold Cable block, meta sensitive
		insulatedGoldCableBlock 		Insulated Gold Cable block, meta sensitive
		doubleInsulatedGoldCableBlock 	Double Insulated Gold Cable block, meta sensitive

		ironCableBlock 					Iron Cable block, meta sensitive
		insulatedIronCableBlock 		Insulated Iron Cable block, meta sensitive
		doubleInsulatedIronCableBlock 	Double Insulated Iron Cable block, meta sensitive
		trippleInsulatedIronCableBlock 	Tripple Insulated Iron Cable block, meta sensitive

		glassFiberCableBlock 			Glass Fiber Cable block, meta sensitive

		tinCableBlock 					Tin Cable block, meta sensitive

		detectorCableBlock 				Detector Cable block, meta sensitive
		splitterCableBlock 				Splitter Cable block, meta sensitive

	generators + related (TE implements IEnergySource ex. reactorChamber)
		generator 				Generator block, meta sensitive
		geothermalGenerator 	Geothermal Generator block, meta sensitive
		waterMill 				Water Mill block, meta sensitive
		solarPanel 				Solar Panel block, meta sensitive
		windMill 				Wind Mill block, meta sensitive
		nuclearReactor 			Nuclear Reactor block, meta sensitive
		reactorChamber 			Reactor Chamber block, currently not meta sensitive

	energy storages (TE implements IEnergySource and IEnergyConductor)
		batBox 					BatBox block, meta sensitive
		mfeUnit 				MFE Unit block, meta sensitive
		mfsUnit 				MFS Unit block, meta sensitive

	transformers (TE implements IEnergySource and IEnergyConductor)
		lvTransformer 			LV Transformer block, meta sensitive
		mvTransformer 			MV Transformer block, meta sensitive
		hvTransformer 			HV Transformer block, meta sensitive

	machines + related (TE implements IEnergySink ex. machine, miningPipe, miningPipeTip)
		machine 				Machine block, meta sensitive
		advancedMachine 		Advanced Machine block, meta sensitive

		ironFurnace 			Iron Furnace block, meta sensitive
		electroFurnace 			Electro Furnace block, meta sensitive
		macerator 				Macerator block, meta sensitive
		extractor 				Extractor block, meta sensitive
		compressor 				Compressor block, meta sensitive
		canner 					Canner block, meta sensitive
		miner 					Miner block, meta sensitive
		pump 					Pump block, meta sensitive
		magnetizer				Magnetizer block, meta sensitive
		electrolyzer 			Electrolyzer block, meta sensitive
		recycler 				Recycler block, meta sensitive
		inductionFurnace 		Induction Furnace block, meta sensitive
		massFabricator 			Mass Fabricator block, meta sensitive
		terraformer				Terraformer block, meta sensitive
		teleporter 				Teleporter block, meta sensitive
		teslaCoil 				Tesla Coil block, meta sensitive
		luminator 				Passive (dark) Luminator block, meta = facing
		activeLuminator 		Active (bright) Luminator block, meta = facing

		miningPipe 				Mining Pipe block, currently not meta sensitive, meta in ItemStack set to 0
		miningPipeTip 			Mining Pipe Tip block, currently not meta sensitive, meta in ItemStack set to 0

	personal blocks
		personalSafe 			Personal Safe block, meta sensitive
		tradeOMat 				Trade-O-Mat block, meta sensitive
		energyOMat 				Energy-O-Mat block, meta sensitive

	explosives
		industrialTnt 			Industrial TNT block, currently not meta sensitive
		nuke 					Nuke block, currently not meta sensitive
		dynamiteStick 			Dynamite Stick block, meta = placement, meta in ItemStack set to 0
		dynamiteStickWithRemote Dynamite Stick with Remote block, meta = placement, meta in ItemStack set to 0

	Agriculture Stuff
		crop 					Crop Block, empty, not meta sensitive


	----- items -----

	rubber + related
		resin 					Resin item, currently not meta sensitive
		rubber 					Rubber item, currently not meta sensitive, ore dictionary: itemRubber

	ore drops
		uraniumDrop 			Uranium Drop item, currently not meta sensitive, ore dictionary: itemDropUranium

	dusts
		bronzeDust 				Bronze Dust item, currently not meta sensitive
		clayDust 				Clay Dust item, currently not meta sensitive
		coalDust 				Coal Dust item, currently not meta sensitive
		copperDust 				Copper Dust item, currently not meta sensitive
		goldDust 				Gold Dust item, currently not meta sensitive
		ironDust 				Iron Dust item, currently not meta sensitive
		silverDust 				Silver Dust item, currently not meta sensitive
		smallIronDust 			Small Iron Dust item, currently not meta sensitive
		tinDust 				Tin Dust item, currently not meta sensitive
		hydratedCoalDust 		Hydrated Coal Dust item, currently not meta sensitive

	ingots
		refinedIronIngot 		Refined Iron Ingot item, currently not meta sensitive, ore dictionary: ingotRefinedIron
		copperIngot 			Copper Ingot item, currently not meta sensitive, ore dictionary: ingotCopper
		tinIngot 				Tin Ingot item, currently not meta sensitive, ore dictionary: ingotTin
		bronzeIngot 			Bronze Ingot item, currently not meta sensitive, ore dictionary: ingotBronze
		mixedMetalIngot 		Mixed Metal Ingot item, currently not meta sensitive
		uraniumIngot 			Uranium Ingot item, currently not meta sensitive, ore dictionary: ingotUranium

	tools/weapons (without electric tools)
		treetap 					Treetap item, meta = damage value
		wrench 						Wrench item, meta = damage value
		cutter 						Insulation Cutter item, meta = damage value
		constructionFoamSprayer 	Construction Foam Sprayer item, meta = charges (as of v1.45)

		bronzePickaxe 				Bronze Pickaxe item, meta = damage value
		bronzeAxe 					Bronze Axe item, meta = damage value
		bronzeSword 				Bronze Sword item, meta = damage value
		bronzeShovel 				Bronze Shovel item, meta = damage value
		bronzeHoe 					Bronze Hoe item, meta = damage value

	el. tools/devices/weapons
		miningDrill 			Mining Drill item, meta = visual charge indicator, implements IElectricItem
		diamondDrill 			Diamond Tipped Mining Drill item, meta = visual charge indicator, implements IElectricItem
		chainsaw 				Chainsaw item, meta = visual charge indicator, implements IElectricItem
		electricWrench			Electric Wrench item, meta = visual charge indicator, implements IElectricItem
		electricTreetap			Electric Treetap item, meta = visual charge indicator, implements IElectricItem
		miningLaser 			Mining Laser item, meta = visual charge indicator, implements IElectricItem

		ecMeter 				EC-Mater item, currently not meta sensitive
		odScanner 				Ore Density Scanner item, meta = damage value for charge level, implements IElectricItem
		ovScanner 				Ore Value Scanner item, meta = visual charge indicator, implements IElectricItem

		frequencyTransmitter 	Frequency Transmitter item, currently not meta sensitive

		nanoSaber 				Idle Nano Saber item, meta = visual charge indicator, implements IElectricItem
		enabledNanoSaber 		Enabled Nano Saber item, meta = visual charge indicator, implements IElectricItem

	armor/wearable
		rubberBoots 			Rubber Boots item, meta = damage value

		bronzeHelmet 			Bronze Helmet Armor item, meta = damage value
		bronzeChestplate 		Bronze Chestplate Armor item, meta = damage value
		bronzeLeggings 			Bronze Leggings Armor item, meta = damage value
		bronzeBoots 			Bronze Boots Armor item, meta = damage value

		compositeArmor 			Composite Armor item, meta = damage value for charge level

		nanoHelmet 				Nano Helmet Armor item, meta = visual charge indicator, implements IElectricItem
		nanoBodyarmor 			Nano Bodyarmor item, meta = visual charge indicator, implements IElectricItem
		nanoLeggings 			Nano Leggings Armor item, meta = visual charge indicator, implements IElectricItem
		nanoBoots 				Nano Boots Armor item, meta = visual charge indicator, implements IElectricItem

		quantumHelmet 			Quantum Helmet Armor item, meta = visual charge indicator, implements IElectricItem
		quantumBodyarmor 		Quantum Bodyarmor item, meta = visual charge indicator, implements IElectricItem
		quantumLeggings 		Quantum Leggings Armor item, meta = visual charge indicator, implements IElectricItem
		quantumBoots 			Quantum Boots Armor item, meta = visual charge indicator, implements IElectricItem

		jetpack 				Jetpack item, meta = damage value for fuel level
		electricJetpack 		Electric Jetpack item, meta = visual charge indicator, implements IElectricItem

		batPack 				BatPack item, meta = visual charge indicator, implements IElectricItem, can provide energy
		lapPack 				LapPack item, meta = visual charge indicator, implements IElectricItem, can provide energy

		cfPack 					CF Pack item, meta = charges (as of v1.45)

		solarHelmet				Solar Helmet item, currently not meta sensitive
		staticBoots				Static Boots item, currently not meta sensitive

	batteries
		reBattery 				Empty RE Battery item, currently not meta sensitive, implements IElectricItem
		chargedReBattery 		RE Battery item, meta = visual charge indicator, implements IElectricItem, can provide energy
		energyCrystal 			Energy Crystal item, meta = visual charge indicator, implements IElectricItem, can provide energy
		lapotronCrystal 		Lapotron Crystal item, meta = visual charge indicator, implements IElectricItem, can provide energy
		suBattery 				SU Battery item, currently not meta sensitive

	cables
		copperCableItem 				Copper Cable item, meta sensitive
		insulatedCopperCableItem 		Insulated Copper Cable item, meta sensitive

		goldCableItem 					Gold Cable item, meta sensitive
		insulatedGoldCableItem 			Insulated Gold Cable item, meta sensitive
		doubleInsulatedGoldCableItem 	Double Insulated Gold Cable item, meta sensitive

		ironCableItem 					Iron Cable item, meta sensitive
		insulatedIronCableItem 			Insulated Iron Cable item, meta sensitive
		doubleInsulatedIronCableItem 	Double Insulated Iron Cable item, meta sensitive
		trippleInsulatedIronCableItem 	Tripple Insulated Iron Cable item, meta sensitive

		glassFiberCableItem 			Glass Fiber Cable item, meta sensitive

		tinCableItem 					Tin Cable item, meta sensitive

		detectorCableItem 				Detector Cable item, meta sensitive
		splitterCableItem 				Splitter Cable item, meta sensitive

	cells/containers (without reactor components)
		cell 					Empty Cell item, currently not meta sensitive
		lavaCell 				Lava Cell item, currently not meta sensitive
		hydratedCoalCell 		Hydrated Coal Cell item, currently not meta sensitive
		bioCell 				Bio Cell item, currently not meta sensitive
		coalfuelCell 			Coalfuel Cell item, currently not meta sensitive
		biofuelCell 			Biofuel Cell item, currently not meta sensitive
		waterCell 				Water Cell item, currently not meta sensitive
		electrolyzedWaterCell 	Electrolyzed Water Cell item, currently not meta sensitive

		fuelCan 				Empty Fuel Can item, currently not meta sensitive
		filledFuelCan 			Fuel Can item, meta = fuel value (as of v1.45)

		tinCan 					Empty Tin Can item, currently not meta sensitive
		filledTinCan 			Filled Tin Can item, currently not meta sensitive

	reactor components
		uraniumCell 				Uranium Cell item, meta = damage value
		coolingCell 				Cooling Cell item, meta = damage value

		depletedIsotopeCell 		Depleted Isotope Cell item, meta = damage value
		reEnrichedUraniumCell 		Re-Enriched Uranium Cell item, currently not meta sensitive
		nearDepletedUraniumCell		Near-Depleted Uranium Cell item, currently not meta sensitive

		integratedReactorPlating 	Integrated Reactor Plating item, meta = damage value
		integratedHeatDisperser		Integrated Heat Disperser item, meta = damage value

	terraformer blueprints
		terraformerBlueprint 				Empty Terraformer Blueprint item, currently not meta sensitive
		cultivationTerraformerBlueprint 	Cultivation Terraformer Blueprint item, currently not meta sensitive
		irrigationTerraformerBlueprint 		Irrigation Terraformer Blueprint item, currently not meta sensitive
		chillingTerraformerBlueprint 		Chilling Terraformer Blueprint item, currently not meta sensitive
		desertificationTerraformerBlueprint Desertification Terraformer Blueprint item, currently not meta sensitive
		flatificatorTerraformerBlueprint 	Flatificator Terraformer Blueprint item, currently not meta sensitive
		mushroomTerraformerBlueprint 	    Mushroom Terraformer Blueprint item, currently not meta sensitive

	diamond chain
		coalBall 				Coal Ball item, currently not meta sensitive
		compressedCoalBall 		Compressed Coal Ball item, currently not meta sensitive
		coalChunk 				Coal Chunk item, currently not meta sensitive
		industrialDiamond 		Industrial Diamond item, currently not meta sensitive, DEPRECATED

	recycler chain
		scrap 					Scrap item, currently not meta sensitive
		scrapBox 				Scrap Box item, currently not meta sensitive

	fuel production chain
		hydratedCoalClump 		Hydrated Coal Clump item, currently not meta sensitive
		plantBall 				Plant Ball item, currently not meta sensitive
		compressedPlantBall 	Compressed Plant Ball item, currently not meta sensitive

	painting
		painter 				Painter item, currently not meta sensitive

		blackPainter 			Black Painter item, meta = damage value
		redPainter 				Red Painter item, meta = damage value
		greenPainter 			Green Painter item, meta = damage value
		brownPainter 			Brown Painter item, meta = damage value
		bluePainter 			Blue Painter item, meta = damage value
		purplePainter 			Purple Painter item, meta = damage value
		cyanPainter 			Cyan Painter item, meta = damage value
		lightGreyPainter 		Light Grey Painter item, meta = damage value
		darkGreyPainter 		Dark Grey Painter item, meta = damage value
		pinkPainter 			Pink Painter item, meta = damage value
		limePainter 			Lime Painter item, meta = damage value
		yellowPainter 			Yellow Painter item, meta = damage value
		cloudPainter 			Cloud Painter item, meta = damage value
		magentaPainter 			Magenta Painter item, meta = damage value
		orangePainter 			Orange Painter item, meta = damage value
		whitePainter 			White Painter item, meta = damage value

	explosives + related
		dynamite 				Throwable Dynamite item, currently not meta sensitive
		stickyDynamite 			Throwable Sticky Dynamite item, currently not meta sensitive

		remote 					Dynamite Remote item, currently not meta sensitive

	misc intermediate recipe ingredients
		electronicCircuit 		Electronic Circuit item, currently not meta sensitive
		advancedCircuit 		Advanced Circuit item, currently not meta sensitive

		advancedAlloy 			Advanced Alloy item, currently not meta sensitive

		carbonFiber 			Raw Carbon Fiber item, currently not meta sensitive
		carbonMesh 				Raw Carbon Mesh item, currently not meta sensitive
		carbonPlate 			Carbon Plate item, currently not meta sensitive

		matter 					UU-Matter item, currently not meta sensitive
		iridiumOre 				Iridium Ore item, currently not meta sensitive
		iridiumPlate 			Iridium Plate item, currently not meta sensitive

	upgrade modules
		overclockerUpgrade		overclocker upgrade item, meta sensitive
		transformerUpgrade		transformer upgrade item, meta sensitive
		energyStorageUpgrade	energy storage upgrade item, meta sensitive

	misc
		coin 					Coin item, currently not meta sensitive
		reinforcedDoor 			Reinforced Door item, currently not meta sensitive
		constructionFoamPellet 	Construction Foam Pellet item, currently not meta sensitive
		cropSeed 				Crop seeds, stuff stored in NBT, don't use for crafting recipes!
		cropnalyzer				Cropnalyzer handheld device
		fertilizer				Basic IC2Item, used to provide nutrients toCropBlocks
		hydratingCell			Cell used to hydrate Crops, meta = Content, 0 = Full, 9999 = Near empty
		electricHoe				Electric Hoe, meta = charge level
		solarHelmet				Solar Helmet item, currently not meta sensitive
		terraWart				Terra Wart item, cures potion effects
		weedEx					Weed-EX can, meta = uses left

	 */

	/**
	 * Get the base IC2 package name, used internally.
	 * 
	 * @return IC2 package name, if unable to be determined defaults to ic2
	 */
	private static String getPackage() {
		Package pkg = Items.class.getPackage();

		if (pkg != null) {
			String packageName = pkg.getName();

			return packageName.substring(0, packageName.length() - ".api.item".length());
		}

		return "ic2";
	}

	private static Class<?> Ic2Items;
}

