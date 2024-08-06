package uk.co.hopperelec.mc.itemrace.pointshandling;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.itemrace.ItemRacePlugin;
import uk.co.hopperelec.mc.itemrace.pointshandling.autodeposit.AutoDepositHandler;

import java.util.*;
import java.util.stream.Collectors;

public class ManualDepositHandler extends AutoDepositHandler {
    protected final Map<UUID, Set<Material>> autoDepositItems = new HashMap<>();

    public ManualDepositHandler(@NotNull ItemRacePlugin plugin) {
        super(plugin);
    }

    @Override
    protected void serializeData(@NotNull YamlConfiguration data) {
        super.serializeData(data);
        final ConfigurationSection serializedAutoDepositItems = data.createSection("auto_deposit_items");
        autoDepositItems.forEach((uuid, items) -> serializedAutoDepositItems.set(
                uuid.toString(),
                items.stream().map(Material::name).toList()
        ));
    }

    @Override
    protected void deserializeData(@NotNull YamlConfiguration data) {
        super.deserializeData(data);
        final ConfigurationSection serializedAutoDepositItems = data.getConfigurationSection("auto_deposit_items");
        if (serializedAutoDepositItems == null) return;
        serializedAutoDepositItems.getValues(false).forEach((uuidString, serializedItems) ->
                autoDepositItems.put(
                        UUID.fromString(uuidString),
                        serializedAutoDepositItems.getStringList(uuidString).stream()
                            .map(Material::valueOf)
                            .collect(Collectors.toCollection(HashSet::new))
                )
        );
    }

    @Override
    protected int autoDeposit(@NotNull Player player, @NotNull ItemStack itemStack) {
        if (!player.hasPermission("itemrace.autodeposit")) return 0;
        final Set<Material> autoDepositItems = this.autoDepositItems.get(player.getUniqueId());
        if (autoDepositItems == null || !autoDepositItems.contains(itemStack.getType())) return 0;
        return super.autoDeposit(player, itemStack);
    }

    private void autoDeposit(@NotNull Player player, @NotNull Material material) {
        if (listeningFor(player)) {
            for (ItemStack itemStack : player.getInventory().getContents()) {
                if (itemStack != null && itemStack.getType() == material)
                    onMoveToInventory(player, itemStack);
            }
        }
    }

    @Override
    protected void setAmount(@NotNull UUID uuid, @NotNull Material itemType, int amount) {
        super.setAmount(uuid, itemType, amount);
        if (amount > config().maxItemsAwardedPoints)
            throw new IllegalArgumentException("Amount exceeds maxItemsAwardedPoints");
    }

    public @NotNull Set<Material> getAutoDepositItems(@NotNull OfflinePlayer player) {
        if (autoDepositItems.containsKey(player.getUniqueId()))
            return Set.copyOf(autoDepositItems.get(player.getUniqueId()));
        return config().defaultAutoDeposit;
    }

    private @NotNull Set<Material> getMutableAutoDepositItems(@NotNull OfflinePlayer player) {
        return autoDepositItems.computeIfAbsent(player.getUniqueId(), uuid -> new HashSet<>(config().defaultAutoDeposit));
    }

    public boolean addAutoDepositItem(@NotNull Player player, @NotNull Material material) {
        if (getMutableAutoDepositItems(player).add(material)) {
            autoDeposit(player, material);
            return true;
        }
        return false;
    }

    public boolean removeAutoDepositItem(@NotNull Player player, @NotNull Material material) {
        return getMutableAutoDepositItems(player).remove(material);
    }

    public boolean toggleAutoDepositItem(@NotNull Player player, @NotNull Material material) {
        final Set<Material> autoDepositItems = getMutableAutoDepositItems(player);
        if (autoDepositItems.contains(material)) {
            autoDepositItems.remove(material);
            return false;
        }
        if (autoDepositItems.add(material))
            autoDeposit(player, material);
        return true;
    }
}
