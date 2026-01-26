package dev.spiritstudios.cantilever;

import dev.spiritstudios.cantilever.bridge.Bridge;
import lgbt.greenhouse.config.api.v3.GreenhouseConfigEventPhases;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ChatType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Cantilever implements ModInitializer {
	public static final String MODID = "cantilever";
	public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
	public static final ResourceKey<ChatType> D2M_MESSAGE_TYPE = ResourceKey.create(
		Registries.CHAT_TYPE,
		id("d2m")
	);
	private static Bridge bridge;

	@Override
	public void onInitialize() {
		CantileverConfig.init();

		ServerLifecycleEvents.SERVER_STARTING.register(
			id("before_bridge"),
			server -> {
				LOGGER.info("Initialising Cantilever...");
				bridge = new Bridge(server);
			}
		);

		ServerLifecycleEvents.SERVER_STARTING.addPhaseOrdering(
			GreenhouseConfigEventPhases.CONFIG_LOAD_PHASE,
			id("before_bridge")
		);
		ServerLifecycleEvents.SERVER_STARTING.addPhaseOrdering(
			id("before_bridge"),
			id("after_bridge")
		);

		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, manager, success) -> {
			CantileverConfig.HOLDER.reload(null);
			JDA api = bridge().api();
			if (api == null)
				return;
			api.getPresence().setActivity(CantileverConfig.HOLDER.get().botStatus().status().isEmpty() ?
				null :
				Activity.of(CantileverConfig.HOLDER.get().botStatus().type(), CantileverConfig.HOLDER.get().botStatus().status()));
		});
	}

	public static Bridge bridge() {
		return bridge;
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MODID, path);
	}
}
