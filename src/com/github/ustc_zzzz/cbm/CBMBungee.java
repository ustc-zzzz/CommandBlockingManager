package com.github.ustc_zzzz.cbm;

import com.github.ustc_zzzz.cbm.util.RuleConfigParser;
import com.github.ustc_zzzz.cbm.util.RulePermissionPair;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.TabCompleteEvent;
import net.md_5.bungee.api.event.TabCompleteResponseEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author ustc_zzzz
 */
public final class CBMBungee extends Plugin implements Listener
{
    private File config;

    private Cache<String, String> tabCompleteCacheMap = this.getTabCompleteCacheMap();

    private List<RulePermissionPair> rulePermissionPairs = ImmutableList.of();

    private List<RulePermissionPair> getRulePermissionPairs()
    {
        try
        {
            String def = "- \\?\n- help\n";
            return new RuleConfigParser().parse(this.config, def);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private Cache<String, String> getTabCompleteCacheMap()
    {
        return CacheBuilder.newBuilder().expireAfterWrite(20, TimeUnit.MINUTES).build();
    }

    @Override
    public void onLoad()
    {
        super.onLoad();
        this.config = new File(this.getDataFolder(), this.getDescription().getName().toLowerCase() + ".conf");
    }

    @Override
    public void onEnable()
    {
        super.onEnable();
        this.rulePermissionPairs = this.getRulePermissionPairs();
        this.getProxy().getPluginManager().registerListener(this, this);
        this.getLogger().info(this.rulePermissionPairs.size() + " rule(s) loaded in total.");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSendCommand(ChatEvent event)
    {
        if (!event.isCancelled() && event.isCommand())
        {
            Connection connection = event.getSender();
            String command = event.getMessage().substring(1);
            if (this.shouldCommandBeRejected(connection, command))
            {
                event.setCancelled(true);
                this.sendErrorMessage(connection);
                String logFormat = "%s has tried to execute an illegal server command: /%s, rejected.";
                this.getLogger().info(String.format(logFormat, this.getConnectionName(connection), command));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTabComplete(TabCompleteEvent event)
    {
        Connection connection = event.getSender();
        String commandToBeCompleted = event.getCursor();
        String connectionName = this.getConnectionName(connection);
        Iterator<String> iterator = event.getSuggestions().iterator();
        this.tabCompleteCacheMap.put(connectionName, commandToBeCompleted);
        while (iterator.hasNext())
        {
            String completion = iterator.next();
            String wholeCommand = commandToBeCompleted.replaceFirst("\\S*$", completion).substring(1);
            if (this.shouldCommandBeRejected(connection, wholeCommand))
            {
                String logFormat = "%s has tried to trigger an illegal tab completion: /%s, removed.";
                this.getLogger().info(String.format(logFormat, connectionName, wholeCommand));
                iterator.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTabCompleteResponse(TabCompleteResponseEvent event)
    {
        Connection connection = event.getReceiver();
        String connectionName = this.getConnectionName(connection);
        Iterator<String> iterator = event.getSuggestions().iterator();
        String commandToBeCompleted = this.tabCompleteCacheMap.getIfPresent(connectionName);
        while (iterator.hasNext())
        {
            String completion = iterator.next();
            if (Objects.isNull(commandToBeCompleted))
            {
                String logFormat = "%s has tried to trigger an outdated tab completion (%s), removed.";
                this.getLogger().info(String.format(logFormat, connectionName, completion));
                iterator.remove();
                continue;
            }
            String wholeCommand = commandToBeCompleted.replaceFirst("\\S*$", completion).substring(1);
            if (this.shouldCommandBeRejected(connection, wholeCommand))
            {
                String logFormat = "%s has tried to trigger an illegal tab completion: /%s, removed.";
                this.getLogger().info(String.format(logFormat, connectionName, wholeCommand));
                iterator.remove();
            }
        }
    }

    private String getConnectionName(Connection connection)
    {
        if (connection instanceof CommandSender)
        {
            return ((CommandSender) connection).getName();
        }
        else
        {
            return connection.getAddress().toString();
        }
    }

    private BaseComponent getErrorMessage()
    {
        TranslatableComponent message = new TranslatableComponent("commands.generic.notFound");
        message.setColor(ChatColor.RED);
        return message;
    }

    private void sendErrorMessage(Connection connection)
    {
        if (connection instanceof CommandSender)
        {
            ((CommandSender) connection).sendMessage(this.getErrorMessage());
        }
    }

    private boolean shouldCommandBeRejected(Connection connection, String command)
    {
        boolean rejected = false;
        if (connection instanceof CommandSender)
        {
            String[] parts = command.split("\\s+");
            CommandSender sender = (CommandSender) connection;
            for (RulePermissionPair pair : this.rulePermissionPairs)
            {
                String permission = pair.permission;
                if (("*".equals(permission) || sender.hasPermission(permission)) && pair.rule.matchRuleParts(parts))
                {
                    rejected = pair.rule.isDisallowed();
                }
            }
        }
        return rejected;
    }
}
