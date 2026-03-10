package pl.twojserwer.guilditems;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import org.bukkit.util.Vector;
import java.util.*;

public class GuildItemsPlugin extends JavaPlugin implements Listener {

    private final HashMap<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private final NamespacedKey itemKey = new NamespacedKey(this, "custom_item_id");

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        registerRemainingItems();
        startPassiveEffectsTask();
        getLogger().info("Zaktualizowano GuildItems - Pozostalo 10 przedmiotow.");
    }

    private void registerRemainingItems() {
        // --- MIECZE I NARZEDZIA ---
        createRecipe("vampire_sword", Material.NETHERITE_SWORD, "§4Ostrze Wampira", 1001, "RNR", "RSR", " R ", 'R', Material.REDSTONE_BLOCK, 'N', Material.NETHERITE_INGOT, 'S', Material.NETHERITE_SWORD);
        createRecipe("thor_hammer", Material.NETHERITE_AXE, "§eMlot Thora", 1002, "III", "ISI", " S ", 'I', Material.IRON_BLOCK, 'S', Material.BLAZE_ROD);
        createRecipe("ice_scythe", Material.NETHERITE_HOE, "§bKosa Mrozu", 1003, "DDI", " S ", " S ", 'D', Material.DIAMOND_BLOCK, 'I', Material.ICE, 'S', Material.STICK);
        createRecipe("poison_dagger", Material.IRON_SWORD, "§2Zatruty Sztylet", 1005, " P ", " S ", "   ", 'P', Material.POISONOUS_POTATO, 'S', Material.IRON_INGOT);

        // --- DYSTANSOWE ---
        createRecipe("artemis_bow", Material.BOW, "§aLuk Artemidy", 1006, " QW", " Q ", " QW", 'Q', Material.QUARTZ_BLOCK, 'W', Material.WHITE_WOOL);
        createRecipe("grappling_hook", Material.FISHING_ROD, "§bHak", 1020, "III", "IRI", "III", 'I', Material.IRON_INGOT, 'R', Material.FISHING_ROD);

        // --- AMULETY I SPECJALNE ---
        createRecipe("life_amulet", Material.NETHER_STAR, "§d§lAmulet Zycia", 1009, "GGG", "GNG", "GGG", 'G', Material.GOLD_BLOCK, 'N', Material.NETHER_STAR);
        createRecipe("tank_shield", Material.SHIELD, "§7Tarcza Tytana", 1010, "OOO", "OSO", "OOO", 'O', Material.OBSIDIAN, 'S', Material.SHIELD);
        createRecipe("escape_totem", Material.TOTEM_OF_UNDYING, "§6Totem Ucieczki", 1013, "EEE", "ETE", "EEE", 'E', Material.ENDER_PEARL, 'T', Material.TOTEM_OF_UNDYING);
        createRecipe("gravity_core", Material.CONDUIT, "§9Rdzeń Grawitacji", 1015, " P ", " P ", " P ", 'P', Material.PHANTOM_MEMBRANE);
    }

    private void createRecipe(String id, Material mat, String name, int cmd, String s1, String s2, String s3, Object... ing) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setCustomModelData(cmd);
            meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, id);
            item.setItemMeta(meta);
            ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(this, id), item);
            recipe.shape(s1, s2, s3);
            for (int i = 0; i < ing.length; i += 2) {
                recipe.setIngredient((Character) ing[i], (Material) ing[i + 1]);
            }
            Bukkit.addRecipe(recipe);
        }
    }

    private void startPassiveEffectsTask() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                ItemStack item = p.getInventory().getItemInMainHand();
                String id = getCustomId(item);

                // TARCZA TYTANA - Naprawione: usuwanie efektu jesli nie trzyma tarczy
                if ("tank_shield".equals(id)) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 40, 1));
                } else if (p.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE) && 
                           p.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE).getDuration() <= 40) {
                    p.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
                }

                if ("life_amulet".equals(id)) {
                    double maxH = p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                    if (p.getHealth() < maxH) p.setHealth(Math.min(p.getHealth() + 0.5, maxH));
                }
            }
        }, 20L, 20L);
    }

    @EventHandler
    public void onGrapple(PlayerFishEvent e) {
        Player p = e.getPlayer();
        if (isCustomItem(p.getInventory().getItemInMainHand(), "grappling_hook")) {
            if (e.getState() == PlayerFishEvent.State.IN_GROUND) {
                if (!checkCooldown(p, "grappling_hook", 90)) return;
                
                Location hookLoc = e.getHook().getLocation();
                Vector direction = hookLoc.toVector().subtract(p.getLocation().toVector()).normalize();
                p.setVelocity(direction.multiply(2.0).setY(1.0));
                p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1, 1);
            }
        }
    }

    @EventHandler
    public void onPlaceBlock(BlockPlaceEvent e) {
        if (isCustomItem(e.getItemInHand(), "gravity_core")) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§cRdzenia Grawitacji nie można postawić!");
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        ItemStack item = e.getItem();
        String id = getCustomId(item);
        if (id == null) return;
        Player p = e.getPlayer();

        if (e.getAction().name().contains("RIGHT_CLICK")) {
            switch (id) {
                case "thor_hammer" -> {
                    if (!checkCooldown(p, id, 60)) return;
                    Block b = p.getTargetBlockExact(30);
                    if (b != null) p.getWorld().strikeLightning(b.getLocation());
                }
                case "vampire_sword" -> {
                    if (!checkCooldown(p, id, 45)) return;
                    p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
                }
                case "escape_totem" -> {
                    if (!checkCooldown(p, id, 300)) return; // 5 minut
                    Random r = new Random();
                    int dist = 50 + r.nextInt(31); // 50-80 kratek
                    double angle = r.nextDouble() * 2 * Math.PI;
                    Location loc = p.getLocation().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                    loc.setY(p.getWorld().getHighestBlockYAt(loc) + 1);
                    p.teleport(loc);
                    p.sendMessage("§aTeleportowano w bezpieczne miejsce!");
                }
                case "gravity_core" -> {
                    if (!checkCooldown(p, id, 60)) return;
                    p.getNearbyEntities(7, 7, 7).forEach(entity -> entity.setVelocity(new Vector(0, 1.2, 0)));
                }
            }
        }
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p) {
            String id = getCustomId(p.getInventory().getItemInMainHand());
            if ("ice_scythe".equals(id) && e.getEntity() instanceof LivingEntity victim) {
                if (checkCooldown(p, "ice_scythe_hit", 5)) victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 1));
            }
            if ("poison_dagger".equals(id) && e.getEntity() instanceof LivingEntity victim) {
                if (checkCooldown(p, "poison_dagger_hit", 8)) victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 80, 0));
            }
        }
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent e) {
        if (e.getEntity() instanceof Player p && isCustomItem(e.getBow(), "artemis_bow")) {
            if (checkCooldown(p, "artemis_bow_shoot", 20)) p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 160, 1));
        }
    }

    private String getCustomId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
    }

    private boolean isCustomItem(ItemStack item, String id) {
        return id.equals(getCustomId(item));
    }

    private boolean checkCooldown(Player p, String item, int sec) {
        cooldowns.putIfAbsent(p.getUniqueId(), new HashMap<>());
        long now = System.currentTimeMillis();
        long last = cooldowns.get(p.getUniqueId()).getOrDefault(item, 0L);
        if (now - last < sec * 1000L) {
            p.sendMessage("§cGotowe za: " + ((sec * 1000L - (now - last)) / 1000) + "s");
            return false;
        }
        cooldowns.get(p.getUniqueId()).put(item, now);
        return true;
    }
}
