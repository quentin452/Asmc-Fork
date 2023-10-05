package astrotibs.asmc.proxy;

import astrotibs.asmc.ModASMC;
import astrotibs.asmc.sounds.BeaconAmbientSound;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntityBeacon;

public class ClientProxy extends CommonProxy
{
	
	@Override
	public void postInit(FMLPostInitializationEvent e)
	{
		super.postInit(e);
		Thread versionCheckThread = new Thread(ModASMC.versionChecker, "Version Check");
		versionCheckThread.start();
	}
	
	public static void handleBeaconSound(TileEntityBeacon beacon)
	{
		Minecraft.getMinecraft().getSoundHandler().playSound(new BeaconAmbientSound(beacon));
	}
}
