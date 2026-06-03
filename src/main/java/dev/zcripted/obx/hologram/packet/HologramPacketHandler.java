package dev.zcripted.obx.hologram.packet;

import dev.zcripted.obx.OBX;
import dev.zcripted.obx.hologram.interact.InteractionDispatcher;
import dev.zcripted.obx.hologram.model.HologramId;
import dev.zcripted.obx.hologram.service.HologramService;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import org.bukkit.entity.Player;

/**
 * Inbound-only Netty handler installed per-player. Inspects each packet's
 * simple class name; if it matches the expected interact packets it forwards
 * a decoded action to {@link InteractionDispatcher} via the main thread.
 *
 * <p>Outbound traffic ({@code write}) is never touched. The handler
 * <strong>always</strong> calls {@code super.channelRead(ctx, msg)} so the
 * vanilla pipeline still receives the packet — we are an observer, not an
 * interceptor. Exceptions are caught and swallowed (logged at most once per
 * minute via {@link PacketAvailability#noteFailure}).
 */
public final class HologramPacketHandler extends ChannelDuplexHandler {

    private final OBX plugin;
    private final HologramService service;
    private final Player viewer;

    public HologramPacketHandler(OBX plugin, HologramService service, Player viewer) {
        this.plugin = plugin;
        this.service = service;
        this.viewer = viewer;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg != null && service != null && service.isActive()) {
                String simpleName = msg.getClass().getSimpleName();
                if ("ServerboundInteractPacket".equals(simpleName)
                        || "PacketPlayInUseEntity".equals(simpleName)) {
                    final InteractDecoder.Decoded decoded = InteractDecoder.decode(msg);
                    if (decoded != null && decoded.entityId > 0) {
                        final HologramId id = service.getRegistry().resolveByEntityId(decoded.entityId);
                        if (id != null) {
                            // Dispatch on main thread — interaction handlers may run commands.
                            plugin.getSchedulerAdapter().runNow(new Runnable() {
                                @Override
                                public void run() {
                                    InteractionDispatcher.dispatch(plugin, service, viewer, id, decoded);
                                }
                            });
                        }
                    }
                }
            }
        } catch (Throwable throwable) {
            PacketAvailability.noteFailure(plugin, throwable);
        }
        super.channelRead(ctx, msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // Don't let our handler kill the channel — log + pass through.
        PacketAvailability.noteFailure(plugin, cause);
        super.exceptionCaught(ctx, cause);
    }
}
