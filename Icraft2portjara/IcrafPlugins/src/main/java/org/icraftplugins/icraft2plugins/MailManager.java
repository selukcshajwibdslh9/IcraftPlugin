package org.icraftplugins.icraft2plugins;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MailManager {

    private final IcraftPlugins plugin;
    private final File file;
    private FileConfiguration config;
    private final List<MailMessage> messages = new ArrayList<>();

    public MailManager(IcraftPlugins plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "mail.yml");
        loadMessages();
    }

    public void addMessage(MailMessage message) {
        messages.add(message);
        saveMessages();
    }

    public List<MailMessage> getInbox(UUID playerUuid) {
        List<MailMessage> inbox = new ArrayList<>();
        for (MailMessage msg : messages) {
            if (msg.getRecipientUuid().equals(playerUuid)) {
                inbox.add(msg);
            }
        }
        return inbox;
    }


    public void loadMessages() {
        if (!file.exists()) return;
        config = YamlConfiguration.loadConfiguration(file);
        messages.clear();

        List<?> rawList = config.getList("messages");
        if (rawList != null) {
            for (Object obj : rawList) {
                if (obj instanceof Map) {
                    try {
                        MailMessage msg = MailMessage.deserialize((Map<String, Object>) obj);
                        messages.add(msg);
                    } catch (Exception ignored) {}
                }
            }
        }
    }
    public List<MailMessage> getOutbox(String senderName) {
        List<MailMessage> outbox = new ArrayList<>();
        for (MailMessage msg : messages) {
            if (msg.getSenderName().equalsIgnoreCase(senderName)) {
                outbox.add(msg);
            }
        }
        return outbox;
    }

    public void clearInbox(UUID playerUuid) {
        messages.removeIf(msg -> msg.getRecipientUuid().equals(playerUuid));
        saveMessages();
    }
    public void saveMessages() {
        if (config == null) config = new YamlConfiguration();
        List<Map<String, Object>> serializedList = new ArrayList<>();
        for (MailMessage msg : messages) {
            serializedList.add(msg.serialize());
        }
        config.set("messages", serializedList);
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить почту: " + e.getMessage());
        }
    }
}