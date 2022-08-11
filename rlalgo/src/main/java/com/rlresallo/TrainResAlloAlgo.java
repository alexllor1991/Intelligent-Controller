package com.rlresallo;

import ai.djl.engine.*;
import ai.djl.Device;
import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDArray;
import ai.djl.nn.Activation;
import ai.djl.nn.Blocks;
import ai.djl.nn.SequentialBlock;
//import ai.djl.nn.convolutional.Conv2d;
import ai.djl.nn.core.Linear;
import ai.djl.metric.Metrics;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.Trainer;
import ai.djl.training.evaluator.Accuracy;
import ai.djl.training.initializer.NormalInitializer;
import ai.djl.training.listener.TrainingListener;
import ai.djl.training.TrainingResult;
import ai.djl.training.loss.Loss;
import ai.djl.training.optimizer.Adam;
import ai.djl.training.tracker.LinearTracker;
import ai.djl.training.tracker.LinearTracker.Builder;
import ai.djl.training.tracker.Tracker;
import ai.djl.nn.Parameter;
import ai.djl.nn.ParameterList;
import com.rlresallo.agents.EpsilonGreedy;
import com.rlresallo.agents.QAgent;
import com.rlresallo.agents.RlAgent;
import com.rlresallo.Environment;
//import com.rlresallo.Arguments;
import com.rlresallo.ResAlloAlgo;
//import org.apache.commons.cli.ParseException;
//import org.sdnhub.odl.tutorial.learningswitch.impl.TutorialL2Forwarding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
//import java.io.LineNumberReader;
//import java.nio.charset.MalformedInputException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.Iterator;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.time.LocalDateTime;

public final class TrainResAlloAlgo {
    private static final Logger logger = LoggerFactory.getLogger(TrainResAlloAlgo.class);

    public static final int OBSERVE = 50; // steps to observe before training
    public static final int EXPLORE = 30000; // frames over which to anneal epsilon
    public static final int SAVE_EVERY_STEPS = 100; // same model every x steps
    public static final int REPLAY_BUFFER_SIZE = 50000; // number of previous transitions to remember
    public static final float REWARD_DISCOUNT = 0.9f; //decay rate of past observations
    public static final float INITIAL_EPSILON = 0.01f;
    public static final float FINAL_EPSILON = 0.0001f;
    public static final String PARAMS_PREFIX = "dqn-trained";
    public static final String MODEL_PATH = "src/main/resources/model";
    public static int NUMBER_INPUTS; // number of inputs in the neural network
    public static int NUMBER_OUTPUTS; // number of outputs in the neural network
    public static int HIDDEN_NEURONS; // number of neurons in the hidden layer

    private static final String CSV_FILE_TRAINING_RESULTS = "src/main/resources/model/Training_results.csv";
    private static File file_training_results = null;
    private static FileWriter outputTraining_results = null;
    private static CSVWriter writerTraining_results = null;

    static Environment.Step[] batchSteps;

