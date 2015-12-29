package theandrey.transferitems;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

public class TransferItemsPlugin extends JavaPlugin implements Listener {

	private final Map<UUID, Long> flooders = new HashMap<>();
	private ExecutorService executor;
	public static Logger log;
	private Config config;
	private Database database;

	@Override
	public void onEnable() {
		log = getLogger();
		getServer().getPluginManager().registerEvents(this, this);
		config = new Config(this);
		try {
			database = new Database(this, config);
		} catch (SQLException ex) {
			Database.handleSqlException(ex);
			getServer().getPluginManager().disablePlugin(this);
		}
	}

	public ExecutorService getExecutor() {
		if(executor == null) executor = Executors.newSingleThreadExecutor();
		return executor;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		try {
			if(!(sender instanceof Player)) throw new CommandException("Команда может быть выполнена только от имени игрока.");
			Player player = (Player)sender;

			switch(command.getName()) {
				case "putitems":
					if(!player.hasPermission("transferitems.put")) throw new CommandException("Ошибка доступа");
					checkForFlood(player);

					List<ItemStack> inventoryStacks = new ArrayList<>();
					PlayerInventory inventory = player.getInventory();
					for(int i = 0; i < inventory.getSize(); i++) {
						ItemStack stack = inventory.getItem(i);
						if(stack == null || stack.getType() == Material.AIR || stack.getAmount() <= 0) continue;
						inventory.setItem(i, null); // забираем предмет у игрока
						inventoryStacks.add(stack);
					}
					if(inventoryStacks.isEmpty()) throw new CommandException("Ваш инвентарь пуст - сохранять нечего.");

					sender.sendMessage(ChatColor.GOLD + "Сохранение предметов...");
					List<StackData> stacksData = new ArrayList<>();
					for(ItemStack stack : inventoryStacks) {
						try {
							stacksData.add(StackData.create(stack));
						} catch (InvalidItemStackException ex) {
							sender.sendMessage(ChatColor.RED + "Неправильный предмет '" + stack.getType().name() + "': " + ex.toString());
							TransferItemsPlugin.log.log(Level.SEVERE, "Неправильный предмет у игрока " + player.getName(), ex);
						}
					}
					if(!stacksData.isEmpty()) {
						getExecutor().submit(() -> {
							if(database.storeItems(player.getUniqueId(), stacksData)) {
								sender.sendMessage(ChatColor.GOLD + "Предметы успешно перемещены в хранилище.");
							} else {
								Bukkit.getScheduler().runTask(this, () -> {
									sender.sendMessage(ChatColor.RED + "Произошла ошибка при переносе предметов в хранилище.");
									tryAddItemsToInventory(player, inventoryStacks);
								});
							}
						});
					} else {
						tryAddItemsToInventory(player, inventoryStacks);
					}

					break;
				case "getitems":
					if(!player.hasPermission("transferitems.get")) throw new CommandException("Ошибка доступа");
					checkForFlood(player);

					sender.sendMessage(ChatColor.GOLD + "Загрузка списка предметов...");
					getExecutor().submit(() -> {
						try {
							List<StackData> list = database.getItems(player.getUniqueId());
							if(!list.isEmpty()) {
								Bukkit.getScheduler().runTask(this, () -> {
									PlayerInventory inv = player.getInventory();
									Set<Integer> removeEntries = new HashSet<>();

									for(StackData stackdata : list) {
										try {
											ItemStack stack = stackdata.createItemStack();
											// ищем свободный слот
											boolean slotFound = false;
											for(int i = 0; i < inv.getSize(); i++) {
												ItemStack slot = inv.getItem(i);
												if(slot == null || slot.getType() == Material.AIR || slot.getAmount() <= 0) {
													inv.setItem(i, stack);
													if(stackdata.getNBT() != null) Utils.placeNBT(stack, stackdata.getNBT()); // Записываем NBT только после помещения в инвентарь
													removeEntries.add(stackdata.getEntryId());
													slotFound = true;
													break;
												}
											}
											if(!slotFound) {
												player.sendMessage(ChatColor.GOLD.toString() + ChatColor.ITALIC + "Мы не смогли поместить все предметы в ваш инвентарь из-за нехватки свободного места.");
												break;
											}
										} catch (InvalidItemStackException ex) {
											TransferItemsPlugin.log.log(Level.SEVERE, "Неправильная стопка", ex);
										}
									}

									player.sendMessage(ChatColor.GOLD + "Предметы помещены в ваш инвентарь.");

									// Удаляем из базы то, что выдали
									if(!removeEntries.isEmpty()) {
										getExecutor().submit(() -> {
											for(Integer entryId : removeEntries) database.deleteItem(entryId);
										});
									}
								});
							} else player.sendMessage(ChatColor.RED + "Ваше хранилище пусто.");
						} catch (Throwable ex) {
							ex.printStackTrace();
						}
					});
					break;
				default:
					throw new CommandException("Неизвестная команда");
			}
		} catch (CommandException ex) {
			sender.sendMessage(ChatColor.RED + ex.getMessage());
		}
		return true;
	}

	private void checkForFlood(Player player) throws CommandException {
		Long lastUse = flooders.get(player.getUniqueId());
		if(lastUse != null && (System.currentTimeMillis() < (lastUse + 2000))) throw new CommandException("Нельзя вызывать данную команду слишком часто.");
		flooders.put(player.getUniqueId(), System.currentTimeMillis());
	}

	public void tryAddItemsToInventory(Player player, Collection<ItemStack> stacks) {
		Set<ItemStack> dropStacks = new HashSet<>();
		PlayerInventory inventory = player.getInventory();
		for(ItemStack stack : stacks) {
			HashMap<Integer, ItemStack> result = inventory.addItem(stack);
			if(!result.isEmpty()) dropStacks.addAll(result.values());
		}
		if(!dropStacks.isEmpty()) {
			for(ItemStack dropedStack : dropStacks) player.getWorld().dropItem(player.getLocation(), dropedStack);
			player.sendMessage(ChatColor.RED + "Некоторые предметы не уместились в вашем инвентаре. Они были выброшены на землю.");
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		flooders.remove(event.getPlayer().getUniqueId());
	}

	@Override
	public void onDisable() {
		if(executor != null) executor.shutdown();
		if(database != null) database.close();
	}

}
