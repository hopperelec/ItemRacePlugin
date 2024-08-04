package uk.co.hopperelec.mc.itemrace.pointshandling;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.itemrace.ItemRacePlugin;
import uk.co.hopperelec.mc.itemrace.listeners.PlayerInventoryListener;

import java.util.Arrays;
import java.util.Objects;

public class MaxInventoryPointsHandler extends DepositedItems implements PlayerInventoryListener {
    public MaxInventoryPointsHandler(@NotNull ItemRacePlugin plugin) {
        super(plugin);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void countItems(@NotNull Player player, @NotNull Material material) {
        final int count = Arrays.stream(player.getInventory().getContents())
                .filter(plugin.config::awardPointsFor)
                .filter(Objects::nonNull) // Has no effect due to awardPointsFor filter
                .map(ItemStack::getAmount)
                .reduce(0, Integer::sum);
        if (getAmount(player, material) < count)
            setAmount(player, material, count);
    }

    @Override
    public void onMoveToInventory(@NotNull Player player, @NotNull ItemStack itemStack) {
        plugin.runInstantly(() -> countItems(player, itemStack.getType()));
    }

    @Override
    public void onPlayerPickupItem(@NotNull Player player, @NotNull EntityPickupItemEvent event) {
        plugin.runInstantly(() -> countItems(player, event.getItem().getItemStack().getType()));
    }
}
