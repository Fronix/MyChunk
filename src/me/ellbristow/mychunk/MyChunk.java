package me.ellbristow.mychunk;

import java.io.File;
import java.util.*;
import me.ellbristow.mychunk.SQLite.SQLiteBridge;
import me.ellbristow.mychunk.lang.Lang;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
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

    protected FileConfiguration config;
    protected boolean foundVault = false;
    protected boolean foundEconomy = false;
    protected boolean unclaimRefund = false;
    protected boolean allowNeighbours = false;
    protected boolean allowOverbuy = false;
    protected boolean protectUnclaimed = false;
    protected boolean useClaimExpiry = false;
    protected boolean allowNether = true;
    protected boolean allowEnd = true;
    protected int claimExpiryDays;
    protected boolean overbuyP2P = true;
    protected double chunkPrice = 0.00;
    protected double overbuyPrice = 0.00;
    protected int maxChunks = 8;
    protected MyChunkVaultLink vault;
    protected HashMap<String, Block> pendingAreas = new HashMap<String, Block>();
    
    // LOOK! SQLite stuff!
    protected SQLiteBridge chunkDb;
    private String[] tableColumns = {"world","x","z","owner","allowed","salePrice","allowMobs","lastActive", "PRIMARY KEY"};
    private String[] tableDims = {"TEXT NOT NULL", "INTEGER NOT NULL", "INTEGER NOT NULL", "TEXT NOT NULL", "TEXT NOT NULL", "INTEGER NOT NULL", "INTEGER(1) NOT NULL", "LONG NOT NULL", "(world, x, z)"};
    
    @Override
    public void onEnable() {
        
        // init SQLite
        initSQLite();
        
        // init Config
        loadConfig(false);
        
        // Register Events
        getServer().getPluginManager().registerEvents(new MyChunkListener(this), this);
        
    }
    
    @Override
    public void onDisable() {
        chunkDb.close();
    }
    
    @Override
    public boolean onCommand (CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (args.length == 0) {
            if (sender.hasPermission("mychunk.commands.stats")) {
                PluginDescriptionFile pdfFile = getDescription();
                sender.sendMessage(ChatColor.GOLD + "MyChunk v"  + ChatColor.WHITE + pdfFile.getVersion() + ChatColor.GOLD + " "+Lang.get("By")+" " + ChatColor.WHITE + "ellbristow");
                HashMap<Integer, HashMap<String, Object>> result = chunkDb.select("COUNT(*) AS counter", "MyChunks", "", "", "");
                if (sender instanceof Player) {
                    sender.sendMessage(ChatColor.GOLD + Lang.get("ChunksOwned")+": " + ChatColor.WHITE + ownedChunkCount(sender.getName()) +"  "+ ChatColor.GOLD + Lang.get("TotalClaimedChunks")+": " + ChatColor.WHITE + result.get(0).get("counter"));
                } else {
                    sender.sendMessage(ChatColor.GOLD + Lang.get("TotalClaimedChunks")+": " + ChatColor.WHITE + result.get(0).get("counter"));
                }
                String yourMax;
                int playerMax = getMaxChunks(sender);
                if (playerMax != 0) {
                    yourMax = ChatColor.GRAY + " ("+Lang.get("Yours")+": " + playerMax + ")";
                } else {
                    yourMax = ChatColor.GRAY + " ("+Lang.get("Yours")+": "+Lang.get("Unlimited")+")";
                }
                sender.sendMessage(ChatColor.GOLD + Lang.get("DefaultMax")+": " + ChatColor.WHITE + maxChunks + yourMax);
                if (foundEconomy){
                    sender.sendMessage(ChatColor.GOLD + Lang.get("ChunkPrice")+": " + ChatColor.WHITE + vault.economy.format(chunkPrice));
                    String paid = Lang.get("No");
                    if (unclaimRefund) {
                        paid = Lang.get("Yes");
                    }
                    sender.sendMessage(ChatColor.GOLD + Lang.get("UnclaimRefunds")+": " + ChatColor.WHITE + paid);
                }
                sender.sendMessage("");
                sender.sendMessage(ChatColor.GOLD + Lang.get("AllowNeighbours")+": " + ChatColor.WHITE + Lang.get(""+allowNeighbours) + ChatColor.GOLD + "  "+Lang.get("ProtectUnclaimed")+": " + ChatColor.WHITE + Lang.get(""+protectUnclaimed));
                if (foundEconomy) {
                    sender.sendMessage(ChatColor.GOLD + Lang.get("AllowOverbuy")+": " + ChatColor.WHITE + Lang.get(""+allowOverbuy));
                    if (allowOverbuy) {
                        String resales = "exc.";
                        if (overbuyP2P) {
                            resales = "inc.";
                        }
                        sender.sendMessage(ChatColor.GOLD + Lang.get("OverbuyFee")+": " + ChatColor.WHITE + vault.economy.format(overbuyPrice) + "(" + resales +" "+Lang.get("Resales")+")");
                    }
                }
                String claimExpiry;
                if (!useClaimExpiry) {
                    claimExpiry = Lang.get("Disabled");
                } else {
                    claimExpiry = claimExpiryDays + " "+Lang.get("DaysWithoutLogin");
                }
                sender.sendMessage(ChatColor.GOLD + Lang.get("ClaimExpiry")+": " + ChatColor.WHITE + claimExpiry);
                sender.sendMessage(ChatColor.GOLD + Lang.get("AllowNether")+": " + ChatColor.WHITE + Lang.get(""+allowNether) + "  " + ChatColor.GOLD + Lang.get("AllowEnd")+": " + ChatColor.WHITE + Lang.get(""+allowEnd));
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + Lang.get("NoPermsCommand"));
            }
        } else if (args.length == 1) {
            if (args[0].equalsIgnoreCase("flags")) {
                if (sender.hasPermission("mychunk.commands.flags")) {
                    sender.sendMessage(ChatColor.GOLD + "MyChunk "+Lang.get("PermissionFlags"));
                    sender.sendMessage(ChatColor.GREEN + "*" + ChatColor.GOLD + " = "+Lang.get("All")+" | " + ChatColor.GREEN + "B" + ChatColor.GOLD + " = "+Lang.get("Build")+" | " + ChatColor.GREEN + "C" + ChatColor.GOLD + " = "+Lang.get("AccessChests")+" | " + ChatColor.GREEN + "D"  + ChatColor.GOLD + " = "+Lang.get("Destroy"));
                    sender.sendMessage(ChatColor.GREEN + "I"  + ChatColor.GOLD + " = "+Lang.get("IgniteBlocks")+" | " + ChatColor.GREEN + "L"  + ChatColor.GOLD + " = "+Lang.get("DropLava")+" | " + ChatColor.GREEN + "O"  + ChatColor.GOLD + " = "+Lang.get("OpenWoodenDoors"));
                    sender.sendMessage(ChatColor.GREEN + "U"  + ChatColor.GOLD + " = "+Lang.get("UseButtonsLevers")+" | " + ChatColor.GREEN + "W"  + ChatColor.GOLD + " = "+Lang.get("DropWater"));
                    return true;
                } else {
                    sender.sendMessage(ChatColor.RED + Lang.get("NoPermsCommand"));
                    return false;
                }
            } else if (args[0].equalsIgnoreCase("max")) {
                sender.sendMessage(ChatColor.RED + Lang.get("SpecifyNewMaxChunks"));
                sender.sendMessage(ChatColor.RED + "/mychunk max {"+Lang.get("NewLimit")+"}");
                return false;
            } else if (args[0].equalsIgnoreCase("price")) {
                sender.sendMessage(ChatColor.RED + Lang.get("SpecifyNewChunkPrice"));
                sender.sendMessage(ChatColor.RED + "/mychunk price {"+Lang.get("NewPrice")+"}");
                return false;
            } else if (args[0].equalsIgnoreCase("obprice")) {
                sender.sendMessage(ChatColor.RED + Lang.get("SpecifyNewOverbuyPrice"));
                sender.sendMessage(ChatColor.RED + "/mychunk obprice {"+Lang.get("NewPrice")+"}");
                return false;
            } else if (args[0].equalsIgnoreCase("toggle")) {
                sender.sendMessage(ChatColor.RED + Lang.get("SpecifyToggle"));
                sender.sendMessage(ChatColor.RED + "/mychunk toggle {refund | overbuy | neighbours | resales | unclaimed | expiry | allownether | allowend}");
                return false;
            } else if (args[0].equalsIgnoreCase("purgep")) {
                sender.sendMessage(ChatColor.RED + Lang.get("SpecifyPurgePlayer"));
                sender.sendMessage(ChatColor.RED + "/mychunk purgep ["+Lang.get("PlayerName")+"]");
                return false;
            } else if (args[0].equalsIgnoreCase("purgew")) {
                sender.sendMessage(ChatColor.RED + Lang.get("SpecifyPurgeWorld"));
                sender.sendMessage(ChatColor.RED + "/mychunk purgew ["+Lang.get("WorldName")+"]");
                return false;
            } else if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("mychunk.commands.reload")) {
                    sender.sendMessage(ChatColor.RED + Lang.get("NoPermsCommand"));
                    return true;
                }
                reloadConfig();
                loadConfig(true);
                Lang.reload();
                sender.sendMessage(ChatColor.GOLD + Lang.get("Reloaded"));
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("price")) {
                if (sender.hasPermission("mychunk.commands.price")) {
                    if (!foundEconomy) {
                        sender.sendMessage(ChatColor.RED + Lang.get("NoEcoPlugin"));
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
                    sender.sendMessage(ChatColor.RED + Lang.get("NoPermsCommand"));
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
                    sender.sendMessage(ChatColor.RED + Lang.get("NoPermsCommand"));
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
                        sender.sendMessage(ChatColor.RED + Lang.get("NoPermsCommand"));
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
                        sender.sendMessage(ChatColor.RED + Lang.get("NoPermsCommand"));
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
                        sender.sendMessage(ChatColor.RED + Lang.get("NoPermsCommand"));
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
                        sender.sendMessage(ChatColor.RED + Lang.get("NoPermsCommand"));
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
                        sender.sendMessage(ChatColor.RED + Lang.get("NoPermsCommand"));
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
                        sender.sendMessage(ChatColor.RED + Lang.get("NoPermsCommand"));
                        return false;
                    }
                } else if (args[1].equalsIgnoreCase("allownether")) {
                    if (!sender.hasPermission("mychunk.commands.toggle.allownether")) {
                        sender.sendMessage(ChatColor.RED + Lang.get("NoPermsCommand"));
                        return false;
                    }
                    if (allowNether) {
                        allowNether = false;
                        sender.sendMessage(ChatColor.GOLD + Lang.get("ToggleNetherCannot"));
                    } else {
                        allowNether = true;
                        sender.sendMessage(ChatColor.GOLD + Lang.get("ToggleNetherCan"));
                    }
                    config.set("allowNether", allowNether);
                    saveConfig();
                    return true;
                } else if (args[1].equalsIgnoreCase("allowend")) {
                    if (!sender.hasPermission("mychunk.commands.toggle.allowend")) {
                        sender.sendMessage(ChatColor.RED + Lang.get("NoPermsCommand"));
                        return false;
                    }
                    if (allowNether) {
                        allowNether = false;
                        sender.sendMessage(ChatColor.GOLD + Lang.get("ToggleEndCannot"));
                    } else {
                        allowNether = true;
                        sender.sendMessage(ChatColor.GOLD + Lang.get("ToggleEndCan"));
                    }
                    config.set("allowNether", allowNether);
                    saveConfig();
                    return true;
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
                    sender.sendMessage(ChatColor.RED + Lang.get("NoPermsCommand"));
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
                    sender.sendMessage(ChatColor.RED + Lang.get("NoPermsCommand"));
                    return false;
                }
            } else if (args[0].equalsIgnoreCase("purgep")) {
                if (!sender.hasPermission("mychunk.commands.purgep")) {
                    sender.sendMessage(ChatColor.RED + Lang.get("NoPermsCommand"));
                    return false;
                }
                OfflinePlayer player = getServer().getOfflinePlayer(args[1]);
                if (!player.hasPlayedBefore()) {
                    sender.sendMessage(ChatColor.RED + "Player "+ChatColor.WHITE+args[1]+ChatColor.RED+" not found!");
                    return false;
                }
                HashMap<Integer, HashMap<String, Object>> results = chunkDb.select("world, x, z","MyChunks","owner = '"+player.getName()+"'","","");
                List<Chunk> chunks = new ArrayList<Chunk>();
                for (int i = 0; i < results.size(); i++) {
                    HashMap<String, Object> result = results.get(i);
                    String world = (String)result.get("world");
                    int x = Integer.parseInt(result.get("x")+"");
                    int z = Integer.parseInt(result.get("z")+"");
                    chunks.add(getServer().getWorld(world).getChunkAt(x, z));
                }
                for (Chunk thisChunk: chunks) {
                    MyChunkChunk chunk = new MyChunkChunk(thisChunk.getBlock(0, 0, 0),this);
                    chunk.unclaim();
                }
                sender.sendMessage(ChatColor.GOLD + "All chunks for " + ChatColor.WHITE + player.getName() + ChatColor.GOLD + " are now Unowned!");
            }  else if (args[0].equalsIgnoreCase("purgew")) {
                if (!sender.hasPermission("mychunk.commands.purgew")) {
                    sender.sendMessage(ChatColor.RED + Lang.get("NoPermsCommand"));
                    return false;
                }
                World world = getServer().getWorld(args[1]);
                if (world == null) {
                    sender.sendMessage(ChatColor.RED + "World "+ChatColor.WHITE+args[1]+ChatColor.RED+" not found!");
                    return false;
                }
                String worldName = world.getName();
                chunkDb.query("DELETE FROM MyChunks WHERE world = '"+worldName+"'");
                sender.sendMessage(ChatColor.GOLD + "All chunks in " + ChatColor.WHITE + worldName + ChatColor.GOLD + " are now Unowned!");
            }
        }
        return false;
    }
    
    private void renewAllOwnerships() {
        chunkDb.query("UPDATE MyChunks SET lastActive = " + (new Date().getTime() / 1000));
    }
    
    public int ownedChunkCount(String playerName) {
        HashMap<Integer, HashMap<String, Object>> results = chunkDb.select("COUNT(*) as counter", "MyChunks", "owner = '"+playerName+"'", "","");
        return Integer.parseInt(results.get(0).get("counter")+"");
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
    
    private void initSQLite() {
        chunkDb = new SQLiteBridge(this);
        if (!chunkDb.checkTable("MyChunks")) {
            // Create empty table
            chunkDb.createTable("MyChunks", tableColumns, tableDims);
        }
        File chunkFile = new File(getDataFolder(),"chunks.yml");
        if (chunkFile.exists()) {
            // Transfer old data
            getLogger().info("Converting old YML data to SQLite");
            FileConfiguration chunkStore = YamlConfiguration.loadConfiguration(chunkFile);
            Set<String> keys = chunkStore.getKeys(false);
            String values = "";
            for (String key : keys) {
                if (!key.equalsIgnoreCase("TotalOwned")) {
                    int split = key.lastIndexOf("_", key.lastIndexOf("_") - 1);
                    String world = key.substring(0, split);
                    String[] elements = key.substring(split + 1).split("_");
                    int x = Integer.parseInt(elements[0]);
                    int z = Integer.parseInt(elements[1]);
                    String owner = chunkStore.getString(key+".owner");
                    String allowed = chunkStore.getString(key+".allowed","");
                    double salePrice = chunkStore.getDouble(key+".forsale",0);
                    boolean allowMobs = chunkStore.getBoolean(key+".allowmobs",false);
                    long lastActive = chunkStore.getLong(key+".lastActive",0);
                    if (!values.equals("")) {
                        values += ",";
                    }
                    chunkDb.query("INSERT OR REPLACE INTO MyChunks (world,x,z,owner,allowed,salePrice,allowMobs,lastActive) VALUES ('"+world+"',"+x+","+z+",'"+owner+"','"+allowed+"',"+salePrice+","+(allowMobs?"1":"0")+","+lastActive+")");
                }
            }
            getLogger().info("YML > SQLite Conversion Complete!");
        }
        chunkFile.delete();
    }
    
    private void loadConfig(boolean reload) {
        if (reload) {
            reloadConfig();
        }
        config = getConfig();
        maxChunks = config.getInt("max_chunks", 8);
        config.set("max_chunks", maxChunks);
        allowNeighbours = config.getBoolean("allow_neighbours", false);
        config.set("allow_neighbours", allowNeighbours);
        protectUnclaimed = config.getBoolean("protect_unclaimed", false);
        config.set("protect_unclaimed", protectUnclaimed);
        useClaimExpiry = config.getBoolean("useClaimExpiry", false);
        config.set("useClaimExpiry", useClaimExpiry);
        claimExpiryDays = config.getInt("claimExpiresAfter", 7);
        config.set("claimExpiresAfter", claimExpiryDays);
        allowNether = config.getBoolean("allowNether", true);
        config.set("allowNether", allowNether);
        allowEnd = config.getBoolean("allowEnd", true);
        config.set("allowEnd", allowEnd);
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
            } else if (!reload) {
                getLogger().info("No economy plugin found! Chunks will be free");
            }
        } else if (!reload) {
            getLogger().info("Vault not found! Chunks will be free");
        }
        saveConfig();
    }
}
