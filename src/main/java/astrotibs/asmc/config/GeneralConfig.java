package astrotibs.asmc.config;

import java.io.File;

import astrotibs.asmc.utility.Reference;
import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.common.config.Configuration;

public class GeneralConfig
{
	public static Configuration config;

	// --- Category: General --- //
	public static boolean versionChecker;
	public static boolean debugMessages;
	
	// --- Category: Ambient --- //
	public static boolean moreRainSounds;
	public static boolean moreCaveSounds;
	
	// --- Category: Blocks --- //
	public static boolean furnaceSounds;
	public static boolean woodendoorSounds_legacy;
	public static boolean woodendoorSounds_modern;
	public static boolean woodentrapdoorSounds;
	public static boolean irondoorSounds;
	public static boolean irontrapdoorSounds;
	public static boolean woodenchestSounds;
	public static boolean enderchestSounds;
	public static boolean fencegateSounds;
	public static boolean enchantmentTableSounds;
	public static boolean lilypadSounds;
	public static boolean noteBlockSounds;
	public static boolean woodenbuttonSounds;
	public static boolean beaconSounds;
	public static boolean soulsandSounds;
	public static boolean netherwartblockSounds;
	public static boolean boneblockSounds;
	public static boolean basaltblockSounds; // v1.0.1
	public static boolean netherrackSounds;
	public static boolean netherquartzoreSounds;
	public static boolean netherbrickSounds;
	public static boolean strippedlogSounds;
	
	// --- Category: Entity --- //
	public static boolean thornsSound;
	public static boolean witchSounds;
	public static boolean squidSounds;
	public static boolean witherskeletonSounds;
	public static boolean horsefeedingSounds;
	public static boolean cowmilkingSounds;
	public static boolean mooshroommilkingSounds;
	public static boolean snowgolemSounds;
	public static boolean zombievillagerSounds;
	
	// --- Category: Items --- //
	public static boolean paintingSounds;
	public static boolean itemframeSounds;
	public static boolean leashSounds;
	public static boolean bucketSounds;
	public static boolean bottleSounds;
	public static boolean endereyelaunchSounds;
	public static boolean endereyeportalSounds;
	public static boolean fishingrodSounds;
	public static boolean farmlandtillSounds;
	public static boolean cropplantingSounds;
	public static boolean cropbreakingSounds;
	public static boolean redstoneSounds;
	
	// --- Category: Player --- //
	public static boolean playerAttackSounds;
	public static boolean playerStrongOnAxe;
	public static boolean playerStrongOnShovel;
	public static boolean playerStrongOnSword;
	public static float playerStrongThreshold;
	public static boolean bookpageSounds;
	public static boolean drowningSounds;
	public static boolean onfireSounds;
	public static boolean swimSounds_legacy;
	public static boolean swimSounds_modern;
	public static boolean armorequipSounds;
	
	
	public static void init(File configFile)
	{
		if (config == null)
		{
			config = new Configuration(configFile);
			loadConfiguration();
		}
	}
	
