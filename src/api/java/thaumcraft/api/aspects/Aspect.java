package thaumcraft.api.aspects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.apache.commons.lang3.text.WordUtils;

public class Aspect {
	
	String tag;
	Aspect[] components;
	int color;
	private String chatcolor;
	ResourceLocation image;
	int blend;

	/**
	 * Use this constructor to register your own aspects.
	 * @param tag the key that will be used to reference this aspect, as well as its latin display name
	 * @param color color to display the tag in
	 * @param components the aspects this one is formed from
	 * @param image ResourceLocation pointing to a 32x32 icon of the aspect
	 * @param blend GL11 blendmode (1 or 771). Used for rendering nodes. Default is 1
	 */
	public Aspect(String tag, int color, Aspect[] components, ResourceLocation image, int blend) {
		if (aspects.containsKey(tag)) throw new IllegalArgumentException(tag+" already registered!");
		this.tag = tag;
		this.components = components;
		this.color = color;
		this.image = image;
		this.blend = blend;
		aspects.put(tag, this);
	}
	
	/**
	 * Shortcut constructor I use for the default aspects - you shouldn't be using this.
	 */
	public Aspect(String tag, int color, Aspect[] components) {
		this(tag,color,components,new ResourceLocation("thaumcraft","textures/aspects/"+tag.toLowerCase()+".png"),1);
	}
	
	/**
	 * Shortcut constructor I use for the default aspects - you shouldn't be using this.
	 */
	public Aspect(String tag, int color, Aspect[] components, int blend) {
		this(tag,color,components,new ResourceLocation("thaumcraft","textures/aspects/"+tag.toLowerCase()+".png"),blend);
	}

	/**
	 * Shortcut constructor I use for the primal aspects - 
	 * you shouldn't use this as making your own primal aspects will break all the things.
	 */
	public Aspect(String tag, int color, String chatcolor, int blend) {
		this(tag,color,(Aspect[])null, blend);
		this.setChatcolor(chatcolor);
	}
	
	public int getColor() {
		return color;
	}
	
	public String getName() {
		return WordUtils.capitalizeFully(tag);
	}
	
	public String getLocalizedDescription() {
		return StatCollector.translateToLocal("tc.aspect."+tag);
	}
	
	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public Aspect[] getComponents() {
		return components;
	}

	public void setComponents(Aspect[] components) {
		this.components = components;
	}
	
	public ResourceLocation getImage() {
		return image;
	}
	
	public static Aspect getAspect(String tag) {
		return aspects.get(tag);
	}
	
	public int getBlend() {
		return blend;
	}

	public void setBlend(int blend) {
		this.blend = blend;
	}
	
	public boolean isPrimal() {
		return getComponents()==null || getComponents().length!=2;
	}
	
	///////////////////////////////
	public static ArrayList<Aspect> getPrimalAspects() {
		ArrayList<Aspect> primals = new ArrayList<Aspect>();
		Collection<Aspect> pa = aspects.values();
		for (Aspect aspect:pa) {
			if (aspect.isPrimal())  primals.add(aspect);
		}
		return primals;
	}
	
	public static ArrayList<Aspect> getCompoundAspects() {
		ArrayList<Aspect> compounds = new ArrayList<Aspect>();
		Collection<Aspect> pa = aspects.values();
		for (Aspect aspect:pa) {
			if (!aspect.isPrimal())  compounds.add(aspect);
		}
		return compounds;
	}

	public String getChatcolor() {
		return chatcolor;
	}

	public void setChatcolor(String chatcolor) {
		this.chatcolor = chatcolor;
	}
	

	///////////////////////////////
	public static LinkedHashMap<String,Aspect> aspects = new LinkedHashMap<String,Aspect>();
	
	//PRIMAL
		public static final Aspect AIR = new Aspect("aer",0xffff7e,"e",1);
		public static final Aspect EARTH = new Aspect("terra",0x56c000,"2",1);
		public static final Aspect FIRE = new Aspect("ignis",0xff5a01,"c",1);
		public static final Aspect WATER = new Aspect("aqua",0x3cd4fc,"3",1);
		public static final Aspect ORDER = new Aspect("ordo",0xd5d4ec,"7",1);
		public static final Aspect ENTROPY = new Aspect("perditio",0x404040,"8",771);
	
