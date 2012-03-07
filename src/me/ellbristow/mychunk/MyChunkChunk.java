package me.ellbristow.mychunk;

import org.bukkit.Chunk;
import org.bukkit.block.Block;

public class MyChunkChunk {
    
    private MyChunk plugin;
    private String[] dims;
    private String owner;
    private Chunk chunk;
    private String chunkWorld;
    private Integer chunkX;
    private Integer chunkZ;
    private Block chunkNE;
    private Block chunkSE;
    private Block chunkSW;
    private Block chunkNW;
    
    public MyChunkChunk (Block block, MyChunk instance) {
        plugin = instance;
        chunk = block.getChunk();
        chunkWorld = chunk.getWorld().getName();
        chunkX = chunk.getX();
        chunkZ = chunk.getZ();
        String[] thisDims = {chunkWorld, chunkX+"", chunkZ+""};
        dims = thisDims;
        owner = plugin.chunkStore.getString(this.dimsToConfigString() + ".owner", "Unowned");
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
    
    public String[] getNeighbours() {
        MyChunkChunk chunkX1 = new MyChunkChunk(chunk.getWorld().getChunkAt(chunkX + 1, chunkZ).getBlock(5, 64, 5), plugin);
        MyChunkChunk chunkX2 = new MyChunkChunk(chunk.getWorld().getChunkAt(chunkX - 1, chunkZ).getBlock(5, 64, 5), plugin);
        MyChunkChunk chunkZ1 = new MyChunkChunk(chunk.getWorld().getChunkAt(chunkX, chunkZ + 1).getBlock(5, 64, 5), plugin);
        MyChunkChunk chunkZ2 = new MyChunkChunk(chunk.getWorld().getChunkAt(chunkX, chunkZ - 1).getBlock(5, 64, 5), plugin);
        String[] neighbours = {chunkX1.getOwner(), chunkX2.getOwner(), chunkZ1.getOwner(), chunkZ2.getOwner()};
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
        while (notAttachable(checkBlock)) {
            y--;
            checkBlock = chunk.getBlock( x , y , z );
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

}
