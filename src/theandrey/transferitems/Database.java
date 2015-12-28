package theandrey.transferitems;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;

public class Database {

	private final Connection connection;
	private final PreparedStatement statementInsertItem;
	private final PreparedStatement statementDeleteItem;
	private final PreparedStatement statementSelectItems;

	public Database(TransferItemsPlugin plugin, Config config) throws SQLException {
		Properties info = new Properties();
		info.put("autoReconnect", "true");
		info.put("user", config.user);
		info.put("password", config.password);
		info.put("useUnicode", "true");
		info.put("characterEncoding", "utf8");

		connection = DriverManager.getConnection(config.url, info);
		Statement createTable = connection.createStatement();
		createTable.execute("CREATE TABLE IF NOT EXISTS `transfer_items` ("
				+ "`id` int(10) NOT NULL AUTO_INCREMENT,"
				+ "`player` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,"
				+ "`material` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,"
				+ "`amount` int(5) NOT NULL,"
				+ "`damage` int(5) NOT NULL,"
				+ "`nbt` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci,"
				+ "PRIMARY KEY (`id`)"
				+ ")"
				+ "ENGINE=InnoDB DEFAULT CHARACTER SET=utf8 COLLATE=utf8_general_ci ROW_FORMAT=COMPACT");
		createTable.close();

		statementInsertItem = connection.prepareStatement("INSERT INTO `transfer_items` (`player`, `material`, `amount`, `damage`, `nbt`) VALUES (?, ?, ?, ?, ?)");
		statementDeleteItem = connection.prepareStatement("DELETE FROM `transfer_items` WHERE `id` = ?");
		statementSelectItems = connection.prepareStatement("SELECT * FROM `transfer_items` WHERE `player` = ? ORDER BY `id` ASC");
	}

	/**
	 * Удаляет предмет из базы
	 * @param id ID записи в таблице
	 * @return true, если запись успешно удалена
	 */
	public boolean deleteItem(int id) {
		try {
			statementDeleteItem.setInt(1, id);
			int affected = statementDeleteItem.executeUpdate();
			return (affected >= 1);
		} catch (SQLException ex) {
			handleSqlException(ex);
		}
		return false;
	}

	/**
	 * Список предметов игрока
	 * @param playerId ID игрока
	 * @return Список предметов (id записи : предмет)
	 */
	public List<StackData> getItems(UUID playerId) {
		List<StackData> list = new ArrayList<>();
		try {
			statementSelectItems.setString(1, playerId.toString());
			ResultSet result = statementSelectItems.executeQuery();
			while(result.next()) {
				list.add(StackData.create(result));
			}
		} catch (SQLException ex) {
			handleSqlException(ex);
		}
		return list;
	}

	public boolean storeItems(UUID playerId, Collection<StackData> items) {
		int inserted = 0;
		if(!items.isEmpty()) {
			for(StackData stack : items) {
				try {
					statementInsertItem.setString(1, playerId.toString());
					statementInsertItem.setString(2, stack.getMaterial());
					statementInsertItem.setInt(3, stack.getAmount());
					statementInsertItem.setInt(4, stack.getDamage());
					statementInsertItem.setString(5, stack.getNBT());
					inserted += statementInsertItem.executeUpdate();
				} catch (SQLException ex) {
					handleSqlException(ex);
				}
			}
		}
		return (inserted == items.size());
	}

	public static void handleSqlException(SQLException ex) {
		TransferItemsPlugin.log.log(Level.SEVERE, "Возникла ошибка при работе с базой данных.", ex);
	}

	public void close() {
		if(connection != null) {
			try {
				connection.close();
			} catch (SQLException ex) {
				handleSqlException(ex);
			}
		}
	}

}
