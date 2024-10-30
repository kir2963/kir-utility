package kir.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Properties;

public final class ConfigManager {

    private static ConfigManager instance;
    private static File context;
    private final LinkedHashMap<String, String> configs;

    private ConfigManager(File context) {
        ConfigManager.context = context;
        configs = new LinkedHashMap<>();
        load();
    }

    public static synchronized ConfigManager getInstance(File context) {
        if (instance == null || ConfigManager.context != context) {
            instance = new ConfigManager(context);
        }
        return instance;
    }

    public String get(String key) {
        synchronized (configs) {
            return configs.get(key);
        }
    }
    public <T> T get(String key, Class<T> targetType) {
        synchronized (configs) {
            var value = configs.get(key);
            if (value != null) {
                return targetType.cast(value);
            }
            return null;
        }
    }
    public void set(String key, String value) {
        synchronized (configs) {
            configs.put(key, value);
        }
    }

    public boolean save() {
        synchronized (configs) {
            if (context == null) {
                return false;
            }

            var properties = new Properties();
            properties.putAll(configs);

            try (var os = new FileOutputStream(context)) {
                properties.store(os, null); // Saving to the context
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
    }
    public boolean load() {
        if (context == null || !context.exists()) {
            return false;
        }

        Properties properties = new Properties();
        try (var is = new FileInputStream(context)) {
            properties.load(is);
            synchronized (configs) {
                configs.clear(); // Clear current config before loading from context
                for (String key : properties.stringPropertyNames()) {
                    configs.put(key, properties.getProperty(key));
                }
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

}
