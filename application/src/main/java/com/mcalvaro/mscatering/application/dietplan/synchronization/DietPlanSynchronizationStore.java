package com.mcalvaro.mscatering.application.dietplan.synchronization;

public interface DietPlanSynchronizationStore {
    void synchronize(SynchronizeDietPlanCommand command);
}
