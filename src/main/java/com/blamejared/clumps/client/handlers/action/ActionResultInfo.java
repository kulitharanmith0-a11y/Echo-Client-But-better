package com.blamejared.clumps.client.handlers.action;

import net.minecraft.util.ActionResult;

public record ActionResultInfo(
        ActionType type,
        ActionStatus status,
        ActionResult minecraftResult,
        int code
) {
    public boolean succeeded() {
        return status == ActionStatus.SUCCEEDED;
    }

    public boolean skipped() {
        return status == ActionStatus.SKIPPED;
    }

    public boolean rejected() {
        return status == ActionStatus.REJECTED;
    }

    public boolean executed() {
        return status != ActionStatus.SKIPPED;
    }

    public static ActionResultInfo succeeded(ActionType type, ActionResult minecraftResult) {
        return new ActionResultInfo(type, ActionStatus.SUCCEEDED, minecraftResult == null ? ActionResult.SUCCESS : minecraftResult, 0);
    }

    public static ActionResultInfo skipped(ActionType type, int code) {
        return new ActionResultInfo(type, ActionStatus.SKIPPED, ActionResult.PASS, code);
    }

    public static ActionResultInfo rejected(ActionType type, ActionResult minecraftResult, int code) {
        return new ActionResultInfo(type, ActionStatus.REJECTED, minecraftResult == null ? ActionResult.PASS : minecraftResult, code);
    }
}
