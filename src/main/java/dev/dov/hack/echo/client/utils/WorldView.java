package dev.dov.hack.echo.client.utils;

import dev.dov.hack.echo.client.runtime.api.BlockStateView;
import dev.dov.hack.echo.client.runtime.api.EntityView;
import dev.dov.hack.echo.client.runtime.api.PlayerView;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

public class WorldView {
    private final ClientWorld world;

    
    public WorldView(ClientWorld world) {
        this.world = world;
    }

    
    public EntityView[] getPlayers() {
        List<? extends PlayerEntity> players = world.getPlayers();
        EntityView[] result = new EntityView[players.size()];
        for (int index = 0; index < players.size(); index++) {
            result[index] = new PlayerView(players.get(index));
        }
        return result;
    }

    
    public EntityView[] getEntities() {
        ArrayList<EntityView> result = new ArrayList<>();
        for (Entity entity : world.getEntities()) {
            result.add(new EntityView(entity));
        }
        return result.toArray(new EntityView[0]);
    }

    
    public BlockStateView getBlock(int x, int y, int z) {
        return new BlockStateView(world.getBlockState(new BlockPos(x, y, z)));
    }
}
