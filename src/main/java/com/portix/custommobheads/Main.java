package com.portix.custommobheads;

import  com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.CreakingHeart;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Main extends JavaPlugin implements Listener {

    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private final Random random = new Random();
    FileConfiguration texturesConfig;
    FileConfiguration chancesConfig;
    FileConfiguration translationsConfig;
    FileConfiguration soundConfig;

    public static Map<Integer, String> TROPICAL_FISH_PRESETS;

    // Red Snapper converts to 919808, Red Cichild to 920064,
    static {
        Map<Integer, String> map = new HashMap<>();
        map.put(65536, "Clownfish");
        map.put(459008, "Triggerfish");
        map.put(917504, "Tomato Clownfish");
        map.put(918273, "Red Snapper");
        map.put(918529, "Red Cichlid");
        map.put(16778497, "Ornate Butterflyfish");
        map.put(50660352, "Queen Angelfish");
        map.put(50726144, "Cotton Candy Betta");
        map.put(67108865, "Threadfin");
        map.put(67110144, "Goatfish");
        map.put(67371009, "Yellow Tang");
        map.put(67699456, "Yellowtail Parrotfish");
        map.put(67764993, "Dottyback");
        map.put(101253888, "Parrotfish");
        map.put(117441025, "Moorish Idol");
        map.put(117441793, "Butterflyfish");
        map.put(117506305, "Anemone");
        map.put(117899265, "Black Tang");
        map.put(118161664, "Cichlid");
        map.put(185008129, "Blue Tang");
        map.put(234882305, "Emperor Red Snapper");
        map.put(235340288, "Red Lipped Blenny");

        TROPICAL_FISH_PRESETS = Collections.unmodifiableMap(map);
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getCommand("gethead").setExecutor(new GetHeadCommand(this));

        File texturesFile = new File(getDataFolder(), "textures.yml");
        if (!texturesFile.exists()) {
            saveResource("textures.yml", false);
        }
        texturesConfig = YamlConfiguration.loadConfiguration(texturesFile);

        File chancesFile = new File(getDataFolder(), "chance_config.yml");
        if (!chancesFile.exists()) {
            saveResource("chance_config.yml", false);
        }
        chancesConfig = YamlConfiguration.loadConfiguration(chancesFile);

        File translationsFile = new File(getDataFolder(), "translations.yml");
        if (!translationsFile.exists()) {
            saveResource("translations.yml", false);
        }
        translationsConfig = YamlConfiguration.loadConfiguration(translationsFile);

        File soundsFile = new File(getDataFolder(), "sound_config.yml");
        if (!soundsFile.exists()) {
            saveResource("sound_config.yml", false);
        }
        soundConfig = YamlConfiguration.loadConfiguration(soundsFile);

        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("CustomMobHeads enabled!");
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        //getLogger().info("Entity " + event.getEntity() + " died, killer: " + event.getEntity().getKiller() + ", ANOTHER: " + event.getDamageSource().getDirectEntity() + ", ANOTHER V2: " + event.getDamageSource().getCausingEntity());
        if (event.getEntity().getKiller() == null) {
            //getLogger().info("Returning, because killer is null");
            return;
        }

        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            //getLogger().warning("Skipping player");
            return;
        }

        Player killer = event.getEntity().getKiller();
        EntityType type = event.getEntityType();
        String typeName = type.name().toLowerCase();
        //getLogger().info("Killer: " + killer + ", typeName: " + typeName);

        if (!(event.getEntity() instanceof Wither)) {
            if (event.getEntity() instanceof HappyGhast) {
                HappyGhast ghast = (HappyGhast) event.getEntity();
                boolean isGhastling = ghast.getAge() < 0;
                if (isGhastling) {
                    typeName = "ghastling";
                }
            }

            //if (!chancesConfig.isConfigurationSection(typeName)) return;

            String variantKey = getVariantKey(event.getEntity());
            String mobKey = typeName.toLowerCase();

            String configPath = variantKey != null ? mobKey + "." + variantKey : mobKey;

            // GETTING THE CHANCE
            //if (!chancesConfig.contains(configPath)) return;
            double chancePercent = getConfig().getDouble("default-drop-chance", 50.0);
            if (chancesConfig.contains(configPath)) {
                chancePercent = chancesConfig.getDouble(configPath);
            } else {
                getLogger().warning("Chance not found: " + configPath);
            }
            double chance = chancePercent / 100.0;

            // CHECK IF THE MOD SHOULD DROP A HEAD
            //if (random.nextDouble() > chance && !getConfig().getBoolean("force-drop-all", false)) return;
            boolean forceDrop = getConfig().getBoolean("force-drop-all", false);
            if (!forceDrop && random.nextDouble() > chance) {
                // Skip dropping the head
                return;
            }

            if (event.getEntity() instanceof Creaking) {
                boolean canContinue = false;
                Creaking creaking = ((Creaking) event.getEntity());
                Location heartPos = creaking.getHome();
                if (heartPos != null) {
                    BlockData blockData = creaking.getWorld().getBlockData(heartPos);
                    if (blockData instanceof CreakingHeart) {
                        if (((CreakingHeart) blockData).isNatural()) {
                            canContinue = true;
                        }
                    }
                }/* else {
                    canContinue = true;
                } */

                if (!canContinue) {
                    return;
                }
            }

            if (forceDrop) {
                getLogger().info("Forcing a drop for: " + configPath);
            }

            // CORRECTING NAMES
            String name = translationsConfig.getString(configPath);
            if (name != null && !name.endsWith("Projectile")) name += " Head";
            //String textureKey = variantKey != null ? mobKey + "_" + variantKey : mobKey;
            //String texture = texturesConfig.getString("textures." + textureKey);

            //getLogger().info("Creating head: " + name + " with texture: " + texture);

            // ADDING KILLER LORE
            List<Component> lore = getConfig().getBoolean("add-killer-lore", true)
                    ? Collections.singletonList(Component.text("Killed by " + killer.getName()))
                    : Collections.emptyList();

            //ASSIGNING A TEXTURE
            String texture = texturesConfig.getString(configPath);

            if (texture == null) {
                //getLogger().warning("Texture key not found: " + textureKey);
                getLogger().warning("Texture key not found: " + configPath);
                // I'll use a mystery head texture instead
                //return;
            }

            ItemStack head = createCustomHead(name, texture, lore, configPath);
            event.getEntity().getWorld().dropItemNaturally(event.getEntity().getLocation(), head);
            //getLogger().info("Dropping head for: " + textureKey);
            //getLogger().info("Dropping head for: " + configPath);
        } else {
            List<String> list = new ArrayList<>();
            list.add("normal");
            list.add("projectile");
            list.add("blue_projectile");
            String finalTypeName = typeName;
            list.forEach(variantKey -> {
                //String variantKey = getVariantKey(event.getEntity());
                String mobKey = finalTypeName.toLowerCase();

                String configPath = mobKey + "." + variantKey;

                //if (!chancesConfig.contains(configPath)) return;
                double chancePercent = getConfig().getDouble("default-drop-chance", 50.0);
                if (chancesConfig.contains(configPath)) {
                    chancePercent = chancesConfig.getDouble(configPath);
                } else {
                    getLogger().warning("Chance not found: " + configPath);
                }
                double chance = chancePercent / 100.0;

                //if (random.nextDouble() > chance && !getConfig().getBoolean("force-drop-all", false)) return;
                boolean forceDrop = getConfig().getBoolean("force-drop-all", false);
                if (!forceDrop && random.nextDouble() > chance) {
                    // Skip dropping the head
                    return;
                }

                if (forceDrop) {
                    getLogger().info("Forcing a drop for: " + configPath);
                }

                String name = translationsConfig.getString(configPath);
                if (name != null && !name.endsWith("Projectile")) name += " Head";
                String texture = texturesConfig.getString(configPath);

                List<Component> lore = getConfig().getBoolean("add-killer-lore", true)
                        ? Collections.singletonList(Component.text("Killed by " + killer.getName()))
                        : Collections.emptyList();

                if (texture == null) {
                    //getLogger().warning("Texture key not found: " + textureKey);
                    getLogger().warning("Texture key not found: " + configPath);
                    // I'll use a mystery head texture instead
                    //return;
                }

                ItemStack head = createCustomHead(name, texture, lore, configPath);
                event.getEntity().getWorld().dropItemNaturally(event.getEntity().getLocation(), head);
            });
        }
    }

    private String getVariantKey(Entity entity) {
        try {
            if (entity instanceof MushroomCow) return ((MushroomCow) entity).getVariant().name().toLowerCase();
        } catch (Exception ignored) {
            return "red";
        }
        try {
            if (entity instanceof Cow) {
                Method getVariant = entity.getClass().getMethod("getVariant");
                Cow.Variant variant = (Cow.Variant) getVariant.invoke(entity);
                //getLogger().info("WE GOT THE VARIANT!!!");
                return variant.getKey().value(); //((Cow) entity).getVariant().getKey().value();
            }
        } catch (Exception ignored) {
            return "temperate";
        }
        try {
            if (entity instanceof Pig) return ((Pig) entity).getVariant().key().value();
        } catch (Exception ignored) {
            return "temperate";
        }
        try {
            if (entity instanceof Chicken) return ((Chicken) entity).getVariant().key().value();
        } catch (Exception ignored) {
            return "temperate";
        }

        if (entity instanceof Fox) return ((Fox) entity).getFoxType().name().toLowerCase();
        if (entity instanceof Frog) return ((Frog) entity).getVariant().key().value();
        if (entity instanceof Goat) return ((Goat) entity).isScreaming() ? "screaming" : "normal";
        if (entity instanceof Llama) return ((Llama) entity).getColor().name().toLowerCase();
        if (entity instanceof Panda) {
            Panda panda = (Panda) entity;
            Panda.Gene main = panda.getMainGene();
            Panda.Gene hidden = panda.getHiddenGene();

            String personality = null;

            // Recessive traits: only show if both genes match
            if (main == Panda.Gene.WEAK && hidden == Panda.Gene.WEAK) {
                personality = "Weak";
            } else if (main == Panda.Gene.BROWN && hidden == Panda.Gene.BROWN) {
                personality = "Brown";
            } else {
                // Dominant traits: show if main gene matches
                if (main == Panda.Gene.NORMAL) {
                    personality = "Normal";
                } else if (main == Panda.Gene.LAZY) {
                    personality = "Lazy";
                } else if (main == Panda.Gene.WORRIED) {
                    personality = "Worried";
                } else if (main == Panda.Gene.PLAYFUL) {
                    personality = "Playful";
                } else if (main == Panda.Gene.AGGRESSIVE) {
                    personality = "Aggressive";
                }
            }

            return personality != null ? personality.toLowerCase() : "normal";
        }
        if (entity instanceof Parrot) return ((Parrot) entity).getVariant().name().toLowerCase();

        if (entity instanceof Wolf) {
            Wolf wolf = (Wolf) entity;
            String prefix = wolf.isAngry() ? "angry_" : "";
            return prefix + wolf.getVariant().getKey().value();
        }

        if (entity instanceof TropicalFish) {
            TropicalFish fish = (TropicalFish) entity;
            //int size = fish.getPattern().ordinal() / 6 < 1 ? 0 : 1;
            int size = (int) Math.floor(fish.getPattern().ordinal() / 6d);
            int patternId = fish.getPattern().ordinal() % 6;
            int bodyColorId = fish.getBodyColor().ordinal();
            int patternColorId = fish.getPatternColor().ordinal();

            int variant = (patternColorId << 24) | (bodyColorId << 16) | (patternId << 8) | size;

            int smallVariant = (patternColorId << 24) | (bodyColorId << 16) | (patternId << 8);
            int largeVariant = smallVariant + 1;
            /*
            int variantIg = entity.getPersistentDataContainer().get(new NamespacedKey("minecraft", "variant"), PersistentDataType.INTEGER);
            getLogger().info("Something variant ig: " + variantIg);
             */

            /*
            getLogger().info("Presets: " + TROPICAL_FISH_PRESETS);
            getLogger().info("Variant(s): " + smallVariant + ", " + largeVariant);
            getLogger().info("PatternColor: " + patternColorId);
            getLogger().info("BodyColor: " + bodyColorId);
            getLogger().info("Pattern: " + patternId);
             */
            //String presetName = TROPICAL_FISH_PRESETS.getOrDefault(smallVariant, TROPICAL_FISH_PRESETS.get(largeVariant));
            String presetName = TROPICAL_FISH_PRESETS.get(variant);
            /*
            String presetName = TROPICAL_FISH_PRESETS.get(smallVariant);
            if (presetName == null) {
                presetName = TROPICAL_FISH_PRESETS.get(largeVariant);
            }
             */

            return presetName != null ? presetName.toLowerCase().replace(" ", "_") : "tropical_fish";
        }
        if (entity instanceof Axolotl) return ((Axolotl) entity).getVariant().name().toLowerCase();
        if (entity instanceof Cat) return ((Cat) entity).getCatType().key().value();
        if (entity instanceof Creeper) return ((Creeper) entity).isPowered() ? "charged" : "normal";

        if (entity instanceof Rabbit) {
            Rabbit rabbit = (Rabbit) entity;
            String type = rabbit.getRabbitType().name().toLowerCase();
            if (Objects.equals(rabbit.customName(), Component.text("Toast"))) {
                type = "toast";
            }
            return type;
        }

        if (entity instanceof Bee) {
            Bee bee = (Bee) entity;
            if (bee.getAnger() > 0 && bee.hasNectar()) return "angry_pollinated";
            if (bee.getAnger() > 0) return "angry";
            if (bee.hasNectar()) return "pollinated";
            return "normal";
        }

        if (entity instanceof Sheep) {
            Sheep sheep = (Sheep) entity;
            String color = sheep.getColor().name().toLowerCase();
            if (sheep.customName() != null && Objects.equals(sheep.customName(), Component.text("jeb_"))) {
                return "jeb_";
            }
            return color;
        }
        if (entity instanceof Villager) {
            Villager villager = (Villager) entity;
            String type = villager.getVillagerType().key().value();
            String profession = villager.getProfession().key().value();
            return type + "." + profession;
        }

        if (entity instanceof Horse) return ((Horse) entity).getColor().name().toLowerCase();
        if (entity instanceof ZombieVillager) {
            ZombieVillager zv = (ZombieVillager) entity;
            String type = zv.getVillagerType().key().value();
            String profession = zv.getVillagerProfession().key().value();
            return type + "." + profession;
        }

        if (entity instanceof Vex) return random.nextDouble() < 0.5 ? "angry" : "normal";

        if (entity instanceof CopperGolem) return ((CopperGolem) entity).getWeatheringState().name().toLowerCase();
        if (entity instanceof ZombieNautilus) return ((ZombieNautilus) entity).getVariant().getKey().value();

        return null; // Non-variant mob
    }

    ItemStack createCustomHead(String name, String texture, List<Component> lore, String configPath) {
        if (name == null || name.isEmpty()) name = "Unknown Mob Head";
        //getLogger().info("Name: " + name);
        if (configPath == null || configPath.isEmpty()) configPath = "mob";
        if (texture == null || texture.isEmpty()) {
            //texture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjM0MGQ1MGQ3ZDEyOTNiYTE2ZDIzYzZkMDdhYjA2NmNkYzE1NzVjNjhiY2E2OWU5NmYwYmI2ZDFjZTFiZjFiYSJ9fX0=";
            texture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDEzZTk2ZGY2ZWQ0YjcwYTVhNzBmYzI5ZGNkZTkzMTRkYmU5NzY2OTY0NzRmMTIwZTBiMzBlYTVkN2I5NmIzYSJ9fX0=";
        }

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;

        meta.displayName(Component.text(name));
        if (lore != null && !lore.isEmpty()) {
            meta.lore(lore);
        }

        String mobKey = configPath.split("\\.")[0];

        if (mobKey.equals("pufferfish")) mobKey = "puffer_fish";
        //getLogger().info("Mob Key for: " + mobKey + ", sound: " + mobKey);
        String soundId;

        if (mobKey.equals("cave_spider")) mobKey = "spider";
        if (mobKey.equals("mooshroom")) mobKey = "cow";

        if (soundConfig.contains("soundOverrides." + mobKey)) {
            soundId = soundConfig.getString("soundOverrides." + mobKey);
        } else if (configPath.equals("goat.screaming")) {
            soundId = "minecraft:entity." + configPath + ".ambient";
        } else if (mobKey.equals("copper_golem")) {
            String temporary = mobKey;
            if (!configPath.endsWith("unaffected") && !configPath.endsWith("exposed")) {
                temporary = configPath.replace(".", "_");
            }
            soundId = "minecraft:entity." + temporary + ".spin";
        } else {
            soundId = "minecraft:entity." + mobKey + ".ambient";
        }

        if (mobKey.equals("allay")) soundId += "_with_item";
        if (mobKey.equals("axolotl")) soundId = "minecraft:entity.axolotl.idle_air";
        if (mobKey.equals("bee")) soundId = "minecraft:entity.bee.pollinate";
        if (mobKey.equals("breeze")) soundId = "minecraft:entity.breeze.slide";
        if (mobKey.equals("cod")) soundId = "minecraft:entity.cod.flop";
        if (mobKey.equals("salmon")) soundId = "minecraft:entity.salmon.flop";
        if (mobKey.equals("dolphin")) soundId += "_water";
        if (mobKey.equals("guardian")) soundId += "_land";
        if (mobKey.equals("hoglin")) soundId = "minecraft:entity.hoglin.angry";
        if (mobKey.equals("iron_golem")) soundId = "minecraft:entity.iron_golem.step";
        if (mobKey.equals("magma_cube")) soundId = "minecraft:entity.magma_cube.jump";
        if (mobKey.equals("puffer_fish")) soundId = "minecraft:entity.puffer_fish.blow_up";
        if (mobKey.equals("slime")) soundId = "minecraft:entity.slime.jump";
        if (mobKey.equals("sniffer")) soundId = "minecraft:entity.sniffer.idle";
        if (mobKey.equals("tadpole")) soundId = "minecraft:entity.tadpole.hurt";
        if (mobKey.equals("turtle")) soundId += "_land";
        if (mobKey.equals("vex") && configPath.endsWith("angry")) soundId = "minecraft:entity.vex.charge";
        if (mobKey.equals("zoglin")) soundId = "minecraft:entity.zoglin.angry";

        if (mobKey.equals("snow_golem")) soundId = "minecraft:entity.snow_golem.death";
        if (mobKey.equals("trader_llama")) soundId = soundId.replace("trader_", "");
        //if (configPath.equals("goat.screaming")) soundId = "minecraft:entity.goat.screaming.ambient";

        if (configPath.equals("wither.projectile")) soundId = "minecraft:entity.wither.shoot";
        if (configPath.equals("wither.blue_projectile")) soundId = "minecraft:entity.wither.shoot";
        if (configPath.equals("rabbit.the_killer_bunny")) soundId = "minecraft:entity.rabbit.attack";

        //getLogger().info("Sound Id: " + soundId);

        if (soundId == null) soundId = "minecraft:entity.player.hurt";
        meta.setNoteBlockSound(NamespacedKey.fromString(soundId));
        /*
        meta.getPersistentDataContainer().set(
                new NamespacedKey("minecraft", "note_block_sound"),
                PersistentDataType.STRING,
                soundId != null ? soundId : "minecraft:entity.player.hurt"
        );
         */

        String capitalizedMobKey = mobKey.substring(0, 1).toUpperCase() + mobKey.substring(1);

        try {
            String idSeed = "OfflinePlayer:" + configPath;  // + name.replace(" ", "_").replace(" Head", "");
            UUID uuid = UUID.nameUUIDFromBytes(idSeed.getBytes(StandardCharsets.UTF_8));
            PlayerProfile profile = Bukkit.createProfile(uuid, capitalizedMobKey);
            profile.setProperty(new ProfileProperty("textures", texture));
            meta.setPlayerProfile(profile);

            /*
            UUID id = UUID.randomUUID();
            Object profile = Class.forName("com.mojang.authlib.GameProfile")
                .getConstructor(UUID.class, String.class)
                .newInstance(id, null);

            Object props = profile.getClass().getDeclaredMethod("getProperties").invoke(profile);
            props.getClass().getMethod("put", Object.class, Object.class)
                .invoke(props, "textures",
                    Class.forName("com.mojang.authlib.properties.Property")
                        .getConstructor(String.class, String.class)
                        .newInstance("textures", texture));

            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
             */
        } catch (Exception e) {
            //e.printStackTrace();
            getLogger().severe("Failed to create custom head for " + capitalizedMobKey + ": " + e.getMessage());
            for (StackTraceElement ste : e.getStackTrace()) {
                getLogger().severe("    at " + ste.toString());
            }
        }

        head.setItemMeta(meta);
        return head;
    }
}
