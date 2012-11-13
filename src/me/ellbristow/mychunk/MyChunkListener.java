package me.ellbristow.mychunk;

import java.util.*;
import me.ellbristow.mychunk.lang.Lang;
import org.bukkit.*;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
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
                if (isClaimed(block.getChunk())) {
                    saveBanks.add(block);
                } else if (plugin.protectUnclaimed) {
                    if (!(event.getEntity() instanceof TNTPrimed) || !plugin.unclaimedTNT) {
                        saveBanks.add(block);
                    } else if (event.getEntity() instanceof TNTPrimed && plugin.unclaimedTNT) {
                        saveBanks.add(block);
                    }
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
        Player player = event.getPlayer();
        Block block = event.getBlock();
        MyChunkChunk chunk = getChunk(block);
        if (chunk.isClaimed() || plugin.protectUnclaimed) {
            if (chunk.isClaimed() && !WorldGuardHook.isRegion(event.getBlock().getLocation())) {
                String owner = chunk.getOwner();
                if (!owner.equalsIgnoreCase(player.getName()) && !chunk.isAllowed(player.getName(), "B")) {
                    if ((!owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.override")) || (owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.server.build"))) {
                        player.sendMessage(ChatColor.RED + Lang.get("NoPermsBuild"));
                        if (block.getTypeId() != 63 && block.getTypeId() != 68) {
                            event.setCancelled(true);
                        }
                    }
                }
            } else if (!player.hasPermission("mychunk.override")) {
                if (block.getTypeId() != 63 && block.getTypeId() != 68) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + Lang.get("NoPermsBuild"));
                }
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onBlockBreak (BlockBreakEvent event) {
        if (event.isCancelled())
            return;
        MyChunkChunk chunk = getChunk(event.getBlock());
        if (chunk.isClaimed() || plugin.protectUnclaimed) {
            Player player = event.getPlayer();
            if (chunk.isClaimed() && !WorldGuardHook.isRegion(event.getBlock().getLocation())) {
                String owner = chunk.getOwner();
                if (!owner.equalsIgnoreCase(player.getName())&& !chunk.isAllowed(player.getName(), "D")) {
                    if ((!owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.override")) || (owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.server.destroy"))) {
                        player.sendMessage(ChatColor.RED + Lang.get("NoPermsBreak"));
                        event.setCancelled(true);
                    }
                }
            } else if (!player.hasPermission("mychunk.override")) {
                player.sendMessage(ChatColor.RED + Lang.get("NoPermsBreak"));
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onBlockIgnite (BlockIgniteEvent event) {
        if (event.isCancelled())
            return;
        String owner = getOwner(event.getBlock().getChunk());
        if (!owner.equalsIgnoreCase("Unowned")) {
            if (event.getCause().equals(IgniteCause.FLINT_AND_STEEL)) {
                MyChunkChunk chunk = getChunk(event.getBlock());
                Player player = event.getPlayer();
                if (!owner.equalsIgnoreCase(player.getName()) && !chunk.isAllowed(player.getName(), "I")) {
                    if ((!owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.override")) || (owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.server.ignite"))) {
                        player.sendMessage(ChatColor.RED + Lang.get("NoPermsFire"));
                        event.setCancelled(true);
                    }
                }
            } else if (event.getCause().equals(IgniteCause.LAVA) || event.getCause().equals(IgniteCause.SPREAD)) {
                event.setCancelled(true);
            }
        } else if (plugin.protectUnclaimed && event.getCause().equals(IgniteCause.FLINT_AND_STEEL)) {
            Player player = event.getPlayer();
            if (player != null && !player.hasPermission("mychunk.override")) {
                player.sendMessage(ChatColor.RED + Lang.get("NoPermsFire"));
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onBlockFromTo (BlockFromToEvent event) {
        Block block = event.getBlock();
        Block toBlock = event.getToBlock();
        if (block.getChunk() != toBlock.getChunk()) {
            String toOwner = getOwner(toBlock.getChunk());
            String fromOwner = getOwner(block.getChunk());
            if (!toOwner.equalsIgnoreCase(fromOwner)) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onBlockBurn (BlockBurnEvent event) {
        Block block = event.getBlock();
        String toOwner = getOwner(block.getChunk());
        if (!toOwner.equalsIgnoreCase("Unowned")) {
            event.setCancelled(true);
            if (block.getRelative(BlockFace.UP).getType().equals(Material.FIRE)) {
                block.getRelative(BlockFace.UP).setType(Material.AIR);
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onBlockSpread (BlockSpreadEvent event) {
        Block toBlock = event.getBlock();
        String toOwner = getOwner(toBlock.getChunk());
        if (!toOwner.equalsIgnoreCase("Unowned")) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onZombieDoorEvent (EntityBreakDoorEvent event) {
        if (event.isCancelled())
            return;
        if (event.getBlock().getTypeId() == 64 && event.getEntityType().equals(EntityType.ZOMBIE)) {
            if (isClaimed(event.getBlock().getChunk()) || plugin.protectUnclaimed) {
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
        if (chunk.isClaimed() || plugin.protectUnclaimed) {
            // Claimed
            Player player = event.getPlayer();
            int bucket = event.getBucket().getId();
            if (chunk.isClaimed()) {
                String owner = chunk.getOwner();
                if ((owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.server.build")) || (!owner.equalsIgnoreCase(player.getName()) && !player.hasPermission("mychunk.override"))) {
                    if (bucket == 327 && !chunk.isAllowed(player.getName(), "L")) {
                        player.sendMessage(ChatColor.RED + Lang.get("NoPermsLava"));
                        event.setCancelled(true);
                        player.setItemInHand(new ItemStack(327,1));
                    } else if (bucket == 326 && !chunk.isAllowed(player.getName(), "W")) {
                        player.sendMessage(ChatColor.RED + Lang.get("NoPermsWater"));
                        event.setCancelled(true);
                        player.setItemInHand(new ItemStack(326,1));
                    }
                }
            } else if (!player.hasPermission("mychunk.override")) {
                // ProtectUnclaimed
                if (bucket == 327) {
                    player.sendMessage(ChatColor.RED + Lang.get("NoPermsLava"));
                    event.setCancelled(true);
                    player.setItemInHand(new ItemStack(327,1));
                } else if (bucket == 326) {
                    player.sendMessage(ChatColor.RED + Lang.get("NoPermsWater"));
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
            if (!isClaimed(block.getChunk())) return;
            MyChunkChunk chunk = getChunk(block);
            Player player = event.getPlayer();
            String owner = chunk.getOwner();
            if (block.getType().equals(Material.NETHERRACK) && block.getRelative(BlockFace.UP) != null && block.getRelative(BlockFace.UP).getType().equals(Material.FIRE)) {
                if (!owner.equals(player.getName()) && !chunk.isAllowed(player.getName(), "B")) {
                    if ((!owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.override")) || (owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.server.doors"))) {
                        player.sendMessage(ChatColor.RED + Lang.get("NoPermsFire"));
                        event.setCancelled(true);
                    }
                }
            } else if (block.getTypeId() == 64 || block.getTypeId() == 96 || block.getTypeId() == 107) {
                if (!owner.equals(player.getName()) && !chunk.isAllowed(player.getName(), "O")) {
                    if ((!owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.override")) || (owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.server.doors"))) {
                        player.sendMessage(ChatColor.RED + Lang.get("NoPermsDoor"));
                        OfflinePlayer ownerPlayer = plugin.getServer().getOfflinePlayer(owner);
                        if (ownerPlayer.isOnline()) {
                            ownerPlayer.getPlayer().sendMessage(ChatColor.GOLD + Lang.get("NoPermsDoorOwner"));
                        }
                        event.setCancelled(true);
                    }
                }
            } else if (block.getTypeId() == 77) {
                if (!owner.equals(player.getName()) && !chunk.isAllowed(player.getName(), "U")) {
                    if ((!owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.override")) || (owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.server.use"))) {
                        player.sendMessage(ChatColor.RED + Lang.get("NoPermsButton"));
                        OfflinePlayer ownerPlayer = plugin.getServer().getOfflinePlayer(owner);
                        if (ownerPlayer.isOnline()) {
                            ownerPlayer.getPlayer().sendMessage(ChatColor.GOLD + Lang.get("NoPermsButtonOwner"));
                        }
                        event.setCancelled(true);
                    }
                }
            } else if (block.getTypeId() == 69) {
                if (!owner.equals(player.getName()) && !chunk.isAllowed(player.getName(), "U")) {
                    if ((!owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.override")) || (owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.server.use"))) {
                        player.sendMessage(ChatColor.RED + Lang.get("NoPermsLever"));
                        OfflinePlayer ownerPlayer = plugin.getServer().getOfflinePlayer(owner);
                        if (ownerPlayer.isOnline()) {
                            ownerPlayer.getPlayer().sendMessage(ChatColor.GOLD + Lang.get("NoPermsLeverOwner"));
                        }
                        event.setCancelled(true);
                    }
                }
            } else if (block.getTypeId() == 54) {
                if (!owner.equals(player.getName()) && !chunk.isAllowed(player.getName(), "C")) {
                    if ((!owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.override")) || (owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.server.chests"))) {
                        player.sendMessage(ChatColor.RED + Lang.get("NoPermsChest"));
                        OfflinePlayer ownerPlayer = plugin.getServer().getOfflinePlayer(owner);
                        if (ownerPlayer.isOnline()) {
                            ownerPlayer.getPlayer().sendMessage(ChatColor.GOLD + Lang.get("NoPermsChestOwner"));
                        }
                        event.setCancelled(true);
                    }
                }
            } else if (block.getTypeId() == 61 || block.getTypeId() == 62 || block.getTypeId() == 23 || block.getTypeId() == 117) {
                if (!owner.equals(player.getName()) && !chunk.isAllowed(player.getName(), "S")) {
                    if ((!owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.override")) || (owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.server.special"))) {
                        player.sendMessage(ChatColor.RED + Lang.get("NoPermsSpecial"));
                        OfflinePlayer ownerPlayer = plugin.getServer().getOfflinePlayer(owner);
                        if (ownerPlayer.isOnline()) {
                            ownerPlayer.getPlayer().sendMessage(ChatColor.GOLD + Lang.get("NoPermsSpecialOwner"));
                        }
                        event.setCancelled(true);
                    }
                }
            }
        } else if (event.getAction().equals(Action.PHYSICAL)) {
            Player player = event.getPlayer();
            Block block = player.getLocation().getBlock();
            if (!isClaimed(block.getChunk())) return;
            MyChunkChunk chunk = getChunk(block);
            String owner = chunk.getOwner();
            if ((block.getType() == Material.CROPS || block.getType() ==  Material.SOIL || block.getType() ==  Material.CARROT || block.getType() ==  Material.POTATO || (block.getType() ==  Material.AIR && block.getRelative(BlockFace.DOWN).getType().equals(Material.SOIL))) && chunk.isClaimed()) {
                event.setCancelled(true);
            } else if (chunk.isClaimed() && !owner.equals(player.getName()) && !chunk.isAllowed(player.getName(), "U")) {
                if ((!owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.override")) || (owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.server.use"))) {
                    event.setCancelled(true);
                }
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onEntityInteract(EntityInteractEvent event) {
        Block block = event.getBlock();
        if ((block.getType() == Material.CROPS || block.getType() ==  Material.SOIL || block.getType() ==  Material.CARROT || block.getType() ==  Material.POTATO || (block.getType() ==  Material.AIR && block.getRelative(BlockFace.DOWN).getType().equals(Material.SOIL))) && isClaimed(block.getChunk())) {
            event.setCancelled(true);
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
            if (!fromChunk.getOwner().equalsIgnoreCase(toChunk.getOwner())) {
                String forSale = "";
                if (toChunk.isForSale()) {
                    forSale = ChatColor.YELLOW + " ["+Lang.get("ChunkForSale");
                    if (plugin.foundEconomy && toChunk.getClaimPrice() != 0) {
                        if (plugin.ownedChunkCount(player.getName()) < plugin.maxChunks || !plugin.allowOverbuy) {
                            forSale += ": " + plugin.vault.economy.format(toChunk.getClaimPrice());
                        } else if (plugin.allowOverbuy && plugin.ownedChunkCount(player.getName()) >= plugin.maxChunks) {
                            forSale += ": " + plugin.vault.economy.format(toChunk.getOverbuyPrice());
                        }
                    }
                    forSale += "]";
                }
                if (!toChunk.isClaimed()) {
                    player.sendMessage(ChatColor.GRAY + "~"+Lang.get("Unowned"));
                } else if (toChunk.getOwner().equalsIgnoreCase("server")) {
                    player.sendMessage(ChatColor.LIGHT_PURPLE + "~"+Lang.get("Server") + forSale);
                } else {
                    player.sendMessage(ChatColor.GOLD + "~" + toChunk.getOwner() + forSale);
                }
            } else if (toChunk.isForSale()) {
                String forSale = ChatColor.YELLOW + "["+Lang.get("ChunkForSale");
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
        if (mob instanceof Monster || mob instanceof Slime) {
            MyChunkChunk chunk = getChunk(event.getLocation().getBlock());
            if (chunk.isClaimed() && !chunk.getAllowMobs()) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onPlayerDamage (EntityDamageByEntityEvent event) {
        if (event.isCancelled())
            return;
        Entity entity = event.getEntity();
        if (entity instanceof Player) {
            Player player = (Player)entity;
            MyChunkChunk chunk = getChunk(player.getLocation().getBlock());
            if (chunk.isClaimed()) {
                Entity damager = event.getDamager();
                if (damager instanceof Player) {
                    event.setCancelled(true);
                    Player naughty = (Player)damager;
                    naughty.sendMessage(ChatColor.RED + Lang.get("NoPermsPVP"));
                } else if (damager instanceof Monster || damager instanceof Slime) {
                    if (!chunk.getAllowMobs()) {
                        event.setCancelled(true);
                    }
                } else if (damager instanceof Projectile) {
                    Entity shooter = ((Projectile) event.getDamager()).getShooter();
                    if (shooter instanceof Player && entity instanceof Player) {
                        event.setCancelled(true);
                    } else if (shooter instanceof Monster && entity instanceof Player) {
                        if (!chunk.getAllowMobs()) {
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onPotionSplash(PotionSplashEvent event) {
        ThrownPotion potion = event.getPotion();
        String owner = getOwner(potion.getLocation().getChunk());
        if (!owner.equalsIgnoreCase("Unowned")) {
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
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onSignChange (SignChangeEvent event) {
        if (event.isCancelled()) return;
        String line0 = event.getLine(0);
        String line1 = event.getLine(1);
        if (line0.equalsIgnoreCase("[claim]")) {
        // Player attempted to claim a chunk
            Player player = event.getPlayer();
            Block block = event.getBlock();
            MyChunkChunk chunk = getChunk(block);
            boolean allowed = true;
            if (!player.hasPermission("mychunk.claim") && !player.hasPermission("mychunk.claim.server")) {
                player.sendMessage(ChatColor.RED + Lang.get("NoPermsClaim"));
                allowed = false;
            } else if (chunk.isClaimed()) {
                String owner = chunk.getOwner();
                if (owner.equalsIgnoreCase(player.getName())) {
                    player.sendMessage(ChatColor.RED + Lang.get("AlreadyOwner"));
                    allowed = false;
                } else if (!chunk.isForSale()) {
                    player.sendMessage(ChatColor.RED + Lang.get("AlreadyOwned")+" " + ChatColor.WHITE + owner + ChatColor.RED + "!");
                    allowed = false;
                } else if (chunk.isForSale() && !player.hasPermission("mychunk.buy")) {
                    player.sendMessage(ChatColor.RED + Lang.get("NoPermsBuyOwned"));
                    allowed = false;
                }
            } else if (!plugin.allowNeighbours && chunk.hasNeighbours() && chunk.isForSale()) {
                MyChunkChunk[] neighbours = chunk.getNeighbours();
                if (!neighbours[0].getOwner().equalsIgnoreCase(line1) || !neighbours[1].getOwner().equalsIgnoreCase(line1) || !neighbours[2].getOwner().equalsIgnoreCase(line1) || !neighbours[3].getOwner().equalsIgnoreCase(line1)) {
                    player.sendMessage(ChatColor.RED + Lang.get("NoNeighbours"));
                }
            } else if(!plugin.allowNether && player.getWorld().getEnvironment().equals(Environment.NETHER)){
                player.sendMessage(ChatColor.RED + Lang.get("NoPermsNether"));
                allowed = false;
            } else if (!plugin.allowEnd && player.getWorld().getEnvironment().equals(Environment.THE_END)) {
                player.sendMessage(ChatColor.RED + Lang.get("NoPermsEnd"));
                allowed = false;
            }
            int playerMax = plugin.getMaxChunks(player);
            if (plugin.foundEconomy && chunk.getClaimPrice() != 0 && !player.hasPermission("mychunk.free") && (playerMax == 0 || plugin.ownedChunkCount(player.getName()) < playerMax) && plugin.vault.economy.getBalance(player.getName()) < chunk.getClaimPrice()) {
                player.sendMessage(ChatColor.RED + Lang.get("CantAfford")+" ("+Lang.get("Price")+": " + ChatColor.WHITE + plugin.vault.economy.format(chunk.getClaimPrice()) + ChatColor.RED + ")!");
                allowed = false;
            } else if (plugin.foundEconomy && playerMax != 0 && plugin.ownedChunkCount(player.getName()) >= playerMax && !player.hasPermission("mychunk.free")) {
                if (plugin.allowOverbuy && player.hasPermission("mychunk.claim.overbuy") && plugin.vault.economy.getBalance(player.getName()) < chunk.getOverbuyPrice()) {
                    player.sendMessage(ChatColor.RED + Lang.get("CantAfford")+" ("+Lang.get("Price")+": " + ChatColor.WHITE + plugin.vault.economy.format(chunk.getOverbuyPrice()) + ChatColor.RED + ")!");
                    allowed = false;
                }
            }
            if (allowed) {
                if (line1.equals("") || line1.equalsIgnoreCase(player.getName())) {
                    int ownedChunks = plugin.ownedChunkCount(player.getName());
                    if ((ownedChunks < playerMax || (plugin.allowOverbuy && player.hasPermission("mychunk.claim.overbuy"))) || player.hasPermission("mychunk.claim.unlimited") || playerMax == 0) {
                        if (plugin.foundEconomy && chunk.getClaimPrice() != 0 && !player.hasPermission("mychunk.free") && (playerMax == 0 || plugin.ownedChunkCount(player.getName()) < playerMax)) {
                            plugin.vault.economy.withdrawPlayer(player.getName(), chunk.getClaimPrice());
                            player.sendMessage(plugin.vault.economy.format(chunk.getClaimPrice()) + ChatColor.GOLD + " "+Lang.get("AmountDeducted"));
                        } else if (plugin.foundEconomy && plugin.allowOverbuy && plugin.ownedChunkCount(player.getName()) >= playerMax && !player.hasPermission("mychunk.free")) {
                            double price;
                            if (plugin.overbuyP2P) {
                                price = chunk.getOverbuyPrice();
                            } else {
                                price = chunk.getClaimPrice();
                            }
                            plugin.vault.economy.withdrawPlayer(player.getName(), price);
                            player.sendMessage(plugin.vault.economy.format(price) + ChatColor.GOLD + " "+Lang.get("AmountDeducted"));
                        }
                        if (plugin.foundEconomy && chunk.isForSale()) {
                            plugin.vault.economy.depositPlayer(chunk.getOwner(), chunk.getClaimPrice());
                            OfflinePlayer oldOwner = plugin.getServer().getOfflinePlayer(chunk.getOwner());
                            if (oldOwner.isOnline()) {
                                oldOwner.getPlayer().sendMessage(player.getName() + ChatColor.GOLD + " "+Lang.get("BoughtFor")+" " + ChatColor.WHITE + plugin.vault.economy.format(chunk.getClaimPrice()) + ChatColor.GOLD + "!");
                            }
                        }
                        if (chunk == null) {
                            chunk = new MyChunkChunk(block,plugin);
                        }
                        chunk.claim(player.getName());
                        player.sendMessage(ChatColor.GOLD + Lang.get("ChunkClaimed"));
                    } else {
                        player.sendMessage(ChatColor.RED + Lang.get("AlreadyOwn")+" " + ownedChunks + " "+Lang.get("Chunks")+"! (Max " + playerMax + ")");
                    }
                } else {
                    String correctName = "";
                    allowed = true;
                    if (line1.equalsIgnoreCase("server")) {
                        if (!player.hasPermission("mychunk.claim.server")) {
                            player.sendMessage(ChatColor.RED + Lang.get("NoPermsClaimServer"));
                            allowed = false;
                        } else {
                            correctName = "Server";
                        }
                    } else {
                        if (player.hasPermission("mychunk.claim.others")) {
                            OfflinePlayer target = plugin.getServer().getOfflinePlayer(line1);
                            if (!target.hasPlayedBefore()) {
                                player.sendMessage(ChatColor.RED + Lang.get("Player")+" " + ChatColor.WHITE + line1 + ChatColor.RED + " "+Lang.get("NotFound")+"!");
                                allowed = false;
                            } else {
                                correctName = target.getName();
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + Lang.get("NoPermsClaimOther"));
                            allowed = false;
                        }
                    }
                    if (allowed) {
                        int ownedChunks = plugin.ownedChunkCount(player.getName());
                        if ((ownedChunks < plugin.maxChunks) || player.hasPermission("mychunk.claim.others.unlimited") || plugin.maxChunks == 0 || (correctName.equalsIgnoreCase("server") && player.hasPermission("mychunk.server.claim"))) {
                            chunk.claim(correctName);
                            player.sendMessage(ChatColor.GOLD + Lang.get("ChunkClaimedFor")+" " + ChatColor.WHITE + correctName + ChatColor.GOLD + "!");
                            if (plugin.foundEconomy && plugin.chunkPrice != 0 && !correctName.equalsIgnoreCase("server") && !player.hasPermission("mychunk.free")) {
                                plugin.vault.economy.withdrawPlayer(player.getName(), plugin.chunkPrice);
                                player.sendMessage(plugin.vault.economy.format(plugin.chunkPrice) + ChatColor.GOLD + " "+Lang.get("AmountDeducted"));
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
                player.sendMessage(ChatColor.RED + Lang.get("NoPermsClaim"));
                allowed = false;
            } else if (!player.hasPermission("mychunk.claim.area")) {
                player.sendMessage(ChatColor.RED + Lang.get("NoPermsClaimArea"));
                allowed = false;
            } else if (!plugin.allowNether && block.getWorld().getEnvironment().equals(Environment.NETHER)) {
                player.sendMessage(ChatColor.RED + Lang.get("NoPermsNether"));
                allowed = false;
            } else if (!plugin.allowEnd && block.getWorld().getEnvironment().equals(Environment.THE_END)) {
                player.sendMessage(ChatColor.RED + Lang.get("NoPermsEnd"));
                allowed = false;
            }
            if (allowed) {
                String correctName;
                if (line1.isEmpty() || line1.equalsIgnoreCase(player.getName())) {
                    correctName = player.getName();
                } else {
                    if (line1.equalsIgnoreCase("server")) {
                        if (!player.hasPermission("mychunk.claim.server")) {
                            player.sendMessage(ChatColor.RED + Lang.get("NoPermsClaimServer"));
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
                                player.sendMessage(ChatColor.RED + Lang.get("Player")+" " + ChatColor.WHITE + line1 + ChatColor.RED + " "+Lang.get("NotFound")+"!");
                                event.setCancelled(true);
                                breakSign(block);
                                return;
                            } else {
                                correctName = target.getName();
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + Lang.get("NoPermsClaimOther"));
                            event.setCancelled(true);
                            breakSign(block);
                            return;
                        }
                    }
                }
                event.setLine(1, correctName);
                if (event.getLine(2).equalsIgnoreCase("cancel")) {
                    plugin.pendingAreas.remove(correctName);
                    player.sendMessage(ChatColor.RED + Lang.get("ClaimAreaCancelled"));
                    event.setCancelled(true);
                    breakSign(block);
                    return;
                }
                if (!plugin.pendingAreas.containsKey(correctName)) {
                    plugin.pendingAreas.put(correctName, event.getBlock());
                    player.sendMessage(ChatColor.GOLD + Lang.get("StartClaimArea1"));
                    player.sendMessage(ChatColor.GOLD + Lang.get("StartClaimArea2"));
                } else {
                    Block startBlock = plugin.pendingAreas.get(correctName);
                    if (startBlock.getWorld() != block.getWorld()) {
                        player.sendMessage(ChatColor.RED + Lang.get("ClaimAreaWorldError"));
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
                    xloop:
                    for (int x = startX; x <= endX; x++) {
                        for (int z = startZ; z <= endZ; z++) {
                            if (chunkCount < 64) {
                                Chunk thisChunk = block.getWorld().getChunkAt(x, z);
                                MyChunkChunk myChunk = getChunk(block.getWorld().getName(), x, z);
                                if (myChunk.isClaimed() && !myChunk.getOwner().equalsIgnoreCase(correctName) && myChunk.isForSale()) {
                                    foundClaimed = true;
                                    break xloop;
                                } else if (myChunk.hasNeighbours()) {
                                    MyChunkChunk[] neighbours = myChunk.getNeighbours();
                                    for (MyChunkChunk neighbour : neighbours) {
                                        if (neighbour.isClaimed() && !neighbour.getOwner().equalsIgnoreCase(correctName) && !neighbour.getOwner().equalsIgnoreCase("Server") && !myChunk.isForSale()) {
                                            foundNeighbour = true;
                                            if (!plugin.allowNeighbours) break xloop;
                                        }
                                    }
                                }
                                foundChunks.add(thisChunk);
                                chunkCount++;
                            } else {
                                player.sendMessage(ChatColor.RED + Lang.get("AreaTooBig"));
                                event.setCancelled(true);
                                breakSign(block);
                                return;
                            }
                        }
                    }
                    if (foundClaimed) {
                        player.sendMessage(ChatColor.RED + Lang.get("FoundClaimedInArea"));
                        event.setCancelled(true);
                        breakSign(block);
                        return;
                    }
                    if (foundNeighbour && !plugin.allowNeighbours) {
                        player.sendMessage(ChatColor.RED + Lang.get("FoundNeighboursInArea"));
                        event.setCancelled(true);
                        breakSign(block);
                        return;
                    }
                    int claimed = plugin.ownedChunkCount(correctName);
                    int max = plugin.getMaxChunks(plugin.getServer().getOfflinePlayer(correctName).getPlayer());
                    if (max != 0 && (!plugin.allowOverbuy || !player.hasPermission("mychunk.claim.overbuy")) && max - claimed < foundChunks.size()) {
                        player.sendMessage(ChatColor.RED + (correctName.equalsIgnoreCase(player.getName())?"You":correctName) + Lang.get("ClaimAreaTooLarge"));
                        player.sendMessage(ChatColor.RED + Lang.get("ChunksOwned")+": " + ChatColor.WHITE + claimed);
                        player.sendMessage(ChatColor.RED + Lang.get("ChunkMax")+": " + ChatColor.WHITE + max);
                        player.sendMessage(ChatColor.RED + Lang.get("ChunksInArea") + ": " + chunkCount);
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
                                areaPrice += myChunk.getClaimPrice();
                                allowance--;
                            } else {
                                areaPrice += myChunk.getOverbuyPrice();
                            }
                        }
                        if (plugin.vault.economy.getBalance(player.getName()) < areaPrice) {
                            player.sendMessage(ChatColor.RED + Lang.get("CantAffordClaimArea"));
                            player.sendMessage(ChatColor.RED + Lang.get("Price")+": " + ChatColor.WHITE + plugin.vault.economy.format(areaPrice));
                            event.setCancelled(true);
                            breakSign(block);
                            return;
                        }
                        plugin.vault.economy.withdrawPlayer(player.getName(), areaPrice);
                        player.sendMessage(ChatColor.GOLD + Lang.get("YouWereCharged")+" "+ChatColor.WHITE+plugin.vault.economy.format(areaPrice));
                    }
                    for (Chunk chunk : foundChunks) {
                        MyChunkChunk myChunk = getChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
                        myChunk.claim(correctName);
                    }
                    player.sendMessage(ChatColor.GOLD + Lang.get("ChunksClaimed")+": "+ChatColor.WHITE+foundChunks.size());
                }
            }
            event.setCancelled(true);
            breakSign(block);
        } else if (line0.equalsIgnoreCase("[unclaim]")) {
        // Player attempted to unclaim a chunk
            Player player = event.getPlayer();
            Block block = event.getBlock();
            MyChunkChunk chunk = getChunk(block);
            if (!chunk.isClaimed()) {
                player.sendMessage(ChatColor.RED + Lang.get("ChunkNotOwned"));
                event.setCancelled(true);
                breakSign(block);
                return;
            }
            String owner = chunk.getOwner();
            boolean allowed =true;
            if (!owner.equalsIgnoreCase(player.getName())) {
                if (owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.unclaim.server")) {
                    player.sendMessage(ChatColor.RED + Lang.get("NoPermsUnclaimServer"));
                    allowed = false;
                } else if (!owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.unclaim.others")) {
                    player.sendMessage(ChatColor.RED + Lang.get("NoPermsUnclaimOther"));
                    allowed = false;
                }
            }
            if (allowed) {
                chunk.unclaim();
                if (owner.equalsIgnoreCase(player.getName())) {
                    player.sendMessage(ChatColor.GOLD + Lang.get("ChunkUnclaimed"));
                } else {
                    player.sendMessage(ChatColor.GOLD + Lang.get("ChunkUnclaimedFor")+" " + ChatColor.WHITE + owner + ChatColor.RED + "!");
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
            if (chunk.isClaimed()) {
                String owner = chunk.getOwner();
                if (owner.equalsIgnoreCase(player.getName())) {
                    player.sendMessage(ChatColor.GOLD + Lang.get("YouOwn"));
                    player.sendMessage(ChatColor.GREEN + Lang.get("AllowedPlayers")+": " + chunk.getAllowed());
                } else {
                    player.sendMessage(ChatColor.GOLD + Lang.get("OwnedBy")+" " + ChatColor.WHITE + owner + ChatColor.GOLD + "!");
                    player.sendMessage(ChatColor.GREEN + Lang.get("AllowedPlayers")+": " + chunk.getAllowed());
                }
            } else {
                player.sendMessage(ChatColor.GOLD + Lang.get("ChunkIs")+" " + ChatColor.WHITE + Lang.get("Unowned") + ChatColor.GOLD + "!");
            }
            event.setCancelled(true);
            breakSign(block);
        } else if (line0.equalsIgnoreCase("[allow]")) {
            // Player attempted to add a player allowance
            Player player = event.getPlayer();
            Block block = event.getBlock();
            MyChunkChunk chunk = getChunk(block);
            if (Lang.get("Everyone").equalsIgnoreCase(line1.toUpperCase())) {
                line1 = "*";
            }
            String line2 = event.getLine(2).toUpperCase();
            String owner = chunk.getOwner();
            if (!owner.equalsIgnoreCase(player.getName()) && !(owner.equalsIgnoreCase("server") && player.hasPermission("mychunk.server.signs"))) {
                player.sendMessage(ChatColor.RED + Lang.get("DoNotOwn"));
           } else if ("".equals(line1) || line1.contains(" ")) {
                player.sendMessage(ChatColor.RED + Lang.get("Line2Player"));
            } else if (line1.equalsIgnoreCase(player.getName()) && !chunk.getOwner().equalsIgnoreCase("Server")) {
                player.sendMessage(ChatColor.RED + Lang.get("AllowSelf"));
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
                            player.sendMessage(ChatColor.RED + Lang.get("Player")+" " + ChatColor.WHITE + line1 + ChatColor.RED + " "+Lang.get("NotFound")+"!");
                            found = false;
                        }
                    } else {
                        targetName = target.getName();
                    }
                }
                String displayName = targetName;
                if (displayName.equals("*")) {
                    displayName = Lang.get("Everyone");
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
                    player.sendMessage(ChatColor.GOLD + Lang.get("PermissionsUpdated"));
                    if (!"".equals(errors)) {
                        player.sendMessage(ChatColor.RED + "Flags not found: " + errors);
                    }
                    chunk.allow(targetName, line2.replaceAll(" ",""));
                    player.sendMessage(ChatColor.WHITE + displayName + ChatColor.GOLD + " has had the following flags added: " + ChatColor.GREEN + line2.replaceAll(" ",""));
                    if (!"*".equals(targetName)) {
                        player.sendMessage(ChatColor.GREEN + "Allowed: " + chunk.getAllowedFlags(targetName));
                    }
                    player.sendMessage(ChatColor.GOLD + "Use an [owner] sign to see all permission flags");
                } else if (found && "*".equalsIgnoreCase(line2)) {
                    chunk.allow(targetName, line2.replaceAll(" ",""));
                    player.sendMessage(ChatColor.GOLD + Lang.get("PermissionsUpdated"));
                    player.sendMessage(ChatColor.WHITE + displayName + ChatColor.GOLD + " has had the following flags added: " + ChatColor.GREEN + line2.replaceAll(" ",""));
                    if (!"*".equals(line2)) {
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
            if (Lang.get("Everyone").equalsIgnoreCase(line1.toUpperCase())) {
                line1 = "*";
            }
            String line2 = event.getLine(2).toUpperCase();
            String owner = chunk.getOwner();
            if (!owner.equalsIgnoreCase(player.getName()) && !(owner.equalsIgnoreCase("server") && player.hasPermission("mychunk.server.signs"))) {
                player.sendMessage(ChatColor.RED + Lang.get("DoNotOwn"));
            } else if ("".equals(line1) || line1.contains(" ")) {
                player.sendMessage(ChatColor.RED + Lang.get("Line2Player"));
            } else if (line1.equalsIgnoreCase(player.getName()) && !chunk.getOwner().equalsIgnoreCase("Server")) {
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
                            player.sendMessage(ChatColor.RED + Lang.get("Player")+" " + ChatColor.WHITE + line1 + ChatColor.RED + " "+Lang.get("NotFound")+"!");
                            found = false;
                        }
                    } else {
                        targetName = target.getName();
                    }
                }
                String displayName = targetName;
                if (displayName.equals("*")) {
                    displayName = Lang.get("Everyone");
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
                    player.sendMessage(ChatColor.GOLD + Lang.get("PermissionsUpdated"));
                    if (!"".equals(errors)) {
                        player.sendMessage(ChatColor.RED + "Flags not found: " + errors);
                    }
                    chunk.disallow(targetName, line2.replaceAll(" ",""));
                    player.sendMessage(ChatColor.WHITE + displayName + ChatColor.GOLD + " has had the following flags removed: " + ChatColor.GREEN + line2.replaceAll(" ",""));
                    if (!"*".equals(targetName)) {
                        player.sendMessage(ChatColor.GREEN + "New Flags: " + chunk.getAllowedFlags(targetName));
                    }
                    player.sendMessage(ChatColor.GOLD + "Use an [owner] sign to see all permission flags");
                } else if (found && "*".equalsIgnoreCase(line2)) {
                    chunk.disallow(targetName, line2.replaceAll(" ", ""));
                    player.sendMessage(ChatColor.GOLD + Lang.get("PermissionsUpdated"));
                    player.sendMessage(ChatColor.WHITE + displayName + ChatColor.GOLD + " has had the following flags removed: " + ChatColor.GREEN + line2.replaceAll(" ",""));
                    if (!"*".equals(line2)) {
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
            } else if (!chunk.getOwner().equalsIgnoreCase(player.getName()) && !(chunk.getOwner().equalsIgnoreCase("server") && player.hasPermission("mychunk.server.signs"))) {
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
            if (!chunk.getOwner().equalsIgnoreCase(player.getName()) && !(chunk.getOwner().equalsIgnoreCase("server") && player.hasPermission("mychunk.server.signs"))) {
                player.sendMessage(ChatColor.RED + Lang.get("DoNotOwn"));
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
            if (!chunk.getOwner().equalsIgnoreCase(player.getName()) && !(chunk.getOwner().equalsIgnoreCase("server") && player.hasPermission("mychunk.server.signs"))) {
                player.sendMessage(ChatColor.RED + Lang.get("DoNotOwn"));
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
        } else {
            Block block = event.getBlock();
            MyChunkChunk chunk = getChunk(block);
            Player player = event.getPlayer();
            if (chunk.isClaimed()) {
                String owner = chunk.getOwner();
                if (!owner.equalsIgnoreCase(player.getName()) && !chunk.isAllowed(player.getName(), "B") && !player.hasPermission("mychunk.override")) {
                    event.setCancelled(true);
                    breakSign(block);
                }
            } else if (plugin.protectUnclaimed && !player.hasPermission("mychunk.override")) {
                event.setCancelled(true);
                breakSign(block);
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onHangingPlace(HangingPlaceEvent event) {
        if (event.isCancelled())
            return;
        Player player = event.getPlayer();
        MyChunkChunk chunk = getChunk(event.getEntity().getLocation().getBlock());
        
        if (chunk.isClaimed() || plugin.protectUnclaimed) {
            if (chunk.isClaimed()) {
                String owner = chunk.getOwner();
                if (!owner.equalsIgnoreCase(player.getName()) && !chunk.isAllowed(player.getName(), "B") && !WorldGuardHook.isRegion(event.getBlock().getLocation())) {
                    if ((!owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.override")) || (owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.server.build"))) {
                        player.sendMessage(ChatColor.RED + Lang.get("NoPermsBuild"));
                        event.setCancelled(true);
                    }
                }
            } else if (!player.hasPermission("mychunk.override")) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + Lang.get("NoPermsBuild"));
            }
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (event.isCancelled())
            return;
        Entity remover = event.getRemover();
        MyChunkChunk chunk = getChunk(event.getEntity().getLocation().getBlock());
        if (chunk.isClaimed() || plugin.protectUnclaimed) {
            if (remover instanceof Player && chunk.isClaimed()) {
                String owner = chunk.getOwner();
                if (!owner.equalsIgnoreCase(((Player)remover).getName())&& !chunk.isAllowed(((Player)remover).getName(), "D") && !WorldGuardHook.isRegion(event.getEntity().getLocation())) {
                    if ((!owner.equalsIgnoreCase("server") && !((Player)remover).hasPermission("mychunk.override")) || (owner.equalsIgnoreCase("server") && !((Player)remover).hasPermission("mychunk.server.destroy"))) {
                    	((Player)remover).sendMessage(ChatColor.RED + Lang.get("NoPermsBreak"));
                        event.setCancelled(true);
                    }
                }
            } else if (!((Player)remover).hasPermission("mychunk.override")) {
            	((Player)remover).sendMessage(ChatColor.RED + Lang.get("NoPermsBreak"));
                event.setCancelled(true);
            }
        }
         
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.isCancelled())
            return;
        Location fromLoc = event.getFrom();
        Location toLoc = event.getTo();
        if (fromLoc.getChunk() != toLoc.getChunk()) {
            MyChunkChunk fromChunk = getChunk(fromLoc.getBlock());
            MyChunkChunk toChunk = getChunk(toLoc.getBlock());
            Player player = event.getPlayer();
            if (!fromChunk.getOwner().equalsIgnoreCase(toChunk.getOwner())) {
                String forSale = "";
                if (toChunk.isForSale()) {
                    forSale = ChatColor.YELLOW + " ["+Lang.get("ChunkForSale");
                    if (plugin.foundEconomy && toChunk.getClaimPrice() != 0) {
                        if (plugin.ownedChunkCount(player.getName()) < plugin.maxChunks || !plugin.allowOverbuy) {
                            forSale += ": " + plugin.vault.economy.format(toChunk.getClaimPrice());
                        } else if (plugin.allowOverbuy && plugin.ownedChunkCount(player.getName()) >= plugin.maxChunks) {
                            forSale += ": " + plugin.vault.economy.format(toChunk.getOverbuyPrice());
                        }
                    }
                    forSale += "]";
                }
                if (!toChunk.isClaimed()) {
                    player.sendMessage(ChatColor.GRAY + "~"+Lang.get("Unowned"));
                } else if (toChunk.getOwner().equalsIgnoreCase("server")) {
                    player.sendMessage(ChatColor.LIGHT_PURPLE + "~"+Lang.get("Server") + forSale);
                } else {
                    player.sendMessage(ChatColor.GOLD + "~" + toChunk.getOwner() + forSale);
                }
            } else if (toChunk.isForSale()) {
                String forSale = ChatColor.YELLOW + "["+Lang.get("ChunkForSale");
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
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.isCancelled())
            return;
        if (event.getBlock().getChunk() != event.getBlock().getRelative(event.getDirection()).getChunk()) {
            String chunk1 = getOwner(event.getBlock().getChunk());
            String chunk2 = getOwner(event.getBlock().getRelative(event.getDirection()).getChunk());
            if (!chunk2.equalsIgnoreCase("Unowned") && !chunk1.equalsIgnoreCase(chunk2)) {
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
                    String chunk1 = getOwner(block.getChunk());
                    String chunk2 = getOwner(block.getRelative(event.getDirection()).getChunk());
                    if (!chunk2.equalsIgnoreCase("Unowned") && !chunk1.equalsIgnoreCase(chunk2)) {
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
                    String chunk1 = getOwner(event.getBlock().getChunk());
                    String chunk2 = getOwner(event.getRetractLocation().getBlock().getChunk());
                    if (!chunk2.equalsIgnoreCase("Unowned") && !chunk1.equalsIgnoreCase(chunk2)) {
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
        if (plugin.useClaimExpiry) {
            refreshOwnership(event.getPlayer().getName());
        }
    }
    
    @EventHandler (priority = EventPriority.NORMAL)
    public void onQuit(PlayerQuitEvent event) {
        if (plugin.useClaimExpiry) {
            refreshOwnership(event.getPlayer().getName());
        }
    }
    
    private void breakSign(Block block) {
        if (block.getTypeId() == 63 || block.getTypeId() == 68) {
            block.setTypeId(0);
            block.getWorld().dropItem(block.getLocation(), new ItemStack(323,1));
        }
    }
    
    private void refreshOwnership(String playerName) {
        plugin.chunkDb.query("UPDATE MyChunks SET lastActive = " + (new Date().getTime() / 1000) + " WHERE owner = '"+playerName+"'");
    }
    
    private MyChunkChunk getChunk(Block block) {
        MyChunkChunk chunk = new MyChunkChunk(block, plugin);
        return chunk;
    }
    
    private MyChunkChunk getChunk(String world, int x, int z) {
        MyChunkChunk chunk = new MyChunkChunk(world,x,z,plugin);
        return chunk;
    }
    
    private boolean isClaimed(Chunk chunk) {
        if (getOwner(chunk).equalsIgnoreCase("Unowned")) {
            return false;
        }
        return true;
    }
    
    private String getOwner(Chunk chunk) {
        HashMap<Integer, HashMap<String, Object>> results = plugin.chunkDb.select("owner", "MyChunks", "world = '"+chunk.getWorld().getName()+"' AND x = "+chunk.getX()+" AND z = " + chunk.getZ(), "", "");
        if (!results.isEmpty()) {
            HashMap<String, Object> result = results.get(0);
            return (String)result.get("owner");
        }
        return "Unowned";
    }
    
}
