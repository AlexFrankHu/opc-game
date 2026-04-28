package com.lastbastion.app.iogame.action;

import com.iohao.game.action.skeleton.annotation.ActionController;
import com.iohao.game.action.skeleton.annotation.ActionMethod;
import com.lastbastion.app.ActionRegistry;
import com.lastbastion.app.iogame.ServiceRegistry;
import com.lastbastion.app.iogame.msg.Messages.OnboardingCompleteReq;
import com.lastbastion.app.iogame.msg.Messages.OnboardingResp;
import com.lastbastion.app.iogame.msg.Messages.OnboardingSkipReq;
import com.lastbastion.game.onboarding.OnboardingStep;
import com.lastbastion.game.player.PlayerContext;

@ActionController(ActionRegistry.CMD_ONBOARDING)
public final class OnboardingCmdAction {

    @ActionMethod(1) // onboarding.completeStep
    public OnboardingResp completeStep(OnboardingCompleteReq req) {
        PlayerContext ctx = ServiceRegistry.sessions().loginOrCreate("ext-" + req.playerId);
        OnboardingStep step = OnboardingStep.valueOf(req.step);
        ServiceRegistry.services().onboarding.complete(ctx, step);
        ServiceRegistry.sessions().save(ctx.externalId(), ctx);
        return buildResp(ctx);
    }

    @ActionMethod(2) // onboarding.skip
    public OnboardingResp skip(OnboardingSkipReq req) {
        PlayerContext ctx = ServiceRegistry.sessions().loginOrCreate("ext-" + req.playerId);
        ServiceRegistry.services().onboarding.skip(ctx);
        ServiceRegistry.sessions().save(ctx.externalId(), ctx);
        return buildResp(ctx);
    }

    private static OnboardingResp buildResp(PlayerContext ctx) {
        OnboardingResp resp = new OnboardingResp();
        resp.current = ctx.onboardingState().current().name();
        resp.skipped = ctx.onboardingState().skipped();
        return resp;
    }
}
