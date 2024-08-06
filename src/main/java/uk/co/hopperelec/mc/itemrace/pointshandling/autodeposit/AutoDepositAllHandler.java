package uk.co.hopperelec.mc.itemrace.pointshandling.autodeposit;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.itemrace.ItemRacePlugin;

public class AutoDepositAllHandler extends AutoDepositor {
    public AutoDepositAllHandler(@NotNull ItemRacePlugin plugin) {
        super(plugin);
    }

    @Override
    protected int autoDeposit(@NotNull Player player, @NotNull ItemStack itemStack)  {
        tryDeposit(player, itemStack);
        return itemStack.getAmount();
    }
}
