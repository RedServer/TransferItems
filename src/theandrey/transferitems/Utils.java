package theandrey.transferitems;

import me.dpohvar.powernbt.nbt.NBTBase;
import me.dpohvar.powernbt.nbt.NBTContainerItem;
import org.bukkit.inventory.ItemStack;

public class Utils {

	/**
	 * Читает NBT предмета
	 * @param stack Предмет
	 * @return NBT свойства предмета, если их нет - null
	 */
	public static NBTBase readNBT(ItemStack stack) {
		NBTContainerItem container = new NBTContainerItem(stack);
		return container.readTag();
	}

	public static void placeNBT(ItemStack stack, NBTBase nbt) {
		NBTContainerItem containerItem = new NBTContainerItem(stack);
		containerItem.setTag(nbt);
	}

}
