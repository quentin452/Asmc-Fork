package astrotibs.asmc.sounds;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.common.collect.Lists;

import astrotibs.asmc.config.GeneralConfig;
import astrotibs.asmc.ieep.PlayerArmorTracker;
import astrotibs.asmc.proxy.ClientProxy;
import astrotibs.asmc.utility.LogHelper;
import astrotibs.asmc.utility.Reference;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockButtonWood;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockEndPortalFrame;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockNetherWart;
import net.minecraft.block.BlockNetherrack;
import net.minecraft.block.BlockSoulSand;
import net.minecraft.block.BlockStoneSlab;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSound;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.GuiEnchantment;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.EntityLeashKnot;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.item.EntityEnderEye;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntitySnowman;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityMooshroom;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerEnchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.potion.Potion;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Direction;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.client.event.sound.PlaySoundEvent17;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.PlaySoundAtEntityEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;

public class SoundEventHandler
{
	private static double enderEyeSoundRadius = 18.0D;
	// Only a fraction of blocks produce the crackle every tick, so only search one every N. I'm hard-coding this to minimize computation footprint.
    final static int everyNblocks = 10*10; // The furnace is ticked for every 10 world ticks, and then only plays the sound 10% of the time.
    final static int furnaceSoundRadius = 15; // Search range, in blocks, that this event looks for active furnaces
    
    // The way I'm going to do this is to randomly generate a fraction of positions from the search cube.
    // This equates to generating (2*soundRange+1)^3 / everyNblocks unique numbers.
    // Then I de-convolve those numbers to 3D positions, and if there's a furnace at that position, I play the crackle noise.
    // The actual calculation takes a bit more nuance.
        
    // Step 1: figure out how many total blocks will be searched.
    final static int totalBlocks = ((2*furnaceSoundRadius)+1)*((2*furnaceSoundRadius)+1)*((2*furnaceSoundRadius)+1); // Total number of blocks in the search cube
    // Step 2: find out how many positions we'll check, rounded up.
    final static int numPosToCheck = (totalBlocks+everyNblocks-1)/everyNblocks;
    
    private static ItemStack itemInSlot; // Used to determine whether something changed in the interface
    private static int currPage = -1; // Used to determine if a page was turned in a book
    
