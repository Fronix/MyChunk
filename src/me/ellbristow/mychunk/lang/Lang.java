package me.ellbristow.mychunk.lang;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;


public class Lang {

    private static FileConfiguration langStore;
    private static HashMap<String, String> lang = new HashMap<String, String>();
    
    public static String get(String key) {
        return lang.get(key);
    }
    
    static {
        reload();
    }
    
    public static void reload() {
        File langFile = new File(Bukkit.getPluginManager().getPlugin("MyChunk").getDataFolder(),"lang.yml");
        langStore = YamlConfiguration.loadConfiguration(langFile);
        
        lang.clear();
        
        // General
        loadLangPhrase("Yes", "Yes");
        loadLangPhrase("No", "No");
        loadLangPhrase("true", "True");
        loadLangPhrase("false", "False");
        loadLangPhrase("Unowned", "Unowned");
        loadLangPhrase("Server", "Server");
        loadLangPhrase("Price", "Price");
        loadLangPhrase("Player", "Player");
        loadLangPhrase("Everyone", "EVERYONE");
        loadLangPhrase("None", "None");
        loadLangPhrase("By", "by");
        loadLangPhrase("Disabled", "Disabled");
        loadLangPhrase("All", "ALL");
        loadLangPhrase("Build", "Build");
        loadLangPhrase("Destroy", "Destroy");
        loadLangPhrase("AccessChests", "Access Chests");
        loadLangPhrase("IgniteBlocks", "Ignite Blocks");
        loadLangPhrase("DropLava", "Drop Lava");
        loadLangPhrase("DropWater", "Drop Water");
        loadLangPhrase("OpenWoodenDoors", "Open Wooden Doors");
        loadLangPhrase("UseButtonsLevers", "Use Buttons/Levers etc");
        
        // Info
        loadLangPhrase("ChunkForSale", "Chunk For Sale");
        loadLangPhrase("AmountDeducted", "was deducted from your account");
        loadLangPhrase("BoughtFor", "bought one of your chunks for");
        loadLangPhrase("ChunkClaimed", "Chunk claimed!");
        loadLangPhrase("ChunkClaimedFor", "Chunk claimed for");
        loadLangPhrase("ChunkUnclaimed", "Chunk unclaimed!");
        loadLangPhrase("ChunkUnclaimedFor", "Chunk unclaimed for");
        loadLangPhrase("YouOwn", "You own this chunk!");
        loadLangPhrase("OwnedBy", "This Chunk is owned by");
        loadLangPhrase("AllowedPlayers", "Allowed Players");
        loadLangPhrase("PermissionsUpdated", "Permissions updated!");
        loadLangPhrase("ChunkIs", "This chunk is");
        loadLangPhrase("StartClaimArea1", "Claim Area Started!");
        loadLangPhrase("ClaimAreaCancelled", "Area claim cancelled!");
        loadLangPhrase("StartClaimArea2", "Place a second [ClaimArea] sign to claim all chunks in the area.");
        loadLangPhrase("YouWereCharged", "You were charged");
        loadLangPhrase("ChunksClaimed", "Chunks Claimed");
        loadLangPhrase("ChunksOwned", "Owned Chunks");
        loadLangPhrase("TotalClaimedChunks", "Total Claimed Chunks");
        loadLangPhrase("Yours", "Yours");
        loadLangPhrase("Unlimited", "Unlimited");
        loadLangPhrase("DefaultMax", "Default Max Chunks Per Player");
        loadLangPhrase("Chunk", "Max Chunks");
        loadLangPhrase("AllowNeighbours", "Allow Neighbours");
        loadLangPhrase("ChunkPrice", "Chunk Price");
        loadLangPhrase("AllowOverbuy", "Allow Overbuy");
        loadLangPhrase("OverbuyFee", "Overbuy Fee");
        loadLangPhrase("ProtectUnclaimed", "Protect Unclaimed");
        loadLangPhrase("Resales", "resales");
        loadLangPhrase("UnclaimRefunds", "Unclaim Refunds");
        loadLangPhrase("DaysWithoutLogin", "day(s) with no login");
        loadLangPhrase("ClaimExpiry", "Claim Expiry");
        loadLangPhrase("AllowNether", "Allow Nether");
        loadLangPhrase("AllowEnd", "Allow End");
        loadLangPhrase("PermissionFlags", "Permission Flags");
        loadLangPhrase("Reloaded", "Mychunk files have been reloaded!");
        loadLangPhrase("ToggleNetherCannot", "Users now CANNOT claim chunks in Nether worlds");
        loadLangPhrase("ToggleNetherCan", "Users now CAN claim chunks in Nether worlds");
        loadLangPhrase("ToggleEndCannot", "Users now CANNOT claim chunks in End worlds");
        loadLangPhrase("ToggleEndCan", "Users now CAN claim chunks in End worlds");
        
        
        //Errors
        loadLangPhrase("AlreadyOwner", "You already own this chunk!");
        loadLangPhrase("AlreadyOwned", "This Chunk is already owned by");
        loadLangPhrase("AlreadyOwn", "You already own");
        loadLangPhrase("ChunkNotOwned", "This chunk is not owned!");
        loadLangPhrase("Chunks", "chunks");
        loadLangPhrase("NoNeighbours", "You cannot claim a chunk next to someone else's chunk!");
        loadLangPhrase("CantAfford", "You cannot afford to claim that chunk!");
        loadLangPhrase("DoNotOwn", "You do not own this chunk!");
        loadLangPhrase("Line2Player", "Line 2 must contain a player name (or * for all)!");
        loadLangPhrase("AllowSelf", "You dont need to allow yourself!");
        loadLangPhrase("CannotDestroyClaim", "You cannot destroy another player's Claim sign!");
        loadLangPhrase("ClaimAreaWorldError", "[ClaimArea] signs must both be in the same world!");
        loadLangPhrase("AreaTooBig", "You cannot claim more than 64 chunks in one area!");
        loadLangPhrase("FoundClaimedInArea", "At least one chunk in the specified area is already claimed!");
        loadLangPhrase("FoundNeighboursInArea", "At least one chunk in the specified area has a neighbour!");
        loadLangPhrase("ClaimAreaTooLarge", "cannot claim that many chunks!");
        loadLangPhrase("ChunksInArea", "Chunks In Area");
        loadLangPhrase("CantAffordClaimArea", "You cannot afford to buy that many chunks!");
        loadLangPhrase("SpecifyNewMaxChunks", "You must specify a new maximum chunk limit!");
        loadLangPhrase("NewLimit", "new limit");
        loadLangPhrase("SpecifyNewChunkPrice", "You must specify a new chunk price!");
        loadLangPhrase("NewPrice", "new price");
        loadLangPhrase("SpecifyNewOverbuyPrice", "You must specify a new overbuy price!");
        loadLangPhrase("SpecifyToggle", "You must specify what to toggle!");
        loadLangPhrase("SpecifyPurgePlayer", "You must specify which player to purge!");
        loadLangPhrase("PlayerName", "Player Name");
        loadLangPhrase("SpecifyPurgeWorld", "You must specify which world to purge!");
        loadLangPhrase("WorldName", "World Name");
        loadLangPhrase("NoEcoPlugin", "There is no economy plugin running! Command aborted.");
        loadLangPhrase("NotFound", "not found");
        
        // Permissions
        loadLangPhrase("NoPermsCommand", "You do not have permission to use this command!");
        loadLangPhrase("NoPermsBuild", "You do not have permission to build here!");
        loadLangPhrase("NoPermsBreak", "You do not have permission to break blocks here!");
        loadLangPhrase("NoPermsFire", "FIRE! Oh phew... you're not allowed!");
        loadLangPhrase("NoPermsLava", "Are you crazy!? You can't drop lava there!");
        loadLangPhrase("NoPermsWater", "Are you crazy!? You can't drop water there!");
        loadLangPhrase("NoPermsDoor", ">KNOCK< >KNOCK< This door is locked!");
        loadLangPhrase("NoPermsDoorOwner", ">KNOCK< >KNOCK< Someone is visiting your chunk!");
        loadLangPhrase("NoPermsButton", ">BUZZZ< The button tripped a silent alarm!");
        loadLangPhrase("NoPermsButtonOwner", ">BUZZ< Someone pressed a button in your chunk!");
        loadLangPhrase("NoPermsLever", ">CLICK< The lever tripped a silent alarm!");
        loadLangPhrase("NoPermsLeverOwner", ">CLICK< Someone touched a lever in your chunk!");
        loadLangPhrase("NoPermsChest", ">CLUNK< That chest isn't yours!");
        loadLangPhrase("NoPermsChestOwner", ">CLUNK< Someone tryed to open a chest on your chunk!");
        loadLangPhrase("NoPermsSpecial", ">BUZZZ< Hands off! That's a special block!");
        loadLangPhrase("NoPermsSpecialOwner", ">BUZZZ< Someone touched a special block in your chunk!");
        loadLangPhrase("NoPermsPVP", "That player is protected by a magic shield!");
        loadLangPhrase("NoPermsClaim", "You do not have permission to claim chunks!");
        loadLangPhrase("NoPermsClaimArea", "You do not have permission to use [ClaimArea] signs!");
        loadLangPhrase("NoPermsBuyOwned", "You do not have permission to buy owned chunks!");
        loadLangPhrase("NoPermsClaimServer", "You do not have permission to claim chunks for the server!");
        loadLangPhrase("NoPermsClaimOther", "You do not have permission to claim chunks for other players!");
        loadLangPhrase("NoPermsUnclaimServer", "You do not have permission to unclaim chunks for the server!");
        loadLangPhrase("NoPermsUnclaimOther", "You do not have permission to unclaim chunks for other players!");
        loadLangPhrase("NoPermsNether", "You do not have permission to claim chunks in Nether worlds!");
        loadLangPhrase("NoPermsEnd", "You do not have permission to claim chunks in End worlds!");
        
        try {
            langStore.save(langFile);
        } catch (IOException ex) {
            Bukkit.getLogger().severe("[MyChunk] Could not save " + langFile);
        }
    }
    
    private static void loadLangPhrase(String key, String defaultString) {
        String value = langStore.getString(key, defaultString);
        langStore.set(key, value);
        lang.put(key, value);
    }

}
