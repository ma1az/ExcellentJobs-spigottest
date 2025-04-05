package su.nightexpress.excellentjobs.job.impl;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.economybridge.api.Currency;
import su.nightexpress.economybridge.currency.CurrencyId;
import su.nightexpress.excellentjobs.JobsPlugin;
import su.nightexpress.excellentjobs.Placeholders;
import su.nightexpress.excellentjobs.job.work.Work;
import su.nightexpress.excellentjobs.job.work.WorkObjective;
import su.nightexpress.excellentjobs.config.Config;
import su.nightexpress.excellentjobs.config.Perms;
import su.nightexpress.excellentjobs.data.impl.JobData;
import su.nightexpress.excellentjobs.job.work.WorkRegistry;
import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.wrapper.UniInt;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class JobObjective {

    private final String id;
    private final String workId;
    private final String displayName;
    private final ItemStack                    icon;
    private final Set<String>                  objects;
    private final Map<String, ObjectiveReward> paymentMap;
    private final ObjectiveReward              xpReward;
    private final int                          unlockLevel;

    private final boolean specialOrderAllowed;
    private final UniInt specialOrderObjectsAmount;
    private final UniInt specialOrderObjectCount;

    public JobObjective(@NotNull String id,
                        @NotNull String workId,
                        @NotNull String displayName,
                        @NotNull ItemStack icon,
                        @NotNull Set<String> objects,
                        @NotNull Map<String, ObjectiveReward> paymentMap,
                        @NotNull ObjectiveReward xpReward,
                        int unlockLevel,
                        boolean specialOrderAllowed,
                        UniInt specialOrderObjectsAmount,
                        UniInt specialOrderObjectCount) {
        this.id = id.toLowerCase();
        this.workId = workId;
        this.displayName = displayName;
        this.icon = icon;
        this.objects = new HashSet<>(objects);
        this.paymentMap = paymentMap;
        this.xpReward = xpReward;
        this.unlockLevel = unlockLevel;
        this.specialOrderAllowed = specialOrderAllowed;
        this.specialOrderObjectsAmount = specialOrderObjectsAmount;
        this.specialOrderObjectCount = specialOrderObjectCount;
    }

    @NotNull
    public static JobObjective read(@NotNull JobsPlugin plugin, @NotNull FileConfig cfg, @NotNull String path, @NotNull String id) {
        String workType = ConfigValue.create(path + ".Type", "null").read(cfg);

        // Add missing currencies for users to know they can use them.
        /*plugin.getCurrencyManager().getCurrencies().forEach(currency -> {
            if (!cfg.contains(path + ".Payment." + currency.getId())) {
                ObjectiveReward.EMPTY.write(cfg, path + ".Payment." + currency.getId());
            }
        });*/

        String displayName = cfg.getString(path + ".Display.Name", id);
        ItemStack icon = cfg.getItem(path + ".Display.Icon");

        Set<String> objects = cfg.getStringSet(path + ".Objects")
            .stream().map(String::toLowerCase).collect(Collectors.toSet());

        Map<String, ObjectiveReward> currencyDrop = new HashMap<>();
        for (String curId : cfg.getSection(path + ".Payment")) {
            ObjectiveReward objectiveReward = ObjectiveReward.read(cfg, path + ".Payment." + curId);
            currencyDrop.put(CurrencyId.reroute(curId), objectiveReward);
        }
        ObjectiveReward xpDrop = ObjectiveReward.read(cfg, path + ".Job_XP");

        int unlockLevel = ConfigValue.create(path + ".Unlock_Level", 1).read(cfg);

        boolean specialOrderAllowed = false;
        UniInt specialOrderObjectsAmount = null;
        UniInt specialOrderObjectCount = null;

        if (Config.SPECIAL_ORDERS_ENABLED.get()) {
            specialOrderAllowed = ConfigValue.create(path + ".SpecialOrder.Allowed", true).read(cfg);

            specialOrderObjectsAmount = ConfigValue.create(path + ".SpecialOrder.Objects_Amount",
                UniInt::read,
                UniInt.of(1, 5)).read(cfg);

            specialOrderObjectCount = ConfigValue.create(path + ".SpecialOrder.Objects_Count",
                UniInt::read,
                UniInt.of(100, 500)).read(cfg);
        }

        return new JobObjective(id, workType, displayName, icon, objects, currencyDrop, xpDrop, unlockLevel,
            specialOrderAllowed, specialOrderObjectsAmount, specialOrderObjectCount
        );
    }

    public void write(@NotNull FileConfig cfg, @NotNull String path) {
        cfg.set(path + ".Type", this.workId);
        cfg.set(path + ".Display.Name", this.getDisplayName());
        cfg.setItem(path + ".Display.Icon", this.getIcon());

        cfg.set(path + ".Objects", this.getObjects());

        this.getPaymentMap().forEach((currencyId, objectiveReward) -> {
            objectiveReward.write(cfg, path + ".Payment." + currencyId);
        });
        this.getXPReward().write(cfg, path + ".Job_XP");

        cfg.set(path + ".Unlock_Level", this.getUnlockLevel());

        if (Config.SPECIAL_ORDERS_ENABLED.get()) {
            cfg.set(path + ".SpecialOrder.Allowed", this.isSpecialOrderAllowed());
            this.getSpecialOrderObjectsAmount().write(cfg, path + ".SpecialOrder.Objects_Amount");
            this.getSpecialOrderObjectCount().write(cfg, path + ".SpecialOrder.Objects_Count");
        }
    }

    public boolean isObjective(@NotNull WorkObjective workObjective) {
        return this.workId.equalsIgnoreCase(workObjective.getWorkId()) && this.hasObject(workObjective.getObjectName());
    }

    public boolean isWork(@NotNull Work<?, ?> work) {
        return this.workId.equalsIgnoreCase(work.getId());
    }

    @Nullable
    public Work<?, ?> getWork() {
        return WorkRegistry.getByName(this.workId);
    }

    public boolean hasObject(@NotNull String name) {
        return this.getObjects().contains(name.toLowerCase()) || this.getObjects().contains(Placeholders.WILDCARD);
    }

    public boolean isUnlocked(@NotNull Player player, @NotNull JobData jobData) {
        if (player.hasPermission(Perms.BYPASS_OBJECTIVE_UNLOCK_LEVEL)) return true;

        return this.isUnlocked(jobData.getLevel());
    }

    public boolean isUnlocked(int skillLevel) {
        return skillLevel >= this.getUnlockLevel();
    }

    public boolean canPay() {
        return !this.getPaymentMap().values().stream().allMatch(ObjectiveReward::isEmpty);
    }

    @NotNull
    public ObjectiveReward getPaymentInfo(@NotNull Currency currency) {
        return this.getPaymentMap().getOrDefault(currency.getInternalId(), ObjectiveReward.EMPTY);
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getWorkId() {
        return this.workId;
    }

    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    @NotNull
    public ItemStack getIcon() {
        return new ItemStack(icon);
    }

    @NotNull
    public Set<String> getObjects() {
        return objects;
    }

    @NotNull
    public Map<String, ObjectiveReward> getPaymentMap() {
        return paymentMap;
    }

    @NotNull
    public ObjectiveReward getXPReward() {
        return xpReward;
    }

    public int getUnlockLevel() {
        return unlockLevel;
    }

    public boolean isSpecialOrderAllowed() {
        return specialOrderAllowed;
    }

    @NotNull
    public UniInt getSpecialOrderObjectsAmount() {
        return specialOrderObjectsAmount;
    }

    @NotNull
    public UniInt getSpecialOrderObjectCount() {
        return specialOrderObjectCount;
    }
}
