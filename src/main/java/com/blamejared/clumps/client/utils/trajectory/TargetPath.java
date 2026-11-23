package com.blamejared.clumps.client.utils.trajectory;

import java.util.List;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public record TargetPath(Vec3d launchPosition, Vec3d inheritedVelocity, Box[] targetBoxes, List<Box> obstructionBoxes) {
    public TargetPath withTargetBoxes(Box[] boxes) {
        return new TargetPath(launchPosition, inheritedVelocity, boxes, obstructionBoxes);
    }

    public TargetPath withoutObstructions() {
        return new TargetPath(launchPosition, inheritedVelocity, targetBoxes, List.of());
    }
}
