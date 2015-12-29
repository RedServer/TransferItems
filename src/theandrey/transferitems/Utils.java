package theandrey.transferitems;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

public class Utils {

	private static Method methodAsNmsCopy;
	private static Method methodNbtToJson;
	private static Field fieldTag;
	private static Method methodParseJson;
	private static Method methodAsCraftCopy;
	private static Field fieldHandle;

	static {
		try {
			String packageName = Bukkit.getServer().getClass().getPackage().getName();
			String nmspackageversion = packageName.substring(packageName.lastIndexOf('.') + 1);

			Class<?> craftItemStack = Class.forName("org.bukkit.craftbukkit." + nmspackageversion + ".inventory.CraftItemStack");
			methodAsNmsCopy = craftItemStack.getMethod("asNMSCopy", ItemStack.class);
			methodAsCraftCopy = craftItemStack.getMethod("asCraftCopy", ItemStack.class);
			fieldHandle = craftItemStack.getDeclaredField("handle");
			fieldHandle.setAccessible(true);

			methodNbtToJson = Class.forName("dh").getMethod("toString"); // NBTTagCompound - toString()
			fieldTag = Class.forName("add").getField("field_77990_d"); // ItemStack - stackTagCompound
			methodParseJson = Class.forName("eb").getMethod("func_150315_a", String.class); // JsonToNBT - func_150315_a()
		} catch (Throwable ex) {
			TransferItemsPlugin.log.log(Level.SEVERE, "Ошибка инициализации методов", ex);
		}
	}

	public static String readNBT(ItemStack stack) {
		try {
			Object vanillaStack = methodAsNmsCopy.invoke(null, stack);
			Object stackTag = fieldTag.get(vanillaStack);
			if(stackTag == null) return null;
			return (String)methodNbtToJson.invoke(stackTag);
		} catch (Exception ex) {
			TransferItemsPlugin.log.log(Level.SEVERE, "Ошибка чтения NBT предмета", ex);
		}
		return null;
	}

	public static void placeNBT(ItemStack stack, String nbt) {
		if(stack != null && nbt != null && !nbt.isEmpty() && !nbt.equals("[]") && !nbt.equals("{}")) {
			try {
				Object tag = methodParseJson.invoke(null, nbt);
				Object vanillaStack = fieldHandle.get(stack);
				fieldTag.set(vanillaStack, tag);
			} catch (Exception ex) {
				TransferItemsPlugin.log.log(Level.SEVERE, "Ошибка записи NBT", ex);
			}
		}
	}

	public static ItemStack createCraftItemStack(ItemStack base) {
		try {
			return (ItemStack)methodAsCraftCopy.invoke(null, base);
		} catch (Exception ex) {
			TransferItemsPlugin.log.log(Level.SEVERE, "Ошибка создания CraftStack", ex);
		}
		return null;
	}

}
