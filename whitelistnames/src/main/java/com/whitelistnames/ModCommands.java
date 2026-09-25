package com.whitelistnames;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class ModCommands {
    private ModCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Brigadier merges this into vanilla's "/whitelist add <targets>", adding an extra <name> argument.
        // Plain "/whitelist add <player>" keeps working exactly like vanilla.
        dispatcher.register(Commands.literal("whitelist")
                .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                .then(Commands.literal("add")
                        .then(Commands.argument("targets", GameProfileArgument.gameProfile())
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(ModCommands::whitelistAddWithName)))));

        // /changename <username or current name> <new name>
        // (put a current name that has spaces in "quotes")
        dispatcher.register(Commands.literal("changename")
                .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                .then(Commands.argument("player", StringArgumentType.string())
                        .suggests(ModCommands::suggestKnown)
                        .then(Commands.argument("newname", StringArgumentType.greedyString())
                                .executes(ModCommands::changeName))));

        // /whitelistnames reload  -> re-reads config.json after you edit it
        dispatcher.register(Commands.literal("whitelistnames")
                .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                .then(Commands.literal("reload").executes(ctx -> {
                    NameStore.load();
                    MinecraftServer server = ctx.getSource().getServer();
                    for (ServerPlayer p : server.getPlayerList().getPlayers()) NameTagManager.update(p);
                    ctx.getSource().sendSuccess(() -> Text.of("&aWhitelist Names config reloaded."), false);
                    return 1;
                })));
    }

    private static int whitelistAddWithName(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String username = rawArgument(ctx, "targets");
        String name = StringArgumentType.getString(ctx, "name").trim();

        if (username == null || username.startsWith("@")) {
            source.sendFailure(Component.literal("Use a single player's username (no selectors) when giving a name."));
            return 0;
        }

        // Run the normal vanilla whitelist add silently, then show our own message.
        boolean alreadyWhitelisted = false;
        try {
            source.getServer().getCommands().getDispatcher()
                    .execute("whitelist add " + username, source.withSuppressedOutput());
        } catch (CommandSyntaxException e) {
            if (isTranslation(e, "commands.whitelist.add.failed")) {
                alreadyWhitelisted = true; // still let them set the name
            } else {
                source.sendFailure(ComponentUtils.fromMessage(e.getRawMessage()));
                return 0;
            }
        }

        NameStore.setName(username, name);
        refresh(source.getServer(), username);

        String template = alreadyWhitelisted
                ? NameStore.config.alreadyWhitelistedMessage
                : NameStore.config.whitelistAddMessage;
        Component msg = Text.format(template, "username", username, "name", name);
        source.sendSuccess(() -> msg, NameStore.config.broadcastToOps);
        return 1;
    }

    private static int changeName(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String query = StringArgumentType.getString(ctx, "player");
        String newName = StringArgumentType.getString(ctx, "newname").trim();

        NameStore.Entry entry = NameStore.find(query);
        if (entry == null) {
            source.sendFailure(Component.literal("No player with the username or name \"" + query
                    + "\". Add them with /whitelist add <username> <name> first."));
            return 0;
        }

        String oldName = entry.name;
        NameStore.setName(entry.username, newName);
        refresh(source.getServer(), entry.username);

        Component msg = Text.format(NameStore.config.nameChangedMessage,
                "username", entry.username, "oldname", oldName, "name", newName);
        source.sendSuccess(() -> msg, NameStore.config.broadcastToOps);
        return 1;
    }

    private static void refresh(MinecraftServer server, String username) {
        ServerPlayer player = server.getPlayerList().getPlayerByName(username);
        if (player != null) NameTagManager.update(player);
    }

    private static CompletableFuture<Suggestions> suggestKnown(CommandContext<CommandSourceStack> ctx,
                                                               SuggestionsBuilder builder) {
        List<String> options = new ArrayList<>();
        for (NameStore.Entry e : NameStore.entries()) {
            options.add(e.username);
            options.add(StringArgumentType.escapeIfRequired(Text.strip(e.name)));
        }
        return SharedSuggestionProvider.suggest(options, builder);
    }

    /** The exact text the user typed for an argument (avoids depending on GameProfile internals). */
    private static String rawArgument(CommandContext<CommandSourceStack> ctx, String argName) {
        for (ParsedCommandNode<CommandSourceStack> node : ctx.getNodes()) {
            if (node.getNode().getName().equals(argName)) {
                return node.getRange().get(ctx.getInput());
            }
        }
        return null;
    }

    private static boolean isTranslation(CommandSyntaxException e, String key) {
        return e.getRawMessage() instanceof Component c
                && c.getContents() instanceof TranslatableContents t
                && t.getKey().equals(key);
    }
}
