package net.swedz.velocitysupport.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.custom.CustomQueryAnswerPayload;

public record QueryAnswerPayload(
		FriendlyByteBuf buffer
) implements CustomQueryAnswerPayload
{
	@Override
	public void write(FriendlyByteBuf buffer)
	{
		buffer.writeBytes(this.buffer.copy());
	}
}
