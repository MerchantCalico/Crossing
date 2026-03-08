package dev.spiritstudios.cantilever;

import dev.spiritstudios.cantilever.bridge.Bridge;
import dev.spiritstudios.cantilever.bridge.BridgeEvents;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ChatType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
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
		bridge = new Bridge();
		BridgeEvents.initMinecraft();

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

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MODID, path);
	}
}
