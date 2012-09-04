package me.ellbristow.mychunk;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

public class MyChunk extends JavaPlugin {

    private static MyChunk plugin;
    private File chunkFile;
    protected FileConfiguration chunkStore;
    protected Integer claimedChunks;
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
    
    @Override
    public void onEnable() {
        plugin = this;
        chunkStore = getChunkStore();
        getServer().getPluginManager().registerEvents(new MyChunkListener(plugin), plugin);
        claimedChunks = chunkStore.getInt("TotalOwned", 0);
        chunkStore.set("TotalOwned", claimedChunks);
        saveChunkStore();
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
                sender.sendMessage(ChatColor.GOLD + "MyChunk v"  + ChatColor.WHITE + pdfFile.getVersion() + ChatColor.GOLD + " by " + ChatColor.WHITE + "ellbristow");
                sender.sendMessage("============================");
                if (sender instanceof Player) {
                    sender.sendMessage(ChatColor.GOLD + "Chunks You Own: " + ChatColor.WHITE + ownedChunkCount(sender.getName()));
                }
                sender.sendMessage(ChatColor.GOLD + "Total Claimed Chunks: " + ChatColor.WHITE + claimedChunks);
                int playerMax = getMaxChunks(sender);
                String yourMax;
                if (playerMax != 0) {
                    yourMax = ChatColor.GRAY + " (Yours: " + playerMax + ")";
                } else {
                    yourMax = ChatColor.GRAY + " (Yours: Unlimited)";
                }
                sender.sendMessage(ChatColor.GOLD + "Default Max Chunks Per Player: " + ChatColor.WHITE + maxChunks + yourMax);
                sender.sendMessage(ChatColor.GOLD + "Allow Neighbours: " + ChatColor.WHITE + allowNeighbours + ChatColor.GOLD + "Protect Unclaimed: " + ChatColor.WHITE + protectUnclaimed);
                if (foundEconomy) {
                    sender.sendMessage(ChatColor.GOLD + "Chunk Price: " + ChatColor.WHITE + vault.economy.format(chunkPrice));
                    sender.sendMessage(ChatColor.GOLD + "Allow Overbuy: " + ChatColor.WHITE + allowOverbuy);
                    if (allowOverbuy) {
                        String resales = "exc.";
                        if (overbuyP2P) {
                            resales = "inc.";
                        }
                        sender.sendMessage(ChatColor.GOLD + "Overbuy Fee: " + ChatColor.WHITE + vault.economy.format(overbuyPrice) + "(" + resales +" resales)");
                    }
                    String paid = "No";
                    if (unclaimRefund) {
                        paid = "Yes";
                    }
                    sender.sendMessage(ChatColor.GOLD + "Unclaim Refunds: " + ChatColor.WHITE + paid);
                }
                String claimExpiry;
                if (!useClaimExpiry) {
                    claimExpiry = "Disabled";
                } else {
                    claimExpiry = claimExpiryDays + " day(s) with no login";
                }
                sender.sendMessage(ChatColor.GOLD + "Claim Expiry: " + ChatColor.WHITE + claimExpiry);
                
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
            }
        } else if (args.length == 1) {
            if (args[0].equalsIgnoreCase("flags")) {
                if (sender.hasPermission("mychunk.commands.flags")) {
                    sender.sendMessage(ChatColor.GOLD + "MyChunk Permission Flags");
                    sender.sendMessage(ChatColor.GOLD + "========================");
                    sender.sendMessage(ChatColor.GREEN + "*" + ChatColor.GOLD + " = ALL | " + ChatColor.GREEN + "B" + ChatColor.GOLD + " = Build | " + ChatColor.GREEN + "C" + ChatColor.GOLD + " = Access Chests | " + ChatColor.GREEN + "D"  + ChatColor.GOLD + " = Destroy");
                    sender.sendMessage(ChatColor.GREEN + "I"  + ChatColor.GOLD + " = Ignite Blocks | " + ChatColor.GREEN + "L"  + ChatColor.GOLD + " = Drop Lava | " + ChatColor.GREEN + "O"  + ChatColor.GOLD + " = Open Wooden Doors");
                    sender.sendMessage(ChatColor.GREEN + "U"  + ChatColor.GOLD + " = Use Buttons/Levers etc | " + ChatColor.GREEN + "W"  + ChatColor.GOLD + " = Drop Water");
                    return true;
                } else {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
                    return false;
                }
            } else if (args[0].equalsIgnoreCase("max")) {
                sender.sendMessage(ChatColor.RED + "You must specify a new maximum chunk limit!");
                sender.sendMessage(ChatColor.RED + "/mychunk max {new limit}");
                return false;
            } else if (args[0].equalsIgnoreCase("price")) {
                sender.sendMessage(ChatColor.RED + "You must specify a new chunk price!");
                sender.sendMessage(ChatColor.RED + "/mychunk price {new price}");
                return false;
            } else if (args[0].equalsIgnoreCase("obprice")) {
                sender.sendMessage(ChatColor.RED + "You must specify a new overbuy price!");
                sender.sendMessage(ChatColor.RED + "/mychunk obprice {new price}");
                return false;
            } else if (args[0].equalsIgnoreCase("toggle")) {
                sender.sendMessage(ChatColor.RED + "You must specify what to toggle!");
                sender.sendMessage(ChatColor.RED + "/mychunk toggle {refund|overbuy|neighbours|resales|unclaimed|expiry}");
                return false;
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("price")) {
                if (sender.hasPermission("mychunk.commands.price")) {
                    if (!foundEconomy) {
                        sender.sendMessage(ChatColor.RED + "There is no economy plugin running! Command aborted.");
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
                    sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
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
                    sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
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
                        sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
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
                        sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
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
                        sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
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
                        sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
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
                        sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
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
                        sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
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
                    sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
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
                    sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
                    return false;
                }
            }
        }
        return false;
    }
    
    private void renewAllOwnerships() {
        Object[] allChunks = plugin.chunkStore.getKeys(true).toArray();
        for (int i = 1; i < allChunks.length; i++) {
            plugin.chunkStore.set(allChunks[i] + ".lastActive", new Date().getTime() / 1000);
        }
        plugin.saveChunkStore();
    }
    
    public int ownedChunkCount(String playerName) {
        int owned = 0;
        Object[] allChunks = plugin.chunkStore.getKeys(true).toArray();
        for (int i = 1; i < allChunks.length; i++) {
            String thisOwner = plugin.chunkStore.getString(allChunks[i] + ".owner");
            if (playerName.equals(thisOwner)) {
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
}
