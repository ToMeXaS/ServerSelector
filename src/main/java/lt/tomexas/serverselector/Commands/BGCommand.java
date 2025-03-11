package lt.tomexas.serverselector.Commands;

import lt.tomexas.serverselector.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BGCommand implements CommandExecutor {
    private final Main plugin = Main.getInstance();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) return false;

        ArmorStand armorStand = (ArmorStand) player.getWorld().spawnEntity(player.getLocation(), EntityType.ARMOR_STAND);
        armorStand.setInvisible(true);
        armorStand.setGravity(false);
        armorStand.setPersistent(true);
        armorStand.setInvulnerable(true);
        armorStand.setRotation(player.getYaw(), player.getPitch());

        plugin.getDatabase().addLocation(armorStand);
        plugin.setArmorStand(armorStand);

        player.sendMessage("Background set!");
        return true;
    }
}
