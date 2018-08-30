package com.github.ustc_zzzz.cbm;

import com.github.ustc_zzzz.cbm.util.RuleConfigParser;
import com.github.ustc_zzzz.cbm.util.RulePermissionPair;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.spongepowered.api.Platform;
import org.spongepowered.api.command.CommandMessageFormatting;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.command.TabCompleteEvent;
import org.spongepowered.api.event.filter.IsCancelled;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.SpongeApiTranslationHelper;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Locatable;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * @author ustc_zzzz
 */
@Plugin(name = "CommandBlockingManager", id = "commandblockingmanager", description = "@introduction@",
        authors = "ustc_zzzz", dependencies = @Dependency(id = Platform.API_ID), version = "@version@")
public final class CBMSponge
{
    @Inject
    private Logger logger;

    @Inject
    @DefaultConfig(sharedRoot = true)
    private File config;

    private List<RulePermissionPair> rulePermissionPairs = ImmutableList.of();

    private List<RulePermissionPair> getRulePermissionPairs()
    {
        try
        {
            String def = "- \\?\n- help\n- sp\n- sponge\n- sponge:\\?\n- sponge:help\n- sponge:sp\n- sponge:sponge\n";
            return new RuleConfigParser().parse(this.config, def);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Listener
    public void onStartingServer(GameStartingServerEvent event)
    {
        this.rulePermissionPairs = this.getRulePermissionPairs();
        this.logger.info("{} rule(s) loaded in total.", this.rulePermissionPairs.size());
    }

    @Listener
    public void onReload(GameReloadEvent event)
    {
        this.rulePermissionPairs = this.getRulePermissionPairs();
        this.logger.info("{} rule(s) loaded in total.", this.rulePermissionPairs.size());
    }

    @Listener(order = Order.EARLY)
    public void onSendCommand(SendCommandEvent event, @Root CommandSource source)
    {
        String command = event.getCommand() + ' ' + event.getArguments();
        if (this.shouldCommandBeRejected(source, command))
        {
            String logFormat = "{} has tried to execute an illegal server command: /{}, blocked.";
            Text errorMessage = SpongeApiTranslationHelper.t("commands.generic.notFound");
            source.sendMessage(CommandMessageFormatting.error(errorMessage));
            this.logger.info(logFormat, source.getName(), command);
            event.setResult(CommandResult.empty());
            event.setCancelled(true);
        }
    }

    @Listener(order = Order.EARLY)
    @IsCancelled(Tristate.UNDEFINED)
    public void onTabComplete(TabCompleteEvent.Command event, @Root CommandSource source)
    {
        String commandToBeCompleted = event.getRawMessage();
        Iterator<String> iterator = event.getTabCompletions().iterator();
        while (iterator.hasNext())
        {
            String completion = iterator.next();
            String wholeCommand = commandToBeCompleted.replaceFirst("\\S*$", completion);
            if (this.shouldCommandBeRejected(source, wholeCommand))
            {
                String logFormat = "{} has tried to trigger an illegal tab completion: /{}, removed.";
                this.logger.info(logFormat, source.getName(), wholeCommand);
                iterator.remove();
            }
        }
    }

    private boolean shouldCommandBeRejected(CommandSource source, String command)
    {
        boolean rejected = false;
        if (source instanceof Locatable)
        {
            String[] parts = command.split("\\s+");
            for (RulePermissionPair pair : this.rulePermissionPairs)
            {
                String permission = pair.permission;
                if (("*".equals(permission) || source.hasPermission(permission)) && pair.rule.matchRuleParts(parts))
                {
                    rejected = pair.rule.isDisallowed();
                }
            }
        }
        return rejected;
    }
}
