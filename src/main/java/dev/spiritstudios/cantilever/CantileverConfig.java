package dev.spiritstudios.cantilever;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import lgbt.greenhouse.config.api.v3.GreenhouseConfigHolder;
import lgbt.greenhouse.config.api.v3.GreenhouseConfigSide;
import lgbt.greenhouse.config.api.v3.builder.DefaultValueCommentSettings;
import lgbt.greenhouse.config.api.v3.lang.GreenhouseConfigJsonCLang;
import net.dv8tion.jda.api.entities.Activity;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

// TODO: Add data-fixer for old configs.
public record CantileverConfig(String botToken,
							   long channelId,
							   BotStatus botStatus,
							   DiscordToMinecraftFormatting discordToMinecraftFormatting,
							   MinecraftToDiscordFormatting minecraftToDiscordFormatting,
							   DiscordChatProxy discordChatProxy) {
	private static final Codec<Long> NON_NEGATIVE_LONG_CODEC = Codec.LONG.validate(l -> {
		if (l > -1) {
			return DataResult.success(l);
		}
		return DataResult.error(() -> "Value must not be negative.");
	});
	private static final Codec<Activity.ActivityType> ACTIVITY_TYPE_CODEC = Codec.STRING.comapFlatMap(s -> {
		for (Activity.ActivityType activityType : Activity.ActivityType.values()) {
			if (activityType.name().toLowerCase(Locale.ROOT).equals(s)) {
				return DataResult.success(activityType);
			}
		}
		return DataResult.error(() -> s + " is not a valid activity type. Accepts [" + Arrays.stream(Activity.ActivityType.values())
			.map(activityType1 -> "\"" + activityType1.name().toLowerCase(Locale.ROOT) + "\"")
			.collect(Collectors.joining()) + "]");
	}, activityType -> activityType.name().toLowerCase(Locale.ROOT));

	public static final GreenhouseConfigHolder<CantileverConfig> HOLDER = GreenhouseConfigHolder.register(
		CantileverConfig.class, "cantilever", 1, GreenhouseConfigJsonCLang.INSTANCE, GreenhouseConfigSide.DEDICATED,
		configBuilder -> configBuilder
			.withValue(
				"bot_token",
				"""
					Your Discord Bot's token

					For a guide on how to set up your bot, please refer to the below...
					https://docs.spiritstudios.dev/use/cantilever""",
				Codec.STRING,
				"<YOUR_BOT_TOKEN>",
				CantileverConfig::botToken,
				DefaultValueCommentSettings.DISABLE
			).withValue(
				"channel_id",
				"""
				The ID of the Discord Channel to send messages to and from Minecraft in

				You can get this value by enabling developer mode in Discord and right-clicking the channel you wish to use as your bridge""",
				Codec.LONG,
				123456789L,
				CantileverConfig::channelId,
				DefaultValueCommentSettings.DISABLE
			).withMapValue(
				BotStatus.class,
				"bot_status",
				"Settings relating to the bot's current botStatus",
				CantileverConfig::botStatus,
				mapBuilder -> mapBuilder
					.withValue(
						"type",
						"""
							The type of botStatus the bot is performing.

							Accepts one of: "playing", "streaming", "listening", "watching", "competing\"""",
						ACTIVITY_TYPE_CODEC,
						Activity.ActivityType.PLAYING,
						BotStatus::type
					).withValue(
						"status",
						"""
							An botStatus status for the Discord Bot.
							Functionally similar to a user status.""",
						Codec.STRING,
						"",
						BotStatus::status,
						DefaultValueCommentSettings.DISABLE
					)
			).withMapValue(
				DiscordToMinecraftFormatting.class,
				"discord_to_minecraft_formatting",
				"Settings relating to formatting Discord messages to Minecraft text components",
				CantileverConfig::discordToMinecraftFormatting,
				mapBuilder -> mapBuilder
					.withValue(
						"discord_message_format",
						"""
							The format used when sending messages from the defined Discord channel to Minecraft chat

							Use a first %s in your value to slot in the Discord User's name, and a second %s to slot in the chat status contents""",
						Codec.STRING,
						"<@%s> %s",
						DiscordToMinecraftFormatting::discordMessageFormat
					).withValue(
						"replacements",
						"""
							A map of text to text replacements from Discord channel messages to Minecraft chat; useful for Styled Chat Emoji

							Examples:
							{
								"obabo": "[REDACTED]"
							}""",
						Codec.unboundedMap(Codec.STRING, Codec.STRING),
						Collections.emptyMap(),
						DiscordToMinecraftFormatting::replacements,
						DefaultValueCommentSettings.DISABLE
					)
			).withMapValue(
				MinecraftToDiscordFormatting.class,
				"minecraft_to_discord_formatting",
				"Settings relating to formatting Minecraft chat to Discord channel messages",
				CantileverConfig::minecraftToDiscordFormatting,
				mapBuilder -> mapBuilder
					.withValue(
						"system_message_format",
						"""
							The format used when sending system/server messages from Minecraft to Discord

							Use %s in your value to slot in server status text contents""",
						Codec.STRING,
						"**%s**",
						MinecraftToDiscordFormatting::systemMessageFormat
					).withValue(
						"webhook_avatar_api",
						"""
							A URL used to determine the avatar of the Discord Webhook when a player sends a status in game chat
							This should typically be an API for obtaining the Minecraft player's skin in some way

							Use %s within the URL to slot in the player's UUID""",
						Codec.STRING,
						"https://vzge.me/face/256/%s.png",
						MinecraftToDiscordFormatting::webhookAvatarApi
					).withValue(
						"replacements",
						"""
							A map of text to text replacements from Minecraft chat messages to Discord channel messages; useful for Styled Chat Emoji

							Example: {
								":tiny_pineapple:": "<:tiny_pineapple:1383623031791816794>"
							}""",
						Codec.unboundedMap(Codec.STRING, Codec.STRING),
						Collections.emptyMap(),
						MinecraftToDiscordFormatting::replacements
					)
			).withMapValue(
				DiscordChatProxy.class,
				"discord_chat_proxy",
				"Settings relating to Discord chat proxies such as PluralKit, Tupperbox, etc",
				CantileverConfig::discordChatProxy,
				builder -> builder
					.withValue(
						"message_delay",
						"How long Cantilever should wait before sending a message if the message has not been deleted",
						NON_NEGATIVE_LONG_CODEC,
						0L,
						DiscordChatProxy::messageDelay
					)
			)
	);

	public static void init() {}

	public record BotStatus(Activity.ActivityType type,
							String status) {}

	// TODO: Support regex within chat replacements.
	public record DiscordToMinecraftFormatting(String discordMessageFormat,
												Map<String, String> replacements) {}

	public record MinecraftToDiscordFormatting(String systemMessageFormat,
											   String webhookAvatarApi,
											   Map<String, String> replacements) {}

	public record DiscordChatProxy(long messageDelay) {}
}
