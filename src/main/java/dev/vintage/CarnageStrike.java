package dev.vintage;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.bukkit.event.entity.EntityPlaceEvent;

import com.cryptomorin.xseries.XPotion;

import java.util.*;

public class CarnageStrike extends JavaPlugin implements Listener {

    private static final int CUSTOM_MODEL_DATA = 12345;
    private static final String[] STRIKE_TYPES = {"nuke", "stab", "dogs", "chunkeater"};
    private static final UUID AUTHORIZED_UUID = UUID.fromString("9eb018b0-b647-462a-aef2-f3d6573a7d02"); // Replace with your UUID
    private static final String AUTHORIZED_USERNAME = "ignVintage"; // Replace with your username

    private final Map<UUID, Set<UUID>> strikeTNT = new HashMap<>();
    private final Map<UUID, String> pendingStrikes = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        strikeTNT.clear();
        pendingStrikes.clear();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!isAuthorized(player)) return;

        String message = event.getMessage().toLowerCase();
        if (message.startsWith("?ob ")) {
            String[] parts = message.split(" ");
            if (parts.length == 2) {
                String type = parts[1];
                if (isValidStrikeType(type)) {
                    giveStrikeRod(player, type);
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().toString().contains("RIGHT") || !event.hasItem()) {
            return;
        }

        ItemStack item = event.getItem();
        if (!isStrikeRod(item)) {
            return;
        }

        String type = getStrikeType(item);
        if (type == null) return;

        Player player = event.getPlayer();

        if (type.equals("chunkeater")) {
            return;
        }

        Location target = getTargetLocation(player);

        if (target == null) {
            return;
        }

        boolean throwRod = true;

        if (throwRod) {
            pendingStrikes.put(player.getUniqueId(), type);
        } else {
            executeStrike(player, item, type, target);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (!true) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (!pendingStrikes.containsKey(playerId)) {
            return;
        }

        if (event.getState() != PlayerFishEvent.State.REEL_IN &&
                event.getState() != PlayerFishEvent.State.FAILED_ATTEMPT &&
                event.getState() != PlayerFishEvent.State.CAUGHT_ENTITY &&
                event.getState() != PlayerFishEvent.State.IN_GROUND &&
                event.getState() != PlayerFishEvent.State.BITE) {
            return;
        }

        String type = pendingStrikes.remove(playerId);

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        boolean consumed = false;
        if (mainHand.getType() == Material.FISHING_ROD && isStrikeRod(mainHand)) {
            mainHand.setAmount(mainHand.getAmount() - 1);
            consumed = true;
        } else if (offHand.getType() == Material.FISHING_ROD && isStrikeRod(offHand)) {
            offHand.setAmount(offHand.getAmount() - 1);
            consumed = true;
        }

        if (consumed) {
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
        }

        Location target = getTargetLocation(player);
        if (target == null) return;

        executeStrike(player, new ItemStack(Material.FISHING_ROD), type, target);
    }

    @EventHandler
    public void onTNTLand(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof TNTPrimed tnt)) {
            return;
        }

        UUID tntId = tnt.getUniqueId();
        if (!isTrackedTNT(tntId)) {
            return;
        }

        event.setCancelled(true);

        if (event.getBlock().getType().isSolid()) {
            removeFromTracking(tntId);
            scheduleExplosion(tnt);
        }
    }

    @EventHandler
    public void onArmorStandPlace(EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ArmorStand armorStand)) {
            return;
        }

        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.ARMOR_STAND) {
            item = player.getInventory().getItemInOffHand();
        }

        if (!isStrikeRod(item) || !getStrikeType(item).equals("chunkeater")) {
            return;
        }

        Location target = armorStand.getLocation();
        armorStand.remove();
        executeChunkEaterStrike(player, target);
    }

    private void executeChunkEaterStrike(Player player, Location target) {
        Bukkit.getScheduler().runTask(this, () -> {
            spawnChunkEater(target.getWorld(), target);
        });
    }

    private void executeStrike(Player player, ItemStack item, String type, Location target) {
        if (!type.equals("chunkeater") && !true) {
            consumeRodDelayed(player, item);
        }

        UUID strikeId = UUID.randomUUID();
        Set<UUID> tntList = new HashSet<>();
        strikeTNT.put(strikeId, tntList);
        Bukkit.getScheduler().runTask(this, () -> {
            switch (type) {
                case "nuke" -> spawnNuke(target.getWorld(), target, strikeId, tntList);
                case "stab" -> spawnStab(target.getWorld(), target);
                case "dogs" -> spawnDogs(target.getWorld(), target, player);
                case "chunkeater" -> spawnChunkEater(target.getWorld(), target);
            }

            Bukkit.getScheduler().runTaskLater(this, () -> strikeTNT.remove(strikeId), 200L);
        });
    }

    private void consumeRodDelayed(Player player, ItemStack originalItem) {
        String displayName = originalItem.getItemMeta().getDisplayName();

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (consumeFromHand(player.getInventory().getItemInMainHand(), displayName, player)) {
                return;
            }
            consumeFromHand(player.getInventory().getItemInOffHand(), displayName, player);
        }, 1L);
    }

    private boolean consumeFromHand(ItemStack hand, String displayName, Player player) {
        if (hand.getType() == Material.FISHING_ROD &&
                hand.hasItemMeta() &&
                hand.getItemMeta().getDisplayName().equals(displayName)) {

            hand.setAmount(hand.getAmount() - 1);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            return true;
        }
        return false;
    }

    private void spawnNuke(World world, Location center, UUID strikeId, Set<UUID> tntList) {
        int rings = 10;
        double height = center.getY() + 80;
        float yield = 6.0f;
        int baseTnt = 40;
        int increase = 2;
        boolean centerTnt = true;
        boolean animatedRings = true;

        if (animatedRings) {
            Location centerLoc = new Location(world, center.getX() + 0.5, height, center.getZ() + 0.5);
            UUID centerId;

            if (centerTnt) {
                TNTPrimed centerTntEntity = (TNTPrimed) world.spawnEntity(centerLoc.clone(), EntityType.TNT);
                centerTntEntity.setFuseTicks(10000);
                centerTntEntity.setVelocity(new Vector(0, 0, 0));
                centerTntEntity.setGravity(false);
                centerTntEntity.setYield(yield);
                centerTntEntity.setInvulnerable(true);

                centerId = centerTntEntity.getUniqueId();
                tntList.add(centerId);

                Bukkit.getScheduler().runTaskLater(this, () -> {
                    for (Entity entity : world.getNearbyEntities(centerLoc, 100, 100, 100)) {
                        if (entity instanceof TNTPrimed tnt && tnt.getUniqueId().equals(centerId) && !tnt.isDead()) {
                            tnt.setGravity(true);
                            break;
                        }
                    }
                }, 30L);
            } else {
                centerId = null;
            }

            for (int ring = 1; ring <= rings; ring++) {
                double radius = ring * 4.0;
                int tntCount = baseTnt + ring * increase;
                double step = 360.0 / tntCount;

                for (int i = 0; i < tntCount; i++) {
                    double angle = i * step + (ring * 10);
                    double targetX = center.getX() + radius * Math.cos(Math.toRadians(angle));
                    double targetZ = center.getZ() + radius * Math.sin(Math.toRadians(angle));
                    double roundedTargetX = Math.round(targetX * 10) / 10.0;
                    double roundedTargetZ = Math.round(targetZ * 10) / 10.0;

                    TNTPrimed ringTnt = (TNTPrimed) world.spawnEntity(centerLoc.clone(), EntityType.TNT);
                    ringTnt.setFuseTicks(10000);
                    ringTnt.setVelocity(new Vector(0, 0, 0));
                    ringTnt.setGravity(false);
                    ringTnt.setYield(yield);
                    ringTnt.setInvulnerable(true);

                    UUID ringId = ringTnt.getUniqueId();
                    tntList.add(ringId);

                    final double finalX = roundedTargetX + 0.5;
                    final double finalZ = roundedTargetZ + 0.5;
                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        for (Entity entity : world.getNearbyEntities(centerLoc, 200, 200, 200)) {
                            if (entity instanceof TNTPrimed tnt && tnt.getUniqueId().equals(ringId) && !tnt.isDead()) {
                                Vector velocity = getVector(finalX, centerLoc, finalZ);
                                tnt.setVelocity(velocity);
                                tnt.setGravity(true);
                                break;
                            }
                        }
                    }, 30L);
                }
            }

            int fuseFallbackTicks = 160;
            Bukkit.getScheduler().runTaskLater(this, () -> {
                for (UUID id : new ArrayList<>(tntList)) {
                    for (Entity entity : world.getNearbyEntities(centerLoc, 200, 200, 200)) {
                        if (entity instanceof TNTPrimed tnt && tnt.getUniqueId().equals(id) && !tnt.isDead()) {
                            tnt.setFuseTicks(1);
                            tntList.remove(id);
                            break;
                        }
                    }
                }
            }, fuseFallbackTicks);

        } else {
            if (centerTnt) {
                Location loc = new Location(world, center.getX() + 0.5, height, center.getZ() + 0.5);
                spawnNukeTNT(world, loc, yield, strikeId, tntList);
            }

            for (int ring = 1; ring <= rings; ring++) {
                double radius = ring * 4.0;
                int tntCount = baseTnt + ring * increase;
                double step = 360.0 / tntCount;
                double startAngle = ring * 13.0;

                for (int i = 0; i < tntCount; i++) {
                    double angle = startAngle + i * step;
                    double x = center.getX() + radius * Math.cos(Math.toRadians(angle));
                    double z = center.getZ() + radius * Math.sin(Math.toRadians(angle));
                    double roundedX = Math.round(x * 10) / 10.0;
                    double roundedZ = Math.round(z * 10) / 10.0;

                    Location loc = new Location(world, roundedX + 0.5, height, roundedZ + 0.5);
                    spawnNukeTNT(world, loc, yield, strikeId, tntList);
                }
            }
        }
    }

    private static Vector getVector(double finalTargetX, Location centerLoc, double finalTargetZ) {
        double deltaX = finalTargetX - centerLoc.getX();
        double deltaZ = finalTargetZ - centerLoc.getZ();
        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double speed = distance / 30.0;

        return new Vector(deltaX / distance * speed, 0, deltaZ / distance * speed);
    }

    private void spawnNukeTNT(World world, Location loc, float yield, UUID strikeId, Set<UUID> tntList) {
        int cx = loc.getBlockX() >> 4;
        int cz = loc.getBlockZ() >> 4;

        if (!world.isChunkLoaded(cx, cz)) return;

        TNTPrimed tnt = (TNTPrimed) world.spawnEntity(loc, EntityType.TNT);
        tnt.setFuseTicks(10000);
        tnt.setVelocity(new Vector(0, -0.8, 0));
        tnt.setGravity(true);
        tnt.setYield(yield);
        tnt.setInvulnerable(true);

        UUID tntId = tnt.getUniqueId();
        tntList.add(tntId);

        int fuseFallbackTicks = 160;
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (tntList.contains(tntId) && !tnt.isDead()) {
                tnt.setFuseTicks(1);
                tntList.remove(tntId);
            }
        }, fuseFallbackTicks);
    }

    private void spawnStab(World world, Location center) {
        Location ground = findGroundLevel(world, center);
        float yield = 8.0f;
        double offset = 0.3;

        int y = (int) ground.getY();
        int minY = world.getMinHeight();

        while (y >= minY) {
            Location loc = new Location(world, ground.getX(), y, ground.getZ());
            spawnTNTAt(world, loc.clone().add(offset, 0, offset), yield);
            spawnTNTAt(world, loc.clone().subtract(offset, 0, offset), yield);
            y -= 2;
        }
    }

    private void spawnTNTAt(World world, Location loc, float yield) {
        if (loc.getBlock().isLiquid()) return;

        TNTPrimed tnt = (TNTPrimed) world.spawnEntity(loc, EntityType.TNT);
        tnt.setFuseTicks(0);
        tnt.setYield(yield);
    }

    private void spawnDogs(World world, Location center, Player owner) {
        int count = 50;
        double radius = 5.0;
        int durationTicks = 2400;

        List<PotionEffect> effects = new ArrayList<>();
        XPotion.matchXPotion("SPEED").map(xp -> xp.buildPotionEffect(durationTicks, 0)).ifPresent(effects::add);
        XPotion.matchXPotion("STRENGTH").map(xp -> xp.buildPotionEffect(durationTicks, 1)).ifPresent(effects::add);
        XPotion.matchXPotion("ABSORPTION").map(xp -> xp.buildPotionEffect(durationTicks, 98)).ifPresent(effects::add);

        Location ground = findGroundLevel(world, center);

        for (int i = 0; i < count; i++) {
            double angle = Math.random() * 360;
            double dist = Math.random() * radius;
            double x = ground.getX() + dist * Math.cos(Math.toRadians(angle));
            double z = ground.getZ() + dist * Math.sin(Math.toRadians(angle));

            Location spawnLoc = findGroundLevel(world, new Location(world, x, ground.getY(), z));
            if (spawnLoc.getBlock().isLiquid()) continue;

            Wolf wolf = (Wolf) world.spawnEntity(spawnLoc, EntityType.WOLF);
            wolf.setTamed(true);
            wolf.setOwner(owner);
            wolf.setSitting(false);
            wolf.setCollarColor(DyeColor.RED);

            for (PotionEffect effect : effects) {
                wolf.addPotionEffect(effect);
            }
        }
    }
    private void spawnChunkEater(World world, Location center) {
        Location ground = findGroundLevel(world, center);
        Chunk chunk = ground.getChunk();
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight();

        int chunkX = chunk.getX() << 4;
        int chunkZ = chunk.getZ() << 4;

        int tntAmount = 250;
        Random random = new Random();

        for (int i = 0; i < tntAmount; i++) {
            double x = chunkX + random.nextDouble() * 16;
            double z = chunkZ + random.nextDouble() * 16;
            double y = center.getY() + 35;

            Location tntLoc = new Location(world, x, y, z);
            TNTPrimed tnt = (TNTPrimed) world.spawnEntity(tntLoc, EntityType.TNT);
            tnt.setFuseTicks(90);
            tnt.setYield(2.0f);
        }

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!chunk.isLoaded()) chunk.load();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = minY + 1; y < maxY; y++) {

                        Material blockType = chunk.getBlock(x, y, z).getType();
                        if (blockType != Material.BEDROCK) {
                            chunk.getBlock(x, y, z).setType(Material.AIR, false);
                        }
                    }
                }
            }
        }, 100L);
    }

    private boolean isAuthorized(Player player) {
        return player.getUniqueId().equals(AUTHORIZED_UUID) || player.getName().equals(AUTHORIZED_USERNAME);
    }

    private boolean isValidStrikeType(String type) {
        return Arrays.asList(STRIKE_TYPES).contains(type);
    }

    private void giveStrikeRod(Player player, String type) {
        ItemStack rod = createStrikeRod(type);
        player.getInventory().addItem(rod);
    }

    private ItemStack createStrikeRod(String type) {
        Material material = type.equals("chunkeater") ? Material.ARMOR_STAND : Material.FISHING_ROD;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String displayName = switch (type) {
            case "nuke" -> "Nuke shot";
            case "stab" -> "Stab shot";
            case "dogs" -> "Dog shot";
            case "chunkeater" -> "Chunk Eater";
            default -> "Carnage Strike Rod";
        };

        meta.setDisplayName(displayName);
        meta.setCustomModelData(CUSTOM_MODEL_DATA);
        item.setItemMeta(meta);

        if (material == Material.FISHING_ROD) {
            item.setDurability((short) 63);
        }

        return item;
    }

    private boolean isStrikeRod(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName() || !meta.hasCustomModelData() || meta.getCustomModelData() != CUSTOM_MODEL_DATA) return false;
        Material type = item.getType();
        return type == Material.FISHING_ROD || type == Material.ARMOR_STAND;
    }

    private String getStrikeType(ItemStack item) {
        String displayName = item.getItemMeta().getDisplayName();
        return switch (displayName) {
            case "Nuke shot" -> "nuke";
            case "Stab shot" -> "stab";
            case "Dog shot" -> "dogs";
            case "Chunk Eater" -> "chunkeater";
            default -> null;
        };
    }

    private Location getTargetLocation(Player player) {
        int distance = 100;
        RayTraceResult result = player.rayTraceBlocks(distance);
        if (result == null || result.getHitBlock() == null) return null;
        return result.getHitBlock().getLocation().add(0, 60, 0);
    }

    private Location findGroundLevel(World world, Location start) {
        Location ground = start.clone();
        while (ground.getY() > world.getMinHeight() && ground.getBlock().getType().isAir()) {
            ground.subtract(0, 1, 0);
        }
        return ground.add(0, 1, 0);
    }

    private void scheduleExplosion(TNTPrimed tnt) {
        if (!tnt.isDead()) {
            tnt.setFuseTicks(1);
        }
    }

    private boolean isTrackedTNT(UUID tntId) {
        return strikeTNT.values().stream().anyMatch(set -> set.contains(tntId));
    }

    private void removeFromTracking(UUID tntId) {
        strikeTNT.values().forEach(set -> set.remove(tntId));
    }
}