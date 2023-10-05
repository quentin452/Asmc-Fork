package astrotibs.asmc.version;

import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.IOUtils;

import astrotibs.asmc.ModASMC;
import astrotibs.asmc.config.GeneralConfig;
import astrotibs.asmc.utility.LogHelper;
import astrotibs.asmc.utility.Reference;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.ForgeHooks;

/**
 * Adapted from Jabelar's tutorials"
 * http://jabelarminecraft.blogspot.com/p/minecraft-forge-1721710-making-mod.html
 * @author AstroTibs
 */
public class VersionChecker implements Runnable {
	
	private static boolean isLatestVersion = false;
	private static boolean warnaboutfailure = false; // Added in v3.1.1
    private static String latestVersion = "";
    
	@Override
	public void run() {
		
        InputStream in = null;
        
        try {
            in = new URL(Reference.VERSION_CHECKER_URL).openStream();
        } 
        catch 
        (Exception e)  {} // Blanked in v3.1.1
        
        try {
            latestVersion = IOUtils.readLines(in).get(0);
        }
        catch (Exception e)  {
        	
        	if (!warnaboutfailure) {
        		// Added in v3.1.1
        		LogHelper.error("Could not connect with server to compare " + Reference.MOD_NAME + " version");
        		LogHelper.error("You can check for new versions at "+Reference.URL);
        		warnaboutfailure=true;
        	}
        }
        finally {
            IOUtils.closeQuietly(in);
        }
        
        isLatestVersion = Reference.VERSION.equals(latestVersion);
        
        if ( !this.isLatestVersion() && !latestVersion.equals("") && !latestVersion.equals(null) )
        {
        	LogHelper.info("This version of "+Reference.MOD_NAME+" (" + Reference.VERSION + ") differs from the latest version: " + latestVersion);
        }
    }
	
    public boolean isLatestVersion()
    {
    	return isLatestVersion;
    }
    
    public String getLatestVersion()
    {
    	return latestVersion;
    }
	
    /**
     * PlayerTickEvent is going to be used for version checking.
     * @param event
     */
    
    @SubscribeEvent(priority=EventPriority.NORMAL, receiveCanceled=true)
    public void onPlayerTickEvent(PlayerTickEvent event) {
    	
        if (
        		event.player.worldObj.isRemote
        		&& event.phase == Phase.END // Stops doubling the checks unnecessarily -- v3.2.4
            	&& event.player.ticksExisted<=50
    			&& event.player.ticksExisted%10==0
        		)
        {
        	// V3.0.1: Used to repeat the version check
        	if (
        			(latestVersion.equals(null) || latestVersion.equals(""))
        			&& !warnaboutfailure // Skip the "run" if a failure was detected
        			)
        	{
        		run();
        	}
        	// Ordinary version checker
        	if (
        			!ModASMC.haveWarnedVersionOutOfDate
            		&& GeneralConfig.versionChecker
            		&& !ModASMC.versionChecker.isLatestVersion()
            		&& !latestVersion.equals(null)
            		&& !latestVersion.equals("")
            		&& !(Reference.VERSION).contains("DEV")
        			)
        	{
        		
                event.player.addChatComponentMessage(
                		new ChatComponentText(
                				EnumChatFormatting.LIGHT_PURPLE + Reference.MOD_NAME + 
                				EnumChatFormatting.RESET + " version " + EnumChatFormatting.GREEN + this.getLatestVersion() + EnumChatFormatting.RESET +
                				" is available! Get it at:"
                		 ));
                event.player.addChatComponentMessage(
                		ForgeHooks.newChatWithLinks(Reference.URL // Made clickable in v3.1.1
                				//EnumChatFormatting.GRAY + Reference.URL + EnumChatFormatting.RESET
                		 ));
                // V3.0.1: Moved inside the "if" condition so that it will only stop version checking when it's confirmed
                ModASMC.haveWarnedVersionOutOfDate = true;
        	}
        	
        }
    	
    }
    
}
