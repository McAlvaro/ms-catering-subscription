package com.mcalvaro.mscatering.application.dietplan.synchronization;

import an.awesome.pipelinr.Command;

public class SynchronizeDietPlanCommandHandler implements Command.Handler<SynchronizeDietPlanCommand, Void> {
    private final DietPlanSynchronizationStore store;

    public SynchronizeDietPlanCommandHandler(DietPlanSynchronizationStore store) {
        this.store = store;
    }

    @Override
    public Void handle(SynchronizeDietPlanCommand command) {
        store.synchronize(command);
        return null;
    }
}
