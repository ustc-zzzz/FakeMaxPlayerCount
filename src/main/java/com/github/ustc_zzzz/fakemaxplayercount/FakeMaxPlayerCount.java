package com.github.ustc_zzzz.fakemaxplayercount;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.event.server.ClientPingServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import com.google.inject.Inject;

import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;

/**
 * @author ustc_zzzz
 */
@NonnullByDefault
@Plugin(id = FakeMaxPlayerCount.ID, name = "FakeMaxPlayerCount", version = "0.1.1", description = FakeMaxPlayerCount.DESC)
public class FakeMaxPlayerCount
{
    public static final String ID = "com.github.ustc_zzzz.fakemaxplayercount";
    public static final String DESC = "A sponge plugin to provide fake max player count. ";

    @Inject
    private Logger logger;

    @Inject
    @ConfigDir(sharedRoot = false)
    private Path configDir;

    @Inject
    @DefaultConfig(sharedRoot = false)
    private ConfigurationLoader<CommentedConfigurationNode> config;

    private boolean fixed = false;
    private int value = 1;

    @Listener
    public void onPreInitialization(GamePreInitializationEvent event)
    {
        try
        {
            CommentedConfigurationNode root = this.config.load();

            // load configuration

            String type = root.getNode("fakemaxplayercount", "type").getString("floating");
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

            int i = root.getNode("fakemaxplayercount", "value").getInt(this.fixed ? Sponge.getServer().getMaxPlayers() : 1);
            this.value = Math.max(i, 1);

            this.logger.info("Configuration loaded. ");

            // save configuration

            root.getNode("fakemaxplayercount", "type").setValue(this.fixed ? "fixed" : "floating");

            root.getNode("fakemaxplayercount", "value").setValue(this.value);

            this.config.save(root);

            this.logger.info("Configuration saved. ");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Listener
    public void onInitialization(GameInitializationEvent event)
    {
        Sponge.getEventManager().registerListener(this, ClientPingServerEvent.class, e ->
        {
            Optional<ClientPingServerEvent.Response.Players> optional = e.getResponse().getPlayers();
            if (optional.isPresent())
            {
                ClientPingServerEvent.Response.Players players = optional.get();
                if (this.fixed)
                {
                    players.setOnline(Math.min(this.value - 1, players.getOnline()));
                    players.setMax(this.value);
                }
                else
                {
                    players.setMax(players.getOnline() + this.value);
                }
            }
        });
    }

    @Listener
    public void onStartedServer(GameStartedServerEvent event)
    {
        this.logger.info("Plugin enabled. ");
    }

    @Listener
    public void onStoppingServer(GameStoppingServerEvent event)
    {
        this.logger.info("Plugin disabled. ");
    }
}
