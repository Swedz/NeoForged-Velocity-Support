package net.swedz.velocitysupport;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.net.InetAddresses;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.ProfilePublicKey;

import javax.crypto.Mac;
import java.net.InetAddress;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * <p>Adapted from <a href="https://github.com/PaperMC/Paper">Paper</a>'s class of the same name.</p>
 *
 * @see <a href="https://github.com/PaperMC/Paper/blob/main/paper-server/src/main/java/com/destroystokyo/paper/proxy/VelocityProxy.java">VelocityProxy</a>
 */
public final class VelocityProxy
{
	private static final int        SUPPORTED_FORWARDING_VERSION     = 1;
	public static final  int        MODERN_FORWARDING_WITH_KEY       = 2;
	public static final  int        MODERN_FORWARDING_WITH_KEY_V2    = 3;
	public static final  int        MODERN_LAZY_SESSION              = 4;
	public static final  byte       MAX_SUPPORTED_FORWARDING_VERSION = MODERN_LAZY_SESSION;
	public static final  Identifier PLAYER_INFO_CHANNEL              = Identifier.fromNamespaceAndPath("velocity", "player_info");
	
	public static boolean checkIntegrity(FriendlyByteBuf buf)
	{
		byte[] signature = new byte[32];
		buf.readBytes(signature);
		
		byte[] data = new byte[buf.readableBytes()];
		buf.getBytes(buf.readerIndex(), data);
		
		try
		{
			var mac = Mac.getInstance("HmacSHA256");
			mac.init(VelocitySupport.secret().spec());
			byte[] mySignature = mac.doFinal(data);
			if(!MessageDigest.isEqual(signature, mySignature))
			{
				return false;
			}
		}
		catch (InvalidKeyException | NoSuchAlgorithmException e)
		{
			throw new AssertionError(e);
		}
		
		return true;
	}
	
	public static InetAddress readAddress(FriendlyByteBuf buf)
	{
		return InetAddresses.forString(buf.readUtf(Short.MAX_VALUE));
	}
	
	public static GameProfile createProfile(FriendlyByteBuf buf)
	{
		return new GameProfile(buf.readUUID(), buf.readUtf(16), readProperties(buf));
	}
	
	private static PropertyMap readProperties(FriendlyByteBuf buf)
	{
		ImmutableMultimap.Builder<String, Property> propertiesBuilder = ImmutableMultimap.builder();
		int properties = buf.readVarInt();
		for(int i1 = 0; i1 < properties; i1++)
		{
			String name = buf.readUtf(Short.MAX_VALUE);
			String value = buf.readUtf(Short.MAX_VALUE);
			String signature = buf.readBoolean() ? buf.readUtf(Short.MAX_VALUE) : null;
			propertiesBuilder.put(name, new Property(name, value, signature));
		}
		var propertiesMap = propertiesBuilder.build();
		return new PropertyMap(propertiesMap);
	}
	
	public static ProfilePublicKey.Data readForwardedKey(FriendlyByteBuf buf)
	{
		return new ProfilePublicKey.Data(buf);
	}
	
	public static UUID readSignerUuidOrElse(FriendlyByteBuf buf, UUID orElse)
	{
		return buf.readBoolean() ? buf.readUUID() : orElse;
	}
}
