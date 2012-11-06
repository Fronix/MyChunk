package me.ellbristow.mychunk;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

public class MyChunk extends JavaPlugin {

    private File chunkFile;
    protected FileConfiguration chunkStore;
    private File langFile;
    protected FileConfiguration langStore;
    protected FileConfiguration config;
    protected boolean foundVault = false;
    protected boolean foundEconomy = false;
    protected boolean unclaimRefund = false;
    protected boolean allowNeighbours = false;
    protected boolean allowOverbuy = false;
    protected boolean protectUnclaimed = false;
    protected boolean useClaimExpiry = false;
    protected int claimExpiryDays;
    protected boolean overbuyP2P = true;
    protected double chunkPrice = 0.00;
    protected double overbuyPrice = 0.00;
    protected int maxChunks = 8;
    protected MyChunkVaultLink vault;
    protected HashMap<String, MyChunkChunk> chunks = new HashMap<String, MyChunkChunk>();
    protected HashMap<String, String> lang = new HashMap<String, String>();
    protected HashMap<String, Block> pendingAreas = new HashMap<String, Block>();
    
    @Override
    public void onEnable() {
        chunkStore = getChunkStore();
        chunkStore.set("TotalOwned", null); // Remove Old "TotalOwned" record
        saveChunkStore();

        loadAllChunks();
        getServer().getPluginManager().registerEvents(new MyChunkListener(this), this);
        
        reloadLang();
        
        config = getConfig();
        maxChunks = config.getInt("max_chunks", 8);
        config.set("max_chunks", maxChunks);
        allowNeighbours = config.getBoolean("allow_neighbours", false);
        config.set("allow_neighbours", allowNeighbours);
        protectUnclaimed = config.getBoolean("protect_unclaimed", false);
        config.set("protect_unlcaimed", protectUnclaimed);
        useClaimExpiry = config.getBoolean("useClaimExpiry", false);
        config.set("useClaimExpiry", useClaimExpiry);
        claimExpiryDays = config.getInt("claimExpiresAfter", 7);
        config.set("claimExpiresAfter", claimExpiryDays);
        if (getServer().getPluginManager().isPluginEnabled("Vault")) {
            foundVault = true;
            vault = new MyChunkVaultLink(this);
            vault.initEconomy();
            getLogger().info("[Vault] found and hooked!");
            if (vault.foundEconomy) {
                foundEconomy = true;
                String message = "[" + vault.economyName + "] found and hooked!";
                getLogger().info(message);
                chunkPrice = config.getDouble("chunk_price", 0.00);
                config.set("chunk_price", chunkPrice);
                unclaimRefund = config.getBoolean("unclaim_refund", false);
                config.set("unclaim_refund", unclaimRefund);
                allowOverbuy = config.getBoolean("allow_overbuy", false);
                config.set("allow_overbuy", allowOverbuy);
                overbuyPrice = config.getDouble("overbuy_price", 0.00);
                config.set("overbuyPrice", overbuyPrice);
                overbuyP2P = config.getBoolean("charge_overbuy_on_resales", true);
                config.set("charge_overbuy_on_resales", overbuyP2P);
            } else {
                getLogger().info("No economy plugin found! Chunks will be free");
            }
        } else {
            getLogger().info("Vault not found! Chunks will be free");
        }
        saveConfig();
    }
    
    @Override
    public void onDisable() {
        
    }
    
