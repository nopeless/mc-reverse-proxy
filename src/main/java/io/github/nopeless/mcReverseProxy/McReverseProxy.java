package io.github.nopeless.mcReverseProxy;

import com.jcraft.jsch.*;

import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public final class McReverseProxy extends JavaPlugin implements CommandExecutor {
    private Map<String, Session> sessions;
    private Map<String, BukkitTask> tasks;

    static McReverseProxy instance;

    @Override
    public void onEnable() {
        instance = this;

        getCommand("mcrp").setExecutor(new McrpCommand(this));
        saveConfig();
        startLifecycle();
    }

    public void startLifecycle() {
        sessions = new HashMap<>();
        tasks = new HashMap<>();

        // Read config
        var config = new Config(getConfig());

        if (config.remotes == null) {
            getLogger().warning("No remotes found. Please edit config and reload.");
            return;
        }

        // Loop all remotes
        List<List<String>> combinedErrors = new ArrayList<>();
        for (var remote : config.remotes.values()) {
            var errors = remote.getErrors();
            if (!errors.isEmpty()) {
                for (var error : errors) {
                    getLogger().warning("Remote " + remote.id + ": " + error);
                }
            }
        }

        // Warn user
        if (!combinedErrors.isEmpty()) {
            getLogger().warning("Errors found in config:");
            for (var errors : combinedErrors) {
                for (var error : errors) {
                    getLogger().warning(error);
                }
            }
            return;
        }

        // Create connection objects
        for (var remote : config.remotes.values()) {
            getLogger().info("Remote: " + remote.id);
            getLogger().info(" - Host: " + remote.host);
            getLogger().info(" - User: " + remote.user);
            getLogger().info(" - Local port: " + remote.localPort);
            getLogger().info(" - Remote port: " + remote.remotePort);
            try {
                var session = createSession(remote);
                getLogger().info("[" + remote.id + "] Connected to " + remote.host);
                sessions.put(remote.id, session);
            } catch (JSchException e) {
                getLogger().warning("Failed to create session for " + remote.id + " (disabled): " + e.getMessage());
            } catch (IOException e) {
                getLogger().warning("Failed to read private key for " + remote.id + " (disabled): " + e.getMessage());
            }
        }

        // Create connection tasks with interval
        for (var remote : config.remotes.values()) {
            var session = sessions.get(remote.id);
            if (session == null) {
                continue;
            }
            var task = getServer().getScheduler().runTaskTimer(this, () -> {
                if (session.isConnected()) {
                    return;
                }
                getLogger().info("Reconnecting to " + remote.id + "...");
                try {
                    connect(session);
                    session.setPortForwardingR(remote.localPort, remote.host, remote.remotePort);
                    getLogger().info("Reconnected to " + remote.id);
                } catch (JSchException e) {
                    getLogger().warning("Failed to connect to " + remote.id + ": " + e.getMessage());
                }
            }, 0, remote.interval / 50L);
            tasks.put(remote.id, task);
        }
    }

    private void connect(Session session) throws JSchException {
        if (!session.isConnected()) {
            session.connect();
        }
    }

    private Session createSession(Config remote) throws JSchException, IOException {
        var jsch = new JSch();

        // Read file content from
        assert remote.privateKey != null;
        jsch.addIdentity(remote.privateKey);

        var session = jsch.getSession(remote.user, remote.host);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        session.setPortForwardingR(remote.remotePort, "localhost", remote.localPort);
        return session;
    }

    private void disconnectAll() {
        if (sessions != null) {
            for (var session : sessions.values()) {
                session.disconnect();
            }
        }
        if (tasks != null) {
            for (var task : tasks.values()) {
                task.cancel();
            }
        }
    }

    @Override
    public void onDisable() {
        disconnectAll();
        instance = null;
    }

    public void reload() {
        disconnectAll();
        reloadConfig();
        startLifecycle();
    }
}