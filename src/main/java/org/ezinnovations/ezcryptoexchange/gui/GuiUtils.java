package org.ezinnovations.ezcryptoexchange.gui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.ezinnovations.ezcryptoexchange.util.ColorUtil;

import java.util.List;

public final class GuiUtils {
    private GuiUtils() {
    }

    public static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (name != null) {
            meta.setDisplayName(ColorUtil.colorize(name));
        }
        if (lore != null) {
            meta.setLore(lore.stream().map(ColorUtil::colorize).toList());
        }
        stack.setItemMeta(meta);
        return stack;
    }

    public static ItemStack glass() {
        return item(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " ", List.of());
    }
}