    // Some magic machinery for scanning for beacons
    private static float beaconSoundRadius = 8F; // Distance from which you can hear an ambient beacon
    private static float beaconRadiusSqrd = beaconSoundRadius*beaconSoundRadius;
    private static int xToScan = 0;
    private static int yToScan = 0;
    private static int zToScan = 0;
    private static HashMap<List<Integer>, Block>  beaconmap;
    
    
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    //public void test(GuiScreenEvent.DrawScreenEvent event)
    public void onClientTick(ClientTickEvent event)
    {
    	// --- START OF PHASE --- //
    	
    	if (event.phase == Phase.START)
    	{
    		
    		// --- Beacon --- //
        	if (
        			Minecraft.getMinecraft().theWorld != null
        			&& Minecraft.getMinecraft().theWorld.getTotalWorldTime() % 40L == 0
        			&& GeneralConfig.beaconSounds
        			)
        	{
        		EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
    			World world = Minecraft.getMinecraft().theWorld;
        		
    	    	// Systematically check all blocks within a squared distance less than 12 (distance less than 3.46)
    	        for (int xScan = MathHelper.ceiling_double_int(player.posX-beaconSoundRadius); xScan <= MathHelper.floor_double(player.posX+beaconSoundRadius); xScan++)
    	        {
    	        	double yzCircleDist = xScan-player.posX; // current search plane's distance from the parrot in the x direction
    	        	
    	        	for (int yScan = MathHelper.ceiling_double_int(player.posY-MathHelper.sqrt_double(beaconRadiusSqrd-yzCircleDist*yzCircleDist)); yScan <= MathHelper.floor_double(player.posY+MathHelper.sqrt_double(beaconRadiusSqrd-yzCircleDist*yzCircleDist)); yScan++)
    	        	{
    	        		double zSliceDist = yScan-player.posY; // Current search row's distance from the parrot in the y direction
    	        		
    	        		for (int zScan = MathHelper.ceiling_double_int(player.posZ-MathHelper.sqrt_double(beaconRadiusSqrd-yzCircleDist*yzCircleDist-zSliceDist*zSliceDist)); zScan <= MathHelper.floor_double(player.posZ+MathHelper.sqrt_double(beaconRadiusSqrd-yzCircleDist*yzCircleDist-zSliceDist*zSliceDist)); zScan++)
    	        		{
    	        			TileEntity tileentity = world.getTileEntity(xScan, yScan, zScan);
    	        			
    	        			if (
    	        					//world.getBlock(xScan, yScan, zScan) instanceof BlockBeacon
    	        					tileentity != null
    	        					&& tileentity instanceof TileEntityBeacon
    	        					)
    	        			{
    	        				TileEntityBeacon teBeacon = (TileEntityBeacon)tileentity;
    	        				boolean beaconThinksItIsOn = teBeacon.getLevels() > 0;
    	        				boolean beaconIsOnBruteFrc = false; 
    	        				
    	        				// Scan the blocks underneath and determine whether the beam SHOULD be on
    	        				int j = 1;
            	                int yBaseScan = teBeacon.yCoord - j;
            	                boolean flag = true; // Trick to breaking out of a multi-level for loop
            	                
            	                if (yBaseScan < 0) {flag = false;} // Beneath is void, so this beacon must be off
            	                
            	                // Scan the 3x3 beneath
            	                for (int xBaseScan = teBeacon.xCoord - j; xBaseScan <= teBeacon.xCoord + j && flag; ++xBaseScan)
            	                {
            	                    for (int zBaseScan = teBeacon.zCoord - j; zBaseScan <= teBeacon.zCoord + j; ++zBaseScan)
            	                    {
            	                        Block block = world.getBlock(xBaseScan, yBaseScan, zBaseScan);

            	                        if (!block.isBeaconBase(world, xBaseScan, yBaseScan, zBaseScan, teBeacon.xCoord, teBeacon.yCoord, teBeacon.zCoord))
            	                        {
            	                        	// The beacon cannot be on.
            	                            flag = false;
            	                            break;
            	                        }
            	                    }
            	                }
            	                
            	                if (flag) {beaconIsOnBruteFrc = true;}
    	        				
    	        				if (beaconThinksItIsOn)
    	        				{
    		        				// If the beacon thinks it's on and it is on, then play the ambient sound.
    	        					if (beaconIsOnBruteFrc)
    	        					{
    	        						// This is done similarly to the Guarian's beam attack
    	        						ClientProxy.handleBeaconSound(teBeacon);
    	        					}
    		        				// If the beacon thinks it's on but it's actually off, then it just switched off.
    	        					else if (Minecraft.getMinecraft().theWorld.getTotalWorldTime() % 80L == 0)
    	        					{
    	        						world.playSound(
    	        								xScan, yScan, zScan,
    	                						Reference.MOD_ID+":block.beacon.deactivate",
    	                						1F,
    	                						1F,
    	                						false);
    	        					}
    	        				}
    	        				// If the beacon thinks it's off but it's actually on, then it just switched on.
    	        				else if (beaconIsOnBruteFrc)
    	        				{
    	        					if (Minecraft.getMinecraft().theWorld.getTotalWorldTime() % 80L == 0)
    	        					{
    	        						world.playSound(
    	        								xScan, yScan, zScan,
    	                						Reference.MOD_ID+":block.beacon.activate",
    	                						1F,
    	                						1F,
    	                						false);
    	        					}
    	        				}
    	        			}
    	        		}
    	        	}
    	        }
        	}
        }
    	
    	// --- END OF PHASE --- //
    	if (event.phase == Phase.END)
    	{
    		EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
			World world = Minecraft.getMinecraft().theWorld;
			
    		
			// --- Furnace crackle --- //
			if (
					!Minecraft.getMinecraft().isGamePaused()
					&& world != null
					&& player != null
					&& GeneralConfig.furnaceSounds
					)
			{
				// Generating a list and going through it takes more computational power (thus FPS) than generating multiple random values.
        		// The downside is that rarely, a position can be double-ticked. I barely care.
				for (int p = 0; p < numPosToCheck ; p++)
				{ 
					int codedPosition = world.rand.nextInt(numPosToCheck*everyNblocks-1);
					
            		if (codedPosition <= (totalBlocks-1)) // Occasionally, a position will be outside the cube, and that's fine. Just reject it.
            		{
            			// Step 3: decode the integer to an x, y, z position
            			
            			// These range from -15 to +15 in x, y, z
            			int sx = (codedPosition / ( ((2*furnaceSoundRadius)+1)*((2*furnaceSoundRadius)+1) )) - furnaceSoundRadius; // X position is unpacked
            			int sy = codedPosition % ( ((2*furnaceSoundRadius)+1)*((2*furnaceSoundRadius)+1) ); // this is Y and Z convolved
            			int sz = (sy % ((2*furnaceSoundRadius)+1)) - furnaceSoundRadius; // Z position is unpacked
            			sy = (sy / ((2*furnaceSoundRadius)+1)) - furnaceSoundRadius; // Y position is unpacked
            			
            			
            			// Step 4: add the player's position as offset.
            			int fx = sx + MathHelper.floor_double(player.posX);
            			int fy = sy + MathHelper.floor_double(player.posY + player.eyeHeight);
            			int fz = sz + MathHelper.floor_double(player.posZ);
            			
            			
            			// Step 5: only continue if the position is inside the sphere AND inside the world:
            			if (
            					fy >= 0 // Greater than the void height
            					&& fy <= world.getHeight() // Less than the sky height
            					&& (sx*sx + sy*sy + sz*sz) <= furnaceSoundRadius*furnaceSoundRadius // Within the search sphere -- cuts the list into about half
            					&& world.getBlock(fx, fy, fz) == Blocks.lit_furnace // Is a lit furnace
            					)
            			{
            				world.playSound(
          						  fx + 0.5
          						, fy + 0.5
          						, fz + 0.5
          						, Reference.MOD_ID+":block.furnace.fire_crackle",
          						1.0F, 1.0F, false);
            			}
            		}
				}
			}
    		
    		
    		// --- Enchantment Table --- //
    		if (
        			Minecraft.getMinecraft().currentScreen instanceof GuiEnchantment
    				&& GeneralConfig.enchantmentTableSounds
    				)
    		{
    			// give @p minecraft:wooden_pickaxe 6
    			
        		GuiEnchantment gui = (GuiEnchantment)Minecraft.getMinecraft().currentScreen;
        		
        		ContainerEnchantment containerEnchantment_reflected = ReflectionHelper.getPrivateValue(GuiEnchantment.class, gui, "field_147075_G");
        		
        		if (this.itemInSlot == null) // Dummy item is null
        		{
        			if (containerEnchantment_reflected.tableInventory.getStackInSlot(0) != null) // but there is something in the slot
        			{
        				// Update the dummy item to match what's in the slot
        				this.itemInSlot = containerEnchantment_reflected.tableInventory.getStackInSlot(0);
        			}
        		}
        		else // The Dummy item is something
        		{
        			if (containerEnchantment_reflected.tableInventory.getStackInSlot(0) == null) // the  slot is empty
        			{
        				// The item was taken out of the slot.
        				this.itemInSlot = null;
        			}
        			else if (
        						!ItemStack.areItemStacksEqual(this.itemInSlot, containerEnchantment_reflected.tableInventory.getStackInSlot(0)) // Slot stack has changed
        					) // The slot item is different now
        			{
        				if (
        						!this.itemInSlot.isItemEnchanted() && containerEnchantment_reflected.tableInventory.getStackInSlot(0).isItemEnchanted() // to something enchanted
        						&& this.itemInSlot.getItem() == containerEnchantment_reflected.tableInventory.getStackInSlot(0).getItem() // But the item is the same
        						&& this.itemInSlot.getItemDamage() == containerEnchantment_reflected.tableInventory.getStackInSlot(0).getItemDamage() // with the same damage
        						)
        				{
        					
        					
        					this.itemInSlot = containerEnchantment_reflected.tableInventory.getStackInSlot(0);
            				
            				if (world!=null && player!=null)
                            {
                    			world.playSound(
                						player.posX, player.posY, player.posZ,
                						Reference.MOD_ID+":block.enchantment_table.use",
                						1.0F, // Accidentally had this as 0.1F
                						world.rand.nextFloat() * 0.1F + 0.9F,
                						false);
                            }
        				}
        				else
            			{
            				this.itemInSlot = containerEnchantment_reflected.tableInventory.getStackInSlot(0);
            			}
        			}
        			
        		}
        		
        		return;
    		}
    		
    		
    		
    		// Reset the dummy item -- used especially to make the Enchanting Table work
    		if (this.itemInSlot != null) {this.itemInSlot = null;}
    		if (this.currPage != -1) {this.currPage = -1;}
    	}
    }
    
    
    /**
     * This is the method used to REPLACE sound effects.
     * This is done client-side, so must be flagged as such, and the config options must say so.
     */
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onPlaySound(PlaySoundEvent17 event)
    {
    	if (GeneralConfig.debugMessages && Minecraft.getMinecraft().theWorld!=null)
    	{
    		LogHelper.info(
    				event.sound.getPositionedSoundLocation().getResourceDomain()+":"+event.name
    				+ ", volume: " + event.sound.getVolume()
    				+ ", pitch: " + event.sound.getPitch()
    				+ ", at block " + Minecraft.getMinecraft().theWorld.getBlock(MathHelper.floor_float(event.sound.getXPosF()), MathHelper.floor_float(event.sound.getYPosF()), MathHelper.floor_float(event.sound.getZPosF()))	
    				+ " at " + event.sound.getXPosF() + " " + event.sound.getYPosF() + " " + event.sound.getZPosF()
    			);
    	}
    	
    	
    	// --- Cancel sounds originating from the player --- //
		if (
				(
				
				// --- Cancel damage sound when drowning or on fire --- //
				(event.name.equals("game.player.hurt")
				&& (GeneralConfig.drowningSounds || GeneralConfig.onfireSounds)) ||
				
				// --- Cancel fishing line --- //
				(event.name.equals("random.bow")
						&& (GeneralConfig.fishingrodSounds))
						)
				
				&& event.sound.getPositionedSoundLocation().getResourceDomain().equals("minecraft")
				)
		{
			float soundX = event.sound.getXPosF();
			float soundY = event.sound.getYPosF();
			float soundZ = event.sound.getZPosF();
			
			SoundManager sndManager_reflected = ReflectionHelper.getPrivateValue(SoundHandler.class, Minecraft.getMinecraft().getSoundHandler(), new String[]{"field_147694_f", "sndManager"});
			Map playingSounds_reflected = ReflectionHelper.getPrivateValue(SoundManager.class, sndManager_reflected, new String[]{"field_148629_h", "playingSounds"});
			
			Iterator itr = playingSounds_reflected.keySet().iterator();
			while(itr.hasNext())
			{
				PositionedSound positionedSound;
				
				// Because Darian experienced a crash relating to CofH
				try {positionedSound = (PositionedSound)playingSounds_reflected.get(itr.next());}
				catch (Exception e) {continue;}
				
				ResourceLocation resourceLocation_reflected = ReflectionHelper.getPrivateValue(PositionedSound.class, positionedSound, "field_147664_a");
				
				if (
						// The new sound is playing
						resourceLocation_reflected.getResourceDomain().equals(Reference.MOD_ID)
						&& (
								(event.name.equals("game.player.hurt") &&
								(resourceLocation_reflected.getResourcePath().equals("entity.player.hurt_drown")
								|| resourceLocation_reflected.getResourcePath().equals("entity.player.hurt_on_fire"))) ||
								
								(event.name.equals("random.bow") &&
								(resourceLocation_reflected.getResourcePath().equals("entity.fishing_bobber.throw")))
								)
						&& (
								((soundX-positionedSound.getXPosF())*(soundX-positionedSound.getXPosF()) +
								(soundY-positionedSound.getYPosF())*(soundY-positionedSound.getYPosF()) +
								(soundZ-positionedSound.getZPosF())*(soundZ-positionedSound.getZPosF())) <= 0.05F 
								)
						)
				{
					// The "hit" sound is coming from within about a quarter-block distance from the "drown" sound, so we'll assume they're the same cause
					event.result = null;
					return;
				}
			}
		}
    	
    	// --- Book page turn --- //
    	if (
    			Minecraft.getMinecraft().currentScreen instanceof GuiScreenBook
    			&& event.name.equals("gui.button.press")
    			&& GeneralConfig.bookpageSounds
    			)
    	{
    		if (
        			Minecraft.getMinecraft().currentScreen instanceof GuiScreenBook
    				//&& GeneralConfig.bookpageSounds
    				)
    		{
    			GuiScreenBook gui = (GuiScreenBook)Minecraft.getMinecraft().currentScreen;
        		
    			int currPage_reflected = ReflectionHelper.getPrivateValue(GuiScreenBook.class, gui, new String[]{"field_146484_x", "currPage"});
    			
    			// If there is a disagreement on page, play the page-turning sound
    			if (currPage_reflected != this.currPage)
    			{
    				this.currPage = currPage_reflected;
    				
    				EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
    	        	World world = Minecraft.getMinecraft().theWorld;
    	        	
    				player.playSound(
    						Reference.MOD_ID+":item.book.page_turn",
    						1.0F,
    						world.rand.nextFloat() * 0.1F + 0.9F
    						);
    				
    				event.result = null;
    				return;
    			}
    		}    		
    	}
    	
    	if (
    			!Minecraft.getMinecraft().isGamePaused()
				&& Minecraft.getMinecraft().theWorld != null
				&& Minecraft.getMinecraft().thePlayer != null
    			)
    	{
    		EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
        	World world = Minecraft.getMinecraft().theWorld;
        	
        	float soundX = event.sound.getXPosF();
        	float soundY = event.sound.getYPosF();
        	float soundZ = event.sound.getZPosF();
        	
    		Block block_at_sound = world.getBlock(
    				MathHelper.floor_float(soundX),
    				MathHelper.floor_float(soundY-0.01F), // Slightly below so that top-surface sounds detect the block underneath
    				MathHelper.floor_float(soundZ));
    		
    		
    		// Check vanilla sounds
    		if (event.sound.getPositionedSoundLocation().getResourceDomain().equals("minecraft"))
    		{
    			
    			// --- Zombie Villager --- //
    			if (GeneralConfig.zombievillagerSounds)
    			{
    				float zombieBoxRange = 0.25F;
    				
    				List zombieList = world.getEntitiesWithinAABB(
		    				EntityZombie.class, AxisAlignedBB.getBoundingBox(
		    						soundX - zombieBoxRange, soundY - zombieBoxRange, soundZ - zombieBoxRange,
		    						soundX + zombieBoxRange, soundY + zombieBoxRange, soundZ + zombieBoxRange
		    						)
		    				);
    				
    				Iterator iterator = zombieList.iterator();
	        		
	        		while (iterator.hasNext())
	        		{
	        			EntityZombie zombie = (EntityZombie)iterator.next();
	        			if (zombie.isVillager())
	        			{
	        				// --- Ambient --- //
	        				if (event.name.equals("mob.zombie.say"))
	                		{
	        					world.playSound(
	            						soundX, soundY, soundZ,
	            						Reference.MOD_ID+":entity.zombie_villager.ambient",
	            						event.sound.getVolume(),
	            						event.sound.getPitch(),
	            						false);
	            				event.result = null;
	            				return;
	                		}
	        				// --- Death --- //
	        				if (event.name.equals("mob.zombie.death"))
	                		{
	        					world.playSound(
	            						soundX, soundY, soundZ,
	            						Reference.MOD_ID+":entity.zombie_villager.death",
	            						event.sound.getVolume(),
	            						event.sound.getPitch(),
	            						false);
	            				event.result = null;
	            				return;
	                		}
	        				// --- Hurt --- //
	        				if (event.name.equals("mob.zombie.hurt"))
	                		{
	        					world.playSound(
	            						soundX, soundY, soundZ,
	            						Reference.MOD_ID+":entity.zombie_villager.hurt",
	            						event.sound.getVolume(),
	            						event.sound.getPitch(),
	            						false);
	            				event.result = null;
	            				return;
	                		}
	        			}
	        		}
    				
    				
    			}
    			
    			
    			// --- Player Swim --- //
        		if (event.name.equals("game.player.swim")
        				&& GeneralConfig.swimSounds_modern)
        		{
        			world.playSound(
    						soundX, soundY, soundZ,
    						Reference.MOD_ID+":entity.player.swim" + (GeneralConfig.swimSounds_legacy ? ".full" : ""),
    						event.sound.getVolume(),
    						event.sound.getPitch(),
    						false);
    				event.result = null;
    				return;
        		}
        		// --- Player Splash --- //
        		if (event.name.equals("game.player.swim.splash")
        				&& GeneralConfig.swimSounds_modern)
        		{
        			float playerBoxRange = 0.25F;
    				
    				List playerList = world.getEntitiesWithinAABB(
		    				EntityPlayer.class, AxisAlignedBB.getBoundingBox(
		    						soundX - playerBoxRange, soundY - playerBoxRange, soundZ - playerBoxRange,
		    						soundX + playerBoxRange, soundY + playerBoxRange, soundZ + playerBoxRange
		    						)
		    				);
    				Iterator iterator = playerList.iterator();
	        		
	        		while (iterator.hasNext())
	        		{
	        			EntityPlayer playerselected = (EntityPlayer)iterator.next();
	        			
	        			// Water-striking speed to determine whether to play the large splash sound
	        			float doWaterSplashEffect_f1 = (MathHelper.sqrt_double(
            					playerselected.motionX * playerselected.motionX * 0.20000000298023224D
            					+ playerselected.motionY * playerselected.motionY
            					+ playerselected.motionZ * playerselected.motionZ * 0.20000000298023224D
            					)) * (playerselected.riddenByEntity==null ? 0.2F : 0.9F);
	        			
	        			if (doWaterSplashEffect_f1 > 1.0F) {doWaterSplashEffect_f1 = 1.0F;}
	        			
	        			// Play fast splash sound instead
	        			if (doWaterSplashEffect_f1 >= 0.25D)
	        			{
	        				world.playSound(
	        						soundX, soundY, soundZ,
	        						Reference.MOD_ID+":entity.player.splash.high_speed",
	        						doWaterSplashEffect_f1,
	        						event.sound.getPitch(),
	        						false);
	        				event.result = null;
	        				return;
	        			}
	        		}
        		}
        		
        		
        		// --- Cancel old Hoe sound --- //
        		if (
        				(block_at_sound==Blocks.dirt || block_at_sound==Blocks.grass)
        				
        				&& world.getBlock(
        	    				MathHelper.floor_float(soundX),
        	    				MathHelper.floor_float(soundY+1-0.01F), // Slightly below so that top-surface sounds detect the block underneath
        	    				MathHelper.floor_float(soundZ))==Blocks.air
        				
	        			&& event.name.equals("step.gravel")
        				&& GeneralConfig.farmlandtillSounds)
        		{
        			// Ray trace and ensure that SOME player is operating on this block
        			
        			List<EntityPlayer> allPlayers = Lists.<EntityPlayer>newArrayList();
					
			        for (Object entity : player.worldObj.playerEntities)
			        {
			            if ( (EntityPlayer.class).isAssignableFrom(entity.getClass()) )
			            {
			            	allPlayers.add((EntityPlayer)entity);
			            }
			        }
			        
			        for (EntityPlayer playerInServerList : allPlayers)
			        {
			        	MovingObjectPosition movingobjectposition = getMovingObjectPositionFromPlayer(world, playerInServerList, true);
			        	
			        	if (movingobjectposition == null) {return;}
						else
						{
							if (movingobjectposition.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK)
							{
								if (
										playerInServerList.getHeldItem() != null
				        				&& playerInServerList.getHeldItem().getItem() instanceof ItemHoe
				        				&& movingobjectposition.blockX==MathHelper.floor_float(soundX)
				        				&& movingobjectposition.blockY==MathHelper.floor_float(soundY-0.01F)
				        				&& movingobjectposition.blockZ==MathHelper.floor_float(soundZ)
										)
								{
									event.result = null;
			        				return;
								}
							}
						}
			        }
        		}
        		
        		
        		
        		// --- Cancel modded Stripped log sound --- //
        		if (
        				block_at_sound.getMaterial() == Material.wood
        				&& block_at_sound.getClass().toString().substring(6).toLowerCase().contains("stripped")
        				
        				&& ((block_at_sound.getClass().toString().substring(6).toLowerCase().contains("etfuturum")
	        					&& Loader.isModLoaded("etfuturum")
	        					&& event.name.equals("step.wood"))
	        					||
	    					(block_at_sound.getClass().toString().substring(6).toLowerCase().contains("uptodatemod")
	            					&& Loader.isModLoaded("uptodate")
	    							&& event.name.equals("dig.cloth"))
	        					)
        				
        				&& GeneralConfig.strippedlogSounds)
        		{
        			
        			// Ray trace and ensure that SOME player is operating on this block
        			
        			List<EntityPlayer> allPlayers = Lists.<EntityPlayer>newArrayList();
					
			        for (Object entity : player.worldObj.playerEntities)
			        {
			            if ( (EntityPlayer.class).isAssignableFrom(entity.getClass()) )
			            {
			            	allPlayers.add((EntityPlayer)entity);
			            }
			        }
			        
			        for (EntityPlayer playerInServerList : allPlayers)
			        {
			        	MovingObjectPosition movingobjectposition = getMovingObjectPositionFromPlayer(world, playerInServerList, true);
			        	
			        	if (movingobjectposition == null) {return;}
						else
						{
							if (movingobjectposition.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK)
							{
								if (
										playerInServerList.getHeldItem() != null
				        				&& playerInServerList.getHeldItem().getItem() instanceof ItemAxe
				        				&& movingobjectposition.blockX==MathHelper.floor_float(soundX)
				        				&& movingobjectposition.blockY==MathHelper.floor_float(soundY-0.01F)
				        				&& movingobjectposition.blockZ==MathHelper.floor_float(soundZ)
										)
								{
									event.result = null;
			        				return;
								}
							}
						}
			        }
        		}
        		
        		
    			// --- Wooden Button --- //
    			if (
        				(
        						block_at_sound==Blocks.wooden_button
        						// Added these to support Et Futurum and the like
           						|| (block_at_sound instanceof BlockButtonWood)
        						|| block_at_sound.getClass().toString().substring(6).toLowerCase().contains("woodbutton")
        						|| block_at_sound.getClass().toString().substring(6).toLowerCase().contains("buttonwood")
        						|| block_at_sound.getClass().toString().substring(6).toLowerCase().contains("woodenbutton")
        						|| block_at_sound.getClass().toString().substring(6).toLowerCase().contains("buttonwooden")
        						)
        				//&& blockatps.getMaterial()==Material.wood // TODO - Check this in 1.8+ for material specifics
        				&& event.name.equals("random.click")
        				&& GeneralConfig.woodenbuttonSounds
        				)
        		{
    				world.playSound(
    						soundX, soundY, soundZ,
    						Reference.MOD_ID+":block.wooden_button.click",
    						event.sound.getVolume(),
    						event.sound.getPitch(),
    						false);
    				event.result = null;
    				return;
        		}
        		
        		
        		// --- Note Blocks --- //
        		if (event.name.equals("note.harp")
        				// Special handler for soul sand
        				|| (event.name.equals("note.snare")
        						&& world.getBlock(MathHelper.floor_float(soundX), MathHelper.floor_float(soundY)-1, MathHelper.floor_float(soundZ))==Blocks.soul_sand)
        				// Special handler for glowstone
        				|| (event.name.equals("note.hat")
        						&& world.getBlock(MathHelper.floor_float(soundX), MathHelper.floor_float(soundY)-1, MathHelper.floor_float(soundZ))==Blocks.glowstone)
        				// Special handler for bone // TODO - Change in 1.10
        				|| (event.name.equals("note.bd")
        						&& (
        								world.getBlock(MathHelper.floor_float(soundX), MathHelper.floor_float(soundY)-1, MathHelper.floor_float(soundZ)).getClass().toString().substring(6).toLowerCase().contains("boneblock")
        								|| world.getBlock(MathHelper.floor_float(soundX), MathHelper.floor_float(soundY)-1, MathHelper.floor_float(soundZ)).getClass().toString().substring(6).toLowerCase().contains("blockbone")
        								|| world.getBlock(MathHelper.floor_float(soundX), MathHelper.floor_float(soundY)-1, MathHelper.floor_float(soundZ)).getClass().toString().substring(6).toLowerCase().contains("blockbopbone")
        								)
        						&& GeneralConfig.boneblockSounds
        						)
        				&& GeneralConfig.noteBlockSounds)
        		{
        			String instrumentToPlay = event.name;
        			
        			Block blockBeneath = world.getBlock(
            				MathHelper.floor_float(soundX),
            				MathHelper.floor_float(soundY)-1,
            				MathHelper.floor_float(soundZ));
        			
        			// Specific blocks
        			     if (blockBeneath==Blocks.soul_sand)             {instrumentToPlay = Reference.MOD_ID+":note.note_block.cow_bell";}
        			else if (blockBeneath==Blocks.hay_block)             {instrumentToPlay = Reference.MOD_ID+":note.note_block.banjo";}
    			    else if (blockBeneath==Blocks.gold_block)            {instrumentToPlay = Reference.MOD_ID+":note.note_block.bell";}
        			else if (blockBeneath==Blocks.emerald_block)         {instrumentToPlay = Reference.MOD_ID+":note.note_block.bit";}
        			else if (blockBeneath==Blocks.packed_ice)            {instrumentToPlay = Reference.MOD_ID+":note.note_block.chime";}
        			else if (blockBeneath==Blocks.pumpkin)               {instrumentToPlay = Reference.MOD_ID+":note.note_block.didgeridoo";}
        			else if (blockBeneath==Blocks.clay)                  {instrumentToPlay = Reference.MOD_ID+":note.note_block.flute";}
        			else if (blockBeneath==Blocks.iron_block)            {instrumentToPlay = Reference.MOD_ID+":note.note_block.iron_xylophone";}
        			else if (blockBeneath==Blocks.glowstone)             {instrumentToPlay = "note.pling";}
        			// Materials that could encompass multiple blocks
        			else if (blockBeneath.getMaterial()==Material.sand)  {instrumentToPlay = "note.snare";} // for Concrete Powder
        			else if (blockBeneath.getMaterial()==Material.cloth) {instrumentToPlay = Reference.MOD_ID+":note.note_block.guitar";}
    			    // TODO - Change in 1.10
        			else if (
        						(
        							blockBeneath.getClass().toString().substring(6).toLowerCase().contains("blockbone")
        							|| blockBeneath.getClass().toString().substring(6).toLowerCase().contains("boneblock")
        							|| blockBeneath.getClass().toString().substring(6).toLowerCase().contains("blockbopbone")
        							)
        					) {instrumentToPlay = Reference.MOD_ID+":note.note_block.xylophone";}
    			    // Harp sound was updated as of 1.12
        			else {instrumentToPlay = Reference.MOD_ID+":note.note_block.harp";}
     				
        			world.playSound(
    						soundX, soundY, soundZ,
    						instrumentToPlay,
    						instrumentToPlay.equals(Reference.MOD_ID+":note.note_block.iron_xylophone") ? 1F : event.sound.getVolume(),
    						event.sound.getPitch(),
    						false);
        			
    				event.result = null;
    				return;
        		}
        		
        		
        		// --- Netherrack --- //
    			if (block_at_sound instanceof BlockNetherrack
    					&& GeneralConfig.netherrackSounds)
    	    	{
    				if (event.name.equals("dig.stone"))
    	    		{
    	    			world.playSound(
        						soundX, soundY, soundZ,
        						Reference.MOD_ID+":block.netherrack.break",
        						event.sound.getVolume(),
        						event.sound.getPitch(),
        						false);
        				event.result = null;
        				return;
    	    		}
    				if (event.name.equals("step.stone"))
    	    		{
    	    			world.playSound(
        						soundX, soundY, soundZ,
        						Reference.MOD_ID+":block.netherrack.hit",
        						event.sound.getVolume(),
        						event.sound.getPitch(),
        						false);
        				event.result = null;
        				return;
    	    		}
    	    	}
    			
    			
    			// --- Nether Quartz Ore --- //
    			if (block_at_sound==Blocks.quartz_ore
    					&& GeneralConfig.netherquartzoreSounds)
    	    	{
    				if (event.name.equals("dig.stone"))
    	    		{
    	    			world.playSound(
        						soundX, soundY, soundZ,
        						Reference.MOD_ID+":block.nether_ore.break",
        						event.sound.getVolume(),
        						event.sound.getPitch(),
        						false);
        				event.result = null;
        				return;
    	    		}
    				if (event.name.equals("step.stone"))
    	    		{
    	    			world.playSound(
        						soundX, soundY, soundZ,
        						Reference.MOD_ID+":block.nether_ore.hit",
        						event.sound.getVolume(),
        						event.sound.getPitch(),
        						false);
        				event.result = null;
        				return;
    	    		}
    	    	}
    			
    			
    			// --- Nether Brick --- //
    			if (
    					(block_at_sound==Blocks.nether_brick
    					||block_at_sound==Blocks.nether_brick_fence
    					||block_at_sound==Blocks.nether_brick_stairs
    					||block_at_sound.getClass().toString().substring(6).toLowerCase().contains("rednetherbrick")
    					||block_at_sound.getClass().toString().substring(6).toLowerCase().contains("red_nether_brick")
    					||(block_at_sound instanceof BlockStoneSlab
    							&& (world.getBlockMetadata(
    									MathHelper.floor_float(soundX),
    				    				MathHelper.floor_float(soundY-0.01F), // Slightly below so that top-surface sounds detect the block underneath
    				    				MathHelper.floor_float(soundZ)
    									)&7)==6 // 6 is Nether Brick
    								)
    							)
    					&& GeneralConfig.netherbrickSounds)
    	    	{
    				if (event.name.equals("dig.stone"))
    	    		{
    	    			world.playSound(
        						soundX, soundY, soundZ,
        						Reference.MOD_ID+":block.nether_bricks.break",
        						event.sound.getVolume()*0.8F,
        						event.sound.getPitch(),
        						false);
        				event.result = null;
        				return;
    	    		}
    				if (event.name.equals("step.stone"))
    	    		{
    	    			world.playSound(
        						soundX, soundY, soundZ,
        						Reference.MOD_ID+":block.nether_bricks.hit",
        						event.sound.getVolume(),
        						event.sound.getPitch(),
        						false);
        				event.result = null;
        				return;
    	    		}
    	    	}
    			
        		
        		// --- Basalt --- //
    			if (
    					(block_at_sound.getClass().toString().substring(6).toLowerCase().contains("basalt"))
    					&& GeneralConfig.basaltblockSounds) //v1.0.1
    	    	{
    				if (event.name.equals("dig.stone"))
    	    		{
    	    			world.playSound(
        						soundX, soundY, soundZ,
        						Reference.MOD_ID+":block.basalt.break",
        						event.sound.getVolume(),
        						event.sound.getPitch(),
        						false);
        				event.result = null;
        				return;
    	    		}
    				if (event.name.equals("step.stone"))
    	    		{
    	    			world.playSound(
        						soundX, soundY, soundZ,
        						Reference.MOD_ID+":block.basalt.hit",
        						event.sound.getVolume(),
        						event.sound.getPitch(),
        						false);
        				event.result = null;
        				return;
    	    		}
    	    	}
        		
        		
        		// --- Bone Block --- //
    			if (
    					(// TODO - Make explicit Bone Block in 1.10
    							block_at_sound.getClass().toString().substring(6).toLowerCase().contains("blockbone")
    							||block_at_sound.getClass().toString().substring(6).toLowerCase().contains("boneblock")
    							||block_at_sound.getClass().toString().substring(6).toLowerCase().contains("blockbopbone")
    							)
    					&& GeneralConfig.boneblockSounds)
    	    	{
    				if (event.name.equals("dig.stone"))
    	    		{
    	    			world.playSound(
        						soundX, soundY, soundZ,
        						Reference.MOD_ID+":block.bone_block.break",
        						event.sound.getVolume(),
        						event.sound.getPitch(),
        						false);
        				event.result = null;
        				return;
    	    		}
    				if (event.name.equals("step.stone"))
    	    		{
    	    			world.playSound(
        						soundX, soundY, soundZ,
        						Reference.MOD_ID+":block.bone_block.hit",
        						event.sound.getVolume(),
        						event.sound.getPitch(),
        						false);
        				event.result = null;
        				return;
    	    		}
    	    	}
        		
        		
        		// --- Nether Wart Block --- //
    			if (
    					(// TODO - Make explicit Nether Wart Block in 1.10
    							block_at_sound.getClass().toString().substring(6).toLowerCase().contains("blocknetherwart")
    							||block_at_sound.getClass().toString().substring(6).toLowerCase().contains("netherwartblock")
    							)
    					&& GeneralConfig.netherwartblockSounds)
    	    	{
    				if (event.name.equals("dig.wood"))
    	    		{
    	    			world.playSound(
        						soundX, soundY, soundZ,
        						Reference.MOD_ID+":block.wart_block.break",
        						event.sound.getVolume(),
        						event.sound.getPitch(),
        						false);
        				event.result = null;
        				return;
    	    		}
    				if (event.name.equals("step.wood"))
    	    		{
    	    			world.playSound(
        						soundX, soundY, soundZ,
        						Reference.MOD_ID+":block.wart_block.hit",
        						event.sound.getVolume(),
        						event.sound.getPitch(),
        						false);
        				event.result = null;
        				return;
    	    		}
    	    	}
    			
    			
    			// --- Soul Sand --- //
    			if (
    					block_at_sound instanceof BlockSoulSand
    					&& GeneralConfig.soulsandSounds
    					)
    	    	{
    	    		if (event.name.equals("dig.sand"))
    	    		{
    	    			world.playSound(
        						soundX, soundY, soundZ,
        						Reference.MOD_ID+":block.soul_sand.break",
        						event.sound.getVolume(),
        						event.sound.getPitch(),
        						false);
        				event.result = null;
        				return;
    	    		}
    	    		if (event.name.equals("step.sand"))
    	    		{
    	    			world.playSound(
        						soundX, soundY, soundZ,
        						Reference.MOD_ID+":block.soul_sand.hit",
        						event.sound.getVolume(),
        						event.sound.getPitch(),
        						false);
        				event.result = null;
        				return;
    	    		}
    	    	}
        		
        		
        		// --- New Cave --- //
        		if (event.name.equals("ambient.cave.cave")
        				&& GeneralConfig.moreCaveSounds)
        		{
        			world.playSound(
    						soundX, soundY, soundZ,
    						Reference.MOD_ID+":ambient.cave",
    						0.7F,
    						0.8F + world.rand.nextFloat() * 0.2F,
    						false);
    				event.result = null;
    				return;
        		}
        		
        		
            	// --- New Rain --- //
            	if (
            			event.name.equals("ambient.weather.rain")
        				&& GeneralConfig.moreRainSounds
        				)
        		{
            		if (event.sound.getVolume()==0.1F && event.sound.getPitch()==0.5F)
                    {
            			world.playSound(
        						soundX, soundY, soundZ,
        						Reference.MOD_ID+":weather.rain.above",
        						0.1F,
        						0.5F,
        						false);
                    }
            		else if (event.sound.getVolume()==0.2F && event.sound.getPitch()==1.0F)
            		{
            			world.playSound(
        						soundX, soundY, soundZ,
        						Reference.MOD_ID+":weather.rain",
        						0.2F,
        						1.0F,
        						false);
            		}
            		else {return;}
            		
    				event.result = null;
    				return;
        		}
            	
        		
        		// --- Breaking crops --- //
        		if (GeneralConfig.cropbreakingSounds)
        		{
        			// --- Mundane crops --- //
        			if (
            				block_at_sound instanceof BlockCrops
            				&& event.name.equals("dig.grass")
            				)
            		{
            			world.playSound(
        						soundX, soundY, soundZ,
        						Reference.MOD_ID+":block.crop.break",
        						0.9F,
        						world.rand.nextFloat() * 0.1F + 0.9F, 
        						false);
        				
        				event.result = null;
        				return;
            		}
        			// --- Nether Wart --- //
        			if (
            				block_at_sound instanceof BlockNetherWart
            				&& event.name.equals("dig.stone")
            				)
            		{
            			world.playSound(
        						soundX, soundY, soundZ,
        						Reference.MOD_ID+":block.nether_wart.break",
        						0.9F,
        						0.9F,
        						false);
        				
        				event.result = null;
        				return;
            		}
        		}
        		
        		
        		
        		// --- Wooden chest --- //
        		if (
        				block_at_sound instanceof BlockChest
        				&& block_at_sound.getMaterial() == Material.wood
        				&& GeneralConfig.woodenchestSounds
        				)
        		{
        			if (event.name.equals("random.chestclosed"))
        			{
        				world.playSound(
        						soundX, soundY, soundZ,
        						Reference.MOD_ID+":block.chest.close",
        						0.5F,
        						world.rand.nextFloat() * 0.1F + 0.9F, 
        						false);
        				
        				event.result = null;
        				return;
        			}
        		}
        		
        		
        		
        		// --- Ender Chest --- //
        		if (
        				block_at_sound instanceof BlockEnderChest
        				//&& blockatps.getMaterial() == Material.wood
        				&& GeneralConfig.enderchestSounds
        				)
        		{
        			if (event.name.equals("random.chestopen"))
        			{
        				world.playSound(
        						soundX, soundY, soundZ,
        						Reference.MOD_ID+":block.ender_chest.open",
        						0.5F,
        						world.rand.nextFloat() * 0.1F + 0.9F, 
        						false);
        				
        				event.result = null;
        				return;
        			}
        			if (event.name.equals("random.chestclosed"))
        			{
        				world.playSound(
        						soundX, soundY, soundZ,
        						Reference.MOD_ID+":block.ender_chest.close",
        						0.5F,
        						world.rand.nextFloat() * 0.1F + 0.9F, 
        						false);
        				
        				event.result = null;
        				return;
        			}
        		}
        		
        		
        		
        		// --- Wooden door --- //
        		if (
        				block_at_sound instanceof BlockDoor
        				&& block_at_sound.getMaterial() == Material.wood
        				&& (GeneralConfig.woodendoorSounds_legacy
        					|| GeneralConfig.woodendoorSounds_modern)
        				)
        		{
        			if (event.name.equals("random.door_open"))
        			{
        				world.playSound(
        						soundX, soundY, soundZ,
        						Reference.MOD_ID+":block.wooden_door.open" + (GeneralConfig.woodendoorSounds_legacy ? ".legacy" : ""),
        						1.0F,
        						world.rand.nextFloat() * 0.1F + 0.9F, 
        						false);
        				
        				event.result = null;
        				return;
        			}
        			if (event.name.equals("random.door_close"))
        			{
        				world.playSound(
        						soundX, soundY, soundZ,
        						Reference.MOD_ID+":block.wooden_door.close" + (GeneralConfig.woodendoorSounds_legacy ? ".legacy" : "") + (GeneralConfig.woodendoorSounds_modern ? ".modern" : ""),
        						1.0F,
        						world.rand.nextFloat() * 0.1F + 0.9F, 
        						false);
        				
        				event.result = null;
        				return;
        			}
        		}

        		
        		
        		// --- Fence gate --- //
        		if (
        				block_at_sound instanceof BlockFenceGate
        				&& GeneralConfig.fencegateSounds
        				)
        		{
        			if (event.name.equals("random.door_open"))
        			{
        				world.playSound(
        						soundX, soundY, soundZ,
        						Reference.MOD_ID+":block.fence_gate.open",
        						1.0F,
        						world.rand.nextFloat() * 0.1F + 0.9F, 
        						false);
        				
        				event.result = null;
        				return;
        			}
        			if (event.name.equals("random.door_close"))
        			{
        				world.playSound(
        						soundX, soundY, soundZ,
        						Reference.MOD_ID+":block.fence_gate.close",
        						1.0F,
        						world.rand.nextFloat() * 0.1F + 0.9F, 
        						false);
        				
        				event.result = null;
        				return;
        			}
        		}
        		
        		
        		
        		// --- Iron door --- //
        		if (
        				block_at_sound instanceof BlockDoor
        				&& block_at_sound.getMaterial() == Material.iron
        				&& GeneralConfig.irondoorSounds
        				)
        		{
        			if (event.name.equals("random.door_open"))
        			{
        				world.playSound(
        						soundX, soundY, soundZ,
        						Reference.MOD_ID+":block.iron_door.open",
        						1.0F,
        						world.rand.nextFloat() * 0.1F + 0.9F, 
        						false);
        				
        				event.result = null;
        				return;
        			}
        			if (event.name.equals("random.door_close"))
        			{
        				world.playSound(
        						soundX, soundY, soundZ,
        						Reference.MOD_ID+":block.iron_door.close",
        						1.0F,
        						world.rand.nextFloat() * 0.1F + 0.9F, 
        						false);
        				
        				event.result = null;
        				return;
        			}
        		}
        		
        		
        		// --- Wooden trapdoor --- //
        		if (
        				block_at_sound instanceof BlockTrapDoor
        				&& block_at_sound.getMaterial() == Material.wood
        				&& GeneralConfig.woodentrapdoorSounds
        				)
        		{
        			if (event.name.equals("random.door_open"))
        			{
        				world.playSound(
        						soundX, soundY, soundZ,
        						Reference.MOD_ID+":block.wooden_trapdoor.open",
        						1.0F,
        						world.rand.nextFloat() * 0.1F + 0.9F, 
        						false);
        				
        				event.result = null;
        				return;
        			}
        			if (event.name.equals("random.door_close"))
        			{
        				world.playSound(
        						soundX, soundY, soundZ,
        						Reference.MOD_ID+":block.wooden_trapdoor.close",
        						1.0F,
        						world.rand.nextFloat() * 0.1F + 0.9F, 
        						false);
        				
        				event.result = null;
        				return;
        			}
        		}
        		
        		    		
        		// --- Iron trapdoor --- //
        		if (
        				block_at_sound instanceof BlockTrapDoor
        				&& block_at_sound.getMaterial() == Material.iron
        				&& GeneralConfig.irontrapdoorSounds
        				)
        		{
        			if (event.name.equals("random.door_open"))
        			{
        				world.playSound(
        						soundX, soundY, soundZ,
        						Reference.MOD_ID+":block.iron_trapdoor.open",
        						1.0F,
        						world.rand.nextFloat() * 0.1F + 0.9F, 
        						false);
        				
        				event.result = null;
        				return;
        			}
        			if (event.name.equals("random.door_close"))
        			{
        				world.playSound(
        						soundX, soundY, soundZ,
        						Reference.MOD_ID+":block.iron_trapdoor.close",
        						1.0F,
        						world.rand.nextFloat() * 0.1F + 0.9F, 
        						false);
        				
        				event.result = null;
        				return;
        			}
        		}
    		}
    	}
    }
    
    
	@SubscribeEvent
	public void onPlaySoundAtEntity(PlaySoundAtEntityEvent event)
	{
		if (!event.entity.worldObj.isRemote)
		{
			// --- Horse --- //
			if (
					event.entity instanceof EntityHorse
					&& GeneralConfig.horsefeedingSounds
					)
			{
				float horseVolume = getEntityLivingVolume((EntityHorse)event.entity, EntityLivingBase.class);
				float horsePitch = getEntityLivingPitch((EntityHorse)event.entity, EntityLivingBase.class);
				
				// --- Horse eat --- //
				if (event.name.equals("eating"))
				{
					event.setCanceled(true);
					event.entity.playSound(Reference.MOD_ID+":entity.horse.eat", horseVolume, horsePitch);
					return;
				}
			}
						
			
			// --- Witch --- //
			if (
					event.entity instanceof EntityWitch
					&& GeneralConfig.witchSounds
					)
			{
				float witchVolume = getEntityLivingVolume((EntityWitch)event.entity, EntityLivingBase.class);
				float witchPitch = getEntityLivingPitch((EntityWitch)event.entity, EntityLivingBase.class);
				
				// --- Witch idle --- //
				if (event.name.equals("mob.witch.idle"))
				{
					event.setCanceled(true);
					event.entity.playSound(Reference.MOD_ID+":entity.witch.ambient", witchVolume, witchPitch);
					return;
				}
				
				// --- Witch hurt --- //
				if (event.name.equals("mob.witch.hurt"))
				{
					event.setCanceled(true);
					event.entity.playSound(Reference.MOD_ID+":entity.witch.hurt", witchVolume, witchPitch);
					return;
				}
				
				// --- Witch death --- //
				if (event.name.equals("mob.witch.death"))
				{
					event.setCanceled(true);
					event.entity.playSound(Reference.MOD_ID+":entity.witch.death", witchVolume, witchPitch);
					return;
				}
				
			}
			
			
			// --- Wither Skeleton --- //
			if (
					event.entity instanceof EntitySkeleton
					&& ((EntitySkeleton)event.entity).getSkeletonType()==1
					&& GeneralConfig.witherskeletonSounds
					)
			{
				float skeletonVolume = getEntityLivingVolume((EntitySkeleton)event.entity, EntityLivingBase.class);
				float skeletonPitch = getEntityLivingPitch((EntitySkeleton)event.entity, EntityLivingBase.class);
				
				// -- Wither Skeleton Ambient -- //
				if (event.name.equals("mob.skeleton.say"))
				{
					event.setCanceled(true);
					event.entity.playSound(Reference.MOD_ID+":entity.wither_skeleton.ambient", skeletonVolume, skeletonPitch);
					return;
				}
				
				// -- Wither Skeleton Hurt -- //
				if (event.name.equals("mob.skeleton.hurt"))
				{
					event.setCanceled(true);
					event.entity.playSound(Reference.MOD_ID+":entity.wither_skeleton.hurt", skeletonVolume, skeletonPitch);
					return;
				}

				// -- Wither Skeleton Death -- //
				if (event.name.equals("mob.skeleton.death"))
				{
					event.setCanceled(true);
					event.entity.playSound(Reference.MOD_ID+":entity.wither_skeleton.death", skeletonVolume, skeletonPitch);
					return;
				}

				// -- Wither Skeleton Step -- //
				if (event.name.equals("mob.skeleton.step"))
				{
					event.setCanceled(true);
					event.entity.playSound(Reference.MOD_ID+":entity.wither_skeleton.step", 0.15F, skeletonPitch);
					return;
				}
				
			}
			
			// --- Thorns enchantment --- //
			if (
					event.name.equals("damage.thorns")
					&& GeneralConfig.thornsSound
					)
			{
				event.setCanceled(true);
				event.entity.playSound(Reference.MOD_ID+":enchant.thorns", event.volume, event.pitch);
				return;
			}
			
		}
		
	}
	
	
	@SubscribeEvent
	public void onLivingUpdateEvent(LivingUpdateEvent event)
	{
		if (!event.entityLiving.worldObj.isRemote)
		{
			// --- Witch drink potion --- //
			if (
					event.entityLiving instanceof EntityWitch
					&& GeneralConfig.witchSounds
					)
			{
				EntityWitch witch = (EntityWitch)event.entityLiving;
				
				int witchAttackTimer_reflected = ReflectionHelper.getPrivateValue(EntityWitch.class, witch, new String[]{"witchAttackTimer",  "field_82200_e"});
				
				if (witch.getAggressive() && witchAttackTimer_reflected==0)
				{
					event.entity.playSound(Reference.MOD_ID+":entity.witch.drink", getEntityLivingVolume(witch, EntityLivingBase.class), getEntityLivingPitch(witch, EntityLivingBase.class));
				}
				
				return;
			}
			

			// --- Squid ambient --- //
			if ( 
					event.entity instanceof EntitySquid
					&& GeneralConfig.squidSounds
					)
			{
				Random rand;
				
				try {rand = ReflectionHelper.getPrivateValue(Entity.class, event.entity, new String[]{"rand", "field_70146_Z"});}
				catch (Exception e) {rand = event.entity.worldObj.rand;}
				
				if (event.entity.isEntityAlive() && rand.nextInt(1000) < ((EntityLiving)event.entityLiving).livingSoundTime++)
				{
					event.entity.playSound(Reference.MOD_ID+":entity.squid.ambient", getEntityLivingVolume((EntitySquid)event.entity, EntitySquid.class), getEntityLivingPitch((EntityLivingBase)event.entity, EntityLivingBase.class));
					((EntityLiving)event.entityLiving).livingSoundTime = -((EntityLiving)event.entityLiving).getTalkInterval();
				}
				
				return;
			}
			
			
			if (event.entity instanceof EntityPlayer)
			{
				EntityPlayer player = (EntityPlayer)event.entity;
				
				
	    		// --- Armor Equip --- //
	    		if (
	    				player!=null
	    				&& player.worldObj != null
	    				&& (GeneralConfig.armorequipSounds)
	    				)
	    		{
	    			// Items currently on the player
					ItemStack playerBoots = player.getEquipmentInSlot(1);
					ItemStack playerLeggings = player.getEquipmentInSlot(2);
					ItemStack playerChestplate = player.getEquipmentInSlot(3);
					ItemStack playerHelmet = player.getEquipmentInSlot(4);
					
					// Items attached to the player as an IEEP tag
					PlayerArmorTracker ipat = PlayerArmorTracker.get(player);
					ItemStack storedBoots = ItemStack.loadItemStackFromNBT(ipat.getBoots());
					ItemStack storedLeggings = ItemStack.loadItemStackFromNBT(ipat.getLeggings());
					ItemStack storedChestplate = ItemStack.loadItemStackFromNBT(ipat.getChestplate());
					ItemStack storedHelmet = ItemStack.loadItemStackFromNBT(ipat.getHelmet());
					
					String itemEquippedSound = "";
					
					// --- Boots --- //
					if (
							playerBoots!=null // Equipment is in the slot
							&& (
									storedBoots==null // and either the NBT thinks there's not an item already there,
											// or that the item is different in some way...
									|| (storedBoots!=null 
											&& (    // ...that's not its durability.
													!playerBoots.getItem().equals(storedBoots.getItem())
													|| !(playerBoots.stackTagCompound == null && storedBoots.stackTagCompound != null ? false : playerBoots.stackTagCompound == null || playerBoots.stackTagCompound.equals(storedBoots.stackTagCompound))
													)
											)
									)
							)
					{
						     if (playerBoots.getItem()==Items.chainmail_boots) {itemEquippedSound = "item.armor.equip_chain";}
						else if (playerBoots.getItem()==Items.diamond_boots)   {itemEquippedSound = "item.armor.equip_diamond";}
						else if (playerBoots.getItem()==Items.golden_boots)    {itemEquippedSound = "item.armor.equip_gold";}
						else if (playerBoots.getItem()==Items.iron_boots)      {itemEquippedSound = "item.armor.equip_iron";}
						else if (playerBoots.getItem()==Items.leather_boots)   {itemEquippedSound = "item.armor.equip_leather";}
						else                                                   {itemEquippedSound = "item.armor.equip_generic";}
					}
					
					// Update the stored itemstack if it's changed, regardless of if you should make a noise
					if (!ItemStack.areItemStacksEqual(playerBoots, storedBoots)) {ipat.setBoots(PlayerArmorTracker.saveItemStackToNBT(playerBoots));}
					
					// --- Leggings --- //
					if (
							playerLeggings!=null // Equipment is in the slot
							&& (
									storedLeggings==null // and either the NBT thinks there's not an item already there,
											// or that the item is different in some way...
									|| (storedLeggings!=null 
											&& (    // ...that's not its durability.
													!playerLeggings.getItem().equals(storedLeggings.getItem())
													|| !(playerLeggings.stackTagCompound == null && storedLeggings.stackTagCompound != null ? false : playerLeggings.stackTagCompound == null || playerLeggings.stackTagCompound.equals(storedLeggings.stackTagCompound))
													)
											)
									)
							)
					{
						     if (playerLeggings.getItem()==Items.chainmail_leggings) {itemEquippedSound = "item.armor.equip_chain";}
						else if (playerLeggings.getItem()==Items.diamond_leggings)   {itemEquippedSound = "item.armor.equip_diamond";}
						else if (playerLeggings.getItem()==Items.golden_leggings)    {itemEquippedSound = "item.armor.equip_gold";}
						else if (playerLeggings.getItem()==Items.iron_leggings)      {itemEquippedSound = "item.armor.equip_iron";}
						else if (playerLeggings.getItem()==Items.leather_leggings)   {itemEquippedSound = "item.armor.equip_leather";}
						else                                                         {itemEquippedSound = "item.armor.equip_generic";}
					}
					
					// Update the stored itemstack if it's changed, regardless of if you should make a noise
					if (!ItemStack.areItemStacksEqual(playerLeggings, storedLeggings)) {ipat.setLeggings(PlayerArmorTracker.saveItemStackToNBT(playerLeggings));}
					
					// --- Chestplate --- //
					if (
							playerChestplate!=null // Equipment is in the slot
							&& (
									storedChestplate==null // and either the NBT thinks there's not an item already there,
											// or that the item is different in some way...
									|| (storedChestplate!=null 
											&& (    // ...that's not its durability.
													!playerChestplate.getItem().equals(storedChestplate.getItem())
													|| !(playerChestplate.stackTagCompound == null && storedChestplate.stackTagCompound != null ? false : playerChestplate.stackTagCompound == null || playerChestplate.stackTagCompound.equals(storedChestplate.stackTagCompound))
													)
											)
									)
							)
					{
						     if (playerChestplate.getItem()==Items.chainmail_chestplate) {itemEquippedSound = "item.armor.equip_chain";}
						else if (playerChestplate.getItem()==Items.diamond_chestplate)   {itemEquippedSound = "item.armor.equip_diamond";}
						else if (playerChestplate.getItem()==Items.golden_chestplate)    {itemEquippedSound = "item.armor.equip_gold";}
						else if (playerChestplate.getItem()==Items.iron_chestplate)      {itemEquippedSound = "item.armor.equip_iron";}
						else if (playerChestplate.getItem()==Items.leather_chestplate)   {itemEquippedSound = "item.armor.equip_leather";}
						else if (playerChestplate.getItem().getUnlocalizedName().toLowerCase().contains("elytra"))
																						 {itemEquippedSound = "item.armor.equip_elytra";} // Elytra is its own sound event
						else                                                             {itemEquippedSound = "item.armor.equip_generic";}
					}
					
					// Update the stored itemstack if it's changed, regardless of if you should make a noise
					if (!ItemStack.areItemStacksEqual(playerChestplate, storedChestplate)) {ipat.setChestplate(PlayerArmorTracker.saveItemStackToNBT(playerChestplate));}
					
					// --- Helmet --- //
					if (
							playerHelmet!=null // Equipment is in the slot
							&& (
									storedHelmet==null // and either the NBT thinks there's not an item already there,
											// or that the item is different in some way...
									|| (storedHelmet!=null 
											&& (    // ...that's not its durability.
													!playerHelmet.getItem().equals(storedHelmet.getItem())
													|| !(playerHelmet.stackTagCompound == null && storedHelmet.stackTagCompound != null ? false : playerHelmet.stackTagCompound == null || playerHelmet.stackTagCompound.equals(storedHelmet.stackTagCompound))
													)
											)
									)
							)
					{
						     if (playerHelmet.getItem()==Items.chainmail_helmet) {itemEquippedSound = "item.armor.equip_chain";}
						else if (playerHelmet.getItem()==Items.diamond_helmet)   {itemEquippedSound = "item.armor.equip_diamond";}
						else if (playerHelmet.getItem()==Items.golden_helmet)    {itemEquippedSound = "item.armor.equip_gold";}
						else if (playerHelmet.getItem()==Items.iron_helmet)      {itemEquippedSound = "item.armor.equip_iron";}
						else if (playerHelmet.getItem()==Items.leather_helmet)   {itemEquippedSound = "item.armor.equip_leather";}
						else                                                     {itemEquippedSound = "item.armor.equip_generic";}
					}
					
					// Update the stored itemstack if it's changed, regardless of if you should make a noise
					if (!ItemStack.areItemStacksEqual(playerHelmet, storedHelmet)) {ipat.setHelmet(PlayerArmorTracker.saveItemStackToNBT(playerHelmet));}
					
					// Play a sound if one of the equipment pieces changed
					if (
							!itemEquippedSound.equals("")
							&& GeneralConfig.armorequipSounds)
					{
						player.worldObj.playSoundEffect(
								player.posX, player.posY, player.posZ,
		    					Reference.MOD_ID+":"+itemEquippedSound, 1F, 1F);
						return;
					}
					
	    		}
				
	    		
				// --- Ender Eye bursts --- //
				if (GeneralConfig.endereyelaunchSounds)
				{
					List<EntityPlayer> allPlayers = Lists.<EntityPlayer>newArrayList();
					
			        for (Object entity : player.worldObj.playerEntities)
			        {
			            if ( (EntityPlayer.class).isAssignableFrom(entity.getClass()) )
			            {
			            	allPlayers.add((EntityPlayer)entity);
			            }
			        }
			        
			        for (EntityPlayer playerInServerList : allPlayers)
			        {
			        	World world = player.worldObj;
			        	
			        	List listEnderEyesInRange = world.getEntitiesWithinAABB(
			    				EntityEnderEye.class, AxisAlignedBB.getBoundingBox(
			    						playerInServerList.posX - enderEyeSoundRadius, playerInServerList.posY - enderEyeSoundRadius, playerInServerList.posZ - enderEyeSoundRadius,
			    						playerInServerList.posX + enderEyeSoundRadius, playerInServerList.posY + enderEyeSoundRadius, playerInServerList.posZ + enderEyeSoundRadius
			    						)
			    				);
			        	
			        	if (listEnderEyesInRange != null)
			        	{
			        		Iterator iterator = listEnderEyesInRange.iterator();
			        		
			        		while (iterator.hasNext())
			        		{
			                	EntityEnderEye entityendereye = (EntityEnderEye)iterator.next();
			                	
			                	double eyeX = entityendereye.posX;
			                	double eyeY = entityendereye.posY;
			                	double eyeZ = entityendereye.posZ;
			                	int despawnTimer_reflected = -1;
			                	
			                	if (
			                			(playerInServerList.posX-eyeX)*(playerInServerList.posX-eyeX) +
			                			(playerInServerList.posY-eyeY)*(playerInServerList.posY-eyeY) +
			                			(playerInServerList.posZ-eyeZ)*(playerInServerList.posZ-eyeZ)
			                			<= enderEyeSoundRadius*enderEyeSoundRadius
			                			)
			                	{
			                		try {despawnTimer_reflected = ReflectionHelper.getPrivateValue(EntityEnderEye.class, entityendereye, new String[]{"despawnTimer", "field_70223_e"});}
			                        catch (Exception e) {} // Could not extract the despawnTimer value
			                		
			                		if (despawnTimer_reflected==80)
			                		{
			                			// Get a list of every player in range of the eye
			                			
			        		    		List listPlayersInRange = world.getEntitiesWithinAABB(
			        		    				EntityPlayer.class, AxisAlignedBB.getBoundingBox(
			        		    						eyeX - enderEyeSoundRadius, eyeY - enderEyeSoundRadius, eyeZ - enderEyeSoundRadius,
			        		    						eyeX + enderEyeSoundRadius, eyeY + enderEyeSoundRadius, eyeZ + enderEyeSoundRadius
			        		    						)
			        		    				);

			        		    		if (playerInServerList == listPlayersInRange.get(0))
			        		    		{
			        				        playerInServerList.worldObj.playSoundEffect(
			        		    					eyeX, eyeY, eyeZ,
			        		    					Reference.MOD_ID+":entity.endereye.dead",
			        		    					1.3F, 1.0F
			        		    					);
			        		    		}
			        		    		return;
			                		}
			                	}
			        		}
			        	}
			        }
				}
			}
		}
	}
	