	//SECONDARY  	
		public static final Aspect VOID = new Aspect("vacuos",0x888888, new Aspect[] {AIR, ENTROPY},771);
		public static final Aspect LIGHT = new Aspect("lux",0xfff663, new Aspect[] {AIR, FIRE});
		public static final Aspect WEATHER = new Aspect("tempestas",0xFFFFFF, new Aspect[] {AIR, WATER});
		public static final Aspect MOTION = new Aspect("motus",0xcdccf4, new Aspect[] {AIR, ORDER});
		public static final Aspect COLD = new Aspect("gelum",0xe1ffff, new Aspect[] {FIRE, ENTROPY});
		public static final Aspect CRYSTAL = new Aspect("vitreus",0x80ffff, new Aspect[] {EARTH, ORDER});
		public static final Aspect LIFE = new Aspect("victus",0xde0005, new Aspect[] {WATER, EARTH});
		public static final Aspect POISON = new Aspect("venenum",0x89f000,  new Aspect[] {WATER, ENTROPY});	
		public static final Aspect ENERGY = new Aspect("potentia",0xc0ffff, new Aspect[] {ORDER, FIRE});
		public static final Aspect EXCHANGE = new Aspect("permutatio",0x578357, new Aspect[] {ENTROPY, ORDER});
//		public static final Aspect ?? = new Aspect("??",0xcdccf4, new Aspect[] {AIR, EARTH});
//		public static final Aspect ?? = new Aspect("??",0xcdccf4, new Aspect[] {FIRE, EARTH});
//		public static final Aspect ?? = new Aspect("??",0xcdccf4, new Aspect[] {FIRE, WATER});
//		public static final Aspect ?? = new Aspect("??",0xcdccf4, new Aspect[] {ORDER, WATER});
//		public static final Aspect ?? = new Aspect("??",0xcdccf4, new Aspect[] {EARTH, ENTROPY});
		
	//TERTIARY  			
		public static final Aspect METAL = new Aspect("metallum",0xb5b5cd, new Aspect[] {EARTH, CRYSTAL});
		public static final Aspect DEATH = new Aspect("mortuus",0x887788, new Aspect[] {LIFE, ENTROPY});
		public static final Aspect FLIGHT = new Aspect("volatus",0xe7e7d7, new Aspect[] {AIR, MOTION});
		public static final Aspect DARKNESS = new Aspect("tenebrae",0x222222, new Aspect[] {VOID, LIGHT});
		public static final Aspect SOUL = new Aspect("spiritus",0xebebfb, new Aspect[] {LIFE, DEATH});
		public static final Aspect HEAL = new Aspect("sano",0xff2f34, new Aspect[] {LIFE, ORDER});
		public static final Aspect TRAVEL = new Aspect("iter",0xe0585b, new Aspect[] {MOTION, EARTH});
		public static final Aspect ELDRITCH = new Aspect("alienis",0x805080, new Aspect[] {VOID, DARKNESS});
		public static final Aspect MAGIC = new Aspect("praecantatio",0x9700c0, new Aspect[] {VOID, ENERGY});
		public static final Aspect AURA = new Aspect("auram",0xffc0ff, new Aspect[] {MAGIC, AIR});
		public static final Aspect TAINT = new Aspect("vitium",0x800080, new Aspect[] {MAGIC, ENTROPY});
		public static final Aspect SLIME = new Aspect("limus",0x01f800, new Aspect[] {LIFE, WATER});
		public static final Aspect PLANT = new Aspect("herba",0x01ac00, new Aspect[] {LIFE, EARTH});
		public static final Aspect TREE = new Aspect("arbor",0x876531, new Aspect[] {AIR, PLANT});		
		public static final Aspect BEAST = new Aspect("bestia",0x9f6409, new Aspect[] {MOTION, LIFE});
		public static final Aspect FLESH = new Aspect("corpus",0xee478d, new Aspect[] {DEATH, BEAST});
		public static final Aspect UNDEAD = new Aspect("exanimis",0x3a4000, new Aspect[] {MOTION, DEATH});
		public static final Aspect MIND = new Aspect("cognitio",0xffc2b3, new Aspect[] {EARTH, SOUL});
		public static final Aspect SENSES = new Aspect("sensus",0x0fd9ff, new Aspect[] {AIR, SOUL});
		public static final Aspect MAN = new Aspect("humanus",0xffd7c0, new Aspect[] {BEAST, MIND});
		public static final Aspect CROP = new Aspect("messis",0xe1b371, new Aspect[] {PLANT, MAN});		
		public static final Aspect MINE = new Aspect("perfodio",0xdcd2d8, new Aspect[] {MAN, EARTH});
		public static final Aspect TOOL = new Aspect("instrumentum",0x4040ee, new Aspect[] {MAN, ORDER});
		public static final Aspect HARVEST = new Aspect("meto",0xeead82, new Aspect[] {CROP, TOOL});
		public static final Aspect WEAPON = new Aspect("telum",0xc05050, new Aspect[] {TOOL, ENTROPY});
		public static final Aspect ARMOR = new Aspect("tutamen",0x00c0c0, new Aspect[] {TOOL, EARTH});
		public static final Aspect HUNGER = new Aspect("fames",0x9a0305, new Aspect[] {LIFE, VOID});
		public static final Aspect GREED = new Aspect("lucrum",0xe6be44, new Aspect[] {MAN, HUNGER});
		public static final Aspect CRAFT = new Aspect("fabrico",0x809d80, new Aspect[] {MAN, TOOL});
		public static final Aspect CLOTH = new Aspect("pannus",0xeaeac2, new Aspect[] {TOOL, BEAST});
		public static final Aspect MECHANISM = new Aspect("machina",0x8080a0, new Aspect[] {MOTION, TOOL});
		public static final Aspect TRAP = new Aspect("vinculum",0x9a8080, new Aspect[] {MOTION, ENTROPY});				
		
		
}
