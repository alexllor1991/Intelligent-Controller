package com.rlresallo;

import ai.djl.ndarray.NDManager;
import com.rlresallo.ActionSpace;
import com.rlresallo.agents.RlAgent;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;

/**
 * Environment to use RL.
 */
public interface Environment extends AutoCloseable {
    
    /**
     * Resets the environment to it's default state.
     */
    void reset();

    /**
     * Returns the current state of the environment.
     * 
     * @return the current state of the environment
     */
    NDList getObservation();

    /**
     * Returns the current action that can be taken in the environment.
     * 
     * @return the current action that can be taken in the environment
     */
    ActionSpace getActionSpace();

    /**
     * Takes a step through an action in this environment.
     * 
     * @param action   the action to perform
     * @param training true if the step is during training
     */
    void step(NDList action, boolean training);

    /**
     * Runs the environment from reset until done.
     * 
     * @param agent    the agent to choose the actions
     * @param training true to run while training. In this case, the steps are stored in the replay buffer
     * @return the replayMemory
     */
    Step[] runEnvironment(RlAgent agent, boolean training);

    /**
     * Returns a batch of steps from the environment {@link ai.djl.modality.rl.ReplayBuffer}.
     * 
     * @return a batch of steps from the environment {@link ai.djl.modality.rl.ReplayBuffer}
     */
    Step[] getBatch();

    /**
     * {@inheritDoc}
     */
    @Override
    void close();

    /**
     * A record of taking a step in the environment.
     */
    interface Step extends AutoCloseable {

        /**
         * Returns the observation before the action that attach to manager (S).
         * 
         * @param manager the one attach to
         * @return the observation before the action that attach to manager
         */
        NDList getPreObservation(NDManager manager);

        /**
         * Returns the observation before the action (S).
         * 
         * @return the observation before the action
         */
        NDList getPreObservation();

        /**
         * Returns the action taken (A).
         * 
         * @return the action taken
         */
        NDList getAction();

        /**
         * Returns the observation after the action that attach to manager (S').
         * 
         * @param manager the one attach to
         * @return the observation after the action that attach to manager
         */
        NDList getPostObservation(NDManager manager);

        /**
         * Returns the observation after the action (S').
         * 
         * @return the observation after the action
         */
        NDList getPostObservation();

        /**
         * Returns the manager that manages this step.
         * 
         * @return the manager that manages this step
         */
        NDManager getManager();

        /**
         * Returns the reward given for the action (R).
         * 
         * @return the reward given for the action
         */
        NDArray getReward();

        /**
         * Returns whether the environment is finished or can accept further actions.
         * 
         * @return true if the environment is finished and can no longer accept actions
         */
        boolean isTerminal();

        /**
         * {@inheritDoc}
         */
        @Override
        void close();
    }
}
