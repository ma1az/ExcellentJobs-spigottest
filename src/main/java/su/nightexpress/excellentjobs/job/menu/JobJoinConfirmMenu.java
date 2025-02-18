package su.nightexpress.excellentjobs.job.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentjobs.JobsPlugin;
import su.nightexpress.excellentjobs.config.Config;
import su.nightexpress.excellentjobs.job.impl.Job;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.menu.MenuOptions;
import su.nightexpress.nightcore.menu.MenuViewer;
import su.nightexpress.nightcore.menu.impl.ConfigMenu;
import su.nightexpress.nightcore.menu.item.ItemHandler;
import su.nightexpress.nightcore.menu.item.MenuItem;
import su.nightexpress.nightcore.menu.link.Linked;
import su.nightexpress.nightcore.menu.link.ViewLink;
import su.nightexpress.nightcore.util.ItemReplacer;
import su.nightexpress.nightcore.util.ItemUtil;
import su.nightexpress.nightcore.util.Lists;

import java.util.ArrayList;
import java.util.List;

import static su.nightexpress.excellentjobs.Placeholders.*;
import static su.nightexpress.nightcore.util.text.tag.Tags.*;

@Deprecated
public class JobJoinConfirmMenu extends ConfigMenu<JobsPlugin> implements Linked<Job> {

    private static final String FILE_NAME = "job_join_confirm.yml";

    private final ViewLink<Job> link;

    private final ItemHandler acceptHandler;
    private final ItemHandler returnHanler;

    public JobJoinConfirmMenu(@NotNull JobsPlugin plugin) {
        super(plugin, FileConfig.loadOrExtract(plugin, Config.DIR_MENU, FILE_NAME));
        this.link = new ViewLink<>();

        this.addHandler(this.acceptHandler = new ItemHandler("accept", (viewer, event) -> {
            Player player = viewer.getPlayer();
            Job job = this.getLink().get(player);

            this.plugin.getJobManager().joinJob(player, job, false);
            this.runNextTick(player::closeInventory);
        }));
        this.addHandler(this.returnHanler = ItemHandler.forReturn(this, (viewer, event) -> {
            this.runNextTick(() -> plugin.getJobManager().openJobsMenu(viewer.getPlayer()));
        }));

        this.load();

        this.getItems().forEach(menuItem -> {
            menuItem.getOptions().addDisplayModifier((viewer, item) -> {
                Job job = this.getLink().get(viewer);

                ItemReplacer.create(item).readMeta().replace(job.replacePlaceholders()).writeMeta();
            });
        });
    }

    @Override
    protected void onPrepare(@NotNull MenuViewer viewer, @NotNull MenuOptions options) {
        Job job = this.getLink().get(viewer);
        options.setTitle(job.replacePlaceholders().apply(options.getTitle()));
    }

    @Override
    protected void onReady(@NotNull MenuViewer viewer, @NotNull Inventory inventory) {

    }

    @NotNull
    @Override
    public ViewLink<Job> getLink() {
        return link;
    }

    @Override
    @NotNull
    protected MenuOptions createDefaultOptions() {
        return new MenuOptions(BLACK.enclose("Join " + JOB_NAME + " Job?"), 9, InventoryType.CHEST);
    }

    @Override
    @NotNull
    protected List<MenuItem> createDefaultItems() {
        List<MenuItem> list = new ArrayList<>();

        ItemStack accept = ItemUtil.getSkinHead("a79a5c95ee17abfef45c8dc224189964944d560f19a44f19f8a46aef3fee4756");
        ItemUtil.editMeta(accept, meta -> {
            meta.setDisplayName(LIGHT_GREEN.enclose(BOLD.enclose("Confirm")));
        });
        list.add(new MenuItem(accept).setSlots(8).setPriority(100).setHandler(this.acceptHandler));

        ItemStack decline = ItemUtil.getSkinHead("27548362a24c0fa8453e4d93e68c5969ddbde57bf6666c0319c1ed1e84d89065");
        ItemUtil.editMeta(decline, meta -> {
            meta.setDisplayName(LIGHT_RED.enclose(BOLD.enclose("Cancel")));
        });
        list.add(new MenuItem(decline).setSlots(0).setPriority(100).setHandler(this.returnHanler));

        ItemStack jobItem = ItemUtil.getSkinHead("7b41996fd20ca21d79adfc0e12057b2f2ceadf7b3cf5bb5f8a92fe3460161acd");
        ItemUtil.editMeta(jobItem, meta -> {
            meta.setDisplayName(LIGHT_YELLOW.enclose(BOLD.enclose(JOB_NAME)));
            meta.setLore(Lists.newList(LIGHT_GRAY.enclose(JOB_DESCRIPTION)));
        });
        list.add(new MenuItem(jobItem).setSlots(4).setPriority(100));

        return list;
    }

    @Override
    protected void loadAdditional() {

    }
}
