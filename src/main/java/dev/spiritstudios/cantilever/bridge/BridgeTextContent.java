package dev.spiritstudios.cantilever.bridge;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.*;

import java.util.Optional;

public record BridgeTextContent(Component content) implements ComponentContents {
	public static MapCodec<BridgeTextContent> CODEC = MapCodec.assumeMapUnsafe(ComponentSerialization.CODEC
		.xmap(BridgeTextContent::new, content -> content.content));

	@Override
	public <T> Optional<T> visit(FormattedText.ContentConsumer<T> visitor) {
		return content.visit(visitor);
	}

	@Override
	public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> visitor, Style style) {
		return content.visit(visitor, style);
	}

	@Override
	public MapCodec<? extends ComponentContents> codec() {
		return CODEC;
	}
}
