package com.rlresallo.agents;

import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDManager;
import com.rlresallo.ActionSpace;
import com.rlresallo.Environment;
import com.rlresallo.Environment.Step;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.training.GradientCollector;
import ai.djl.training.Trainer;
import ai.djl.training.listener.TrainingListener.BatchData;
import ai.djl.translate.Batchifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link RlAgent} implementing Q or Deep-Q Learning.
 * 
 * <p>Deep-Q Learning estimates the total reward that will be given until the environment ends in a
 * particular state after taking an specifi action. It is trained by ensuring that the prediction before
 * taking the action match what would be predicted after taking the action.
 */
public class QAgent implements RlAgent {
    
    private final Trainer trainer;  //Trainer provides an easy, and manageable interface for training. The trainer for the model to learn. 
    private final float rewardDiscount; //Value to apply to rewards from future states. 

    /**
     * Constructs a {@link ai.djl.modality.rl.agent.QAgent} 
     */
    public QAgent(Trainer trainer, float rewardDiscount) {
        this.trainer = trainer;
        this.rewardDiscount = rewardDiscount;
    }

    private static final Logger logger = LoggerFactory.getLogger(QAgent.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public NDList chooseAction(Environment env, boolean training) {
        ActionSpace actionSpace = env.getActionSpace();
        NDArray actionReward = trainer.evaluate(env.getObservation()).singletonOrThrow().get(0); //Evaluates function of the model once on the given input and return the predict function.
        logger.info(Arrays.toString(actionReward.toFloatArray()));
        int bestAction = Math.toIntExact(actionReward.argMax().getLong());
        return actionSpace.get(bestAction);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void trainBatch(Step[] batchSteps) {
        BatchData batchData = new BatchData(null, new ConcurrentHashMap<>(), new ConcurrentHashMap<>()); //Create a TrainingListener.BatchData.

        NDManager temporaryManager = NDManager.newBaseManager(); //Creates a temporary manager for attaching NDArray to reduce the Device(CPU/GPU) memory usage.

        NDList preObservationBatch = new NDList();
        Arrays.stream(batchSteps).forEach(step -> preObservationBatch.addAll(step.getPreObservation(temporaryManager)));
        NDList preInput = new NDList(NDArrays.concat(preObservationBatch, 0));

        NDList postObservationBatch = new NDList();
        Arrays.stream(batchSteps).forEach(step -> postObservationBatch.addAll(step.getPostObservation(temporaryManager)));
        NDList postInput = new NDList(NDArrays.concat(postObservationBatch, 0));

        NDList actionBatch = new NDList();
        Arrays.stream(batchSteps).forEach(step -> actionBatch.addAll(step.getAction()));
        NDList actionInput = new NDList(NDArrays.stack(actionBatch, 0));

        NDList rewardBatch = new NDList();
        Arrays.stream(batchSteps).forEach(step -> rewardBatch.addAll(new NDList(step.getReward())));
        NDList rewardInput = new NDList(NDArrays.stack(rewardBatch, 0));

        //GradientCollector provides a mechanism to collect gradients during training.
        try (GradientCollector collector = trainer.newGradientCollector()) { 
            NDList QReward = trainer.forward(preInput);
            NDList targetQReward = trainer.forward(postInput);

            NDList Q = new NDList(QReward.singletonOrThrow().mul(actionInput.singletonOrThrow()).sum(new int[]{1}));

            NDArray[] targetQValue = new NDArray[batchSteps.length];
            for (int i = 0; i < batchSteps.length; i++) {
                if (batchSteps[i].isTerminal()) {
                    targetQValue[i] = batchSteps[i].getReward();
                } else {
                    targetQValue[i] = targetQReward.singletonOrThrow().get(i).max().mul(rewardDiscount).add(rewardInput.singletonOrThrow().get(i));
                }
            }
            NDList targetQBatch = new NDList();
            Arrays.stream(targetQValue).forEach(value -> targetQBatch.addAll(new NDList(value)));
            NDList targetQ = new NDList(NDArrays.stack(targetQBatch, 0));

            NDArray lossValue = trainer.getLoss().evaluate(targetQ, Q); //Gets the training loss function of the trainer
            collector.backward(lossValue); // Calculate the gradient
            batchData.getLabels().put(targetQ.singletonOrThrow().getDevice(), targetQ);
            batchData.getPredictions().put(Q.singletonOrThrow().getDevice(), Q);
            this.trainer.step(); //Updates the model parameters
        }
        for (Step step : batchSteps) {
            step.getPreObservation().attach(step.getManager());
            step.getPostObservation().attach(step.getManager());
        }
        temporaryManager.close();  //close the temporary manager
    }
}
