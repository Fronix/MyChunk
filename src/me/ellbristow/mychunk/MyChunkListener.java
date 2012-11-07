package me.ellbristow.mychunk;

import java.util.*;
import org.bukkit.*;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

public class MyChunkListener implements Listener {
    
    public static MyChunk plugin;
    
    public MyChunkListener (MyChunk instance) {
        plugin = instance;
    }

    @EventHandler (priority = EventPriority.NORMAL)
    public void onExplosion (EntityExplodeEvent event) {
        if (event.isCancelled())
            return;
        List<Block> blocks = event.blockList();
        if (blocks != null) {
            int index = 0;
            Collection<Block> saveBanks = new HashSet<Block>();
            for (Iterator<Block> it = blocks.iterator(); it.hasNext();) {
                Block block = it.next();
                MyChunkChunk chunk = getChunk(block);
                if (chunk != null || plugin.protectUnclaimed) {
                    saveBanks.add(block);
                }
                index++;
            }
            if (!saveBanks.isEmpty()) {
                event.blockList().removeAll(saveBanks);
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onBlockPlace (BlockPlaceEvent event) {
        if (event.isCancelled())
            return;
        Block block = event.getBlock();
        MyChunkChunk chunk = getChunk(block);
        if (chunk != null || plugin.protectUnclaimed) {
            Player player = event.getPlayer();
            if (chunk != null) {
                String owner = chunk.getOwner();
                if (!owner.equalsIgnoreCase(player.getName()) && !chunk.isAllowed(player.getName(), "B") && !WorldGuardHook.isRegion(event.getBlock().getLocation())) {
                    if ((!owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.override")) || (owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.server.build"))) {
                        player.sendMessage(ChatColor.RED + plugin.lang.get("NoPermsBuild"));
                        if (block.getTypeId() != 63 && block.getTypeId() != 68) {
                            event.setCancelled(true);
                        }
                    }
                }
            } else {
                player.sendMessage(ChatColor.RED + plugin.lang.get("NoPermsBuild"));
                if (block.getTypeId() != 63 && block.getTypeId() != 68) {
                    event.setCancelled(true);
                }
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onBlockBreak (BlockBreakEvent event) {
        if (event.isCancelled())
            return;
        MyChunkChunk chunk = getChunk(event.getBlock());
        if (chunk != null || plugin.protectUnclaimed) {
            Player player = event.getPlayer();
            if (chunk != null) {
                String owner = chunk.getOwner();
                if (!owner.equalsIgnoreCase(player.getName())&& !chunk.isAllowed(player.getName(), "D") && !WorldGuardHook.isRegion(event.getBlock().getLocation())) {
                    if ((!owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.override")) || (owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.server.destroy"))) {
                        player.sendMessage(ChatColor.RED + plugin.lang.get("NoPermsBreak"));
                        event.setCancelled(true);
                    }
                }
            } else {
                player.sendMessage(ChatColor.RED + plugin.lang.get("NoPermsBreak"));
                event.setCancelled(true);
            }
        } else if (event.getBlock().getState() instanceof Sign) {
            Sign sign = (Sign)event.getBlock().getState();
            if (sign.getLine(0).equalsIgnoreCase("[ClaimArea]") && !sign.getLine(1).equalsIgnoreCase(event.getPlayer().getName())) {
                event.getPlayer().sendMessage(ChatColor.RED + plugin.lang.get("CannotDestroyClaim"));
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onBlockIgnite (BlockIgniteEvent event) {
        if (event.isCancelled())
            return;
        MyChunkChunk chunk = getChunk(event.getBlock());
        if (chunk!= null || plugin.protectUnclaimed) {
            Player player = event.getPlayer();
            if (chunk != null) {
                String owner = chunk.getOwner();
                if (event.getCause() == IgniteCause.FLINT_AND_STEEL) {
                    if (!owner.equalsIgnoreCase(player.getName()) && !chunk.isAllowed(player.getName(), "I")) {
                        if ((!owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.override")) || (owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.server.ignite"))) {
                            player.sendMessage(ChatColor.RED + plugin.lang.get("NoPermsFire"));
                            event.setCancelled(true);
                        }
                    }
                } else if (event.getCause() == IgniteCause.LAVA || event.getCause() == IgniteCause.SPREAD) {
                    event.setCancelled(true);
                }
            } else {
                player.sendMessage(ChatColor.RED + plugin.lang.get("NoPermsFire"));
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onZombieDoorEvent (EntityBreakDoorEvent event) {
        if (event.isCancelled())
            return;
        if (event.getBlock().getTypeId() == 64 && event.getEntityType().equals(EntityType.ZOMBIE)) {
            MyChunkChunk chunk = getChunk(event.getBlock());
            if (chunk != null || plugin.protectUnclaimed) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onPlayerEmptyBucket (PlayerBucketEmptyEvent event) {
        if (event.isCancelled())
            return;
        Block block = event.getBlockClicked();
        BlockFace face = event.getBlockFace();
        Block targetBlock;
        if (face.equals(BlockFace.UP) || face.equals(BlockFace.DOWN)|| face.equals(BlockFace.SELF)) {
            targetBlock = block;
        } else {
            targetBlock = block.getRelative(face);
        }
        MyChunkChunk chunk = getChunk(targetBlock);
        if (chunk != null || plugin.protectUnclaimed) {
            Player player = event.getPlayer();
            int bucket = event.getBucket().getId();
            if (chunk != null) {
                String owner = chunk.getOwner();
                if ((!owner.equalsIgnoreCase(player.getName()) && !player.hasPermission("mychunk.override")) || (owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.server.build"))) {
                    if (bucket == 327 && !chunk.isAllowed(player.getName(), "L")) {
                        player.sendMessage(ChatColor.RED + plugin.lang.get("NoPermsLava"));
                        event.setCancelled(true);
                        player.setItemInHand(new ItemStack(327,1));
                    } else if (bucket == 326 && !chunk.isAllowed(player.getName(), "W")) {
                        player.sendMessage(ChatColor.RED + plugin.lang.get("NoPermsWater"));
                        event.setCancelled(true);
                        player.setItemInHand(new ItemStack(326,1));
                    }
                }
            } else {
                if (bucket == 327 && !chunk.isAllowed(player.getName(), "L")) {
                    player.sendMessage(ChatColor.RED + plugin.lang.get("NoPermsLava"));
                    event.setCancelled(true);
                    player.setItemInHand(new ItemStack(327,1));
                } else if (bucket == 326 && !chunk.isAllowed(player.getName(), "W")) {
                    player.sendMessage(ChatColor.RED + plugin.lang.get("NoPermsWater"));
                    event.setCancelled(true);
                    player.setItemInHand(new ItemStack(326,1));
                }
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onPlayerInteract (PlayerInteractEvent event) {
        if (event.isCancelled())
            return;
        if (event.getAction().equals(Action.LEFT_CLICK_BLOCK) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            Block block = event.getClickedBlock();
            MyChunkChunk chunk = getChunk(block);
            if (chunk == null) return;
            Player player = event.getPlayer();
            String owner = chunk.getOwner();
            if (block.getTypeId() == 64 || block.getTypeId() == 96 || block.getTypeId() == 107) {
                if (!owner.equals(player.getName()) && !chunk.isAllowed(player.getName(), "O")) {
                    if ((!owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.override")) || (owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.server.doors"))) {
                        player.sendMessage(ChatColor.RED + plugin.lang.get("NoPermsDoor"));
                        OfflinePlayer ownerPlayer = plugin.getServer().getOfflinePlayer(owner);
                        if (ownerPlayer.isOnline()) {
                            ownerPlayer.getPlayer().sendMessage(ChatColor.GOLD + plugin.lang.get("NoPermsDoorOwner"));
                        }
                        event.setCancelled(true);
                    }
                }
            } else if (block.getTypeId() == 77) {
                if (!owner.equals(player.getName()) && !chunk.isAllowed(player.getName(), "U")) {
                    if ((!owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.override")) || (owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.server.use"))) {
                        player.sendMessage(ChatColor.RED + plugin.lang.get("NoPermsButton"));
                        OfflinePlayer ownerPlayer = plugin.getServer().getOfflinePlayer(owner);
                        if (ownerPlayer.isOnline()) {
                            ownerPlayer.getPlayer().sendMessage(ChatColor.GOLD + plugin.lang.get("NoPermsButtonOwner"));
                        }
                        event.setCancelled(true);
                    }
                }
            } else if (block.getTypeId() == 69) {
                if (!owner.equals(player.getName()) && !chunk.isAllowed(player.getName(), "U")) {
                    if ((!owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.override")) || (owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.server.use"))) {
                        player.sendMessage(ChatColor.RED + plugin.lang.get("NoPermsLever"));
                        OfflinePlayer ownerPlayer = plugin.getServer().getOfflinePlayer(owner);
                        if (ownerPlayer.isOnline()) {
                            ownerPlayer.getPlayer().sendMessage(ChatColor.GOLD + plugin.lang.get("NoPermsLeverOwner"));
                        }
                        event.setCancelled(true);
                    }
                }
            } else if (block.getTypeId() == 54) {
                if (!owner.equals(player.getName()) && !chunk.isAllowed(player.getName(), "C")) {
                    if ((!owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.override")) || (owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.server.chests"))) {
                        player.sendMessage(ChatColor.RED + plugin.lang.get("NoPermsChest"));
                        OfflinePlayer ownerPlayer = plugin.getServer().getOfflinePlayer(owner);
                        if (ownerPlayer.isOnline()) {
                            ownerPlayer.getPlayer().sendMessage(ChatColor.GOLD + plugin.lang.get("NoPermsChestOwner"));
                        }
                        event.setCancelled(true);
                    }
                }
            } else if (block.getTypeId() == 61 || block.getTypeId() == 62 || block.getTypeId() == 23 || block.getTypeId() == 117) {
                if (!owner.equals(player.getName()) && !chunk.isAllowed(player.getName(), "S")) {
                    if ((!owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.override")) || (owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.server.special"))) {
                        player.sendMessage(ChatColor.RED + plugin.lang.get("NoPermsSpecial"));
                        OfflinePlayer ownerPlayer = plugin.getServer().getOfflinePlayer(owner);
                        if (ownerPlayer.isOnline()) {
                            ownerPlayer.getPlayer().sendMessage(ChatColor.GOLD + plugin.lang.get("NoPermsSpecialOwner"));
                        }
                        event.setCancelled(true);
                    }
                }
            }
        } else if (event.getAction().equals(Action.PHYSICAL)) {
            Player player = event.getPlayer();
            Block block = player.getLocation().getBlock();
            MyChunkChunk chunk = getChunk(block);
            if (chunk == null) return;
            String owner = chunk.getOwner();
            if (chunk.isClaimed() && !owner.equals(player.getName()) && !chunk.isAllowed(player.getName(), "U")) {
                if ((!owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.override")) || (owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.server.use"))) {
                    event.setCancelled(true);
                }
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onPlayerMove (PlayerMoveEvent event) {
        if (event.isCancelled())
            return;
        Location fromLoc = event.getFrom();
        Location toLoc = event.getTo();
        if (fromLoc.getChunk() != toLoc.getChunk()) {
            MyChunkChunk fromChunk = getChunk(fromLoc.getBlock());
            MyChunkChunk toChunk = getChunk(toLoc.getBlock());
            Player player = event.getPlayer();
            if ((fromChunk != null || toChunk != null) && ((fromChunk == null && toChunk != null) || (toChunk == null && fromChunk != null) || !fromChunk.getOwner().equals(toChunk.getOwner()))) {
                String forSale = "";
                if (isForSale(toChunk)) {
                    forSale = ChatColor.YELLOW + " ["+plugin.lang.get("ChunkForSale");
                    if (plugin.foundEconomy && toChunk.getClaimPrice() != 0) {
                        if (plugin.ownedChunkCount(player.getName()) < plugin.maxChunks || !plugin.allowOverbuy) {
                            forSale += ": " + plugin.vault.economy.format(toChunk.getClaimPrice());
                        } else if (plugin.allowOverbuy && plugin.ownedChunkCount(player.getName()) >= plugin.maxChunks) {
                            forSale += ": " + plugin.vault.economy.format(toChunk.getOverbuyPrice());
                        }
                    }
                    forSale += "]";
                }
                if (toChunk == null) {
                    player.sendMessage(ChatColor.GRAY + "~"+plugin.lang.get("Unowned"));
                } else if (toChunk.getOwner().equalsIgnoreCase("server")) {
                    player.sendMessage(ChatColor.LIGHT_PURPLE + "~"+plugin.lang.get("Server") + forSale);
                } else {
                    player.sendMessage(ChatColor.GOLD + "~" + toChunk.getOwner() + forSale);
                }
            } else if (isForSale(toChunk)) {
                String forSale = ChatColor.YELLOW + "["+plugin.lang.get("ChunkForSale");
                if (plugin.foundEconomy && toChunk.getClaimPrice() != 0) {
                    if (plugin.ownedChunkCount(player.getName()) < plugin.maxChunks || !plugin.allowOverbuy || (plugin.allowOverbuy && player.hasPermission("mychunk.free"))) {
                        forSale += ": " + plugin.vault.economy.format(toChunk.getClaimPrice());
                    } else if (plugin.allowOverbuy && plugin.ownedChunkCount(player.getName()) >= plugin.maxChunks) {
                        forSale += ": " + plugin.vault.economy.format(toChunk.getOverbuyPrice());
                    }
                }
                forSale += "]";
                player.sendMessage(forSale);
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onMonsterSpawn (CreatureSpawnEvent event) {
        if (event.isCancelled())
            return;
        LivingEntity mob = event.getEntity();
        if (mob instanceof Monster) {
            MyChunkChunk chunk = getChunk(event.getLocation().getBlock());
            if (chunk != null && !chunk.getAllowMobs()) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onPlayerPVP (EntityDamageByEntityEvent event) {
        if (event.isCancelled())
            return;
        Entity entity = event.getEntity();
        if (entity instanceof Player) {
            Player player = (Player)entity;
            MyChunkChunk chunk = getChunk(player.getLocation().getBlock());
            if (chunk != null) {
                Entity damager = event.getDamager();
                if (damager instanceof Player) {
                    event.setCancelled(true);
                    Player naughty = (Player)damager;
                    naughty.sendMessage(ChatColor.RED + plugin.lang.get("NoPermsPVP"));
                } else if (damager instanceof Monster) {
                    if (!chunk.getAllowMobs()) {
                        event.setCancelled(true);
                    }
                } else if (damager instanceof Arrow) {
                    Entity shooter = ((Arrow) event.getDamager()).getShooter();
                    if (shooter instanceof Player && entity instanceof Player) {
                        event.setCancelled(true);
                    } else if (shooter instanceof Monster && entity instanceof Player) {
                        if (chunk.isClaimed() && !chunk.getAllowMobs()) {
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onSignChange (SignChangeEvent event) {
        if (event.isCancelled())
            return;
        String line0 = event.getLine(0);
        String line1 = event.getLine(1);
        if (line0.equalsIgnoreCase("[claim]")) {
        // Player attempted to claim a chunk
            Player player = event.getPlayer();
            Block block = event.getBlock();
            MyChunkChunk chunk = getChunk(block);
            boolean allowed = true;
            if (!player.hasPermission("mychunk.claim") && !player.hasPermission("mychunk.claim.server")) {
                player.sendMessage(ChatColor.RED + plugin.lang.get("NoPermsClaim"));
                allowed = false;
            } else if (chunk != null) {
                String owner = chunk.getOwner();
                if (owner.equalsIgnoreCase(player.getName())) {
                    player.sendMessage(ChatColor.RED + plugin.lang.get("AlreadyOwner"));
                    allowed = false;
                } else if (!chunk.isForSale()) {
                    player.sendMessage(ChatColor.RED + plugin.lang.get("AlreadyOwned")+" " + ChatColor.WHITE + owner + ChatColor.RED + "!");
                    allowed = false;
                } else if (chunk.isForSale() && !player.hasPermission("mychunk.buy")) {
                    player.sendMessage(ChatColor.RED + plugin.lang.get("NoPermsBuyOwned"));
                    allowed = false;
                }
            } else if (plugin.allowNeighbours == false && hasNeighbours(chunk)&& !isForSale(chunk)) {
                String[] neighbours = chunk.getNeighbours();
                for (int i = 0; i<neighbours.length; i++) {
                    if (!neighbours[i].equalsIgnoreCase("") && !neighbours[i].equalsIgnoreCase("server") && !neighbours[i].equalsIgnoreCase("unowned")) {
                        if (!neighbours[i].equalsIgnoreCase(player.getName()) && line1.equalsIgnoreCase("")) {
                            player.sendMessage(ChatColor.RED + plugin.lang.get("NoNeighbours"));
                            allowed = false;
                        } else if (!line1.equals("") && !neighbours[i].equalsIgnoreCase(line1) && !line1.equalsIgnoreCase("server")) {
                            player.sendMessage(ChatColor.RED + plugin.lang.get("NoNeighbours"));
                            allowed = false;
                        }
                    }
                }
            } else if (!plugin.allowNether && plugin.getServer().getWorld(chunk.getWorldName()).getEnvironment().equals(Environment.NETHER)) {
                player.sendMessage(ChatColor.RED + plugin.lang.get("NoPermsNether"));
                allowed = false;
            } else if (!plugin.allowEnd && plugin.getServer().getWorld(chunk.getWorldName()).getEnvironment().equals(Environment.THE_END)) {
                player.sendMessage(ChatColor.RED + plugin.lang.get("NoPermsEnd"));
                allowed = false;
            }
            int playerMax = plugin.getMaxChunks(player);
            if (plugin.foundEconomy && getClaimPrice(chunk) != 0 && !player.hasPermission("mychunk.free") && (playerMax == 0 || plugin.ownedChunkCount(player.getName()) < playerMax) && plugin.vault.economy.getBalance(player.getName()) < getClaimPrice(chunk)) {
                player.sendMessage(ChatColor.RED + plugin.lang.get("CantAfford")+" ("+plugin.lang.get("Price")+": " + ChatColor.WHITE + plugin.vault.economy.format(getClaimPrice(chunk)) + ChatColor.RED + ")!");
                allowed = false;
            } else if (plugin.foundEconomy && playerMax != 0 && plugin.ownedChunkCount(player.getName()) >= playerMax && !player.hasPermission("mychunk.free")) {
                if (plugin.allowOverbuy && player.hasPermission("mychunk.claim.overbuy") && plugin.vault.economy.getBalance(player.getName()) < getOverbuyPrice(chunk)) {
                    player.sendMessage(ChatColor.RED + plugin.lang.get("CantAfford")+" ("+plugin.lang.get("Price")+": " + ChatColor.WHITE + plugin.vault.economy.format(chunk.getOverbuyPrice()) + ChatColor.RED + ")!");
                    allowed = false;
                }
            }
            if (allowed) {
                if (line1.equals("") || line1.equalsIgnoreCase(player.getName())) {
                    int ownedChunks = plugin.ownedChunkCount(player.getName());
                    if ((ownedChunks < playerMax || (plugin.allowOverbuy && player.hasPermission("mychunk.claim.overbuy"))) || player.hasPermission("mychunk.claim.unlimited") || playerMax == 0) {
                        if (plugin.foundEconomy && getClaimPrice(chunk) != 0 && !player.hasPermission("mychunk.free") && (playerMax == 0 || plugin.ownedChunkCount(player.getName()) < playerMax)) {
                            plugin.vault.economy.withdrawPlayer(player.getName(), getClaimPrice(chunk));
                            player.sendMessage(plugin.vault.economy.format(getClaimPrice(chunk)) + ChatColor.GOLD + " "+plugin.lang.get("AmountDeducted"));
                        } else if (plugin.foundEconomy && plugin.allowOverbuy && plugin.ownedChunkCount(player.getName()) >= playerMax && !player.hasPermission("mychunk.free")) {
                            double price;
                            if (plugin.overbuyP2P) {
                                price = getOverbuyPrice(chunk);
                            } else {
                                price = getClaimPrice(chunk);
                            }
                            plugin.vault.economy.withdrawPlayer(player.getName(), price);
                            player.sendMessage(plugin.vault.economy.format(price) + ChatColor.GOLD + " "+plugin.lang.get("AmountDeducted"));
                        }
                        if (plugin.foundEconomy && isForSale(chunk)) {
                            plugin.vault.economy.depositPlayer(chunk.getOwner(), getClaimPrice(chunk));
                            OfflinePlayer oldOwner = plugin.getServer().getOfflinePlayer(chunk.getOwner());
                            if (oldOwner.isOnline()) {
                                oldOwner.getPlayer().sendMessage(player.getName() + ChatColor.GOLD + " "+plugin.lang.get("BoughtFor")+" " + ChatColor.WHITE + plugin.vault.economy.format(chunk.getClaimPrice()) + ChatColor.GOLD + "!");
                            }
                        }
                        if (chunk == null) {
                            chunk = new MyChunkChunk(block,plugin);
                        }
                        chunk.claim(player.getName());
                        plugin.chunks.put(chunk.getWorldName()+"_"+chunk.getX()+"_"+chunk.getZ(), chunk);
                        player.sendMessage(ChatColor.GOLD + plugin.lang.get("ChunkClaimed"));
                    } else {
                        player.sendMessage(ChatColor.RED + plugin.lang.get("AlreadyOwn")+" " + ownedChunks + " "+plugin.lang.get("Chunks")+"! (Max " + playerMax + ")");
                    }
                } else {
                    String correctName = "";
                    allowed = true;
                    if (line1.equalsIgnoreCase("server")) {
                        if (!player.hasPermission("mychunk.claim.server")) {
                            player.sendMessage(ChatColor.RED + plugin.lang.get("NoPermsClaimServer"));
                            allowed = false;
                        } else {
                            correctName = "Server";
                        }
                    } else {
                        if (player.hasPermission("mychunk.claim.others")) {
                            OfflinePlayer target = plugin.getServer().getOfflinePlayer(line1);
                            if (!target.hasPlayedBefore()) {
                                player.sendMessage(ChatColor.RED + plugin.lang.get("Player")+" " + ChatColor.WHITE + line1 + ChatColor.RED + " "+plugin.lang.get("NotFound")+"!");
                                allowed = false;
                            } else {
                                correctName = target.getName();
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + plugin.lang.get("NoPermsClaimOther"));
                            allowed = false;
                        }
                    }
                    if (allowed) {
                        int ownedChunks = plugin.ownedChunkCount(player.getName());
                        if ((ownedChunks < plugin.maxChunks) || player.hasPermission("mychunk.claim.others.unlimited") || plugin.maxChunks == 0 || (correctName.equalsIgnoreCase("server") && player.hasPermission("mychunk.server.claim"))) {
                            if (chunk == null) {
                                chunk = new MyChunkChunk(block,plugin);
                            }
                            chunk.claim(correctName);
                            plugin.chunks.put(chunk.getWorldName()+"_"+chunk.getX()+"_"+chunk.getZ(), chunk);
                            player.sendMessage(ChatColor.GOLD + plugin.lang.get("ChunkClaimedFor")+" " + ChatColor.WHITE + correctName + ChatColor.GOLD + "!");
                            if (plugin.foundEconomy && plugin.chunkPrice != 0 && !correctName.equalsIgnoreCase("server")) {
                                plugin.vault.economy.withdrawPlayer(player.getName(), plugin.chunkPrice);
                                player.sendMessage(plugin.vault.economy.format(plugin.chunkPrice) + ChatColor.GOLD + " "+plugin.lang.get("AmountDeducted"));
                            }
                        }
                    }
                }
            }
            event.setCancelled(true);
            breakSign(block);
        } else if (line0.equalsIgnoreCase("[ClaimArea]")) {
            Player player = event.getPlayer();
            Block block = event.getBlock();
            boolean allowed = true;
            if (!player.hasPermission("mychunk.claim")) {
                player.sendMessage(ChatColor.RED + plugin.lang.get("NoPermsClaim"));
                allowed = false;
            } else if (!player.hasPermission("mychunk.claim.area")) {
                player.sendMessage(ChatColor.RED + plugin.lang.get("NoPermsClaimArea"));
                allowed = false;
            } else if (!plugin.allowNether && block.getWorld().getEnvironment().equals(Environment.NETHER)) {
                player.sendMessage(ChatColor.RED + plugin.lang.get("NoPermsNether"));
                allowed = false;
            } else if (!plugin.allowEnd && block.getWorld().getEnvironment().equals(Environment.THE_END)) {
                player.sendMessage(ChatColor.RED + plugin.lang.get("NoPermsEnd"));
                allowed = false;
            }
            if (allowed) {
                String correctName;
                if (line1.isEmpty() || line1.equalsIgnoreCase(player.getName())) {
                    correctName = player.getName();
                } else {
                    if (line1.equalsIgnoreCase("server")) {
                        if (!player.hasPermission("mychunk.claim.server")) {
                            player.sendMessage(ChatColor.RED + plugin.lang.get("NoPermsClaimServer"));
                            event.setCancelled(true);
                            breakSign(block);
                            return;
                        } else {
                            correctName = "Server";
                        }
                    } else {
                        if (player.hasPermission("mychunk.claim.others")) {
                            OfflinePlayer target = plugin.getServer().getOfflinePlayer(line1);
                            if (!target.hasPlayedBefore()) {
                                player.sendMessage(ChatColor.RED + plugin.lang.get("Player")+" " + ChatColor.WHITE + line1 + ChatColor.RED + " "+plugin.lang.get("NotFound")+"!");
                                event.setCancelled(true);
                                breakSign(block);
                                return;
                            } else {
                                correctName = target.getName();
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + plugin.lang.get("NoPermsClaimOther"));
                            event.setCancelled(true);
                            breakSign(block);
                            return;
                        }
                    }
                }
                event.setLine(1, correctName);
                if (event.getLine(2).equalsIgnoreCase("cancel")) {
                    plugin.pendingAreas.remove(correctName);
                    player.sendMessage(ChatColor.RED + plugin.lang.get("ClaimAreaCancelled"));
                    event.setCancelled(true);
                    breakSign(block);
                    return;
                }
                if (!plugin.pendingAreas.containsKey(correctName)) {
                    plugin.pendingAreas.put(correctName, event.getBlock());
                    player.sendMessage(ChatColor.GOLD + plugin.lang.get("StartClaimArea1"));
                    player.sendMessage(ChatColor.GOLD + plugin.lang.get("StartClaimArea2"));
                } else {
                    Block startBlock = plugin.pendingAreas.get(correctName);
                    if (startBlock.getWorld() != block.getWorld()) {
                        player.sendMessage(ChatColor.RED + plugin.lang.get("ClaimAreaWorldError"));
                        event.setCancelled(true);
                        breakSign(block);
                        return;
                    }
                    Chunk startChunk = startBlock.getChunk();
                    plugin.pendingAreas.remove(correctName);
                    Chunk endChunk = block.getChunk();
                    int startX;
                    int startZ;
                    int endX;
                    int endZ;
                    if (startChunk.getX() <= endChunk.getX()) {
                        startX = startChunk.getX();
                        endX = endChunk.getX();
                    } else {
                        startX = endChunk.getX();
                        endX = startChunk.getX();
                    }
                    if (startChunk.getZ() <= endChunk.getZ()) {
                        startZ = startChunk.getZ();
                        endZ = endChunk.getZ();
                    } else {
                        startZ = endChunk.getZ();
                        endZ = startChunk.getZ();
                    }
                    boolean foundClaimed = false;
                    boolean foundNeighbour = false;
                    List<Chunk> foundChunks = new ArrayList<Chunk>();
                    int chunkCount = 0;
                    for (int x = startX; x <= endX; x++) {
                        for (int z = startZ; z <= endZ; z++) {
                            if (chunkCount < 64) {
                                Chunk thisChunk = block.getWorld().getChunkAt(x, z);
                                MyChunkChunk myChunk = getChunk(block.getWorld().getName(), x, z);
                                if (myChunk != null) {
                                    if (!myChunk.getOwner().equalsIgnoreCase(correctName)) {
                                        foundClaimed = true;
                                    } else if (myChunk.hasNeighbours()) {
                                        foundNeighbour = true;
                                    }
                                } else if (hasNeighbours(thisChunk)) {
                                    foundNeighbour = true;
                                } else {
                                    foundChunks.add(thisChunk);
                                    chunkCount++;
                                }
                            } else {
                                player.sendMessage(ChatColor.RED + plugin.lang.get("AreaTooBig"));
                                event.setCancelled(true);
                                breakSign(block);
                                return;
                            }
                        }
                    }
                    if (foundClaimed) {
                        player.sendMessage(ChatColor.RED + plugin.lang.get("FoundClaimedInArea"));
                        event.setCancelled(true);
                        breakSign(block);
                        return;
                    }
                    if (foundNeighbour && !plugin.allowNeighbours) {
                        player.sendMessage(ChatColor.RED + plugin.lang.get("FoundNeighboursInArea"));
                        event.setCancelled(true);
                        breakSign(block);
                        return;
                    }
                    int claimed = plugin.ownedChunkCount(correctName);
                    int max = plugin.getMaxChunks(plugin.getServer().getOfflinePlayer(correctName).getPlayer());
                    if (max != 0 && (!plugin.allowOverbuy || !player.hasPermission("mychunk.claim.overbuy")) && max - claimed < foundChunks.size()) {
                        player.sendMessage(ChatColor.RED + (correctName.equalsIgnoreCase(player.getName())?"You":correctName) + plugin.lang.get("ClaimAreaTooLarge"));
                        player.sendMessage(ChatColor.RED + plugin.lang.get("ChunksOwned")+": " + ChatColor.WHITE + claimed);
                        player.sendMessage(ChatColor.RED + plugin.lang.get("ChunkMax")+": " + ChatColor.WHITE + max);
                        player.sendMessage(ChatColor.RED + plugin.lang.get("ChunksInArea") + ": " + chunkCount);
                        event.setCancelled(true);
                        breakSign(block);
                        return;
                    }
                    int allowance = max - claimed;
                    if (allowance < 0) allowance = 0;
                    if (plugin.foundEconomy) {
                        double areaPrice = 0;
                        for (Chunk chunk : foundChunks) {
                            MyChunkChunk myChunk = getChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
                            if (allowance > 0) {
                                areaPrice += getClaimPrice(myChunk);
                                allowance--;
                            } else {
                                areaPrice += getOverbuyPrice(myChunk);
                            }
                        }
                        if (plugin.vault.economy.getBalance(player.getName()) < areaPrice) {
                            player.sendMessage(ChatColor.RED + plugin.lang.get("CantAffordClaimArea"));
                            player.sendMessage(ChatColor.RED + plugin.lang.get("Price")+": " + ChatColor.WHITE + plugin.vault.economy.format(areaPrice));
                            event.setCancelled(true);
                            breakSign(block);
                            return;
                        }
                        plugin.vault.economy.withdrawPlayer(player.getName(), areaPrice);
                        player.sendMessage(ChatColor.GOLD + plugin.lang.get("YouWereCharged")+" "+ChatColor.WHITE+plugin.vault.economy.format(areaPrice));
                    }
                    for (Chunk chunk : foundChunks) {
                        MyChunkChunk myChunk = new MyChunkChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ(), plugin);
                        myChunk.claim(correctName);
                        plugin.chunks.put(myChunk.getWorldName()+"_"+myChunk.getX()+"_"+myChunk.getZ(), myChunk);
                    }
                    player.sendMessage(ChatColor.GOLD + plugin.lang.get("ChunksClaimed")+": "+ChatColor.WHITE+foundChunks.size());
                }
            }
            event.setCancelled(true);
            breakSign(block);
        } else if (line0.equalsIgnoreCase("[unclaim]")) {
        // Player attempted to unclaim a chunk
            Player player = event.getPlayer();
            Block block = event.getBlock();
            MyChunkChunk chunk = getChunk(block);
            if (chunk == null) {
                player.sendMessage(ChatColor.RED + plugin.lang.get("ChunkNotOwned"));
                event.setCancelled(true);
                breakSign(block);
                return;
            }
            String owner = chunk.getOwner();
            boolean allowed =true;
            if (!owner.equalsIgnoreCase(player.getName())) {
                if (owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.unclaim.server")) {
                    player.sendMessage(ChatColor.RED + plugin.lang.get("NoPermsUnclaimServer"));
                    allowed = false;
                } else if (!owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.unclaim.others")) {
                    player.sendMessage(ChatColor.RED + plugin.lang.get("NoPermsUnclaimOther"));
                    allowed = false;
                }
            }
            if (allowed) {
                chunk.unclaim();
                plugin.chunks.remove(chunk.getWorldName()+"_"+chunk.getX()+"_"+chunk.getZ());
                if (owner.equalsIgnoreCase(player.getName())) {
                    player.sendMessage(ChatColor.GOLD + plugin.lang.get("ChunkUnclaimed"));
                } else {
                    player.sendMessage(ChatColor.GOLD + plugin.lang.get("ChunkUnclaimedFor")+" " + ChatColor.WHITE + owner + ChatColor.RED + "!");
                }
                if (plugin.unclaimRefund && !player.hasPermission("mychunk.free")) {
                    plugin.vault.economy.depositPlayer(player.getName(), plugin.chunkPrice);
                }
            }
            event.setCancelled(true);
            breakSign(block);
        } else if (line0.equalsIgnoreCase("[owner]")) {
        // Player requested chunk's Owner info
            Player player = event.getPlayer();
            Block block = event.getBlock();
            MyChunkChunk chunk = getChunk(block);
            if (chunk != null) {
                String owner = chunk.getOwner();
                if (owner.equalsIgnoreCase(player.getName())) {
                    player.sendMessage(ChatColor.GOLD + plugin.lang.get("YouOwn"));
                    player.sendMessage(ChatColor.GREEN + plugin.lang.get("AllowedPlayers")+": " + chunk.getAllowed());
                } else {
                    player.sendMessage(ChatColor.GOLD + plugin.lang.get("OwnedBy")+" " + ChatColor.WHITE + owner + ChatColor.GOLD + "!");
                    player.sendMessage(ChatColor.GREEN + plugin.lang.get("AllowedPlayers")+": " + chunk.getAllowed());
                }
            } else {
                player.sendMessage(ChatColor.GOLD + plugin.lang.get("ChunkIs")+" " + ChatColor.WHITE + plugin.lang.get("Unowned") + ChatColor.GOLD + "!");
            }
            event.setCancelled(true);
            breakSign(block);
        } else if (line0.equalsIgnoreCase("[allow]")) {
            // Player attempted to add a player allowance
            Player player = event.getPlayer();
            Block block = event.getBlock();
            MyChunkChunk chunk = getChunk(block);
            if (plugin.lang.get("Everyone").equals(line1.toUpperCase())) {
                line1 = "*";
            }
            String line2 = event.getLine(2).toUpperCase();
            if (chunk == null || !chunk.getOwner().equalsIgnoreCase(player.getName())) {
                player.sendMessage(ChatColor.RED + plugin.lang.get("DoNotOwn"));
            } else if ("".equals(line1) || line1.contains(" ")) {
                player.sendMessage(ChatColor.RED + plugin.lang.get("Line2Player"));
            } else if (line1.equalsIgnoreCase(player.getName())) {
                player.sendMessage(ChatColor.RED + plugin.lang.get("AllowSelf"));
            } else {
                if ("".equals(line2)) {
                    line2 = "*";
                }
                boolean found = true;
                String targetName = "*";
                if (!"*".equalsIgnoreCase(line1)) {
                    Player target = plugin.getServer().getPlayer(line1);
                    if (target == null) {
                        OfflinePlayer offTarget = plugin.getServer().getOfflinePlayer(line1);
                        targetName = offTarget.getName();
                        if (!offTarget.hasPlayedBefore()) {
                            player.sendMessage(ChatColor.RED + plugin.lang.get("Player")+" " + ChatColor.WHITE + line1 + ChatColor.RED + " "+plugin.lang.get("NotFound")+"!");
                            found = false;
                        }
                    } else {
                        targetName = target.getName();
                    }
                }
                String displayName = targetName;
                if (displayName.equals("*")) {
                    displayName = plugin.lang.get("Everyone");
                }
                if (found && !"*".equalsIgnoreCase(line2)) {
                    String errors = "";
                    for (int i = 0; i < line2.length(); i++) {
                        String thisChar = line2.substring(i, i+1).replaceAll(" ","");
                        if (chunk.isFlag(thisChar.toUpperCase())) {
                            chunk.allow(targetName, thisChar);
                        } else {
                            errors += thisChar;
                        }
                    }
                    player.sendMessage(ChatColor.GOLD + plugin.lang.get("PermissionsUpdated"));
                    if (!"".equals(errors)) {
                        player.sendMessage(ChatColor.RED + "Flags not found: " + errors);
                    }
                    player.sendMessage(ChatColor.WHITE + displayName + ChatColor.GOLD + " has had the following flags added: " + ChatColor.GREEN + line2.replaceAll(" ",""));
                    if (!"*".equals(targetName)) {
                        player.sendMessage(ChatColor.GREEN + "Allowed: " + chunk.getAllowedFlags(targetName));
                    }
                    player.sendMessage(ChatColor.GOLD + "Use an [owner] sign to see all permission flags");
                } else if (found && "*".equalsIgnoreCase(line2)) {
                    chunk.allow(targetName, "*");
                    player.sendMessage(ChatColor.GOLD + plugin.lang.get("PermissionsUpdated"));
                    player.sendMessage(ChatColor.WHITE + displayName + ChatColor.GOLD + " has had the following flags added: " + ChatColor.GREEN + line2.replaceAll(" ",""));
                    if (!"*".equals(targetName)) {
                        player.sendMessage(ChatColor.GREEN + "New Flags: " + chunk.getAllowedFlags(targetName));
                    }
                    player.sendMessage(ChatColor.GOLD + "Use an [owner] sign to see all permission flags");
                }
            } 
            event.setCancelled(true);
            breakSign(block);
        } else if (line0.equalsIgnoreCase("[disallow]")) {
            // Player attempted to add a player allowance
            Player player = event.getPlayer();
            Block block = event.getBlock();
            MyChunkChunk chunk = getChunk(block);
            if (plugin.lang.get("Everyone").equals(line1.toUpperCase())) {
                line1 = "*";
            }
            String line2 = event.getLine(2).toUpperCase();
            if (chunk == null || !chunk.getOwner().equalsIgnoreCase(player.getName())) {
                player.sendMessage(ChatColor.RED + plugin.lang.get("DoNotOwn"));
            } else if ("".equals(line1) || line1.contains(" ")) {
                player.sendMessage(ChatColor.RED + plugin.lang.get("Line2Player"));
            } else if (line1.equalsIgnoreCase(player.getName())) {
                player.sendMessage(ChatColor.RED + "You cannot disallow yourself!");
            } else if (!"*".equals(line1) && chunk.isAllowed("*",line2)) {
                player.sendMessage(ChatColor.RED + "You cannot disallow flags allowed to EVERYONE!");
            }else {
                if ("".equals(line2)) {
                    line2 = "*";
                }
                boolean found = true;
                String targetName = "*";
                if (!"*".equalsIgnoreCase(line1)) {
                    Player target = plugin.getServer().getPlayer(line1);
                    if (target == null) {
                        OfflinePlayer offTarget = plugin.getServer().getOfflinePlayer(line1);
                        targetName = offTarget.getName();
                        if (!offTarget.hasPlayedBefore()) {
                            player.sendMessage(ChatColor.RED + plugin.lang.get("Player")+" " + ChatColor.WHITE + line1 + ChatColor.RED + " "+plugin.lang.get("NotFound")+"!");
                            found = false;
                        }
                    } else {
                        targetName = target.getName();
                    }
                }
                String displayName = targetName;
                if (displayName.equals("*")) {
                    displayName = plugin.lang.get("Everyone");
                }
                if (found && !"*".equalsIgnoreCase(line2)) {
                    String errors = "";
                    for (int i = 0; i < line2.length(); i++) {
                        String thisChar = line2.substring(i, i+1).replaceAll(" ","");
                        if (chunk.isFlag(thisChar.toUpperCase())) {
                            chunk.disallow(targetName, thisChar);
                        } else {
                            errors += thisChar;
                        }
                    }
                    player.sendMessage(ChatColor.GOLD + plugin.lang.get("PermissionsUpdated"));
                    if (!"".equals(errors)) {
                        player.sendMessage(ChatColor.RED + "Flags not found: " + errors);
                    }
                    player.sendMessage(ChatColor.WHITE + displayName + ChatColor.GOLD + " has had the following flags removed: " + ChatColor.GREEN + line2.replaceAll(" ",""));
                    if (!"*".equals(targetName)) {
                        player.sendMessage(ChatColor.GREEN + "New Flags: " + chunk.getAllowedFlags(targetName));
                    }
                    player.sendMessage(ChatColor.GOLD + "Use an [owner] sign to see all permission flags");
                } else if (found && "*".equalsIgnoreCase(line2)) {
                    chunk.disallow(targetName, "*");
                    player.sendMessage(ChatColor.GOLD + plugin.lang.get("PermissionsUpdated"));
                    player.sendMessage(ChatColor.WHITE + displayName + ChatColor.GOLD + " has had the following flags removed: " + ChatColor.GREEN + line2.replaceAll(" ",""));
                    if (!"*".equals(targetName)) {
                        player.sendMessage(ChatColor.GREEN + "New Flags: " + chunk.getAllowedFlags(targetName));
                    }
                    player.sendMessage(ChatColor.GOLD + "Use an [owner] sign to see all permission flags");
                }
            } 
            event.setCancelled(true);
            breakSign(block);
        } else if (line0.equalsIgnoreCase("[for sale]")) {
            Player player = event.getPlayer();
            MyChunkChunk chunk = getChunk(event.getBlock());
            boolean allowed = true;
            Double price = 0.00;
            if (!player.hasPermission("mychunk.sell")) {
                player.sendMessage(ChatColor.RED + "You do not have permission to use [For Sale] signs!");
                event.setCancelled(true);
                breakSign(event.getBlock());
                allowed = false;
            } else if (player.hasPermission("mychunk.free")) {
                player.sendMessage(ChatColor.RED + "You can claim chunks for free! You're not allowed to sell them!");
                event.setCancelled(true);
                breakSign(event.getBlock());
                allowed = false;
            } else if (chunk == null || !chunk.getOwner().equalsIgnoreCase(player.getName())) {
                player.sendMessage(ChatColor.RED + "You can't sell this chunk, you don't own it!");
                event.setCancelled(true);
                breakSign(event.getBlock());
                allowed = false;
            } else if (plugin.foundEconomy) {
                if (line1.isEmpty() || line1.equals("")) {
                    player.sendMessage(ChatColor.RED + "Line 2 must contain your sale price!");
                    event.setCancelled(true);
                    breakSign(event.getBlock());
                    allowed = false;
                } else {
                    try {
                        price = Double.parseDouble(line1);
                    } catch (NumberFormatException nfe) {
                        player.sendMessage(ChatColor.RED + "Line 2 must contain your sale price (in #.## format)!");
                        event.setCancelled(true);
                        breakSign(event.getBlock());
                        allowed = false;
                    }
                    if (price == 0) {
                        player.sendMessage(ChatColor.RED + "Sale price cannot be 0!");
                        event.setCancelled(true);
                        breakSign(event.getBlock());
                        allowed = false;
                    }
                }

            }
            if (allowed) {
                if (plugin.foundEconomy) {
                    player.sendMessage(ChatColor.GOLD + "Chunk on sale for " + plugin.vault.economy.format(price) + "!");
                    chunk.setForSale(price);
                } else {
                    player.sendMessage(ChatColor.GOLD + "Chunk on sale!");
                    chunk.setForSale(plugin.chunkPrice);
                }
                breakSign(event.getBlock());
            }
        } else if (line0.equalsIgnoreCase("[not for sale]")) {
            Player player = event.getPlayer();
            MyChunkChunk chunk = getChunk(event.getBlock());
            boolean allowed = true;
            if (chunk == null || !chunk.getOwner().equalsIgnoreCase(player.getName())) {
                player.sendMessage(ChatColor.RED + plugin.lang.get("DoNotOwn"));
                event.setCancelled(true);
                breakSign(event.getBlock());
                allowed = false;
            } else if (!chunk.isForSale()) {
                player.sendMessage(ChatColor.RED + "This chunk is not for sale!");
                event.setCancelled(true);
                breakSign(event.getBlock());
                allowed = false;
            }
            if (allowed) {
                player.sendMessage(ChatColor.GOLD + "Chunk taken off sale!");
                chunk.setNotForSale();
                breakSign(event.getBlock());
            }
        } else if (line0.equalsIgnoreCase("[AllowMobs]")) {
            Player player = event.getPlayer();
            MyChunkChunk chunk = getChunk(event.getBlock());
            boolean allowed = true;
            if (chunk == null || !chunk.getOwner().equalsIgnoreCase(player.getName())) {
                player.sendMessage(ChatColor.RED + plugin.lang.get("DoNotOwn"));
                event.setCancelled(true);
                breakSign(event.getBlock());
                allowed = false;
            }
            if (!player.hasPermission("mychunk.allowmobs")) {
                player.sendMessage(ChatColor.RED + "You do not have permission to use [AllowMobs] signs!");
                event.setCancelled(true);
                breakSign(event.getBlock());
                allowed = false;
            }
            if (!line1.equalsIgnoreCase("on") && !line1.equalsIgnoreCase("off")) {
                player.sendMessage(ChatColor.RED + "Line 2 must say either " + ChatColor.GOLD + "on" + ChatColor.RED + " or " + ChatColor.GOLD + "off" + ChatColor.RED + "!");
                event.setCancelled(true);
                breakSign(event.getBlock());
                allowed = false;
            }
            if (allowed) {
                if (line1.equalsIgnoreCase("on")) {
                    chunk.setAllowMobs(true);
                    player.sendMessage(ChatColor.GOLD + "Mobs now " + ChatColor.GREEN + "CAN" + ChatColor.GOLD + " spawn in this chunk!");
                } else {
                    chunk.setAllowMobs(false);
                    player.sendMessage(ChatColor.GOLD + "Mobs now " + ChatColor.RED + "CAN NOT" + ChatColor.GOLD + " spawn in this chunk!");
                }
                breakSign(event.getBlock());
            }
        }
        if (!event.isCancelled()) {
            Block block = event.getBlock();
            MyChunkChunk chunk = getChunk(block);
            if (chunk != null) {
                String owner = chunk.getOwner();
                Player player = event.getPlayer();
                if (!owner.equalsIgnoreCase(player.getName()) && !chunk.isAllowed(player.getName(), "B") && !player.hasPermission("mychunk.override")) {
                    if (!owner.equalsIgnoreCase("server") ^ !player.hasPermission("mychunk.server.build")) {
                        event.setCancelled(true);
                        breakSign(block);
                    }
                }
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onHangingPlace(HangingPlaceEvent event) {
        if (event.isCancelled())
            return;
        Player player = event.getPlayer();
        MyChunkChunk chunk = getChunk(event.getEntity().getLocation().getBlock());
        if (!player.hasPermission("mychunk.override") && (chunk != null && !player.getName().equals(chunk.getOwner()) && !chunk.isAllowed(player.getName(), "B")) || plugin.protectUnclaimed) {
            player.sendMessage(ChatColor.RED + plugin.lang.get("NoPermsBuild"));
            event.setCancelled(true);
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (event.isCancelled())
            return;
        Entity remover = event.getRemover();
        if (remover instanceof Player) {
            MyChunkChunk chunk = getChunk(event.getEntity().getLocation().getBlock());
            if (!((Player)remover).hasPermission("mychunk.override") && (chunk != null && !((Player)remover).getName().equals(chunk.getOwner()) && !chunk.isAllowed(((Player)remover).getName(), "B")) || plugin.protectUnclaimed) {
                ((Player)remover).sendMessage(ChatColor.RED + plugin.lang.get("NoPermsBreak"));
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.isCancelled())
            return;
        MyChunkChunk fromChunk = getChunk(event.getFrom().getBlock());
        MyChunkChunk toChunk = getChunk(event.getTo().getBlock());
        if (toChunk != null && (fromChunk == null || !fromChunk.getOwner().equals(toChunk.getOwner()))) {
            if (toChunk.getOwner().equalsIgnoreCase("server")) {
                event.getPlayer().sendMessage(ChatColor.LIGHT_PURPLE + "~" + plugin.lang.get("Server"));
            } else {
                event.getPlayer().sendMessage(ChatColor.GOLD + "~" + toChunk.getOwner());
            }
        } else  if (toChunk == null && fromChunk != null) {
            event.getPlayer().sendMessage(ChatColor.GRAY + "~"+plugin.lang.get("Unowned"));
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.isCancelled())
            return;
        if (event.getBlock().getChunk() != event.getBlock().getRelative(event.getDirection()).getChunk()) {
            MyChunkChunk chunk1 = getChunk(event.getBlock());
            MyChunkChunk chunk2 = getChunk(event.getBlock().getRelative(event.getDirection()));
            if (chunk2 != null && (chunk1 == null || !chunk1.getOwner().equals(chunk2.getOwner()))) {
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
                    MyChunkChunk chunk1 = getChunk(block);
                    MyChunkChunk chunk2 = getChunk(block.getRelative(event.getDirection()));
                    if (chunk2 != null && (chunk1 == null || !chunk1.getOwner().equals(chunk2.getOwner()))) {
                        // Pushing into an owned chunk with a different owner
                        event.setCancelled(true);
                    }
                }
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.isCancelled())
            return;
        if (!event.getDirection().equals(BlockFace.UP) && !event.getDirection().equals(BlockFace.DOWN)) {
            if (event.isSticky()) {
                if (event.getBlock().getChunk() != event.getRetractLocation().getBlock().getChunk()) {
                    MyChunkChunk chunk1 = getChunk(event.getBlock());
                    MyChunkChunk chunk2 = getChunk(event.getRetractLocation().getBlock());
                    if (chunk2 != null && (chunk1 == null || !chunk1.getOwner().equals(chunk2.getOwner()))) {
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
    public void onJoin(PlayerJoinEvent event) {
        refreshOwnership(event.getPlayer().getName());
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onQuit(PlayerQuitEvent event) {
        refreshOwnership(event.getPlayer().getName());
    }
    
    private void breakSign(Block block) {
        if (block.getTypeId() == 63 || block.getTypeId() == 68) {
            block.setTypeId(0);
            block.getWorld().dropItem(block.getLocation(), new ItemStack(323,1));
        }
    }
    
    private void refreshOwnership(String playerName) {
        Object[] allChunks = plugin.chunkStore.getKeys(true).toArray();
        for (int i = 1; i < allChunks.length; i++) {
            String thisOwner = plugin.chunkStore.getString(allChunks[i] + ".owner");
            if (playerName.equals(thisOwner)) {
                plugin.chunkStore.set(allChunks[i] + ".lastActive", new Date().getTime() / 1000);
            }
        }
        plugin.saveChunkStore();
        plugin.loadAllChunks();
    }
    
    private MyChunkChunk getChunk(Block block) {
        return plugin.chunks.get(block.getWorld().getName()+"_"+block.getChunk().getX()+"_"+block.getChunk().getZ());
    }
    
    private MyChunkChunk getChunk(String world, int x, int z) {
        return plugin.chunks.get(world+"_"+x+"_"+z);
    }
    
    private boolean isForSale(MyChunkChunk chunk) {
        if(chunk == null) return false;
        else return chunk.isForSale();
    }
    
    private boolean hasNeighbours(Chunk chunk) {
        MyChunkChunk chunkX1 = plugin.chunks.get(chunk.getWorld().getName()+"_"+(chunk.getX() + 1)+"_"+chunk.getZ());
        MyChunkChunk chunkX2 = plugin.chunks.get(chunk.getWorld().getName()+"_"+(chunk.getX() - 1)+"_"+chunk.getZ());
        MyChunkChunk chunkZ1 = plugin.chunks.get(chunk.getWorld().getName()+"_"+chunk.getX()+"_"+(chunk.getZ()+1));
        MyChunkChunk chunkZ2 = plugin.chunks.get(chunk.getWorld().getName()+"_"+chunk.getX()+"_"+(chunk.getZ()-1));
        if (chunkX1 != null || chunkX2 != null || chunkZ1 != null || chunkZ2 != null) {
            return true;
        }
        return false;
    }
    
    private boolean hasNeighbours(MyChunkChunk chunk) {
        if (chunk != null) return chunk.hasNeighbours();
        return false;
    }
    
    private double getClaimPrice(MyChunkChunk chunk) {
        if (chunk == null) return plugin.chunkPrice;
        else return chunk.getClaimPrice();
    }
    
    private double getOverbuyPrice(MyChunkChunk chunk) {
        if (chunk == null) {
            double price = plugin.chunkPrice;
            if (plugin.allowOverbuy) {
                price += plugin.overbuyPrice;
            }
            return price;
        }
        else return chunk.getOverbuyPrice();
    }
    
}
