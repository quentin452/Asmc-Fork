package astrotibs.asmc.config;

import java.util.ArrayList;
import java.util.List;

import astrotibs.asmc.ModASMC;
import astrotibs.asmc.utility.Reference;
import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.GuiMessageDialog;
import cpw.mods.fml.client.config.IConfigElement;
import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import cpw.mods.fml.client.event.ConfigChangedEvent.PostConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.Event.Result;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigElement;

/**
 * @author AstroTibs
 * Adapted from Jabelar's Magic Beans:
 * https://github.com/jabelar/MagicBeans-1.7.10/blob/52dc91bfa2e515dcd6ebe116453dc98951f03dcb/src/main/java/com/blogspot/jabelarminecraft/magicbeans/gui/GuiConfig.java
 * and FunWayGuy's EnviroMine:
 * https://github.com/EnviroMine/EnviroMine-1.7/blob/1652062539adba36563450caefa1879127ccb950/src/main/java/enviromine/client/gui/menu/config/EM_ConfigMenu.java
 */
public class ASMCGuiConfig extends GuiConfig 
{

	public ASMCGuiConfig(GuiScreen guiScreen)
	{
		super(
				guiScreen,         // parentScreen: the parent GuiScreen object
				getElements(),     // configElements: a List of IConfigProperty objects
                Reference.MOD_ID,  // modID: the mod ID for the mod whose config settings will be edited
				false,             // allRequireWorldRestart: send true if all configElements on this screen require a world restart
				false,             // allRequireMcRestart: send true if all configElements on this screen require MC to be restarted
				getHeader()        // title: the desired title for this screen. For consistency it is recommended that you pass the path of the config file being edited.
				);
	}
	

	// I was going to use this to warn the player if they had an old config folder but I kind of don't care
	private static String getHeader() {
		return EnumChatFormatting.YELLOW 
				+ ModASMC.configDirectory.getAbsolutePath();
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static List<IConfigElement> getElements()
	{
		List<IConfigElement> subCats = new ArrayList<IConfigElement>();
		//List<IConfigElement> subCats;
		ConfigCategory cc;
		
		// General config
		subCats = new ArrayList<IConfigElement>();
		
		cc = GeneralConfig.config.getCategory("general");
		cc.setComment("The version checker is here");
		subCats.add( new ConfigElement(cc) );
		
		cc = GeneralConfig.config.getCategory("sounds: ambient");
		cc.setComment("Sound effects related to ambience");
		subCats.add( new ConfigElement(cc) );
		
		cc = GeneralConfig.config.getCategory("sounds: block");
		cc.setComment("Sound effects related to blocks");
		subCats.add( new ConfigElement(cc) );
		
		cc = GeneralConfig.config.getCategory("sounds: entity");
		cc.setComment("Sound effects related to mobs");
		//cc.setRequiresMcRestart(true);
		subCats.add( new ConfigElement(cc) );
		
		cc = GeneralConfig.config.getCategory("sounds: item");
		cc.setComment("Sound effects related to items");
		//cc.setRequiresMcRestart(true);
		subCats.add( new ConfigElement(cc) );
		
		cc = GeneralConfig.config.getCategory("sounds: player");
		cc.setComment("Sound effects related to the player");
		//cc.setRequiresMcRestart(true);
		subCats.add( new ConfigElement(cc) );
		
		
		return subCats;
	}
	
	
	@Override
    protected void actionPerformed(GuiButton button)
    {
        if (button.id == 2000) // The topmost "Done" button
        {
            boolean flag = true;
            
            try
            {
                if ((configID != null || this.parentScreen == null || !(this.parentScreen instanceof ASMCGuiConfig)) 
                        && (this.entryList.hasChangedEntry(true)))
                {
                    boolean requiresMcRestart = this.entryList.saveConfigElements();
                    
                    if (Loader.isModLoaded(modID))
                    {
                        ConfigChangedEvent event = new OnConfigChangedEvent(modID, configID, isWorldRunning, requiresMcRestart);
                        FMLCommonHandler.instance().bus().post(event);
                        
                        if (!event.getResult().equals(Result.DENY))
                            FMLCommonHandler.instance().bus().post(new PostConfigChangedEvent(modID, configID, isWorldRunning, requiresMcRestart));
                        	GeneralConfig.loadConfiguration(); // To force-sync the config options
                        if (requiresMcRestart)
                        {
                            flag = false;
                            mc.displayGuiScreen(new GuiMessageDialog(parentScreen, "fml.configgui.gameRestartTitle", new ChatComponentText(I18n.format("fml.configgui.gameRestartRequired")), "fml.configgui.confirmRestartMessage"));
                        }
                        
                        if (this.parentScreen instanceof ASMCGuiConfig)
                            ((ASMCGuiConfig) this.parentScreen).needsRefresh = true;
                    }
                }
            }
            catch (Throwable e)
            {
                e.printStackTrace();
            }
            
            if (flag)
            {
            	this.mc.displayGuiScreen(this.parentScreen);
            }
            
        }
    }
	
	
	
}
