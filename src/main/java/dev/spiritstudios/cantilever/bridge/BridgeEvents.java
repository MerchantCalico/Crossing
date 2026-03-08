package dev.spiritstudios.cantilever.bridge;

import dev.spiritstudios.cantilever.Cantilever;
import dev.spiritstudios.cantilever.CantileverConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static dev.spiritstudios.cantilever.Cantilever.*;

public class BridgeEvents {
	public static void initMinecraft() {
		ServerLifecycleEvents.SERVER_STARTING.register(
			id("before_bridge"),
			server -> {
				execIfBridgePresent(bridge ->
					bridge().setServer(server)
				);
			}
		);

		ServerLifecycleEvents.SERVER_STARTING.addPhaseOrdering(
			id("before_bridge"),
			id("after_bridge")
		);

		ServerLifecycleEvents.SERVER_STARTING.register(
			id("after_bridge"),
			server ->
				execIfBridgePresent(bridge ->
					bridge.sendBasicMessageM2D(
						CantileverConfig.HOLDER.get()
							.minecraftToDiscordFormatting()
							.systemMessageFormat()
							.formatted("Server starting...")
					)
				)
		);

		ServerLifecycleEvents.SERVER_STARTED.register(server ->
			execIfBridgePresent(bridge ->
				bridge.sendBasicMessageM2D(CantileverConfig.HOLDER.get()
					.minecraftToDiscordFormatting()
					.systemMessageFormat()
					.formatted("Server started")
				)
			)
		);

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			if (scheduler != null) {
				scheduler.shutdownNow();
			}
			execIfBridgePresent(bridge ->
				bridge.sendBasicMessageM2D(CantileverConfig.HOLDER.get()
					.minecraftToDiscordFormatting()
					.systemMessageFormat()
					.formatted("Server stopping...")
				)
			);
		});

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			execIfBridgePresent(bridge -> {
					bridge.sendShutdownMessageM2D(CantileverConfig.HOLDER.get()
						.minecraftToDiscordFormatting()
						.systemMessageFormat()
						.formatted("Server stopped")
					);
					bridge.stop();
				}
			);
		});

		ServerMessageEvents.GAME_MESSAGE.register((server, message, overlay) -> {
			if (message.getContents() instanceof BridgeTextContent) return;
			execIfBridgePresent(bridge ->
					bridge.sendBasicMessageM2D(CantileverConfig.HOLDER.get()
						.minecraftToDiscordFormatting()
						.systemMessageFormat()
						.formatted(message.getString())
					)
			);
		});

		ServerMessageEvents.COMMAND_MESSAGE.register((message, source, parameters) -> {
			if (message.decoratedContent().getContents() instanceof BridgeTextContent) return;

			execIfBridgePresent(bridge -> {
				if (source.isPlayer()) {
					bridge.sendWebhookMessageM2D(message.decoratedContent(), source.getPlayer());
					return;
				}
				bridge.sendBasicMessageM2D(CantileverConfig.HOLDER.get()
					.minecraftToDiscordFormatting()
					.systemMessageFormat()
					.formatted(message.decoratedContent().getString())
				);
			});
		});

		ServerMessageEvents.CHAT_MESSAGE.register((message, user, params) ->
			execIfBridgePresent(bridge ->
				bridge.sendWebhookMessageM2D(message.decoratedContent(), user)
			)
		);
	}

	private static ScheduledExecutorService scheduler;

	public static void initDiscord(Bridge bridge) {
		if (bridge == null)
			return;

		JDA api = bridge.api();
		if (api == null)
			return;

		api.addEventListener(new ListenerAdapter() {
			@Override
			public void onMessageReceived(@NotNull MessageReceivedEvent event) {
				if (
					!bridge.channel().map(c -> c == event.getChannel()).orElse(false)
						|| event.getAuthor().getIdLong() == event.getJDA().getSelfUser().getIdLong()
						|| event.getAuthor().getIdLong() == bridge.getWebhookId()
				) {
					return;
				}

				String authorName = event.getMember() != null ?
					event.getMember().getEffectiveName() : event.getAuthor().getEffectiveName();

				// TODO: Implement PK API checks.
				if (scheduler == null && CantileverConfig.HOLDER.get().discordChatProxy().messageDelay() > 0) {
					scheduler = Executors.newScheduledThreadPool(1, runnable -> {
						var thread = new Thread(runnable, "Cantilever D2M Message Scheduler");
						thread.setDaemon(true);
						thread.setUncaughtExceptionHandler((thread1, throwable) -> Cantilever.LOGGER.error("Caught exception in D2M Message Scheduler", throwable));
						return thread;
					});
				}

				if (scheduler != null) {
					scheduler.schedule(() -> {
						event.getChannel().retrieveMessageById(event.getMessageIdLong()).onSuccess(message ->
							bridge.sendUserMessageD2M(authorName, message.getContentDisplay())
						).complete();
					}, CantileverConfig.HOLDER.get().discordChatProxy().messageDelay(), TimeUnit.MILLISECONDS);
					return;
				}

				bridge.sendUserMessageD2M(authorName, event.getMessage().getContentDisplay());
			}
		});
	}

	public static void execIfBridgePresent(Consumer<Bridge> consumer) {
		if (bridge() == null)
			return;
		consumer.accept(bridge());
	}
}
