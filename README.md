# NeoForged Velocity Support

Adds support for [Velocity](https://papermc.io/software/velocity/)'s Modern Forwarding in NeoForge. Velocity's Modern
Forwarding protocol transfer user data directly from the proxy to the server, which acts as a second layer of
protection against bad actors on top of your firewall. For more detailed information, see the official documentation
[here](https://docs.papermc.io/velocity/).

This implementation strives for consistency with [Paper](https://papermc.io/software/paper/)'s implementation of modern
forwarding. This helps with compatibility and keeps the code simple.

For any questions, please ask in the `#velocity-support` channel on [my discord](https://discord.gg/vNaqDzSNaB).

## Getting Started

First, you will need a Velocity proxy set up. I assume if you are already looking at this mod, you already have this
set up. If not, for detailed steps on setting up a proxy, see the official documentation
[here](https://docs.papermc.io/velocity/getting-started/).

Once you are at the point where you want to connect your NeoForge server to the proxy, install the mod of the
appropriate version to your server. After that, simply copy your proxy's `forwarding.secret` file into your NeoForge
server's directory. Of course, make sure your server is in offline mode (`online-mode=false` in `server.properties`).
Launch your server and you are good to go!

## Troubleshooting

If you are having troubles connecting to your server and Velocity is giving a packet decoding error, add the following
argument to your proxy's startup script:

```
-Dvelocity.packet-decode-logging=true
```

This will make Velocity give more information regarding packet decoding errors.