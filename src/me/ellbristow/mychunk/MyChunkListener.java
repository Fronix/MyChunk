package me.ellbristow.mychunk;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

public class MyChunkListener implements Listener {
    
    public static MyChunk plugin;
    
    public MyChunkListener (MyChunk instance) {
        plugin = instance;
    }

    @EventHandler (priority = EventPriority.NORMAL)
    public void onExplosion (EntityExplodeEvent event) {
        if (!event.isCancelled()) {
            List<Block> blocks = event.blockList();
            if (blocks != null) {
                int index = 0;
                Collection<Block> saveBanks = new HashSet<Block>();
                for (Iterator<Block> it = blocks.iterator(); it.hasNext();) {
                    Block block = it.next();
                    MyChunkChunk chunk = new MyChunkChunk(block, plugin);
                    if (chunk.isClaimed()) {
                        saveBanks.add(block);
                    }
                    index++;
                }
                if (!saveBanks.isEmpty()) {
                    event.blockList().removeAll(saveBanks);
                }
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onBlockPlace (BlockPlaceEvent event) {
        if (!event.isCancelled()) {
            MyChunkChunk chunk = new MyChunkChunk(event.getBlock(), plugin);
            if (chunk.isClaimed()) {
                String owner = chunk.getOwner();
                Player player = event.getPlayer();
                if (!owner.equalsIgnoreCase(player.getName())) {
                    if (!owner.equalsIgnoreCase("server") ^ !player.hasPermission("mychunk.server.build")) {
                        player.sendMessage(ChatColor.RED + "You do not have permission to build here!");
                        event.setCancelled(true);
                    }
                }
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onBlockBreak (BlockBreakEvent event) {
        if (!event.isCancelled()) {
            MyChunkChunk chunk = new MyChunkChunk(event.getBlock(), plugin);
            if (chunk.isClaimed()) {
                String owner = chunk.getOwner();
                Player player = event.getPlayer();
                if (!owner.equalsIgnoreCase(player.getName())) {
                    if (!owner.equalsIgnoreCase("server") ^ !player.hasPermission("mychunk.server.destroy")) {
                        player.sendMessage(ChatColor.RED + "You do not have permission to break blocks here!");
                        event.setCancelled(true);
                    }
                }
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onBlockIgnite (BlockIgniteEvent event) {
        if (!event.isCancelled()) {
            MyChunkChunk chunk = new MyChunkChunk(event.getBlock(), plugin);
            if (chunk.isClaimed()) {
                String owner = chunk.getOwner();
                Player player = event.getPlayer();
                if (!owner.equalsIgnoreCase(player.getName())) {
                    if (!owner.equalsIgnoreCase("server") ^ !player.hasPermission("mychunk.server.ignite"))
                        player.sendMessage(ChatColor.RED + "FIRE! Oh phew... you're not allowed!");
                        event.setCancelled(true);
                }
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onBlockFromTo (BlockFromToEvent event) {
        if (!event.isCancelled()) {
            Block sourceBlock = event.getBlock();
            Block targetBlock = event.getToBlock();
            MyChunkChunk targetChunk = new MyChunkChunk(targetBlock, plugin);
            MyChunkChunk sourceChunk = new MyChunkChunk(sourceBlock, plugin);
            String sourceOwner = sourceChunk.getOwner();
            String targetOwner = targetChunk.getOwner();
            if (!sourceOwner.equals(targetOwner) && !targetOwner.equals("Unowned")) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onPlayerEmptyBucket (PlayerBucketEmptyEvent event) {
        if (!event.isCancelled()) {
            Block block = event.getBlockClicked();
            BlockFace face = event.getBlockFace();
            Block targetBlock = null;
            if (face.equals(BlockFace.UP) || face.equals(BlockFace.DOWN)|| face.equals(BlockFace.SELF)) {
                targetBlock = block;
            }
            MyChunkChunk chunk = new MyChunkChunk(targetBlock, plugin);
            if (chunk.isClaimed()) {
                String owner = chunk.getOwner();
                Player player = event.getPlayer();
                if (!owner.equalsIgnoreCase(player.getName()) || (owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.server.build"))) {
                    int bucket = event.getBucket().getId();
                    if (bucket == 327) {
                        player.sendMessage(ChatColor.RED + "Are you crazy!? You can't drop lava there!");
                        event.setCancelled(true);
                        player.setItemInHand(new ItemStack(327,1));
                    } else if (bucket == 326) {
                        player.sendMessage(ChatColor.RED + "Are you crazy!? You can't drop water there!");
                        event.setCancelled(true);
                        player.setItemInHand(new ItemStack(326,1));
                    }
                }
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onPlayerInteract (PlayerInteractEvent event) {
        if (!event.isCancelled()) {
            if (event.getAction().equals(Action.LEFT_CLICK_BLOCK) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                Block block = event.getClickedBlock();
                if (block.getTypeId() == 64 || block.getTypeId() == 71 || block.getTypeId() == 96 || block.getTypeId() == 107) {
                    MyChunkChunk chunk = new MyChunkChunk(block, plugin);
                    Player player = event.getPlayer();
                    String owner = chunk.getOwner();
                    if (chunk.isClaimed() && !owner.equals(player.getName())) {
                        if (!chunk.getOwner().equalsIgnoreCase("server") ^ !player.hasPermission("mychunk.server.use")) {
                            player.sendMessage(ChatColor.RED + ">KNOCK< >KNOCK< This door is locked!");
                            OfflinePlayer ownerPlayer = plugin.getServer().getOfflinePlayer(owner);
                            if (ownerPlayer.isOnline()) {
                                ownerPlayer.getPlayer().sendMessage(ChatColor.GOLD + ">KNOCK< >KNOCK< Someone is visiting your chunk!");
                            }
                            event.setCancelled(true);
                        }
                    }
                } else if (block.getTypeId() == 77) {
                    MyChunkChunk chunk = new MyChunkChunk(block, plugin);
                    Player player = event.getPlayer();
                    String owner = chunk.getOwner();
                    if (chunk.isClaimed() && !owner.equals(player.getName())) {
                        if (!chunk.getOwner().equalsIgnoreCase("server") ^ !player.hasPermission("mychunk.server.use")) {
                            player.sendMessage(ChatColor.RED + ">BUZZZ< The button tripped a silent alarm!");
                            OfflinePlayer ownerPlayer = plugin.getServer().getOfflinePlayer(owner);
                            if (ownerPlayer.isOnline()) {
                                ownerPlayer.getPlayer().sendMessage(ChatColor.GOLD + ">BUZZ< Someone pressed a button in your chunk!");
                            }
                            event.setCancelled(true);
                        }
                    }
                } else if (block.getTypeId() == 69) {
                    MyChunkChunk chunk = new MyChunkChunk(block, plugin);
                    Player player = event.getPlayer();
                    String owner = chunk.getOwner();
                    if (chunk.isClaimed() && !owner.equals(player.getName())) {
                        if (!chunk.getOwner().equalsIgnoreCase("server") ^ !player.hasPermission("mychunk.server.use")) {
                            player.sendMessage(ChatColor.RED + ">CLICK< The lever tripped a silent alarm!");
                            OfflinePlayer ownerPlayer = plugin.getServer().getOfflinePlayer(owner);
                            if (ownerPlayer.isOnline()) {
                                ownerPlayer.getPlayer().sendMessage(ChatColor.GOLD + ">CLICK< Someone touched a lever in your chunk!");
                            }
                            event.setCancelled(true);
                        }
                    }
                } 
            } else if (event.getAction().equals(Action.PHYSICAL)) {
                Player player = event.getPlayer();
                Block block = player.getLocation().getBlock();
                MyChunkChunk chunk = new MyChunkChunk(block, plugin);
                String owner = chunk.getOwner();
                if (chunk.isClaimed() && !owner.equals(player.getName())) {
                    if (!chunk.getOwner().equalsIgnoreCase("server") ^ !player.hasPermission("mychunk.server.use")) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onPlayerMove (PlayerMoveEvent event) {
        if (!event.isCancelled()) {
            Location fromLoc = event.getFrom();
            Location toLoc = event.getTo();
            if (fromLoc.getChunk() != toLoc.getChunk()) {
                MyChunkChunk fromChunk = new MyChunkChunk(fromLoc.getBlock(), plugin);
                MyChunkChunk toChunk = new MyChunkChunk(toLoc.getBlock(), plugin);
                if (!fromChunk.getOwner().equals(toChunk.getOwner())) {
                    Player player = event.getPlayer();
                    if (toChunk.getOwner().equalsIgnoreCase("server")) {
                        player.sendMessage(ChatColor.LIGHT_PURPLE + "~Server");
                    } else {
                        player.sendMessage(ChatColor.GOLD + "~" + toChunk.getOwner());
                    }
                }
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onMonsterSpawn (CreatureSpawnEvent event) {
        if (!event.isCancelled()) {
            LivingEntity mob = event.getEntity();
            if (mob instanceof Monster) {
                MyChunkChunk chunk = new MyChunkChunk(event.getLocation().getBlock(), plugin);
                if (chunk.isClaimed()) {
                    event.setCancelled(true);
                }
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onPlayerPVP (EntityDamageByEntityEvent event) {
        if (!event.isCancelled()) {
            Entity entity = event.getEntity();
            if (entity instanceof Player) {
                Player player = (Player)entity;
                MyChunkChunk chunk = new MyChunkChunk(player.getLocation().getBlock(),plugin);
                if (chunk.getOwner().equalsIgnoreCase(player.getName()) || chunk.getOwner().equalsIgnoreCase("server")) {
                    event.setCancelled(true);
                    Entity damager = event.getDamager();
                    if (damager instanceof Player) {
                        Player naughty = (Player)damager;
                        naughty.sendMessage(ChatColor.RED + "That player is protected by a magic shield!");
                    }
                }
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onSignChange (SignChangeEvent event) {
        if (!event.isCancelled()) {
            String line0 = event.getLine(0);
            if (line0.equalsIgnoreCase("[claim]")) {
            // Player attempted to claim a chunk
            // Only applies if player has rights to build in the chunk
                Player player = event.getPlayer();
                Block block = event.getBlock();
                MyChunkChunk chunk = new MyChunkChunk(block, plugin);
                boolean allowed = true;
                if (!player.hasPermission("mychunk.claim") && !player.hasPermission("mychunk.claim.server")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to claim chunks!");
                    allowed = false;
                } else if (chunk.isClaimed()) {
                    String owner = chunk.getOwner();
                    if (owner.equalsIgnoreCase(player.getName())) {
                        player.sendMessage(ChatColor.RED + "You already own this chunk!");
                    } else {
                        player.sendMessage(ChatColor.RED + "This Chunk is already owned by " + ChatColor.WHITE + owner + ChatColor.RED + "!");
                    }
                    allowed = false;
                } else if (chunk.hasNeighbours()) {
                    String[] neighbours = chunk.getNeighbours();
                    for (int i = 0; i<neighbours.length; i++) {
                        if (!neighbours[i].equalsIgnoreCase("") && !neighbours[i].equalsIgnoreCase("server") && !neighbours[i].equalsIgnoreCase("unclaimed") && !neighbours[i].equalsIgnoreCase(player.getName())) {
                            player.sendMessage(ChatColor.RED + "You cannot claim a chunk next to someone elses chunk!");
                            allowed = false;
                        }
                    }
                }
                if (allowed) {
                    String line1 = event.getLine(1);
                    if (line1.equals("") || line1.equalsIgnoreCase(player.getName())) {
                        int ownedChunks = plugin.ownedChunks(player.getName());
                        if ((ownedChunks < plugin.maxChunks) || player.hasPermission("mychunk.claim.unlimited") || plugin.maxChunks == 0) {
                            chunk.claim(player.getName());
                            player.sendMessage(ChatColor.GOLD + "Chunk claimed!");
                            if (plugin.foundEconomy && plugin.chunkPrice != 0 && !player.hasPermission("mychunk.free")) {
                                plugin.vault.economy.withdrawPlayer(player.getName(), plugin.chunkPrice);
                                player.sendMessage(plugin.vault.economy.format(plugin.chunkPrice) + ChatColor.GOLD + " was deducted from your account");
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "You already own " + ownedChunks + " chunks! (Max " + plugin.maxChunks + ")");
                        }
                    } else {
                        String correctName = "";
                        allowed = true;
                        if (line1.equalsIgnoreCase("server")) {
                            if (!player.hasPermission("mychunk.claim.server")) {
                                player.sendMessage(ChatColor.RED + "You do not have permission to claim chunks for the server!");
                                allowed = false;
                            } else {
                                correctName = "Server";
                            }
                        } else {
                            if (player.hasPermission("mychunk.claim.others")) {
                                OfflinePlayer target = plugin.getServer().getOfflinePlayer(line1);
                                if (!target.hasPlayedBefore()) {
                                    player.sendMessage(ChatColor.RED + "Player " + ChatColor.WHITE + line1 + ChatColor.RED + " not found!");
                                    allowed = false;
                                } else {
                                    correctName = target.getName();
                                }
                            } else {
                                player.sendMessage(ChatColor.RED + "You do not have permission to claim chunks for other players!");
                                allowed = false;
                            }
                        }
                        if (allowed) {
                            int ownedChunks = plugin.ownedChunks(player.getName());
                            if ((ownedChunks < plugin.maxChunks) || player.hasPermission("mychunk.claim.others.unlimited") || plugin.maxChunks == 0 || (correctName.equalsIgnoreCase("server") && player.hasPermission("mychunk.server.claim"))) {
                                chunk.claim(correctName);
                                player.sendMessage(ChatColor.GOLD + "Chunk claimed for " + ChatColor.WHITE + correctName + ChatColor.GOLD + "!");
                                if (plugin.foundEconomy && plugin.chunkPrice != 0 && !correctName.equalsIgnoreCase("server")) {
                                    plugin.vault.economy.withdrawPlayer(player.getName(), plugin.chunkPrice);
                                    player.sendMessage(plugin.vault.economy.format(plugin.chunkPrice) + ChatColor.GOLD + " was deducted from your account");
                                }
                            }
                        }
                    }
                }
                event.setCancelled(true);
                breakSign(block);
            } else if (line0.equalsIgnoreCase("[unclaim]")) {
            // Player attempted to unclaim a chunk
            // Only applies if player has rights to build in the chunk
                Player player = event.getPlayer();
                Block block = event.getBlock();
                MyChunkChunk chunk = new MyChunkChunk(block, plugin);
                String owner = chunk.getOwner();
                boolean allowed =true;
                if (!chunk.isClaimed()) {
                    player.sendMessage(ChatColor.RED + "This chunk is not owned!");
                    allowed = false;
                } else if (!owner.equalsIgnoreCase(player.getName())) {
                    if (owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.unclaim.server")) {
                        player.sendMessage(ChatColor.RED + "You do not have permission to claim chunks for the server!");
                        allowed = false;
                    } else if (!owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.unclaim.others")) {
                        player.sendMessage(ChatColor.RED + "You do not have permission to claim chunks for " + ChatColor.WHITE + owner + ChatColor.RED + "!");
                        allowed = false;
                    }
                }
                if (allowed) {
                    chunk.unclaim();
                    if (owner.equalsIgnoreCase(player.getName())) {
                        player.sendMessage(ChatColor.GOLD + "Chunk unclaimed!");
                    } else {
                        player.sendMessage(ChatColor.GOLD + "Chunk unclaimed for " + ChatColor.WHITE + owner + ChatColor.RED + "!");
                    }
                    if (plugin.unclaimRefund && !player.hasPermission("mychunk.free")) {
                        plugin.vault.economy.depositPlayer(player.getName(), plugin.chunkPrice);
                    }
                }
                event.setCancelled(true);
                breakSign(block);
            } else if (line0.equalsIgnoreCase("[owner]")) {
            // Player requested chunk's Owner info
            // Only applies if player has rights to build in the chunk
                Player player = event.getPlayer();
                Block block = event.getBlock();
                MyChunkChunk chunk = new MyChunkChunk(block, plugin);
                if (chunk.isClaimed()) {
                    String owner = chunk.getOwner();
                    if (owner.equalsIgnoreCase(player.getName())) {
                        player.sendMessage(ChatColor.GOLD + "You own this chunk!");
                    } else {
                        player.sendMessage(ChatColor.GOLD + "This Chunk is owned by " + ChatColor.WHITE + owner + ChatColor.GOLD + "!");
                    }
                } else {
                    player.sendMessage(ChatColor.GOLD + "This chunk is " + ChatColor.WHITE + "Unowned" + ChatColor.GOLD + "!");
                }
                event.setCancelled(true);
                breakSign(block);
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    private void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!event.isCancelled()) {
            MyChunkChunk fromChunk = new MyChunkChunk(event.getFrom().getBlock(), plugin);
            MyChunkChunk toChunk = new MyChunkChunk(event.getTo().getBlock(), plugin);
            if (!fromChunk.getOwner().equals(toChunk.getOwner())) {
                if (toChunk.getOwner().equalsIgnoreCase("server")) {
                    event.getPlayer().sendMessage(ChatColor.LIGHT_PURPLE + "~" + toChunk.getOwner());
                } else {
                    event.getPlayer().sendMessage(ChatColor.GOLD + "~" + toChunk.getOwner());
                }
            }
        }
    }
    
    private void breakSign(Block block) {
        if (block.getTypeId() == 63 || block.getTypeId() == 68) {
            block.setTypeId(0);
            block.getWorld().dropItem(block.getLocation(), new ItemStack(323,1));
        }
    }
}
