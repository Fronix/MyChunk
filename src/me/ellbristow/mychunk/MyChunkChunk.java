package me.ellbristow.mychunk;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import me.ellbristow.mychunk.utils.SQLiteBridge;
import me.ellbristow.mychunk.lang.Lang;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class MyChunkChunk {
    
    private String owner;
    private HashMap<String,String> allowed = new HashMap<String, String>();
    private Chunk chunk;
    private String chunkWorld;
    private Integer chunkX;
    private Integer chunkZ;
    private Block chunkNE;
    private Block chunkSE;
    private Block chunkSW;
    private Block chunkNW;
    private boolean forSale;
    private double claimPrice;
    private static String[] availableFlags = {"*","B","C","D","I","L","O","S","U","W"};
    private boolean allowMobs;
    private long lastActive;
    
    public MyChunkChunk (Block block) {
        chunk = block.getChunk();
        getFromChunk(chunk);
    }
    
    public MyChunkChunk (String world, int x, int y) {
        chunk = Bukkit.getServer().getWorld(world).getChunkAt(x, y);
        getFromChunk(chunk);
    }
    
    private void getFromChunk(Chunk chunk) {
        chunkWorld = chunk.getWorld().getName();
        chunkX = chunk.getX();
        chunkZ = chunk.getZ();
        HashMap<Integer, HashMap<String, Object>> chunkData = SQLiteBridge.select("", "MyChunks", "world = '"+chunkWorld+"' AND x = "+chunkX+" AND z = " + chunkZ, "", "");
        if (chunkData.isEmpty()) {
            owner = "Unowned";
            claimPrice = MyChunk.getDoubleSetting("chunkPrice");
            forSale = false;
            allowMobs = true;
            lastActive = new Date().getTime() / 1000;
            chunkNE = findCorner("NE");
            chunkSE = findCorner("SE");
            chunkSW = findCorner("SW");
            chunkNW = findCorner("NW");
        } else {
            owner = (String)chunkData.get(0).get("owner");
            if (owner.equals("")) owner = "Unowned";
            Double price = Double.parseDouble(chunkData.get(0).get("salePrice")+"");
            if (price == 0) {
                claimPrice = MyChunk.getDoubleSetting("chunkPrice");
                forSale = false;
            } else {
                claimPrice = price;
                forSale = true;
            }
            String allowedString = (String)chunkData.get(0).get("allowed");
            if (!allowedString.equals("")) {
                for (String allowedPlayer : allowedString.split(";")) {
                    String[] splitPlayer = allowedPlayer.split(":");
                    allowed.put(splitPlayer[0], splitPlayer[1]);
                }
            }
            chunkNE = findCorner("NE");
            chunkSE = findCorner("SE");
            chunkSW = findCorner("SW");
            chunkNW = findCorner("NW");
            allowMobs = "1".equals(chunkData.get(0).get("allowMobs"));

            // Claim expiry check
            lastActive = Long.parseLong(chunkData.get(0).get("lastActive")+"");
            if (!owner.equalsIgnoreCase("Server")){
                if (lastActive == 0) {
                    lastActive = new Date().getTime() / 1000;
                    SQLiteBridge.query("UPDATE MyChunks SET lastActive = " + lastActive + " WHERE world = '"+chunkWorld+"' AND x = " + chunkX + " AND z = " + chunkZ);
                }
                if (MyChunk.getToggle("useClaimExpiry")) {
                    if (lastActive < new Date().getTime() / 1000 - (MyChunk.getIntSetting("claimExpiryDays") * 60 * 60 * 24)) {
                        forSale = true;
                    }
                }
            }
        }
    }
    
    /**
     * Claim the chunk for a player
     * <p>
     * NOTE: This does not check to see if the chunk is already owned!
     * 
     * @param playerName Name of the player claiming the chunk
     */
    public void claim(String playerName) {
        this.owner = playerName;
        SQLiteBridge.query("INSERT OR REPLACE INTO MyChunks (world, x, z, owner, salePrice, allowMobs, allowed, lastActive) VALUES ('"+chunkWorld+"', "+chunkX+", "+chunkZ+", '"+playerName+"', 0, 0, '', "+lastActive+")");
        forSale = false;
        if (chunkNE.isLiquid() || chunkNE.getTypeId() == 79) {
            chunkNE.setTypeId(4);
        }
        Block above = chunkNE.getWorld().getBlockAt(chunkNE.getX(), chunkNE.getY()+1, chunkNE.getZ());
        above.setTypeId(50);
        if (chunkSE.isLiquid() || chunkSE.getTypeId() == 79) {
            chunkSE.setTypeId(4);
        }
        above = chunkSE.getWorld().getBlockAt(chunkSE.getX(), chunkSE.getY()+1, chunkSE.getZ());
        above.setTypeId(50);
        if (chunkSW.isLiquid() || chunkSW.getTypeId() == 79) {
            chunkSW.setTypeId(4);
        }
        above = chunkSW.getWorld().getBlockAt(chunkSW.getX(), chunkSW.getY()+1, chunkSW.getZ());
        above.setTypeId(50);
        if (chunkNW.isLiquid() || chunkNW.getTypeId() == 79) {
            chunkNW.setTypeId(4);
        }
        above = chunkNW.getWorld().getBlockAt(chunkNW.getX(), chunkNW.getY()+1, chunkNW.getZ());
        above.setTypeId(50);
    }
    
    /**
     * Unclaim the chunk
     */
    public void unclaim() {
        owner = "Unowned";
        SQLiteBridge.query("DELETE FROM MyChunks WHERE world = '"+chunkWorld+"' AND x = " + chunkX + " AND z = " + chunkZ);
        Block above = chunkNE.getWorld().getBlockAt(chunkNE.getX(), chunkNE.getY()+1, chunkNE.getZ());
        if (above.getTypeId()==50) {
            above.setTypeId(0);
        }
        above = chunkSE.getWorld().getBlockAt(chunkSE.getX(), chunkSE.getY()+1, chunkSE.getZ());
        if (above.getTypeId()==50) {
            above.setTypeId(0);
        }
        above = chunkSW.getWorld().getBlockAt(chunkSW.getX(), chunkSW.getY()+1, chunkSW.getZ());
        if (above.getTypeId()==50) {
            above.setTypeId(0);
        }
        above = chunkNW.getWorld().getBlockAt(chunkNW.getX(), chunkNW.getY()+1, chunkNW.getZ());
        if (above.getTypeId()==50) {
            above.setTypeId(0);
        }
    }
    
    /**
     * Give a player access flags in this chunk
     * 
     * @param playerName Player to give access to
     * @param flag Access flags to allow
     */
    public void allow(String playerName, String flag) {
        playerName = playerName.toLowerCase();
        flag = flag.replaceAll(" ","").toUpperCase();
        String flags = allowed.get(playerName);
        if (flags == null) {
            flags = "";
        }
        String allFlags = "";
        for (String thisFlag : availableFlags) {
            if (!"*".equals(thisFlag)) {
                allFlags += thisFlag;
            }
        }
        if (!"*".equals(flag) && !isAllowed(playerName, flag)) {
            flags += flag.toUpperCase();
            char[] flagArray = flags.toCharArray();
            Arrays.sort(flagArray);
            flags = new String(flagArray);
        }
        if ("*".equals(flag) || flags.equals(allFlags)) {
            flags = "*";
            if ("*".equals(playerName)) {
                allowed.clear();
            }
        }
        allowed.put(playerName.toLowerCase(), flags);
        savePerms();
    }
    
    /**
     * Remove access flags for a player in this chunk
     * 
     * @param playerName Player to disallow
     * @param flagString Access flags to remove
     */
    public void disallow(String playerName, String flagString) {
        playerName = playerName.toLowerCase();
        flagString = flagString.toUpperCase().replaceAll(" ","");
        if ("*".equals(playerName) && ("*".equals(flagString) || "".equals(flagString) || "".equals(flagString))) {
            allowed.clear();
        } else if ("*".equals(flagString) || "".equals(flagString) || " ".equals(flagString)) {
            allowed.put(playerName,"");
        } else {
            String flags = allowed.get(playerName);
            if (flags == null && !"*".equals(playerName)) {
                return;
            } else if ("*".equals(flags)) {
                flags = "";
                for (String thisFlag : availableFlags) {
                    if (!"*".equals(thisFlag)) {
                        flags += thisFlag;
                    }
                }
            }
            for(int i = 0; i < flagString.length(); i++) {
                String flag = flagString.substring(i, i+1);
                if ("*".equals(flag)) {
                    if ("*".equals(playerName)) {
                        Object[] players = allowed.keySet().toArray();
                        for (Object player : players) {
                            String perms = allowed.get((String)player);
                            perms = perms.replaceAll(flag,"");
                            allowed.put((String)player,perms);
                        }
                    } else {
                        allowed.put(playerName, "");
                    }
                    break;
                } else {
                    if ("*".equals(playerName)) {
                        Object[] players = allowed.keySet().toArray();
                        for (Object player : players) {
                            String perms = allowed.get((String)player);
                            if ("*".equals(perms)) {
                                perms = "";
                                for (String thisFlag : availableFlags) {
                                    if (!"*".equals(thisFlag)) {
                                        perms += thisFlag;
                                    }
                                }
                            }
                            perms = perms.replaceAll(flag,"");
                            allowed.put((String)player,perms);
                        }
                    } else {
                        flags = flags.replaceAll(flag, "");
                        allowed.put(playerName, flags);
                    }
                }
            }
        }
        savePerms();
    }
    
    /**
     * Get a string of all allowed players and their allowed flags
     * 
     * @return 
     */
    public String getAllowed() {
        String allowedPlayers = "";
        Object[] players = allowed.keySet().toArray();
        if (players.length != 0) {
            if ("*".equals((String)players[0]) && getAllowedFlags("*").equalsIgnoreCase("*")) {
                allowedPlayers = Lang.get("Everyone")+"(*)";
            } else {
                for (Object player : players) {
                    if (player.equals("*")) {
                        player = Lang.get("Everyone");
                    }
                    allowedPlayers += " " + player + "(" + getAllowedFlags((String)player) + ChatColor.GREEN + ")";
                }
                allowedPlayers = allowedPlayers.trim();
                if ("".equals(allowedPlayers)) {
                    allowedPlayers = Lang.get("None");
                }
            }
        } else {
            allowedPlayers = Lang.get("None");
        }
        return allowedPlayers;
    }
    
    /**
     * Check what access flags a player has
     * 
     * @param playerName Player to check
     * @return String containing all current access flags
     */
    public String getAllowedFlags(String playerName) {
        String flags = "";
        String allFlags = "";
        for (String flag : availableFlags) {
            if (!"*".equals(flag)) {
                if (isAllowed(playerName, flag)) {
                    flags += flag;
                }
                allFlags += flag;
            }
        }
        flags = flags.trim();
        allFlags = allFlags.trim();
        if (allFlags.equalsIgnoreCase(flags)) {
          flags = "*";  
        } 
        if (!"".equals(flags)) {
            return ChatColor.GREEN + flags;
        } else {
            return ChatColor.RED + Lang.get("None");
        }
    }
    
    /**
     * Change whether mobs can spawn/damage in this chunk
     * 
     * @param allow New setting
     */
    public void setAllowMobs(Boolean allow) {
        allowMobs = allow;
        SQLiteBridge.query("UPDATE MyChunks SET allowMobs = " + (allow?"1":"0") + " WHERE world = '"+chunkWorld+"' AND x = " + chunkX + " AND z = " + chunkZ);
    }
    
    /**
     * Check if mobs can spawn/damage in this chunk
     * 
     * @return 
     */
    public boolean getAllowMobs() {
        return allowMobs;
    }
    
    /**
     * Get the standard claim price for this chunk
     * 
     * @return 
     */
    public double getClaimPrice() {
        return claimPrice;
    }
    
    /**
     * Get the overbuy price for this chunk<br>
     * NOTE: Claim Price + Overbuy premium
     * 
     * @return 
     */
    public double getOverbuyPrice() {
        double price;
        if (MyChunk.getToggle("allowOverbuy")) {
            price = claimPrice + MyChunk.getDoubleSetting("overbuyPrice");
        } else {
            price = claimPrice;
        }
        return price;
    }
    
    /**
     * Get an array of MyChunkChunks representing all chunks neighbouring this one
     * 
     * @return 
     */
    public MyChunkChunk[] getNeighbours() {
        MyChunkChunk chunkX1 = new MyChunkChunk(chunkWorld, chunkX + 1, chunkZ);
        MyChunkChunk chunkX2 = new MyChunkChunk(chunkWorld, chunkX - 1, chunkZ);
        MyChunkChunk chunkZ1 = new MyChunkChunk(chunkWorld, chunkX, chunkZ + 1);
        MyChunkChunk chunkZ2 = new MyChunkChunk(chunkWorld, chunkX, chunkZ - 1);
        MyChunkChunk[] neighbours = {chunkX1, chunkX2, chunkZ1, chunkZ2};
        return neighbours;
    }
    
    /**
     * Get the name of the owner of this chunk
     * 
     * @return 
     */
    public String getOwner() {
        return owner;
    }
    
    /**
     * Get the name of the world this chunk is in
     * @return 
     */
    public String getWorldName() {
        return chunkWorld;
    }
    
    /**
     * Get the X coordinate of this chunk
     * 
     * @return 
     */
    public int getX() {
        return chunkX;
    }
    
    /**
     * Get the Z coordinate of this chunk
     * 
     * @return 
     */
    public int getZ() {
        return chunkZ;
    }
    
    /**
     * Check if this chunk has neighbours
     * 
     * @return 
     */
    public boolean hasNeighbours() {
        HashMap<Integer, HashMap<String, Object>> results = SQLiteBridge.select("owner", "MyChunks", "world = '"+chunkWorld+"' AND ("
                                                          + "(x = "+(chunkX + 1)+" AND z = "+chunkZ+") OR "
                                                          + "(x = "+(chunkX - 1)+" AND z = "+chunkZ+") OR "
                                                          + "(x = "+chunkX+" AND z = "+(chunkZ + 1)+") OR "
                                                          + "(x = "+chunkX+" AND z = "+(chunkZ - 1)+")"
                                                          + ")", "", "");
        if (results.isEmpty()) {
            return false;
        }
        return true;
    }
    
    /**
     * Check if a player has an access flag
     * 
     * @param playerName Player name to check for
     * @param flag Access flag to check for
     * @return 
     */
    public boolean isAllowed(String playerName, String flag) {
        String allowedFlags = allowed.get(playerName.toLowerCase());
        if (allowedFlags != null) {
            char[] flags = allowedFlags.toUpperCase().toCharArray();
            for (char checkFlag: flags) {
                for (char thisFlag : flag.toUpperCase().toCharArray()) {
                    if (thisFlag == checkFlag || "*".charAt(0) == checkFlag) {
                        return true;
                    }
                }
            }
        }
        allowedFlags = allowed.get("*");
        if (allowedFlags != null) {
            char[] flags = allowedFlags.toUpperCase().toCharArray();
            for (char checkFlag: flags) {
                for (char thisFlag : flag.toUpperCase().toCharArray()) {
                    if (thisFlag == checkFlag || "*".charAt(0) == checkFlag) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Check if this chunk is claimed
     * 
     * @return 
     */
    public boolean isClaimed() {
        if (owner.equalsIgnoreCase("Unowned")) {
            return false;
        }
        return true;
    }
    
    /**
     * Check if this chunk is for sale
     * 
     * @return 
     */
    public boolean isForSale() {
        return forSale;
    }
    
    /**
     * Set the chunk as For Sale for the specified price
     * 
     * @param price New For Sale price
     */
    public void setForSale(Double price) {
        forSale = true;
        claimPrice = price;
        SQLiteBridge.query("UPDATE MyChunks SET salePrice = " + claimPrice + " WHERE world = '"+chunkWorld+"' AND x = " + chunkX + " AND z = " + chunkZ);
    }
    
    /**
     * Remove For Sale status from this chunk
     */
    public void setNotForSale() {
        forSale = false;
        SQLiteBridge.query("UPDATE MyChunks SET salePrice = 0 WHERE world = '"+chunkWorld+"' AND x = " + chunkX + " AND z = " + chunkZ);
    }
    
    /**
     * Set a new owner for this chunk without the claim process
     * 
     * @param newOwner Name of the 
     */
    public void setOwner (String newOwner) {
        if (isClaimed()) {
            SQLiteBridge.query("UPDATE MyChunks SET owner = '"+newOwner+"' WHERE world = '"+chunkWorld+"' AND x = " + chunkX + " AND z = " + chunkZ);
        } else {
            claim(newOwner);
        }
    }
    
    /**
     * Set the timestamp of the last time the owner of this chunk was seen active<br>
     * Used to check for Claim Expiry
     * 
     * @param time Timestamp of the last activity
     */
    public void setLastActive(long time) {
        lastActive = time;
    }
        
    /*
     * Background Methods
     */
    
    private Block findCorner(String corner) {
        int y = chunk.getWorld().getMaxHeight()-1;
        int x = 0;
        int z = 0;
        if (corner.equalsIgnoreCase("NE")) {
            x = 15;
            z = 15;
        } else if (corner.equalsIgnoreCase("SE")) {
            x = 15;
        } else if (corner.equalsIgnoreCase("NW")) {
            z = 15;
        }
        // First find the highest buildable AIR block in the correct corner
        Block checkBlock = chunk.getBlock( x , y , z );
        while (checkBlock.getTypeId() != 0 && y > 0) {
            y--;
            checkBlock = chunk.getBlock( x , y , z );
        }
        // Now we have an air block, drop down until we find a block which is solid
        int attempts = 0;
        while (notAttachable(checkBlock)) {
            if (attempts > chunk.getWorld().getMaxHeight()) {
                // ALL AIR (i.e. THE_END)
                checkBlock = chunk.getBlock(x,64,z);
                break;
            }
            y--;
            checkBlock = chunk.getBlock( x , y , z );
            attempts++;
        }
        return checkBlock;
    }
    
    private boolean notAttachable(Block block) {
        Integer[] nonSolids = {0, 6, 10, 11, 18, 30, 31, 32, 37, 38, 39, 40, 50, 51, 59, 75, 76, 78, 83, 90, 104, 105, 106, 111, 115, 119};
        for (int type : nonSolids) {
            if (block.getTypeId() == type) {
                return true;
            }
        }
        return false;
    }
    
    private void savePerms() {
        if (allowed.isEmpty()) {
            SQLiteBridge.query("UPDATE MyChunks SET allowed = '' WHERE world = '"+chunkWorld+"' AND x = " + chunkX + " AND z = " + chunkZ);
        } else {
            String newAllowed = "";
            Object[] allowedPlayers = allowed.keySet().toArray();
            Object[] allowedFlags = allowed.values().toArray();
            for (int i = 0; i < allowedPlayers.length; i++) {
                if (!"".equals(allowedFlags[i])) {
                    if (!newAllowed.equals("")) {
                        newAllowed += ";";
                    }
                    newAllowed += allowedPlayers[i] + ":" + allowedFlags[i];
                } else {
                    allowed.remove((String)allowedPlayers[i]);
                }
            }
            if ("".equals(newAllowed)) {
                SQLiteBridge.query("UPDATE MyChunks SET allowed = '' WHERE world = '"+chunkWorld+"' AND x = " + chunkX + " AND z = " + chunkZ);
            } else {
                SQLiteBridge.query("UPDATE MyChunks SET allowed = '"+newAllowed+"' WHERE world = '"+chunkWorld+"' AND x = " + chunkX + " AND z = " + chunkZ);
            }
        }
    }
    
    /*
     * Static Functions
     */
    
     /**
     * Checks if a player is allowed to perform a particular action in the given chunk
     * <p>
     * Allowed Flags:<br>
     * B : Build<br>
     * C : Chest Access<br>
     * D : Destroy<br>
     * I : Ignite<br>
     * L : Lava Buckets<br>
     * O : Open Wooden Doors<br>
     * S : Special Blocks (Furnace, Workbench etc)<br>
     * U : Use levers, buttons etc<br>
     * W : Water Buckets
     * 
     * @param chunk Chunk to be checked
     * @param player Player to be checked
     * @param flag Flag for the action being performed
     * @return True if allowed, otherwise false
     */
    public static boolean isAllowed(Chunk chunk, Player player, String flag) {
        
        flag = flag.toUpperCase();
        HashMap<Integer, HashMap<String, Object>> results = SQLiteBridge.select("owner, allowed", "MyChunks", "world = '"+chunk.getWorld().getName() +"' AND x = " + chunk.getX() + " AND z = " + chunk.getZ(), "", "");
        
        if (results.isEmpty()) {
            
            if (MyChunk.getToggle("protectUnclaimed") && !player.hasPermission("mychunk.override")) {
                return false;
            }
            
            return true;
            
        } else {
            
            String owner = results.get(0).get("owner").toString();
            
            if (player.hasPermission("mychunk.override") && !owner.equalsIgnoreCase("Server")) {
                return true;
            }
            
            if (player.getName().equalsIgnoreCase(owner)) {
                return true;
            } else {
                
                String allAllowed = results.get(0).get("allowed").toString();
                
                for (String players : allAllowed.split(";")) {
                    
                    String[] split = players.split(":");
                    
                    if (split[0].equalsIgnoreCase(player.getName()) || split[0].equals("*")) {
                        
                        if (split[1].contains(flag) || split[1].equals("*")) {
                            return true;
                        }
                    }
                    
                }
                
                if (owner.equalsIgnoreCase("Server")) {
                    return serverCheck(player, flag);
                }
                
                return false;
                
            }
            
        }
        
    }
    
    /**
     * Check if a chunk is claimed
     * 
     * @param chunk Chunk to be checked
     * @return True if chunk claimed, otherwise false
     */
    public static boolean isClaimed(Chunk chunk) {
        
        if (getOwner(chunk).equalsIgnoreCase("")) {
            return false;
        }
        
        return true;
        
    }
    
    /**
     * Check if the provided character is a valid access flag
     * 
     * @param flag Access flag to check for
     * @return 
     */
    public static boolean isFlag(String flag) {
        
        for (String thisFlag : availableFlags) {
            
            if (thisFlag.equalsIgnoreCase(flag)) {
                return true;
            }
            
        }
        
        return false;
        
    }
    
    public static boolean getAllowMobs(Chunk chunk) {
        
        HashMap<Integer, HashMap<String, Object>> results = SQLiteBridge.select("allowMobs", "MyChunks", "world = '"+chunk.getWorld().getName()+"' AND x = "+chunk.getX()+" AND z = " + chunk.getZ(), "", "");
        
        if (!results.isEmpty()) {
            if (results.get(0).get("allowMobs").toString().equalsIgnoreCase("0")) {
                return false;
            }
        }
        
        return true;
        
    }
    
    /**
     * Find the owner of a chunk
     * 
     * @param chunk Chunk to be checked
     * @return Name of the owner or "" if unclaimed
     */
    public static String getOwner(Chunk chunk) {
        
        HashMap<Integer, HashMap<String, Object>> results = SQLiteBridge.select("owner", "MyChunks", "world = '"+chunk.getWorld().getName()+"' AND x = "+chunk.getX()+" AND z = " + chunk.getZ(), "", "");
        
        if (!results.isEmpty()) {
            
            HashMap<String, Object> result = results.get(0);
            return result.get("owner").toString();
            
        }
        
        return "";
        
    }
    
    /**
     * Returns the price for a player to claim a chunk, taking into account overbuy pricing if applicable
     * 
     * @param chunk Chunk to check
     * @param player Player wanting to buy
     * @return Claim price of 0 if not claimable
     */
    public static double getClaimPrice(Chunk chunk, Player player) {
        
        HashMap<Integer, HashMap<String, Object>> results = SQLiteBridge.select("owner, salePrice, (SELECT COUNT(*) FROM MyChunks WHERE owner = '"+player.getName()+"') AS alreadyOwned", "MyChunks", "world = '"+chunk.getWorld().getName()+"' AND x = "+chunk.getX()+" AND z = " + chunk.getZ(), "", "");
        
        if (!results.isEmpty()) {
            
            int alreadyOwned = Integer.parseInt(results.get(0).get("alreadyOwned").toString());
            String owner = results.get(0).get("owner").toString();
            double salePrice = Double.parseDouble(results.get(0).get("salePrice").toString());
            
            int maxChunks = MyChunk.getMaxChunks(player);
            
            if (!owner.equalsIgnoreCase(player.getName())) {
                
                if (maxChunks == 0 || alreadyOwned < maxChunks) {
                    return salePrice;
                } else {

                    // Overbuy

                    if (MyChunk.getToggle("allowOverbuy")) {
                        
                        if (!player.hasPermission("mychunk.claim.unlimited") && MyChunk.getToggle("overbuyP2P")) {
                            return salePrice + MyChunk.getDoubleSetting("overbuyPrice");
                        } else {
                            return salePrice;
                        }
                        
                    } else {
                        return 0;
                    }

                }
                
            }
            
            return 0;
            
        }
        
        return 0;
        
    }
    
    /**
     * Returns the price for a player to claim an UNOWNED chunk, taking into account overbuy pricing if applicable
     * 
     * @param chunk Chunk to check
     * @param player Player wanting to buy
     * @return Claim price of 0 if not claimable
     */
    public static double getChunkPrice(Chunk chunk, Player player) {
        
        int ownedChunks = getOwnedChunkCount(player.getName());
        
        int maxChunks = MyChunk.getMaxChunks(player);
        
        if (maxChunks == 0 || ownedChunks < maxChunks) {
            return MyChunk.getDoubleSetting("chunkPrice");
        } else {
            
            // Overbuy
            
            if (MyChunk.getToggle("allowOverbuy")) {
                
                if (!player.hasPermission("mychunk.claim.unlimited")) {
                    return MyChunk.getDoubleSetting("chunkPrice") + MyChunk.getDoubleSetting("overbuyPrice");
                } else {
                    return MyChunk.getDoubleSetting("chunkPrice");
                }
                
            } else {
                return 0;
            }
            
        }
    }
    
    /**
     * Returns the number of chunks a player owns
     * 
     * @param playerName
     * @return Number of chunks
     */
    public static int getOwnedChunkCount(String playerName) {
        
        HashMap<Integer, HashMap<String, Object>> results = SQLiteBridge.select("COUNT(*) as counter", "MyChunks", "owner = '"+playerName+"'", "","");
        
        return Integer.parseInt(results.get(0).get("counter")+"");
        
    }
    
    /**
     * Update activity log on all chunks owned by a player to avoid ChunkExpiry unclaiming them
     * 
     * @param playerName Name of the player to update
     */
    public static void refreshOwnership(String playerName) {
        SQLiteBridge.query("UPDATE MyChunks SET lastActive = " + (new Date().getTime() / 1000) + " WHERE owner = '"+playerName+"'");
    }
    
    private static boolean serverCheck(Player player, String flag) {
        
        if (flag.equals("B") && player.hasPermission("mychunk.server.build")) return true;
        if (flag.equals("C") && player.hasPermission("mychunk.server.chests")) return true;
        if (flag.equals("D") && player.hasPermission("mychunk.server.destroy")) return true;
        if (flag.equals("I") && player.hasPermission("mychunk.server.ignite")) return true;
        if (flag.equals("L") && player.hasPermission("mychunk.server.lava")) return true;
        if (flag.equals("O") && player.hasPermission("mychunk.server.doors")) return true;
        if (flag.equals("S") && player.hasPermission("mychunk.server.special")) return true;
        if (flag.equals("U") && player.hasPermission("mychunk.server.use")) return true;
        if (flag.equals("W") && player.hasPermission("mychunk.server.water")) return true;
        if (flag.equals("X") && player.hasPermission("mychunk.server.signs")) return true;
        
        return false;
        
    }
    
    
    /**
     * Force a chunk to be unclaimed by removing it from the database.
     * <p>
     * NOTE: This does NOT respect UnclaimRefund
     * 
     * @param chunk Chunk to be unclaimed
     */
    public static void unclaim(Chunk chunk) {
        SQLiteBridge.query("DELETE FROM MyChunks WHERE world = '"+chunk.getWorld().getName()+"' AND x = "+chunk.getX()+" AND z = " + chunk.getZ());
    }
    
}
