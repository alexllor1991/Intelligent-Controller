package com.rlresallo;

import com.rlresallo.Environment;

/**
 * Saves {@link Environment.Step}s, i.e., collection of experience tuples (S, A, R, S'). 
 * By randomly subseting the experience replay buffer, the Q-network can be updated instead
 * of using the single most recent experience. 
 * Using this buffer ensures that several states are trained on for every training batch,
 * thus making the training more stable.
 */
public interface ReplayBuffer {

    /**
     * Returns a batch of steps from this buffer.
     * 
     * @return a batch of steps from this buffer 
     */
    Environment.Step[] getBatch();

    /**
     * Close the step not pointed to.
     */
    void closeStep();

    /**
     * Add a new step to the buffer.
     * 
     * @param step the step to add
     */
    void addStep(Environment.Step step);
}
