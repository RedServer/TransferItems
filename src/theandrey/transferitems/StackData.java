package theandrey.transferitems;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class StackData {

	private StackData() {
	}

	private int entryId = -1;
	private String material;
	private int amount;
	private short damage;
	private String nbt;

	public int getEntryId() {
		return entryId;
	}

	public String getMaterial() {
		return material;
	}

	public int getAmount() {
		return amount;
	}

	public short getDamage() {
		return damage;
	}

	public String getNBT() {
		return nbt;
	}

	/**
	 * Создаёт ItemStack
	 * @return ItemStack без NBT (свойства можно записывать только после выдачи предмета игроку).
	 * @throws InvalidItemStackException
	 */
	public ItemStack createItemStack() throws InvalidItemStackException {
		try {
			Material mat = Material.getMaterial(material);
			if(mat == null) throw new InvalidItemStackException("Неизвестный материал: " + material);
			if(amount <= 0) throw new InvalidItemStackException("Неправильное количество: " + amount);
			ItemStack stack = new ItemStack(mat, amount, damage);
			if(nbt != null) {
				stack = Utils.createCraftItemStack(stack);
			}
			return stack;
		} catch (Throwable ex) {
			throw new InvalidItemStackException("Ошибка создания ItemStack", ex);
		}
	}

	public static StackData create(ResultSet rs) throws SQLException {
		StackData data = new StackData();
		data.entryId = rs.getInt("id");
		data.material = rs.getString("material");
		data.amount = rs.getInt("amount");
		data.damage = rs.getShort("damage");
		data.nbt = rs.getString("nbt");
		return data;
	}

	public static StackData create(ItemStack stack) throws InvalidItemStackException {
		StackData data = new StackData();
		data.material = stack.getType().name();
		data.amount = stack.getAmount();
		if(data.amount <= 0 || data.amount > 64) throw new InvalidItemStackException("Неправильное кол-во: " + data.amount);
		data.damage = stack.getDurability();
		data.nbt = Utils.readNBT(stack);
		return data;
	}

}
