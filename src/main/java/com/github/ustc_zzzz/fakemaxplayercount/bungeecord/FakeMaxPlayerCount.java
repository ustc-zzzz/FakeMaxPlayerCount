package com.github.ustc_zzzz.fakemaxplayercount.bungeecord;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.spongepowered.api.Sponge;

import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;

public class FakeMaxPlayerCount extends Plugin implements Listener
{
    private Logger logger;

    private HoconConfigurationLoader config;
    private CommentedConfigurationNode root;

    private boolean fixed = false;
    private int value = 1;

    private void checkConfig()
    {
        if (!this.getDataFolder().exists())
        {
            this.getDataFolder().mkdir();
        }
        File configFile = new File(this.getDataFolder(), this.getDescription().getName() + ".conf");
        this.config = HoconConfigurationLoader.builder().setFile(configFile).build();
    }

    private void loadConfig() throws IOException
    {
        this.root = this.config.load();

        // load configuration

        String type = this.root.getNode("fakemaxplayercount", "type").getString("floating");
        switch (type.toLowerCase())
        {
        case "floating":
            this.fixed = false;
            break;
        case "fixed":
            this.fixed = true;
            break;
        default:
            throw new IOException(String.format("Invalid type in fakemaxplayercount.conf: %s", type));
        }

        int i = this.root.getNode("fakemaxplayercount", "value")
                .getInt(this.fixed ? Sponge.getServer().getMaxPlayers() : 1);
        this.value = Math.max(i, 1);

        this.logger.info("Configuration loaded. ");
    }

    private void saveConfig() throws IOException
    {
        // save configuration

        this.root.getNode("fakemaxplayercount", "type").setValue(this.fixed ? "fixed" : "floating");

        this.root.getNode("fakemaxplayercount", "value").setValue(this.value);

        this.config.save(this.root);

        this.logger.info("Configuration saved. ");
    }

    @Override
    public void onEnable()
    {
        this.logger = this.getLogger();
        try
        {
            this.checkConfig();
            this.loadConfig();
            this.saveConfig();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        this.getProxy().getPluginManager().registerListener(this, this);
        this.logger.info("Plugin enabled. ");
    }

    @Override
    public void onDisable()
    {
        this.logger.info("Plugin disabled. ");
    }

    @EventHandler
    public void onProxyPing(ProxyPingEvent event)
    {
        ServerPing ping = event.getResponse();
        if (this.fixed)
        {
            ping.getPlayers().setOnline(Math.min(this.value - 1, ping.getPlayers().getOnline()));
            ping.getPlayers().setMax(this.value);
        }
        else
        {
            ping.getPlayers().setMax(ping.getPlayers().getOnline() + this.value);
        }
        event.setResponse(ping);
    }
}
