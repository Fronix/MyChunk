package me.ellbristow.mychunk.listeners;

import me.ellbristow.mychunk.MyChunk;
import me.ellbristow.mychunk.MyChunkChunk;
import me.ellbristow.mychunk.MyChunkVaultLink;
import me.ellbristow.mychunk.WorldGuardHook;
import me.ellbristow.mychunk.lang.Lang;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

public class PlayerListener implements Listener {
    
    public PlayerListener() {
    }
    
    @EventHandler (priority = EventPriority.HIGH)
    public void onPlayerDamage (EntityDamageByEntityEvent event) {
        
        if (event.isCancelled()) return;
        
        Entity entity = event.getEntity();
        
        if (entity instanceof Player) {
            
            Player hurtPlayer = (Player)entity;
            Chunk chunk = hurtPlayer.getLocation().getChunk();
            
            if (MyChunkChunk.isClaimed(chunk)) {
                
                Entity damager = event.getDamager();
                
                if (damager instanceof Player) {
                    
                    event.setCancelled(true);
                    Player naughty = (Player)damager;
                    naughty.sendMessage(ChatColor.RED + Lang.get("NoPermsPVP"));
                    
                } else if (damager instanceof Monster || damager instanceof Slime) {
                    
                    if (!MyChunkChunk.getAllowMobs(chunk)) {
                        event.setCancelled(true);
                    }
                    
                } else if (damager instanceof Projectile) {
                    
                    Entity shooter = ((Projectile) event.getDamager()).getShooter();
                    
                    if (shooter instanceof Player && entity instanceof Player) {
                        event.setCancelled(true);
                    } else if (shooter instanceof Monster && entity instanceof Player) {
                        
                        if (!MyChunkChunk.getAllowMobs(chunk)) {
                            event.setCancelled(true);
                        }
                        
                    }
                    
                }
                
            }
            
        }
        
    }
    
    @EventHandler (priority = EventPriority.HIGH)
    public void onPlayerEmptyBucket (PlayerBucketEmptyEvent event) {
        
        if (event.isCancelled()) return;
        
        Player player = event.getPlayer();
        int bucket = event.getBucket().getId();
        Block block = event.getBlockClicked();
        BlockFace face = event.getBlockFace();
        Block targetBlock;
        String flag;
        String lang;
        
        if (face.equals(BlockFace.UP) || face.equals(BlockFace.DOWN)|| face.equals(BlockFace.SELF)) {
            targetBlock = block;
        } else {
            targetBlock = block.getRelative(face);
        }
        
        Chunk chunk = targetBlock.getChunk();
        
        if (bucket == 327) {
            flag = "L";
            lang = "NoPermsLava";
        } else {
            flag = "W";
            lang = "NoPermsWater";
        }
        
        if (!MyChunkChunk.isAllowed(chunk, player, flag) && !WorldGuardHook.isRegion(block.getLocation())) {
            
            player.sendMessage(ChatColor.RED + Lang.get(lang));
            event.setCancelled(true);
            player.setItemInHand(new ItemStack(bucket));
            
        }
        
    }
    
