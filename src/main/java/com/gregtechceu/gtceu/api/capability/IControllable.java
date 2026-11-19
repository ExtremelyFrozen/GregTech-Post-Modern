package com.gregtechceu.gtceu.api.capability;

import com.gregtechceu.gtceu.api.sync_system.rpc.RPCMethod;

public interface IControllable {

    /**
     * @return true if the controllable is allowed to work
     */
    boolean isWorkingEnabled();

    /**
     * Set if the controllable can work or not
     *
     * @param isWorkingAllowed true if the workable can work, otherwise false
     */
    @RPCMethod
    void setWorkingEnabled(boolean isWorkingAllowed);

    default void setSuspendAfterFinish(boolean suspendAfterFinish) {}

    default boolean isSuspendAfterFinish() {
        return false;
    }
}
