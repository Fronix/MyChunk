package me.ellbristow.mychunk.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import me.ellbristow.mychunk.MyChunk;
import me.ellbristow.mychunk.MyChunkChunk;
import me.ellbristow.mychunk.MyChunkVaultLink;
import me.ellbristow.mychunk.WorldGuardHook;
import me.ellbristow.mychunk.utils.SQLiteBridge;
import me.ellbristow.mychunk.lang.Lang;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.ItemStack;

public class SignListener implements Listener {

    private static HashMap<String, Block> pendingAreas = new HashMap<String, Block>();
    private static HashMap<String, Block> pendingUnclaims = new HashMap<String, Block>();

    public SignListener() {
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSignChange(SignChangeEvent event) {

        if (event.isCancelled()) {
            return;
        }

        String line0 = event.getLine(0);
        String line1 = event.getLine(1);

        if (line0.equalsIgnoreCase("[claim]")) {

            // Player attempted to claim a chunk
            Player player = event.getPlayer();
            Block block = event.getBlock();
            MyChunkChunk chunk = new MyChunkChunk(block);

            if (!player.hasPermission("mychunk.claim") && !player.hasPermission("mychunk.claim.server")) {

                player.sendMessage(ChatColor.RED + Lang.get("NoPermsClaim"));
                breakSign(block);
                return;

            } else if (chunk.isClaimed()) {

                String owner = chunk.getOwner();

                if (owner.equalsIgnoreCase(player.getName())) {

                    player.sendMessage(ChatColor.RED + Lang.get("AlreadyOwner"));
                    breakSign(block);
                    return;

                } else if (!chunk.isForSale()) {

                    player.sendMessage(ChatColor.RED + Lang.get("AlreadyOwned") + " " + ChatColor.WHITE + owner + ChatColor.RED + "!");
                    breakSign(block);
                    return;

                } else if (chunk.isForSale() && !player.hasPermission("mychunk.buy")) {

                    player.sendMessage(ChatColor.RED + Lang.get("NoPermsBuyOwned"));
                    breakSign(block);
                    return;

                }

            } else if (!MyChunk.getToggle("allowNether") && player.getWorld().getEnvironment().equals(Environment.NETHER)) {

                player.sendMessage(ChatColor.RED + Lang.get("NoPermsNether"));
                breakSign(block);
                return;

            } else if (!MyChunk.getToggle("allowEnd") && player.getWorld().getEnvironment().equals(Environment.THE_END)) {

                player.sendMessage(ChatColor.RED + Lang.get("NoPermsEnd"));
                breakSign(block);
                return;

            }

            int playerMax = MyChunk.getMaxChunks(player);
            int playerClaimed = MyChunkChunk.getOwnedChunkCount(player.getName());

            if (playerMax != 0 && playerClaimed >= playerMax) {

                player.sendMessage(ChatColor.RED + Lang.get("MaxChunksReached") + " (" + playerMax + ")!");
                breakSign(block);
                return;

            }

            if (MyChunk.getToggle("foundEconomy") && chunk.getClaimPrice() != 0 && !player.hasPermission("mychunk.free") && (playerMax == 0 || MyChunkChunk.getOwnedChunkCount(player.getName()) < playerMax) && MyChunkVaultLink.getEconomy().getBalance(player.getName()) < chunk.getClaimPrice()) {

                player.sendMessage(ChatColor.RED + Lang.get("CantAfford") + " (" + Lang.get("Price") + ": " + ChatColor.WHITE + MyChunkVaultLink.getEconomy().format(chunk.getClaimPrice()) + ChatColor.RED + ")!");
                breakSign(block);
                return;

            } else if (MyChunk.getToggle("foundEconomy") && playerMax != 0 && MyChunkChunk.getOwnedChunkCount(player.getName()) >= playerMax && !player.hasPermission("mychunk.free")) {

                if (MyChunk.getToggle("allowOverbuy") && player.hasPermission("mychunk.claim.overbuy") && MyChunkVaultLink.economy.getBalance(player.getName()) < chunk.getOverbuyPrice()) {

                    player.sendMessage(ChatColor.RED + Lang.get("CantAfford") + " (" + Lang.get("Price") + ": " + ChatColor.WHITE + MyChunkVaultLink.economy.format(chunk.getOverbuyPrice()) + ChatColor.RED + ")!");
                    breakSign(block);
                    return;

                }

            }

            if (line1.equals("") || line1.equalsIgnoreCase(player.getName())) {

                if (!MyChunk.getToggle("allowNeighbours") && chunk.hasNeighbours() && !chunk.isForSale()) {

                    HashMap<Integer, HashMap<String, Object>> results = SQLiteBridge.select("owner", "MyChunks", "world = '" + chunk.getWorldName() + "' AND ((x = " + chunk.getX() + "+1 AND z = " + chunk.getZ() + ") OR (x = " + chunk.getX() + "-1 AND z = " + chunk.getZ() + ") OR (x = " + chunk.getX() + " AND z = " + chunk.getZ() + "+1) OR (x = " + chunk.getX() + " AND z = " + chunk.getZ() + "-1))", "", "");

                    if (!results.isEmpty()) {
                        for (HashMap<String, Object> result : results.values()) {
                            if (result.get("owner").toString().equalsIgnoreCase(player.getName()) || result.get("owner").toString().equalsIgnoreCase("Server")) {
                                continue;
                            }
                            player.sendMessage(ChatColor.RED + Lang.get("NoNeighbours"));
                            breakSign(block);
                            return;
                        }
                    }

                }

                int ownedChunks = MyChunkChunk.getOwnedChunkCount(player.getName());

                if ((ownedChunks < playerMax || (MyChunk.getToggle("allowOverbuy") && player.hasPermission("mychunk.claim.overbuy"))) || player.hasPermission("mychunk.claim.unlimited") || playerMax == 0) {

                    if (MyChunk.getToggle("foundEconomy") && chunk.getClaimPrice() != 0 && !player.hasPermission("mychunk.free") && (playerMax == 0 || MyChunkChunk.getOwnedChunkCount(player.getName()) < playerMax)) {

                        MyChunkVaultLink.getEconomy().withdrawPlayer(player.getName(), chunk.getClaimPrice());
                        player.sendMessage(MyChunkVaultLink.getEconomy().format(chunk.getClaimPrice()) + ChatColor.GOLD + " " + Lang.get("AmountDeducted"));

                    } else if (MyChunk.getToggle("foundEconomy") && MyChunk.getToggle("allowOverbuy") && MyChunkChunk.getOwnedChunkCount(player.getName()) >= playerMax && !player.hasPermission("mychunk.free")) {

                        double price;

                        if (MyChunk.getToggle("overbuyP2P")) {

                            price = chunk.getOverbuyPrice();

                        } else {

                            price = chunk.getClaimPrice();

                        }

                        MyChunkVaultLink.getEconomy().withdrawPlayer(player.getName(), price);
                        player.sendMessage(MyChunkVaultLink.getEconomy().format(price) + ChatColor.GOLD + " " + Lang.get("AmountDeducted"));

                    }

                    if (MyChunk.getToggle("foundEconomy") && chunk.isForSale()) {

                        MyChunkVaultLink.getEconomy().depositPlayer(chunk.getOwner(), chunk.getClaimPrice());
                        OfflinePlayer oldOwner = Bukkit.getServer().getOfflinePlayer(chunk.getOwner());

                        if (oldOwner.isOnline()) {
                            oldOwner.getPlayer().sendMessage(player.getName() + ChatColor.GOLD + " " + Lang.get("BoughtFor") + " " + ChatColor.WHITE + MyChunkVaultLink.getEconomy().format(chunk.getClaimPrice()) + ChatColor.GOLD + "!");
                        }

                    }

                    chunk.claim(player.getName());
                    player.sendMessage(ChatColor.GOLD + Lang.get("ChunkClaimed"));

                } else {

                    player.sendMessage(ChatColor.RED + Lang.get("AlreadyOwn") + " " + ownedChunks + " " + Lang.get("Chunks") + "! (Max " + playerMax + ")");

                }

                breakSign(block);

            } else {

                String correctName;

                if (line1.equalsIgnoreCase("server")) {

                    if (!player.hasPermission("mychunk.claim.server")) {

                        player.sendMessage(ChatColor.RED + Lang.get("NoPermsClaimServer"));
                        breakSign(block);
                        return;

                    } else {
                        correctName = "Server";
                    }

                } else if (player.hasPermission("mychunk.claim.others")) {

                    if (!MyChunk.getToggle("allowNeighbours") && chunk.hasNeighbours() && !chunk.isForSale()) {

                        MyChunkChunk[] neighbours = chunk.getNeighbours();

                        if (!neighbours[0].getOwner().equalsIgnoreCase(line1) || !neighbours[1].getOwner().equalsIgnoreCase(line1) || !neighbours[2].getOwner().equalsIgnoreCase(line1) || !neighbours[3].getOwner().equalsIgnoreCase(line1)) {

                            player.sendMessage(ChatColor.RED + Lang.get("NoNeighbours"));
                            breakSign(block);
                            return;

                        }

                    }

                    OfflinePlayer target = Bukkit.getServer().getOfflinePlayer(line1);

                    if (!target.hasPlayedBefore()) {

                        player.sendMessage(ChatColor.RED + Lang.get("Player") + " " + ChatColor.WHITE + line1 + ChatColor.RED + " " + Lang.get("NotFound") + "!");
                        breakSign(block);
                        return;

                    } else {
                        correctName = target.getName();
                    }

                } else {

                    player.sendMessage(ChatColor.RED + Lang.get("NoPermsClaimOther"));
                    breakSign(block);
                    return;

                }


                chunk.claim(correctName);
                player.sendMessage(ChatColor.GOLD + Lang.get("ChunkClaimedFor") + " " + ChatColor.WHITE + correctName + ChatColor.GOLD + "!");

                if (MyChunk.getToggle("foundEconomy") && MyChunk.getDoubleSetting("chunkPrice") != 0 && !correctName.equalsIgnoreCase("server") && !player.hasPermission("mychunk.free")) {

                    MyChunkVaultLink.getEconomy().withdrawPlayer(player.getName(), MyChunk.getDoubleSetting("chunkPrice"));
                    player.sendMessage(MyChunkVaultLink.getEconomy().format(MyChunk.getDoubleSetting("chunkPrice")) + ChatColor.GOLD + " " + Lang.get("AmountDeducted"));

                }

                breakSign(block);

            }

        } else if (line0.equalsIgnoreCase("[ClaimArea]")) {

            Player player = event.getPlayer();
            Block block = event.getBlock();

            if (!player.hasPermission("mychunk.claim")) {

                player.sendMessage(ChatColor.RED + Lang.get("NoPermsClaim"));
                breakSign(block);
                return;

            } else if (!player.hasPermission("mychunk.claim.area")) {

                player.sendMessage(ChatColor.RED + Lang.get("NoPermsClaimArea"));
                breakSign(block);
                return;

            } else if (!MyChunk.getToggle("allowNether") && block.getWorld().getEnvironment().equals(Environment.NETHER)) {

                player.sendMessage(ChatColor.RED + Lang.get("NoPermsNether"));
                breakSign(block);
                return;

            } else if (!MyChunk.getToggle("allowEnd") && block.getWorld().getEnvironment().equals(Environment.THE_END)) {

                player.sendMessage(ChatColor.RED + Lang.get("NoPermsEnd"));
                breakSign(block);
                return;

            }

            String correctName;

            if (line1.isEmpty() || line1.equalsIgnoreCase(player.getName())) {

                correctName = player.getName();

            } else {

                if (line1.equalsIgnoreCase("Server")) {

                    if (!player.hasPermission("mychunk.claim.server")) {

                        player.sendMessage(ChatColor.RED + Lang.get("NoPermsClaimServer"));
                        breakSign(block);
                        return;

                    } else {
                        correctName = "Server";
                    }

                } else {

                    if (player.hasPermission("mychunk.claim.others")) {

                        OfflinePlayer target = Bukkit.getServer().getOfflinePlayer(line1);
                        if (!target.hasPlayedBefore()) {

                            player.sendMessage(ChatColor.RED + Lang.get("Player") + " " + ChatColor.WHITE + line1 + ChatColor.RED + " " + Lang.get("NotFound") + "!");
                            breakSign(block);
                            return;

                        } else {
                            correctName = target.getName();
                        }

                    } else {

                        player.sendMessage(ChatColor.RED + Lang.get("NoPermsClaimOther"));
                        breakSign(block);
                        return;

                    }
                }
            }

            if (event.getLine(2).equalsIgnoreCase("cancel")) {

                pendingAreas.remove(correctName);
                player.sendMessage(ChatColor.RED + Lang.get("ClaimAreaCancelled"));
                breakSign(block);
                return;

            }

            if (!pendingAreas.containsKey(correctName)) {

                pendingAreas.put(correctName, event.getBlock());
                player.sendMessage(ChatColor.GOLD + Lang.get("StartClaimArea1"));
                player.sendMessage(ChatColor.GOLD + Lang.get("StartClaimArea2"));

            } else {

                Block startBlock = pendingAreas.get(correctName);

                if (startBlock.getWorld() != block.getWorld()) {
                    player.sendMessage(ChatColor.RED + Lang.get("ClaimAreaWorldError"));
                    breakSign(block);
                    return;
                }

                Chunk startChunk = startBlock.getChunk();
                pendingAreas.remove(correctName);
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
                List<MyChunkChunk> foundChunks = new ArrayList<MyChunkChunk>();
                int chunkCount = 0;
                xloop:
                for (int x = startX; x <= endX; x++) {

                    for (int z = startZ; z <= endZ; z++) {

                        if (chunkCount < 64) {

                            MyChunkChunk myChunk = new MyChunkChunk(block.getWorld().getName(), x, z);

                            if (myChunk.isClaimed() && !myChunk.getOwner().equalsIgnoreCase(correctName) && !myChunk.isForSale()) {

                                foundClaimed = true;
                                break xloop;

                            } else if (myChunk.hasNeighbours()) {

                                MyChunkChunk[] neighbours = myChunk.getNeighbours();

                                for (MyChunkChunk neighbour : neighbours) {

                                    if (neighbour.isClaimed() && !neighbour.getOwner().equalsIgnoreCase(correctName) && !neighbour.getOwner().equalsIgnoreCase("Server") && !myChunk.isForSale()) {

                                        foundNeighbour = true;
                                        if (!MyChunk.getToggle("allowNeighbours")) {
                                            break xloop;
                                        }

                                    }

                                }
                            }

                            foundChunks.add(myChunk);
                            chunkCount++;

                        } else {

                            player.sendMessage(ChatColor.RED + Lang.get("AreaTooBig"));
                            breakSign(block);
                            return;

                        }

                    }

                }

                if (foundClaimed) {

                    player.sendMessage(ChatColor.RED + Lang.get("FoundClaimedInArea"));
                    breakSign(block);
                    return;

                }
                if (foundNeighbour && !MyChunk.getToggle("allowNeighbours")) {

                    player.sendMessage(ChatColor.RED + Lang.get("FoundNeighboursInArea"));
                    breakSign(block);
                    return;

                }

                int claimed = MyChunkChunk.getOwnedChunkCount(correctName);
                int max = MyChunk.getMaxChunks(player);

                if (max != 0 && (!MyChunk.getToggle("allowOverbuy") || !player.hasPermission("mychunk.claim.overbuy")) && max - claimed < foundChunks.size()) {

                    player.sendMessage(ChatColor.RED + (correctName.equalsIgnoreCase(player.getName()) ? "You" : correctName) + Lang.get("ClaimAreaTooLarge"));
                    player.sendMessage(ChatColor.RED + Lang.get("ChunksOwned") + ": " + ChatColor.WHITE + claimed);
                    player.sendMessage(ChatColor.RED + Lang.get("ChunkMax") + ": " + ChatColor.WHITE + max);
                    player.sendMessage(ChatColor.RED + Lang.get("ChunksInArea") + ": " + chunkCount);
                    breakSign(block);
                    return;

                }

                int allowance = max - claimed;
                if (allowance < 0) {
                    allowance = 0;
                }

                if (MyChunk.getToggle("foundEconomy") && !player.hasPermission("mychunk.free") && !correctName.equalsIgnoreCase("Server")) {

                    double areaPrice = 0;

                    for (MyChunkChunk myChunk : foundChunks) {

                        if (allowance > 0) {

                            areaPrice += myChunk.getClaimPrice();
                            allowance--;

                        } else {
                            areaPrice += myChunk.getOverbuyPrice();
                        }

                    }

                    if (MyChunkVaultLink.getEconomy().getBalance(player.getName()) < areaPrice) {

                        player.sendMessage(ChatColor.RED + Lang.get("CantAffordClaimArea"));
                        player.sendMessage(ChatColor.RED + Lang.get("Price") + ": " + ChatColor.WHITE + MyChunkVaultLink.getEconomy().format(areaPrice));
                        breakSign(block);
                        return;

                    }

                    MyChunkVaultLink.getEconomy().withdrawPlayer(player.getName(), areaPrice);
                    player.sendMessage(ChatColor.GOLD + Lang.get("YouWereCharged") + " " + ChatColor.WHITE + MyChunkVaultLink.getEconomy().format(areaPrice));

                }

                for (MyChunkChunk myChunk : foundChunks) {
                    myChunk.claim(correctName);
                }

                player.sendMessage(ChatColor.GOLD + Lang.get("ChunksClaimed") + ": " + ChatColor.WHITE + foundChunks.size());

            }

            breakSign(block);

        } else if (line0.equalsIgnoreCase("[unclaim]")) {

            // Player attempted to unclaim a chunk

            Player player = event.getPlayer();
            Block block = event.getBlock();
            MyChunkChunk chunk = new MyChunkChunk(block);

            if (!chunk.isClaimed()) {

                player.sendMessage(ChatColor.RED + Lang.get("ChunkNotOwned"));
                breakSign(block);
                return;

            }

            String owner = chunk.getOwner();

            if (!owner.equalsIgnoreCase(player.getName())) {

                if (owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.unclaim.server")) {

                    player.sendMessage(ChatColor.RED + Lang.get("NoPermsUnclaimServer"));
                    breakSign(block);
                    return;

                } else if (!owner.equalsIgnoreCase("server") && !player.hasPermission("mychunk.unclaim.others")) {

                    player.sendMessage(ChatColor.RED + Lang.get("NoPermsUnclaimOther"));
                    breakSign(block);
                    return;

                }

            }

            chunk.unclaim();

            if (owner.equalsIgnoreCase(player.getName())) {

                player.sendMessage(ChatColor.GOLD + Lang.get("ChunkUnclaimed"));

            } else {

                player.sendMessage(ChatColor.GOLD + Lang.get("ChunkUnclaimedFor") + " " + ChatColor.WHITE + owner + ChatColor.RED + "!");

            }

            if (MyChunk.getToggle("unclaimRefund") && !player.hasPermission("mychunk.free")) {

                MyChunkVaultLink.getEconomy().depositPlayer(player.getName(), MyChunk.getDoubleSetting("chunkPrice"));

            }

            breakSign(block);

        } else if (line0.equalsIgnoreCase("[unclaimarea]")) {

            Player player = event.getPlayer();
            Block block = event.getBlock();

            String correctName;

            if (line1.isEmpty() || line1.equalsIgnoreCase(player.getName())) {

                correctName = player.getName();

            } else {

                if (line1.equalsIgnoreCase("Server")) {

                    if (!player.hasPermission("mychunk.unclaim.server")) {

                        player.sendMessage(ChatColor.RED + Lang.get("NoPermsUnclaimServer"));
                        breakSign(block);
                        return;

                    } else {
                        correctName = "Server";
                    }

                } else {

                    if (player.hasPermission("mychunk.unclaim.others")) {

                        OfflinePlayer target = Bukkit.getServer().getOfflinePlayer(line1);
                        if (!target.hasPlayedBefore()) {

                            player.sendMessage(ChatColor.RED + Lang.get("Player") + " " + ChatColor.WHITE + line1 + ChatColor.RED + " " + Lang.get("NotFound") + "!");
                            breakSign(block);
                            return;

                        } else {
                            correctName = target.getName();
                        }

                    } else {

                        player.sendMessage(ChatColor.RED + Lang.get("NoPermsUnclaimOther"));
                        breakSign(block);
                        return;

                    }
                }

            }

            if (event.getLine(2).equalsIgnoreCase("cancel")) {

                pendingAreas.remove(correctName);
                player.sendMessage(ChatColor.RED + Lang.get("UnclaimAreaCancelled"));
                breakSign(block);
                return;

            }

            if (!pendingUnclaims.containsKey(correctName)) {

                pendingUnclaims.put(correctName, event.getBlock());
                player.sendMessage(ChatColor.GOLD + Lang.get("StartUnclaimArea1"));
                player.sendMessage(ChatColor.GOLD + Lang.get("StartUnclaimArea2"));

            } else {

                Block startBlock = pendingUnclaims.get(correctName);

                if (startBlock.getWorld() != block.getWorld()) {
                    player.sendMessage(ChatColor.RED + Lang.get("UnclaimAreaWorldError"));
                    breakSign(block);
                    return;
                }

                Chunk startChunk = startBlock.getChunk();
                pendingUnclaims.remove(correctName);
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

                List<MyChunkChunk> foundChunks = new ArrayList<MyChunkChunk>();
                int chunkCount = 0;
                xloop:
                for (int x = startX; x <= endX; x++) {

                    for (int z = startZ; z <= endZ; z++) {

                        if (chunkCount < 64) {

                            MyChunkChunk myChunk = new MyChunkChunk(block.getWorld().getName(), x, z);

                            if (myChunk.isClaimed() && myChunk.getOwner().equalsIgnoreCase(correctName)) {

                                foundChunks.add(myChunk);
                                chunkCount++;

                            }

                        } else {

                            player.sendMessage(ChatColor.RED + Lang.get("UnclaimAreaTooBig"));
                            breakSign(block);
                            return;

                        }

                    }

                }

                if (foundChunks.isEmpty()) {
                    player.sendMessage(ChatColor.RED + Lang.get("UnclaimAreaNoneFound"));
                    breakSign(block);
                    return;
                }

                if (MyChunk.getToggle("foundEconomy") && !player.hasPermission("mychunk.free") && !correctName.equalsIgnoreCase("Server") && MyChunk.getToggle("unclaimRefund")) {

                    MyChunkVaultLink.getEconomy().depositPlayer(player.getName(), MyChunk.getDoubleSetting("chunkPrice") * chunkCount);

                    player.sendMessage(ChatColor.GOLD + Lang.get("YouWereCharged") + " " + ChatColor.WHITE + MyChunkVaultLink.getEconomy().format(MyChunk.getDoubleSetting("chunkPrice") * chunkCount));

                }

                for (MyChunkChunk myChunk : foundChunks) {
                    myChunk.unclaim();
                }

                player.sendMessage(ChatColor.GOLD + Lang.get("ChunksUnclaimed") + ": " + ChatColor.WHITE + foundChunks.size());

            }

            breakSign(block);

        } else if (line0.equalsIgnoreCase("[owner]")) {

            // Player requested chunk's Owner info
            Player player = event.getPlayer();
            Block block = event.getBlock();
            MyChunkChunk chunk = new MyChunkChunk(block);

            if (chunk.isClaimed()) {

                String owner = chunk.getOwner();

                if (owner.equalsIgnoreCase(player.getName())) {

                    player.sendMessage(ChatColor.GOLD + Lang.get("YouOwn"));
                    player.sendMessage(ChatColor.GREEN + Lang.get("AllowedPlayers") + ": " + chunk.getAllowed());

                } else {

                    player.sendMessage(ChatColor.GOLD + Lang.get("OwnedBy") + " " + ChatColor.WHITE + owner + ChatColor.GOLD + "!");
                    player.sendMessage(ChatColor.GREEN + Lang.get("AllowedPlayers") + ": " + chunk.getAllowed());

                }

            } else {
                player.sendMessage(ChatColor.GOLD + Lang.get("ChunkIs") + " " + ChatColor.WHITE + Lang.get("Unowned") + ChatColor.GOLD + "!");
            }

            breakSign(block);

        } else if (line0.equalsIgnoreCase("[allow]")) {

            // Player attempted to add a player allowance
            Player player = event.getPlayer();
            Block block = event.getBlock();
            MyChunkChunk chunk = new MyChunkChunk(block);

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

                String targetName = "*";

                if (!"*".equalsIgnoreCase(line1)) {

                    OfflinePlayer target = Bukkit.getServer().getOfflinePlayer(line1);

                    if (!target.hasPlayedBefore()) {

                        player.sendMessage(ChatColor.RED + Lang.get("Player") + " " + ChatColor.WHITE + line1 + ChatColor.RED + " " + Lang.get("NotFound") + "!");
                        breakSign(block);
                        return;

                    } else {
                        targetName = target.getName();
                    }

                }

                String displayName = targetName;
                if (displayName.equals("*")) {
                    displayName = Lang.get("Everyone");
                }

                if (!"*".equalsIgnoreCase(line2)) {

                    String errors = "";

                    for (int i = 0; i < line2.length(); i++) {

                        String thisChar = line2.substring(i, i + 1).replaceAll(" ", "");

                        if (MyChunkChunk.isFlag(thisChar.toUpperCase())) {
                            chunk.allow(targetName, thisChar);
                        } else {
                            errors += thisChar;
                        }

                    }

                    player.sendMessage(ChatColor.GOLD + Lang.get("PermissionsUpdated"));
                    // TODO: Lang
                    if (!"".equals(errors)) {
                        player.sendMessage(ChatColor.RED + "Flags not found: " + errors);
                    }

                    player.sendMessage(ChatColor.WHITE + displayName + ChatColor.GOLD + " has had the following flags added: " + ChatColor.GREEN + line2.replaceAll(" ", ""));

                    if (!"*".equals(targetName)) {
                        player.sendMessage(ChatColor.GREEN + "Allowed: " + chunk.getAllowedFlags(targetName));
                    }

                    player.sendMessage(ChatColor.GOLD + "Use an [owner] sign to see all permission flags");

                } else {

                    chunk.allow(targetName, line2.replaceAll(" ", ""));
                    player.sendMessage(ChatColor.GOLD + Lang.get("PermissionsUpdated"));
                    player.sendMessage(ChatColor.WHITE + displayName + ChatColor.GOLD + " has had the following flags added: " + ChatColor.GREEN + line2.replaceAll(" ", ""));

                    if (!"*".equals(line2)) {
                        player.sendMessage(ChatColor.GREEN + "New Flags: " + chunk.getAllowedFlags(targetName));
                    }

                    player.sendMessage(ChatColor.GOLD + "Use an [owner] sign to see all permission flags");

                }

            }

            breakSign(block);

        } else if (line0.equalsIgnoreCase("[allow*]")) {

            Player player = event.getPlayer();
            Block block = event.getBlock();

            if (Lang.get("Everyone").equalsIgnoreCase(line1.toUpperCase())) {
                line1 = "*";
            }

            String line2 = event.getLine(2).toUpperCase();

            if ("".equals(line1) || line1.contains(" ")) {
                player.sendMessage(ChatColor.RED + Lang.get("Line2Player"));
            } else if (line1.equalsIgnoreCase(player.getName())) {
                player.sendMessage(ChatColor.RED + Lang.get("AllowSelf"));
            } else {

                if ("".equals(line2)) {
                    line2 = "*";
                }

                String targetName = "*";

                if (!"*".equalsIgnoreCase(line1)) {

                    OfflinePlayer target = Bukkit.getServer().getOfflinePlayer(line1);

                    if (!target.hasPlayedBefore()) {

                        player.sendMessage(ChatColor.RED + Lang.get("Player") + " " + ChatColor.WHITE + line1 + ChatColor.RED + " " + Lang.get("NotFound") + "!");
                        breakSign(block);
                        return;

                    } else {
                        targetName = target.getName();
                    }

                }

                String displayName = targetName;

                if (displayName.equals("*")) {
                    displayName = Lang.get("Everyone");
                }

                HashMap<Integer, HashMap<String, Object>> results = SQLiteBridge.select("world,x,z", "MyChunks", "owner = '" + player.getName() + "'", "", "");

                if (results.isEmpty()) {

                    player.sendMessage(ChatColor.RED + Lang.get("NoChunksOwned"));
                    breakSign(block);
                    return;

                }

                String errors = "";

                for (int i = 0; i < line2.length(); i++) {

                    String thisChar = line2.substring(i, i + 1).replaceAll(" ", "");

                    if (!MyChunkChunk.isFlag(thisChar.toUpperCase())) {
                        errors += thisChar;
                    }

                }

                if (!"".equals(errors)) {
                    player.sendMessage(ChatColor.RED + "Flags not found: " + errors);
                }

                for (HashMap<String, Object> result : ((HashMap<Integer, HashMap<String, Object>>) results.clone()).values()) {

                    MyChunkChunk myChunk = new MyChunkChunk(result.get("world").toString(), Integer.parseInt(result.get("x").toString()), Integer.parseInt(result.get("z").toString()));

                    if (!"*".equalsIgnoreCase(line2)) {

                        for (int i = 0; i < line2.length(); i++) {

                            String thisChar = line2.substring(i, i + 1).replaceAll(" ", "");

                            if (MyChunkChunk.isFlag(thisChar.toUpperCase())) {
                                myChunk.allow(targetName, thisChar);
                            }

                        }

                    } else {

                        myChunk.allow(targetName, line2.replaceAll(" ", ""));

                    }

                }

                player.sendMessage(ChatColor.GOLD + Lang.get("PermissionsUpdated"));
                player.sendMessage(ChatColor.WHITE + displayName + ChatColor.GOLD + " has had the following flags added to all your chunks: " + ChatColor.GREEN + line2.replaceAll(" ", ""));
                player.sendMessage(ChatColor.GOLD + "Use an [owner] sign to see all permission flags");

            }

            breakSign(block);

        } else if (line0.equalsIgnoreCase("[disallow]")) {

            // Player attempted to add a player allowance
            Player player = event.getPlayer();
            Block block = event.getBlock();
            MyChunkChunk chunk = new MyChunkChunk(block);

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
            } else if (!"*".equals(line1) && chunk.isAllowed("*", line2)) {
                player.sendMessage(ChatColor.RED + "You cannot disallow flags allowed to EVERYONE!");
            } else {

                if ("".equals(line2)) {
                    line2 = "*";
                }

                String targetName = "*";

                if (!"*".equalsIgnoreCase(line1)) {

                    OfflinePlayer target = Bukkit.getServer().getOfflinePlayer(line1);

                    if (!target.hasPlayedBefore()) {

                        player.sendMessage(ChatColor.RED + Lang.get("Player") + " " + ChatColor.WHITE + line1 + ChatColor.RED + " " + Lang.get("NotFound") + "!");
                        breakSign(block);
                        return;

                    } else {
                        targetName = target.getName();
                    }

                }

                String displayName = targetName;

                if (displayName.equals("*")) {
                    displayName = Lang.get("Everyone");
                }

                if (!"*".equalsIgnoreCase(line2)) {

                    String errors = "";

                    for (int i = 0; i < line2.length(); i++) {

                        String thisChar = line2.substring(i, i + 1).replaceAll(" ", "");

                        if (MyChunkChunk.isFlag(thisChar.toUpperCase())) {
                            chunk.disallow(targetName, thisChar);
                        } else {
                            errors += thisChar;
                        }

                    }

                    player.sendMessage(ChatColor.GOLD + Lang.get("PermissionsUpdated"));

                    if (!"".equals(errors)) {
                        player.sendMessage(ChatColor.RED + "Flags not found: " + errors);
                    }

                    player.sendMessage(ChatColor.WHITE + displayName + ChatColor.GOLD + " has had the following flags removed: " + ChatColor.GREEN + line2.replaceAll(" ", ""));

                    if (!"*".equals(targetName)) {
                        player.sendMessage(ChatColor.GREEN + "New Flags: " + chunk.getAllowedFlags(targetName));
                    }

                    player.sendMessage(ChatColor.GOLD + "Use an [owner] sign to see all permission flags");

                } else {

                    chunk.disallow(targetName, line2.replaceAll(" ", ""));
                    player.sendMessage(ChatColor.GOLD + Lang.get("PermissionsUpdated"));
                    player.sendMessage(ChatColor.WHITE + displayName + ChatColor.GOLD + " has had the following flags removed: " + ChatColor.GREEN + line2.replaceAll(" ", ""));

                    if (!"*".equals(line2)) {
                        player.sendMessage(ChatColor.GREEN + "New Flags: " + chunk.getAllowedFlags(targetName));
                    }

                    player.sendMessage(ChatColor.GOLD + "Use an [owner] sign to see all permission flags");

                }

            }

            breakSign(block);

        } else if (line0.equalsIgnoreCase("[disallow*]")) {

            Player player = event.getPlayer();
            Block block = event.getBlock();

            if (Lang.get("Everyone").equalsIgnoreCase(line1.toUpperCase())) {
                line1 = "*";
            }

            String line2 = event.getLine(2).toUpperCase();

            if ("".equals(line1) || line1.contains(" ")) {
                player.sendMessage(ChatColor.RED + Lang.get("Line2Player"));
            } else if (line1.equalsIgnoreCase(player.getName())) {
                player.sendMessage(ChatColor.RED + "You cannot disallow yourself!");
            } else {

                if ("".equals(line2)) {
                    line2 = "*";
                }

                String targetName = "*";

                if (!"*".equalsIgnoreCase(line1)) {

                    OfflinePlayer target = Bukkit.getServer().getOfflinePlayer(line1);

                    if (!target.hasPlayedBefore()) {

                        player.sendMessage(ChatColor.RED + Lang.get("Player") + " " + ChatColor.WHITE + line1 + ChatColor.RED + " " + Lang.get("NotFound") + "!");
                        breakSign(block);
                        return;

                    } else {
                        targetName = target.getName();
                    }

                }

                String displayName = targetName;

                if (displayName.equals("*")) {
                    displayName = Lang.get("Everyone");
                }

                HashMap<Integer, HashMap<String, Object>> results = SQLiteBridge.select("world,x,z", "MyChunks", "owner = '" + player.getName() + "'", "", "");

                if (results.isEmpty()) {

                    player.sendMessage(ChatColor.RED + Lang.get("NoChunksOwned"));
                    breakSign(block);
                    return;

                }

                String errors = "";

                for (int i = 0; i < line2.length(); i++) {

                    String thisChar = line2.substring(i, i + 1).replaceAll(" ", "");

                    if (!MyChunkChunk.isFlag(thisChar.toUpperCase())) {
                        errors += thisChar;
                    }

                }

                if (!"".equals(errors)) {
                    player.sendMessage(ChatColor.RED + "Flags not found: " + errors);
                }

                for (HashMap<String, Object> result : ((HashMap<Integer, HashMap<String, Object>>) results.clone()).values()) {

                    MyChunkChunk chunk = new MyChunkChunk(result.get("world").toString(), Integer.parseInt(result.get("x").toString()), Integer.parseInt(result.get("z").toString()));

                    if (!"*".equalsIgnoreCase(line2)) {

                        for (int i = 0; i < line2.length(); i++) {

                            String thisChar = line2.substring(i, i + 1).replaceAll(" ", "");

                            if (MyChunkChunk.isFlag(thisChar.toUpperCase())) {
                                chunk.disallow(targetName, thisChar);
                            }

                        }

                    } else {

                        chunk.disallow(targetName, line2.replaceAll(" ", ""));

                    }

                }

                player.sendMessage(ChatColor.GOLD + Lang.get("PermissionsUpdated"));
                player.sendMessage(ChatColor.WHITE + displayName + ChatColor.GOLD + " has had the following flags removed from all your chunks: " + ChatColor.GREEN + line2.replaceAll(" ", ""));
                player.sendMessage(ChatColor.GOLD + "Use an [owner] sign to see all permission flags");

            }

            breakSign(block);

        } else if (line0.equalsIgnoreCase("[for sale]")) {

            Player player = event.getPlayer();
            Block block = event.getBlock();

            MyChunkChunk chunk = new MyChunkChunk(block);
            Double price = 0.00;

            if (!player.hasPermission("mychunk.sell")) {

                player.sendMessage(ChatColor.RED + "You do not have permission to use [For Sale] signs!");
                breakSign(block);
                return;

            } else if (player.hasPermission("mychunk.free")) {

                player.sendMessage(ChatColor.RED + "You can claim chunks for free! You're not allowed to sell them!");
                breakSign(block);
                return;

            } else if (!chunk.getOwner().equalsIgnoreCase(player.getName()) && !(chunk.getOwner().equalsIgnoreCase("server") && player.hasPermission("mychunk.server.signs"))) {

                player.sendMessage(ChatColor.RED + "You can't sell this chunk, you don't own it!");
                breakSign(block);
                return;

            } else if (MyChunk.getToggle("foundEconomy")) {

                if (line1.isEmpty() || line1.equals("")) {

                    player.sendMessage(ChatColor.RED + "Line 2 must contain your sale price!");
                    breakSign(block);
                    return;

                } else {

                    try {
                        price = Double.parseDouble(line1);
                    } catch (NumberFormatException nfe) {

                        player.sendMessage(ChatColor.RED + "Line 2 must contain your sale price (in #.## format)!");
                        breakSign(block);
                        return;

                    }

                    if (price == 0) {

                        player.sendMessage(ChatColor.RED + "Sale price cannot be 0!");
                        breakSign(block);
                        return;

                    }

                }

            }

            if (MyChunk.getToggle("foundEconomy")) {

                player.sendMessage(ChatColor.GOLD + "Chunk on sale for " + MyChunkVaultLink.getEconomy().format(price) + "!");
                chunk.setForSale(price);

            } else {

                player.sendMessage(ChatColor.GOLD + "Chunk on sale!");
                chunk.setForSale(MyChunk.getDoubleSetting("chunkPrice"));

            }

            breakSign(block);

        } else if (line0.equalsIgnoreCase("[not for sale]")) {

            Player player = event.getPlayer();
            Block block = event.getBlock();

            MyChunkChunk chunk = new MyChunkChunk(block);

            if (!chunk.getOwner().equalsIgnoreCase(player.getName()) && !(chunk.getOwner().equalsIgnoreCase("server") && player.hasPermission("mychunk.server.signs"))) {

                player.sendMessage(ChatColor.RED + Lang.get("DoNotOwn"));
                breakSign(block);
                return;

            } else if (!chunk.isForSale()) {

                player.sendMessage(ChatColor.RED + "This chunk is not for sale!");
                breakSign(block);
                return;

            }

            player.sendMessage(ChatColor.GOLD + "Chunk taken off sale!");
            chunk.setNotForSale();
            breakSign(block);

        } else if (line0.equalsIgnoreCase("[AllowMobs]")) {

            Player player = event.getPlayer();
            Block block = event.getBlock();

            MyChunkChunk chunk = new MyChunkChunk(block);

            if (!chunk.getOwner().equalsIgnoreCase(player.getName()) && !(chunk.getOwner().equalsIgnoreCase("server") && player.hasPermission("mychunk.server.signs"))) {

                player.sendMessage(ChatColor.RED + Lang.get("DoNotOwn"));
                breakSign(block);
                return;

            }

            if (!player.hasPermission("mychunk.allowmobs")) {

                player.sendMessage(ChatColor.RED + "You do not have permission to use [AllowMobs] signs!");
                breakSign(block);
                return;

            }

            if (!line1.equalsIgnoreCase("on") && !line1.equalsIgnoreCase("off")) {

                player.sendMessage(ChatColor.RED + "Line 2 must say either 'on' or 'off'!");
                breakSign(block);
                return;

            }

            if (line1.equalsIgnoreCase("on")) {

                chunk.setAllowMobs(true);
                player.sendMessage(ChatColor.GOLD + "Mobs now CAN spawn in this chunk!");

            } else {

                chunk.setAllowMobs(false);
                player.sendMessage(ChatColor.GOLD + "Mobs now CAN NOT spawn in this chunk!");

            }

            breakSign(event.getBlock());

        } else {

            Block block = event.getBlock();
            MyChunkChunk chunk = new MyChunkChunk(block);
            Player player = event.getPlayer();
            
            if(!WorldGuardHook.isRegion(block.getLocation())){

                if (chunk.isClaimed()) {
    
                    String owner = chunk.getOwner();
                    if (!owner.equalsIgnoreCase(player.getName()) && !chunk.isAllowed(player.getName(), "B") && !player.hasPermission("mychunk.override")) {
                        event.setCancelled(true);
                        breakSign(block);
                    }
    
                } else if (MyChunk.getToggle("protectUnclaimed") && !player.hasPermission("mychunk.override")) {
    
                    event.setCancelled(true);
                    breakSign(block);
    
                    if (!WorldGuardHook.isRegion(block.getLocation())) {
                        if (chunk.isClaimed()) {
                            String owner = chunk.getOwner();
                            if (!owner.equalsIgnoreCase(player.getName()) && !chunk.isAllowed(player.getName(), "B") && !player.hasPermission("mychunk.override")) {
                                event.setCancelled(true);
                                breakSign(block);
                            }
                        } else if (MyChunk.getToggle("protectUnclaimed") && !player.hasPermission("mychunk.override")) {
                            event.setCancelled(true);
                            breakSign(block);
                        }
                    }
    
                }
            }
        }

    }

    private void breakSign(Block block) {

        if (block.getTypeId() == 63 || block.getTypeId() == 68) {

            block.setTypeId(0);
            block.getWorld().dropItem(block.getLocation(), new ItemStack(323, 1));

        }

    }
}
