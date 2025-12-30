package net.swedz.velocitysupport.mixin;

import com.mojang.brigadier.arguments.ArgumentType;
import io.netty.buffer.Unpooled;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.swedz.velocitysupport.VelocitySupport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

/**
 * <p>Adapted from <a href="https://github.com/VelocityPowered/CrossStitch">CrossStitch</a>'s patch.</p>
 *
 * @see <a href="https://github.com/VelocityPowered/CrossStitch/blob/master/src/main/java/com/velocitypowered/crossstitch/mixin/command/CommandTreeSerializationMixin.java">CommandTreeSerializationMixin</a>
 */
@Mixin(targets = "net.minecraft.network.protocol.game.ClientboundCommandsPacket$ArgumentNodeStub")
public class WrapCommandSerializationMixin
{
	@Unique
	private static final Set<String> BUILT_IN_REGISTRY_KEYS = Set.of("minecraft", "brigadier");
	
	@Unique
	private static final int MOD_ARGUMENT_INDICATOR = -256;
	
	@Inject(
			method = "serializeCap(Lnet/minecraft/network/FriendlyByteBuf;Lnet/minecraft/commands/synchronization/ArgumentTypeInfo;Lnet/minecraft/commands/synchronization/ArgumentTypeInfo$Template;)V",
			at = @At("HEAD"),
			cancellable = true
	)
	private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void serializeCap(
			FriendlyByteBuf buffer,
			ArgumentTypeInfo<A, T> serializer,
			ArgumentTypeInfo.Template<A> properties,
			CallbackInfo callback
	)
	{
		if(VelocitySupport.hasSecret())
		{
			var key = BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getKey(serializer);
			if(key == null || !BUILT_IN_REGISTRY_KEYS.contains(key.getNamespace()))
			{
				callback.cancel();
				
				buffer.writeVarInt(MOD_ARGUMENT_INDICATOR);
				buffer.writeVarInt(BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getId(serializer));
				
				var extraData = new FriendlyByteBuf(Unpooled.buffer());
				serializer.serializeToNetwork((T) properties, extraData);
				
				buffer.writeVarInt(extraData.readableBytes());
				buffer.writeBytes(extraData);
			}
		}
	}
}
