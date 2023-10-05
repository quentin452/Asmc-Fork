package astrotibs.asmc.proxy;

import astrotibs.asmc.config.GeneralConfig;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;

public class CommonProxy
{
	protected Configuration config;
	
	public void preInit(FMLPreInitializationEvent e) {}
	
	public void init(FMLInitializationEvent e)
	{
		MinecraftForge.EVENT_BUS.register(new GeneralConfig());
	}
	
	public void postInit(FMLPostInitializationEvent e) {}
	
}
