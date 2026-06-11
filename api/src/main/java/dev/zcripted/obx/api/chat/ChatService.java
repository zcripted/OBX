package dev.zcripted.obx.api.chat;

/**
 * Public chat-configuration API. Implemented by
 * {@code feature.chat.service.ChatServiceImpl}; exposes the resolved chat
 * formatting/templates and toggles read by the chat listener and consumers.
 */
public interface ChatService {

    void load();

    void reload();

    boolean isEnabled();

    boolean isConsoleMirror();

    boolean isConsoleTimestampEnabled();

    String getConsoleTimestampFormat();

    boolean allowFormattingInMessages();

    String getMasterTemplate();

    String getUsernameTemplate();

    String getStaffUsernameTemplate();

    String getSeparatorCharacter();

    String getSeparatorTemplate();

    String getMessageTemplate();

    boolean isStaffPrefixEnabled();

    String getStaffPrefix();
}