    public TrainResAlloAlgo(int noClusters, int noNodes, int batchSize, boolean preTrained, boolean testing) {
        NUMBER_INPUTS = 2 * noClusters * noNodes + 4; //not considered bandwidht resource, in that case is 5
        NUMBER_OUTPUTS =  noClusters * noNodes + 1;
        HIDDEN_NEURONS = (NUMBER_INPUTS + NUMBER_OUTPUTS) / 2;

        file_training_results = new File(CSV_FILE_TRAINING_RESULTS);

        String[] headerTraining_results = {"Timestamp",
                                           "Epoch",
                                           "L2Loss",
                                           "Accuracy"};

        try {
            outputTraining_results = new FileWriter(file_training_results);
            writerTraining_results = new CSVWriter(outputTraining_results);
            writerTraining_results.writeNext(headerTraining_results);
            writerTraining_results.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Model createOrLoadModel(boolean preTrained) throws IOException, MalformedModelException {
        Model model = Model.newInstance("QNetwork"); // Creates an empty model instance.
        model.setBlock(getBlock()); // Sets the block (It is a composable function that forms a NN) for the Model for training and inference.
        if (preTrained) {
            model.load(Paths.get(MODEL_PATH), PARAMS_PREFIX); // Loads the model from the modelPath and the given name.
        }
        
        return model;
    }

    public void train(int miniBatch, boolean preTrained, boolean testing, Model model, int noClusters, int noNodes) {
        boolean training = !testing;
        int batchSize = miniBatch; // size of the mini batch

        ResAlloAlgo env = new ResAlloAlgo(NDManager.newBaseManager(), batchSize, REPLAY_BUFFER_SIZE, noClusters, noNodes);

        System.out.println("------------Action Space-----------");
        Iterator<NDList> it = env.getActionSpace().iterator();
        while (it.hasNext()) {
            System.out.println(Arrays.toString(it.next().toArray())); 
        }

        int numOfThreads = 1;
        List<Runnable> runnables = new ArrayList<>(numOfThreads);
        if(training) {
            runnables.add(new TrainerGeneratorRunnable(model, batchSize, env, training));
        }
        ExecutorService executorService = Executors.newFixedThreadPool(numOfThreads);
        for (Runnable runnable : runnables) {
            executorService.execute(runnable);
        }

        /** 
        DefaultTrainingConfig config = setupTrainingConfig();
        try (Trainer trainer = model.newTrainer(config)) {
            trainer.initialize(new Shape(batchSize, NUMBER_INPUTS)); // Initialize the model to be trained. 
            trainer.notifyListeners(listener -> listener.onTrainingBegin(trainer)); // Listens to the beginning of training.

            System.out.println("------------Initial model-----------");
            System.out.println(trainer.getModel());
            System.out.println(trainer.getModel().describeInput().toMap(false));
            System.out.println(trainer.getModel().getBlock());
            //System.out.println(trainer.getModel().getBlock().getParameters().toMap(false));

            ParameterList params = trainer.getModel().getBlock().getParameters(); 

            //NDArray Linear02_bias = params.get("02Linear_bias").getArray();
            NDArray Linear02_weight = params.get("02Linear_weight").getArray();

            //NDArray Linear04_bias = params.get("04Linear_bias").getArray();
            NDArray Linear04_weight = params.get("04Linear_weight").getArray();

            System.out.println("----02Linear_weight parameters-----");
            System.out.println(Linear02_weight);

            //System.out.println("----02Linear_bias parameters-----");
            //System.out.println(Linear02_bias);

            System.out.println("----04Linear_weight parameters-----");
            System.out.println(Linear04_weight);

            //System.out.println("----04Linear_bias parameters-----");
            //System.out.println(Linear04_bias);
            
            //System.out.println(trainer.getModel().describeOutput().toMap(false));

            Metrics metrics = new Metrics();
            trainer.setMetrics(metrics);

            //System.out.println("---Metrics---");
            //System.out.println(trainer.getMetrics().getMetricNames());

            System.out.println("---Evaluators---");
            System.out.println(trainer.getEvaluators());

            System.out.println("------------Initializing Q-agent-----------");
            RlAgent agent = new QAgent(trainer, REWARD_DISCOUNT);
            // Tracker represents a hyper-parameter that changes gradually through the training process.
            Tracker exploreRate = LinearTracker.builder()  // A tracker that is updated by a constant factor.
                                        .setBaseValue(INITIAL_EPSILON) // Sets the initial value after no steps.
                                        .optSlope(-(INITIAL_EPSILON - FINAL_EPSILON) / EXPLORE) // Sets the value of the linear slope.
                                        .optMinValue(FINAL_EPSILON) // Sets the minimum value for a negative slope.
                                        .build(); // Builds a LinearTracker block.
            agent = new EpsilonGreedy(agent, exploreRate);

            System.out.println("------------Waiting for input values-----------");
            //while(true) {
            //    if (ODLBrain.getVariableareValuesvnfRequestedtoDeploy()) {
            //        break;
            //    }
            //}
            //ODLBrain.areValuesvnfRequestedtoDeploy.set(false);
            try {
				Thread.sleep(10000);
			} catch (InterruptedException ix) {
				System.err.println("INTERRUPTED");
			}

            int numOfThreads = 2;
            //List<Callable<Object>> callables = new ArrayList<>(numOfThreads); 
            List<Runnable> runnables = new ArrayList<>(numOfThreads); 
            //callables.add(new GeneratorCallable(env, agent, training));
            runnables.add(new GeneratorRunnable(env, agent, training));
            if(training) {
                //callables.add(new TrainerCallable(model, agent));
                runnables.add(new TrainerRunnable(model, agent));
            }
            ExecutorService executorService = Executors.newFixedThreadPool(numOfThreads);
            // try {
            //     try {
            //         List<Future<Object>> futures = new ArrayList<>();
            //         for (Callable<Object> callable : callables) {
            //             futures.add(executorService.submit(callable));
            //         }
            //         for (Future<Object> future : futures) {
            //             future.get();
            //         }
            //     } catch (InterruptedException | ExecutionException e) {
            //         logger.error("", e);
            //     }
            // } finally {
            //     executorService.shutdown();
            // }
            //try {
            for (Runnable runnable : runnables) {
                executorService.execute(runnable);
            }
            //} catch (InterruptedException | ExecutionException e) {
            //    logger.error("", e);
            //}
            //} finally {
            //    executorService.shutdown();
            //}
            
        }
        */
    }

    public void test(Model model) {
        ResAlloAlgo env = new ResAlloAlgo(NDManager.newBaseManager(), 1, 1, 1, 1);
        DefaultTrainingConfig config = setupTrainingConfig();
        try (Trainer trainer = model.newTrainer(config)) {
            RlAgent agent = new QAgent(trainer, REWARD_DISCOUNT);
            while (true) {
                env.runEnvironment(agent, false);
            }
        }
    }

    public static class TrainerGeneratorRunnable implements Runnable {
        private Model model;
        private int batchsize;
        private ResAlloAlgo env;
        private RlAgent agent;
        private boolean training;

        public TrainerGeneratorRunnable(Model model, int batchsize, ResAlloAlgo env, boolean training) {
            this.model = model;
            this.batchsize = batchsize;
            this.env = env;
            this.training = training;
        }

        @Override
        public void run() {

            DefaultTrainingConfig config = setupTrainingConfig();
            try (Trainer trainer = model.newTrainer(config)) {
                trainer.initialize(new Shape(batchsize, NUMBER_INPUTS)); // Initialize the model to be trained. 
                trainer.notifyListeners(listener -> listener.onTrainingBegin(trainer)); // Listens to the beginning of training.

                System.out.println("------------Initial model-----------");
                System.out.println(trainer.getModel());
                System.out.println(trainer.getModel().describeInput().toMap(false));
                System.out.println(trainer.getModel().getBlock());

                ParameterList params = trainer.getModel().getBlock().getParameters(); 

                NDArray Linear02_weight = params.get("02Linear_weight").getArray();
                NDArray Linear04_weight = params.get("04Linear_weight").getArray();

                System.out.println("----02Linear_weight parameters-----");
                System.out.println(Linear02_weight);

                System.out.println("----04Linear_weight parameters-----");
                System.out.println(Linear04_weight);

                Metrics metrics = new Metrics();
                trainer.setMetrics(metrics);

                System.out.println("---Evaluators---");
                System.out.println(trainer.getEvaluators());

                System.out.println("------------Initializing Q-agent-----------");
                agent = new QAgent(trainer, REWARD_DISCOUNT);
                // Tracker represents a hyper-parameter that changes gradually through the training process.
                Tracker exploreRate = LinearTracker.builder()  // A tracker that is updated by a constant factor.
                                            .setBaseValue(INITIAL_EPSILON) // Sets the initial value after no steps.
                                            .optSlope(-(INITIAL_EPSILON - FINAL_EPSILON) / EXPLORE) // Sets the value of the linear slope.
                                            .optMinValue(FINAL_EPSILON) // Sets the minimum value for a negative slope.
                                            .build(); // Builds a LinearTracker block.
                agent = new EpsilonGreedy(agent, exploreRate);

                System.out.println("------------Waiting for input values-----------");
            
			    Thread.sleep(10000);

                while (ResAlloAlgo.trainStep < EXPLORE) {
                    Thread.sleep(0);
                    batchSteps = env.runEnvironment(agent, training);
                    //System.out.println(ResAlloAlgo.envStep);
                    if (ResAlloAlgo.envStep > OBSERVE) {
                        System.out.println("------------Enough experiences in replay buffer-----------");
                        agent.trainBatch(batchSteps);
                        trainer.step();
                        ResAlloAlgo.trainStep++;

                        trainer.notifyListeners(listener -> listener.onEpoch(trainer));

                        //ParameterList param = trainer.getModel().getBlock().getParameters(); 

                        //NDArray Linear02_bias = param.get("02Linear_bias").getArray();
                        //NDArray Linear02_weights = param.get("02Linear_weight").getArray();
                
                        //NDArray Linear04_bias = param.get("04Linear_bias").getArray();
                        //NDArray Linear04_weights = param.get("04Linear_weight").getArray();
                
                        //System.out.println("----02Linear_weight parameters-----");
                        //System.out.println(Linear02_weights);
                
                        //System.out.println("----02Linear_bias parameters-----");
                        //System.out.println(Linear02_bias);
                
                        //System.out.println("----04Linear_weight parameters-----");
                        //System.out.println(Linear04_weights);
                
                        //System.out.println("----04Linear_bias parameters-----");
                        //System.out.println(Linear04_bias);
                
                        Metrics metric = trainer.getMetrics();
                        double l2loss_epoch = metric.latestMetric("train_epoch_L2Loss").getValue().doubleValue();
                        double accuracy_epoch = metric.latestMetric("train_epoch_Accuracy").getValue().doubleValue();
                        //double l2loss_all = metrics.latestMetric("train_all_L2Loss").getValue().doubleValue();
                        //double accuracy_all = metrics.latestMetric("train_all_Accuracy").getValue().doubleValue();
                        //int epoch = metrics.latestMetric("epoch").getValue().intValue();
                        //System.out.println(metrics.latestMetric("train_epoch_L2Loss"));
                        //System.out.println(metrics.latestMetric("train_epoch_Accuracy"));
                        System.out.println("---Training Results---");
                        System.out.println("Epoch: " + Integer.toString(ResAlloAlgo.trainStep));
                        System.out.println("L2Loss: " + Double.toString(l2loss_epoch));
                        System.out.println("Accuracy: " + Double.toString(accuracy_epoch));

                        LocalDateTime timestamp = LocalDateTime.now();
                        String[] trainingResults = {timestamp.toString(),
                                                    Integer.toString(ResAlloAlgo.trainStep),
                                                    Double.toString(l2loss_epoch),
                                                    Double.toString(accuracy_epoch)};

                        try {
                            writerTraining_results.writeNext(trainingResults);
                            writerTraining_results.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        if (ResAlloAlgo.trainStep > 0 && ResAlloAlgo.trainStep % SAVE_EVERY_STEPS == 0) {
                            model.save(Paths.get(MODEL_PATH), "dqn-" + ResAlloAlgo.trainStep);
                        }
                    }
                }
            } catch (InterruptedException | IOException e) {
                logger.error("", e);
            }
        }
    }

    public static class TrainerCallable implements Callable<Object> {
        private final RlAgent agent;
        private final Model model;

        public TrainerCallable(Model model, RlAgent agent) {
            this.model = model;
            this.agent = agent;
        }

        @Override
        public Object call() throws Exception {
            while (ResAlloAlgo.trainStep < EXPLORE) {
                Thread.sleep(0);
                if (ResAlloAlgo.envStep > OBSERVE) {
                    System.out.println("------------Enough experiences in replay buffer-----------");
                    this.agent.trainBatch(batchSteps);
                    ResAlloAlgo.trainStep++;
                    if (ResAlloAlgo.trainStep > 0 && ResAlloAlgo.trainStep % SAVE_EVERY_STEPS == 0) {
                        model.save(Paths.get(MODEL_PATH), "dqn-" + ResAlloAlgo.trainStep);
                    }
                }
            }
            return null;
        }
    }

    public static class TrainerRunnable implements Runnable {
        private final RlAgent agent;
        private final Model model;

        public TrainerRunnable(Model model, RlAgent agent) {
            this.model = model;
            this.agent = agent;
        }

        @Override
        public void run() {
            try {
                while (ResAlloAlgo.trainStep < EXPLORE) {
                    Thread.sleep(0);
                    //System.out.println(ResAlloAlgo.envStep);
                    if (ResAlloAlgo.envStep > OBSERVE) {
                        System.out.println("------------Enough experiences in replay buffer-----------");
                        this.agent.trainBatch(batchSteps);
                        ResAlloAlgo.trainStep++;
                        if (ResAlloAlgo.trainStep > 0 && ResAlloAlgo.trainStep % SAVE_EVERY_STEPS == 0) {
                            model.save(Paths.get(MODEL_PATH), "dqn-" + ResAlloAlgo.trainStep);
                        }
                    }
                }
            } catch (InterruptedException | IOException e) {
                logger.error("", e);
            }

        }
    }

    private static class GeneratorCallable implements Callable<Object> {
        private final ResAlloAlgo env;
        private final RlAgent agent;
        private final boolean training;

        public GeneratorCallable(ResAlloAlgo env, RlAgent agent, boolean training) {
            this.env = env;
            this.agent = agent;
            this.training = training;
        }

        @Override
        public Object call() {
            while (ResAlloAlgo.trainStep < EXPLORE) {
                batchSteps = env.runEnvironment(agent, training);
            }
            return null;
        }
    }

    private static class GeneratorRunnable implements Runnable {
        private final ResAlloAlgo env;
        private final RlAgent agent;
        private final boolean training;

        public GeneratorRunnable(ResAlloAlgo env, RlAgent agent, boolean training) {
            this.env = env;
            this.agent = agent;
            this.training = training;
        }

        @Override
        public void run() {
            while (ResAlloAlgo.trainStep < EXPLORE) {
                batchSteps = env.runEnvironment(agent, training);
            }
        }
    }

    public static SequentialBlock getBlock() {
        /**
         * A Block whose children form a chain of blocks with each child block feeding its output to the next. 
         * The output of the last child is returned as the output of the SequentialBlock.
         */ 
        return new SequentialBlock()
                .add(Blocks.batchFlattenBlock(NUMBER_INPUTS)) 

                .add(Linear
                        .builder()
                        .setUnits(HIDDEN_NEURONS).build()) // Number of neurons in the hidden layer can be defined as the mean of the input and output layer.
                .add(Activation::relu) // ReLU is defined by: y=max(0,x).

                .add(Linear
                        .builder() // A Linear block applies a linear transformation Y=XWT+b.
                        .setUnits(NUMBER_OUTPUTS).build()); // Sets the number of output channels.
    }

    /**
     * An interface that is responsible for holding the configuration required by Trainer.
     * A Trainer requires an Initializer to initialize the parameters of the model, 
     * an Optimizer to compute gradients and update the parameters according to a Loss function. 
     * It also needs to know the Evaluators that need to be computed during training.
     */
    public static DefaultTrainingConfig setupTrainingConfig() {
        return new DefaultTrainingConfig(Loss.l2Loss()) // Creates an instance of DefaultTrainingConfig with the given Loss. Calculates L2Loss between label and prediction, a.k.a. MSE(Mean Square Error).
                .optOptimizer(Adam.builder().optLearningRateTracker(Tracker.fixed(0.0001f)).build()) // Adam is a generalization of the AdaGrad Optimizer. Sets the learningRateTracker for this optimizer.
                .addEvaluator(new Accuracy())  // Adds an Evaluator that needs to be computed during training. Accuracy is an Evaluator that computes the accuracy score. It is defined as accuracy(y,y^)=1/n∑n−1i=01(yi^==yi).
                .optInitializer(new NormalInitializer(), Parameter.Type.WEIGHT) // NormalInitializer initializes weights with random values sampled from a normal distribution with a mean of zero and standard deviation of sigma. Default standard deviation is 0.01.
                .addTrainingListeners(TrainingListener.Defaults.basic()); // A basic TrainingListener set with minimal recommended functionality.
    }
}