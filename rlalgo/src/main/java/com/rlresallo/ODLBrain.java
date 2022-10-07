package com.rlresallo;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.NoSuchElementException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.IOException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;

import com.opencsv.CSVWriter;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rti.dds.domain.DomainParticipant;
import com.rti.dds.domain.DomainParticipantFactory;
import com.rti.dds.infrastructure.Duration_t;
import com.rti.dds.infrastructure.RETCODE_ERROR;
import com.rti.dds.infrastructure.RETCODE_NO_DATA;
import com.rti.dds.infrastructure.InstanceHandle_t;
import com.rti.dds.infrastructure.StatusCondition;
import com.rti.dds.infrastructure.StatusKind;
import com.rti.dds.infrastructure.WaitSet;
import com.rti.dds.publication.DataWriterQos;
import com.rti.dds.publication.Publisher;
import com.rti.dds.publication.PublisherQos;
import com.rti.dds.publication.builtin.PublicationBuiltinTopicData;
import com.rti.dds.publication.builtin.PublicationBuiltinTopicDataDataReader;
import com.rti.dds.subscription.DataReader;
import com.rti.dds.subscription.DataReaderAdapter;
import com.rti.dds.subscription.DataReaderQos;
import com.rti.dds.subscription.InstanceStateKind;
import com.rti.dds.subscription.SampleInfo;
import com.rti.dds.subscription.Subscriber;
import com.rti.dds.topic.Topic;
import com.rti.dds.publication.builtin.*;
import com.rti.dds.subscription.*;
import com.rti.dds.infrastructure.*;
import com.rti.dds.domain.*;
import com.rti.dds.domain.builtin.*;

import ai.djl.engine.Engine;
import ai.djl.engine.EngineProvider;
import ai.djl.engine.EngineException;
import ai.djl.ndarray.NDManager;
import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.util.Utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.time.LocalDateTime;

