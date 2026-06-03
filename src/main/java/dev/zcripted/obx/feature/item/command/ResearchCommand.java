package dev.zcripted.obx.feature.item.command;

import dev.zcripted.obx.core.command.AbstractObxCommand;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.util.text.ComponentMessenger;
import dev.zcripted.obx.util.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ResearchCommand extends AbstractObxCommand implements TabCompleter {


    public ResearchCommand(ObxPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obx.research")) {
            languages.send(sender, "core.no-permission");
            return true;
        }
        if (args.length == 0) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                ItemStack held = player.getInventory().getItemInMainHand();
                if (held != null && held.getType() != Material.AIR) {
                    sendProfile(sender, held.getType());
                    return true;
                }
                languages.send(sender, "utility.craft.hand-empty");
                return true;
            }
            languages.send(sender, "utility.craft.usage-console");
            return true;
        }

        String input = String.join(" ", args).trim();
        Material material = resolveMaterial(input);
        if (material == null) {
            languages.send(sender, "utility.craft.not-found", Placeholders.with("item", input));
            return true;
        }

        sendProfile(sender, material);
        return true;
    }

    private void sendProfile(CommandSender sender, Material material) {
        String displayName = formatMaterialName(material);
        Map<String, String> namePlaceholders = Placeholders.with("item", displayName);
        sender.sendMessage(" ");
        sender.sendMessage(languages.get(sender, "core.divider"));
        sender.sendMessage(languages.get(sender, "utility.craft.header"));
        sender.sendMessage(languages.get(sender, "utility.craft.item-name", namePlaceholders));
        sender.sendMessage(languages.get(sender, "core.divider"));
        sender.sendMessage(" ");

        String clickSuggestion = "/research " + displayName;

        sendSection(sender, "utility.craft.section.about.label", "utility.craft.section.about.hint",
                buildAboutHover(sender, material, displayName), clickSuggestion);
        sender.sendMessage(" ");
        sendSection(sender, "utility.craft.section.recipe.label", "utility.craft.section.recipe.hint",
                buildRecipeHover(sender, material), clickSuggestion);
        sender.sendMessage(" ");
        sendSection(sender, "utility.craft.section.use.label", "utility.craft.section.use.hint",
                buildUseHover(sender, material), clickSuggestion);
        sender.sendMessage(" ");
        sendSection(sender, "utility.craft.section.find.label", "utility.craft.section.find.hint",
                buildFindHover(sender, material), clickSuggestion);
        sender.sendMessage(" ");
        sendSection(sender, "utility.craft.section.dimensions.label", "utility.craft.section.dimensions.hint",
                buildDimensionsHover(sender, material), clickSuggestion);
        sender.sendMessage(" ");
        sendSection(sender, "utility.craft.section.extra.label", "utility.craft.section.extra.hint",
                buildExtraHover(sender, material), clickSuggestion);
    }

    private void sendSection(CommandSender sender, String labelKey, String hintKey, List<String> hover, String clickSuggestion) {
        String label = languages.get(sender, labelKey);
        String hint = languages.get(sender, hintKey);
        ComponentMessenger.sendHoverMessage(sender, label, hover, clickSuggestion);
        ComponentMessenger.sendHoverMessage(sender, hint, hover, clickSuggestion);
    }

    private List<String> buildAboutHover(CommandSender sender, Material material, String displayName) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("item", displayName);
        placeholders.put("type", material.isBlock()
                ? languages.get(sender, "utility.craft.value.block")
                : languages.get(sender, "utility.craft.value.item"));
        placeholders.put("stack", String.valueOf(material.getMaxStackSize()));
        String durability = material.getMaxDurability() > 0
                ? String.valueOf(material.getMaxDurability())
                : languages.get(sender, "utility.craft.value.none");
        placeholders.put("durability", durability);
        return languages.list(sender, "utility.craft.hover.about", placeholders);
    }

    private List<String> buildRecipeHover(CommandSender sender, Material material) {
        ItemStack target = new ItemStack(material);
        List<Recipe> recipes = Bukkit.getRecipesFor(target);
        if (recipes == null || recipes.isEmpty()) {
            return languages.list(sender, "utility.craft.hover.recipe.none", Collections.<String, String>emptyMap());
        }
        Recipe recipe = recipes.get(0);
        String type = recipeTypeLabel(sender, recipe);
        Map<String, String> placeholders = Placeholders.with("type", type);
        List<String> lines = new ArrayList<>(languages.list(sender, "utility.craft.hover.recipe.header", placeholders));
        String[][] grid = new String[3][3];
        String outputName = formatMaterialName(material);

        if (recipe instanceof ShapedRecipe) {
            ShapedRecipe shaped = (ShapedRecipe) recipe;
            String[] shape = shaped.getShape();
            Map<Character, ItemStack> map = shaped.getIngredientMap();
            for (int row = 0; row < 3; row++) {
                if (row >= shape.length) {
                    continue;
                }
                String rowShape = shape[row];
                for (int col = 0; col < 3; col++) {
                    if (col >= rowShape.length()) {
                        continue;
                    }
                    ItemStack ingredient = map.get(rowShape.charAt(col));
                    if (ingredient != null && ingredient.getType() != Material.AIR) {
                        grid[row][col] = formatMaterialName(ingredient.getType());
                    }
                }
            }
            lines.addAll(buildGridLines(sender, grid, outputName));
            return lines;
        }
        if (recipe instanceof ShapelessRecipe) {
            ShapelessRecipe shapeless = (ShapelessRecipe) recipe;
            int index = 0;
            for (ItemStack ingredient : shapeless.getIngredientList()) {
                if (ingredient == null || ingredient.getType() == Material.AIR) {
                    continue;
                }
                if (index >= 9) {
                    break;
                }
                int row = index / 3;
                int col = index % 3;
                grid[row][col] = formatMaterialName(ingredient.getType());
                index++;
            }
            lines.addAll(buildGridLines(sender, grid, outputName));
            return lines;
        }
        if (recipe instanceof FurnaceRecipe) {
            FurnaceRecipe furnace = (FurnaceRecipe) recipe;
            grid[1][1] = formatIngredient(sender, furnace.getInput());
            Map<String, String> smeltPlaceholders = Placeholders.with("input", formatIngredient(sender, furnace.getInput()));
            lines.addAll(buildGridLines(sender, grid, outputName));
            lines.add(languages.get(sender, "utility.craft.hover.recipe.smelt", smeltPlaceholders));
            return lines;
        }

        return lines;
    }

    private List<String> buildGridLines(CommandSender sender, String[][] grid, String outputName) {
        List<String> lines = new ArrayList<>();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("row1", formatRowCompact(sender, grid[0]));
        placeholders.put("row2", formatRowCompact(sender, grid[1]));
        placeholders.put("row3", formatRowCompact(sender, grid[2]));
        placeholders.put("output", clip(outputName, 16));
        lines.add(languages.get(sender, "utility.craft.hover.recipe.grid-line", placeholders));
        return lines;
    }

    private String formatRowCompact(CommandSender sender, String[] row) {
        return formatCellCompact(sender, row[0]) + "|"
                + formatCellCompact(sender, row[1]) + "|"
                + formatCellCompact(sender, row[2]);
    }

    private String formatCellCompact(CommandSender sender, String value) {
        String display = value == null || value.trim().isEmpty()
                ? languages.get(sender, "utility.craft.value.empty")
                : value;
        return clip(display, 8);
    }

    private String clip(String value, int max) {
        if (value == null) {
            return "";
        }
        if (value.length() <= max) {
            return value;
        }
        if (max <= 1) {
            return value.substring(0, 1);
        }
        return value.substring(0, max - 1) + "~";
    }


    private List<String> buildUseHover(CommandSender sender, Material material) {
        String summary = useSummary(sender, material);
        return languages.list(sender, "utility.craft.hover.use", Placeholders.with("summary", summary));
    }

    private List<String> buildFindHover(CommandSender sender, Material material) {
        String summary = findSummary(sender, material);
        return languages.list(sender, "utility.craft.hover.find", Placeholders.with("summary", summary));
    }

    private List<String> buildDimensionsHover(CommandSender sender, Material material) {
        String summary = dimensionSummary(sender, material);
        return languages.list(sender, "utility.craft.hover.dimensions", Placeholders.with("summary", summary));
    }

    private List<String> buildExtraHover(CommandSender sender, Material material) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("key", resolveKey(material));
        placeholders.put("id", resolveLegacyId(sender, material));
        placeholders.put("fuel", resolveBoolean(sender, material, "isFuel"));
        placeholders.put("edible", resolveBoolean(sender, material, "isEdible"));
        return languages.list(sender, "utility.craft.hover.extra", placeholders);
    }

    private String useSummary(CommandSender sender, Material material) {
        String name = material.name();
        if (isEdible(material)) {
            return languages.get(sender, "utility.craft.use.consume");
        }
        if (isArmor(name)) {
            return languages.get(sender, "utility.craft.use.armor");
        }
        if (isTool(name)) {
            return languages.get(sender, "utility.craft.use.tool");
        }
        if (material.isBlock()) {
            return languages.get(sender, "utility.craft.use.block");
        }
        return languages.get(sender, "utility.craft.use.misc");
    }

    private String findSummary(CommandSender sender, Material material) {
        List<Recipe> recipes = Bukkit.getRecipesFor(new ItemStack(material));
        String name = material.name();
        if (recipes != null && !recipes.isEmpty()) {
            return languages.get(sender, "utility.craft.find.craftable");
        }
        if (material.isBlock() && name.endsWith("_ORE")) {
            return languages.get(sender, "utility.craft.find.ore");
        }
        if (material.isBlock()) {
            return languages.get(sender, "utility.craft.find.natural");
        }
        return languages.get(sender, "utility.craft.find.loot");
    }

    private String dimensionSummary(CommandSender sender, Material material) {
        String name = material.name();
        boolean nether = name.contains("NETHER") || name.contains("CRIMSON") || name.contains("WARPED")
                || name.contains("BASALT") || name.contains("BLACKSTONE");
        boolean end = name.contains("END") || name.contains("CHORUS") || name.contains("PURPUR");
        if (nether && end) {
            return languages.get(sender, "utility.craft.dimensions.multi");
        }
        if (nether) {
            return languages.get(sender, "utility.craft.dimensions.nether");
        }
        if (end) {
            return languages.get(sender, "utility.craft.dimensions.end");
        }
        return languages.get(sender, "utility.craft.dimensions.overworld");
    }

    private String recipeTypeLabel(CommandSender sender, Recipe recipe) {
        if (recipe instanceof ShapedRecipe) {
            return languages.get(sender, "utility.craft.recipe-type.shaped");
        }
        if (recipe instanceof ShapelessRecipe) {
            return languages.get(sender, "utility.craft.recipe-type.shapeless");
        }
        if (recipe instanceof FurnaceRecipe) {
            return languages.get(sender, "utility.craft.recipe-type.smelting");
        }
        return languages.get(sender, "utility.craft.recipe-type.unknown");
    }

    private String formatIngredient(CommandSender sender, ItemStack item) {
        if (item == null || item.getType() == null || item.getType() == Material.AIR) {
            return languages.get(sender, "utility.craft.value.empty");
        }
        return formatMaterialName(item.getType());
    }

    private String resolveKey(Material material) {
        try {
            Method method = material.getClass().getMethod("getKey");
            Object value = method.invoke(material);
            if (value != null) {
                return value.toString();
            }
        } catch (Exception ignored) {
        }
        return "minecraft:" + material.name().toLowerCase(Locale.ENGLISH);
    }

    private String resolveLegacyId(CommandSender sender, Material material) {
        try {
            Method method = material.getClass().getMethod("getId");
            Object value = method.invoke(material);
            if (value != null) {
                return String.valueOf(value);
            }
        } catch (Exception ignored) {
        }
        return languages.get(sender, "utility.craft.value.none");
    }

    private String resolveBoolean(CommandSender sender, Material material, String methodName) {
        try {
            Method method = material.getClass().getMethod(methodName);
            Object value = method.invoke(material);
            if (value instanceof Boolean) {
                return (Boolean) value
                        ? languages.get(sender, "utility.craft.value.yes")
                        : languages.get(sender, "utility.craft.value.no");
            }
        } catch (Exception ignored) {
        }
        return languages.get(sender, "utility.craft.value.unknown");
    }

    private boolean isEdible(Material material) {
        try {
            Method method = material.getClass().getMethod("isEdible");
            Object value = method.invoke(material);
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private boolean isTool(String name) {
        return name.endsWith("_SWORD")
                || name.endsWith("_AXE")
                || name.endsWith("_PICKAXE")
                || name.endsWith("_SHOVEL")
                || name.endsWith("_HOE")
                || name.endsWith("_BOW")
                || name.endsWith("_CROSSBOW");
    }

    private boolean isArmor(String name) {
        return name.endsWith("_HELMET")
                || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS");
    }

    private Material resolveMaterial(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        Material direct = Material.matchMaterial(trimmed);
        if (direct != null) {
            return direct;
        }
        String normalized = trimmed.toUpperCase(Locale.ENGLISH).replace(' ', '_').replace('-', '_');
        normalized = normalized.replaceAll("[^A-Z0-9_]", "");
        return Material.matchMaterial(normalized);
    }

    private String formatMaterialName(Material material) {
        String name = material.name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
        StringBuilder builder = new StringBuilder();
        String[] words = name.split(" ");
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.isEmpty()) {
                continue;
            }
            if (i > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                builder.append(word.substring(1));
            }
        }
        return builder.toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) {
            return Collections.emptyList();
        }
        String current = String.join(" ", args).toLowerCase(Locale.ENGLISH);
        List<String> suggestions = new ArrayList<>();
        for (Material material : Material.values()) {
            if (material.name().startsWith("LEGACY_")) {
                continue;
            }
            String display = formatMaterialName(material);
            if (display.toLowerCase(Locale.ENGLISH).startsWith(current)) {
                suggestions.add(display);
            }
        }
        return suggestions;
    }
}
