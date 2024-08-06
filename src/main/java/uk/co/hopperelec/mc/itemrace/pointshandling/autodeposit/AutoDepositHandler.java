package uk.co.hopperelec.mc.itemrace.pointshandling.autodeposit;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.itemrace.ItemRacePlugin;

import static uk.co.hopperelec.mc.itemrace.ItemRaceUtils.isDamaged;

public class AutoDepositHandler extends AutoDepositor {
    public AutoDepositHandler(@NotNull ItemRacePlugin plugin) {
        super(plugin);
    }

    @Override
    protected int autoDeposit(@NotNull Player player, @NotNull ItemStack itemStack)  {
        if (!config().allowDamagedTools && isDamaged(itemStack)) return 0;
        final int amount = Math.min(itemStack.getAmount(), numberOfItemsLeftToMax(player, itemStack.getType()));
        if (amount != 0)
            try {
                deposit(player, itemStack.getType(), amount);
            } catch (IllegalArgumentException e) {
                return 0;
            }
        return amount;
    }
}
