package org.icraftplugins.icraft2plugins;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MailMessage {
    private final String senderName;
    private final UUID recipientUuid;
    private final String title;
    private final String content;
    private boolean read;

    public MailMessage(String senderName, UUID recipientUuid, String title, String content) {
        this.senderName = senderName;
        this.recipientUuid = recipientUuid;
        this.title = title;
        this.content = content;
        this.read = false;
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("senderName", senderName);
        map.put("recipientUuid", recipientUuid.toString());
        map.put("title", title);
        map.put("content", content);
        map.put("read", read);
        return map;
    }

    public static MailMessage deserialize(Map<String, Object> map) {
        String sender = (String) map.get("senderName");
        UUID uuid = UUID.fromString((String) map.get("recipientUuid"));
        String title = (String) map.get("title");
        String content = (String) map.get("content");
        boolean read = (Boolean) map.get("read");
        MailMessage msg = new MailMessage(sender, uuid, title, content);
        msg.setRead(read);
        return msg;
    }

    public String getSenderName() { return senderName; }
    public UUID getRecipientUuid() { return recipientUuid; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}