    @EventHandler (priority = EventPriority.HIGH)
    public void onPlayerInteract (PlayerInteractEvent event) {
        
        if (event.isCancelled()) return;
        
        if (event.getAction().equals(Action.LEFT_CLICK_BLOCK) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            
            Block block = event.getClickedBlock();
            Chunk chunk = block.getChunk();
            
            if (!MyChunkChunk.isClaimed(chunk) && !MyChunk.getToggle("protectUncliamed")) return;
            
            Player player = event.getPlayer();
            if (block.getType().equals(Material.NETHERRACK) && block.getRelative(BlockFace.UP) != null && block.getRelative(BlockFace.UP).getType().equals(Material.FIRE)) {
                
                // Extinguish Fire
                
                if (!MyChunkChunk.isAllowed(chunk, player, "I")) {
                    
                    player.sendMessage(ChatColor.RED + Lang.get("NoPermsFire"));
                    event.setCancelled(true);
                        
                }
                
            } else if (block.getTypeId() == 64 || block.getTypeId() == 96 || block.getTypeId() == 107) {
                
                // Door
                
                if (!MyChunkChunk.isAllowed(chunk, player, "O")) {
                    
                    player.sendMessage(ChatColor.RED + Lang.get("NoPermsDoor"));
                    OfflinePlayer owner = Bukkit.getServer().getOfflinePlayer(MyChunkChunk.getOwner(chunk));
                    
                    if (owner.isOnline()) {
                        owner.getPlayer().sendMessage(ChatColor.GOLD + Lang.get("NoPermsDoorOwner"));
                    }
                    
                    event.setCancelled(true);
                
                }
                
            } else if (block.getTypeId() == 77) {
                
                // Button
                
                if (!MyChunkChunk.isAllowed(chunk, player, "U")) {
                    
                    player.sendMessage(ChatColor.RED + Lang.get("NoPermsButton"));
                    OfflinePlayer owner = Bukkit.getServer().getOfflinePlayer(MyChunkChunk.getOwner(chunk));
                    
                    if (owner.isOnline()) {
                        owner.getPlayer().sendMessage(ChatColor.GOLD + Lang.get("NoPermsButtonOwner"));
                    }
                    
                    event.setCancelled(true);
                    
                }
                
            } else if (block.getTypeId() == 69) {
                
                // Lever
                
                if (!MyChunkChunk.isAllowed(chunk, player, "U")) {
                    
                    player.sendMessage(ChatColor.RED + Lang.get("NoPermsLever"));
                    OfflinePlayer owner = Bukkit.getServer().getOfflinePlayer(MyChunkChunk.getOwner(chunk));
                    
                    if (owner.isOnline()) {
                        owner.getPlayer().sendMessage(ChatColor.GOLD + Lang.get("NoPermsLeverOwner"));
                    }
                    
                    event.setCancelled(true);
                    
                }
                
            } else if (block.getTypeId() == 54) {
                
                // Chest
                
                if (!MyChunkChunk.isAllowed(chunk, player, "C")) {
                    
                    player.sendMessage(ChatColor.RED + Lang.get("NoPermsChest"));
                    OfflinePlayer owner = Bukkit.getServer().getOfflinePlayer(MyChunkChunk.getOwner(chunk));
                    
                    if (owner.isOnline()) {
                        owner.getPlayer().sendMessage(ChatColor.GOLD + Lang.get("NoPermsChestOwner"));
                    }
                    
                    event.setCancelled(true);
                    
                }
                
            } else if (block.getTypeId() == 61 || block.getTypeId() == 62 || block.getTypeId() == 23 || block.getTypeId() == 117) {
                
                // Special Block
                
                if (!MyChunkChunk.isAllowed(chunk, player, "S")) {
                    
                    player.sendMessage(ChatColor.RED + Lang.get("NoPermsSpecial"));
                    OfflinePlayer owner = Bukkit.getServer().getOfflinePlayer(MyChunkChunk.getOwner(chunk));
                    
                    if (owner.isOnline()) {
                        owner.getPlayer().sendMessage(ChatColor.GOLD + Lang.get("NoPermsSpecialOwner"));
                    }
                    
                    event.setCancelled(true);
                    
                }
                
            }
            
        } else if (event.getAction().equals(Action.PHYSICAL)) {
            
            // Pressure Plates and Crop Trample
            
            Player player = event.getPlayer();
            Block block = player.getLocation().getBlock();
            Chunk chunk = block.getChunk();
            
            if (!MyChunkChunk.isClaimed(chunk) && !MyChunk.getToggle("protectUnclaimed")) return;
            
            if ((block.getType() == Material.CROPS || block.getType() ==  Material.SOIL || block.getType() ==  Material.CARROT || block.getType() ==  Material.POTATO || (block.getType() ==  Material.AIR && block.getRelative(BlockFace.DOWN).getType().equals(Material.SOIL)))) {
                event.setCancelled(true);
            } else if (!MyChunkChunk.isAllowed(chunk, player, "U")) {
                
                event.setCancelled(true);
                
            }
        }
    }
    
        @EventHandler (priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
            
        if (MyChunk.getToggle("useClaimExpiry")) {
            MyChunkChunk.refreshOwnership(event.getPlayer().getName());
        }
        
    }
    