	@SubscribeEvent
	public void onLivingHurtEvent(LivingHurtEvent event)
	{
		if (!event.entityLiving.worldObj.isRemote)
		{
			// --- Player Drowning --- //
			if (
					event.entity instanceof EntityPlayer &&
					event.entity.worldObj != null &&
					event.source.damageType.equals("drown")
    				&& GeneralConfig.drowningSounds
    				)
    		{
				event.entity.worldObj.playSoundEffect(
						event.entity.posX,
						event.entity.posY,
						event.entity.posZ,
						Reference.MOD_ID+":entity.player.hurt_drown",
						getEntityLivingVolume((EntityLivingBase)event.entity, EntityLivingBase.class),
						getEntityLivingPitch((EntityLivingBase)event.entity, EntityLivingBase.class)
						);
				return;
    		}
        	
			
			// --- Player on FiRe --- //
			if (
					//event.entity instanceof EntityPlayer &&
					event.entity.worldObj != null &&
					(event.source.damageType.equals("onFire") || event.source.damageType.equals("inFire")) // Burning sound when IN fire 
    				&& GeneralConfig.onfireSounds
    				)
    		{
				event.entity.worldObj.playSoundEffect(
						event.entity.posX,
						event.entity.posY,
						event.entity.posZ,
						Reference.MOD_ID+":entity.player.hurt_on_fire",
						getEntityLivingVolume((EntityLivingBase)event.entity, EntityLivingBase.class),
						getEntityLivingPitch((EntityLivingBase)event.entity, EntityLivingBase.class)
						);
				return;
    		}
			
			
			// --- Damage via Thorns --- //
			if (
					event.source.damageType.equals("thorns")
					&& GeneralConfig.thornsSound
					)
			{
				event.source.getEntity().playSound(Reference.MOD_ID+":enchant.thorns", 
						event.source.getEntity() instanceof EntityLivingBase ? getEntityLivingVolume((EntityLivingBase)event.source.getEntity(), EntityLivingBase.class) : 1F,
						(event.entity.worldObj.rand.nextFloat() - event.entity.worldObj.rand.nextFloat()) * 0.2F + 1.0F);
				
				return;
			}
			
			
			// --- Squid hurt --- //
			if (event.entity instanceof EntitySquid && ((EntityLiving)event.entity).getHealth()-event.ammount > 0
					&& GeneralConfig.squidSounds
					)
			{
				event.entity.playSound(Reference.MOD_ID+":entity.squid.hurt", getEntityLivingVolume((EntitySquid)event.entity, EntitySquid.class), getEntityLivingPitch((EntityLivingBase)event.entity, EntityLivingBase.class));
				return;
			}
			
			
			// --- Snow Golem hurt --- //
			if (event.entity instanceof EntitySnowman && ((EntityLiving)event.entity).getHealth()-event.ammount > 0
					&& GeneralConfig.snowgolemSounds
					)
			{
				event.entity.playSound(
						Reference.MOD_ID+":entity.snow_golem.hurt",
						getEntityLivingVolume((EntityLivingBase)event.entity, EntityLivingBase.class),
						getEntityLivingPitch((EntityLivingBase)event.entity, EntityLivingBase.class)
						);
				return;
			}
			
			
			// --- Attack a living entity --- //
			if (
					event.source.damageType.equals("player")
					&& GeneralConfig.playerAttackSounds
					)
			{
				Entity targetEntity = event.entity;
				EntityPlayer playerSource = (EntityPlayer) event.source.getEntity();
				World world = playerSource.worldObj;
				
				if (targetEntity.canAttackWithItem())
				{
					if (!targetEntity.hitByEntity(playerSource))
					{
						float attackDamage = (float)playerSource.getEntityAttribute(SharedMonsterAttributes.attackDamage).getAttributeValue();
						float enchantmentDamage = 0.0F;

						if (targetEntity instanceof EntityLivingBase)
						{
							enchantmentDamage = EnchantmentHelper.getEnchantmentModifierLiving(playerSource, (EntityLivingBase)targetEntity);
						}
						
						if (attackDamage > 0.0F || enchantmentDamage > 0.0F)
						{
							boolean isStrongAttack = 
									(
											playerSource.getHeldItem() != null
											&& playerSource.getHeldItem().getItem() instanceof ItemSword
											&& GeneralConfig.playerStrongOnSword)
									|| event.ammount >= GeneralConfig.playerStrongThreshold; //f2 > 0.9F;
							
							// Knockback degree
							int i = EnchantmentHelper.getKnockbackModifier(playerSource, (EntityLivingBase)targetEntity);
							
							if (playerSource.isSprinting())
							{
								// --- Knockback attack sound --- //
								world.playSoundEffect(
										targetEntity.posX
										, targetEntity.posY
										, targetEntity.posZ
										, Reference.MOD_ID+":entity.player.attack.knockback", 0.7F, 1.0F);
								
								++i;
							}
							
							boolean isCriticalHit =
									playerSource.fallDistance > 0.0F
									&& !playerSource.onGround
									&& !playerSource.isOnLadder()
									&& !playerSource.isInWater()
									&& !playerSource.isPotionActive(Potion.blindness)
									&& playerSource.ridingEntity == null
									&& targetEntity instanceof EntityLivingBase
									&& !playerSource.isSprinting(); // Added in 1.12
							
							if (isCriticalHit)
							{
								attackDamage *= 1.5F;
							}
							
							attackDamage += enchantmentDamage;
							
							boolean targetTakesDamage = !targetEntity.isEntityInvulnerable() && event.ammount > 0F;//targetEntity.attackEntityFrom(DamageSource.causePlayerDamage(playerSource), f);
							
							if (targetTakesDamage)
							{
								if (isCriticalHit)
								{
									// --- Crit attack sound --- //
									world.playSoundEffect(
											targetEntity.posX
											, targetEntity.posY
											, targetEntity.posZ
											, Reference.MOD_ID+":entity.player.attack.crit", 0.7F, 1.0F);
								}
								
								if (
										!isCriticalHit // flag2 is critical hit in 1.12
										//&& !flag3 // flag3 is sweeping hit in 1.12
										)
								{
									if (isStrongAttack) // flag in 1.12 is playerSource.getCooledAttackStrength(0.5F) > 0.9F
									{
										// --- Strong attack sound --- //
										world.playSoundEffect(
												targetEntity.posX
												, targetEntity.posY
												, targetEntity.posZ
												, Reference.MOD_ID+":entity.player.attack.strong", 0.7F, 1.0F);
									}
									else
									{
										// --- Weak attack sound --- //
										world.playSoundEffect(
												targetEntity.posX
												, targetEntity.posY
												, targetEntity.posZ
												, Reference.MOD_ID+":entity.player.attack.weak", 0.7F, 1.0F);
									}
								}
							}
							else
							{
								// --- No damage attack sound --- //
								world.playSoundEffect(
										targetEntity.posX
										, targetEntity.posY
										, targetEntity.posZ
										, Reference.MOD_ID+":entity.player.attack.nodamage", 0.7F, 1.0F);
							} 
						}
					}
				}
			}
		}
	}
	
	
	@SubscribeEvent
	public void onLivingDeathEvent(LivingDeathEvent event)
	{
		if (!event.entityLiving.worldObj.isRemote)
		{
			// --- Squid death --- //
			if ( 
					event.entity instanceof EntitySquid
					&& GeneralConfig.squidSounds
					)
			{
				event.entity.playSound(
						Reference.MOD_ID+":entity.squid.death",
						getEntityLivingVolume((EntitySquid)event.entity, EntitySquid.class),
						getEntityLivingPitch((EntityLivingBase)event.entity, EntityLivingBase.class)
						);
				return;
			}
			
			
			// --- Snow Golem death --- //
			if ( 
					event.entity instanceof EntitySnowman
					&& GeneralConfig.snowgolemSounds
					)
			{
				event.entity.playSound(
						Reference.MOD_ID+":entity.snow_golem.death",
						getEntityLivingVolume((EntityLivingBase)event.entity, EntityLivingBase.class),
						getEntityLivingPitch((EntityLivingBase)event.entity, EntityLivingBase.class)
						);
				return;
			}
		}
	}
	
	
	@SubscribeEvent
	public void onAttackEntityEvent(AttackEntityEvent event) // Only fires when a player LEFT-CLICKS an entity.
	{
		if (!event.target.worldObj.isRemote)
		{
			// --- Left-click an item frame --- //
			if (
					event.target instanceof EntityItemFrame
					&& GeneralConfig.itemframeSounds
					)
			{
				EntityItemFrame itemframe = (EntityItemFrame)event.target;
				
				if (itemframe.getDisplayedItem() != null)
				{
					event.target.playSound(Reference.MOD_ID+":entity.itemframe.remove_item", 1.0F, 1.0F);
				}
				else
				{
					event.target.playSound(Reference.MOD_ID+":entity.itemframe.break", 1.0F, 1.0F);
				}
				
				return;
			}

			
			// --- Break a Lead Knot --- //
			if (
					event.target instanceof EntityLeashKnot
					&& GeneralConfig.leashSounds
					)
			{
				event.target.playSound(Reference.MOD_ID+":entity.leashknot.break", 1.0F, 1.0F);
				return;
			}
			
			
			// --- Break a painting (or some other hanging entity) --- //
			if (
					event.target instanceof EntityHanging
					&& GeneralConfig.paintingSounds
					)
			{
				event.target.playSound(Reference.MOD_ID+":entity.painting.break", 1.0F, 1.0F);
				return;
			}
		}
	}
	
	
	@SubscribeEvent
	public void onPlayerInteractEvent(PlayerInteractEvent event)
	{
		World world = event.world;
		
		if ( !world.isRemote )
		{
			EntityPlayer player = event.entityPlayer;
			ItemStack itemStack = player.getHeldItem();
			MovingObjectPosition movingobjectposition = getMovingObjectPositionFromPlayer(world, player, true);
			
			// --- Place a lead knot --- //
			if (	// This has to go FIRST in line, because the sound should play no matter what's in your hand.
					isPlayerLeadingAnimal(player, world, event.x, event.y, event.z)
					//&& itemStack != null
					//&& itemStack.getItem() == Items.lead
					&& world.getBlock(event.x, event.y, event.z).getRenderType() == 11 // "fence" type
					&& event.action == Action.RIGHT_CLICK_BLOCK
					&& GeneralConfig.leashSounds
					)
			{
				world.playSoundEffect(
						  event.x + 0.5
						, event.y + 0.5
						, event.z + 0.5
						, Reference.MOD_ID+":entity.leashknot.place", 1.0F, 1.0F);
				
				return;
			}
			
			
			// --- Hanging entities --- //
			
			String hangingSound="";
			Class hangingEntityClass=null;
			
			// --- Place an item frame --- //
			if (
					itemStack != null &&
					itemStack.getItem() == Items.item_frame
					&& event.action == Action.RIGHT_CLICK_BLOCK
					&& GeneralConfig.itemframeSounds
					)
			{
				hangingSound = "itemframe";
				hangingEntityClass = EntityItemFrame.class;
			}
			
			
			// --- Place a painting --- //
			if (
					itemStack != null &&
					itemStack.getItem() == Items.painting
					&& event.action == Action.RIGHT_CLICK_BLOCK
					&& GeneralConfig.paintingSounds
					)
			{
				hangingSound = "painting";
				hangingEntityClass = EntityPainting.class;
			}
			
			if(hangingEntityClass!=null && !hangingSound.equals(""))
			{
				if (event.face == 0) {return;} // Can't be placed on the bottom
		        else if (event.face == 1) {return;} // Can't be placed on the top
		        else
		        {
		            int i1 = Direction.facingToDirection[event.face];
		            
		            EntityHanging entityhanging = (EntityHanging)
		            		(hangingEntityClass == EntityPainting.class ?
		            				new EntityPainting(world, event.x, event.y, event.z, i1) :
		            					(hangingEntityClass == EntityItemFrame.class ?
		            							new EntityItemFrame(world, event.x, event.y, event.z, i1) :
		            								null));
		            
		            if (!player.canPlayerEdit(event.x, event.y, event.z, event.face, itemStack)) {return;} // Player can't modify this face
		            else
		            {
		                if (entityhanging != null && entityhanging.onValidSurface())
		                {
		                	// Play the sound here
		                	world.playSoundEffect(
		  						  event.x + 0.5
		  						, event.y + 0.5
		  						, event.z + 0.5
		  						, Reference.MOD_ID+":entity."+hangingSound+".place",
		  						1.0F, 1.0F);
		                	return;
		                }
		            }
		        }
			}
			
			
			// --- Play the stripped log sound here, cancel it in the client-side section --- //
			if (
					(Loader.isModLoaded("etfuturum") || Loader.isModLoaded("uptodate"))
					&& itemStack != null
					&& itemStack.getItem() instanceof ItemAxe
					&& event.action == Action.RIGHT_CLICK_BLOCK 
					&& (world.getBlock(event.x, event.y, event.z) == Blocks.log
						|| world.getBlock(event.x, event.y, event.z) == Blocks.log2)
					&& GeneralConfig.strippedlogSounds
					)
			{
				world.playSoundEffect(
						  event.x + 0.5
						, event.y + 0.5
						, event.z + 0.5
						, Reference.MOD_ID+":item.axe.strip",
						0.9F,
						world.rand.nextBoolean() ? 0.85F : 1F
						);
				return;
			}
			
			
			// --- Place an Ender Eye into a portal frame block --- //
			if (
					itemStack != null
					&& itemStack.getItem() == Items.ender_eye
					&& GeneralConfig.endereyeportalSounds
					)
			{
				if (
						event.action == Action.RIGHT_CLICK_BLOCK 
						&& world.getBlock(event.x, event.y, event.z) == Blocks.end_portal_frame
						)
				{
					if (!(((BlockEndPortalFrame)world.getBlock(event.x, event.y, event.z)).isEnderEyeInserted( world.getBlockMetadata(event.x, event.y, event.z) )))
					{
						// Play end eye insertion sound
						world.playSoundEffect(
								  event.x + 0.5
								, event.y + 0.5
								, event.z + 0.5
								, Reference.MOD_ID+":block.end_portal.eyeplace", 1.0F, 1.0F);
						
						// Check to see if a portal is opening
						Integer[] portalCenter = endPortalTriggering(world, event.x, event.y, event.z);
						
						if (portalCenter != null)
						{
							world.playSoundEffect(
									  portalCenter[0] + 0.5
									, portalCenter[1] + 0.5
									, portalCenter[2] + 0.5
									, Reference.MOD_ID+":block.end_portal.endportal",
									1.0F, 1.0F);
						}
						
						return;
					}
				}
			}
			
			
			// --- Hoeing --- //
			if (
					itemStack != null
					&& itemStack.getItem() instanceof ItemHoe
					&& event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK
					&& GeneralConfig.farmlandtillSounds
					)
		    {
				int side = event.face;
				
				if (side<=0) {return;} // I assume 0 means "bottom"
				else if (player.canPlayerEdit(event.x, event.y, event.z, side, itemStack)
		        		&& player.canPlayerEdit(event.x, event.y + 1, event.z, side, itemStack))
		        {
	            	if (
	            			(world.getBlock(event.x, event.y, event.z) == Blocks.dirt
	            			|| world.getBlock(event.x, event.y, event.z) == Blocks.grass)
	            			&& (world.getBlock(event.x, event.y+1, event.z) == Blocks.air)
	            			)
	            	{
	            		world.playSoundEffect(
	            				  event.x + 0.5
								, event.y + 1F // At the top face of the block
								, event.z + 0.5
								, Reference.MOD_ID+":item.hoe.till",
	    						1.0F, 1.0F);
	            		return;
	            	}
		        }
		    }
			
			
			// --- Redstone --- //
			if(
					itemStack != null
					&& itemStack.getItem()==Items.redstone
					&& event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK
					&& GeneralConfig.redstoneSounds
					)
			{
				int eventX_mutable = event.x;
				int eventY_mutable = event.y;
				int eventZ_mutable = event.z;
				
			    if (world.getBlock(eventX_mutable, eventY_mutable, eventZ_mutable) != Blocks.snow_layer)
		        {
			    	switch (event.face)
			    	{
			    	case 0:
			    		--eventY_mutable;
			    		break;
			    	case 1:
			    		++eventY_mutable;
			    		break;
			    	case 2:
			    		--eventZ_mutable;
			    		break;
			    	case 3:
			    		++eventZ_mutable;
			    		break;
			    	case 4:
			    		--eventX_mutable;
			    		break;
			    	case 5:
			    		++eventX_mutable;
			    		break;
			    	}
			    	
		            if (!world.isAirBlock(eventX_mutable, eventY_mutable, eventZ_mutable))
		            {
		            	// Can't put redstone here because it's not air
		                return;
		            }
		        }

		        if (!player.canPlayerEdit(eventX_mutable, eventY_mutable, eventZ_mutable, event.face, itemStack))
		        {
		        	// Can't put redstone here because player is disallowed
		            return;
		        }
		        else
		        {
		            if (Blocks.redstone_wire.canPlaceBlockAt(world, eventX_mutable, eventY_mutable, eventZ_mutable))
		            {
		            	// Here is where item would be consumed and block would be set
		            	// Redstone is successfully placed
			            world.playSoundEffect(
			            		  eventX_mutable+0.5
								, eventY_mutable+0.5 // At the top face of the block
								, eventZ_mutable+0.5
								, "dig.stone",
								1F, 1F);
			            return;
		            }
		            
		            
		        }
			}
			
			
			// --- Planting --- //
			if (
					itemStack != null
					&& itemStack.getItem() instanceof IPlantable
					&& event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK
					&& GeneralConfig.cropplantingSounds
					)
		    {
				int side = event.face;
				
				if (side != 1) {return;} // Must be top face
		        else if (player.canPlayerEdit(event.x, event.y, event.z, side, itemStack)
		        		&& player.canPlayerEdit(event.x, event.y + 1, event.z, side, itemStack))
		        {
		            if (world.getBlock(event.x, event.y, event.z).canSustainPlant(world, event.x, event.y, event.z, ForgeDirection.UP, (IPlantable)itemStack.getItem())
		            		&& world.isAirBlock(event.x, event.y + 1, event.z))
		            {
		            	// Mundane seeds
		            	if (world.getBlock(event.x, event.y, event.z) == Blocks.farmland)
		            	{
		            		world.playSoundEffect(
		            				  event.x + 0.5
									, event.y + 1F // At the top face of the block
									, event.z + 0.5
									, Reference.MOD_ID+":item.crop.plant",
									0.45F, world.rand.nextBoolean() ? 1.0F : 1.2F);
		            		return;
		            	}
		            	// Nether wart
		            	if (world.getBlock(event.x, event.y, event.z) == Blocks.soul_sand)
		            	{
		            		world.playSoundEffect(
		            				  event.x + 0.5
									, event.y + 1F // At the top face of the block
									, event.z + 0.5
									, Reference.MOD_ID+":item.nether_wart.plant",
									0.9F, world.rand.nextBoolean() ? 1.0F : 1.12F);
		            		return;
		            	}
		            }
		        }
		    }
			
			
			// --- Lilypad sounds --- //
			if (
					itemStack != null
					&& (
							itemStack.getItem() == Item.getItemFromBlock(Blocks.waterlily)
							|| itemStack.getItem().getUnlocalizedName().toLowerCase().contains("waterlily")
							|| itemStack.getItem().getUnlocalizedName().toLowerCase().contains("lilypad")
							)
					&& GeneralConfig.lilypadSounds
					)
			{
				if (movingobjectposition == null) {return;}
				else
				{
					if (movingobjectposition.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK)
					{
						int i = movingobjectposition.blockX;
						int j = movingobjectposition.blockY;
						int k = movingobjectposition.blockZ;
						
						if(!world.canMineBlock(player, i, j, k)) {return;}
						if(!player.canPlayerEdit(i, j, k, movingobjectposition.sideHit, itemStack)) {return;}
						
						boolean isValidLilypadPlace = false;
						
						if(
								world.getBlock(i, j, k) == Blocks.water
								&& world.getBlock(i, j+1, k) == Blocks.air
								) {isValidLilypadPlace = true;}
						
						if(isValidLilypadPlace && event.action == PlayerInteractEvent.Action.RIGHT_CLICK_AIR)
						{
							world.playSoundEffect(
								player.posX
								, player.posY
								, player.posZ
								, Reference.MOD_ID+":block.lily_pad.place", 1.0F, 1.0F);
							
							return;
						}
					}
				}
			}
			
			
			// --- Bottle fill sounds --- //
			if (
					itemStack != null
					&& itemStack.getItem() == Items.glass_bottle
					&& GeneralConfig.bottleSounds
					)
			{
				if (movingobjectposition == null) {return;}
				else
				{
					if (movingobjectposition.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK)
					{
						int i = movingobjectposition.blockX;
						int j = movingobjectposition.blockY;
						int k = movingobjectposition.blockZ;
						
						boolean isValidCauldron = (world.getBlock(i, j, k) == Blocks.cauldron && world.getBlockMetadata(i, j, k) > 0);
						
						if(!world.canMineBlock(player, i, j, k)) {return;}
						if(!player.canPlayerEdit(i, j, k, movingobjectposition.sideHit, itemStack)) {return;}
						
						boolean isWater = false;
						
						if(world.getBlock(i, j, k) == Blocks.water || world.getBlock(i, j, k) == Blocks.flowing_water) {isWater = true;}
						
						if(
								(isWater && event.action == PlayerInteractEvent.Action.RIGHT_CLICK_AIR) ||
								(isValidCauldron && event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK)
								)
						{
							world.playSoundEffect(
								player.posX
								, player.posY
								, player.posZ
								, Reference.MOD_ID+":item.bottle.fill", 1.0F, 1.0F);
							
							return;
						}
					}
				}
			}
			
			
			// --- Bucketing water to/from a cauldron --- //
			if (
					itemStack != null
					&& (
							itemStack.getItem() == Items.water_bucket
							//|| itemStack.getItem() == Items.bucket // Can't extract water in 1.7.10
							)
					&& GeneralConfig.bucketSounds
					)
			{
				if (movingobjectposition == null) {return;}
				else
				{
					if (movingobjectposition.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK)
					{
						int i = movingobjectposition.blockX;
						int j = movingobjectposition.blockY;
						int k = movingobjectposition.blockZ;
						
						boolean bucketIsFull = (itemStack.getItem()==Items.water_bucket);
						
						boolean isValidCauldron = (
								world.getBlock(i, j, k) == Blocks.cauldron
								&& (bucketIsFull ? world.getBlockMetadata(i, j, k)<3 : world.getBlockMetadata(i, j, k)==3)
								);

						if(!world.canMineBlock(player, i, j, k)) {return;}

						if(!player.canPlayerEdit(i, j, k, movingobjectposition.sideHit, itemStack)) {return;}
						
						if(isValidCauldron)
						{
							world.playSoundEffect(
									player.posX
									, player.posY
									, player.posZ
									, bucketIsFull ? Reference.MOD_ID+":item.bucket.pour_water" : Reference.MOD_ID+":item.bucket.fill_water", 1.0F, 1.0F);
							
							return;
						}
					}
				}
			}
			
			
			// --- Fishing Line --- //
			if (
					itemStack != null
					&& itemStack.getItem() instanceof ItemFishingRod
					&& GeneralConfig.fishingrodSounds
					&& event.action == PlayerInteractEvent.Action.RIGHT_CLICK_AIR
					)
			{
				if (player.fishEntity == null)
				{
					player.worldObj.playSoundEffect(
							player.posX
    						, player.posY
    						, player.posZ
    						, Reference.MOD_ID+":entity.fishing_bobber.throw", 1.0F, 0.42F);
				}
				else
				{
					player.worldObj.playSoundEffect(
							player.posX
    						, player.posY
    						, player.posZ
    						, Reference.MOD_ID+":entity.fishing_bobber.retrieve", 1.0F, 1.0F);
				}
				return;
			}
		}
	}
	
	
	@SubscribeEvent
	public void onEntityInteractEvent(EntityInteractEvent event) // Only fires when a player RIGHT-CLICKS an entity.
	{
		World world = event.target.worldObj;
		
		if (!world.isRemote)
		{
			Entity target = event.target;
			EntityPlayer player = event.entityPlayer;
			ItemStack heldItemStack = player.getHeldItem();//.getHeldItem(); // Changed this out because of walking on top of stripped logs
			
			
			// --- Milk Mooshroom --- //
			if (
					target instanceof EntityMooshroom
					&& heldItemStack != null
					&& (heldItemStack.getItem() == Items.bucket || heldItemStack.getItem() == Items.bowl)
					&& !player.capabilities.isCreativeMode
					&& GeneralConfig.mooshroommilkingSounds
					)
			{
				world.playSoundEffect(
						  target.posX
						, target.posY
						, target.posZ
						, Reference.MOD_ID+":entity.mooshroom.milk", 1.0F, 0.9F + (world.rand.nextInt(3)-1)*0.1F);
				
				return;
			}
			
			
			// --- Milk Cow --- //
			if (
					target instanceof EntityCow
					&& heldItemStack != null
					&& heldItemStack.getItem() == Items.bucket
					&& !player.capabilities.isCreativeMode
					&& GeneralConfig.cowmilkingSounds
					)
			{
				world.playSoundEffect(
						  target.posX
						, target.posY
						, target.posZ
						, Reference.MOD_ID+":entity.cow.milk", 1.0F, 1.0F);
				
				return;
			}
						
			
			// --- Add/Rotate within Item Frame --- //
			if (
					target instanceof EntityItemFrame
					&& GeneralConfig.itemframeSounds
					) // Only fires when a player RIGHT-CLICKS a hanging entity.
			{
				EntityItemFrame itemframe = (EntityItemFrame)target;
				
				if (
						heldItemStack != null
						&& itemframe.getDisplayedItem() == null
						) // Place item into frame
				{
					world.playSoundEffect(
							  target.posX + 0.5
							, target.posY + 0.5
							, target.posZ + 0.5
							, Reference.MOD_ID+":entity.itemframe.add_item", 1.0F, 1.0F);
					
					return;
				}
				
				if (itemframe.getDisplayedItem() != null) // There is an item in there. Rotate it.
				{
					world.playSoundEffect(
						      target.posX + 0.5
							, target.posY + 0.5
							, target.posZ + 0.5
							, Reference.MOD_ID+":entity.itemframe.rotate_item", 1.0F, 1.0F);

					return;
				}
			}
			
			
			// --- Remove a Lead Knot --- //
			if (
					target instanceof EntityLeashKnot
					&& GeneralConfig.leashSounds
					)
			{
				world.playSoundEffect(
						  target.posX + 0.5
						, target.posY + 0.5
						, target.posZ + 0.5
						, Reference.MOD_ID+":entity.leashknot.break", 1.0F, 1.0F);
				
				return;
			}
		}
	}
	
	
	@SubscribeEvent
	public void onEntityJoinWorldEvent(EntityJoinWorldEvent event)
	{
		if (!event.entity.worldObj.isRemote)
		{
			// --- Throw Ender Eye --- //
			if (
					GeneralConfig.endereyelaunchSounds
					&& event.entity instanceof EntityEnderEye
					)
			{
				event.world.playSoundEffect(
						  event.entity.posX
						, event.entity.posY
						, event.entity.posZ
						, Reference.MOD_ID+":entity.endereye.endereye_launch", 1.0F, 1.0F);
				
				return;
			}
		}
	}
	
