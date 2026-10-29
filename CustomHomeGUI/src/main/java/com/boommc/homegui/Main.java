package com.boommc.homegui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class Main extends JavaPlugin implements CommandExecutor, Listener {

    @Override
    public void onEnable() {
        // কনফিগ ফাইল লোড করা
        saveDefaultConfig();
        
        // কমান্ড ও ইভেন্ট রেজিস্টার করা
        if (getCommand("home") != null) {
            getCommand("home").setExecutor(this);
        }
        getServer().getPluginManager().registerEvents(this, this);
        
        getLogger().info("CustomHomeGUI (1.21) Successfully Enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("CustomHomeGUI Disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("এই কমান্ডটি শুধু প্লেয়াররা ব্যবহার করতে পারবে!");
            return true;
        }

        Player player = (Player) sender;
        openHomeGUI(player);
        return true;
    }

    // হোম জিইউআই (GUI) ওপেন করার ফাংশন
    public void openHomeGUI(Player player) {
        Component guiTitle = Component.text("আমার হোম মেনু (Home GUI)", NamedTextColor.DARK_PURPLE);
        Inventory gui = Bukkit.createInventory(null, 27, guiTitle);

        // ব্যাকগ্রাউন্ডের জন্য কালো গ্লাস প্যানেল
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.text(" "));
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < 27; i++) {
            gui.setItem(i, filler);
        }

        // মাঝখানের সারি (Slot 10 থেকে 16 পর্যন্ত মোট ৭টি হোম স্লট)
        for (int i = 10; i <= 16; i++) {
            int homeNumber = i - 9; // হোম ১ থেকে ৭
            String path = "players." + player.getUniqueId() + ".home" + homeNumber;
            
            boolean isSet = getConfig().contains(path);

            ItemStack item;
            ItemMeta meta;

            if (isSet) {
                // হোম সেট করা থাকলে সবুজ শুলকার বক্স
                item = new ItemStack(Material.LIME_SHULKER_BOX);
                meta = item.getItemMeta();
                meta.displayName(Component.text("হোম #" + homeNumber, NamedTextColor.GREEN));
                
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("ক্লিক করে এই হোমে টেলিপোর্ট হও!", NamedTextColor.YELLOW));
                lore.add(Component.text("বদল করতে Shift + Click করো।", NamedTextColor.GRAY));
                meta.lore(lore);
            } else {
                // হোম খালি থাকলে লাল শুলকার বক্স
                item = new ItemStack(Material.RED_SHULKER_BOX);
                meta = item.getItemMeta();
                meta.displayName(Component.text("খালি হোম স্লট #" + homeNumber, NamedTextColor.RED));
                
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("ক্লিক করে বর্তমান লোকেশনে হোম সেট করো!", NamedTextColor.YELLOW));
                meta.lore(lore);
            }
            item.setItemMeta(meta);
            gui.setItem(i, item);
        }

        player.openInventory(gui);
    }

    // জিইউআই-তে ক্লিক হ্যান্ডেল করার ইভেন্ট
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Component expectedTitle = Component.text("আমার হোম মেনু (Home GUI)", NamedTextColor.DARK_PURPLE);
        if (!event.getView().title().equals(expectedTitle)) {
            return;
        }

        event.setCancelled(true); // প্লেয়ার যাতে আইটেম সরাতে না পারে

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        int slot = event.getSlot();
        if (slot >= 10 && slot <= 16) {
            int homeNumber = slot - 9;
            String path = "players." + player.getUniqueId() + ".home" + homeNumber;

            if (event.isShiftClick()) {
                // শিফট ক্লিক করলে নতুন লোকেশন হোম হিসেবে ওভাররাইট হবে
                Location loc = player.getLocation();
                getConfig().set(path + ".world", loc.getWorld().getName());
                getConfig().set(path + ".x", loc.getX());
                getConfig().set(path + ".y", loc.getY());
                getConfig().set(path + ".z", loc.getZ());
                getConfig().set(path + ".yaw", loc.getYaw());
                getConfig().set(path + ".pitch", loc.getPitch());
                saveConfig();

                player.sendMessage(Component.text("সফলভাবে হোম #" + homeNumber + " আপডেট করা হয়েছে!", NamedTextColor.GREEN));
                openHomeGUI(player);
            } else {
                if (getConfig().contains(path)) {
                    // হোম সেট থাকলে টেলিপোর্ট করবে
                    String worldName = getConfig().getString(path + ".world");
                    double x = getConfig().getDouble(path + ".x");
                    double y = getConfig().getDouble(path + ".y");
                    double z = getConfig().getDouble(path + ".z");
                    float yaw = (float) getConfig().getDouble(path + ".yaw");
                    float pitch = (float) getConfig().getDouble(path + ".pitch");

                    Location loc = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
                    player.closeInventory();
                    player.teleport(loc);
                    player.sendMessage(Component.text("হোম #" + homeNumber + "-এ টেলিপোর্ট করা হয়েছে!", NamedTextColor.AQUA));
                } else {
                    // খালি থাকলে ক্লিক করলেই হোম সেট হয়ে যাবে
                    Location loc = player.getLocation();
                    getConfig().set(path + ".world", loc.getWorld().getName());
                    getConfig().set(path + ".x", loc.getX());
                    getConfig().set(path + ".y", loc.getY());
                    getConfig().set(path + ".z", loc.getZ());
                    getConfig().set(path + ".yaw", loc.getYaw());
                    getConfig().set(path + ".pitch", loc.getPitch());
                    saveConfig();

                    player.sendMessage(Component.text("হোম #" + homeNumber + " সফলভাবে তৈরি করা হয়েছে!", NamedTextColor.GREEN));
                    openHomeGUI(player);
                }
            }
        }
    }
}