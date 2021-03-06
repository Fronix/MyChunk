package me.ellbristow.mychunk.utils;

import java.io.File;
import java.sql.*;
import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class SQLiteBridge {

    private static final Plugin plugin;
    private static Connection conn;
    private static File sqlFile;
    private static Statement statement;
    private static HashMap<Integer, HashMap<String, Object>> rows = new HashMap<Integer, HashMap<String, Object>>();
    private static int numRows = 0;
    
    static {
        plugin = Bukkit.getPluginManager().getPlugin("MyChunk");
        sqlFile = new File("plugins/" + plugin.getDataFolder().getName() + File.separator + plugin.getName() + ".db");
    }
    
    public static Connection getConnection() {
        try {
            if (conn == null || conn.isClosed()) {
                return open();
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }
    
    public static Connection open() {
        try {
            if (conn == null || conn.isClosed()) {
            	Class.forName("org.sqlite.JDBC");
                conn = DriverManager.getConnection("jdbc:sqlite:" + sqlFile.getAbsolutePath());
            }
            return conn;
        } catch (Exception e) {
            plugin.getLogger().severe(e.getMessage());
        }
        return null;
    }
    
    public static void close() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (Exception e) {
            plugin.getLogger().severe(e.getMessage());
        }
    }
    
    public static boolean checkTable(String tableName) {
        DatabaseMetaData dbm;
        try {
            dbm = open().getMetaData();
            ResultSet tables = dbm.getTables(null, null, tableName, null);
            if (tables.next()) {
                close();
                return true;
            } else {
                close();
                return false;
            }
        } catch (Exception e) {
            plugin.getLogger().severe(e.getMessage());
            return false;
        }
    }
    
    public static boolean createTable(String tableName, String[] columns, String[] dims) {
        try {
            if (conn == null || conn.isClosed())
                open();
            statement = conn.createStatement();
            String query = "CREATE TABLE " + tableName + "(";
            for (int i = 0; i < columns.length; i++) {
                if (i!=0) {
                    query += ",";
                }
                query += columns[i] + " " + dims[i];
            }
            query += ")";
            statement.execute(query);
        } catch (Exception e) {
            plugin.getLogger().severe(e.getMessage());
        }
        close();
        return true;
    }
    
    public static ResultSet query(String query) {
        try {
            if (conn == null || conn.isClosed())
                open();
            statement = conn.createStatement();
            ResultSet results = statement.executeQuery(query);
            close();
            return results;
        } catch (Exception e) {
            if (!e.getMessage().contains("not return ResultSet") || (e.getMessage().contains("not return ResultSet") && query.startsWith("SELECT"))) {
                plugin.getLogger().severe(e.getMessage());
                e.printStackTrace();
            }
        }
        return null;
    }
    
    public static HashMap<Integer, HashMap<String, Object>> select(String fields, String tableName, String where, String group, String order) {
        if ("".equals(fields) || fields == null) {
            fields = "*";
        }
        String query = "SELECT " + fields + " FROM " + tableName;
        try {
            if (conn == null || conn.isClosed())
                open();
            statement = conn.createStatement();
            if (!"".equals(where) && where != null) {
                query += " WHERE " + where;
            }
            if (!"".equals(group) && group != null) {
                query += " GROUP BY " + group;
            }
            if (!"".equals(order) && order != null) {
                query += " ORDER BY " + order;
            }
            rows.clear();
            numRows = 0;
            ResultSet results = statement.executeQuery(query);
            if (results != null) {
                int columns = results.getMetaData().getColumnCount();
                String columnNames = "";
                for (int i = 1; i <= columns; i++) {
                    if (!"".equals(columnNames)) {
                        columnNames += ",";
                    }
                    columnNames += results.getMetaData().getColumnName(i);
                }
                String[] columnArray = columnNames.split(",");
                numRows = 0;
                while (results.next()) {
                    HashMap<String, Object> thisColumn = new HashMap<String, Object>();
                    for (String columnName : columnArray) {
                        thisColumn.put(columnName, results.getObject(columnName));
                    }
                    rows.put(numRows, thisColumn);
                    numRows++;
                }
                results.close();
                close();
                return rows;
            } else {
                results.close();
                close();
                return null;
            }
        } catch (Exception e) {
            plugin.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }
        close();
        return null;
    }
    
    public static boolean tableContainsColumn(String tableName, String columnName) {
        try {
            if (conn == null || conn.isClosed())
                open();
            statement = conn.createStatement();
            ResultSet rs = statement.executeQuery("SELECT " + columnName + " FROM " + tableName + " LIMIT 1");
            if (rs == null) {
                return false;
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("no such column: " + columnName)) {
                return false;
            }
            plugin.getLogger().severe(e.getMessage());
        }
        return true;
    }
    
    public static void addColumn(String tableName, String columnDef) {
        try {
            if (conn == null || conn.isClosed())
                open();
            statement = conn.createStatement();
            statement.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnDef);
        } catch (SQLException e) {
            plugin.getLogger().severe(e.getMessage());
        }
    }
    
}