public class ODLBrain extends DataReaderAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ODLBrain.class);
    public static final float MAX_CPU_NODES = 4000f;
	private static final float MAX_SOC_NODES = 100f;
	private static final float usedResourcesCost = 0.2f;
    private static final float zeta = 0.2f; // adjustable positive weight lifetime term
	private static final float xi = 0.5f; // adjustable positive weight deployed service term   0.4f
	private static final float phi = 0.3f; // adjustable positive weight used resource cost term 0.2f
	public static int multiDeployedService = 0;
	public static int multiRequestedService = 0;
	public static int multiRejectedService = 0;
	//public static AtomicInteger multiRejectedService = new AtomicInteger();
	public static int multiDeployedVNFs = 0;
	public static int multiRequestedVNFs = 0; 
	public static int multiRejectedVNFs = 0;
	//public static AtomicInteger multiRejectedVNFs = new AtomicInteger();
	public static int deadlineViolations = 0;
	public static int multiFailedVNFs = 0;
	//public static AtomicInteger multiFailedVNFs = new AtomicInteger(); 
	private static String currentVNF = "";
	private static String currentService = "";
	public static AtomicBoolean isCurrentVNFDeployed;
	public static AtomicBoolean isCurrentVNFRejected;
	public static AtomicBoolean areValuesvnfRequestedtoDeploy;
    private static int alertWithinMs = 5;
	private static String selectedNode = "";
	private static int node = 0;
	private static int cluster = 0;
	private static int numberNodes = 0;
	private static int bypassReward = 0;

    private static Map<String, List<String>> ipLocalandPublicPerController = new HashMap<String, List<String>>();

    private static ConcurrentSkipListMap<String, Map<String, Float>> nodesSOCPerMaster; //Concurrent dictionary with master_node of cluster as main key; the values are another dictionary with nodeName as key and SOC of node as value.
	public static ConcurrentSkipListMap<String, Map<String, Float>> nodesCPUPerMaster; //Concurrent dictionary with master_node of cluster as main key; the values are another dictionary with nodeName as key and CPU of node as value.
	public static ConcurrentSkipListMap<String, Map<String, String>> nodesAvailabilityPerMaster; //Concurrent dictionary with master_node of cluster as main key; the values are another dictionary with nodeName as key and node's status as value.
	private static ConcurrentSkipListMap<String, Map<String, String>> anyVNFInNodesPerMaster; //Concurrent dictionary with master_node of cluster as main key; the values are another dictionary with nodeName as key and boolean value indicating if there is any VNF deployed.
	private static ConcurrentSkipListMap<String, Map<String, Map<String, List<String>>>> serviceRequestedPerMaster;
	public static ConcurrentSkipListMap<String, Map<String, List<String>>> serviceRequestedtoDeploy;
	public static ConcurrentSkipListMap<String, List<String>> vnfRequestedtoDeploy;
	public static ConcurrentSkipListMap<String, List<String>> usingNodes;

    private static ConcurrentSkipListMap<String, ParticipantBuiltinTopicData> discoveredParticipants;
	private static ConcurrentSkipListMap<String, ParticipantBuiltinTopicData> failureParticipants;
	private static ConcurrentSkipListMap<String, String> keys;

    private static final String ownKeyName = "GC_UPC_1";

    static DomainParticipant participant = null;
	private static Topic topic = null;
	private static Publisher publisher_local = null;
	private static Publisher publisher_global = null;
	private static Subscriber subscriber_local = null;
	private static Subscriber subscriber_global = null;
	private static topologiaDataWriter dataWriter_global = null;
	private static topologiaDataWriter dataWriter_local = null;
	private static topologiaDataReader dataReader_global = null;
	private static topologiaDataReader dataReader_local = null;

    private static ParticipantBuiltinTopicDataDataReader participantsDR;
	private static PublicationBuiltinTopicDataDataReader publicationsDR;
	private static String id = "gc_upc_1";

    private static TrainResAlloAlgo trainAlgo = null;

	static int sampleCount = 0;

	private static boolean areValuesnodesSOCPerMaster = false;
	private static boolean areValuesnodesCPUPerMaster = false;
	private static boolean areValuesnodesAvailabilityPerMaster = false;

	private static final String CSV_FILE_RESULTS = "src/main/resources/model/Results.csv";
	private static final String CSV_FILE_USAGE = "src/main/resources/model/Usage.csv";
	
	private static File file_results = null;
	private static File file_usage = null;

	private static FileWriter outputResults = null;
	private static FileWriter outputUsage = null;

	private static CSVWriter writerResults = null;
	private static CSVWriter writerUsage = null;

	public ODLBrain() {}

    public static void main(String[] arg) {

        System.setProperty("org.slf4j.simpleLogger.showThreadName", "false");
		System.setProperty("org.slf4j.simpleLogger.showLogName", "false");
		System.setProperty("org.slf4j.simpleLogger.log.ai.djl.pytorch", "WARN");
		System.setProperty("org.slf4j.simpleLogger.log.ai.djl.mxnet", "WARN");
		System.setProperty("org.slf4j.simpleLogger.log.ai.djl.tensorflow", "WARN");
		System.setProperty("ai.djl.default_engine", "MXNet");

		Engine.debugEnvironment();

        int noClusters = 3;
		int noNodes = 4;
		numberNodes = noNodes;
		int batchSize = 64; //32
		boolean preTrained = false;
		boolean testing = false;

		file_results = new File(CSV_FILE_RESULTS);
		file_usage = new File(CSV_FILE_USAGE);

		String[] headerResults = {"Timestamp", 
						   		  "Requested_services", 
						   		  "Scheduled_services", 
								  "Rejected_services",
								  "Requested_VNFs", 
								  "Scheduled_VNFs", 
								  "Rejected_VNFs", 
								  "Failed_VNFs", 
								  "Deadline_violations"};

		String[] headerUsage = {"Timestamp",
								"Cluster",
								"Node", 
								"CPU", 
								"SoC"};

		try {
			outputResults = new FileWriter(file_results);
			outputUsage = new FileWriter(file_usage);

			writerResults = new CSVWriter(outputResults);
			writerUsage = new CSVWriter(outputUsage);

			writerResults.writeNext(headerResults);
			writerUsage.writeNext(headerUsage);

			writerResults.flush();
			writerUsage.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

        trainAlgo = new TrainResAlloAlgo(noClusters, noNodes, batchSize, preTrained, testing);

        discoveredParticipants = new ConcurrentSkipListMap<String, ParticipantBuiltinTopicData>();
		failureParticipants = new ConcurrentSkipListMap<String, ParticipantBuiltinTopicData>();
		keys = new ConcurrentSkipListMap<String, String>();
		nodesSOCPerMaster = new ConcurrentSkipListMap<String, Map<String, Float>>();
		nodesCPUPerMaster = new ConcurrentSkipListMap<String, Map<String, Float>>();
		nodesAvailabilityPerMaster = new ConcurrentSkipListMap<String, Map<String, String>>();
		anyVNFInNodesPerMaster = new ConcurrentSkipListMap<String, Map<String, String>>();
		serviceRequestedPerMaster = new ConcurrentSkipListMap<String, Map<String, Map<String, List<String>>>>();
		serviceRequestedtoDeploy = new ConcurrentSkipListMap<String, Map<String, List<String>>>();
		vnfRequestedtoDeploy = new ConcurrentSkipListMap<String, List<String>>();
		isCurrentVNFDeployed = new AtomicBoolean(false);
		isCurrentVNFRejected = new AtomicBoolean(false);
		areValuesvnfRequestedtoDeploy = new AtomicBoolean(false);
		usingNodes = new ConcurrentSkipListMap<String, List<String>>();

        // Creating DDS participant, Topic, DataWriter and DataReader
		try {
			DomainParticipantFactoryQos factoryQos = new DomainParticipantFactoryQos();
			com.rti.ndds.transport.UDPv4Transport.Property_t transportProperty = new com.rti.ndds.transport.UDPv4Transport.Property_t();

			DomainParticipantFactory.TheParticipantFactory.get_qos(factoryQos);
			factoryQos.entity_factory.autoenable_created_entities = false;
			DomainParticipantFactory.TheParticipantFactory.set_qos(factoryQos);

			DomainParticipantQos pQos = new DomainParticipantQos();
			DomainParticipantFactory.TheParticipantFactory
					.get_default_participant_qos(pQos);
			pQos.discovery_config.participant_liveliness_lease_duration.sec = 10;
			pQos.discovery_config.participant_liveliness_lease_duration.nanosec = 0;
			pQos.discovery_config.participant_liveliness_assert_period.sec = 2;
			pQos.discovery_config.participant_liveliness_assert_period.nanosec = 0;
			pQos.discovery_config.max_liveliness_loss_detection_period.sec = 1;
			pQos.discovery_config.max_liveliness_loss_detection_period.nanosec = 0;
			pQos.resource_limits.participant_user_data_max_length = 1024;
			pQos.receiver_pool.buffer_size = 65530;
			pQos.resource_limits.type_code_max_serialized_length = 65530;
			pQos.resource_limits.type_object_max_serialized_length = 65530;
			pQos.discovery.initial_peers.clear();
			pQos.discovery.initial_peers.setMaximum(8);
			pQos.discovery.initial_peers.add("239.255.0.1");
			pQos.discovery.initial_peers.add("8@builtin.udpv4://127.0.0.1");
			pQos.discovery.initial_peers.add("8@builtin.udpv4://147.83.118.141");
			pQos.discovery.initial_peers.add("8@builtin.udpv4://172.16.10.49");
			pQos.discovery.initial_peers.add("8@builtin.udpv4://163.117.140.219");
			pQos.discovery.initial_peers.add("8@builtin.udpv4://172.16.2.230");
			pQos.discovery.initial_peers.add("8@builtin.udpv4://172.16.2.152");
			pQos.discovery.initial_peers.add("8@builtin.shmem://");
			pQos.discovery.multicast_receive_addresses.clear();
			pQos.discovery.multicast_receive_addresses.setMaximum(2);
			pQos.discovery.multicast_receive_addresses.add("239.255.0.1");
			pQos.participant_name.name = "GC_UPC_1";
			//pQos.user_data.value.addAllByte(id.getBytes());

			participant = DomainParticipantFactory.TheParticipantFactory
					.create_participant(0, pQos, null,
							StatusKind.STATUS_MASK_NONE);
			if (participant == null) {
				System.err.println("create_participant error\n");
				return;
			}

			com.rti.ndds.transport.TransportSupport
					.get_builtin_transport_property(participant,
							transportProperty);
			transportProperty.public_address = "172.16.10.47"; 
			transportProperty.message_size_max = 65530;
			transportProperty.recv_socket_buffer_size = 1048576;
			transportProperty.send_socket_buffer_size = 65530;
			com.rti.ndds.transport.TransportSupport
					.set_builtin_transport_property(participant,
							transportProperty);

			participantsDR = (ParticipantBuiltinTopicDataDataReader) participant
					.get_builtin_subscriber()
					.lookup_datareader(
							ParticipantBuiltinTopicDataTypeSupport.PARTICIPANT_TOPIC_NAME);

			BuiltinParticipantListener builtin_participant_listener = new BuiltinParticipantListener();
			participantsDR.set_listener(builtin_participant_listener,
					StatusKind.DATA_AVAILABLE_STATUS);

			publicationsDR = (PublicationBuiltinTopicDataDataReader) participant
					.get_builtin_subscriber()
					.lookup_datareader(
							PublicationBuiltinTopicDataTypeSupport.PUBLICATION_TOPIC_NAME);

			BuiltinPublicationListener builtin_publication_listener = new BuiltinPublicationListener();
			publicationsDR.set_listener(builtin_publication_listener,
					StatusKind.DATA_AVAILABLE_STATUS);

			participant.enable();

		} catch (Exception e) {
			String lastStartError = "Error creating the DDS domain. Common causes are:"
					+ "\n  - Lack of a network. E.g disconected wireles.s"
					+ "\n  - A network interface that does not bind multicast address. In some platforms enabling using the TUN interface "
					+ "\n     for (Open) VPN causes this. If this is your situation try configure (Open)VPN to use TAP instead.";

			System.out.println(lastStartError);
		}

		// Topic for communication GC-GC, GC-AC, GC-EdgeNodes

		String typeName = topologiaTypeSupport.get_type_name();
		topologiaTypeSupport.register_type(participant, typeName);

		topic = participant.create_topic("status", typeName,
				DomainParticipant.TOPIC_QOS_DEFAULT, null, // listener
				StatusKind.STATUS_MASK_NONE);
		if (topic == null) {
			System.err.println("Unable to create topic.");
			return;
		}

		// Getting the default PublisherQoS and adding a partition name

		//PublisherQos pub_qos_local = new PublisherQos();
		//participant.get_default_publisher_qos(pub_qos_local);
		//pub_qos_local.partition.name.clear();
		//pub_qos_local.partition.name.add("mainupc");
		//pub_qos_local.partition.name.add("backup");
		
		PublisherQos pub_qos_global = new PublisherQos();
		participant.get_default_publisher_qos(pub_qos_global);
		pub_qos_global.partition.name.clear();
		pub_qos_global.exclusive_area.use_shared_exclusive_area = true;
		//pub_qos_global.partition.name.add("global");

		// Publisher for communication GC-AC

		/**
		publisher_local = participant.create_publisher(pub_qos_local, null,
				StatusKind.STATUS_MASK_NONE);

		if (publisher_local == null) {
			System.err.println("Unable to create publisher\n");
			return;
		}
		*/
		
		// Publisher for communication GC-GC

		publisher_global = participant.create_publisher(pub_qos_global, null,
				StatusKind.STATUS_MASK_NONE);

		if (publisher_global == null) {
			System.err.println("Unable to create publisher\n");
			return;
		}

		// Getting the default DataWriterQoS and adding other parameters

		DataWriterQos dwqos = new DataWriterQos();
		participant.get_default_datawriter_qos(dwqos);
		dwqos.liveliness.lease_duration.sec = 2;
		dwqos.liveliness.lease_duration.nanosec = 0;
		dwqos.reliability.kind = ReliabilityQosPolicyKind.RELIABLE_RELIABILITY_QOS;
		dwqos.history.kind = HistoryQosPolicyKind.KEEP_ALL_HISTORY_QOS;
		dwqos.durability.kind = DurabilityQosPolicyKind.TRANSIENT_LOCAL_DURABILITY_QOS;
		// dwqos.reliability.max_blocking_time.sec = 0;
		// dwqos.reliability.max_blocking_time.nanosec = 8 * alertWithinMs * 1000000;
		dwqos.reliability.max_blocking_time.sec = 2;
		dwqos.reliability.max_blocking_time.nanosec = 0;
		dwqos.resource_limits.max_samples = com.rti.dds.infrastructure.ResourceLimitsQosPolicy.LENGTH_UNLIMITED;
		dwqos.protocol.rtps_reliable_writer.min_send_window_size = 20;
		dwqos.protocol.rtps_reliable_writer.max_send_window_size = dwqos.protocol.rtps_reliable_writer.min_send_window_size;
		dwqos.protocol.rtps_reliable_writer.heartbeats_per_max_samples = dwqos.protocol.rtps_reliable_writer.max_send_window_size;
		dwqos.protocol.rtps_reliable_writer.min_nack_response_delay.sec = 0;
		dwqos.protocol.rtps_reliable_writer.min_nack_response_delay.nanosec = 0;
		dwqos.protocol.rtps_reliable_writer.max_nack_response_delay.sec = 0;
		dwqos.protocol.rtps_reliable_writer.max_nack_response_delay.nanosec = 0;
		dwqos.protocol.rtps_reliable_writer.fast_heartbeat_period.sec = 0;
		dwqos.protocol.rtps_reliable_writer.fast_heartbeat_period.nanosec = alertWithinMs * 1000000;
		dwqos.protocol.rtps_reliable_writer.max_heartbeat_retries = 7;
		dwqos.protocol.rtps_reliable_writer.late_joiner_heartbeat_period.sec = 0;
		dwqos.protocol.rtps_reliable_writer.late_joiner_heartbeat_period.nanosec = alertWithinMs * 1000000;
		dwqos.publication_name.name = "GC_UPC_1";

		// DataWriter for communication GC-GC

		dataWriter_global = (topologiaDataWriter) publisher_global.create_datawriter(
				topic, dwqos, null, // listener
				StatusKind.STATUS_MASK_NONE);
		if (dataWriter_global == null) {
			System.err.println("Unable to create data writer\n");
			return;
		}
		
		InstanceHandle_t instancelocal_g = dataWriter_global.get_instance_handle();
		participant.ignore_publication(instancelocal_g);

		// DataWriter for communication GC-AC

		/** 
		dataWriter_local = (topologiaDataWriter) publisher_local.create_datawriter(
				topic, dwqos, null, // listener
				StatusKind.STATUS_MASK_NONE);
		if (dataWriter_local == null) {
			System.err.println("Unable to create data writer\n");
			return;
		}
		
		InstanceHandle_t instancelocal_l = dataWriter_local.get_instance_handle();
		participant.ignore_publication(instancelocal_l);
		*/

		// Getting the default SubscriberQoS and adding a partition name
		
		//SubscriberQos sub_qos_local = new SubscriberQos();
		//participant.get_default_subscriber_qos(sub_qos_local);
		//sub_qos_local.partition.name.clear();
		//sub_qos_local.partition.name.add("mainupc");
		//sub_qos_local.partition.name.add("backup");
		
		SubscriberQos sub_qos_global = new SubscriberQos();
		participant.get_default_subscriber_qos(sub_qos_global);
		sub_qos_global.partition.name.clear();
		sub_qos_global.exclusive_area.use_shared_exclusive_area = true;
		//sub_qos_global.partition.name.add("global");
		
		// Subscriber for communication GC-GC
		
		subscriber_global = participant.create_subscriber(sub_qos_global, null, StatusKind.STATUS_MASK_NONE);
				
		if (subscriber_global == null) {
			System.err.println("Unable to create subscriber\n");
			return;
		}

		// Subscriber for communication GC-AC

		/**
		subscriber_local = participant.create_subscriber(sub_qos_local, null,
				StatusKind.STATUS_MASK_NONE);

		if (subscriber_local == null) {
			System.err.println("Unable to create subscriber\n");
			return;
		}
		*/

		// Getting the default DataReaderQoS and adding other parameters

		DataReaderQos drqos = new DataReaderQos();
		participant.get_default_datareader_qos(drqos);
		drqos.liveliness.lease_duration.sec = 2;
		drqos.liveliness.lease_duration.nanosec = 0;
		drqos.reliability.kind = ReliabilityQosPolicyKind.RELIABLE_RELIABILITY_QOS;
		drqos.history.kind = HistoryQosPolicyKind.KEEP_ALL_HISTORY_QOS;
		drqos.durability.kind = DurabilityQosPolicyKind.TRANSIENT_LOCAL_DURABILITY_QOS;
		drqos.reliability.max_blocking_time.sec = 2;
		drqos.reliability.max_blocking_time.nanosec = 0;
		drqos.resource_limits.max_samples = com.rti.dds.infrastructure.ResourceLimitsQosPolicy.LENGTH_UNLIMITED;
		drqos.protocol.rtps_reliable_reader.min_heartbeat_response_delay.sec = 0;
		drqos.protocol.rtps_reliable_reader.min_heartbeat_response_delay.nanosec = 0;
		drqos.protocol.rtps_reliable_reader.max_heartbeat_response_delay.sec = 0;
		drqos.protocol.rtps_reliable_reader.max_heartbeat_response_delay.nanosec = 0;
		drqos.subscription_name.name = "GC_UPC_1";

		// DataReader for communication GC-GC

		//ReaderListener reader_listener = new ReaderListener();
		
		dataReader_global = (topologiaDataReader) subscriber_global
				.create_datareader(topic, drqos, null, // Listener
						StatusKind.DATA_AVAILABLE_STATUS);
		if (dataReader_global == null) {
			System.err.println("Unable to create DDS Data Reader");
			return;
		}

		// DataReader for communication GC-AC

		/**
		dataReader_local = (topologiaDataReader) subscriber_local.create_datareader(
				topic, drqos, null,// listener
				StatusKind.STATUS_MASK_NONE);
		if (dataReader_local == null) {
			System.err.println("Unable to create DDS Data Reader");
			return;
		}
		*/

		// Configuring the reader conditions using StatusCondition and WaitSet
		
		WaitSet waitset = new WaitSet();
		
		StatusCondition status_condition_global = dataReader_global
				.get_statuscondition();
		if (status_condition_global == null) {
			System.err.println("get_statuscondition error\n");
			return;
		}
		
		/**
		StatusCondition status_condition_local = dataReader_local
				.get_statuscondition();
		if (status_condition_local == null) {
			System.err.println("get_statuscondition error\n");
			return;
		}
		*/
		
		status_condition_global
				.set_enabled_statuses(StatusKind.DATA_AVAILABLE_STATUS);
		//status_condition_local
		//		.set_enabled_statuses(StatusKind.DATA_AVAILABLE_STATUS);

		waitset.attach_condition(status_condition_global);
		//waitset.attach_condition(status_condition_local);

		final long receivedata = 1;

		try {
			Model model = trainAlgo.createOrLoadModel(preTrained);
			if (testing) {
				trainAlgo.test(model);
			} else {
				System.out.println("------------Training model-----------");
				trainAlgo.train(batchSize, preTrained, testing, model, noClusters, noNodes);
			}
		} catch (IOException io) {
			System.out.println(io);
		} catch (MalformedModelException me) {
			System.out.println(me);
		}
		
		for (int count = 0; (sampleCount == 0) || (count < sampleCount); ++count) {

			ConditionSeq active_condition_seq = new ConditionSeq();
			Duration_t wait_timeout = new Duration_t(
					Duration_t.DURATION_INFINITE);

			try {
				waitset.wait(active_condition_seq, wait_timeout);
			} catch (RETCODE_TIMEOUT e) {
				continue;
			}

			for (int i = 0; i < active_condition_seq.size(); ++i) {

				if (active_condition_seq.get(i) == status_condition_global) {
					int triggerMask_global = dataReader_global
							.get_status_changes();
					// Data available
					if ((triggerMask_global & StatusKind.DATA_AVAILABLE_STATUS) != 0) {
						on_data_available_global();
					}
				}

				/**
				if (active_condition_seq.get(i) == status_condition_local) {
					int triggerMask_local = dataReader_local
							.get_status_changes();
					// Data available
					if ((triggerMask_local & StatusKind.DATA_AVAILABLE_STATUS) != 0) {
						on_data_available_local();
					}
				}
				*/
			}
			try {
				Thread.sleep(receivedata * 1000);
			} catch (InterruptedException ix) {
				System.err.println("INTERRUPTED");
				closeWriters();
				break;
			}
		}
		
    }
	public static boolean getVariableareValuesvnfRequestedtoDeploy(){
		boolean value = areValuesvnfRequestedtoDeploy.get();
		return value;
	}

	public static void closeWriters() {
		try {
			writerResults.flush();
			writerUsage.flush();
			writerResults.close();
			writerUsage.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static boolean currentVNFDeployed(){
		boolean value = isCurrentVNFDeployed.get();
		return value;
	}

	public static boolean currentVNFRejected(){
		boolean value = isCurrentVNFRejected.get();
		return value;
	}

	//public static int getFailedVNFs() {
	//	int value = multiFailedVNFs.get();
	//	return value;
	//}

	//public static int getRejectedVNFs() {
	//	int value = multiRejectedVNFs.get();
	//	return value;
	//}

	//public static int getRejectedServices() {
	//	int value = multiRejectedService.get();
	//	return value;
	//}

	private static void on_data_available_global() {

		SampleInfo info = new SampleInfo();
		topologia sample = new topologia();
		
		boolean follow = true;
		while (follow) {
			try {
				dataReader_global.take_next_sample(sample, info);

				// Reading nodes' information belonging to registered clusters
				if (sample.Identificador.equals("Node_Status")) {

					PublicationBuiltinTopicData publicationData = new PublicationBuiltinTopicData();

					dataReader_global.get_matched_publication_data(publicationData, info.publication_handle);

					ParticipantBuiltinTopicData participantDataInfo = discoveredParticipants
							.get(publicationData.participant_key.toString());

					String cluster = participantDataInfo.participant_name.name.substring(participantDataInfo.participant_name.name.length() - 1);
					float soc = 0f;
					String k8snodeId = sample.NodeId;
					float cpu = Float.parseFloat(sample.TerminationPointId); 
					if (sample.LinkId.length() > 0) {
						soc = Float.parseFloat(sample.LinkId);
					} 
					String anyVNFRunning = sample.SourceNode;
					String nodeReady = sample.SourceNodeTp;

					if (!ODLBrain.nodesSOCPerMaster.containsKey(participantDataInfo.participant_name.name)) {
						ODLBrain.nodesSOCPerMaster.put(participantDataInfo.participant_name.name, new HashMap<String, Float>());
						ODLBrain.areValuesnodesSOCPerMaster = true;
					} else {
						ODLBrain.nodesSOCPerMaster.get(participantDataInfo.participant_name.name).put(k8snodeId, soc);
						//System.out.println(nodesSOCPerMaster);
					}

					if (!ODLBrain.nodesCPUPerMaster.containsKey(participantDataInfo.participant_name.name)) {
						ODLBrain.nodesCPUPerMaster.put(participantDataInfo.participant_name.name, new HashMap<String, Float>());
						ODLBrain.areValuesnodesCPUPerMaster = true;
					} else {
						ODLBrain.nodesCPUPerMaster.get(participantDataInfo.participant_name.name).put(k8snodeId, cpu);
						//System.out.println(nodesCPUPerMaster);
					}

					if (!ODLBrain.nodesAvailabilityPerMaster.containsKey(participantDataInfo.participant_name.name)) {
						ODLBrain.nodesAvailabilityPerMaster.put(participantDataInfo.participant_name.name, new HashMap<String, String>());
						ODLBrain.areValuesnodesAvailabilityPerMaster = true;
					} else {
						ODLBrain.nodesAvailabilityPerMaster.get(participantDataInfo.participant_name.name).put(k8snodeId, nodeReady);
					}

					if (!ODLBrain.anyVNFInNodesPerMaster.containsKey(participantDataInfo.participant_name.name)) {
						ODLBrain.anyVNFInNodesPerMaster.put(participantDataInfo.participant_name.name, new HashMap<String, String>());
					} else {
						ODLBrain.anyVNFInNodesPerMaster.get(participantDataInfo.participant_name.name).put(k8snodeId, anyVNFRunning);
						//System.out.println(anyVNFInNodesPerMaster);
					}

					LocalDateTime timestamp = LocalDateTime.now();
					String[] usage = {timestamp.toString(), 		//timestamp
									  cluster,              		//ID_cluster
									  k8snodeId,					//ID_Node
									  sample.TerminationPointId,	//CPU_Node
									  sample.LinkId};				//SOC_Node

					try {
						writerUsage.writeNext(usage);
						writerUsage.flush();
					} catch (IOException e) {
						e.printStackTrace();
					}

				}	

				// Reading service request with the current VNF to deploy, its requirements and remaining VNFs to deploy.
				if (sample.Identificador.startsWith("serv-")) {
					String vnfId = "";
					float vnfCpuRequested = 0f;
					int vnfRunningTime = 0;
					String vnfsInService = "";
					PublicationBuiltinTopicData publicationData = new PublicationBuiltinTopicData();

					dataReader_global.get_matched_publication_data(publicationData, info.publication_handle);

					ParticipantBuiltinTopicData participantDataInfo = discoveredParticipants
							.get(publicationData.participant_key.toString());

					String serviceId = sample.Identificador;
					if (sample.NodeId.length() > 0) {
						vnfId = sample.NodeId;
					}
					if (Float.parseFloat(sample.TerminationPointId) != 0f) {
						vnfCpuRequested = Float.parseFloat(sample.TerminationPointId);
					}
					if (Integer.parseInt(sample.LinkId) != 0) {
						vnfRunningTime = Integer.parseInt(sample.LinkId);
					}
					String vnfDeadline = sample.SourceNodeTp;
					vnfsInService = sample.DestinationNodeTp;
					/*
					if (sample.DestinationNodeTp.equals("0")) { // Check if service was deployed
						TutorialL2Forwarding.serviceRequestedPerMaster.get(participantDataInfo.participant_name.name).remove(serviceId);
						float reward = TutorialL2Forwarding.calculateReward();
						ResAlloAlgo.setCurrentReward(reward);
					} else if (sample.DestinationNodeTp.equals("-1")) { // Check if service was rejected
						Set<String> keysNodes = TutorialL2Forwarding.serviceRequestedPerMaster.get(participantDataInfo.participant_name.name).get(serviceId).keySet();
						Iterator<String> iterNodes = keysNodes.iterator();
						while (iterNodes.hasNext()) {
							TutorialL2Forwarding.vnfRequestedtoDeploy.remove(iterNodes.next());
						}
						TutorialL2Forwarding.serviceRequestedPerMaster.get(participantDataInfo.participant_name.name).remove(serviceId);
						ResAlloAlgo.setCurrentReward(0f);
					} else {
						vnfsInService = sample.DestinationNodeTp;
					}
					*/
					String isMultiClusterDeployment = sample.SourceNode;
					List<String> vnfRequirements = new ArrayList<String>();
					vnfRequirements.add(0, Float.toString(vnfCpuRequested));
					vnfRequirements.add(1, Integer.toString(vnfRunningTime));
					vnfRequirements.add(2, vnfDeadline);
					vnfRequirements.add(3, vnfsInService);
					vnfRequirements.add(4, serviceId);
					vnfRequirements.add(5, "not deployed");
					
					if (!ODLBrain.serviceRequestedPerMaster.containsKey(participantDataInfo.participant_name.name)) {
						ODLBrain.serviceRequestedPerMaster.put(participantDataInfo.participant_name.name, new HashMap<String, Map<String, List<String>>>());
					} 
					if (!ODLBrain.serviceRequestedPerMaster.get(participantDataInfo.participant_name.name).containsKey(serviceId)) {
						ODLBrain.serviceRequestedPerMaster.get(participantDataInfo.participant_name.name).put(serviceId, new HashMap<String, List<String>>());
						ODLBrain.serviceRequestedtoDeploy.put(serviceId, new HashMap<String, List<String>>());
						ODLBrain.multiRequestedService++;
						//System.out.println("Multi-requested services: " + Integer.toString(ODLBrain.multiRequestedService));

						LocalDateTime timestamp = LocalDateTime.now();
						String[] result = {timestamp.toString(), 		//timestamp
										   Integer.toString(ODLBrain.multiRequestedService), 
										   Integer.toString(ODLBrain.multiDeployedService), 
										   Integer.toString(ODLBrain.multiRejectedService), 
										   Integer.toString(ODLBrain.multiRequestedVNFs), 
										   Integer.toString(ODLBrain.multiDeployedVNFs), 
										   Integer.toString(ODLBrain.multiRejectedVNFs), 
										   Integer.toString(ODLBrain.multiFailedVNFs), 
										   Integer.toString(ODLBrain.deadlineViolations)};				
	
						try {
							writerResults.writeNext(result);
							writerResults.flush();
						} catch (IOException e) {
							e.printStackTrace();
						}

					}
					if (!ODLBrain.serviceRequestedPerMaster.get(participantDataInfo.participant_name.name).get(serviceId).containsKey(vnfId)) {
						ODLBrain.serviceRequestedPerMaster.get(participantDataInfo.participant_name.name).get(serviceId).put(vnfId, vnfRequirements);
						ODLBrain.serviceRequestedtoDeploy.get(serviceId).put(vnfId, vnfRequirements);
						ODLBrain.multiRequestedVNFs++;
						//System.out.println("Multi-requested VNFs: " + Integer.toString(ODLBrain.multiRequestedVNFs));
						//System.out.println(ODLBrain.serviceRequestedPerMaster);

						LocalDateTime timestamp = LocalDateTime.now();
						String[] result = {timestamp.toString(), 		//timestamp
										   Integer.toString(ODLBrain.multiRequestedService), 
										   Integer.toString(ODLBrain.multiDeployedService), 
										   Integer.toString(ODLBrain.multiRejectedService), 
										   Integer.toString(ODLBrain.multiRequestedVNFs), 
										   Integer.toString(ODLBrain.multiDeployedVNFs), 
										   Integer.toString(ODLBrain.multiRejectedVNFs), 
										   Integer.toString(ODLBrain.multiFailedVNFs), 
										   Integer.toString(ODLBrain.deadlineViolations)};				

						try {
							writerResults.writeNext(result);
							writerResults.flush();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}

					if (!ODLBrain.vnfRequestedtoDeploy.containsKey(vnfId) && isMultiClusterDeployment.equals("True")) {
						ODLBrain.vnfRequestedtoDeploy.put(vnfDeadline.concat(vnfId), vnfRequirements);
						//ODLBrain.setVariableareValuesvnfRequestedtoDeploy(true);
						areValuesvnfRequestedtoDeploy.set(true);
					}

					/*
					if (!sample.DestinationNodeTp.equals("0") || !sample.DestinationNodeTp.equals("-1")) {
						List<String> vnfRequirements = new ArrayList<String>();
						vnfRequirements.add(0, Integer.toString(vnfCpuRequested));
						vnfRequirements.add(1, Integer.toString(vnfRunningTime));
						vnfRequirements.add(2, vnfDeadline);
						vnfRequirements.add(3, vnfsInService);
						vnfRequirements.add(4, serviceId);
						vnfRequirements.add(5, "not deployed");

						if (!TutorialL2Forwarding.serviceRequestedPerMaster.containsKey(participantDataInfo.participant_name.name)) {
							TutorialL2Forwarding.serviceRequestedPerMaster.put(participantDataInfo.participant_name.name, new HashMap<String, Map<String, List<String>>>());
						} 
						if (!TutorialL2Forwarding.serviceRequestedPerMaster.get(participantDataInfo.participant_name.name).containsKey(serviceId)) {
							TutorialL2Forwarding.serviceRequestedPerMaster.get(participantDataInfo.participant_name.name).put(serviceId, new HashMap<String, List<String>>());
						}
						if (!TutorialL2Forwarding.serviceRequestedPerMaster.get(participantDataInfo.participant_name.name).get(serviceId).containsKey(vnfId)) {
							TutorialL2Forwarding.serviceRequestedPerMaster.get(participantDataInfo.participant_name.name).get(serviceId).put(vnfId, vnfRequirements);
						}
						if (!TutorialL2Forwarding.vnfRequestedtoDeploy.containsKey(vnfId) && isMultiClusterDeployment.equals("True")) {
							TutorialL2Forwarding.vnfRequestedtoDeploy.put(vnfDeadline.concat(vnfId), vnfRequirements);
						}
					}
					*/
				}

				// Reading information regarding the status of the last VNF deployed.
				if (sample.Identificador.equals("VNF_Deployed")) {
					PublicationBuiltinTopicData publicationData = new PublicationBuiltinTopicData();

					dataReader_global.get_matched_publication_data(publicationData, info.publication_handle);

					ParticipantBuiltinTopicData participantDataInfo = discoveredParticipants
							.get(publicationData.participant_key.toString());

					String vnfId = sample.NodeId;
					String service = sample.TerminationPointId;

					System.out.println(vnfId + " deployed in cluster: " + participantDataInfo.participant_name.name);

					if (vnfId.equals(ODLBrain.currentVNF)) {
						//isCurrentVNFDeployed.set(true); 
						ODLBrain.multiDeployedVNFs++;
						//System.out.println("Multi-deployed VNFs: " + Integer.toString(ODLBrain.multiDeployedVNFs));

						LocalDateTime timestamp = LocalDateTime.now();
						String[] result = {timestamp.toString(), 		//timestamp
										   Integer.toString(ODLBrain.multiRequestedService), 
										   Integer.toString(ODLBrain.multiDeployedService), 
										   Integer.toString(ODLBrain.multiRejectedService), 
										   Integer.toString(ODLBrain.multiRequestedVNFs), 
										   Integer.toString(ODLBrain.multiDeployedVNFs), 
										   Integer.toString(ODLBrain.multiRejectedVNFs), 
										   Integer.toString(ODLBrain.multiFailedVNFs), 
										   Integer.toString(ODLBrain.deadlineViolations)};				
	
						try {
							writerResults.writeNext(result);
							writerResults.flush();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					List<String> vnfInformation = ODLBrain.serviceRequestedPerMaster.get("kubernetes-control-plane1").get(service).get(vnfId);
					if (vnfInformation.get(5).equals("not deployed")) {
						vnfInformation.add(5, "deployed");
						ODLBrain.serviceRequestedPerMaster.get("kubernetes-control-plane1").get(service).put(vnfId, vnfInformation);
					}
					boolean serviceDeployed = ODLBrain.areAllVNFScheduled("kubernetes-control-plane1", service);
					if (serviceDeployed) {
						ODLBrain.multiDeployedService++;
						//System.out.println("Multi-deployed Services: " + Integer.toString(ODLBrain.multiDeployedService));

						LocalDateTime timestamp = LocalDateTime.now();
						String[] result = {timestamp.toString(), 		//timestamp
										   Integer.toString(ODLBrain.multiRequestedService), 
										   Integer.toString(ODLBrain.multiDeployedService), 
										   Integer.toString(ODLBrain.multiRejectedService), 
										   Integer.toString(ODLBrain.multiRequestedVNFs), 
										   Integer.toString(ODLBrain.multiDeployedVNFs), 
										   Integer.toString(ODLBrain.multiRejectedVNFs), 
										   Integer.toString(ODLBrain.multiFailedVNFs), 
										   Integer.toString(ODLBrain.deadlineViolations)};				
	
						try {
							writerResults.writeNext(result);
							writerResults.flush();
						} catch (IOException e) {
							e.printStackTrace();
						}

						float reward = ODLBrain.calculateReward();
						ResAlloAlgo.setCurrentReward(reward);
						isCurrentVNFDeployed.set(true); 
					} else {
						float reward = ODLBrain.calculateReward();
						ResAlloAlgo.setCurrentReward(reward);
						isCurrentVNFDeployed.set(true); 
					}
				}

				// Reading information regarding the status of the last VNF rejected.
				if (sample.Identificador.equals("VNF_Rejected")) {
					PublicationBuiltinTopicData publicationData = new PublicationBuiltinTopicData();

					dataReader_global.get_matched_publication_data(publicationData, info.publication_handle);

					ParticipantBuiltinTopicData participantDataInfo = discoveredParticipants
							.get(publicationData.participant_key.toString());

					String vnfId = sample.NodeId;
					//String service = sample.TerminationPointId;

					System.out.println(vnfId + " rejected in cluster: " + participantDataInfo.participant_name.name);

					if (vnfId.equals(ODLBrain.currentVNF)) {
						//isCurrentVNFDeployed.set(true); 
						ODLBrain.multiFailedVNFs++;
						//System.out.println("Multi-deployed VNFs: " + Integer.toString(ODLBrain.multiDeployedVNFs));

						int vnfServScheduled = 0;
						Set<String> keysVNfs = ODLBrain.serviceRequestedtoDeploy.get(ODLBrain.currentService).keySet();
						Iterator<String> iteratorKeys = keysVNfs.iterator();
						while(iteratorKeys.hasNext()) {
							String key = iteratorKeys.next();
							List<String> infoVNf = ODLBrain.serviceRequestedtoDeploy.get(ODLBrain.currentService).get(key);
							if (infoVNf.get(5).equals("deployed")) {
								vnfServScheduled++;
							}
							String keyConcat = infoVNf.get(2).concat(key);
							if (ODLBrain.vnfRequestedtoDeploy.containsKey(keyConcat)) {
								ODLBrain.vnfRequestedtoDeploy.remove(keyConcat);
								System.out.println("Removing key: " + keyConcat + " from vnfRequestedtoDeploy");
							}
						}
						int delta = keysVNfs.size() - vnfServScheduled;
						ODLBrain.multiRejectedVNFs = ODLBrain.multiRejectedVNFs + delta;
						ODLBrain.multiRejectedService++;

						LocalDateTime timestamp = LocalDateTime.now();
						String[] result = {timestamp.toString(), 		//timestamp
										   Integer.toString(ODLBrain.multiRequestedService), 
										   Integer.toString(ODLBrain.multiDeployedService), 
										   Integer.toString(ODLBrain.multiRejectedService), 
										   Integer.toString(ODLBrain.multiRequestedVNFs), 
										   Integer.toString(ODLBrain.multiDeployedVNFs), 
										   Integer.toString(ODLBrain.multiRejectedVNFs), 
										   Integer.toString(ODLBrain.multiFailedVNFs), 
										   Integer.toString(ODLBrain.deadlineViolations)};				
	
						try {
							writerResults.writeNext(result);
							writerResults.flush();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					ResAlloAlgo.setCurrentReward(0f);
					isCurrentVNFRejected.set(true); 
				}

			} catch (RETCODE_NO_DATA noData) {
				// No more data to read
				follow = false;
			} catch (RETCODE_PRECONDITION_NOT_MET notMet) {
				PrintStream fileErr = null;
				try {
					fileErr = new PrintStream(new FileOutputStream("./err.txt", true), true);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				notMet.printStackTrace(fileErr);
			} catch (RETCODE_BAD_PARAMETER bp) {
				PrintStream fileErr = null;
				try {
					fileErr = new PrintStream(new FileOutputStream("./err.txt", true), true);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				bp.printStackTrace(fileErr);
			} catch (RETCODE_ERROR err) {
				// An error occurred
				PrintStream fileErr = null;
				try {
					fileErr = new PrintStream(new FileOutputStream("./err.txt", true), true);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				err.printStackTrace(fileErr);
			//} catch (FileNotFoundException e) {
			//	e.printStackTrace();
			} finally {
			}
		}
	}

    /**
	 * Class to implement a listener to access to the data sent by datawriters in DDS domain.
	 */
	/** 
	public static class ReaderListener extends DataReaderAdapter {
		public void on_data_available (DataReader reader) {

			SampleInfo info = new SampleInfo();
			topologia sample = new topologia();
			topologiaDataReader dataReader = (topologiaDataReader) reader;
			
			boolean follow = true;
			while (follow) {
				try {
					dataReader.take_next_sample(sample, info);
					
					//PrintStream originalOut = System.out;
					
					//PrintStream fileOut = new PrintStream(new FileOutputStream("./out.txt", true), true);
					
					//System.setOut(fileOut);
	
					// Reading nodes' information belonging to registered clusters
					if (sample.Identificador.equals("Node_Status")) {
	
						PublicationBuiltinTopicData publicationData = new PublicationBuiltinTopicData();
	
						dataReader.get_matched_publication_data(publicationData, info.publication_handle);
	
						ParticipantBuiltinTopicData participantDataInfo = discoveredParticipants
								.get(publicationData.participant_key.toString());
	
						// String keyIPPublicMaster = IPfromLocatorMetatraffic(participantDataInfo.metatraffic_unicast_locators);
	
						//System.out.println("Receiving node information from: "
						//		+ participantDataInfo.participant_name.name);
	
						float soc = 0f;
						//NodeId nodeId = new NodeId(sample.NodeId);
						String k8snodeId = sample.NodeId;
						float cpu = Float.parseFloat(sample.TerminationPointId); 
						if (sample.LinkId.length() > 0) {
							soc = Float.parseFloat(sample.LinkId);
						} 
						String anyVNFRunning = sample.SourceNode;
	
						//String nodeid = nodeId.getValue().toString();
						//String k8snodeId = TranslateFormatControllerId(nodeid);
						if (!ODLBrain.nodesSOCPerMaster.containsKey(participantDataInfo.participant_name.name)) {
							ODLBrain.nodesSOCPerMaster.put(participantDataInfo.participant_name.name, new HashMap<String, Float>());
							ODLBrain.areValuesnodesSOCPerMaster = true;
						} else {
							ODLBrain.nodesSOCPerMaster.get(participantDataInfo.participant_name.name).put(k8snodeId, soc);
							System.out.println(nodesSOCPerMaster);
						}
	
						if (!ODLBrain.nodesCPUPerMaster.containsKey(participantDataInfo.participant_name.name)) {
							ODLBrain.nodesCPUPerMaster.put(participantDataInfo.participant_name.name, new HashMap<String, Float>());
							ODLBrain.areValuesnodesCPUPerMaster = true;
						} else {
							ODLBrain.nodesCPUPerMaster.get(participantDataInfo.participant_name.name).put(k8snodeId, cpu);
							System.out.println(nodesCPUPerMaster);
						}
	
						if (!ODLBrain.anyVNFInNodesPerMaster.containsKey(participantDataInfo.participant_name.name)) {
							ODLBrain.anyVNFInNodesPerMaster.put(participantDataInfo.participant_name.name, new HashMap<String, String>());
						} else {
							ODLBrain.anyVNFInNodesPerMaster.get(participantDataInfo.participant_name.name).put(k8snodeId, anyVNFRunning);
							//System.out.println(anyVNFInNodesPerMaster);
						}
					}	
	
					// Reading service request with the current VNF to deploy, its requirements and remaining VNFs to deploy.
					if (sample.Identificador.startsWith("serv-")) {
						String vnfId = "";
						float vnfCpuRequested = 0f;
						int vnfRunningTime = 0;
						String vnfsInService = "";
						PublicationBuiltinTopicData publicationData = new PublicationBuiltinTopicData();
	
						dataReader.get_matched_publication_data(publicationData, info.publication_handle);
	
						ParticipantBuiltinTopicData participantDataInfo = discoveredParticipants
								.get(publicationData.participant_key.toString());
						
						//System.out.println("Receiving service request from: " + participantDataInfo.participant_name.name);
	
						String serviceId = sample.Identificador;
						if (sample.NodeId.length() > 0) {
							vnfId = sample.NodeId;
						}
						if (Float.parseFloat(sample.TerminationPointId) != 0f) {
							vnfCpuRequested = Float.parseFloat(sample.TerminationPointId);
						}
						if (Integer.parseInt(sample.LinkId) != 0) {
							vnfRunningTime = Integer.parseInt(sample.LinkId);
						}
						String vnfDeadline = sample.SourceNodeTp;
						vnfsInService = sample.DestinationNodeTp;
						/*
						if (sample.DestinationNodeTp.equals("0")) { // Check if service was deployed
							TutorialL2Forwarding.serviceRequestedPerMaster.get(participantDataInfo.participant_name.name).remove(serviceId);
							float reward = TutorialL2Forwarding.calculateReward();
							ResAlloAlgo.setCurrentReward(reward);
						} else if (sample.DestinationNodeTp.equals("-1")) { // Check if service was rejected
							Set<String> keysNodes = TutorialL2Forwarding.serviceRequestedPerMaster.get(participantDataInfo.participant_name.name).get(serviceId).keySet();
							Iterator<String> iterNodes = keysNodes.iterator();
							while (iterNodes.hasNext()) {
								TutorialL2Forwarding.vnfRequestedtoDeploy.remove(iterNodes.next());
							}
							TutorialL2Forwarding.serviceRequestedPerMaster.get(participantDataInfo.participant_name.name).remove(serviceId);
							ResAlloAlgo.setCurrentReward(0f);
						} else {
							vnfsInService = sample.DestinationNodeTp;
						}
						
						String isMultiClusterDeployment = sample.SourceNode;
						List<String> vnfRequirements = new ArrayList<String>();
						vnfRequirements.add(0, Float.toString(vnfCpuRequested));
						vnfRequirements.add(1, Integer.toString(vnfRunningTime));
						vnfRequirements.add(2, vnfDeadline);
						vnfRequirements.add(3, vnfsInService);
						vnfRequirements.add(4, serviceId);
						vnfRequirements.add(5, "not deployed");
						//System.out.println(vnfRequirements);
						
						if (!ODLBrain.serviceRequestedPerMaster.containsKey(participantDataInfo.participant_name.name)) {
							ODLBrain.serviceRequestedPerMaster.put(participantDataInfo.participant_name.name, new HashMap<String, Map<String, List<String>>>());
						} 
						if (!ODLBrain.serviceRequestedPerMaster.get(participantDataInfo.participant_name.name).containsKey(serviceId)) {
							ODLBrain.serviceRequestedPerMaster.get(participantDataInfo.participant_name.name).put(serviceId, new HashMap<String, List<String>>());
							ODLBrain.multiRequestedService++;
							System.out.println("Multi-requested services: " + Integer.toString(ODLBrain.multiRequestedService));
						}
						if (!ODLBrain.serviceRequestedPerMaster.get(participantDataInfo.participant_name.name).get(serviceId).containsKey(vnfId)) {
							ODLBrain.serviceRequestedPerMaster.get(participantDataInfo.participant_name.name).get(serviceId).put(vnfId, vnfRequirements);
							ODLBrain.mutilRequestedVNFs++;
							System.out.println("Multi-requested VNFs: " + Integer.toString(ODLBrain.mutilRequestedVNFs));
							//System.out.println(ODLBrain.serviceRequestedPerMaster);
						}
						if (!ODLBrain.vnfRequestedtoDeploy.containsKey(vnfId) && isMultiClusterDeployment.equals("True")) {
							ODLBrain.vnfRequestedtoDeploy.put(vnfDeadline.concat(vnfId), vnfRequirements);
							//ODLBrain.setVariableareValuesvnfRequestedtoDeploy(true);
							ODLBrain.areValuesvnfRequestedtoDeploy = true;
							//System.out.println(ODLBrain.vnfRequestedtoDeploy);
							//System.out.println(ODLBrain.areValuesvnfRequestedtoDeploy);
							//System.out.println(areValuesvnfRequestedtoDeploy);
						}
	
						
						if (!sample.DestinationNodeTp.equals("0") || !sample.DestinationNodeTp.equals("-1")) {
							List<String> vnfRequirements = new ArrayList<String>();
							vnfRequirements.add(0, Integer.toString(vnfCpuRequested));
							vnfRequirements.add(1, Integer.toString(vnfRunningTime));
							vnfRequirements.add(2, vnfDeadline);
							vnfRequirements.add(3, vnfsInService);
							vnfRequirements.add(4, serviceId);
							vnfRequirements.add(5, "not deployed");
	
							if (!TutorialL2Forwarding.serviceRequestedPerMaster.containsKey(participantDataInfo.participant_name.name)) {
								TutorialL2Forwarding.serviceRequestedPerMaster.put(participantDataInfo.participant_name.name, new HashMap<String, Map<String, List<String>>>());
							} 
							if (!TutorialL2Forwarding.serviceRequestedPerMaster.get(participantDataInfo.participant_name.name).containsKey(serviceId)) {
								TutorialL2Forwarding.serviceRequestedPerMaster.get(participantDataInfo.participant_name.name).put(serviceId, new HashMap<String, List<String>>());
							}
							if (!TutorialL2Forwarding.serviceRequestedPerMaster.get(participantDataInfo.participant_name.name).get(serviceId).containsKey(vnfId)) {
								TutorialL2Forwarding.serviceRequestedPerMaster.get(participantDataInfo.participant_name.name).get(serviceId).put(vnfId, vnfRequirements);
							}
							if (!TutorialL2Forwarding.vnfRequestedtoDeploy.containsKey(vnfId) && isMultiClusterDeployment.equals("True")) {
								TutorialL2Forwarding.vnfRequestedtoDeploy.put(vnfDeadline.concat(vnfId), vnfRequirements);
							}
						}
						
					}
	
					// Reading information regarding the status of the last VNf deployed.
					if (sample.Identificador.equals("VNF_Deployed")) {
						PublicationBuiltinTopicData publicationData = new PublicationBuiltinTopicData();
	
						dataReader.get_matched_publication_data(publicationData, info.publication_handle);
	
						ParticipantBuiltinTopicData participantDataInfo = discoveredParticipants
								.get(publicationData.participant_key.toString());
						
						System.out.println("Receiving service request from: " + participantDataInfo.participant_name.name);
	
						String vnfId = sample.NodeId;
						String service = sample.TerminationPointId;
						//System.out.println(vnfId);
						//System.out.println(currentVNF);
						//System.out.println(ODLBrain.currentVNF);
						if (vnfId.equals(ODLBrain.currentVNF)) {
							isCurrentVNFDeployed.set(true); 
							ODLBrain.mutilDeployedVNFs++;
							System.out.println("Multi-deployed VNFs: " + Integer.toString(ODLBrain.mutilDeployedVNFs));
						}
						List<String> vnfInformation = ODLBrain.serviceRequestedPerMaster.get(participantDataInfo.participant_name.name).get(service).get(vnfId);
						if (vnfInformation.get(5).equals("not deployed")) {
							vnfInformation.add(5, "deployed");
							ODLBrain.serviceRequestedPerMaster.get(participantDataInfo.participant_name.name).get(service).put(vnfId, vnfInformation);
						}
						boolean serviceDeployed = ODLBrain.areAllVNFScheduled(participantDataInfo.participant_name.name, service);
						if (serviceDeployed) {
							ODLBrain.multiDeployedService++;
							System.out.println("Multi-deployed Services: " + Integer.toString(ODLBrain.multiDeployedService));
							float reward = ODLBrain.calculateReward();
							ResAlloAlgo.setCurrentReward(reward);
						}
					}
					
					//System.setOut(originalOut);
	
				} catch (RETCODE_NO_DATA noData) {
					// No more data to read
					follow = false;
				} catch (RETCODE_PRECONDITION_NOT_MET notMet) {
					PrintStream fileErr = null;
					try {
						fileErr = new PrintStream(new FileOutputStream("./err.txt", true), true);
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
					notMet.printStackTrace(fileErr);
				} catch (RETCODE_BAD_PARAMETER bp) {
					PrintStream fileErr = null;
					try {
						fileErr = new PrintStream(new FileOutputStream("./err.txt", true), true);
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
					bp.printStackTrace(fileErr);
				} catch (RETCODE_ERROR err) {
					// An error occurred
					PrintStream fileErr = null;
					try {
						fileErr = new PrintStream(new FileOutputStream("./err.txt", true), true);
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
					err.printStackTrace(fileErr);
				//} catch (FileNotFoundException e) {
				//	e.printStackTrace();
				} finally {
				}
			}
		}
	}
	*/

	/**
	 * Class to implement a listener to access to the data participants in DDS domain.
	 * Its main funcion is to monitor the register participants status.
	 */
	public static class BuiltinParticipantListener extends DataReaderAdapter {
		ParticipantBuiltinTopicDataSeq dataSeq = new ParticipantBuiltinTopicDataSeq();
		SampleInfoSeq infoSeq = new SampleInfoSeq();

		public void on_data_available(DataReader reader) {
			ParticipantBuiltinTopicDataDataReader builtin_reader = (ParticipantBuiltinTopicDataDataReader) reader;
			ParticipantBuiltinTopicData participantData = new ParticipantBuiltinTopicData();
			SampleInfo info = new SampleInfo();

			try {
				while (true) {
					builtin_reader.take_next_sample(participantData, info);

					if (info.instance_state == InstanceStateKind.ALIVE_INSTANCE_STATE) {
						
						String ipPublic = IPfromLocatorMetatraffic(participantData.metatraffic_unicast_locators);
						
						System.out
								.println("Participant (New)"
										+ " messageNum: "
										+ info.reception_sequence_number.low
										+ " name: \""
										+ participantData.participant_name.name
										+ "\""
										+ " Key " + participantData.key.toString()
										+ " created at: "
										+ info.source_timestamp
										+ " detected at: "
										+ info.reception_timestamp
										+ " ip_address: "
										+ ipPublic);
						
						byte[] ip = info.instance_handle.get_valuesI();
						String ipAddress = byteToInt(ip[0]) + "."
								+ byteToInt(ip[1]) + "." + byteToInt(ip[2])
								+ "." + byteToInt(ip[3]);
						
						List<String> ips = new ArrayList<String>();
						ips.add(0, ipAddress);
						ips.add(1, ipPublic);

						ipLocalandPublicPerController.put(
								participantData.participant_name.name, ips);
						System.out.println(ipLocalandPublicPerController);
						
						discoveredParticipants
						.put(participantData.key.toString(),
								participantData);
						
						byte[] id = info.instance_handle.get_valuesI();
						String ID = byteToInt(id[0]) + "_" + byteToInt(id[1])
								+ "_" + byteToInt(id[2]) + "_"
								+ byteToInt(id[3]) + "_" + byteToInt(id[4])
								+ "_" + byteToInt(id[5]) + "_"
								+ byteToInt(id[6]) + "_" + byteToInt(id[7]);

						keys.put(ID, participantData.key.toString());

						if (failureParticipants
								.containsKey(participantData.participant_name.name)) {
							failureParticipants
									.remove(participantData.participant_name.name);
						}

					} else {
						String dissapearReason;
						if (info.instance_state == InstanceStateKind.NOT_ALIVE_DISPOSED_INSTANCE_STATE) {
							dissapearReason = "deleted";
						} else {
							dissapearReason = "lost connection";
						}
						if (info.valid_data) {
							System.out
									.println("Participant (Dissapeared - "
											+ dissapearReason
											+ "):"
											+ " messageNum: "
											+ info.reception_sequence_number.low
											+ " name: \""
											+ participantData.participant_name.name
											+ "\""
											+ " Key "
											+ participantData.key.toString()
											+ " created at: "
											+ info.source_timestamp
											+ " detected at: "
											+ info.reception_timestamp
											+ " ip_address: "
											+ IPfromLocatorMetatraffic(participantData.metatraffic_unicast_locators));
						} else {
							
							byte[] id = info.instance_handle.get_valuesI();
							String ID = byteToInt(id[0]) + "_"
									+ byteToInt(id[1]) + "_" + byteToInt(id[2])
									+ "_" + byteToInt(id[3]) + "_"
									+ byteToInt(id[4]) + "_" + byteToInt(id[5])
									+ "_" + byteToInt(id[6]) + "_"
									+ byteToInt(id[7]);

							String key = keys.get(ID);

							ParticipantBuiltinTopicData participantDataFail = discoveredParticipants
									.get(key);

							String ipPublicControllerFail = IPfromLocatorMetatraffic(participantDataFail.metatraffic_unicast_locators);

							System.out.println("Participant (Dissapeared - "
									+ dissapearReason + "):" + " messageNum: "
									+ info.reception_sequence_number.low
									+ " name: \""
									+ participantDataFail.participant_name.name
									+ "\"" + " Key "
									+ participantDataFail.key.toString()
									+ " source sn: "
									+ info.publication_sequence_number.low
									+ " detected at: "
									+ info.reception_timestamp
									+ " ip_address: " + ipPublicControllerFail);

							failureParticipants.put(
									participantDataFail.participant_name.name,
									participantDataFail);
						}
					}
				}
			} catch (RETCODE_NO_DATA noData) {
				// catch (RETCODE_NO_DATA | UnknownHostException noData) {
				return;
			} finally {
			}
		}
	}

    /**
	 * Class to implement a listener to access to the information of the publishing instances
	 * of the DDS domain. Its main funcion is to monitor the publishers status.
	 */
	public static class BuiltinPublicationListener extends DataReaderAdapter {
		PublicationBuiltinTopicDataSeq dataSeq = new PublicationBuiltinTopicDataSeq();
		SampleInfoSeq infoSeq = new SampleInfoSeq();

		public void on_data_available(DataReader reader) {
			PublicationBuiltinTopicDataDataReader builtin_reader = (PublicationBuiltinTopicDataDataReader) reader;
			PublicationBuiltinTopicData publicationData = new PublicationBuiltinTopicData();
			SampleInfo info = new SampleInfo();

			try {
				while (true) {
					builtin_reader.take_next_sample(publicationData, info);

					if (info.instance_state == InstanceStateKind.ALIVE_INSTANCE_STATE) {
						System.out.println("DataWriter (New)" + " messageNum: "
								+ info.reception_sequence_number.low
								+ " name: \""
								+ publicationData.publication_name.name + "\""
								+ " key: "
								+ publicationData.participant_key.toString()
								+ " topic: " + publicationData.topic_name
								+ " type: " + publicationData.type_name
								+ " created at: " + info.source_timestamp
								+ " detected at: " + info.reception_timestamp
								+ " full details: "
								+ publicationData.toString());
					} else {
						String dissapearReason;
						if (info.instance_state == InstanceStateKind.NOT_ALIVE_DISPOSED_INSTANCE_STATE) {
							dissapearReason = "delected";
						} else {
							dissapearReason = "lost connection";
						}
						if (info.valid_data) {
							System.out.println("DataWriter (Dissapeared - "
									+ dissapearReason + "):" + " messageNum: "
									+ info.reception_sequence_number.low
									+ " name: \""
									+ publicationData.publication_name.name
									+ "\"" 				
									+ " key: "
									+ publicationData.participant_key
											.toString()+ " created at: "
									+ info.source_timestamp + " detected at: "
									+ info.reception_timestamp
									+ " full details: "
									+ publicationData.toString());
						} else {
							
							byte[] id = info.instance_handle.get_valuesI();
							String ID = byteToInt(id[0]) + "_"
									+ byteToInt(id[1]) + "_" + byteToInt(id[2])
									+ "_" + byteToInt(id[3]) + "_"
									+ byteToInt(id[4]) + "_" + byteToInt(id[5])
									+ "_" + byteToInt(id[6]) + "_"
									+ byteToInt(id[7]);

							String key = keys.get(ID);

							ParticipantBuiltinTopicData participantDataFail = discoveredParticipants
									.get(key);
							String ipControllerFail = IPfromLocatorMetatraffic(participantDataFail.metatraffic_unicast_locators);

							System.out.println("DataWriter (Dissapeared - "
									+ dissapearReason + "):" + " messageNum: "
									+ info.reception_sequence_number.low
									+ " name: \""
									+ participantDataFail.participant_name.name
									+ "\"" + " key: " + key + " detected at: "
									+ info.reception_timestamp
									+ " ip_address: " + ipControllerFail);
						}
					}
				}
			} catch (RETCODE_NO_DATA noData) {
				return;
			} finally {
			}
		}
	}

    /**
	 * Method to verify if all the constituent VNF of a service have been deployed
	 * @param participant_cluster master node of the cluster that request the service
	 * @param service  service name to check if its VNF have been deployed
	 */
	public static boolean areAllVNFScheduled(String participant_cluster, String service) {
		int count = 0;
		Collection<List<String>> vnfInfo = ODLBrain.serviceRequestedPerMaster.get(participant_cluster).get(service).values();
		Iterator<List<String>> vnfInfoIter = vnfInfo.iterator();
		while (vnfInfoIter.hasNext()) {
			List<String> info = vnfInfoIter.next();
			if (info.get(5).equals("deployed")) {
				count++;
			}
		}
		if (count == vnfInfo.size()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Method to indicate in which node must be deployed the current VNF.
	 * It publishes the cluster, node and VNF to the registered master nodes.
	 * 
	 * @param cluster   selected cluster to deploy vnf
	 * @param node		selected node of the cluster where the VNF is deployed
	 * @param vnfName   current VNF to deploy
	 */
	public static void notifyVNFDeployment(int cluster, int node, String vnfName, float cpuRequested, String deploymentType, String serviceName) {
		topologia VNF = new topologia();
		VNF.Identificador = "kubernetes-control-plane" + Integer.toString(cluster);
		if (ODLBrain.nodesCPUPerMaster.get(VNF.Identificador).size() == node) {
			VNF.NodeId = "kubernetes-control-plane";
		} else {
			VNF.NodeId = "kubernetes-worker" + Integer.toString(node);
		}
		if (!ODLBrain.usingNodes.containsKey(VNF.Identificador)) {
			ODLBrain.usingNodes.put(VNF.Identificador, new ArrayList<String>()); 
		}
		if (!ODLBrain.usingNodes.get(VNF.Identificador).contains(VNF.NodeId)) {
			ODLBrain.usingNodes.get(VNF.Identificador).add(VNF.NodeId);
		}

		VNF.TerminationPointId = vnfName.substring(vnfName.indexOf("vnf-"));
		VNF.DestinationNode = ownKeyName;
		VNF.SourceNode = Float.toString(cpuRequested);
		VNF.SourceNodeTp = deploymentType;
		VNF.DestinationNodeTp = serviceName;
		ODLBrain.selectedNode = VNF.NodeId;
		ODLBrain.cluster = cluster;
		ODLBrain.node = node;
		System.out.println("------------Taking action-----------");
		dataWriter_global.write(VNF, InstanceHandle_t.HANDLE_NIL);
	}

	/**
	 * Method used by the DQN algorithm to obtain the clusters' nodes information
	 * to create the current state (S). 
	 * 
	 * @param clusters  amount of managed clusters by this controller
	 * @param nodes     amount of nodes in each cluster
	 * @return          the input state in the DQN regarding CPU and SOC of each cluster node
	 */
	public static NDArray getEdgeNodesStatus(int clusters, int nodes) {

		NDManager manager = NDManager.newBaseManager();
		int indexes = clusters * nodes;

		float[] cpu_values = new float[indexes];
		float[] soc_values = new float[indexes];

		if (!ODLBrain.areValuesnodesCPUPerMaster) {
			for (int j = 0; j < indexes; j++) {
				cpu_values[j] = 0f;
			}
		} else {
			NavigableSet<String> keysCPU = ODLBrain.nodesCPUPerMaster.keySet();
			Iterator<String> iteratorCPU = keysCPU.iterator();
			int i = 0;
			while (iteratorCPU.hasNext()) {
				String next = iteratorCPU.next();
				Collection<Float> cpus = ODLBrain.nodesCPUPerMaster.get(next).values();
				Iterator<Float> cpu_iter = cpus.iterator();
				while(cpu_iter.hasNext()) {
					cpu_values[i] = cpu_iter.next();
					i++;
				}
			}
		}

		if (!ODLBrain.areValuesnodesSOCPerMaster) {
			for (int n = 0; n < indexes; n++) {
				soc_values[n] = 0f;
			}
		} else {
			NavigableSet<String> keysSOC = ODLBrain.nodesSOCPerMaster.keySet();
			Iterator<String> iteratorSOC = keysSOC.iterator();
			int m = 0;
			while (iteratorSOC.hasNext()) {
				String next = iteratorSOC.next();
				Collection<Float> socs = ODLBrain.nodesSOCPerMaster.get(next).values();
				Iterator<Float> soc_iter = socs.iterator();
				while(soc_iter.hasNext()) {
					soc_values[m] = soc_iter.next();
					m++;
				}
			}
		}

		NDArray cpu = manager.create(cpu_values);
		NDArray soc = manager.create(soc_values);
		//NDArray bw = new NDArray[indexes];
		NDArray cpu_nor = NDArrays.div(cpu, MAX_CPU_NODES);
		NDArray soc_nor = NDArrays.div(soc, MAX_SOC_NODES);

		return NDArrays.concat(new NDList(cpu_nor,soc_nor), 0);
	}

	/**
	 * Method used by the DQN algorithm to get the name of current VNF
	 * to deploy
	 * 
	 * @return   name of the current VNF to deploy
	 */
	public static String getCurrentVNF() {
		String vnf_name = "";
		if (areValuesvnfRequestedtoDeploy.get() == true) {
			if(ODLBrain.vnfRequestedtoDeploy.isEmpty() == false) {
				vnf_name = ODLBrain.vnfRequestedtoDeploy.firstKey();
				ODLBrain.currentVNF = vnf_name.substring(vnf_name.indexOf("vnf-"));
			} 
		}
		return vnf_name;
	}

	/**
	 * Method used by the DQN algorithm to get the service's name where 
	 * the current VNFs belongs to
	 * 
	 * @return   name of the associated service to the current VNF to deploy
	 */
	public static String getServiceName() {
		String service_name = "";
		if (areValuesvnfRequestedtoDeploy.get() == true) {
			if(ODLBrain.vnfRequestedtoDeploy.isEmpty() == false) {
				String key = ODLBrain.vnfRequestedtoDeploy.firstKey();
				service_name = ODLBrain.vnfRequestedtoDeploy.get(key).get(4);
				ODLBrain.currentService = service_name;
			} 
		}
		return service_name;
	}

	/**
	 * Method used by the DQN algorithm to create the input state of the 
	 * current VNF to deploy.
	 * 
	 * @return   the input state in the DQN regarding the VNF requirements
	 */
	public static NDArray getVNFRequest() {
		NDManager manager = NDManager.newBaseManager();
		float[] request = new float[4];
		if (areValuesvnfRequestedtoDeploy.get() == false) {
			for (int i = 0; i < 4; i++) {
				request[i] = 0f;
			}
		} else {
			if (ODLBrain.vnfRequestedtoDeploy.isEmpty() == false) {
				String key = ODLBrain.vnfRequestedtoDeploy.firstKey();
				List<String> vnf = ODLBrain.vnfRequestedtoDeploy.get(key);
				request[0] = Float.parseFloat(vnf.get(0)) / MAX_CPU_NODES; // CPU demand of analyzed VNF
				request[1] = Float.parseFloat(vnf.get(1)) / 100f; // Running time of analyzed VNF
				request[2] = Float.parseFloat(vnf.get(2)) / 10000f; // Deadline of analyzed VNF
				request[3] = Float.parseFloat(vnf.get(3)) / 10f;  // Pending functions of service request
				ODLBrain.vnfRequestedtoDeploy.remove(key);
			}
		}
		System.out.println("Pending VNFs: " + Integer.toString(ODLBrain.vnfRequestedtoDeploy.size()));

		NDArray vnf_request = manager.create(request);

		return vnf_request;
	}

	/**
	 * Method used by the DQN algorithm to create the node's status mask. 
	 * Thus, only available nodes are considered during the Q_max calculation.
	 * 
	 * @param clusters  amount of managed clusters by this controller
	 * @param nodes     amount of nodes in each cluster
	 * @return   the node's status mask regarding available nodes
	 */
	public static NDArray getNodesMask(int clusters, int nodes) {
		NDManager manager = NDManager.newBaseManager();
		int indexes = (clusters * nodes) + 1;
		float[] mask = new float[indexes];
		
		if (!ODLBrain.areValuesnodesAvailabilityPerMaster) {
			for (int j = 0; j < indexes; j++) {
				mask[j] = 1f;
			}
		} else {
			NavigableSet<String> keysAvai = ODLBrain.nodesAvailabilityPerMaster.keySet();
			Iterator<String> iteratorAvai = keysAvai.iterator();
			//int i = 1;
			mask[0] = 1f;
			while (iteratorAvai.hasNext()) {
				String next = iteratorAvai.next();
				//Collection<String> avail = ODLBrain.nodesAvailabilityPerMaster.get(next).values();
				//Iterator<String> avail_iter = avail.iterator();
				Set<String> keysNode = ODLBrain.nodesAvailabilityPerMaster.get(next).keySet();
				Iterator<String> iteratorNodes = keysNode.iterator();
				/** 
				while(avail_iter.hasNext()) {
					String next1 = avail_iter.next();
					if (next1.equals("True")) {
						mask[i] = 1f;
					} else {
						mask[i] = 0f;
					}
					i++;
				}
				*/
				while(iteratorNodes.hasNext()) {
					String next1 = iteratorNodes.next();
					String value = ODLBrain.nodesAvailabilityPerMaster.get(next).get(next1);

					int cluster = Integer.parseInt(next.substring(next.lastIndexOf("e") + 1));
                    int initialIndex = (cluster - 1) * nodes;

					int nodeActionSpace = 0;
					if (!next1.equals("kubernetes-control-plane")) {
						nodeActionSpace = initialIndex + Integer.parseInt(next1.substring(next1.lastIndexOf("r") + 1));
					} else {
						nodeActionSpace = initialIndex + 4;
					} 

					if (value.equals("True")) {
						mask[nodeActionSpace] = 1f;
					} else {
						mask[nodeActionSpace] = 0f;
					}
				}
			}
		}
		//System.out.println(ODLBrain.nodesAvailabilityPerMaster);

		NDArray availNodes = manager.create(mask);

		//System.out.println(availNodes);

		return availNodes;
	}

	/**
	 * Method to calculate the cost of the used resources in
	 * edge nodes where there is any VNF deployed.
	 * 
	 * @return   total cost of used resources in edge nodes
	 */
	public static float getTotalCostUsedResources() {
		float totalCost = 0f;

		NavigableSet<String> keys = ODLBrain.anyVNFInNodesPerMaster.keySet();
		Iterator<String> iter = keys.iterator();
		while (iter.hasNext()) {
			String next = iter.next();
			Set<String> keysNodes = ODLBrain.anyVNFInNodesPerMaster.get(next).keySet();
			Iterator<String> iterNodes = keysNodes.iterator();
			while (iterNodes.hasNext()) {
				String nextNode = iterNodes.next();
				if (ODLBrain.anyVNFInNodesPerMaster.get(next).get(nextNode).equals("1")) {
					float remaingCPU = ODLBrain.nodesCPUPerMaster.get(next).get(nextNode);
					float cost = usedResourcesCost * remaingCPU / MAX_CPU_NODES;
					totalCost = totalCost + cost;
				} else {
					totalCost = totalCost + 0f;
				}
			}
		}

		return totalCost;
	}

	/**
	 * Method to obtain the remaining SOC of each node in the registered clusters
	 *  
	 * @return  unit value of the lifetime of the clusters
	 */
	public static float getTotalLifeTime() {
		float totalLifetime = 0f;

		NavigableSet<String> keys = ODLBrain.nodesSOCPerMaster.keySet();
		Iterator<String> iter = keys.iterator();
		while (iter.hasNext()) {
			String next = iter.next();
			Collection<Float> socs = ODLBrain.nodesSOCPerMaster.get(next).values();
			Iterator<Float> soc_iter = socs.iterator();
			while (soc_iter.hasNext()) {
				float nextSOC = soc_iter.next() / MAX_SOC_NODES;
				totalLifetime = totalLifetime + nextSOC;
			}
		}

		return totalLifetime;
	}

	/**
	 * Method to calculate the obtained reward of deploying a service.
	 * The reward is calculated as a weighted sum of several terms
	 * (i.e., lifetime of cluster elements, deployed services and cost 
	 * of used resources) 
	 * 
	 * @return   the obtained reward of deploying a service.
	 */
	public static float calculateReward() {

		float reward = 0f;
		float masterPenalization = 0.7f;
		float overloadingNodePenalization = 1f;
		float imbalancePenalization = 1f;

		float lifetimeTerm = zeta * ODLBrain.getTotalLifeTime();
		float deployedServiceTerm = xi * ODLBrain.multiDeployedService / ODLBrain.multiRequestedService;
		float resourceCostTerm = phi * ODLBrain.getTotalCostUsedResources();

		reward = lifetimeTerm + deployedServiceTerm - resourceCostTerm;

		NDList currentObservation = ResAlloAlgo.getExtCurrentObservation();

		// Analizing overloading penalization
		if (!ODLBrain.selectedNode.equals("kubernetes-control-plane")) {
			int iniIndex = (ODLBrain.cluster - 1) * ODLBrain.numberNodes;
			int indexNode = iniIndex + ODLBrain.node;
			float capacity = currentObservation.singletonOrThrow().getFloat(indexNode);
			if (capacity > 0.875f) {//0.875f
				reward = reward - overloadingNodePenalization;
			}
		}

		// Analizing imbalance penalization
		NavigableSet<String> keys = ODLBrain.usingNodes.keySet();
		Iterator<String> iteratorKey = keys.iterator();
		List<Integer> indexNodes = new ArrayList<Integer>();
		boolean usingMultipleNodes = false;

		if (keys.size() > 1) {
			usingMultipleNodes = true;
		}

		while (iteratorKey.hasNext()) {
			String master = iteratorKey.next();
			//System.out.println(master);
			//System.out.println(master.substring(master.lastIndexOf("e") + 1));
			List<String> usedNodes = ODLBrain.usingNodes.get(master);

			if (usedNodes.size() > 1) {
				usingMultipleNodes = true; 
			}

			int cluster = Integer.parseInt(master.substring(master.lastIndexOf("e") + 1));
			int initialIndex = (cluster - 1) * ODLBrain.numberNodes;

			for (int i = 0; i < usedNodes.size(); i++) {
				String node = usedNodes.get(i);
				int index = 0;

				if (!node.equals("kubernetes-control-plane")) {
					index = initialIndex + Integer.parseInt(node.substring(node.lastIndexOf("r") + 1));
				} else {
					index = initialIndex;
				}

				if (usingMultipleNodes) {
					if (currentObservation.singletonOrThrow().getFloat(index) < 0.03f) {
						ODLBrain.usingNodes.get(master).remove(node);
						if (ODLBrain.usingNodes.get(master).isEmpty()) {
							ODLBrain.usingNodes.remove(master);
						}
					} else {
						indexNodes.add(index);
					}
				}
			}
		}
		//System.out.println(indexNodes);

		if (usingMultipleNodes) {
			List<Float> cpu = new ArrayList<Float>();
			for (int i = 0; i < indexNodes.size(); i++) {
				int index = indexNodes.get(i);
				cpu.add(currentObservation.singletonOrThrow().getFloat(index));
			}
			System.out.println(ODLBrain.usingNodes);
			System.out.println(cpu);

			float maxCPU = Collections.max(cpu);
			float minCPU = Collections.min(cpu);
			if (maxCPU - minCPU > 0.125f) {
				reward = reward - imbalancePenalization;
			}
		} else {
			System.out.println(ODLBrain.usingNodes);
		}

		// Analizing master penalization
		if (ODLBrain.selectedNode.equals("kubernetes-control-plane")) {
			reward = reward - masterPenalization;
		} 

		return reward;
	}

    public static String IPfromLocatorMetatraffic(LocatorSeq locatorseq) {

		String ip_a = "";
		for (int i = 0; i < locatorseq.size(); i++) {
			Locator_t locator = (Locator_t) locatorseq.get(i);
			if (locator.kind == 1) {
				ip_a = byteToInt(locator.address[12]) + "."
						+ byteToInt(locator.address[13]) + "."
						+ byteToInt(locator.address[14]) + "."
						+ byteToInt(locator.address[15]);
			}
		}
		return ip_a;
	}

	public static String LocatorSeq2String(LocatorSeq locatorSeq) {
		String str = "";
		if (locatorSeq.size() == 0) {
			return "";
		}

		str += "[";
		for (int i = 0; i < locatorSeq.size(); i++) {
			Locator_t locator = (Locator_t) locatorSeq.get(i);
			if (i > 0) {
				str += ", ";
			}
			str += "{ kind = " + locatorKind2String(locator) + ", address = "
					+ LocatorAddress2String(locator) + ", port = "
					+ locator.port + " }";
		}
		str += " ]";

		return str;
	}

	public static String locatorKind2String(Locator_t locator) {
		switch (locator.kind) {
		case Locator_t.KIND_SHMEM:
			return "SHMEM";
		case Locator_t.KIND_UDPv4:
			return "UDPv4";
		case Locator_t.KIND_TCPV4_LAN:
			return "TCPv4 (LAN)";
		case Locator_t.KIND_TCPV4_WAN:
			return "TCPv4 (WAN)";
		case Locator_t.KIND_DTLS:
			return "DTLS";
		case Locator_t.KIND_TLSV4_LAN:
			return "TLS (LAN)";
		case Locator_t.KIND_TLSV4_WAN:
			return "TLS (WAN)";
		case Locator_t.KIND_UDPv6:
			return "UDPv6";
		}

		String str = "" + locator.kind;
		return str;
	}

	public static String LocatorAddress2String(Locator_t locator) {
		String str = "";

		switch (locator.kind) {
		case Locator_t.KIND_UDPv4:
		case Locator_t.KIND_TCPV4_LAN:
		case Locator_t.KIND_TCPV4_WAN:
		case Locator_t.KIND_TLSV4_LAN:
		case Locator_t.KIND_TLSV4_WAN:
			str = byteToInt(locator.address[12]) + "."
					+ byteToInt(locator.address[13]) + "."
					+ byteToInt(locator.address[14]) + "."
					+ byteToInt(locator.address[15]);
			break;
		case Locator_t.KIND_UDPv6:
			for (int i = 0; i < 16; i += 4) {
				str += Integer.toHexString(byteToInt(locator.address[i]))
						+ Integer
								.toHexString(byteToInt(locator.address[i + 1]))
						+ Integer
								.toHexString(byteToInt(locator.address[i + 2]))
						+ Integer
								.toHexString(byteToInt(locator.address[i + 3]))
						+ ":";
			}
			break;
		case Locator_t.KIND_SHMEM:
			str = "<not applicable>";
		}

		return str;
	}

	private static int byteToInt(byte b) {
		if (b < 0) {
			return b + 256;
		} else
			return b;
	}

}