package me.ellbristow.mychunk;

import java.util.Arrays;
import java.util.HashMap;
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
    private String[] availableFlags = {"*","B","C","D","I","L","O","U","W"};
    
    public MyChunkChunk (Block block, MyChunk instance) {
        plugin = instance;
        chunk = block.getChunk();
        chunkWorld = chunk.getWorld().getName();
        chunkX = chunk.getX();
        chunkZ = chunk.getZ();
        String[] thisDims = {chunkWorld, chunkX+"", chunkZ+""};
        dims = thisDims;
        owner = plugin.chunkStore.getString(this.dimsToConfigString() + ".owner", "Unowned");
        String allowedString = plugin.chunkStore.getString(this.dimsToConfigString() + ".allowed", "");
        if (!allowedString.equals("")) {
            for (String allowedPlayer : allowedString.split(";")) {
                String[] splitPlayer = allowedPlayer.split(":");
                allowed.put(splitPlayer[0], splitPlayer[1]);
            }
        }
        String disallowedString = plugin.chunkStore.getString(this.dimsToConfigString() + ".disallowed", "");
        if (!disallowedString.equals("")) {
            for (String disallowedPlayer : disallowedString.split(";")) {
                String[] splitPlayer = disallowedPlayer.split(":");
            }
        }
        chunkNE = findCorner("NE");
        chunkSE = findCorner("SE");
        chunkSW = findCorner("SW");
        chunkNW = findCorner("NW");
    }
    
    public void claim(String playerName) {
        this.owner = playerName;
        plugin.chunkStore.set(this.dimsToConfigString() + ".owner", playerName);
        plugin.claimedChunks++;
        plugin.chunkStore.set("TotalOwned", plugin.claimedChunks);
        plugin.saveChunkStore();
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
        plugin.chunkStore.set(this.dimsToConfigString() + ".owner", null);
        plugin.claimedChunks--;
        plugin.chunkStore.set("TotalOwned", plugin.claimedChunks);
        plugin.saveChunkStore();
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
        String flags = allowed.get(playerName.toLowerCase());
        if (flags == null) {
            flags = "";
        }
        String allFlags = "";
        for (String thisFlag : availableFlags) {
            if (!"*".equals(thisFlag)) {
                allFlags += thisFlag;
            }
        }
        if (!"*".equals(flag) && !isAllowed(playerName.toLowerCase(), flag)) {
            flags += flag.toUpperCase();
            char[] flagArray = flags.toCharArray();
            Arrays.sort(flagArray);
            flags = new String(flagArray);
        }
        if ("*".equals(flag) || flags.equals(allFlags)) {
            flags = "*";
            if ("*".equals(playerName.toLowerCase())) {
                allowed.clear();
            }
        }
        allowed.put(playerName.toLowerCase(), flags);
        savePerms();
    }
    
    public void disallow(String playerName, String flag) {
        String flags = allowed.get(playerName.toLowerCase());
        if (flags == null) {
            return;
        } else if (flags.equals("*")) {
            flags = "";
            for (String thisFlag : availableFlags) {
                if (!"*".equals(thisFlag)) {
                    flags += thisFlag;
                }
            }
        }
        if (!"*".equals(flag) && isAllowed(playerName.toLowerCase(), flag)) {
            flags = flags.replaceAll(flag.toUpperCase(), "");
            if ("*".equals(playerName.toLowerCase())) {
                allowed.clear();
            }
            allowed.put(playerName.toLowerCase(), flags);
            savePerms();
        } else if (!"*".equals(flag) && !isAllowed(playerName.toLowerCase(), flag)) {
            //Do stuff
        } else if ("*".equals(flag)) {
            if ("*".equals(playerName.toLowerCase())) {
                allowed.clear();
            }
            savePerms();
        }
    }
    
    public String[] getNeighbours() {
        MyChunkChunk chunkX1 = new MyChunkChunk(chunk.getWorld().getChunkAt(chunkX + 1, chunkZ).getBlock(5, 64, 5), plugin);
        MyChunkChunk chunkX2 = new MyChunkChunk(chunk.getWorld().getChunkAt(chunkX - 1, chunkZ).getBlock(5, 64, 5), plugin);
        MyChunkChunk chunkZ1 = new MyChunkChunk(chunk.getWorld().getChunkAt(chunkX, chunkZ + 1).getBlock(5, 64, 5), plugin);
        MyChunkChunk chunkZ2 = new MyChunkChunk(chunk.getWorld().getChunkAt(chunkX, chunkZ - 1).getBlock(5, 64, 5), plugin);
        String[] neighbours = {chunkX1.getOwner(), chunkX2.getOwner(), chunkZ1.getOwner(), chunkZ2.getOwner()};
        return neighbours;
    }
    
    public String getAllowed() {
        String allowedPlayers = "";
        Object[] players = allowed.keySet().toArray();
        if (players.length != 0) {
            if ("*".equals((String)players[0])) {
                allowedPlayers = "*";
            } else {
                for (Object player : players) {
                    allowedPlayers += " " + player + "(" + getAllowedFlags((String)player) + ChatColor.GREEN + ")";
                }
                allowedPlayers = allowedPlayers.trim();
                if ("".equals(allowedPlayers)) {
                    allowedPlayers = "NONE";
                }
            }
        } else {
            allowedPlayers = "NONE";
        }
        return allowedPlayers;
    }
    
    public String getAllowedFlags(String playerName) {
        String flags = "";
        for (String flag : availableFlags) {
            if (!"*".equals(flag) && isAllowed(playerName, flag)) {
                flags += flag;
            }
        }
        flags = flags.trim();
        if ("BCDILOUW".equalsIgnoreCase(flags)) {
          flags = "*";  
        } 
        if (!"".equals(flags)) {
            return ChatColor.GREEN + flags;
        } else {
            return ChatColor.RED + "NONE";
        }
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
        MyChunkChunk chunkX1 = new MyChunkChunk(chunk.getWorld().getChunkAt(chunkX + 1, chunkZ).getBlock(5, 64, 5), plugin);
        MyChunkChunk chunkX2 = new MyChunkChunk(chunk.getWorld().getChunkAt(chunkX - 1, chunkZ).getBlock(5, 64, 5), plugin);
        MyChunkChunk chunkZ1 = new MyChunkChunk(chunk.getWorld().getChunkAt(chunkX, chunkZ + 1).getBlock(5, 64, 5), plugin);
        MyChunkChunk chunkZ2 = new MyChunkChunk(chunk.getWorld().getChunkAt(chunkX, chunkZ - 1).getBlock(5, 64, 5), plugin);
        if (!chunkX1.isClaimed() || !chunkX2.isClaimed() || !chunkZ1.isClaimed() || !chunkZ2.isClaimed()) {
            return true;
        }
        return false;
    }
    
    public boolean isClaimed() {
        if (!owner.equals("Unowned")) {
            return true;
        }
        return false;
    }
    
    public boolean isAllowed(String playerName, String flag) {
        String allowedFlags = allowed.get(playerName.toLowerCase());
        if (allowedFlags != null) {
            char[] flags = allowedFlags.toUpperCase().toCharArray();
            for (char checkFlag: flags) {
                if (flag.charAt(0) == checkFlag || "*".charAt(0) == checkFlag) {
                    return true;
                 }
            }
        }
        allowedFlags = allowed.get("*");
        if (allowedFlags != null) {
            char[] flags = allowedFlags.toUpperCase().toCharArray();
            for (char checkFlag: flags) {
                if (flag.charAt(0) == checkFlag || "*".charAt(0) == checkFlag) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public boolean isFlag(String flag) {
        for (String thisFlag : availableFlags) {
            if (thisFlag.equalsIgnoreCase(flag)) {
                return true;
            }
        }
        return false;
    }
    
    public void setOwner (String newOwner) {
        if (isClaimed()) {
            plugin.chunkStore.set(this.dimsToConfigString() + ".owner", newOwner);
            plugin.saveChunkStore();
        } else {
            claim(newOwner);
        }
    }
        
    /*
     * Background Methods
     */
    private String dimsToConfigString() {
        return dims[0] + "_" + dims[1] + "_" + dims[2];
    }
    
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
        while (checkBlock.getTypeId() != 0) {
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
        String newAllowed = "";
        Object[] allowedPlayers = allowed.keySet().toArray();
        Object[] allowedFlags = allowed.values().toArray();
        for (int i = 0; i < allowedPlayers.length; i++) {
            if (!newAllowed.equals("")) {
                newAllowed += ";";
            }
            newAllowed += allowedPlayers[i] + ":" + allowedFlags[i];
        }
        plugin.chunkStore.set(this.dimsToConfigString() + ".allowed", newAllowed);
        plugin.saveChunkStore();
    }

}
