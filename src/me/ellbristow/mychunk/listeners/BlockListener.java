package me.ellbristow.mychunk.listeners;

import me.ellbristow.mychunk.MyChunk;
import me.ellbristow.mychunk.MyChunkChunk;
import me.ellbristow.mychunk.WorldGuardHook;
import me.ellbristow.mychunk.lang.Lang;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent.RemoveCause;
import org.bukkit.event.hanging.HangingPlaceEvent;


public class BlockListener implements Listener {
    
    
    public BlockListener() {
    }
    
    @EventHandler (priority = EventPriority.HIGH)
    public void onBlockPlace (BlockPlaceEvent event) {
        
        if (event.isCancelled()) return;
        
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Chunk chunk = event.getBlock().getChunk();
        
        if (!(block.getState() instanceof Sign)) {
            
            if (!MyChunkChunk.isAllowed(chunk, player, "B") && !WorldGuardHook.isRegion(block.getLocation())) {
                
                player.sendMessage(ChatColor.RED + Lang.get("NoPermsBuild"));
                event.setCancelled(true);
                
            }
            
        }
        
    }
    
    @EventHandler (priority = EventPriority.HIGH)
    public void onBlockBreak (BlockBreakEvent event) {
        
        if (event.isCancelled()) return;
        
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Chunk chunk = event.getBlock().getChunk();
        
        if (!MyChunkChunk.isAllowed(chunk, player, "D") && !WorldGuardHook.isRegion(block.getLocation())) {
            
            player.sendMessage(ChatColor.RED + Lang.get("NoPermsBreak"));
            event.setCancelled(true);
            
            if (block.getState() instanceof Sign) {
                
                Sign sign = (Sign)block.getState();
                sign.setLine(0, sign.getLine(0));
                sign.update();
                
            }
            
        }
        
    }
    
    @EventHandler (priority = EventPriority.HIGH)
    public void onBlockIgnite (BlockIgniteEvent event) {
        
        if (event.isCancelled()) return;
        
        if (event.getCause().equals(BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL)) {
            
            Player player = event.getPlayer();
            Block block = event.getBlock();
            Chunk chunk = event.getBlock().getChunk();

            if (!MyChunkChunk.isAllowed(chunk, player, "I") && !WorldGuardHook.isRegion(block.getLocation())) {
                
                player.sendMessage(ChatColor.RED + Lang.get("NoPermsFire"));
                event.setCancelled(true);
                
            }

        } else if (event.getCause().equals(BlockIgniteEvent.IgniteCause.LAVA) || event.getCause().equals(BlockIgniteEvent.IgniteCause.SPREAD)) {
            
            if (MyChunkChunk.isClaimed(event.getBlock().getChunk()) || MyChunk.getToggle("protectUnclaimed")) {
                event.setCancelled(true);
            }
            
        }
        
    }
    
    @EventHandler (priority = EventPriority.HIGH)
    public void onBlockFromTo (BlockFromToEvent event) {
        
        if (event.isCancelled()) return;
        
        Block block = event.getBlock();
        Block toBlock = event.getToBlock();
        
        if (block.getChunk() != toBlock.getChunk()) {
            
            String toOwner = MyChunkChunk.getOwner(toBlock.getChunk());
            String fromOwner = MyChunkChunk.getOwner(block.getChunk());
            
            if (!toOwner.equalsIgnoreCase(fromOwner)) {
                event.setCancelled(true);
            }
            
        }
        
    }
    
    @EventHandler (priority = EventPriority.HIGH)
    public void onBlockBurn (BlockBurnEvent event) {
        
        if (event.isCancelled()) return;
        
        Block block = event.getBlock();
        
        if (MyChunkChunk.isClaimed(block.getChunk())) {
            
            event.setCancelled(true);
            
            if (block.getRelative(BlockFace.UP).getType().equals(Material.FIRE)) {
                block.getRelative(BlockFace.UP).setType(Material.AIR);
            }
            
        }
        
    }
    
    @EventHandler (priority = EventPriority.HIGH)
    public void onBlockSpread (BlockSpreadEvent event) {
        
        if (event.isCancelled()) return;
        
        if (event.getSource().getType().equals(Material.WATER) || event.getSource().getType().equals(Material.LAVA)) {
            
            if (MyChunkChunk.isClaimed(event.getBlock().getChunk())) {
                event.setCancelled(true);
            }
            
        }
        
    }
    
    @EventHandler (priority = EventPriority.HIGH)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        
        if (event.isCancelled()) return;
        
        Entity remover = event.getRemover();
        Chunk chunk = event.getEntity().getLocation().getChunk();
        
        if (remover instanceof Player) {
            
            if (!MyChunkChunk.isAllowed(chunk, (Player)remover, "D") && !WorldGuardHook.isRegion(event.getEntity().getLocation())) {
                
                ((Player)remover).sendMessage(ChatColor.RED + Lang.get("NoPermsBreak"));
                event.setCancelled(true);
                
            }
            
        } else if (event.getCause().equals(RemoveCause.EXPLOSION)) {
            if (MyChunkChunk.isClaimed(chunk)) {
                
                event.setCancelled(true);
                
            }
        }
         
    }
    
    @EventHandler (priority = EventPriority.HIGH)
    public void onHangingPlace(HangingPlaceEvent event) {
        
        if (event.isCancelled()) return;
        
        Player player = event.getPlayer();
        Chunk chunk = event.getBlock().getChunk();
        
        if (!MyChunkChunk.isAllowed(chunk, player, "B") && !WorldGuardHook.isRegion(event.getEntity().getLocation())) {
            
            player.sendMessage(ChatColor.RED + Lang.get("NoPermsBuild"));
            event.setCancelled(true);
            
        }

    }
    
}
