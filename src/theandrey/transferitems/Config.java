package theandrey.transferitems;

import java.io.File;
import java.io.IOException;
import org.bukkit.configuration.file.YamlConfiguration;

public class Config {

	private final TransferItemsPlugin plugin;
	public final String url;
	public final String user;
	public final String password;

	public Config(TransferItemsPlugin plugin) {
		this.plugin = plugin;

		File file = new File(plugin.getDataFolder(), "database.yml");
		YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

		config.addDefault("url", "jdbc:mysql://localhost/minecraft");
		config.addDefault("user", "root");
		config.addDefault("password", "hackme");
		config.options().copyDefaults(true);

		url = config.getString("url");
		user = config.getString("user");
		password = config.getString("password");

		try {
			config.save(file);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

}
