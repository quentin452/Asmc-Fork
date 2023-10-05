package astrotibs.asmc.sounds;

import astrotibs.asmc.utility.Reference;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.BlockBeacon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

@SideOnly(Side.CLIENT)
public class BeaconAmbientSound extends MovingSound
{
	private final TileEntityBeacon beacon;
	
	public BeaconAmbientSound(TileEntityBeacon beacon)
	{
		super(new ResourceLocation(Reference.MOD_ID+":block.beacon.ambient"));
		
		this.beacon = beacon;
        this.field_147666_i = ISound.AttenuationType.NONE; //field_147666_i is attenuationType in 1.8
        this.repeat = false;
        this.field_147665_h = 0; //field_147665_h is repeatDelay in 1.8
	}

	@Override
	public void update()
	{
		if (
				this.beacon != null
				&& (beacon.getWorldObj().getBlock(beacon.xCoord, beacon.yCoord, beacon.zCoord) instanceof BlockBeacon)
				&& this.beacon.getLevels() > 0)
		{
            this.xPosF = (float)this.beacon.xCoord;
            this.yPosF = (float)this.beacon.yCoord;
            this.zPosF = (float)this.beacon.zCoord;
            
            EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
			World world = Minecraft.getMinecraft().theWorld;
            
            float distance = MathHelper.sqrt_double(
            		(this.xPosF-player.posX)*(this.xPosF-player.posX) +
            		(this.yPosF-player.posY)*(this.yPosF-player.posY) +
            		(this.zPosF-player.posZ)*(this.zPosF-player.posZ));
            
            this.volume = MathHelper.clamp_float((1F - (distance/7F))*0.9F, 0F, 1F);
		}
		else
		{
			this.donePlaying = true;
		}
	}
}
