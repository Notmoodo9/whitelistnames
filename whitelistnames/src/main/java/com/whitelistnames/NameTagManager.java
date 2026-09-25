package com.whitelistnames;

import com.mojang.math.Transformation;
import com.whitelistnames.mixin.DisplayAccessor;
import com.whitelistnames.mixin.TextDisplayAccessor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import org.joml.Vector3f;

import java.util.*;

/**
 * Nametags: vanilla draws the real username above heads on the client, so a server-only mod
 * hides that with a scoreboard team and puts a text display entity riding the player instead.
 */
public final class NameTagManager {
    public static final String ENTITY_TAG = "whitelistnames_tag";
    private static final String TEAM_NAME = "wlnames_hide";
    private static final Map<UUID, Display.TextDisplay> TAGS = new HashMap<>();
    private static int ticks;

    private NameTagManager() {}

    /** The custom name for this player, or null if they don't have one. */
    public static Component nicknameFor(ServerPlayer player) {
        NameStore.Entry e = NameStore.get(player.getScoreboardName());
        return e == null ? null : Text.of(e.name);
    }

    /** Call after a join or a name change. */
    public static void update(ServerPlayer player) {
        MinecraftServer server = ((ServerLevel) player.level()).getServer();

        // Tab list
        server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(
                EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME), List.of(player)));

        // Nametag text
        Display.TextDisplay tag = TAGS.get(player.getUUID());
        Component nick = nicknameFor(player);
        if (tag != null && nick != null && isAttached(tag, player)) {
            ((TextDisplayAccessor) tag).whitelistnames$setText(nick);
        }
        sync(player);
    }

    public static void tick(MinecraftServer server) {
        if (++ticks % 10 != 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) sync(player);
        if (ticks % 200 == 0) cleanupOrphans(server);
    }

    /** Makes sure the tag exists, is riding the player, and vanilla's tag is hidden (or undoes it). */
    private static void sync(ServerPlayer player) {
        UUID id = player.getUUID();
        Component nick = nicknameFor(player);
        boolean wanted = nick != null && NameStore.config.customNameTags;
        boolean visible = wanted && player.isAlive() && !player.isSpectator() && !player.isInvisible();

        if (wanted) hideVanillaTag(player);
        else showVanillaTag(player);

        Display.TextDisplay tag = TAGS.get(id);
        if (!visible) {
            if (tag != null) {
                tag.discard();
                TAGS.remove(id);
            }
            return;
        }
        if (tag == null || !isAttached(tag, player)) {
            if (tag != null) tag.discard();
            Display.TextDisplay fresh = spawnTag(player, nick);
            if (fresh != null) TAGS.put(id, fresh);
            else TAGS.remove(id);
        }
    }

    private static Display.TextDisplay spawnTag(ServerPlayer player, Component text) {
        ServerLevel level = (ServerLevel) player.level();
        Display.TextDisplay tag = new Display.TextDisplay(EntityType.TEXT_DISPLAY, level);
        tag.setPos(player.getX(), player.getY() + player.getBbHeight(), player.getZ());
        ((TextDisplayAccessor) tag).whitelistnames$setText(text);
        ((DisplayAccessor) tag).whitelistnames$setBillboardConstraints(Display.BillboardConstraints.CENTER);
        ((DisplayAccessor) tag).whitelistnames$setTransformation(
                new Transformation(new Vector3f(0f, 0.25f, 0f), null, null, null));
        tag.addTag(ENTITY_TAG);

        level.addFreshEntity(tag);
        if (!tag.startRiding(player)) {
            tag.discard();
            return null;
        }
        return tag;
    }

    private static boolean isAttached(Display.TextDisplay tag, ServerPlayer player) {
        return !tag.isRemoved() && tag.getVehicle() == player;
    }

    private static void hideVanillaTag(ServerPlayer player) {
        Scoreboard scoreboard = ((ServerLevel) player.level()).getServer().getScoreboard();
        String entry = player.getScoreboardName();
        if (scoreboard.getPlayersTeam(entry) != null) return; // already on ours, or on someone else's team

        PlayerTeam team = scoreboard.getPlayerTeam(TEAM_NAME);
        if (team == null) team = scoreboard.addPlayerTeam(TEAM_NAME);
        if (team.getNameTagVisibility() != Team.Visibility.NEVER) {
            team.setNameTagVisibility(Team.Visibility.NEVER);
        }
        scoreboard.addPlayerToTeam(entry, team);
    }

    private static void showVanillaTag(ServerPlayer player) {
        Scoreboard scoreboard = ((ServerLevel) player.level()).getServer().getScoreboard();
        String entry = player.getScoreboardName();
        PlayerTeam current = scoreboard.getPlayersTeam(entry);
        if (current != null && current.getName().equals(TEAM_NAME)) {
            scoreboard.removePlayerFromTeam(entry, current);
        }
    }

    public static void remove(ServerPlayer player) {
        Display.TextDisplay tag = TAGS.remove(player.getUUID());
        if (tag != null) tag.discard();
        showVanillaTag(player);
    }

    public static void removeAll(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) remove(p);
        TAGS.values().forEach(Entity::discard);
        TAGS.clear();
    }

    /** Removes leftover tags (e.g. after a crash) so they don't float around the world. */
    private static void cleanupOrphans(MinecraftServer server) {
        Set<Entity> live = Collections.newSetFromMap(new IdentityHashMap<>());
        live.addAll(TAGS.values());
        List<Entity> dead = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity e : level.getAllEntities()) {
                if (e instanceof Display.TextDisplay && e.getTags().contains(ENTITY_TAG) && !live.contains(e)) {
                    dead.add(e);
                }
            }
        }
        dead.forEach(Entity::discard);
    }
}
