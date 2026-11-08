package com.saltysmp.saltshop.util;

import org.bukkit.entity.Villager;

public final class VillagerLabels {

    private VillagerLabels() {
    }

    public static String friendlyType(Villager.Type type) {
        if (type == null) {
            return "Unknown";
        }
        return switch (type.name()) {
            case "PLAINS" -> "Plains";
            case "DESERT" -> "Desert";
            case "JUNGLE" -> "Jungle";
            case "SAVANNA" -> "Savanna";
            case "SNOW" -> "Snow";
            case "SWAMP" -> "Swamp";
            case "TAIGA" -> "Taiga";
            default -> capitalize(type.name());
        };
    }

    public static String friendlyProfession(Villager.Profession profession) {
        if (profession == null) {
            return "Unknown";
        }
        return switch (profession.name()) {
            case "NONE" -> "No job";
            case "NITWIT" -> "Nitwit";
            case "ARMORER" -> "Armorer";
            case "BUTCHER" -> "Butcher";
            case "CARTOGRAPHER" -> "Cartographer";
            case "CLERIC" -> "Cleric";
            case "FARMER" -> "Farmer";
            case "FISHERMAN" -> "Fisherman";
            case "FLETCHER" -> "Fletcher";
            case "LEATHERWORKER" -> "Leatherworker";
            case "LIBRARIAN" -> "Librarian";
            case "MASON" -> "Mason";
            case "SHEPHERD" -> "Shepherd";
            case "TOOLSMITH" -> "Toolsmith";
            case "WEAPONSMITH" -> "Weaponsmith";
            default -> capitalize(profession.name());
        };
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        String lower = value.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
