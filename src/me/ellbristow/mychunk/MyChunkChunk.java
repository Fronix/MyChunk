package me.ellbristow.mychunk;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import me.ellbristow.mychunk.lang.Lang;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.block.Block;

public class MyChunkChunk {
    
    private MyChunk plugin;
    private String[] dims;
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
    private String[] availableFlags = {"*","B","C","D","I","L","O","S","U","W"};
    private boolean allowMobs;
    private long lastActive;
    
    public MyChunkChunk (Block block, MyChunk instance) {
        plugin = instance;
        chunk = block.getChunk();
        getFromChunk(chunk);
    }
    
    public MyChunkChunk (String world, int x, int y, MyChunk instance) {
        plugin = instance;
        chunk = plugin.getServer().getWorld(world).getChunkAt(x, y);
        getFromChunk(chunk);
    }
    
    private void getFromChunk(Chunk chunk) {
        chunkWorld = chunk.getWorld().getName();
        chunkX = chunk.getX();
        chunkZ = chunk.getZ();
        String[] thisDims = {chunkWorld, chunkX+"", chunkZ+""};
        dims = thisDims;
        HashMap<Integer, HashMap<String, Object>> chunkData = plugin.chunkDb.select("", "MyChunks", "world = '"+chunkWorld+"' AND x = "+chunkX+" AND z = " + chunkZ, "", "");
        if (chunkData.isEmpty()) {
            owner = "Unowned";
            claimPrice = plugin.chunkPrice;
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
                claimPrice = plugin.chunkPrice;
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
                    plugin.chunkDb.query("UPDATE MyChunks SET lastActive = " + lastActive + " WHERE world = '"+chunkWorld+"' AND x = " + chunkX + " AND z = " + chunkZ);
                }
                if (plugin.useClaimExpiry) {
                    if (lastActive < new Date().getTime() / 1000 - (plugin.claimExpiryDays * 60 * 60 * 24)) {
                        forSale = true;
                    }
                }
            }
        }
    }
    
    public void claim(String playerName) {
        this.owner = playerName;
        plugin.chunkDb.query("INSERT OR REPLACE INTO MyChunks (world, x, z, owner, salePrice, allowMobs, allowed, lastActive) VALUES ('"+chunkWorld+"', "+chunkX+", "+chunkZ+", '"+playerName+"', 0, "+(allowMobs?"1":"0")+", '', "+lastActive+")");
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
    
    public void unclaim() {
        owner = "Unowned";
        plugin.chunkDb.query("DELETE FROM MyChunks WHERE world = '"+chunkWorld+"' AND x = " + chunkX + " AND z = " + chunkZ);
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
    
    public void setAllowMobs(Boolean allow) {
        allowMobs = allow;
        plugin.chunkDb.query("UPDATE MyChunks SET allowMobs = " + (allow?"1":"0") + " WHERE world = '"+chunkWorld+"' AND x = " + chunkX + " AND z = " + chunkZ);
    }
    
    public boolean getAllowMobs() {
        return allowMobs;
    }
    
    public double getClaimPrice() {
        return claimPrice;
    }
    
    public double getOverbuyPrice() {
        double price;
        if (plugin.allowOverbuy) {
            price = claimPrice + plugin.overbuyPrice;
        } else {
            price = claimPrice;
        }
        return price;
    }
    
    public MyChunkChunk[] getNeighbours() {
        MyChunkChunk chunkX1 = new MyChunkChunk(chunkWorld, chunkX + 1, chunkZ, plugin);
        MyChunkChunk chunkX2 = new MyChunkChunk(chunkWorld, chunkX - 1, chunkZ, plugin);
        MyChunkChunk chunkZ1 = new MyChunkChunk(chunkWorld, chunkX, chunkZ + 1, plugin);
        MyChunkChunk chunkZ2 = new MyChunkChunk(chunkWorld, chunkX, chunkZ - 1, plugin);
        MyChunkChunk[] neighbours = {chunkX1, chunkX2, chunkZ1, chunkZ2};
        return neighbours;
    }
    
    public String getOwner() {
        return owner;
    }
    
    public String getWorldName() {
        return chunkWorld;
    }
    
    public int getX() {
        return chunkX;
    }
    
    public int getZ() {
        return chunkZ;
    }
    
    public boolean hasNeighbours() {
        HashMap<Integer, HashMap<String, Object>> results = plugin.chunkDb.select("owner", "MyChunks", "world = '"+chunkWorld+"' AND ("
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
    
    public boolean isClaimed() {
        if (owner.equalsIgnoreCase("Unowned")) {
            return false;
        }
        return true;
    }
            
    
    public boolean isFlag(String flag) {
        for (String thisFlag : availableFlags) {
            if (thisFlag.equalsIgnoreCase(flag)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isForSale() {
        return forSale;
    }
    
    public void setForSale(Double price) {
        forSale = true;
        claimPrice = price;
        plugin.chunkDb.query("UPDATE MyChunks SET salePrice = " + claimPrice + " WHERE world = '"+chunkWorld+"' AND x = " + chunkX + " AND z = " + chunkZ);
    }
    
    public void setNotForSale() {
        forSale = false;
        plugin.chunkDb.query("UPDATE MyChunks SET salePrice = 0 WHERE world = '"+chunkWorld+"' AND x = " + chunkX + " AND z = " + chunkZ);
    }
    
    public void setOwner (String newOwner) {
        if (isClaimed()) {
            plugin.chunkDb.query("UPDATE MyChunks SET owner = '"+newOwner+"' WHERE world = '"+chunkWorld+"' AND x = " + chunkX + " AND z = " + chunkZ);
        } else {
            claim(newOwner);
        }
    }
    
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
            plugin.chunkDb.query("UPDATE MyChunks SET allowed = '' WHERE world = '"+chunkWorld+"' AND x = " + chunkX + " AND z = " + chunkZ);
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
                plugin.chunkDb.query("UPDATE MyChunks SET allowed = '' WHERE world = '"+chunkWorld+"' AND x = " + chunkX + " AND z = " + chunkZ);
            } else {
                plugin.chunkDb.query("UPDATE MyChunks SET allowed = '"+newAllowed+"' WHERE world = '"+chunkWorld+"' AND x = " + chunkX + " AND z = " + chunkZ);
            }
        }
    }

}
