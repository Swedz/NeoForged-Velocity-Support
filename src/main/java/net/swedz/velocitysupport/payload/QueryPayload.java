package net.swedz.velocitysupport.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.custom.CustomQueryPayload;
import net.minecraft.resources.Identifier;

public record QueryPayload(
		Identifier id,
		FriendlyByteBuf buffer
) implements CustomQueryPayload
{
	@Override
	public void write(FriendlyByteBuf buffer)
	{
		buffer.writeBytes(this.buffer.copy());
	}
}
