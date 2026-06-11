package dev.zcripted.obx.feature.chat;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.module.AbstractModule;
import dev.zcripted.obx.feature.chat.listener.ChatManagementListener;
import dev.zcripted.obx.api.chat.ChatService;

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
    protected void onEnable(ObxPlugin plugin) {
        ChatService chatService = service(ChatService.class, new dev.zcripted.obx.feature.chat.service.ChatServiceImpl(plugin));
        chatService.load();
        listener(new ChatManagementListener(plugin, chatService));
    }

    @Override
    public void reload(ObxPlugin plugin) {
        ChatService chatService = plugin.getChatService();
        if (chatService != null) {
            chatService.reload();
        }
    }
}