    @Override
    public boolean onCommand (CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (args.length == 0) {
            if (sender.hasPermission("mychunk.commands.stats")) {
                PluginDescriptionFile pdfFile = getDescription();
                sender.sendMessage(ChatColor.GOLD + "MyChunk v"  + ChatColor.WHITE + pdfFile.getVersion() + ChatColor.GOLD + " "+lang.get("By")+" " + ChatColor.WHITE + "ellbristow");
                if (sender instanceof Player) {
                    sender.sendMessage(ChatColor.GOLD + lang.get("ChunksOwned")+": " + ChatColor.WHITE + ownedChunkCount(sender.getName()));
                }
                sender.sendMessage(ChatColor.GOLD + lang.get("TotalClaimedChunks")+": " + ChatColor.WHITE + chunks.size());
                int playerMax = getMaxChunks(sender);
                String yourMax;
                if (playerMax != 0) {
                    yourMax = ChatColor.GRAY + " ("+lang.get("Yours")+": " + playerMax + ")";
                } else {
                    yourMax = ChatColor.GRAY + " ("+lang.get("Yours")+": "+lang.get("Unlimited")+")";
                }
                sender.sendMessage(ChatColor.GOLD + lang.get("DefaultMax")+": " + ChatColor.WHITE + maxChunks + yourMax);
                sender.sendMessage(ChatColor.GOLD + lang.get("AllowNeighbours")+": " + ChatColor.WHITE + allowNeighbours + ChatColor.GOLD + " "+lang.get("ProtectUnclaimed")+": " + ChatColor.WHITE + protectUnclaimed);
                if (foundEconomy) {
                    sender.sendMessage(ChatColor.GOLD + lang.get("ChunkPrice")+": " + ChatColor.WHITE + vault.economy.format(chunkPrice));
                    sender.sendMessage(ChatColor.GOLD + lang.get("AllowOverbuy")+": " + ChatColor.WHITE + allowOverbuy);
                    if (allowOverbuy) {
                        String resales = "exc.";
                        if (overbuyP2P) {
                            resales = "inc.";
                        }
                        sender.sendMessage(ChatColor.GOLD + lang.get("OverbuyFee")+": " + ChatColor.WHITE + vault.economy.format(overbuyPrice) + "(" + resales +" "+lang.get("Resales")+")");
                    }
                    String paid = lang.get("No");
                    if (unclaimRefund) {
                        paid = lang.get("Yes");
                    }
                    sender.sendMessage(ChatColor.GOLD + lang.get("UnclaimRefunds")+": " + ChatColor.WHITE + paid);
                }
                String claimExpiry;
                if (!useClaimExpiry) {
                    claimExpiry = lang.get("Disabled");
                } else {
                    claimExpiry = claimExpiryDays + " "+lang.get("DaysWithoutLogin");
                }
                sender.sendMessage(ChatColor.GOLD + lang.get("ClaimExpiry")+": " + ChatColor.WHITE + claimExpiry);
                
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + lang.get("NoPermsCommand"));
            }
        } else if (args.length == 1) {
            if (args[0].equalsIgnoreCase("flags")) {
                if (sender.hasPermission("mychunk.commands.flags")) {
                    sender.sendMessage(ChatColor.GOLD + "MyChunk "+lang.get("PermissionFlags"));
                    sender.sendMessage(ChatColor.GREEN + "*" + ChatColor.GOLD + " = "+lang.get("All")+" | " + ChatColor.GREEN + "B" + ChatColor.GOLD + " = "+lang.get("Build")+" | " + ChatColor.GREEN + "C" + ChatColor.GOLD + " = "+lang.get("AccessChests")+" | " + ChatColor.GREEN + "D"  + ChatColor.GOLD + " = "+lang.get("Destroy"));
                    sender.sendMessage(ChatColor.GREEN + "I"  + ChatColor.GOLD + " = "+lang.get("IgniteBlocks")+" | " + ChatColor.GREEN + "L"  + ChatColor.GOLD + " = "+lang.get("DropLava")+" | " + ChatColor.GREEN + "O"  + ChatColor.GOLD + " = "+lang.get("OpenWoodenDoors"));
                    sender.sendMessage(ChatColor.GREEN + "U"  + ChatColor.GOLD + " = "+lang.get("UseButtonsLevers")+" | " + ChatColor.GREEN + "W"  + ChatColor.GOLD + " = "+lang.get("DropWater"));
                    return true;
                } else {
                    sender.sendMessage(ChatColor.RED + lang.get("NoPermsCommand"));
                    return false;
                }
            } else if (args[0].equalsIgnoreCase("max")) {
                sender.sendMessage(ChatColor.RED + lang.get("SpecifyNewMaxChunks"));
                sender.sendMessage(ChatColor.RED + "/mychunk max {"+lang.get("NewLimit")+"}");
                return false;
            } else if (args[0].equalsIgnoreCase("price")) {
                sender.sendMessage(ChatColor.RED + lang.get("SpecifyNewChunkPrice"));
                sender.sendMessage(ChatColor.RED + "/mychunk price {"+lang.get("NewPrice")+"}");
                return false;
            } else if (args[0].equalsIgnoreCase("obprice")) {
                sender.sendMessage(ChatColor.RED + lang.get("SpecifyNewOverbuyPrice"));
                sender.sendMessage(ChatColor.RED + "/mychunk obprice {"+lang.get("NewPrice")+"}");
                return false;
            } else if (args[0].equalsIgnoreCase("toggle")) {
                sender.sendMessage(ChatColor.RED + lang.get("SpecifyToggle"));
                sender.sendMessage(ChatColor.RED + "/mychunk toggle {refund|overbuy|neighbours|resales|unclaimed|expiry}");
                return false;
            } else if (args[0].equalsIgnoreCase("purgep")) {
                sender.sendMessage(ChatColor.RED + lang.get("SpecifyPurgePlayer"));
                sender.sendMessage(ChatColor.RED + "/mychunk purgep ["+lang.get("PlayerName")+"]");
                return false;
            } else if (args[0].equalsIgnoreCase("purgew")) {
                sender.sendMessage(ChatColor.RED + lang.get("SpecifyPurgeWorld"));
                sender.sendMessage(ChatColor.RED + "/mychunk purgep ["+lang.get("WorldName")+"]");
                return false;
            } else if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("mychunk.commands.reload")) {
                    sender.sendMessage(ChatColor.RED + lang.get("NoPermsCommand"));
                    return true;
                }
                chunkStore = getChunkStore();
                loadAllChunks();
                reloadLang();
                sender.sendMessage(ChatColor.GOLD + lang.get("Reloaded"));
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("price")) {
                if (sender.hasPermission("mychunk.commands.price")) {
                    if (!foundEconomy) {
                        sender.sendMessage(ChatColor.RED + lang.get("NoEcoPlugin"));
                        return false;
                    } else {
                        double newPrice;
                        try {
                            newPrice = Double.parseDouble(args[1]);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ChatColor.RED + "Amount must be a number! (e.g. 5.00)");
                            sender.sendMessage(ChatColor.RED + "/mychunk price {new_price}");
                            return false;
                        }
                        config.set("chunk_price", newPrice);
                        chunkPrice = newPrice;
                        saveConfig();
                        sender.sendMessage(ChatColor.GOLD + "Chunk price set to " + vault.economy.format(newPrice));
                        return true;
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + lang.get("NoPermsCommand"));
                    return false;
                }
            } else if (args[0].equalsIgnoreCase("obprice")) {
                if (sender.hasPermission("mychunk.commands.obprice")) {
                    if (!foundEconomy) {
                        sender.sendMessage(ChatColor.RED + "There is no economy plugin running! Command aborted.");
                        return false;
                    } else {
                        double newPrice;
                        try {
                            newPrice = Double.parseDouble(args[1]);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ChatColor.RED + "Amount must be a number! (e.g. 5.00)");
                            sender.sendMessage(ChatColor.RED + "/mychunk obprice {new_price}");
                            return false;
                        }
                        config.set("overbuy_price", newPrice);
                        overbuyPrice = newPrice;
                        saveConfig();
                        sender.sendMessage(ChatColor.GOLD + "Overbuy price set to " + vault.economy.format(newPrice));
                        return true;
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + lang.get("NoPermsCommand"));
                    return false;
                }
            } else if (args[0].equalsIgnoreCase("toggle")) {
                if (args[1].equalsIgnoreCase("refund")) {
                    if (sender.hasPermission("mychunk.commands.toggle.refund")) {
                        if (!foundEconomy) {
                            sender.sendMessage(ChatColor.RED + "There is no economy plugin running! Command aborted.");
                            return false;
                        } else {
                            if (unclaimRefund) {
                                unclaimRefund = false;
                                sender.sendMessage(ChatColor.GOLD + "Unclaiming chunks now " + ChatColor.RED + "DOES NOT" + ChatColor.GOLD + " provide a refund.");
                            } else {
                                unclaimRefund = true;
                                sender.sendMessage(ChatColor.GOLD + "Unclaiming chunks now " + ChatColor.GREEN + "DOES" + ChatColor.GOLD + " provides a refund.");
                            }
                            config.set("unclaim_refund", unclaimRefund);
                            saveConfig();
                            return true;
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + lang.get("NoPermsCommand"));
                        return false;
                    }
                } else if (args[1].equalsIgnoreCase("overbuy")) {
                    if (sender.hasPermission("mychunk.commands.toggle.overbuy")) {
                        if (!foundEconomy) {
                            sender.sendMessage(ChatColor.RED + "There is no economy plugin running! Command aborted.");
                            return false;
                        } else {
                            if (allowOverbuy) {
                                allowOverbuy = false;
                                sender.sendMessage(ChatColor.GOLD + "Buying over the chunk limit is now "+ ChatColor.RED + "disabled");
                            } else {
                                allowOverbuy = true;
                                sender.sendMessage(ChatColor.GOLD + "Buying over the chunk limit is now "+ ChatColor.GREEN + "enabled");
                            }
                            config.set("allow_overbuy", allowOverbuy);
                            saveConfig();
                            return true;
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + lang.get("NoPermsCommand"));
                        return false;
                    }
                } else if (args[1].equalsIgnoreCase("resales")) {
                    if (sender.hasPermission("mychunk.commands.toggle.resales")) {
                        if (!foundEconomy) {
                            sender.sendMessage(ChatColor.RED + "There is no economy plugin running! Command aborted.");
                            return false;
                        }
                        if (!allowOverbuy) {
                            sender.sendMessage(ChatColor.RED + "Overbuying is disabled! Command aborted.");
                            return false;
                        }
                        if (overbuyP2P) {
                            overbuyP2P = false;
                            sender.sendMessage(ChatColor.GOLD + "Overbuy fee when buying from other players is now "+ ChatColor.RED + "disabled");
                        } else {
                            overbuyP2P = true;
                            sender.sendMessage(ChatColor.GOLD + "Overbuy fee when buying from other players is now "+ ChatColor.GREEN + "enabled");
                        }
                        config.set("charge_overbuy_on_resales", overbuyP2P);
                        saveConfig();
                        return true;
                    } else {
                        sender.sendMessage(ChatColor.RED + lang.get("NoPermsCommand"));
                        return false;
                    }
                } else if (args[1].equalsIgnoreCase("neighbours")) {
                    if (sender.hasPermission("mychunk.commands.toggle.neighbours")) {
                        if (allowNeighbours) {
                            allowNeighbours = false;
                            sender.sendMessage(ChatColor.GOLD + "Claiming chunks next to other players "+ ChatColor.RED + "disabled");
                        } else {
                            allowNeighbours = true;
                            sender.sendMessage(ChatColor.GOLD + "Claiming chunks next to other players "+ ChatColor.GREEN + "enabled");
                        }
                        config.set("allow_neighbours", allowNeighbours);
                        saveConfig();
                        return true;
                    } else {
                        sender.sendMessage(ChatColor.RED + lang.get("NoPermsCommand"));
                        return false;
                    }
                } else if (args[1].equalsIgnoreCase("unclaimed")) {
                    if (sender.hasPermission("mychunk.commands.toggle.unclaimed")) {
                        if (protectUnclaimed) {
                            protectUnclaimed = false;
                            sender.sendMessage(ChatColor.GOLD + "Unclaimed chunks are now "+ChatColor.RED+"NOT"+ChatColor.GOLD+" protected");
                        } else {
                            protectUnclaimed = true;
                            sender.sendMessage(ChatColor.GOLD + "Unclaimed chunks are now "+ChatColor.GREEN+"protected");
                        }
                        config.set("protect_unclaimed", protectUnclaimed);
                        saveConfig();
                        return true;
                    } else {
                        sender.sendMessage(ChatColor.RED + lang.get("NoPermsCommand"));
                        return false;
                    }
                } else if (args[1].equalsIgnoreCase("expiry")) {
                    if (sender.hasPermission("mychunk.commands.toggle.expiry")) {
                        if (useClaimExpiry) {
                            useClaimExpiry = false;
                            sender.sendMessage(ChatColor.GOLD + "Claimed chunks will now "+ChatColor.RED+"NOT"+ChatColor.GOLD+" expire after inactivity");
                        } else {
                            useClaimExpiry = true;
                            sender.sendMessage(ChatColor.GOLD + "Claimed chunks "+ChatColor.GREEN+"WILL"+ChatColor.GOLD+" now expire after " + claimExpiryDays + " days of inactivity");
                            renewAllOwnerships();
                            sender.sendMessage(ChatColor.GOLD + "All activity records have been reset to this time");
                        }
                        config.set("useClaimExpiry", useClaimExpiry);
                        saveConfig();
                        return true;
                    } else {
                        sender.sendMessage(ChatColor.RED + lang.get("NoPermsCommand"));
                        return false;
                    }
                }
            } else if (args[0].equalsIgnoreCase("max")) {
                if (sender.hasPermission("mychunk.commands.max")) {
                    int newMax;
                    try {
                        newMax = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Amount must be an integer! (0 = unlimited)");
                    sender.sendMessage(ChatColor.RED + "/mychunk max {new_max}");
                    return false;
                    }
                    
                    config.set("max_chunks", newMax);
                    maxChunks = newMax;
                    sender.sendMessage(ChatColor.GOLD + "Max Chunks is now set at " + ChatColor.WHITE + newMax + ChatColor.GOLD + "!");
                    saveConfig();
                    return true;
                } else {
                    sender.sendMessage(ChatColor.RED + lang.get("NoPermsCommand"));
                    return false;
                }
            } else if (args[0].equalsIgnoreCase("expiryDays")) {
                if (sender.hasPermission("mychunk.commands.expirydays")) {
                    if (!useClaimExpiry) {
                        sender.sendMessage(ChatColor.RED + "Claim expiry is disabled!");
                        return true;
                    }
                    int newDays;
                    try {
                        newDays = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "Amount must be an integer!");
                        sender.sendMessage(ChatColor.RED + "/mychunk expirydays {new_days}");
                        return false;
                    }
                    if (newDays <= 0) {
                        sender.sendMessage(ChatColor.RED + "Amount must be greater than 0!");
                        sender.sendMessage(ChatColor.RED + "/mychunk expirydays {new_days}");
                        return false;
                    }
                    config.set("claimExpiresAfter", newDays);
                    claimExpiryDays = newDays;
                    sender.sendMessage(ChatColor.GOLD + "Claimed chunks will now expire after " + ChatColor.GREEN + claimExpiryDays + ChatColor.GOLD + " days of inactivity");
                    saveConfig();
                    return true;
                } else {
                    sender.sendMessage(ChatColor.RED + lang.get("NoPermsCommand"));
                    return false;
                }
            } else if (args[0].equalsIgnoreCase("purgep")) {
                if (!sender.hasPermission("mychunk.commands.purgep")) {
                    sender.sendMessage(ChatColor.RED + lang.get("NoPermsCommand"));
                    return false;
                }
                OfflinePlayer player = getServer().getOfflinePlayer(args[1]);
                if (!player.hasPlayedBefore()) {
                    sender.sendMessage(ChatColor.RED + "Player "+ChatColor.WHITE+args[1]+ChatColor.RED+" not found!");
                    return false;
                }
                Object[] allChunks = chunkStore.getKeys(true).toArray();
                for (int i = 1; i < allChunks.length; i++) {
                    String thisOwner = chunkStore.getString(allChunks[i] + ".owner");
                    if (args[1].equalsIgnoreCase(thisOwner)) {
                        chunkStore.set(String.valueOf(allChunks[i]), null);
                    }
                }
                saveChunkStore();
                sender.sendMessage(ChatColor.GOLD + "All chunks for " + ChatColor.WHITE + player.getName() + ChatColor.GOLD + " are now Unowned!");
            }  else if (args[0].equalsIgnoreCase("purgew")) {
                if (!sender.hasPermission("mychunk.commands.purgew")) {
                    sender.sendMessage(ChatColor.RED + lang.get("NoPermsCommand"));
                    return false;
                }
                World world = getServer().getWorld(args[1]);
                if (world == null) {
                    sender.sendMessage(ChatColor.RED + "World "+ChatColor.WHITE+args[1]+ChatColor.RED+" not found!");
                    return false;
                }
                String worldName = world.getName();
                Object[] allChunks = chunkStore.getKeys(true).toArray();
                for (int i = 1; i < allChunks.length; i++) {
                    String[] chunkSplit = String.valueOf(allChunks[i]).split("_");
                    if (worldName.equalsIgnoreCase(chunkSplit[0])) {
                        chunkStore.set(String.valueOf(allChunks[i]), null);
                    }
                }
                saveChunkStore();
                sender.sendMessage(ChatColor.GOLD + "All chunks in " + ChatColor.WHITE + worldName + ChatColor.GOLD + " are now Unowned!");
            }
        }
        return false;
    }
    
    private void renewAllOwnerships() {
        Object[] allChunks = chunkStore.getKeys(true).toArray();
        for (int i = 1; i < allChunks.length; i++) {
            chunkStore.set(allChunks[i] + ".lastActive", new Date().getTime() / 1000);
        }
        saveChunkStore();
        loadAllChunks();
    }
    
    public int ownedChunkCount(String playerName) {
        int owned = 0;
        for (MyChunkChunk chunk : chunks.values()) {
            if (chunk.getOwner().equalsIgnoreCase(playerName)) {
                owned++;
            }
        }
        return owned;
    }
    
    public int getMaxChunks(CommandSender player) {
        int max = maxChunks;
        if (player instanceof Player) {
            if (player.hasPermission("mychunk.claim.max.0") || player.hasPermission("mychunk.claim.unlimited")) {
                max = 0;
            } else {
                for (int i = 1; i <= 256; i++) {
                    if (player.hasPermission("mychunk.claim.max." + i)) {
                        max = i;
                    }
                }
            }
        } else {
            max = 0;
        }
        return max;
    }
    
    protected void loadAllChunks() {
        chunks.clear();
        Object[] chunkSource = chunkStore.getKeys(false).toArray();
        for (Object chunk : chunkSource) {
            String[] elements = ((String)chunk).split("_");
            int x = Integer.parseInt(elements[1]);
            int y = Integer.parseInt(elements[2]);
            chunks.put(chunk.toString(), new MyChunkChunk(elements[0], x, y, this));
        }
    }
    
    protected void loadChunkStore() {
        if (chunkFile == null) {
            chunkFile = new File(getDataFolder(),"chunks.yml");
        }
        chunkStore = YamlConfiguration.loadConfiguration(chunkFile);
    }
    
    protected FileConfiguration getChunkStore() {
        if (chunkStore == null) {
            loadChunkStore();
        }
        return chunkStore;
    }
	
    protected void saveChunkStore() {
        if (chunkStore == null || chunkFile == null) {
            return;
        }
        try {
            chunkStore.save(chunkFile);
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Could not save " + chunkFile, ex );
        }
    }
    
    private void reloadLang() {
        lang.clear();
        langStore = getLang();
        
        // General
        loadLangPhrase("Yes", "Yes");
        loadLangPhrase("No", "No");
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
        loadLangPhrase("PermissionFlags", "Permission Flags");
        loadLangPhrase("Reloaded", "Mychunk files have been reloaded!");
        
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
        
    }
    
    private void loadLangPhrase(String key, String defaultString) {
        String value = langStore.getString(key, defaultString);
        langStore.set(key, value);
        lang.put(key, value);
    }
    
    protected void loadLang() {
        if (langFile == null) {
            langFile = new File(getDataFolder(),"lang.yml");
        }
        langStore = YamlConfiguration.loadConfiguration(langFile);
    }
    
    protected FileConfiguration getLang() {
        if (langStore == null) {
            loadLang();
        }
        return langStore;
    }
	
    protected void saveLang() {
        if (langStore == null || langFile == null) {
            return;
        }
        try {
            langStore.save(langFile);
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Could not save " + langFile, ex );
        }
    }
}
