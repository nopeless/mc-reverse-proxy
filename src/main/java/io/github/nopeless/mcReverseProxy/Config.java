package io.github.nopeless.mcReverseProxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;

public class Config {
    // global config
    public final String host;
    public final String user;
    public final String privateKey;
    public final int interval;

    public final int localPort;
    public final int remotePort;

    // null if local
    public final Map<String, Config> remotes;
    // null if global
    public final String id;

    public Config(FileConfiguration config) {
        id = null;

        host = config.getString("host");
        user = config.getString("user");
        interval = config.getInt("interval", 5000); // 5 seconds
        privateKey = config.getString("privateKey");

        localPort = 0;
        remotePort = 0;

        if (config.getConfigurationSection("remotes") != null) {
            remotes = new HashMap<>();
            for (var key : config.getConfigurationSection("remotes").getKeys(false)) {
                remotes.put(key, new Config(config, this, key));
            }
        } else {
            remotes = null;
        }
    }

    private Config(FileConfiguration config, Config globalConfig, String id) {
        this.id = id;
        remotes = null;

        var base = "remotes." + id + ".";

        host = config.getString(base + "host", globalConfig.host);
        user = config.getString(base + "user", globalConfig.user);
        String pk = config.getString(base + "private-key", globalConfig.privateKey);
        privateKey = pk == null ? null : McReverseProxy.instance.getDataFolder().toPath().resolve(pk).toFile().getAbsolutePath();
        interval = config.getInt(base + "interval", globalConfig.interval);

        localPort = config.getInt(base + "local-port");
        remotePort = config.getInt(base + "remote-port");
    }

    public List<String> getErrors() {
        var errors = new ArrayList<String>();

        if (host == null) {
            errors.add("host is not set");
        }

        if (user == null) {
            errors.add("user is not set");
        }

        if (privateKey == null) {
            errors.add("private-key is not set");
        }

        if (interval == 0) {
            errors.add("interval is not set");
        }

        if (localPort == 0) {
            errors.add("local-port is not set");
        }

        if (remotePort == 0) {
            errors.add("remote-port is not set");
        }

        return errors;
    }
}
