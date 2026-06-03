package dev.zcripted.obx.feature.chat;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.core.module.AbstractModule;
import dev.zcripted.obx.feature.chat.listener.ChatManagementListener;
import dev.zcripted.obx.feature.chat.service.ChatService;

/**
 * Chat formatting + chat-management feature. Owns {@link ChatService} and the
 * {@link ChatManagementListener} that applies formatting/filtering to chat.
 */
public final class ChatModule extends AbstractModule {

    @Override
    public String id() {
        return "chat";
    }

    @Override
    protected void onEnable(OBX plugin) {
        ChatService chatService = service(ChatService.class, new ChatService(plugin));
        chatService.load();
        listener(new ChatManagementListener(plugin, chatService));
    }

    @Override
    public void reload(OBX plugin) {
        ChatService chatService = plugin.getChatService();
        if (chatService != null) {
            chatService.reload();
        }
    }
}
