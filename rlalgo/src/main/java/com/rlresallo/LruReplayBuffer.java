package com.rlresallo;

import ai.djl.util.RandomUtils;
import com.rlresallo.Environment;
import java.util.ArrayList;

/**
 * {@link ReplayBuffer} that randomly selects from the whole buffer and removes 
 * the oldest items in the buffer when it is full.
 */
public class LruReplayBuffer implements ReplayBuffer {
    
    private final int batchSize;
    private final Environment.Step[] steps;
    private final ArrayList<Environment.Step> stepToClose;
    private int firstStepIndex;
    private int stepsActualSize;

    /**
     * Constructs a {@link ai.djl.modality.rl.LruReplayBuffer}.
     * 
     * @param batchSize  the number of steps to train on per batch
     * @param bufferSize the number of steps to hold in the buffer
     */
    public LruReplayBuffer(int batchSize, int bufferSize) {
        this.batchSize = batchSize;
        steps = new Environment.Step[bufferSize];
        stepToClose = new ArrayList<>(bufferSize);
        firstStepIndex = 0;
        stepsActualSize = 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("PMD.AvoidArrayLoops")
    public Environment.Step[] getBatch() {
        Environment.Step[] batch = new Environment.Step[batchSize];
        for (int i = 0; i < batchSize; i++) {
            int baseIndex = RandomUtils.nextInt(stepsActualSize);
            int index = Math.floorMod(firstStepIndex + baseIndex, steps.length);
            batch[i] = steps[index];
        }
        return batch;
    }

    /**
     * {@inheritDoc}
     */
    public void closeStep() {
        for (Environment.Step step : stepToClose) {
            step.close();
        }
        stepToClose.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addStep(Environment.Step step) {
        if (stepsActualSize == steps.length) {
            int stepToReplace = Math.floorMod(firstStepIndex - 1, steps.length);
            stepToClose.add(steps[stepToReplace]);
            steps[stepToReplace] = step;
            firstStepIndex = Math.floorMod(firstStepIndex + 1, steps.length);
        } else {
            steps[stepsActualSize] = step;
            stepsActualSize++;
        }
    }
}
