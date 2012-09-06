package me.ellbristow.mychunk;

/**
 *  Name: WorldGuardHook.java
 *  Date: 23:35:08 - 18 aug 2012
 * 
 *  Author: LucasEmanuel @ bukkit forums
 *  
 *  
 *  Description:
 *  Edited by ellbristow for MyChunk
 * 
 */

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class WorldGuardHook {
	
	public static boolean isRegion(Location location) {
		if (getWorldGuard() == null) {
                    return false;
                }
		ApplicableRegionSet ars = getApplicableRegionSet(location);
		
		if(ars.size() == 0)
			return false;
		else
			return true;
	}
	
	public static LocalPlayer getLocalPlayer(Player player) {
		WorldGuardPlugin wg = getWorldGuard();
		
		return wg.wrapPlayer(player);
	}
	
	public static boolean isMember(Player player, Location location) {
		ApplicableRegionSet ars = getApplicableRegionSet(location);
		for(ProtectedRegion pr : ars) {
			if(pr.isMember(getLocalPlayer(player)))
				return true;
		}
		
		return false;
	}
	
	public static ApplicableRegionSet getApplicableRegionSet(Location location) {
        WorldGuardPlugin wg = getWorldGuard();
        if (wg == null) {
            return null;
        }
        RegionManager rm = wg.getRegionManager(location.getWorld());
        if (rm == null) {
            return null;
        }
        return rm.getApplicableRegions(com.sk89q.worldguard.bukkit.BukkitUtil.toVector(location));
    }
	
	public static WorldGuardPlugin getWorldGuard() {
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");
 
        // WorldGuard may not be loaded
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            return null;
        }
        return (WorldGuardPlugin) plugin;
    }
}