    @EventHandler (priority = EventPriority.HIGH)
    public void onPlayerMove (PlayerMoveEvent event) {
        
        if (event.isCancelled()) return;
        
        Location fromLoc = event.getFrom();
        Location toLoc = event.getTo();
        
        if (fromLoc.getChunk() != toLoc.getChunk()) {
            
            Chunk fromChunk = fromLoc.getChunk();
            Chunk toChunk = toLoc.getChunk();
            Player player = event.getPlayer();
            String fromChunkOwner = MyChunkChunk.getOwner(fromChunk);
            String toChunkOwner = MyChunkChunk.getOwner(toChunk);
            
            if (!fromChunkOwner.equalsIgnoreCase(toChunkOwner)) {
                
                if (toChunkOwner.equals("")) {
                    player.sendMessage(ChatColor.GRAY + "~"+Lang.get("Unowned"));
                    return;
                }
                
                String forSale = "";
                
                if (MyChunk.getToggle("foundEconomy")) {
                    
                    double claimPrice = MyChunkChunk.getClaimPrice(toChunk, player);
                    
                    if (claimPrice != 0) {
                        
                        forSale = ChatColor.YELLOW + " ["+Lang.get("ChunkForSale")+ ": " + MyChunkVaultLink.getEconomy().format(claimPrice) + "]";

                    }
                }
                
                if (toChunkOwner.equalsIgnoreCase("server")) {
                    player.sendMessage(ChatColor.LIGHT_PURPLE + "~"+Lang.get("Server") + forSale);
                } else {
                    player.sendMessage(ChatColor.GOLD + "~" + toChunkOwner + forSale);
                }
                
            } else if (!toChunkOwner.equals("")) {
                
                if (MyChunk.getToggle("foundEconomy")) {

                    double claimPrice = MyChunkChunk.getClaimPrice(toChunk, player);

                    if (claimPrice == 0) {
                        return;
                    }

                    String forSale = ChatColor.YELLOW + "["+Lang.get("ChunkForSale") + ": " + MyChunkVaultLink.getEconomy().format(claimPrice) + "]";

                    player.sendMessage(forSale);
                }
                
            }
        }
    }
    
    @EventHandler (priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        
        if (MyChunk.getToggle("useClaimExpiry")) {
            MyChunkChunk.refreshOwnership(event.getPlayer().getName());
        }
        
    }
    
    @EventHandler (priority = EventPriority.HIGH)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        
        if (event.isCancelled()) return;
        
        Location fromLoc = event.getFrom();
        Location toLoc = event.getTo();
        
        if (fromLoc.getChunk() != toLoc.getChunk()) {
            
            Chunk fromChunk = fromLoc.getChunk();
            Chunk toChunk = toLoc.getChunk();
            Player player = event.getPlayer();
            String fromChunkOwner = MyChunkChunk.getOwner(fromChunk);
            String toChunkOwner = MyChunkChunk.getOwner(toChunk);
            
            if (!fromChunkOwner.equalsIgnoreCase(toChunkOwner)) {
                
                if (toChunkOwner.equals("")) {
                    player.sendMessage(ChatColor.GRAY + "~"+Lang.get("Unowned"));
                    return;
                }
                
                String forSale = "";
                
                if (MyChunk.getToggle("foundEconomy")) {
                    
                    double claimPrice = MyChunkChunk.getClaimPrice(toChunk, player);
                    
                    if (claimPrice != 0) {
                        
                        forSale = ChatColor.YELLOW + " ["+Lang.get("ChunkForSale")+ ": " + MyChunkVaultLink.getEconomy().format(claimPrice) + "]";

                    }
                }
                
                if (toChunkOwner.equalsIgnoreCase("server")) {
                    player.sendMessage(ChatColor.LIGHT_PURPLE + "~"+Lang.get("Server") + forSale);
                } else {
                    player.sendMessage(ChatColor.GOLD + "~" + toChunkOwner + forSale);
                }
                
            } else if (!toChunkOwner.equals("")) {
                
                if (MyChunk.getToggle("foundEconomy")) {

                    double claimPrice = MyChunkChunk.getClaimPrice(toChunk, player);

                    if (claimPrice == 0) {
                        return;
                    }

                    String forSale = ChatColor.YELLOW + "["+Lang.get("ChunkForSale") + ": " + MyChunkVaultLink.getEconomy().format(claimPrice) + "]";

                    player.sendMessage(forSale);
                }
                
            }
            
        }
        
    }
    
}
