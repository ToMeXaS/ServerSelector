package lt.tomexas.serverselector;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.checkerframework.checker.units.qual.A;

import java.sql.*;

public class Database {

    private static Connection connection;

    public Database(String path) throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + path);

        try(Statement statement = connection.createStatement();) {
            statement.execute("""
                CREATE TABLE IF NOT EXISTS armorStand
                (UUID TEXT PRIMARY KEY)
            """);
        }
    }

    public void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    public void addLocation(ArmorStand as) {
        if (playerExists(as)) updateLocation(as);
        else {
            try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO armorStand (UUID) VALUES (?)")) {
                preparedStatement.setString(1, as.getUniqueId().toString());
                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean playerExists(ArmorStand as) {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM armorStand WHERE UUID = ?")){
            preparedStatement.setString(1, as.getUniqueId().toString());
            ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void updateLocation(ArmorStand as) {
        try (PreparedStatement preparedStatement = connection.prepareStatement("UPDATE armorStand SET UUID = ? WHERE UUID = ?")) {
            preparedStatement.setString(1, as.getUniqueId().toString());
            preparedStatement.setString(2, as.getUniqueId().toString());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public float getBgYaw() {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT yaw FROM bgLocation")) {
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getFloat("yaw");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0f;
    }

    public float getBgPitch() {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT pitch FROM bgLocation")) {
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getFloat("pitch");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0f;
    }

    public String getUUID() {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM armorStand")){
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) return resultSet.getString("UUID");
            else return null;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
