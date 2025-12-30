package net.swedz.velocitysupport.mixin;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.swedz.velocitysupport.VelocityProxy;
import net.swedz.velocitysupport.VelocitySupport;
import net.swedz.velocitysupport.payload.QueryAnswerPayload;
import net.swedz.velocitysupport.payload.QueryPayload;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetSocketAddress;
import java.util.concurrent.ThreadLocalRandom;

@Mixin(ServerLoginPacketListenerImpl.class)
public class HandleVelocityModernForwardingMixin
{
	@Shadow
	@Final
	static Logger LOGGER;
	
	@Shadow
	@Final
	Connection connection;
	
	@Shadow
	private GameProfile authenticatedProfile;
	
	@Unique
	private int velocityLoginMessageId = -1;
	
	@Shadow
	public void disconnect(Component reason)
	{
		throw new UnsupportedOperationException();
	}
	
	@Shadow
	void startClientVerification(GameProfile profile)
	{
		throw new UnsupportedOperationException();
	}
	
	@Shadow
	@Final
	MinecraftServer server;
	
	@Inject(
			method = "handleHello",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/server/network/ServerLoginPacketListenerImpl;startClientVerification(Lcom/mojang/authlib/GameProfile;)V"
			),
			cancellable = true
	)
	private void handleHello(ServerboundHelloPacket packet,
							 CallbackInfo callback)
	{
		if(VelocitySupport.hasSecret())
		{
			velocityLoginMessageId = ThreadLocalRandom.current().nextInt();
			
			var buf = new FriendlyByteBuf(Unpooled.buffer());
			buf.writeByte(VelocityProxy.MAX_SUPPORTED_FORWARDING_VERSION);
			
			var queryPacket = new ClientboundCustomQueryPacket(
					velocityLoginMessageId,
					new QueryPayload(VelocityProxy.PLAYER_INFO_CHANNEL, buf)
			);
			connection.send(queryPacket);
			
			callback.cancel();
		}
	}
	
	@Inject(
			method = "handleCustomQueryPacket",
			at = @At("HEAD"),
			cancellable = true
	)
	private void handleCustomQueryPacket(ServerboundCustomQueryAnswerPacket packet,
										 CallbackInfo callback)
	{
		if(VelocitySupport.hasSecret() &&
		   packet.transactionId() == velocityLoginMessageId)
		{
			callback.cancel();
			
			var payload = (QueryAnswerPayload) packet.payload();
			try
			{
				if(payload == null || velocityLoginMessageId == -1)
				{
					this.disconnect(Component.literal("You must connect through the server proxy."));
					return;
				}
				
				var buf = payload.buffer();
				if(!VelocityProxy.checkIntegrity(buf))
				{
					this.disconnect(Component.literal("Unable to verify player details."));
					return;
				}
				
				int version = buf.readVarInt();
				if(version > VelocityProxy.MAX_SUPPORTED_FORWARDING_VERSION)
				{
					throw new IllegalStateException("Unsupported forwarding version " + version + ", expected up to " + VelocityProxy.MAX_SUPPORTED_FORWARDING_VERSION);
				}
				
				var listening = connection.getRemoteAddress();
				int port = listening instanceof InetSocketAddress address ? address.getPort() : 0;
				connection.address = new InetSocketAddress(VelocityProxy.readAddress(buf), port);
				authenticatedProfile = VelocityProxy.createProfile(buf);
				
				LOGGER.info("UUID of player {} is {}", authenticatedProfile.getName(), authenticatedProfile.getId());
				this.startClientVerification(authenticatedProfile);
			}
			catch (Exception ex)
			{
				this.disconnect(Component.literal("Failed to verify."));
				VelocitySupport.LOGGER.warn("Failed to verify {}", authenticatedProfile.getName(), ex);
			}
			finally
			{
				if(payload != null)
				{
					payload.buffer().release();
				}
			}
		}
	}
}
