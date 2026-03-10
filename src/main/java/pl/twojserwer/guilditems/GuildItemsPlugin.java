package pl.twojserwer.guilditems;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.*;
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
        registerAllTwentyItems();
        startPassiveEffectsTask();
        getLogger().info("Zaladowano 20 unikalnych przedmiotow!");
    }

    private void registerAllTwentyItems() {
        // --- MIECZE I NARZEDZIA ---
        createRecipe("vampire_sword", Material.NETHERITE_SWORD, "§4Ostrze Wampira", 1001, "RNR", "RSR", " R ", 'R', Material.REDSTONE_BLOCK, 'N', Material.NETHERITE_INGOT, 'S', Material.NETHERITE_SWORD);
        createRecipe("thor_hammer", Material.NETHERITE_AXE, "§eMlot Thora", 1002, "III", "ISI", " S ", 'I', Material.IRON_BLOCK, 'S', Material.BLAZE_ROD);
        createRecipe("ice_scythe", Material.NETHERITE_HOE, "§bKosa Mrozu", 1003, "DDI", " S ", " S ", 'D', Material.DIAMOND_BLOCK, 'I', Material.ICE, 'S', Material.STICK);
        createRecipe("lava_blade", Material.NETHERITE_SWORD, "§6Ognisty Język", 1004, " L ", " L ", " S ", 'L', Material.LAVA_BUCKET, 'S', Material.BLAZE_ROD);
        createRecipe("poison_dagger", Material.IRON_SWORD, "§2Zatruty Sztylet", 1005, " P ", " S ", "   ", 'P', Material.POISONOUS_POTATO, 'S', Material.IRON_INGOT);

        // --- LUKI ---
        createRecipe("artemis_bow", Material.BOW, "§aLuk Artemidy", 1006, " QW", " Q ", " QW", 'Q', Material.QUARTZ_BLOCK, 'W', Material.WHITE_WOOL);
        createRecipe("explosive_bow", Material.BOW, "§cŁuk Wybuchowy", 1007, "TNT", "TBT", "TNT", 'T', Material.TNT, 'B', Material.BOW);
        createRecipe("ender_bow", Material.BOW, "§5Łuk Pereł", 1008, " E ", " E ", " B ", 'E', Material.ENDER_PEARL, 'B', Material.BOW);

        // --- AMULETY I EKWIPUNEK ---
        createRecipe("life_amulet", Material.NETHER_STAR, "§d§lAmulet Zycia", 1009, "GGG", "GNG", "GGG", 'G', Material.GOLD_BLOCK, 'N', Material.NETHER_STAR);
        createRecipe("tank_shield", Material.SHIELD, "§7Tarcza Tytana", 1010, "OOO", "OSO", "OOO", 'O', Material.OBSIDIAN, 'S', Material.SHIELD);
        createRecipe("speed_boots", Material.NETHERITE_BOOTS, "§fButy Hermesa", 1011, "F F", "B B", "   ", 'F', Material.FEATHER, 'B', Material.NETHERITE_BOOTS);
        createRecipe("miner_pickaxe", Material.NETHERITE_PICKAXE, "§6Kilof Fortuny", 1012, "DDD", " S ", " S ", 'D', Material.DIAMOND, 'S', Material.STICK);

        // --- SPECJALNE ---
        createRecipe("escape_totem", Material.TOTEM_OF_UNDYING, "§6Totem Ucieczki", 1013, "EEE", "ETE", "EEE", 'E', Material.ENDER_PEARL, 'T', Material.TOTEM_OF_UNDYING);
        // POPRAWKA: LAPIS_BLOCK zamiast LAZULI_BLOCK
        createRecipe("knock_stick", Material.STICK, "§bPatyk Odrzutu", 1014, " L ", " S ", " L ", 'L', Material.LAPIS_BLOCK, 'S', Material.STICK);
        createRecipe("gravity_core", Material.CONDUIT, "§9Rdzeń Grawitacji", 1015, " P ", " P ", " P ", 'P', Material.PHANTOM_MEMBRANE);
        createRecipe("invisibility_cloak", Material.PHANTOM_MEMBRANE, "§fCałun Niewidki", 1016, "PPP", "P P", "PPP", 'P', Material.PHANTOM_MEMBRANE);
        createRecipe("dragon_breath", Material.DRAGON_BREATH, "§5Oddech Smoka", 1017, " B ", " B ", " B ", 'B', Material.DRAGON_BREATH);
        createRecipe("farmer_hoe", Material.GOLDEN_HOE, "§eMotyka Farmera", 1018, "WWW", " S ", " S ", 'W', Material.WHEAT, 'S', Material.STICK);
        createRecipe("jump_feather", Material.FEATHER, "§fPióro Skoku", 1019, " F ", " F ", " F ", 'F', Material.FEATHER);
        createRecipe("web_launcher", Material.FISHING_ROD, "§7Sieciomiot", 1020, "CCC", "CRC", "CCC", 'C', Material.COBWEB, 'R', Material.FISHING_ROD);
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
                if (id == null) continue;

                if (id.equals("life_amulet")) {
                    double maxH = p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                    if (p.getHealth() < maxH) p.setHealth(Math.min(p.getHealth() + 1.0, maxH));
                }
                // POPRAWKA: DAMAGE_RESISTANCE dla lepszej kompatybilnosci
                if (id.equals("tank_shield")) p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 40, 1));
                if (id.equals("speed_boots")) p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1));
            }
        }, 40L, 40L);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        ItemStack item = e.getItem();
        String id = getCustomId(item);
        if (id == null) return;
        Player p = e.getPlayer();

        if (e.getAction().name().contains("RIGHT_CLICK")) {
            if (!checkCooldown(p, id, 5)) return;

            switch (id) {
                case "thor_hammer" -> {
                    Block b = p.getTargetBlockExact(30);
                    if (b != null) p.getWorld().strikeLightning(b.getLocation());
                }
                case "vampire_sword" -> p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
                case "escape_totem" -> p.teleport(p.getLocation().add(new Random().nextInt(20)-10, 5, new Random().nextInt(20)-10));
                case "jump_feather" -> p.setVelocity(new Vector(0, 1.2, 0));
                case "invisibility_cloak" -> p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 200, 0));
                case "gravity_core" -> p.getNearbyEntities(5, 5, 5).forEach(entity -> entity.setVelocity(new Vector(0, 1, 0)));
            }
        }
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p) {
            String id = getCustomId(p.getInventory().getItemInMainHand());
            if (id == null) return;

            if (id.equals("ice_scythe") && e.getEntity() instanceof LivingEntity victim) {
                victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 2));
            }
            if (id.equals("poison_dagger") && e.getEntity() instanceof LivingEntity victim) {
                victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 0));
            }
        }
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent e) {
        if (e.getEntity() instanceof Player p) {
            String id = getCustomId(e.getBow());
            if (id == null) return;

            if (id.equals("artemis_bow")) p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 1));
            if (id.equals("explosive_bow")) e.getProjectile().setMetadata("explode", new org.bukkit.metadata.FixedMetadataValue(this, true));
        }
    }

    private String getCustomId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
    }

    private boolean checkCooldown(Player p, String item, int sec) {
        cooldowns.putIfAbsent(p.getUniqueId(), new HashMap<>());
        long now = System.currentTimeMillis();
        long last = cooldowns.get(p.getUniqueId()).getOrDefault(item, 0L);
        if (now - last < sec * 1000L) {
            p.sendMessage("§cOdczekaj " + (sec - (now - last) / 1000) + "s!");
            return false;
        }
        cooldowns.get(p.getUniqueId()).put(item, now);
        return true;
    }
}
