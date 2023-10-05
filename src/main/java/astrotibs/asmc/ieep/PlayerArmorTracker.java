package astrotibs.asmc.ieep;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;

public class PlayerArmorTracker implements IExtendedEntityProperties
{
	public final static String KEY_TAGGROUPNAME = "asmc_armortracker";

	public final static String KEY_BOOTS = "Boots";
	public final static String KEY_LEGGINGS = "Leggings";
	public final static String KEY_CHESTPLATE = "Chestplate";
	public final static String KEY_HELMET = "Helmet";
	
	private NBTTagCompound boots;
	private NBTTagCompound leggings;
	private NBTTagCompound chestplate;
	private NBTTagCompound helmet;
	
	
	public PlayerArmorTracker(EntityPlayer player)
	{
		this.boots = new NBTTagCompound();
		this.leggings = new NBTTagCompound();
		this.chestplate = new NBTTagCompound();
		this.helmet = new NBTTagCompound();
	}
	
	/**
	* Used to register these extended properties for the player during EntityConstructing event
	* This method is for convenience only; it will make your code look nicer
	*/
	public static final void register(EntityPlayer player)
	{
		player.registerExtendedProperties(PlayerArmorTracker.KEY_TAGGROUPNAME, new PlayerArmorTracker(player));
	}
	
	/**
	* Returns properties for player
	* This method is for convenience only; it will make your code look nicer
	*/
	public static final PlayerArmorTracker get(EntityPlayer player)
	{
		return (PlayerArmorTracker) player.getExtendedProperties(KEY_TAGGROUPNAME);
	}
	
	
	
	@Override
	public void saveNBTData(NBTTagCompound topTag)
	{
		NBTTagCompound ieepTags = new NBTTagCompound();
		
		ieepTags.setTag(KEY_BOOTS, this.boots);
		ieepTags.setTag(KEY_LEGGINGS, this.leggings);
		ieepTags.setTag(KEY_CHESTPLATE, this.chestplate);
		ieepTags.setTag(KEY_HELMET, this.helmet);
		
		topTag.setTag(KEY_TAGGROUPNAME, ieepTags); 
	}

	@Override
	public void loadNBTData(NBTTagCompound topTag)
	{
		NBTTagCompound ieepTags = (NBTTagCompound)topTag.getTag(KEY_TAGGROUPNAME);
		
		if (ieepTags != null)
		{
			this.boots = ieepTags.getCompoundTag(KEY_BOOTS);
			this.leggings = ieepTags.getCompoundTag(KEY_LEGGINGS);
			this.chestplate = ieepTags.getCompoundTag(KEY_CHESTPLATE);
			this.helmet = ieepTags.getCompoundTag(KEY_HELMET);
		}
		else
		{
			this.boots = new NBTTagCompound();
			this.leggings = new NBTTagCompound();
			this.chestplate = new NBTTagCompound();
			this.helmet = new NBTTagCompound();
		}
	}

	@Override
	public void init(Entity entity, World world)
	{
		// Sorry, nothing
	}
	
	
	// --- Setters --- //
	public void setBoots(NBTTagCompound item) {this.boots = item;}
	public void setLeggings(NBTTagCompound item) {this.leggings = item;}
	public void setChestplate(NBTTagCompound item) {this.chestplate = item;}
	public void setHelmet(NBTTagCompound item) {this.helmet = item;}
	
	// --- Getters --- //
	public NBTTagCompound getBoots() {return this.boots;}
	public NBTTagCompound getLeggings() {return this.leggings;}
	public NBTTagCompound getChestplate() {return this.chestplate;}
	public NBTTagCompound getHelmet() {return this.helmet;}
	
	
	/**
	 * Converts an itemstack into a useable NBTTagCompound
	 */
	public static NBTTagCompound saveItemStackToNBT(ItemStack itemstack)
    {
    	NBTTagCompound nbttagcompound = new NBTTagCompound();
    	if (itemstack!=null) {itemstack.writeToNBT(nbttagcompound);}
    	return nbttagcompound != null ? nbttagcompound : new NBTTagCompound();
    }
	
}
