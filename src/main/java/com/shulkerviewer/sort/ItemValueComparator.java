package com.shulkerviewer.sort;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;

/**
 * Value-based item comparator for sorting.
 * Higher value = appears first in the shulker.
 */
public final class ItemValueComparator {

    private ItemValueComparator() {
    }

    private static int value(ItemStack stack) {
        if (stack.isEmpty()) return -1;
        // ResourceLocation on <=1.21.10, Identifier on 1.21.11+/26.x — use Object
        Object id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) return 0;
        String key = id.toString();
        boolean enchanted = stack.isEnchanted();

        return switch (key) {
            // === ULTRA RARE (1200+) ===
            case "minecraft:dragon_egg" -> 1250;
            case "minecraft:enchanted_golden_apple" -> 1240;
            case "minecraft:nether_star" -> 1230;

            // === NETHERITE (1100+) ===
            case "minecraft:netherite_block" -> 1190;
            case "minecraft:netherite_ingot" -> 1180;
            case "minecraft:netherite_scrap" -> 1170;
            case "minecraft:netherite_sword" -> 1160;
            case "minecraft:netherite_pickaxe", "minecraft:netherite_axe" -> 1155;
            case "minecraft:netherite_shovel", "minecraft:netherite_hoe" -> 1150;
            case "minecraft:netherite_helmet" -> 1145;
            case "minecraft:netherite_chestplate" -> 1144;
            case "minecraft:netherite_leggings" -> 1143;
            case "minecraft:netherite_boots" -> 1142;

            // === DIAMOND (1000+) ===
            case "minecraft:diamond_block" -> 1090;
            case "minecraft:diamond" -> 1080;
            case "minecraft:diamond_sword" -> 1070;
            case "minecraft:diamond_pickaxe", "minecraft:diamond_axe" -> 1065;
            case "minecraft:diamond_shovel", "minecraft:diamond_hoe" -> 1060;
            case "minecraft:diamond_helmet" -> 1055;
            case "minecraft:diamond_chestplate" -> 1054;
            case "minecraft:diamond_leggings" -> 1053;
            case "minecraft:diamond_boots" -> 1052;

            // === RARE COMBAT / TOOLS (950+) ===
            case "minecraft:trident" -> 990;
            case "minecraft:mace" -> 985;
            case "minecraft:heavy_core" -> 980;
            case "minecraft:elytra" -> 970;
            case "minecraft:totem_of_undying" -> 960;

            // === EMERALD (900+) ===
            case "minecraft:emerald_block" -> 930;
            case "minecraft:emerald" -> 920;

            // === RARE CRAFTING / LOOT (850+) ===
            case "minecraft:heart_of_the_sea" -> 890;
            case "minecraft:echo_shard" -> 885;
            case "minecraft:conduit" -> 880;
            case "minecraft:beacon" -> 870;

            // === SMITHING / TRIM TEMPLATES (800+) ===
            case "minecraft:netherite_upgrade_smithing_template" -> 850;
            case "minecraft:ward_armor_trim_smithing_template" -> 845;
            case "minecraft:silence_armor_trim_smithing_template" -> 844;
            case "minecraft:rib_armor_trim_smithing_template" -> 843;
            case "minecraft:snout_armor_trim_smithing_template" -> 842;
            case "minecraft:coast_armor_trim_smithing_template" -> 841;
            case "minecraft:dune_armor_trim_smithing_template" -> 840;
            case "minecraft:eye_armor_trim_smithing_template" -> 839;
            case "minecraft:host_armor_trim_smithing_template" -> 838;
            case "minecraft:vex_armor_trim_smithing_template" -> 837;
            case "minecraft:tide_armor_trim_smithing_template" -> 836;
            case "minecraft:sentry_armor_trim_smithing_template" -> 835;
            case "minecraft:wayfinder_armor_trim_smithing_template" -> 834;
            case "minecraft:shaper_armor_trim_smithing_template" -> 833;
            case "minecraft:raiser_armor_trim_smithing_template" -> 832;
            case "minecraft:keeper_armor_trim_smithing_template" -> 831;

            // === SHULKER BOXES (750+) ===
            case "minecraft:shulker_box" -> 750;
            case "minecraft:white_shulker_box", "minecraft:orange_shulker_box",
                 "minecraft:magenta_shulker_box", "minecraft:light_blue_shulker_box",
                 "minecraft:yellow_shulker_box", "minecraft:lime_shulker_box",
                 "minecraft:pink_shulker_box", "minecraft:gray_shulker_box",
                 "minecraft:light_gray_shulker_box", "minecraft:cyan_shulker_box",
                 "minecraft:purple_shulker_box", "minecraft:blue_shulker_box",
                 "minecraft:brown_shulker_box", "minecraft:green_shulker_box",
                 "minecraft:red_shulker_box", "minecraft:black_shulker_box" -> 745;

            // === MUSIC DISCS (700+) ===
            case "minecraft:music_disc_5" -> 730;
            case "minecraft:music_disc_otherside" -> 725;
            case "minecraft:music_disc_13" -> 720;
            case "minecraft:music_disc_cat" -> 719;
            case "minecraft:music_disc_blocks" -> 718;
            case "minecraft:music_disc_chirp" -> 717;
            case "minecraft:music_disc_far" -> 716;
            case "minecraft:music_disc_mall" -> 715;
            case "minecraft:music_disc_mellohi" -> 714;
            case "minecraft:music_disc_stal" -> 713;
            case "minecraft:music_disc_strad" -> 712;
            case "minecraft:music_disc_ward" -> 711;
            case "minecraft:music_disc_11" -> 710;
            case "minecraft:music_disc_wait" -> 709;
            case "minecraft:music_disc_pigstep" -> 708;
            case "minecraft:music_disc_relic" -> 707;
            case "minecraft:music_disc_creator" -> 706;
            case "minecraft:music_disc_creator_music_box" -> 705;
            case "minecraft:music_disc_precipice" -> 704;

            // === VALUABLE BLOCKS (600+) ===
            case "minecraft:gold_block" -> 650;
            case "minecraft:iron_block" -> 640;
            case "minecraft:lapis_block" -> 630;
            case "minecraft:redstone_block" -> 625;
            case "minecraft:quartz_block" -> 620;
            case "minecraft:copper_block" -> 615;

            // === GOLD (500+) ===
            case "minecraft:gold_ingot" -> 540;
            case "minecraft:gold_nugget" -> 530;
            case "minecraft:netherite_horse_armor" -> 525;
            case "minecraft:diamond_horse_armor" -> 524;
            case "minecraft:golden_apple" -> 520;
            case "minecraft:bottle_o_enchanting" -> 515;
            case "minecraft:nautilus_shell" -> 510;

            // === IRON TOOLS / ARMOR (450+) ===
            case "minecraft:iron_sword" -> 480;
            case "minecraft:iron_pickaxe", "minecraft:iron_axe" -> 475;
            case "minecraft:iron_shovel", "minecraft:iron_hoe" -> 470;
            case "minecraft:iron_helmet" -> 465;
            case "minecraft:iron_chestplate" -> 464;
            case "minecraft:iron_leggings" -> 463;
            case "minecraft:iron_boots" -> 462;
            case "minecraft:iron_ingot" -> 455;
            case "minecraft:iron_nugget" -> 450;

            // === CHAINMAIL (440+) ===
            case "minecraft:chainmail_helmet" -> 445;
            case "minecraft:chainmail_chestplate" -> 444;
            case "minecraft:chainmail_leggings" -> 443;
            case "minecraft:chainmail_boots" -> 442;
            case "minecraft:chainmail_sword" -> 441;
            case "minecraft:chainmail_pickaxe" -> 440;

            // === REDSTONE / LAPIS / QUARTZ (400+) ===
            case "minecraft:redstone" -> 420;
            case "minecraft:lapis_lazuli" -> 415;
            case "minecraft:nether_quartz" -> 410;

            // === ENDER (350+) ===
            case "minecraft:ender_pearl" -> 380;
            case "minecraft:eye_of_ender" -> 370;
            case "minecraft:end_stone" -> 360;

            // === COAL (200+) ===
            case "minecraft:coal" -> 220;
            case "minecraft:charcoal" -> 210;

            // === WOOD / BUILDING (100+) — higher than default 0 ===
            case "minecraft:oak_planks", "minecraft:spruce_planks",
                 "minecraft:birch_planks", "minecraft:jungle_planks",
                 "minecraft:acacia_planks", "minecraft:dark_oak_planks",
                 "minecraft:mangrove_planks", "minecraft:cherry_planks",
                 "minecraft:bamboo_planks", "minecraft:crimson_planks",
                 "minecraft:warped_planks" -> 50;
            case "minecraft:cobblestone", "minecraft:cobbled_deepslate",
                 "minecraft:stone", "minecraft:deepslate" -> 40;
            case "minecraft:dirt", "minecraft:coarse_dirt",
                 "minecraft:grass_block", "minecraft:stone_bricks",
                 "minecraft:mossy_stone_bricks" -> 30;

            default -> {
                if (enchanted) yield 800;
                yield 0;
            }
        };
    }

    /** Comparator for sorting items by value (highest first), then alphabetically. */
    public static Comparator<ItemStack> comparator() {
        return (a, b) -> {
            if (a.isEmpty() && b.isEmpty()) return 0;
            if (a.isEmpty()) return 1;
            if (b.isEmpty()) return -1;
            int va = value(a);
            int vb = value(b);
            if (va != vb) return Integer.compare(vb, va);
            String ka = BuiltInRegistries.ITEM.getKey(a.getItem()).toString();
            String kb = BuiltInRegistries.ITEM.getKey(b.getItem()).toString();
            int c = ka.compareTo(kb);
            if (c != 0) return c;
            return Integer.compare(b.getCount(), a.getCount());
        };
    }
}
