package com.rlresallo.agents;

import com.rlresallo.Environment;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDArray;

/**
 * An {@link ai.djl.modality.rl.agent.RlAgent} is the model to decide the actions to take in an {@link Environment}.
 */
public interface RlAgent {
    
    /**
     * Chooses the next action to take in the {@link Environment}.
     * 
     * @param env      the current environment
     * @param training true if the agent is currently training
     * @return the action to take
     */
    NDList chooseAction(Environment env, boolean training, int nodes, int clusters, NDArray mask);

    /**
     * Train this {@link ai.djl.modality.rl.agent.RlAgent} on a batch of of {@link Environment.Step}s.
     * 
     * @param batchSteps the steps to train on
     */
    void trainBatch(Environment.Step[] batchSteps);
}
