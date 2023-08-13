package org.skydom.server.chosen.coreprotectclean;

import net.coreprotect.database.Database;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public final class CoreProtectClean extends JavaPlugin {
    private String prefix;
    private FileConfiguration config;
    private List<String> cleanProjects;
    private int cleanDay = 7;
    private int period = 24;

    @Override
    public void onEnable() {
        getPrefix();
        config = loadYamlFile("config.yml");
        cleanProjects = config.getStringList("cleanProject");
        cleanDay = config.getInt("day");
        period = config.getInt("period");
        startClean();

    }

    private void startClean() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            Connection conn = Database.getConnection(false);
            for (String project : cleanProjects) {
                int affect = execSpecificClean(conn, project);
                if (affect == 0) {
                    getLogger().info("没有可以清理的 " + project + " 数据");
                } else {
                    getLogger().info("成功清理了 " + affect + " 条 " + project + " 数据");
                }
            }
            closeConnection(conn);
            getLogger().info("完成了一次清理任务");
        }, 20L, period * 20 * 60 * 60);
    }

    private void getPrefix() {
        try {
            // 最新版本的反射获取
            Class<?> configClass = Class.forName("net.coreprotect.config.ConfigHandler");
            Field field_prefix = configClass.getField("prefix");
            prefix = (String) field_prefix.get(null);
        } catch (Exception e) {
            try {
                // 1.12.2以下版本的反射获取
                Class<?> configClass = Class.forName("net.coreprotect.model.Config");
                Field field_prefix = configClass.getField("prefix");
                prefix = (String) field_prefix.get(null);
            } catch (Exception e2) {
                getLogger().warning("您CoreProtect的版本与该插件不兼容");
                // 不兼容 关闭
                onDisable();
            }
        }
        getLogger().info("成功读取表前缀 " + prefix);
    }

    private int execSpecificClean(Connection conn, String name) {
        // 执行指定清理任务的方法 并返回影响条数
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(String.format("delete from %s where time<?", prefix + name));
            ps.setLong(1, System.currentTimeMillis() / 1000 - cleanDay * 24 * 60 * 60);
            return ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closePreparedStatement(ps);
        }
        return 0;
    }

    private void closePreparedStatement(PreparedStatement preparedStatement) {
        // PreparedStatement关闭方法
        if (preparedStatement != null) {
            try {
                preparedStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private static void closeConnection(Connection conn) {
        // Connection关闭方法
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private FileConfiguration loadYamlFile(String path) {
        File file = new File(getDataFolder(), path);
        if (!file.exists())
            saveResource(path, false);
        return YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public void onDisable() {
        getLogger().info("插件已卸载！");
    }
}
