package net.swedz.velocitysupport;

import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@Mod(
		value = VelocitySupport.ID,
		dist = Dist.DEDICATED_SERVER
)
public final class VelocitySupport
{
	public static final String ID   = "velocity_support";
	public static final String NAME = "Velocity Support";
	
	public static Identifier id(String path)
	{
		return Identifier.fromNamespaceAndPath(ID, path);
	}
	
	public static final Logger LOGGER = LoggerFactory.getLogger(NAME);
	
	public VelocitySupport(IEventBus bus, ModContainer container)
	{
		try
		{
			SECRET_KEY = getSecretKey();
			LOGGER.info("Successfully read secret key from forwarding.secret!");
		}
		catch (Exception ex)
		{
			LOGGER.warn("Failed to read secret key from file forwarding.secret. Without this, modern forwarding will not work.", ex);
		}
	}
	
	private static Secret SECRET_KEY;
	
	private static Secret getSecretKey() throws IOException, IllegalStateException
	{
		var path = FMLPaths.GAMEDIR.get()
				.resolve("forwarding.secret");
		if(Files.exists(path))
		{
			var content = String.join("", Files.readAllLines(path));
			if(!content.isEmpty())
			{
				return new Secret(content.getBytes(StandardCharsets.UTF_8));
			}
		}
		throw new IllegalStateException("Unable to find secret key file");
	}
	
	public static boolean hasSecret()
	{
		return SECRET_KEY != null;
	}
	
	public static Secret secret()
	{
		return SECRET_KEY;
	}
	
	public static final class Secret
	{
		private final byte[] data;
		
		public Secret(byte[] data)
		{
			this.data = data;
		}
		
		public SecretKeySpec spec()
		{
			return new SecretKeySpec(data, "HmacSHA256");
		}
	}
}
