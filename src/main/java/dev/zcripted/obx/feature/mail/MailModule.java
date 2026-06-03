package dev.zcripted.obx.feature.mail;

import dev.zcripted.obx.core.ObxPlugin;
import dev.zcripted.obx.core.module.AbstractModule;
import dev.zcripted.obx.feature.mail.command.BroadcastCommand;
import dev.zcripted.obx.feature.mail.command.IgnoreCommand;
import dev.zcripted.obx.feature.mail.command.InboxCommand;
import dev.zcripted.obx.feature.mail.command.MailCommand;
import dev.zcripted.obx.feature.mail.command.MeCommand;
import dev.zcripted.obx.feature.mail.command.MsgCommand;
import dev.zcripted.obx.feature.mail.command.ReplyCommand;
import dev.zcripted.obx.feature.mail.command.SocialSpyCommand;
import dev.zcripted.obx.feature.mail.command.StaffChatCommand;
import dev.zcripted.obx.feature.mail.mail.MailService;
import dev.zcripted.obx.feature.mail.pm.MessageStore;
import dev.zcripted.obx.feature.mail.pm.PrivateMessageService;
import dev.zcripted.obx.feature.mail.pm.gui.InboxMenuListener;

/**
 * Mail / messaging feature: private {@code /msg}+reply+inbox (PrivateMessageService,
 * also a listener), offline mailbox (MailService), ignore + social-spy, and the
 * broadcast/me/staffchat chat-dispatch commands.
 */
public final class MailModule extends AbstractModule {

    @Override
    public String id() {
        return "mail";
    }

    @Override
    protected void onEnable(ObxPlugin plugin) {
        MessageStore store = new MessageStore(plugin);
        store.load();
        PrivateMessageService pm = service(PrivateMessageService.class, new PrivateMessageService(plugin, store));
        MailService mail = service(MailService.class, new MailService(plugin));
        mail.load();

        listener(pm);
        listener(new InboxMenuListener(plugin));

        command("msg", new MsgCommand(plugin));
        command("rply", new ReplyCommand(plugin));
        command("inbox", new InboxCommand(plugin));
        command("ignore", new IgnoreCommand(plugin));
        command("socialspy", new SocialSpyCommand(plugin));
        command("mail", new MailCommand(plugin));
        command("me", new MeCommand(plugin));
        command("broadcast", new BroadcastCommand(plugin));
        command("staffchat", new StaffChatCommand(plugin));
    }
}