	@SubscribeEvent
	public void onFillBucketEvent(FillBucketEvent event)
	{
		Block isFull = Blocks.air;
		try {isFull = ReflectionHelper.getPrivateValue(ItemBucket.class, (ItemBucket)event.current.getItem(), new String[]{"isFull", "field_77876_a"});}
		catch (Exception e) {}
		
		if (!event.world.isRemote)
		{
			MovingObjectPosition target = event.target;
			
			
			// --- Pour water --- //
			if ( 
					GeneralConfig.bucketSounds &&
					event.current.getItem()==Items.water_bucket
					)
			{
				event.world.playSoundEffect(
						  target.blockX + 0.5
						, target.blockY + 0.5
						, target.blockZ + 0.5
						, Reference.MOD_ID+":item.bucket.pour_water", 1.0F, 1.0F);
				
				return;
			}
			
			
			// --- Pour something else --- //
			if ( 
					GeneralConfig.bucketSounds //&&
					&& !(isFull == Blocks.air)
					)
			{
				event.world.playSoundEffect(
							  target.blockX + 0.5
							, target.blockY + 0.5
							, target.blockZ + 0.5
							, Reference.MOD_ID+":item.bucket.pour_lava", 1.0F, 1.0F);
				
				return;
			}
			
			
			// --- Fill with water --- //
			if ( 
					GeneralConfig.bucketSounds &&
					event.current.getItem()==Items.bucket
					&& event.world.getBlock(target.blockX, target.blockY, target.blockZ).getMaterial() == Material.water
					)
			{
				event.world.playSoundEffect(
							  target.blockX + 0.5
							, target.blockY + 0.5
							, target.blockZ + 0.5
							, Reference.MOD_ID+":item.bucket.fill_water", 1.0F, 1.0F);
				
				return;
			}
			
			
			// --- Fill with something else --- //
			if ( 
					GeneralConfig.bucketSounds &&
					event.current.getItem()==Items.bucket
					&& event.world.getBlock(target.blockX, target.blockY, target.blockZ).getMaterial().isLiquid()
					)
			{
				event.world.playSoundEffect(
							  target.blockX + 0.5
							, target.blockY + 0.5
							, target.blockZ + 0.5
							, Reference.MOD_ID+":item.bucket.fill_lava", 1.0F, 1.0F);
				
				return;
			}
		}
	}
	
	
    /**
     * For attaching IEEP - v2.0.0
     */
    @SubscribeEvent
    public void onEntityConstructing(EntityConstructing event)
    {
    	if (event.entity instanceof EntityPlayer)
    	{
    		PlayerArmorTracker.register((EntityPlayer) event.entity);
    	}
    }
	
	
	// Pasted this in from Item.class because the vanilla version is protected
	protected static MovingObjectPosition getMovingObjectPositionFromPlayer(World worldIn, EntityPlayer playerIn, boolean useLiquids)
	{
		float f = 1.0F;
		float f1 = playerIn.prevRotationPitch + (playerIn.rotationPitch - playerIn.prevRotationPitch) * f;
		float f2 = playerIn.prevRotationYaw + (playerIn.rotationYaw - playerIn.prevRotationYaw) * f;
		double d0 = playerIn.prevPosX + (playerIn.posX - playerIn.prevPosX) * (double)f;
		double d1 = playerIn.prevPosY + (playerIn.posY - playerIn.prevPosY) * (double)f + (double)(worldIn.isRemote ? playerIn.getEyeHeight() - playerIn.getDefaultEyeHeight() : playerIn.getEyeHeight()); // isRemote check to revert changes to ray trace position due to adding the eye height clientside and player yOffset differences
		double d2 = playerIn.prevPosZ + (playerIn.posZ - playerIn.prevPosZ) * (double)f;
		Vec3 vec3 = Vec3.createVectorHelper(d0, d1, d2);
		float f3 = MathHelper.cos(-f2 * 0.017453292F - (float)Math.PI);
		float f4 = MathHelper.sin(-f2 * 0.017453292F - (float)Math.PI);
		float f5 = -MathHelper.cos(-f1 * 0.017453292F);
		float f6 = MathHelper.sin(-f1 * 0.017453292F);
		float f7 = f4 * f5;
		float f8 = f3 * f5;
		double d3 = 5.0D;
		if(playerIn instanceof EntityPlayerMP)
		{
			d3 = ((EntityPlayerMP)playerIn).theItemInWorldManager.getBlockReachDistance();
		}
		Vec3 vec31 = vec3.addVector((double)f7 * d3, (double)f6 * d3, (double)f8 * d3);
		return worldIn.func_147447_a(vec3, vec31, useLiquids, !useLiquids, false);
	}
	
	
	/**
	 * This takes in an x, y, z and checks whether that block is a an ender portal frame
	 * WITHOUT an eye, and is a component of a portal where all the OTHER blocks have eyes.
	 * In essence, this is called when right-clicking an empty frame block while holding
	 * an ender eye so it will tell you whether you've just activated an End portal. 
	 */
	private Integer[] endPortalTriggering (World world, int blockX, int blockY, int blockZ)
	{
		Block targetBlock = world.getBlock(blockX, blockY, blockZ);
		int targetMeta = world.getBlockMetadata(blockX, blockY, blockZ);
		
		if ( 
				targetBlock == Blocks.end_portal_frame
				&& (targetMeta & 4) == 0 // No Ender Eye inserted
				)
		{
			// This is an end frame without an ender eye.
			
			Integer[] xOffsetsToScan = new Integer[]{0,1,2,2,2,1,0,-1,-2,-2,-2,-1};
			Integer[] zOffsetsToScan = new Integer[]{-2,-2,-1,0,1,2,2,2,1,0,-1,-2};
			Integer[] metaRequired   = new Integer[]{4,4,5,5,5,6,6,6,7,7,7,4};
			
			int portalCenterOffsetX = targetMeta==3 ? 2 : (targetMeta==1 ? -2 : 0);
			int portalCenterOffsetZ = targetMeta==0 ? 2 : (targetMeta==2 ? -2 : 0);
			
			Block possibleFrameBlock=null;
			int possibleFrameMeta=-1;
			
			
			for (int nudge = -1; nudge <= 1; nudge++) // The frame block might not be the center of a side, so check each of the three possible offsets
			{
				int portalCenterOffsetNudgeX = targetMeta%2==0 ? nudge : 0;
				int portalCenterOffsetNudgeZ = (targetMeta+1)%2==0 ? nudge : 0;
				
				int eyedCorrectFramePieces = 0;
				int uneyedCorrectFramePieces = 0;
				
				// Scan all 12 blocks around the presumed center of the portal
				for (int i=0; i < xOffsetsToScan.length; i++)
				{
					possibleFrameBlock = world.getBlock(
							blockX + portalCenterOffsetX + xOffsetsToScan[i] + portalCenterOffsetNudgeX,
							blockY,
							blockZ + portalCenterOffsetZ + zOffsetsToScan[i] + portalCenterOffsetNudgeZ
							);
					
					possibleFrameMeta = world.getBlockMetadata(
							blockX + portalCenterOffsetX + xOffsetsToScan[i] + portalCenterOffsetNudgeX,
							blockY,
							blockZ + portalCenterOffsetZ + zOffsetsToScan[i] + portalCenterOffsetNudgeZ
							);
					
					if (possibleFrameBlock == Blocks.end_portal_frame)
					{
						if (possibleFrameMeta == metaRequired[i]) {eyedCorrectFramePieces++;}
						else if (possibleFrameMeta == metaRequired[i]-4) {uneyedCorrectFramePieces++;}
						else {break;}
					}
				}
				
				if (eyedCorrectFramePieces==11 && uneyedCorrectFramePieces==1)
				{
					// This is a newly-formed End Portal!
					return new Integer[]{
							blockX + portalCenterOffsetX + portalCenterOffsetNudgeX,
							blockY,
							blockZ + portalCenterOffsetZ + portalCenterOffsetNudgeZ
									};
				}
			}
		}
		
		return null;
	}
	
	
	/**
	 * Copied over from ItemLead.class
	 * This method checks whether you're leading an animal and have clicked on a fence,
	 * causing you to tie the lead to the fence.
	 * If I were to use the original, it would cause issues when called only client-side.
	 * The difference with this version is that it doesn't cause you to tie the lead.
	 */
    public static boolean isPlayerLeadingAnimal(EntityPlayer player, World world, int posX, int posY, int posZ)
    {
        EntityLeashKnot entityleashknot = EntityLeashKnot.getKnotForBlock(world, posX, posY, posZ);
        boolean flag = false;
        double boxRadius = 7.0D;
        List list = world.getEntitiesWithinAABB(EntityLiving.class, AxisAlignedBB.getBoundingBox((double)posX - boxRadius, (double)posY - boxRadius, (double)posZ - boxRadius, (double)posX + boxRadius, (double)posY + boxRadius, (double)posZ + boxRadius));

        if (list != null)
        {
            Iterator iterator = list.iterator();

            while (iterator.hasNext())
            {
                EntityLiving entityliving = (EntityLiving)iterator.next();
                
                flag = (entityliving.getLeashed()
                		&& entityliving.getLeashedToEntity() == player
                		&& entityleashknot == null);
            }
        }
        
        return flag;
    }
	
    
    /**
     * Invoking the general EntityLivingBase volume method
     */
	static float getEntityLivingVolume(EntityLivingBase livingBase, Class classIn)
	{
		float entityVolume = 1F;
		Method getSoundVolume_reflected = ReflectionHelper.findMethod(classIn, livingBase, new String[]{"getSoundVolume", "func_70599_aP"});
		try {entityVolume = (Float)getSoundVolume_reflected.invoke(livingBase);}
		catch (Exception e) {} // Failed to reflect Witch volume
		
		return entityVolume;
	}
	

    /**
     * Invoking the general EntityLivingBase pitch method
     */
	static float getEntityLivingPitch(EntityLivingBase livingBase, Class classIn)
	{
		float entityPitch = 1F;
		Method getSoundPitch_reflected = ReflectionHelper.findMethod(classIn, livingBase, new String[]{"getSoundPitch", "func_70647_i"});
		try {entityPitch = (Float)getSoundPitch_reflected.invoke(livingBase);}
		catch (Exception e) {
			entityPitch = livingBase.isChild() ? (livingBase.worldObj.rand.nextFloat() - livingBase.worldObj.rand.nextFloat()) * 0.2F + 1.5F : (livingBase.worldObj.rand.nextFloat() - livingBase.worldObj.rand.nextFloat()) * 0.2F + 1.0F;
			} // Failed to reflect Witch pitch
		
		return entityPitch;
	}
	
}
