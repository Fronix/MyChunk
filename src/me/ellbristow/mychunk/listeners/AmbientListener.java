package me.ellbristow.mychunk.listeners;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import me.ellbristow.mychunk.MyChunk;
import me.ellbristow.mychunk.MyChunkChunk;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PotionSplashEvent;


public class AmbientListener implements Listener {
    
    public AmbientListener() {
    }
    
    @EventHandler (priority = EventPriority.HIGH)
    public void onExplosion (EntityExplodeEvent event) {
        
        if (event.isCancelled()) return;
        
        List<Block> blocks = event.blockList();
        
        if (blocks != null) {
            
            int index = 0;
            Collection<Block> saveBlocks = new HashSet<Block>();
            
            for (Iterator<Block> it = blocks.iterator(); it.hasNext();) {
                
                Block block = it.next();
                
                if (MyChunkChunk.isClaimed(block.getChunk())) {
                    saveBlocks.add(block);
                } else if (MyChunk.getToggle("protectUnclaimed")) {
                    
                    if (!(event.getEntity() instanceof TNTPrimed) || (event.getEntity() instanceof TNTPrimed && MyChunk.getToggle("unclaimedTNT"))) {
                        saveBlocks.add(block);
                    }
                    
                }
                
                index++;
                
            }
            
            if (!saveBlocks.isEmpty()) {
                event.blockList().removeAll(saveBlocks);
            }
            
        }
        
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        
        if (event.isCancelled()) return;
        
        if (event.getBlock().getChunk() != event.getBlock().getRelative(event.getDirection()).getChunk()) {
            
            String chunk1 = MyChunkChunk.getOwner(event.getBlock().getChunk());
            String chunk2 = MyChunkChunk.getOwner(event.getBlock().getRelative(event.getDirection()).getChunk());
            
            if (!chunk2.equalsIgnoreCase("") && !chunk1.equalsIgnoreCase(chunk2)) {
                
                // Pushing into an owned chunk with a different owner
                event.setCancelled(true);
                return;
                
            }
            
        }
        
        if (!event.getDirection().equals(BlockFace.UP) && !event.getDirection().equals(BlockFace.DOWN)) {
            
            // Pushing Sideways
            List<Block> blocks = event.getBlocks();
            
            for (Block block : blocks) {
                
                if (block.getChunk() != block.getRelative(event.getDirection()).getChunk()) {
                    
                    String chunk1 = MyChunkChunk.getOwner(block.getChunk());
                    String chunk2 = MyChunkChunk.getOwner(block.getRelative(event.getDirection()).getChunk());
                    
                    if (!chunk2.equalsIgnoreCase("") && !chunk1.equalsIgnoreCase(chunk2)) {
                        
                        // Pushing into an owned chunk with a different owner
                        event.setCancelled(true);
                        
                    }
                    
                }
            }
            
        }
        
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onPistonRetract(BlockPistonRetractEvent event) {
         
        if (event.isCancelled()) return;
        
        if (!event.getDirection().equals(BlockFace.UP) && !event.getDirection().equals(BlockFace.DOWN)) {
            
            if (event.isSticky()) {
                
                if (event.getBlock().getChunk() != event.getRetractLocation().getBlock().getChunk()) {
                    
                    String chunk1 = MyChunkChunk.getOwner(event.getBlock().getChunk());
                    String chunk2 = MyChunkChunk. getOwner(event.getRetractLocation().getBlock().getChunk());
                    
                    if (!chunk2.equalsIgnoreCase("") && !chunk1.equalsIgnoreCase(chunk2)) {
                        
                        // Pulling out of an owned chunk with a different owner
                        
                        event.setCancelled(true);
                        event.getBlock().setType(Material.PISTON_STICKY_BASE);
                        
                        switch (event.getDirection()) {
                            case NORTH:
                                event.getBlock().setData((byte)4);
                                break;
                            case SOUTH:
                                event.getBlock().setData((byte)5);
                                break;
                            case EAST:
                                event.getBlock().setData((byte)2);
                                break;
                            case WEST:
                                event.getBlock().setData((byte)3);
                                break;
                        }
                        
                        event.getBlock().getRelative(event.getDirection()).setType(Material.AIR);
                        event.getBlock().getWorld().playSound(event.getBlock().getLocation(), Sound.PISTON_RETRACT, 1, 1);
                        
                    }
                }
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onPotionSplash(PotionSplashEvent event) {
        
        ThrownPotion potion = event.getPotion();
        String owner = MyChunkChunk.getOwner(potion.getLocation().getChunk());
        
        if (!owner.equalsIgnoreCase("")) {
            
            if (owner.equalsIgnoreCase("Server")) {
                event.setCancelled(true);
            } else {
                
                LivingEntity shooter = potion.getShooter();
                
                if (shooter instanceof Player) {
                    
                    Player player = (Player)shooter;
                    
                    if (owner.equalsIgnoreCase(player.getName())) {
                        event.setCancelled(true);
                    }
                    
                } else {
                    event.setCancelled(true);
                }
                
            }
            
        }
        
    }
    
    @EventHandler (priority = EventPriority.HIGH)
    public void onZombieDoorEvent (EntityBreakDoorEvent event) {
        
        if (event.isCancelled()) return;
        
        if (event.getEntityType().equals(EntityType.ZOMBIE)) {
            
            if (MyChunkChunk.isClaimed(event.getBlock().getChunk()) || MyChunk.getToggle("protectUnclaimed")) {
                event.setCancelled(true);
            }
            
        }
        
    }
    
}