	protected static void loadConfiguration()
	{
		
		// --- Category: General --- //
	    versionChecker = config.getBoolean("Version Checker", "General", true, "Displays a client-side chat message if there's an update available");
	    debugMessages = config.getBoolean("Debug Messages", "General", false, "Print ALL positional sound occurrences to the console. This includes sound event path, volume, and pitch; and the sound's location, and what block is in that location.");
	    
	    
	    // --- Category: Ambient --- //
	    moreRainSounds = config.getBoolean("More Rain", "Sounds: Ambient", true, "Add the [1.9+] rain sounds (affects client side only)");
	    moreCaveSounds = config.getBoolean("More Cave", "Sounds: Ambient", true, "Add the [1.13+] cave sounds (affects client side only)");
	    
	    
		// --- Category: Blocks --- //
	    furnaceSounds = config.getBoolean("Furnace", "Sounds: Block", true, "Add the [1.9+] lit furnace crackle sounds (affects client side only; disable if you experience tick lag)");
	    woodendoorSounds_legacy = config.getBoolean("Wooden Door: Legacy", "Sounds: Block", false, "Use the [1.9-1.12] Wooden Door sounds (affects client side only)");
	    woodendoorSounds_modern = config.getBoolean("Wooden Door: Modern", "Sounds: Block", true, "Use the [1.13+] Wooden Door sounds (affects client side only)");
	    woodentrapdoorSounds = config.getBoolean("Wooden Trapdoor", "Sounds: Block", true, "Use the [1.9+] Wooden Trapdoor sounds (affects client side only)");
	    irondoorSounds = config.getBoolean("Iron Door", "Sounds: Block", true, "Use the [1.9+] Iron Door sounds (affects client side only)");
	    irontrapdoorSounds = config.getBoolean("Iron Trapdoor", "Sounds: Block", true, "Use the [1.9+] Iron Trapdoor sounds (affects client side only)");
	    woodenchestSounds = config.getBoolean("Wooden Chest", "Sounds: Block", true, "Use the [1.9+] Wooden Chest sounds (affects client side only)");
	    enderchestSounds = config.getBoolean("Ender Chest", "Sounds: Block", true, "Use the [1.10+] Ender Chest sounds (affects client side only)");
	    fencegateSounds = config.getBoolean("Fence Gate", "Sounds: Block", true, "Use the [1.9+] Fence Gate sounds (affects client side only)");
	    enchantmentTableSounds = config.getBoolean("Enchantment Table", "Sounds: Block", true, "Add the [1.10+] sounds when enchanting items (affects client side only)");
	    lilypadSounds = config.getBoolean("Lilypad", "Sounds: Block", true, "Add the [1.9+] sounds when placing a lilypad");
	    noteBlockSounds = config.getBoolean("Note Blocks", "Sounds: Block", true, "Add the [1.14+] Note Block tones (affects client side only)");
	    woodenbuttonSounds = config.getBoolean("Wooden Button", "Sounds: Block", true, "Use the [1.9+] Wooden Button sounds (affects client side only)");
	    beaconSounds = config.getBoolean("Beacon", "Sounds: Block", true, "Add the [1.13+] ambient and start/stop sounds for the Beacon (affects client side only; disable if you experience tick lag)");
	    soulsandSounds = config.getBoolean("Soul Sand", "Sounds: Block", true, "Use the [1.16+] Soul Sand sounds (affects client side only)");
	    netherwartblockSounds = config.getBoolean("Nether Wart", "Sounds: Block", true, "Use the [1.16+] Nether Wart Block sounds (affects client side only)");
	    boneblockSounds = config.getBoolean("Bone", "Sounds: Block", true, "Use the [1.16+] Bone Block sounds (affects client side only)");
	    basaltblockSounds = config.getBoolean("Basalt", "Sounds: Block", true, "Use the [1.16+] Basalt Block sounds (affects client side only)"); // v1.0.1
	    netherrackSounds = config.getBoolean("Netherrack", "Sounds: Block", true, "Use the [1.16+] Netherrack sounds (affects client side only)");
	    netherquartzoreSounds = config.getBoolean("Nether Quartz Ore", "Sounds: Block", true, "Use the [1.16+] Quartz Ore sounds (affects client side only)");
	    netherbrickSounds = config.getBoolean("Nether Brick", "Sounds: Block", true, "Use the [1.16+] Nether Brick sounds (affects client side only)");
	    strippedlogSounds = config.getBoolean("Stripped Log", "Sounds: Block", true, "Use the correct Stripped Log sounds when using Et Futurum or UpToDateMod"); // Disable in 
	    
	    
		// --- Category: Entity --- //
	    squidSounds = config.getBoolean("Squid", "Sounds: Entity", true, "Add the [1.9+] Squid sounds");
	    thornsSound = config.getBoolean("Thorns", "Sounds: Entity", true, "Add the [1.9+] sounds when the Thorns enchantment triggers");
	    witchSounds = config.getBoolean("Witch", "Sounds: Entity", true, "Add the [1.9+] Witch sounds");
	    witherskeletonSounds = config.getBoolean("Wither Skeleton", "Sounds: Entity", true, "Use the [1.11+] Wither Skeleton sounds");
	    horsefeedingSounds = config.getBoolean("Horse Feeding", "Sounds: Entity", true, "Add the [1.9+] Horse feeding sounds");
	    cowmilkingSounds = config.getBoolean("Cow Milking", "Sounds: Entity", true, "Use the [1.9+] Cow milking sounds");
	    mooshroommilkingSounds = config.getBoolean("Mooshroom Milking", "Sounds: Entity", true, "Add the distinct [1.14+] Mooshroom milking/bowling sounds");
	    snowgolemSounds = config.getBoolean("Snow Golem", "Sounds: Entity", true, "Add the distinct [1.9+] Snow Golem sounds");
	    zombievillagerSounds = config.getBoolean("Zombie Villager", "Sounds: Entity", true, "Use the [1.9+] Zombie Villager sounds (affects client side only)");
	    
	    
		// --- Category: Items --- //
	    bottleSounds = config.getBoolean("Bottle", "Sounds: Item", true, "Add the [1.9+] sounds when filling a bottle with water");
	    bucketSounds = config.getBoolean("Bucket", "Sounds: Item", true, "Add the [1.9+] sounds when filling/emptying a bucket with water or lava");
	    endereyelaunchSounds = config.getBoolean("Ender Eye Launch", "Sounds: Item", true, "Use the [1.12+] sound when launching an Ender Eye");
	    endereyeportalSounds = config.getBoolean("Ender Eye Portal", "Sounds: Item", true, "Use the [1.12+] sounds when activating an End portal");
	    itemframeSounds = config.getBoolean("Item Frame", "Sounds: Item", true, "Add the [1.9+] sounds when placing or breaking item frames, and when inserting and rotating items");
	    leashSounds = config.getBoolean("Lead Knot", "Sounds: Item", true, "Add the [1.9+] sounds when tying leads onto fences or removing them");
	    paintingSounds = config.getBoolean("Painting", "Sounds: Item", true, "Add the [1.9+] sounds when placing or breaking paintings");
	    fishingrodSounds = config.getBoolean("Fishing Rod", "Sounds: Item", true, "Add the [1.12+] sounds when casting or retrieving a fishing line");
	    farmlandtillSounds = config.getBoolean("Farmland Tilling", "Sounds: Item", true, "Use the [1.9+] sounds when hoeing soil (affects client side only)");
	    cropplantingSounds = config.getBoolean("Crop Planting", "Sounds: Item", true, "Use the [1.14+] sounds when planting seeds or nether wart");
	    cropbreakingSounds = config.getBoolean("Crop Breaking", "Sounds: Item", true, "Use the [1.14+] sounds when breaking seeds or nether wart (affects client side only)");
	    redstoneSounds = config.getBoolean("Redstone", "Sounds: Item", true, "Use the [1.14+] stone sounds when placing redstone dust");
	    
	    
	    // --- Category: Player --- //
	    playerAttackSounds = config.getBoolean("Attack", "Sounds: Player", true, "Add the [1.9+] melee sounds when attacking entities");
	    playerStrongOnAxe = config.getBoolean("Strong Sound With Axe", "Sounds: Player", false, "Play the \"strong\" attack sound (rather than \"weak\") if the player attacks with an ItemAxe, regardless of the damage dealt");
	    playerStrongOnShovel = config.getBoolean("Strong Sound With Shovel", "Sounds: Player", false, "Play the \"strong\" attack sound (rather than \"weak\") if the player attacks with an ItemSpade, regardless of the damage dealt");
	    playerStrongOnSword = config.getBoolean("Strong Sound With Sword", "Sounds: Player", false, "Play the \"strong\" attack sound (rather than \"weak\") if the player attacks with an ItemSword, regardless of the damage dealt");
	    playerStrongThreshold = config.getFloat("Strong Attack Threshold", "Sounds: Player", 4F, 0F, Float.MAX_VALUE, "Play the \"strong\" attack sound (rather than \"weak\") if the player deals at least this much damage, regardless of the item in hand");
	    bookpageSounds = config.getBoolean("Book", "Sounds: Player", true, "Use the [1.14+] sounds when turning a book page (affects client side only)");
	    drowningSounds = config.getBoolean("Drowning", "Sounds: Player", true, "Add the [1.12+] sounds when taking drowning damage");
	    onfireSounds = config.getBoolean("Burning", "Sounds: Player", true, "Add the [1.12+] sounds when on fire");
	    swimSounds_legacy = config.getBoolean("Swimming: Legacy", "Sounds: Player", false, "Include the swimming sounds used up through [1.12] (affects client side only)");
	    swimSounds_modern = config.getBoolean("Swimming: Modern", "Sounds: Player", true, "Add the [1.13+] swimming sounds (affects client side only)");
	    armorequipSounds = config.getBoolean("Armor Equip", "Sounds: Player", true, "Use the [1.9+] sounds when equipping armor");
	    
	    
	    
	    if (config.hasChanged()) config.save();
	}
	
	@SubscribeEvent
	public void onConfigurationChangedEvent(ConfigChangedEvent.OnConfigChangedEvent event)
	{
		if (event.modID.equalsIgnoreCase(Reference.MOD_ID))
		{
			this.loadConfiguration();
		}
	}
	
}
