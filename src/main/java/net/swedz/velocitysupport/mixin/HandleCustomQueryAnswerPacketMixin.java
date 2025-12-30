package net.swedz.velocitysupport.mixin;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.custom.CustomQueryAnswerPayload;
import net.swedz.velocitysupport.VelocitySupport;
import net.swedz.velocitysupport.payload.QueryAnswerPayload;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerboundCustomQueryAnswerPacket.class)
public class HandleCustomQueryAnswerPacketMixin
{
	@Shadow
	@Final
	private static int MAX_PAYLOAD_SIZE;
	
	@Inject(
			method = "readPayload",
			at = @At("HEAD"),
			cancellable = true
	)
	private static void readUnknownPayload(int transactionId, FriendlyByteBuf buffer,
										   CallbackInfoReturnable<CustomQueryAnswerPayload> callback)
	{
		if(VelocitySupport.hasSecret())
		{
			var buf = buffer.readNullable((reader) ->
			{
				int readableBytes = reader.readableBytes();
				if(readableBytes >= 0 && readableBytes <= MAX_PAYLOAD_SIZE)
				{
					return new FriendlyByteBuf(reader.readBytes(readableBytes));
				}
				else
				{
					throw new IllegalArgumentException("Payload may not be larger than " + MAX_PAYLOAD_SIZE + " bytes");
				}
			});
			callback.setReturnValue(buf == null ? null : new QueryAnswerPayload(buf));
		}
	}
}
