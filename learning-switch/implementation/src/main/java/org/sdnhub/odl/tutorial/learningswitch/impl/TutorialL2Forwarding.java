/*
 * Copyright (C) 2015 SDN Hub

 Licensed under the GNU GENERAL PUBLIC LICENSE, Version 3.
 You may not use this file except in compliance with this License.
 You may obtain a copy of the License at

    http://www.gnu.org/licenses/gpl-3.0.txt

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 implied.

 *
 */

package org.sdnhub.odl.tutorial.learningswitch.impl;

//import java.io.BufferedInputStream;
//import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStreamReader;
import java.io.PrintStream;
//import java.net.InetAddress;
//import java.net.UnknownHostException;
import java.nio.ByteBuffer;
//import java.nio.file.WatchEvent.Kind;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.io.IOException;
import java.io.FileReader;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
//import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInputBuilder;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
//import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.sdnhub.odl.tutorial.utils.GenericTransactionUtils;
import org.sdnhub.odl.tutorial.utils.PacketParsingUtils;
import org.sdnhub.odl.tutorial.utils.inventory.InventoryUtils;
import org.sdnhub.odl.tutorial.utils.openflow13.MatchUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.tokens.TagTuple;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.LinkId;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Source;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
//import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.Link;
//import org.opendaylight.openflowplugin.api.openflow.*;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.openflowplugin.api.types.rev150327.OfpRole;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.openflow.common.config.impl.rev140326.OfpRole; //openflow-impl
//import org.opendaylight.openflowplugin.openflow.*;
//import org.opendaylight.openflowplugin.openflow.md.core.sal.OpenflowPluginProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.LinkDiscoveredBuilder;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.LinkDiscovered;

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
//import com.rti.dds.subscription.DataReaderListener;
import com.rti.dds.subscription.DataReaderQos;
import com.rti.dds.subscription.InstanceStateKind;
//import com.rti.dds.subscription.LivelinessChangedStatus;
import com.rti.dds.subscription.SampleInfo;
import com.rti.dds.subscription.Subscriber;
import com.rti.dds.topic.Topic;
//import com.rti.dds.type.builtin.StringDataWriter;
//import com.rti.dds.type.builtin.StringDataReader;
//import com.rti.dds.type.builtin.StringTypeSupport;
import com.rti.dds.publication.builtin.*;
//import com.rti.dds.subscription.builtin.*;
import com.rti.dds.subscription.*;
import com.rti.dds.infrastructure.*;
import com.rti.dds.domain.*;
import com.rti.dds.domain.builtin.*;
//import com.rti.dds.dynamicdata.DynamicData;
//import com.rti.dds.dynamicdata.DynamicDataReader;
//import com.rlresallo.*;
//import static org.sdnhub.odl.tutorial.learningswitch.impl.TrainResAlloAlgo.REPLAY_BUFFER_SIZE;
import ai.djl.ndarray.NDManager;
import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;

//import org.sdnhub.odl.tutorial.learningswitch.impl.*;

//import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;


public class TutorialL2Forwarding extends DataReaderAdapter implements
		AutoCloseable, PacketProcessingListener {
	private final Logger LOG = LoggerFactory.getLogger(this.getClass());
	// private final static long FLOOD_PORT_NUMBER = 0xfffffffbL;
	private static final String kubeConfigPath = System.getenv("HOME") + "/.kube/config";
	private static final long MAX_CAPACITY_AC = 500L; //Maximum capacity in Area Controllers
	private static final float MAX_CPU_NODES = 4000f;
	private static final float MAX_SOC_NODES = 100f;
	private static final float usedResourcesCost = 0.2f;
	private static final float zeta = 0.3f; // adjustable positive weight lifetime term
	private static final float xi = 0.3f; // adjustable positive weight deployed service term
	private static final float phi = 0.3f; // adjustable positive weight used resource cost term
	private static int deployedService = 0;
	private static String currentVNF = "";
	public static boolean isCurrentVNFDeployed;
	private static long noPacket = 0; // For debugging
	// private int index;
	// private int vez=0;
	private int alertWithinMs = 5;

	private Map<String, Map<String, String>> macTablePerSwitch = new HashMap<String, Map<String, String>>();
	// private static Map<String, List<String>> switchesPerController = new HashMap<String, List<String>>();
	private static Map<String, List<String>> ipLocalandPublicPerController = new HashMap<String, List<String>>();
	private static Map<String, Map<String, Float>> nodesLoadPerAC = new HashMap<String, Map<String, Float>>();
	private static ConcurrentSkipListMap<String, Map<String, Float>> nodesSOCPerMaster; //Concurrent dictionary with master_node of cluster as main key; the values are another dictionary with nodeName as key and SOC of node as value.
	private static ConcurrentSkipListMap<String, Map<String, Integer>> nodesCPUPerMaster; //Concurrent dictionary with master_node of cluster as main key; the values are another dictionary with nodeName as key and CPU of node as value.
	private static ConcurrentSkipListMap<String, Map<String, Boolean>> anyVNFInNodesPerMaster; //Concurrent dictionary with master_node of cluster as main key; the values are another dictionary with nodeName as key and boolean value indicating if there is any VNF deployed.
	private static ConcurrentSkipListMap<String, Map<String, Map<String, List<String>>>> serviceRequestedPerMaster;
	private static ConcurrentSkipListMap<String, List<String>> vnfRequestedtoDeploy;
	private static Map<String, Map<String, List<String>>> switchesACPerGC = new HashMap<String, Map<String, List<String>>>();
	private static Map<String, Long> loadPerController = new HashMap<String, Long>();
	private static Map<String, Double> arrivalRatePerController = new HashMap<String, Double>();
	private static Map<String, Map<String, Long>> packetInNodesPerAC = new HashMap<String, Map<String, Long>>();
	private static Map<String, Map<String, Long>> deltaTimeArrivalPacketNodesPerAC = new HashMap<String, Map<String, Long>>();
	private static ConcurrentSkipListMap<String, ParticipantBuiltinTopicData> discoveredParticipants;
	private static ConcurrentSkipListMap<String, ParticipantBuiltinTopicData> failureParticipants;
	private static ConcurrentSkipListMap<String, String> keys;
	// private String keyipController = "";
	// private String keyipLocalController = "";
	// private String keyipPublicController = "";
	// private String keynameGC = "";
	private static String ownKeyName = "GC_UPC_1";

	// Members specific to this class
	// private Map<String, NodeConnectorId> macTable = new HashMap<String,
	// NodeConnectorId>();
	private String function = "learning";

	// Members related to MD-SAL operations
	private List<Registration> registrations;
	private DataBroker dataBroker;
	// private PacketProcessingService packetProcessingService;
	private NotificationProviderService notifService;
	DomainParticipant participant = null;
	private Topic topic = null;
	private static Publisher publisher_local = null;
	private static Publisher publisher_global = null;
	private static Subscriber subscriber_local = null;
	private static Subscriber subscriber_global = null;
	private static topologiaDataWriter dataWriter_global = null;
	private static topologiaDataWriter dataWriter_local = null;
	private static topologiaDataReader dataReader_global = null;
	private static topologiaDataReader dataReader_local = null;
	// private int triggerMask_global = 0;
	// private int triggerMask_local = 0;
	private ParticipantBuiltinTopicDataDataReader participantsDR;
	private PublicationBuiltinTopicDataDataReader publicationsDR;
	private static String id = "gc_upc_1";
	
	private static TrainResAlloAlgo trainAlgo = null;
	private static CoreV1Api v1 = null;
	private static ApiClient client = null;

	int sampleCount = 0;

	/**
	 * Constructs an L2Forwarding mechanism, initializes the DDS mechanism to exchange information
	 * between controllers and edge nodes. Additionally, instantiates the training class of DQN algorithm. 
	 * 
	 * @param dataBroker           data broker for reading/writing from inventory store
	 * @param notificationService  object for receiving notifications when existing PACKET_INs
	 * @param rpcProviderRegistry  get access to the packet processing service for making RPC calls
	 */

	public TutorialL2Forwarding(DataBroker dataBroker,
			NotificationProviderService notificationService,
			RpcProviderRegistry rpcProviderRegistry) {
		// Store the data broker for reading/writing from inventory store
		this.dataBroker = dataBroker;

		// Get access to the packet processing service for making RPC calls
		// later
		//this.packetProcessingService = rpcProviderRegistry.getRpcService(PacketProcessingService.class);

		// List used to track notification (both data change and YANG-defined)
		// listener registrations
		this.registrations = Lists.newArrayList();

		this.notifService = notificationService;

		// Register this object for receiving notifications when there are PACKET_INs
		registrations.add(notificationService
				.registerNotificationListener(this));

		// Authentication and client configuration for Kubernetes clusters
		try {
			client = ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(new FileReader(kubeConfigPath))).build();
			Configuration.setDefaultApiClient(client);
			v1 = new CoreV1Api();
		} catch (IOException io) {
			System.out.println(io);
		}
		
		int noClusters = 2;
		int noNodes = 4;
		int batchSize = 32;
		boolean preTrained = false;
		boolean testing = false;

		trainAlgo = new TrainResAlloAlgo(noClusters, noNodes, batchSize, preTrained, testing);
		try {
			Model model = trainAlgo.createOrLoadModel(preTrained);
			if (testing) {
				trainAlgo.test(model);
			} else {
				trainAlgo.train(batchSize, preTrained, testing, model, noClusters, noNodes);
			}
		} catch (IOException io) {
			System.out.println(io);
		} catch (MalformedModelException me) {
			System.out.println(me);
		}
		
		discoveredParticipants = new ConcurrentSkipListMap<String, ParticipantBuiltinTopicData>();
		failureParticipants = new ConcurrentSkipListMap<String, ParticipantBuiltinTopicData>();
		keys = new ConcurrentSkipListMap<String, String>();
		nodesSOCPerMaster = new ConcurrentSkipListMap<String, Map<String, Float>>();
		nodesCPUPerMaster = new ConcurrentSkipListMap<String, Map<String, Integer>>();
		anyVNFInNodesPerMaster = new ConcurrentSkipListMap<String, Map<String, Boolean>>();
		serviceRequestedPerMaster = new ConcurrentSkipListMap<String, Map<String, Map<String, List<String>>>>();
		vnfRequestedtoDeploy = new ConcurrentSkipListMap<String, List<String>>();
		isCurrentVNFDeployed = false;

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
			pQos.discovery.initial_peers.setMaximum(6);
			pQos.discovery.initial_peers.add("239.255.0.1");
			pQos.discovery.initial_peers.add("6@builtin.udpv4://127.0.0.1");
			pQos.discovery.initial_peers.add("6@builtin.udpv4://147.83.113.39");
			pQos.discovery.initial_peers.add("6@builtin.udpv4://147.83.118.147");
			pQos.discovery.initial_peers.add("6@builtin.shmem://");
			pQos.discovery.multicast_receive_addresses.clear();
			pQos.discovery.multicast_receive_addresses.setMaximum(2);
			pQos.discovery.multicast_receive_addresses.add("239.255.0.1");
			pQos.participant_name.name = "GC_UPC_1";
			pQos.user_data.value.addAllByte(id.getBytes());

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
			transportProperty.public_address = "172.28.26.115";
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

		topic = participant.create_topic("topologia", typeName,
				DomainParticipant.TOPIC_QOS_DEFAULT, null, // listener
				StatusKind.STATUS_MASK_NONE);
		if (topic == null) {
			System.err.println("Unable to create topic.");
			return;
		}

		// Getting the default PublisherQoS and adding a partition name

		PublisherQos pub_qos_local = new PublisherQos();
		participant.get_default_publisher_qos(pub_qos_local);
		pub_qos_local.partition.name.clear();
		pub_qos_local.partition.name.add("mainupc");
		pub_qos_local.partition.name.add("backup");
		
		PublisherQos pub_qos_global = new PublisherQos();
		participant.get_default_publisher_qos(pub_qos_global);
		pub_qos_global.partition.name.clear();
		pub_qos_global.partition.name.add("global");

		// Publisher for communication GC-AC

		publisher_local = participant.create_publisher(pub_qos_local, null,
				StatusKind.STATUS_MASK_NONE);

		if (publisher_local == null) {
			System.err.println("Unable to create publisher\n");
			return;
		}
		
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

		dataWriter_local = (topologiaDataWriter) publisher_local.create_datawriter(
				topic, dwqos, null, // listener
				StatusKind.STATUS_MASK_NONE);
		if (dataWriter_local == null) {
			System.err.println("Unable to create data writer\n");
			return;
		}
		
		InstanceHandle_t instancelocal_l = dataWriter_local.get_instance_handle();
		participant.ignore_publication(instancelocal_l);

		// Getting the default SubscriberQoS and adding a partition name
		
		SubscriberQos sub_qos_local = new SubscriberQos();
		participant.get_default_subscriber_qos(sub_qos_local);
		sub_qos_local.partition.name.clear();
		sub_qos_local.partition.name.add("mainupc");
		sub_qos_local.partition.name.add("backup");
		
		SubscriberQos sub_qos_global = new SubscriberQos();
		participant.get_default_subscriber_qos(sub_qos_global);
		sub_qos_global.partition.name.clear();
		sub_qos_global.partition.name.add("global");
		
		// Subscriber for communication GC-GC
		
		subscriber_global = participant.create_subscriber(sub_qos_global, null, StatusKind.STATUS_MASK_NONE);
				
		if (subscriber_global == null) {
			System.err.println("Unable to create subscriber\n");
			return;
		}

		// Subscriber for communication GC-AC
		
		subscriber_local = participant.create_subscriber(sub_qos_local, null,
				StatusKind.STATUS_MASK_NONE);

		if (subscriber_local == null) {
			System.err.println("Unable to create subscriber\n");
			return;
		}

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

		// DataReaderListener myListener = new TutorialL2Forwarding(dataBroker);

		// DataReader for communication GC-GC
		
		dataReader_global = (topologiaDataReader) subscriber_global
				.create_datareader(topic, drqos, null, // Listener
						StatusKind.STATUS_MASK_NONE);
		if (dataReader_global == null) {
			System.err.println("Unable to create DDS Data Reader");
			return;
		}

		// DataReader for communication GC-AC

		dataReader_local = (topologiaDataReader) subscriber_local.create_datareader(
				topic, drqos, null,// listener
				StatusKind.STATUS_MASK_NONE);
		if (dataReader_local == null) {
			System.err.println("Unable to create DDS Data Reader");
			return;
		}

		// Configuring the reader conditions using StatusCondition and WaitSet
		
		WaitSet waitset = new WaitSet();
		
		StatusCondition status_condition_global = dataReader_global
				.get_statuscondition();
		if (status_condition_global == null) {
			System.err.println("get_statuscondition error\n");
			return;
		}

		StatusCondition status_condition_local = dataReader_local
				.get_statuscondition();
		if (status_condition_local == null) {
			System.err.println("get_statuscondition error\n");
			return;
		}
		
		status_condition_global
				.set_enabled_statuses(StatusKind.DATA_AVAILABLE_STATUS);
		status_condition_local
				.set_enabled_statuses(StatusKind.DATA_AVAILABLE_STATUS);

		waitset.attach_condition(status_condition_global);
		waitset.attach_condition(status_condition_local);

		final long receivedata = 1;

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

				if (active_condition_seq.get(i) == status_condition_local) {
					int triggerMask_local = dataReader_local
							.get_status_changes();
					// Data available
					if ((triggerMask_local & StatusKind.DATA_AVAILABLE_STATUS) != 0) {
						on_data_available_local();
					}
				}
			}
			try {
				Thread.sleep(receivedata * 1000);
			} catch (InterruptedException ix) {
				System.err.println("INTERRUPTED");
				break;
			}
		}
	}

	/**
	 * Method to read any subscribed information between GCs and EdgeNodes. 
	 * The kind of information is announced through the Identificador field
	 * of the topologia topic.
	 */
	private void on_data_available_global() {

		SampleInfo info = new SampleInfo();
		topologia sample = new topologia();
		
		boolean follow = true;
		while (follow) {
			try {
				dataReader_global.take_next_sample(sample, info);
				
				PrintStream originalOut = System.out;
				
				PrintStream fileOut = new PrintStream(new FileOutputStream("./out.txt", true), true);
				
				System.setOut(fileOut);
				/*
				 * if (info.valid_data) {
				 * 
				 * PublicationBuiltinTopicData publicationData = new
				 * PublicationBuiltinTopicData();
				 * 
				 * dataReader_global.get_matched_publication_data(
				 * publicationData, info.publication_handle);
				 * 
				 * ParticipantBuiltinTopicData participantDataInfo =
				 * discoveredParticipants
				 * .get(publicationData.participant_key.toString());
				 * 
				 * keynameGC = participantDataInfo.participant_name.name; }
				 */
				if (sample.Identificador.endsWith("Node")) {
					
					PublicationBuiltinTopicData publicationData = new PublicationBuiltinTopicData();

					dataReader_global.get_matched_publication_data(
							publicationData, info.publication_handle);

					ParticipantBuiltinTopicData participantDataInfo = discoveredParticipants
							.get(publicationData.participant_key.toString());

					/*
					byte[] id = info.instance_handle.get_valuesI();
					String ID = byteToInt(id[0]) + "_"
							+ byteToInt(id[1]) + "_" + byteToInt(id[2])
							+ "_" + byteToInt(id[3]) + "_"
							+ byteToInt(id[4]) + "_" + byteToInt(id[5])
							+ "_" + byteToInt(id[6]) + "_"
							+ byteToInt(id[7]);

					String keyC = keys.get(ID);

					ParticipantBuiltinTopicData participantDataInfo = discoveredParticipants
							.get(keyC);
					*/
					
					String keynameGC = participantDataInfo.participant_name.name;

					NodeBuilder node = new NodeBuilder();
					NodeId nodeId = new NodeId(sample.NodeId);
					node.setId(nodeId);
					NodeKey key = new NodeKey(new NodeId(nodeId));
					node.setKey(key);

					InstanceIdentifier<Node> instanceIdentifier = InstanceIdentifier
							.builder(Nodes.class)
							.child(Node.class, new NodeKey(nodeId)).build();
					GenericTransactionUtils.writeData(dataBroker,
							LogicalDatastoreType.OPERATIONAL,
							instanceIdentifier, node.build(), true);

					originalOut.println("Node: " + nodeId.getValue().toString()
							+ " controlled by: "
							+ sample.Identificador.substring(0, 7)
							+ " belonging to: " + keynameGC
							+ " has been introduced in Data Store");

					String nodeid = nodeId.getValue().toString();
					String switchId = TranslateFormatControllerId(nodeid);

					if (!TutorialL2Forwarding.switchesACPerGC
							.containsKey(keynameGC)) {
						TutorialL2Forwarding.switchesACPerGC.put(keynameGC,
								new HashMap<String, List<String>>());
					}
					if (!TutorialL2Forwarding.switchesACPerGC.get(keynameGC)
							.containsKey(sample.Identificador.substring(0, 7))) {
						TutorialL2Forwarding.switchesACPerGC.get(keynameGC)
								.put(sample.Identificador.substring(0, 7),
										new ArrayList<String>());
					}
					if (!TutorialL2Forwarding.switchesACPerGC.get(keynameGC)
							.get(sample.Identificador.substring(0, 7))
							.contains(switchId)) {
						TutorialL2Forwarding.switchesACPerGC.get(keynameGC)
								.get(sample.Identificador.substring(0, 7))
								.add(switchId);
						originalOut.println(switchesACPerGC);
						System.out.println(switchesACPerGC);
					}
				}

				if (sample.Identificador.equals("TerminationPoint")) {
					
					PublicationBuiltinTopicData publicationData = new PublicationBuiltinTopicData();

					dataReader_global.get_matched_publication_data(
							publicationData, info.publication_handle);

					ParticipantBuiltinTopicData participantDataInfo = discoveredParticipants
							.get(publicationData.participant_key.toString());
					/*
					byte[] id = info.instance_handle.get_valuesI();
					String ID = byteToInt(id[0]) + "_"
							+ byteToInt(id[1]) + "_" + byteToInt(id[2])
							+ "_" + byteToInt(id[3]) + "_"
							+ byteToInt(id[4]) + "_" + byteToInt(id[5])
							+ "_" + byteToInt(id[6]) + "_"
							+ byteToInt(id[7]);

					String keyC = keys.get(ID);
		
					ParticipantBuiltinTopicData participantDataInfo = discoveredParticipants
							.get(keyC);
					*/
					
					String keynameGC = participantDataInfo.participant_name.name;

					NodeConnectorBuilder nodeConnector = new NodeConnectorBuilder();
					//NodeId nodeId = new NodeId(sample.NodeId);
					NodeConnectorId nodeConnectorId = new NodeConnectorId(
							sample.TerminationPointId);
					NodeConnectorKey key = new NodeConnectorKey(
							new NodeConnectorId(nodeConnectorId));
					nodeConnector.setId(nodeConnectorId);
					nodeConnector.setKey(key);

					InstanceIdentifier<NodeConnector> instanceIdentifier2 = InstanceIdentifier
							.builder(Nodes.class)
							.child(Node.class,
									new NodeKey(new NodeId(sample.NodeId)))
							.child(NodeConnector.class,
									new NodeConnectorKey(new NodeConnectorId(
											sample.TerminationPointId)))
							.build();
					GenericTransactionUtils.writeData(dataBroker,
							LogicalDatastoreType.OPERATIONAL,
							instanceIdentifier2, nodeConnector.build(), true);

					originalOut.println("TerminationPoint: "
							+ nodeConnectorId.getValue().toString() + " from: "
							+ keynameGC + " has been introduced in Data Store");
				}

				if (sample.Identificador.equals("Link")) {
					
					PublicationBuiltinTopicData publicationData = new PublicationBuiltinTopicData();

					dataReader_global.get_matched_publication_data(
							publicationData, info.publication_handle);

					ParticipantBuiltinTopicData participantDataInfo = discoveredParticipants
							.get(publicationData.participant_key.toString());

					/*
					byte[] id = info.instance_handle.get_valuesI();
					String ID = byteToInt(id[0]) + "_"
							+ byteToInt(id[1]) + "_" + byteToInt(id[2])
							+ "_" + byteToInt(id[3]) + "_"
							+ byteToInt(id[4]) + "_" + byteToInt(id[5])
							+ "_" + byteToInt(id[6]) + "_"
							+ byteToInt(id[7]);

					String keyC = keys.get(ID);

					ParticipantBuiltinTopicData participantDataInfo = discoveredParticipants
							.get(keyC);
					*/
					
					String keynameGC = participantDataInfo.participant_name.name;

					originalOut.println("Receiving link information from: "
							+ keynameGC);

					//LinkId linkid = new LinkId(sample.LinkId);
					NodeId localNodeId = new NodeId(sample.SourceNode);
					NodeConnectorId localNodeConnectorId = new NodeConnectorId(
							sample.SourceNodeTp);
					NodeId remoteNodeId = new NodeId(sample.DestinationNode);
					NodeConnectorId remoteNodeConnectorId = new NodeConnectorId(
							sample.DestinationNodeTp);

					// LinkBuilder link = new LinkBuilder();
					// org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId
					// linkId = new
					// org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId(sample.LinkId);
					// LinkKey linkkey = new LinkKey(linkId);
					// link.setLinkId(linkId);
					// link.setKey(linkkey);
					// SourceBuilder source = new SourceBuilder();
					// source.setSourceNode(localNodeId);
					// source.setSourceTp(localNodeConnectorId);
					// DestinationBuilder destination = new
					// DestinationBuilder();
					// destination.setDestNode(remoteNodeId);
					// destination.setDestTp(remoteNodeConnectorId);
					// link.setSource(source);
					// link.setDestination(destination);

					@SuppressWarnings("deprecation")
					InstanceIdentifier<NodeConnector> localInstanceIdentifier = InstanceIdentifier
							.builder(Nodes.class)
							.child(Node.class, new NodeKey(localNodeId))
							.child(NodeConnector.class,
									new NodeConnectorKey(localNodeConnectorId))
							.toInstance();
					NodeConnectorRef localNodeConnectorRef = new NodeConnectorRef(
							localInstanceIdentifier);

					@SuppressWarnings("deprecation")
					InstanceIdentifier<NodeConnector> remoteInstanceIdentifier = InstanceIdentifier
							.builder(Nodes.class)
							.child(Node.class, new NodeKey(remoteNodeId))
							.child(NodeConnector.class,
									new NodeConnectorKey(remoteNodeConnectorId))
							.toInstance();
					NodeConnectorRef remoteNodeConnectorRef = new NodeConnectorRef(
							remoteInstanceIdentifier);

					// InstanceIdentifier<LinkDiscovered> InstanceIdentifier
					// =
					// InstanceIdentifier.builder(Lin.class).toInstance();
					// Falta crear el link y meterlo en la base de datos

					LinkDiscoveredBuilder ldb = new LinkDiscoveredBuilder();
					ldb.setSource(localNodeConnectorRef);
					ldb.setDestination(remoteNodeConnectorRef);

					// LinkDiscovered link = ldb.build();
					// @SuppressWarnings("unchecked")
					// InstanceIdentifier<LinkDiscovered> instanceIdentifier
					// = InstanceIdentifier.create((LinkDiscovered.class));

					TutorialL2Forwarding.this.notifService.publish(ldb.build());

					originalOut
							.println("Link has been introduced in Data Store");
					// GenericTransactionUtils.writeData(dataBroker,
					// LogicalDatastoreType.OPERATIONAL,
					// instanceIdentifier,ldb.build(),true);

				}

				if (sample.Identificador.equals("Flow")) {
					
					PublicationBuiltinTopicData publicationData = new PublicationBuiltinTopicData();

					dataReader_global.get_matched_publication_data(
							publicationData, info.publication_handle);

					ParticipantBuiltinTopicData participantDataInfo = discoveredParticipants
							.get(publicationData.participant_key.toString());

					/*	
					byte[] id = info.instance_handle.get_valuesI();
					String ID = byteToInt(id[0]) + "_"
							+ byteToInt(id[1]) + "_" + byteToInt(id[2])
							+ "_" + byteToInt(id[3]) + "_"
							+ byteToInt(id[4]) + "_" + byteToInt(id[5])
							+ "_" + byteToInt(id[6]) + "_"
							+ byteToInt(id[7]);

					String keyC = keys.get(ID);

					ParticipantBuiltinTopicData participantDataInfo = discoveredParticipants
							.get(keyC);
					*/
					
					String keynameGC = participantDataInfo.participant_name.name;

					originalOut.println("Receiving flow information from: "
							+ keynameGC);
					NodeId nodeId = new NodeId(sample.NodeId);
					String srcMac = sample.SourceNode;
					String dstMac = sample.DestinationNode;
					NodeConnectorId ingressNodeConnectorId = new NodeConnectorId(
							sample.SourceNodeTp);

					// Create a table for this switch if it does not exist
					if (!this.macTablePerSwitch.containsKey(sample.NodeId)) {
						this.macTablePerSwitch.put(sample.NodeId,
								new HashMap<String, String>());
					}

					// Learn source MAC address (2.2)
					String previousSrcMacPortStr = this.macTablePerSwitch.get(
							sample.NodeId).get(srcMac);
					if ((previousSrcMacPortStr == null)
							|| (!sample.SourceNodeTp
									.equals(previousSrcMacPortStr))) {
						this.macTablePerSwitch.get(sample.NodeId).put(srcMac,
								sample.SourceNodeTp);
					}

					String egressNodeConnectorIdStr = this.macTablePerSwitch
							.get(sample.NodeId).get(dstMac);
					NodeConnectorId egressNodeConnectorId = null;
					if (egressNodeConnectorIdStr != null) {
						// Entry found (2.3.1)
						egressNodeConnectorId = new NodeConnectorId(
								egressNodeConnectorIdStr);
						// Perform FLOW_MOD (2.3.1.1)
						programL2FlowOperational(nodeId, dstMac,
								ingressNodeConnectorId, egressNodeConnectorId);
						originalOut
								.println("Flow has been introduced in Data Store");
					}

				}

				if (sample.Identificador.endsWith("LoadTotal")) {
					
					PublicationBuiltinTopicData publicationData = new PublicationBuiltinTopicData();

					dataReader_global.get_matched_publication_data(
							publicationData, info.publication_handle);

					ParticipantBuiltinTopicData participantDataInfo = discoveredParticipants
							.get(publicationData.participant_key.toString());

					/*	
					byte[] id = info.instance_handle.get_valuesI();
					String ID = byteToInt(id[0]) + "_"
							+ byteToInt(id[1]) + "_" + byteToInt(id[2])
							+ "_" + byteToInt(id[3]) + "_"
							+ byteToInt(id[4]) + "_" + byteToInt(id[5])
							+ "_" + byteToInt(id[6]) + "_"
							+ byteToInt(id[7]);

					String keyC = keys.get(ID);

					ParticipantBuiltinTopicData participantDataInfo = discoveredParticipants
							.get(keyC);
					*/
					
					String keynameGC = participantDataInfo.participant_name.name;

					System.out
							.println("Receiving overall load information from: "
									+ keynameGC);

					long loadT = Long.parseLong(sample.NodeId);
					double arrivalRate = Double
							.parseDouble(sample.TerminationPointId);

					System.out.println("Controller: "
							+ sample.Identificador.substring(0, 7)
							+ " has Load: " + loadT + " and Arrival Rate: "
							+ arrivalRate);

					TutorialL2Forwarding.loadPerController.put(sample.LinkId,
							loadT);

					System.out.println(loadPerController);

					TutorialL2Forwarding.arrivalRatePerController.put(
							sample.LinkId, arrivalRate);

					System.out.println(arrivalRatePerController);
				}

				if (sample.Identificador.endsWith("LoadNodesPerAC")) {
					
					PublicationBuiltinTopicData publicationData = new PublicationBuiltinTopicData();

					dataReader_global.get_matched_publication_data(
							publicationData, info.publication_handle);

					ParticipantBuiltinTopicData participantDataInfo = discoveredParticipants
							.get(publicationData.participant_key.toString());
					
					/*
					byte[] id = info.instance_handle.get_valuesI();
					String ID = byteToInt(id[0]) + "_"
							+ byteToInt(id[1]) + "_" + byteToInt(id[2])
							+ "_" + byteToInt(id[3]) + "_"
							+ byteToInt(id[4]) + "_" + byteToInt(id[5])
							+ "_" + byteToInt(id[6]) + "_"
							+ byteToInt(id[7]);

					String keyC = keys.get(ID);

					ParticipantBuiltinTopicData participantDataInfo = discoveredParticipants
							.get(keyC);
					*/
					
					String keynameGC = participantDataInfo.participant_name.name;

					System.out.println("Receiving load information from: "
							+ keynameGC);

					NodeId nodeId = new NodeId(sample.NodeId);
					String nodeid = nodeId.getValue().toString();
					String switchId = TranslateFormatControllerId(nodeid);
					float arrivalRateNode = Float
							.parseFloat(sample.TerminationPointId);

					if (!TutorialL2Forwarding.nodesLoadPerAC
							.containsKey(sample.Identificador.substring(0, 7))) {
						TutorialL2Forwarding.nodesLoadPerAC.put(
								sample.Identificador.substring(0, 7),
								new HashMap<String, Float>());
					} else {
						TutorialL2Forwarding.nodesLoadPerAC.get(
								sample.Identificador.substring(0, 7)).put(
								switchId, arrivalRateNode);
						System.out.println(nodesLoadPerAC);
					}

				}

				// Reading remaining SOC of any node belonging to registered clusters
				if (sample.Identificador.equals("SoC")) {

					PublicationBuiltinTopicData publicationData = new PublicationBuiltinTopicData();

					dataReader_global.get_matched_publication_data(publicationData, info.publication_handle);

					ParticipantBuiltinTopicData participantDataInfo = discoveredParticipants
							.get(publicationData.participant_key.toString());

					// String keyIPPublicMaster = IPfromLocatorMetatraffic(participantDataInfo.metatraffic_unicast_locators);

					System.out.println("Receiving SOC per node information from: "
							+ participantDataInfo.participant_name.name);

					//NodeId nodeId = new NodeId(sample.NodeId);
					float soc = Float.parseFloat(sample.TerminationPointId);
					String k8snodeId = sample.NodeId;

					//String nodeid = nodeId.getValue().toString();
					//String k8snodeId = TranslateFormatControllerId(nodeid);

					if (!TutorialL2Forwarding.nodesSOCPerMaster.containsKey(participantDataInfo.participant_name.name)) {
						TutorialL2Forwarding.nodesSOCPerMaster.put(participantDataInfo.participant_name.name, new HashMap<String, Float>());
					} else {
						TutorialL2Forwarding.nodesSOCPerMaster.get(participantDataInfo.participant_name.name).put(k8snodeId, soc);
						System.out.println(nodesSOCPerMaster);
					}
				}

				// Reading remaining CPU of any node belonging to registered clusters
				if (sample.Identificador.equals("CPU")) {

					PublicationBuiltinTopicData publicationData = new PublicationBuiltinTopicData();

					dataReader_global.get_matched_publication_data(publicationData, info.publication_handle);

					ParticipantBuiltinTopicData participantDataInfo = discoveredParticipants
							.get(publicationData.participant_key.toString());

					// String keyIPPublicMaster = IPfromLocatorMetatraffic(participantDataInfo.metatraffic_unicast_locators);

					System.out.println("Receiving CPU per node information from: "
							+ participantDataInfo.participant_name.name);

					//NodeId nodeId = new NodeId(sample.NodeId);
					int cpu = Integer.parseInt(sample.TerminationPointId);
					String k8snodeId = sample.NodeId;
					boolean anyVNFRunning = Boolean.parseBoolean(sample.LinkId);

					//String nodeid = nodeId.getValue().toString();
					//String k8snodeId = TranslateFormatControllerId(nodeid);

					if (!TutorialL2Forwarding.nodesCPUPerMaster.containsKey(participantDataInfo.participant_name.name)) {
						TutorialL2Forwarding.nodesCPUPerMaster.put(participantDataInfo.participant_name.name, new HashMap<String, Integer>());
					} else {
						TutorialL2Forwarding.nodesCPUPerMaster.get(participantDataInfo.participant_name.name).put(k8snodeId, cpu);
						System.out.println(nodesCPUPerMaster);
					}

					if (!TutorialL2Forwarding.anyVNFInNodesPerMaster.containsKey(participantDataInfo.participant_name.name)) {
						TutorialL2Forwarding.anyVNFInNodesPerMaster.put(participantDataInfo.participant_name.name, new HashMap<String, Boolean>());
					} else {
						TutorialL2Forwarding.anyVNFInNodesPerMaster.get(participantDataInfo.participant_name.name).put(k8snodeId, anyVNFRunning);
						System.out.println(anyVNFInNodesPerMaster);
					}
				}	

				// Reading service request with the current VNF to deploy, its requirements and remaining VNFs to deploy.
				if (sample.Identificador.startsWith("serv-")) {
					String vnfId = "";
					int vnfCpuRequested = 0;
					int vnfRunningTime = 0;
					String vnfsInService = "";
					PublicationBuiltinTopicData publicationData = new PublicationBuiltinTopicData();

					dataReader_global.get_matched_publication_data(publicationData, info.publication_handle);

					ParticipantBuiltinTopicData participantDataInfo = discoveredParticipants
							.get(publicationData.participant_key.toString());
					
					System.out.println("Receiving service request from: " + participantDataInfo);

					String serviceId = sample.Identificador;
					if (sample.NodeId.length() > 0) {
						vnfId = sample.NodeId;
					}
					if (Integer.parseInt(sample.TerminationPointId) != 0) {
						vnfCpuRequested = Integer.parseInt(sample.TerminationPointId);
					}
					if (Integer.parseInt(sample.LinkId) != 0) {
						vnfRunningTime = Integer.parseInt(sample.LinkId);
					}
					String vnfDeadline = sample.SourceNodeTp;
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

					if (!sample.DestinationNodeTp.equals("0") || !sample.DestinationNodeTp.equals("-1")) {
						List<String> vnfRequirements = new ArrayList<String>();
						vnfRequirements.add(0, Integer.toString(vnfCpuRequested));
						vnfRequirements.add(1, Integer.toString(vnfRunningTime));
						vnfRequirements.add(2, vnfDeadline);
						vnfRequirements.add(3, vnfsInService);
						vnfRequirements.add(4, serviceId);

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
				
				System.setOut(originalOut);

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
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} finally {
			}
		}
	}
	
	/**
	 * Method to read any subscribed information between GCs and ACs. 
	 * The kind of information is announced through the Identificador field
	 * of the topologia topic.
	 */
	private void on_data_available_local() {

		SampleInfo info = new SampleInfo();
		topologia sample = new topologia();

		boolean follow = true;
		while (follow) {
			try {
				dataReader_local.take_next_sample(sample, info);
				
				PrintStream originalOut = System.out;
				
				PrintStream fileOut = new PrintStream(new FileOutputStream("./out.txt", true), true);
				
				System.setOut(fileOut);
				/*
				 * if (info.valid_data) { byte[] ip =
				 * info.publication_handle.get_valuesI(); String ipAddress =
				 * byteToInt(ip[0]) + "." + byteToInt(ip[1]) + "." +
				 * byteToInt(ip[2]) + "." + byteToInt(ip[3]);
				 * 
				 * String keyipLocalController = ipAddress; }
				 */
				
				if (sample.Identificador.equals("Node")) {
					
					PublicationBuiltinTopicData publicationData = new PublicationBuiltinTopicData();

					dataReader_local.get_matched_publication_data(
							publicationData, info.publication_handle);

					ParticipantBuiltinTopicData participantDataInfo = discoveredParticipants
							.get(publicationData.participant_key.toString());

					NodeBuilder node = new NodeBuilder();
					NodeId nodeId = new NodeId(sample.NodeId);
					node.setId(nodeId);
					NodeKey key = new NodeKey(new NodeId(nodeId));
					node.setKey(key);

					InstanceIdentifier<Node> instanceIdentifier = InstanceIdentifier
							.builder(Nodes.class)
							.child(Node.class, new NodeKey(nodeId)).build();
					GenericTransactionUtils.writeData(dataBroker,
							LogicalDatastoreType.OPERATIONAL,
							instanceIdentifier, node.build(), true);

					originalOut.println("Node: " + nodeId.getValue().toString()
							+ " from: "
							+ participantDataInfo.participant_name.name
							+ " has been introduced in Data Store");

					String nodeid = nodeId.getValue().toString();
					String switchId = TranslateFormatControllerId(nodeid);

					if (!TutorialL2Forwarding.switchesACPerGC
							.containsKey(ownKeyName)) {
						TutorialL2Forwarding.switchesACPerGC.put(ownKeyName,
								new HashMap<String, List<String>>());
					}

					if (!TutorialL2Forwarding.switchesACPerGC.get(ownKeyName)
							.containsKey(
									participantDataInfo.participant_name.name)) {
						TutorialL2Forwarding.switchesACPerGC.get(ownKeyName)
								.put(participantDataInfo.participant_name.name,
										new ArrayList<String>());
					}

					if (!TutorialL2Forwarding.switchesACPerGC.get(ownKeyName)
							.get(participantDataInfo.participant_name.name)
							.contains(switchId)) {
						TutorialL2Forwarding.switchesACPerGC.get(ownKeyName)
								.get(participantDataInfo.participant_name.name)
								.add(switchId);
						originalOut.println(switchesACPerGC);
						System.out.println(switchesACPerGC);
					}

					// if (!TutorialL2Forwarding.switchesPerController
					// .containsKey(keyipLocalController)) {
					// TutorialL2Forwarding.switchesPerController.put(
					// keyipLocalController,
					// new ArrayList<String>());
					// }

					// if (!TutorialL2Forwarding.switchesPerController.get(
					// keyipLocalController).contains(switchId)) {
					// TutorialL2Forwarding.switchesPerController.get(
					// keyipLocalController).add(switchId);
					// }

					topologia node1 = new topologia();
					node1.Identificador = participantDataInfo.participant_name.name
							+ "_Node";
					node1.NodeId = nodeId.getValue().toString();

					dataWriter_global.write(node1, InstanceHandle_t.HANDLE_NIL);

					originalOut.println("Node: " + nodeId.getValue().toString()
							+ " from: "
							+ participantDataInfo.participant_name.name
							+ " has been sent");
				}

				if (sample.Identificador.equals("TerminationPoint")) {
					
					PublicationBuiltinTopicData publicationData = new PublicationBuiltinTopicData();

					dataReader_local.get_matched_publication_data(
							publicationData, info.publication_handle);

					ParticipantBuiltinTopicData participantDataInfo = discoveredParticipants
							.get(publicationData.participant_key.toString());

					NodeConnectorBuilder nodeConnector = new NodeConnectorBuilder();
					NodeId nodeId = new NodeId(sample.NodeId);
					NodeConnectorId nodeConnectorId = new NodeConnectorId(
							sample.TerminationPointId);
					NodeConnectorKey key = new NodeConnectorKey(
							new NodeConnectorId(nodeConnectorId));
					nodeConnector.setId(nodeConnectorId);
					nodeConnector.setKey(key);

					InstanceIdentifier<NodeConnector> instanceIdentifier2 = InstanceIdentifier
							.builder(Nodes.class)
							.child(Node.class,
									new NodeKey(new NodeId(sample.NodeId)))
							.child(NodeConnector.class,
									new NodeConnectorKey(new NodeConnectorId(
											sample.TerminationPointId)))
							.build();
					GenericTransactionUtils.writeData(dataBroker,
							LogicalDatastoreType.OPERATIONAL,
							instanceIdentifier2, nodeConnector.build(), true);

					originalOut.println("TerminationPoint: "
							+ nodeConnectorId.getValue().toString() + " from: "
							+ participantDataInfo.participant_name.name
							+ " has been introduced in Data Store");

					topologia tp = new topologia();
					tp.Identificador = "TerminationPoint";
					tp.NodeId = nodeId.getValue().toString();
					tp.TerminationPointId = nodeConnectorId.getValue()
							.toString();
					dataWriter_global.write(tp, InstanceHandle_t.HANDLE_NIL);

					originalOut.println("TerminationPoint: "
							+ nodeConnectorId.getValue().toString() + " from: "
							+ participantDataInfo.participant_name.name
							+ " has been sent");
				}

				if (sample.Identificador.equals("Link")) {
					
					PublicationBuiltinTopicData publicationData = new PublicationBuiltinTopicData();

					dataReader_local.get_matched_publication_data(
							publicationData, info.publication_handle);

					ParticipantBuiltinTopicData participantDataInfo = discoveredParticipants
							.get(publicationData.participant_key.toString());

					originalOut.println("Receiving link information from: "
							+ participantDataInfo.participant_name.name);

					LinkId linkid = new LinkId(sample.LinkId);
					NodeId localNodeId = new NodeId(sample.SourceNode);
					NodeConnectorId localNodeConnectorId = new NodeConnectorId(
							sample.SourceNodeTp);
					NodeId remoteNodeId = new NodeId(sample.DestinationNode);
					NodeConnectorId remoteNodeConnectorId = new NodeConnectorId(
							sample.DestinationNodeTp);

					// LinkBuilder link = new LinkBuilder();
					// org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId
					// linkId = new
					// org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId(sample.LinkId);
					// LinkKey linkkey = new LinkKey(linkId);
					// link.setLinkId(linkId);
					// link.setKey(linkkey);
					// SourceBuilder source = new SourceBuilder();
					// source.setSourceNode(localNodeId);
					// source.setSourceTp(localNodeConnectorId);
					// DestinationBuilder destination = new
					// DestinationBuilder();
					// destination.setDestNode(remoteNodeId);
					// destination.setDestTp(remoteNodeConnectorId);
					// link.setSource(source);
					// link.setDestination(destination);

					@SuppressWarnings("deprecation")
					InstanceIdentifier<NodeConnector> localInstanceIdentifier = InstanceIdentifier
							.builder(Nodes.class)
							.child(Node.class, new NodeKey(localNodeId))
							.child(NodeConnector.class,
									new NodeConnectorKey(localNodeConnectorId))
							.toInstance();
					NodeConnectorRef localNodeConnectorRef = new NodeConnectorRef(
							localInstanceIdentifier);

					@SuppressWarnings("deprecation")
					InstanceIdentifier<NodeConnector> remoteInstanceIdentifier = InstanceIdentifier
							.builder(Nodes.class)
							.child(Node.class, new NodeKey(remoteNodeId))
							.child(NodeConnector.class,
									new NodeConnectorKey(remoteNodeConnectorId))
							.toInstance();
					NodeConnectorRef remoteNodeConnectorRef = new NodeConnectorRef(
							remoteInstanceIdentifier);
					// InstanceIdentifier<LinkDiscovered> InstanceIdentifier
					// =
					// InstanceIdentifier.builder(Lin.class).toInstance();
					// Falta crear el link y meterlo en la base de datos

					LinkDiscoveredBuilder ldb = new LinkDiscoveredBuilder();
					ldb.setSource(localNodeConnectorRef);
					ldb.setDestination(remoteNodeConnectorRef);

					// LinkDiscovered link = ldb.build();
					// @SuppressWarnings("unchecked")
					// InstanceIdentifier<LinkDiscovered> instanceIdentifier
					// = InstanceIdentifier.create((LinkDiscovered.class));
					TutorialL2Forwarding.this.notifService.publish(ldb.build());
					originalOut
							.println("Link has been introduced in Data Store");
					// GenericTransactionUtils.writeData(dataBroker,
					// LogicalDatastoreType.OPERATIONAL,
					// instanceIdentifier,ldb.build(),true);

					topologia link1 = new topologia();
					link1.Identificador = "Link";
					link1.LinkId = linkid.getValue().toString();
					link1.SourceNode = localNodeId.getValue().toString();
					link1.SourceNodeTp = localNodeConnectorId.getValue()
							.toString();
					link1.DestinationNode = remoteNodeId.getValue().toString();
					link1.DestinationNodeTp = remoteNodeConnectorId.getValue()
							.toString();
					dataWriter_global.write(link1, InstanceHandle_t.HANDLE_NIL);

					originalOut.println("Sending link information");
					// System.out.println(linkid.getValue().toString());
					originalOut.println(localNodeId.getValue().toString());
					originalOut.println(localNodeConnectorId.getValue()
							.toString());
					originalOut.println(remoteNodeId.getValue().toString());
					originalOut.println(remoteNodeConnectorId.getValue()
							.toString());
				}

				if (sample.Identificador.equals("Flow")) {
					
					PublicationBuiltinTopicData publicationData = new PublicationBuiltinTopicData();

					dataReader_local.get_matched_publication_data(
							publicationData, info.publication_handle);

					ParticipantBuiltinTopicData participantDataInfo = discoveredParticipants
							.get(publicationData.participant_key.toString());

					originalOut.println("Receiving flow information from: "
							+ participantDataInfo.participant_name.name);

					NodeId nodeId = new NodeId(sample.NodeId);
					String srcMac = sample.SourceNode;
					String dstMac = sample.DestinationNode;
					NodeConnectorId ingressNodeConnectorId = new NodeConnectorId(
							sample.SourceNodeTp);

					// Create a table for this switch if it does not exist
					if (!this.macTablePerSwitch.containsKey(sample.NodeId)) {
						this.macTablePerSwitch.put(sample.NodeId,
								new HashMap<String, String>());
					}

					// Learn source MAC address (2.2)
					String previousSrcMacPortStr = this.macTablePerSwitch.get(
							sample.NodeId).get(srcMac);
					if ((previousSrcMacPortStr == null)
							|| (!sample.SourceNodeTp
									.equals(previousSrcMacPortStr))) {
						this.macTablePerSwitch.get(sample.NodeId).put(srcMac,
								sample.SourceNodeTp);
					}

					String egressNodeConnectorIdStr = this.macTablePerSwitch
							.get(sample.NodeId).get(dstMac);
					NodeConnectorId egressNodeConnectorId = null;
					if (egressNodeConnectorIdStr != null) {
						// Entry found (2.3.1)
						egressNodeConnectorId = new NodeConnectorId(
								egressNodeConnectorIdStr);
						// Perform FLOW_MOD (2.3.1.1)
						programL2FlowOperational(nodeId, dstMac,
								ingressNodeConnectorId, egressNodeConnectorId);
						originalOut
								.println("Flow has been introduced in Data Store");
					}

					topologia flow = new topologia();
					flow.Identificador = "Flow";
					flow.NodeId = nodeId.getValue().toString();
					flow.SourceNodeTp = ingressNodeConnectorId.getValue()
							.toString();
					flow.SourceNode = srcMac;
					// flow.DestinationNodeTp = egressNodeConnectorIdStr;
					flow.DestinationNode = dstMac;
					dataWriter_global.write(flow, InstanceHandle_t.HANDLE_NIL);

					originalOut.println("Sending flow information");
				}

				if (sample.Identificador.equals("LoadTotal")) {
					
					PublicationBuiltinTopicData publicationData = new PublicationBuiltinTopicData();

					dataReader_local.get_matched_publication_data(
							publicationData, info.publication_handle);

					ParticipantBuiltinTopicData participantDataInfo = discoveredParticipants
							.get(publicationData.participant_key.toString());

					String keyipPublicController = IPfromLocatorMetatraffic(participantDataInfo.metatraffic_unicast_locators);

					System.out
							.println("Receiving overall load information from: "
									+ participantDataInfo.participant_name.name);

					long loadT = Long.parseLong(sample.NodeId);
					double arrivalRate = Double
							.parseDouble(sample.TerminationPointId);

					System.out.println("Controller: "
							+ participantDataInfo.participant_name.name
							+ " has Load: " + loadT + " and Arrival Rate: "
							+ arrivalRate);

					TutorialL2Forwarding.loadPerController.put(
							keyipPublicController, loadT);

					System.out.println(loadPerController);

					TutorialL2Forwarding.arrivalRatePerController.put(
							keyipPublicController, arrivalRate);

					System.out.println(arrivalRatePerController);

					topologia load = new topologia();
					load.Identificador = participantDataInfo.participant_name.name
							+ "_LoadTotal";
					load.NodeId = Long.toString(loadT);
					load.TerminationPointId = Double.toString(arrivalRate);
					load.LinkId = keyipPublicController;

					dataWriter_global.write(load, InstanceHandle_t.HANDLE_NIL);

					System.out
							.println("Sending overall load information of controller: "
									+ participantDataInfo.participant_name.name
									+ " which load is: "
									+ Long.toString(loadT)
									+ " and Arrival Rate: "
									+ Double.toString(arrivalRate));

				}

				if (sample.Identificador.equals("LoadNodesPerAC")) {
					
					PublicationBuiltinTopicData publicationData = new PublicationBuiltinTopicData();

					dataReader_local.get_matched_publication_data(
							publicationData, info.publication_handle);

					ParticipantBuiltinTopicData participantDataInfo = discoveredParticipants
							.get(publicationData.participant_key.toString());

					String keyipPublicController = IPfromLocatorMetatraffic(participantDataInfo.metatraffic_unicast_locators);

					System.out
							.println("Receiving load per node information from: "
									+ participantDataInfo.participant_name.name);

					NodeId nodeId = new NodeId(sample.NodeId);
					long loadN = Long.parseLong(sample.TerminationPointId);
					long deltaTimeArrivalPacket = Long.parseLong(sample.LinkId);

					String nodeid = nodeId.getValue().toString();
					String switchId = TranslateFormatControllerId(nodeid);

					if (!TutorialL2Forwarding.packetInNodesPerAC
							.containsKey(keyipPublicController)) {
						TutorialL2Forwarding.packetInNodesPerAC.put(
								keyipPublicController,
								new HashMap<String, Long>());
					} else {
						TutorialL2Forwarding.packetInNodesPerAC.get(
								keyipPublicController).put(switchId, loadN);
						System.out.println(packetInNodesPerAC);
					}

					if (!TutorialL2Forwarding.deltaTimeArrivalPacketNodesPerAC
							.containsKey(keyipPublicController)) {
						TutorialL2Forwarding.deltaTimeArrivalPacketNodesPerAC
								.put(keyipPublicController,
										new HashMap<String, Long>());
					} else {
						TutorialL2Forwarding.deltaTimeArrivalPacketNodesPerAC
								.get(keyipPublicController).put(switchId,
										deltaTimeArrivalPacket);
						System.out.println(deltaTimeArrivalPacketNodesPerAC);
					}

					float arrivalRateNode = (deltaTimeArrivalPacket == 0) ? 0
							: ((float) loadN / (float) deltaTimeArrivalPacket);

					if (!TutorialL2Forwarding.nodesLoadPerAC
							.containsKey(participantDataInfo.participant_name.name)) {
						TutorialL2Forwarding.nodesLoadPerAC.put(
								participantDataInfo.participant_name.name,
								new HashMap<String, Float>());
					} else {
						TutorialL2Forwarding.nodesLoadPerAC.get(
								participantDataInfo.participant_name.name).put(
								switchId, arrivalRateNode);
						System.out.println(nodesLoadPerAC);
					}

					topologia load = new topologia();
					load.Identificador = participantDataInfo.participant_name.name
							+ "_LoadNodesPerAC";
					load.NodeId = nodeId.getValue().toString();
					load.TerminationPointId = Float.toString(arrivalRateNode);

					dataWriter_global.write(load, InstanceHandle_t.HANDLE_NIL);

					System.out.println("Sending load information of Node: "
							+ nodeId.getValue().toString() + " which load is: "
							+ Float.toString(arrivalRateNode));
				}

				if (sample.Identificador.equals("Overloaded")) {
					
					PublicationBuiltinTopicData publicationData = new PublicationBuiltinTopicData();

					dataReader_local.get_matched_publication_data(
							publicationData, info.publication_handle);

					ParticipantBuiltinTopicData participantDataInfo = discoveredParticipants
							.get(publicationData.participant_key.toString());

					String keyipPublicController = IPfromLocatorMetatraffic(participantDataInfo.metatraffic_unicast_locators);

					double arrivalRate = Double.parseDouble(sample.NodeId);
					TutorialL2Forwarding.arrivalRatePerController.put(
							keyipPublicController, arrivalRate);

					System.out.println("Controller " + keyipPublicController
							+ " overloaded with an arrival rate of "
							+ arrivalRate);
					// SelectSwitchToMigrate(keyipController, arrivalRate);
				}

				// Reading remaining SOC of any node belonging to registered clusters
				if (sample.Identificador.equals("SoC")) {

					PublicationBuiltinTopicData publicationData = new PublicationBuiltinTopicData();

					dataReader_local.get_matched_publication_data(publicationData, info.publication_handle);

					ParticipantBuiltinTopicData participantDataInfo = discoveredParticipants
							.get(publicationData.participant_key.toString());

					// String keyIPPublicMaster = IPfromLocatorMetatraffic(participantDataInfo.metatraffic_unicast_locators);

					System.out.println("Receiving SOC per node information from: "
							+ participantDataInfo.participant_name.name);

					NodeId nodeId = new NodeId(sample.NodeId);
					float soc = Float.parseFloat(sample.TerminationPointId);
					String k8snodeId = sample.NodeId;

					//String nodeid = nodeId.getValue().toString();
					//String k8snodeId = TranslateFormatControllerId(nodeid);

					if (!TutorialL2Forwarding.nodesSOCPerMaster.containsKey(participantDataInfo.participant_name.name)) {
						TutorialL2Forwarding.nodesSOCPerMaster.put(participantDataInfo.participant_name.name, new HashMap<String, Float>());
					} else {
						TutorialL2Forwarding.nodesSOCPerMaster.get(participantDataInfo.participant_name.name).put(k8snodeId, soc);
						System.out.println(nodesSOCPerMaster);
					}

					topologia SoC = new topologia();
					SoC.Identificador = participantDataInfo.participant_name.name + "_SOCNodesPerMaster";
					SoC.NodeId = nodeId.getValue().toString();
					SoC.TerminationPointId = Float.toString(soc);
					
					dataWriter_global.write(SoC, InstanceHandle_t.HANDLE_NIL);

					System.out.println("Sending SOC information of Node: "
							+ nodeId.getValue().toString() + " which SOC is: "
							+ Float.toString(soc));
				}

				// Reading remaining CPU of any node belonging to registered clusters
				if (sample.Identificador.equals("CPU")) {

					PublicationBuiltinTopicData publicationData = new PublicationBuiltinTopicData();

					dataReader_local.get_matched_publication_data(publicationData, info.publication_handle);

					ParticipantBuiltinTopicData participantDataInfo = discoveredParticipants
							.get(publicationData.participant_key.toString());

					// String keyIPPublicMaster = IPfromLocatorMetatraffic(participantDataInfo.metatraffic_unicast_locators);

					System.out.println("Receiving CPU per node information from: "
							+ participantDataInfo.participant_name.name);

					NodeId nodeId = new NodeId(sample.NodeId);
					int cpu = Integer.parseInt(sample.TerminationPointId);
					String k8snodeId = sample.NodeId;
					boolean anyVNFRunning = Boolean.parseBoolean(sample.LinkId);

					//String nodeid = nodeId.getValue().toString();
					//String k8snodeId = TranslateFormatControllerId(nodeid);

					if (!TutorialL2Forwarding.nodesCPUPerMaster.containsKey(participantDataInfo.participant_name.name)) {
						TutorialL2Forwarding.nodesCPUPerMaster.put(participantDataInfo.participant_name.name, new HashMap<String, Integer>());
					} else {
						TutorialL2Forwarding.nodesCPUPerMaster.get(participantDataInfo.participant_name.name).put(k8snodeId, cpu);
						System.out.println(nodesCPUPerMaster);
					}

					if (!TutorialL2Forwarding.anyVNFInNodesPerMaster.containsKey(participantDataInfo.participant_name.name)) {
						TutorialL2Forwarding.anyVNFInNodesPerMaster.put(participantDataInfo.participant_name.name, new HashMap<String, Boolean>());
					} else {
						TutorialL2Forwarding.anyVNFInNodesPerMaster.get(participantDataInfo.participant_name.name).put(k8snodeId, anyVNFRunning);
						System.out.println(anyVNFInNodesPerMaster);
					}

					topologia CPU = new topologia();
					CPU.Identificador = participantDataInfo.participant_name.name + "_CPUNodesPerMaster";
					CPU.NodeId = nodeId.getValue().toString();
					CPU.TerminationPointId = Integer.toString(cpu);
					CPU.LinkId = Boolean.toString(anyVNFRunning);
					
					dataWriter_global.write(CPU, InstanceHandle_t.HANDLE_NIL);

					System.out.println("Sending CPU information of Node: "
							+ nodeId.getValue().toString() + " which CPU is: "
							+ Integer.toString(cpu));
				}	
				
				// Reading service request with the current VNF to deploy, its requirements and remaining VNFs to deploy.
				if (sample.Identificador.startsWith("serv-")) {
					String vnfId = "";
					int vnfCpuRequested = 0;
					int vnfRunningTime = 0;
					String vnfsInService = "";
					PublicationBuiltinTopicData publicationData = new PublicationBuiltinTopicData();

					dataReader_local.get_matched_publication_data(publicationData, info.publication_handle);

					ParticipantBuiltinTopicData participantDataInfo = discoveredParticipants
							.get(publicationData.participant_key.toString());
					
					System.out.println("Receiving service request from: " + participantDataInfo);

					String serviceId = sample.Identificador;
					if (sample.NodeId.length() > 0) {
						vnfId = sample.NodeId;
					}
					if (Integer.parseInt(sample.TerminationPointId) != 0) {
						vnfCpuRequested = Integer.parseInt(sample.TerminationPointId);
					}
					if (Integer.parseInt(sample.LinkId) != 0) {
						vnfRunningTime = Integer.parseInt(sample.LinkId);
					}
					String vnfDeadline = sample.SourceNodeTp;
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

					if (!sample.DestinationNodeTp.equals("0") || !sample.DestinationNodeTp.equals("-1")) {
						List<String> vnfRequirements = new ArrayList<String>();
						vnfRequirements.add(0, Integer.toString(vnfCpuRequested));
						vnfRequirements.add(1, Integer.toString(vnfRunningTime));
						vnfRequirements.add(2, vnfDeadline);
						vnfRequirements.add(3, vnfsInService);
						vnfRequirements.add(4, serviceId);

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
					String vnfId = sample.NodeId;
					if (vnfId.equals(currentVNF)) {
						TutorialL2Forwarding.isCurrentVNFDeployed = true;
					}
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
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} finally {
			}
		}
	}

	public TutorialL2Forwarding(DataBroker dataBroker) {
		this.dataBroker = dataBroker;
	}

	@Override
	public void close() throws Exception {
		for (Registration registration : registrations) {
			registration.close();
		}
		registrations.clear();
	}

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
						
						if (TutorialL2Forwarding.switchesACPerGC.containsKey(participantData.participant_name.name)) {
							Set<String> ac = TutorialL2Forwarding.switchesACPerGC.get(participantData.participant_name.name).keySet();
							
							Iterator<String> iter = ac.iterator();
							
							while (iter.hasNext()) {
								String acs = iter.next();
								
								if (TutorialL2Forwarding.switchesACPerGC.get(ownKeyName).containsKey(acs))  {
									TutorialL2Forwarding.switchesACPerGC.get(ownKeyName).remove(acs);
									System.out.println(switchesACPerGC);
								}
							}
						}

					} else {
						String dissapearReason;
						if (info.instance_state == InstanceStateKind.NOT_ALIVE_DISPOSED_INSTANCE_STATE) {
							dissapearReason = "delected";
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
							
							// Failover in my ACs
							if (participantDataFail.participant_name.name
									.endsWith("_UPC")) {
								System.out
										.println("Controller "
												+ participantDataFail.participant_name.name
												+ " with IP "
												+ ipPublicControllerFail
												+ " has failed. Executing failover");

								Map<String, List<String>> tupleAC_Nodes = ControllerTarget(
										ownKeyName,
										participantDataFail.participant_name.name);

								TutorialL2Forwarding
										.migrateSwitches(tupleAC_Nodes);

								// TutorialL2Forwarding.checkFailoverOwnACs(
								// ipLocalControllerFail,
								// ipControllerTarget);
							}

							// Failover in other ACs
							if (participantDataFail.participant_name.name
									.startsWith("AC")
									&& !participantDataFail.participant_name.name
											.endsWith("_UPC")) {

								String itsGC = new String(
										"GC"
												+ participantDataFail.participant_name.name
														.substring(3));

								if (failureParticipants.containsKey(itsGC)) {
									System.out
											.println("Controller "
													+ participantDataFail.participant_name.name
													+ " with IP "
													+ ipPublicControllerFail
													+ " has failed. Executing failover");

									Map<String, List<String>> tupleAC_Nodes = ControllerTarget(
											itsGC,
											participantDataFail.participant_name.name);

									TutorialL2Forwarding
											.migrateSwitches(tupleAC_Nodes);

									// TutorialL2Forwarding
									// .checkFailoverOtherACs(
									// ipPublicControllerFail,
									// ipControllerTarget,
									// itsGC,
									// participantDataFail.participant_name.name);

								} else {
									System.out
											.println("Controller "
													+ participantDataFail.participant_name.name
													+ " with IP "
													+ ipPublicControllerFail
													+ " has failed. Execute failover");
								}
							}
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
	 * This method is executed automatically after receiving a Packet_In.
	 * It implements a L2 forwarding logic by configuring the openflow rules
	 * in the assigned switches. Additionally, it publishes new switches, links
	 * and flows to registered controllers through the DDS.
	 */
	@Override
	public void onPacketReceived(PacketReceived notification) {

		LOG.trace("Received packet notification {}", notification.getMatch());

		NodeConnectorRef ingressNodeConnectorRef = notification.getIngress();
		//NodeRef ingressNodeRef = InventoryUtils.getNodeRef(ingressNodeConnectorRef);
		NodeConnectorId ingressNodeConnectorId = InventoryUtils
				.getNodeConnectorId(ingressNodeConnectorRef);
		NodeId ingressNodeId = InventoryUtils
				.getNodeId(ingressNodeConnectorRef);

		// Useful to create it beforehand
		//NodeConnectorId floodNodeConnectorId = InventoryUtils.getNodeConnectorId(ingressNodeId, FLOOD_PORT_NUMBER);
		//NodeConnectorRef floodNodeConnectorRef = InventoryUtils.getNodeConnectorRef(floodNodeConnectorId);

		/*
		 * Logic: 0. Ignore LLDP packets 1. If behaving as "hub", perform a
		 * PACKET_OUT with FLOOD action 2. Else if behaving as
		 * "learning switch", 2.1. Extract MAC addresses 2.2. Update MAC table
		 * with source MAC address 2.3. Lookup in MAC table for the target node
		 * connector of dst_mac 2.3.1 If found, 2.3.1.1 perform FLOW_MOD for
		 * that dst_mac through the target node connector 2.3.1.2 perform
		 * PACKET_OUT of this packet to target node connector 2.3.2 If not
		 * found, perform a PACKET_OUT with FLOOD action
		 */

		// Ignore LLDP packets, or you will be in big trouble
		byte[] etherTypeRaw = PacketParsingUtils.extractEtherType(notification
				.getPayload());
		int etherType = (0x0000ffff & ByteBuffer.wrap(etherTypeRaw).getShort());
		if (etherType == 0x88cc) {
			return;
		}

		// Hub implementation
		if (function.equals("hub")) {

			// flood packet (1)
			// packetOut(ingressNodeRef, floodNodeConnectorRef,
			// notification.getPayload());
		} else {
			noPacket = noPacket + 1;

			byte[] payload = notification.getPayload();
			byte[] dstMacRaw = PacketParsingUtils.extractDstMac(payload);
			byte[] srcMacRaw = PacketParsingUtils.extractSrcMac(payload);

			// Extract MAC addresses (2.1)
			String srcMac = PacketParsingUtils.rawMacToString(srcMacRaw);
			String dstMac = PacketParsingUtils.rawMacToString(dstMacRaw);

			// Strings for ingressNodeId and ingressNodeConnectorId
			String ingressNodeIdStr = ingressNodeId.getValue();
			String ingressNodeConnectorIdStr = ingressNodeConnectorId
					.getValue();

			// Create a table for this switch if it does not exist
			if (!this.macTablePerSwitch.containsKey(ingressNodeIdStr)) {
				// LOG.debug("JNa>> noPacket {}, switch:port {} -> Creating MAC table for this switch in the controller",
				// noPacket, ingressNodeConnectorIdStr, ingressNodeIdStr);
				this.macTablePerSwitch.put(ingressNodeIdStr,
						new HashMap<String, String>());
			}

			// Learn source MAC address (2.2)
			String previousSrcMacPortStr = this.macTablePerSwitch.get(
					ingressNodeIdStr).get(srcMac);
			if ((previousSrcMacPortStr == null)
					|| (!ingressNodeConnectorIdStr
							.equals(previousSrcMacPortStr))) {
				this.macTablePerSwitch.get(ingressNodeIdStr).put(srcMac,
						ingressNodeConnectorIdStr);
				// programL2Flow(ingressNodeId, srcMac, null,
				// ingressNodeConnectorId);
			}

			// Lookup destination MAC address in table (2.3)
			String egressNodeConnectorIdStr = this.macTablePerSwitch.get(
					ingressNodeIdStr).get(dstMac);
			//NodeConnectorId egressNodeConnectorId = null;
			//NodeConnectorRef egressNodeConnectorRef = null;
			if (egressNodeConnectorIdStr != null) {
				// Entry found (2.3.1)
				//egressNodeConnectorId = new NodeConnectorId(egressNodeConnectorIdStr);
				//egressNodeConnectorRef = InventoryUtils.getNodeConnectorRef(egressNodeConnectorId);
				topologia flow = new topologia();
				flow.Identificador = "Flow";
				flow.NodeId = ingressNodeIdStr;
				flow.SourceNodeTp = ingressNodeConnectorIdStr;
				flow.SourceNode = srcMac;
				flow.DestinationNodeTp = egressNodeConnectorIdStr;
				flow.DestinationNode = dstMac;
				dataWriter_global.write(flow, InstanceHandle_t.HANDLE_NIL);
				dataWriter_local.write(flow, InstanceHandle_t.HANDLE_NIL);

				// Perform FLOW_MOD (2.3.1.1)
				// programL2Flow(ingressNodeId, dstMac, ingressNodeConnectorId,
				// egressNodeConnectorId);

				// Perform PACKET_OUT (2.3.1.2)
				// packetOut(ingressNodeRef, egressNodeConnectorRef, payload);
			} else {
				// Flood packet (2.3.2)
				// floodingPacket(ingressNodeConnectorRef, payload); // FLOODING
				// packets to each port for any topology

				// Añadir aqui la parte para transferir los flujos con DDS
				// TOPOLOGY
				InstanceIdentifier<NetworkTopology> ntII = InstanceIdentifier
						.builder(NetworkTopology.class).build();
				NetworkTopology networkTopology = GenericTransactionUtils
						.readData(dataBroker, LogicalDatastoreType.OPERATIONAL,
								ntII);
				LOG.debug("JNa>> networkTopology = {}", networkTopology);
				// System.out.println(networkTopology.toString());

				// Topology
				List<Topology> topologies = networkTopology.getTopology();
				// for (Topology topo : topologies) {
				// Only the first topology is defined by default in ODL
				Topology topo = topologies.get(0);
				LOG.debug("JNa>> topo = {}", topo);
				System.out.println(topo.toString());
				List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> nodes = topo
						.getNode();
				LOG.debug("JNa>> nodes = {}", nodes);

				//index = 0;
				for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node node : nodes) {
					System.out.println(node.getNodeId().getValue().toString());
					// dataWriter.write(node.getNodeId().getValue().toString(),
					// InstanceHandle_t.HANDLE_NIL);
					topologia instance = new topologia();
					instance.Identificador = "Node";
					instance.NodeId = node.getNodeId().getValue().toString();
					dataWriter_global.write(instance,
							InstanceHandle_t.HANDLE_NIL);
					dataWriter_local.write(instance,
							InstanceHandle_t.HANDLE_NIL);
					//index = index + 1;
					List<TerminationPoint> TP = node.getTerminationPoint();
					for (TerminationPoint Tp : TP) {
						topologia instance2 = new topologia();
						instance2.Identificador = "TerminationPoint";
						instance2.NodeId = node.getNodeId().getValue()
								.toString();
						instance2.TerminationPointId = Tp.getTpId().getValue()
								.toString();
						dataWriter_global.write(instance2,
								InstanceHandle_t.HANDLE_NIL);
						dataWriter_local.write(instance2,
								InstanceHandle_t.HANDLE_NIL);
						System.out.println(Tp.getTpId().getValue().toString());
					}
				}
				List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> links = topo
						.getLink();

				//index = 0;
				for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link link : links) {
					topologia instance = new topologia();
					instance.Identificador = "Link";
					instance.LinkId = link.getLinkId().getValue().toString();
					instance.SourceNode = link.getSource().getSourceNode()
							.getValue().toString();
					instance.SourceNodeTp = link.getSource().getSourceTp()
							.getValue().toString();
					instance.DestinationNode = link.getDestination()
							.getDestNode().getValue().toString();
					instance.DestinationNodeTp = link.getDestination()
							.getDestTp().getValue().toString();
					dataWriter_global.write(instance,
							InstanceHandle_t.HANDLE_NIL);
					dataWriter_local.write(instance,
							InstanceHandle_t.HANDLE_NIL);
					System.out.println("New Link");
					// System.out.println(link.getLinkId().getValue());
					System.out.println(link.getSource().getSourceNode()
							.getValue().toString());
					System.out.println(link.getSource().getSourceTp()
							.getValue().toString());
					System.out.println(link.getDestination().getDestNode()
							.getValue().toString());
					System.out.println(link.getDestination().getDestTp()
							.getValue().toString());
					//index = index + 1;
				}
				//System.out.println(index);

			}
		}
	}

	/*
	private static String getControllerTarget(Map<String, List<String>> map,
			String ipController) {
		Set<String> ipACs = map.keySet();
		String ipAddress = ipController;
		int minvalue = map.values().size();
		String ipControllerTarget = "";
		for (String key : ipACs) {
			if (!key.equals(ipAddress)) {
				int size = switchesPerController.get(key).size();
				if (size < minvalue) {
					minvalue = size;
					ipControllerTarget = key;
				}
			}
		}
		return ipControllerTarget;
	}
	*/
	
	/**
	 * Method to indicate in which node must be deployed the current VNF.
	 * It publishes the cluster, node and VNF to the registered master nodes.
	 * 
	 * @param cluster   selected cluster to deploy vnf
	 * @param node		selected node of the cluster where the VNF is deployed
	 * @param vnfName   current VNF to deploy
	 */
	public static void notifyVNFDeployment(int cluster, int node, String vnfName) {
		topologia VNF = new topologia();
		VNF.Identificador = "kubernetes-control-plane" + Integer.toString(cluster);
		if (TutorialL2Forwarding.nodesCPUPerMaster.get(VNF.Identificador).size() == node) {
			VNF.NodeId = "kubernetes-control-plane";
		} else {
			VNF.NodeId = "kubernetes-worker" + Integer.toString(node);
		}
		VNF.TerminationPointId = vnfName;
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

		NavigableSet<String> keysCPU = TutorialL2Forwarding.nodesCPUPerMaster.keySet();
		Iterator<String> iteratorCPU = keysCPU.iterator();
		int i = 0;
		while (iteratorCPU.hasNext()) {
			String next = iteratorCPU.next();
			Collection<Integer> cpus = TutorialL2Forwarding.nodesCPUPerMaster.get(next).values();
			Iterator<Integer> cpu_iter = cpus.iterator();
			while(cpu_iter.hasNext()) {
				cpu_values[i] = cpu_iter.next();
				i++;
			}
		}

		NavigableSet<String> keysSOC = TutorialL2Forwarding.nodesSOCPerMaster.keySet();
		Iterator<String> iteratorSOC = keysSOC.iterator();
		int j = 0;
		while (iteratorSOC.hasNext()) {
			String next = iteratorSOC.next();
			Collection<Float> socs = TutorialL2Forwarding.nodesSOCPerMaster.get(next).values();
			Iterator<Float> soc_iter = socs.iterator();
			while(soc_iter.hasNext()) {
				soc_values[j] = soc_iter.next();
				j++;
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
	 * Method used by the DQM algorithm to get the name of current VNF
	 * to deploy
	 * 
	 * @return   name of the current VNF to deploy
	 */
	public static String getCurrentVNF() {
		String vnf_name = "";
		if (TutorialL2Forwarding.vnfRequestedtoDeploy.size() > 1) {
			vnf_name = TutorialL2Forwarding.vnfRequestedtoDeploy.firstKey();
			currentVNF = vnf_name;
		}
		return vnf_name;
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
		if (TutorialL2Forwarding.vnfRequestedtoDeploy.isEmpty()) {
			for (int i = 0; i < 4; i++) {
				request[i] = 0f;
			}
		} else {
			String key = TutorialL2Forwarding.vnfRequestedtoDeploy.firstKey();
			List<String> vnf = TutorialL2Forwarding.vnfRequestedtoDeploy.get(key);
			request[0] = Float.parseFloat(vnf.get(0)) / MAX_CPU_NODES; // CPU demand of analyzed VNF
			request[1] = Float.parseFloat(vnf.get(1)) / 100f; // Running time of analyzed VNF
			request[2] = Float.parseFloat(vnf.get(2)) / 100f; // Deadline of analyzed VNF
			request[3] = Float.parseFloat(vnf.get(3)) / 10f;  // Pending functions of service request
			TutorialL2Forwarding.vnfRequestedtoDeploy.remove(key);
		}

		NDArray vnf_request = manager.create(request);

		return vnf_request;
	}

	/**
	 * Method to calculate the cost of the used resources in
	 * edge nodes where there is any VNF deployed.
	 * 
	 * @return   total cost of used resources in edge nodes
	 */
	public static float getTotalCostUsedResources() {
		float totalCost = 0f;

		NavigableSet<String> keys = TutorialL2Forwarding.anyVNFInNodesPerMaster.keySet();
		Iterator<String> iter = keys.iterator();
		while (iter.hasNext()) {
			String next = iter.next();
			Set<String> keysNodes = TutorialL2Forwarding.anyVNFInNodesPerMaster.get(next).keySet();
			Iterator<String> iterNodes = keysNodes.iterator();
			while (iterNodes.hasNext()) {
				String nextNode = iterNodes.next();
				if (TutorialL2Forwarding.anyVNFInNodesPerMaster.get(next).get(nextNode)) {
					int remaingCPU = TutorialL2Forwarding.nodesCPUPerMaster.get(next).get(nextNode);
					float cost = usedResourcesCost * remaingCPU / MAX_CPU_NODES;
					totalCost = totalCost + cost;
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

		NavigableSet<String> keys = TutorialL2Forwarding.nodesSOCPerMaster.keySet();
		Iterator<String> iter = keys.iterator();
		while (iter.hasNext()) {
			String next = iter.next();
			Collection<Float> socs = TutorialL2Forwarding.nodesSOCPerMaster.get(next).values();
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
		float lifetimeTerm = zeta * TutorialL2Forwarding.getTotalLifeTime();
		float deployedServiceTerm = xi * deployedService;
		float resourceCostTerm = phi * TutorialL2Forwarding.getTotalCostUsedResources();
		
		float reward = lifetimeTerm + deployedServiceTerm - resourceCostTerm;

		return reward;
	}
	
	/**
	 * Method to execute a failover mechanism when an AC fails.
	 * It migrates assigned nodes of failure AC to healthy ones.
	 * 
	 * @param GC       name of the GC that manages the failure AC 
	 * @param ACFail   name of the failure AC
	 * @return         the switches to migrate and the new assigned ACs
	 */
	private static Map<String, List<String>> ControllerTarget(String GC,
			String ACFail) {
		
		PrintStream originalOut = System.out;
		
		PrintStream fileOut = null;
		try {
			fileOut = new PrintStream(new FileOutputStream("./out.txt", true), true);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		System.setOut(fileOut);

		Map<String, List<String>> tupleAC_Nodes = new HashMap<String, List<String>>();

		List<String> switchesToMigrate = TutorialL2Forwarding.switchesACPerGC
				.get(GC).get(ACFail);

		System.out.println(switchesToMigrate);
		
		Set<String> ac = TutorialL2Forwarding.switchesACPerGC.get(
				GC).keySet();
		
		List<String> candidates = new ArrayList<String>(ac);
			
		for (int i = 0; i < candidates.size(); i++) {
			String item = candidates.get(i);
				
			if (!item.endsWith("_UPC")) {
				candidates.remove(i);
				candidates.add(item);
			}
		}

		System.out.println(candidates);

		Iterator<String> iterator1 = candidates.iterator();

		// First, analyze ACs belonging to the same GC
		while (iterator1.hasNext()) {
			String candidate = iterator1.next();

			System.out.println(candidate);

			if (!failureParticipants.containsKey(candidate)) {
				Collection<Float> values = TutorialL2Forwarding.nodesLoadPerAC
						.get(candidate).values();

				System.out.println(values);

				float current_capacity_AC = 0f;

				for (float value : values) {
					current_capacity_AC = current_capacity_AC + value;
				}

				System.out.println(current_capacity_AC);

				float budget = (float) MAX_CAPACITY_AC - current_capacity_AC;

				System.out.println(budget);

				Iterator<String> iterator2 = switchesToMigrate.iterator();

				while (iterator2.hasNext()) {
					String node = iterator2.next();

					System.out.println(node);
					if (budget > TutorialL2Forwarding.nodesLoadPerAC
							.get(ACFail).get(node)) {
						iterator2.remove();

						System.out.println(switchesToMigrate);

						budget = budget
								- TutorialL2Forwarding.nodesLoadPerAC.get(
										ACFail).get(node);

						System.out.println(budget);

						if (!tupleAC_Nodes.containsKey(candidate)) {
							tupleAC_Nodes.put(candidate,
									new ArrayList<String>());
						}
						if (!tupleAC_Nodes.get(candidate).contains(node)) {
							tupleAC_Nodes.get(candidate).add(node);
							TutorialL2Forwarding.switchesACPerGC.get(GC).get(candidate).add(node);
							System.out.println(tupleAC_Nodes);
						}
					} else {
						break;
					}
				}
				if (switchesToMigrate.size() == 0) {
					// break switches_migrated;
					System.out.println(tupleAC_Nodes);
					return tupleAC_Nodes;
				}
			}
		}

		// Then, analyze ACs in other GCs

		Set<String> keyGC = TutorialL2Forwarding.switchesACPerGC.keySet();

		System.out.println(keyGC);

		Iterator<String> iterator3 = keyGC.iterator();

		while (iterator3.hasNext()) {
			String iterGC = iterator3.next();

			System.out.println(iterGC);

			if (iterGC.equals(GC)) {
				continue;
			}
			Set<String> newcandidateAC_GC = TutorialL2Forwarding.switchesACPerGC
					.get(iterGC).keySet();

			System.out.println(newcandidateAC_GC);

			Iterator<String> iterator4 = newcandidateAC_GC.iterator();

			while (iterator4.hasNext()) {
				String candidateAC = iterator4.next();

				System.out.println(candidateAC);

				if (!failureParticipants.containsKey(candidateAC)) {
					Collection<Float> values = TutorialL2Forwarding.nodesLoadPerAC
							.get(candidateAC).values();

					System.out.println(values);

					float current_capacity_AC = 0f;

					for (float value : values) {
						current_capacity_AC = current_capacity_AC + value;
					}

					System.out.println(current_capacity_AC);

					float budget = (float) MAX_CAPACITY_AC
							- current_capacity_AC;

					System.out.println(budget);

					Iterator<String> iterator5 = switchesToMigrate.iterator();

					while (iterator5.hasNext()) {
						String node = iterator5.next();

						System.out.println(node);

						if (budget > TutorialL2Forwarding.nodesLoadPerAC.get(
								ACFail).get(node)) {
							iterator5.remove();

							System.out.println(switchesToMigrate);

							budget = budget
									- TutorialL2Forwarding.nodesLoadPerAC.get(
											ACFail).get(node);

							System.out.println(budget);

							if (!tupleAC_Nodes.containsKey(candidateAC)) {
								tupleAC_Nodes.put(candidateAC,
										new ArrayList<String>());
							}
							if (!tupleAC_Nodes.get(candidateAC).contains(node)) {
								tupleAC_Nodes.get(candidateAC).add(node);
								TutorialL2Forwarding.switchesACPerGC
										.get(iterGC).get(candidateAC).add(node);
								System.out.println(tupleAC_Nodes);
							}
						} else {
							break;
						}
					}
					if (switchesToMigrate.size() == 0) {
						// break switches_migrated;
						System.out.println(tupleAC_Nodes);
						return tupleAC_Nodes;
					}
				}
			}
		}
		System.out.println(tupleAC_Nodes);
		System.setOut(originalOut);
		return tupleAC_Nodes;
	}
	
	/*
	 * private static List<String> SelectSwitchToMigrate( String
	 * overloadedcontroller, double arrivalRate) {
	 * 
	 * Map<String, Long> candidateSwitchesToMigrate =
	 * TutorialL2Forwarding.packetInNodesPerAC .get(overloadedcontroller);
	 * 
	 * Set<String> keySwitches = candidateSwitchesToMigrate.keySet();
	 * 
	 * Map<String, Double> arrivalRatePerNode = new HashMap<String, Double>();
	 * 
	 * for (Iterator<String> iterator = keySwitches.iterator(); iterator
	 * .hasNext();) { double arrivalRateNode =
	 * (deltaTimeArrivalPacketNodesPerAC.get(
	 * overloadedcontroller).get(iterator.next()) == 0) ? 0 :
	 * candidateSwitchesToMigrate.get(iterator.next()) /
	 * deltaTimeArrivalPacketNodesPerAC.get(
	 * overloadedcontroller).get(iterator.next());
	 * arrivalRatePerNode.put(iterator.next(), arrivalRateNode); }
	 * 
	 * Collection<Double> rateValues = arrivalRatePerController.values();
	 * 
	 * double sum = 0; for (double value : rateValues) { sum += value; }
	 * 
	 * double aveArrivalRatePerAC = sum / rateValues.size();
	 * 
	 * double target = arrivalRate - aveArrivalRatePerAC;
	 * 
	 * List<String> switchesToMigrate = (List<String>)
	 * candidateSwitchesToMigrate;
	 * 
	 * return switchesToMigrate; }
	 */

	private static String TranslateFormatControllerId(String nodeid) {
		String str = "";
		for (int i = 0; i < nodeid.length(); i++) {
			if (nodeid.substring(9) == "0" || nodeid.substring(9).length() > 8) {
				str = "s0";
			} else {
				str = "s" + nodeid.substring(9);
			}
		}
		return str;
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
	
	public static void migrateSwitches(Map<String, List<String>> map) {
		
		PrintStream originalOut = System.out;
		
		PrintStream fileOut = null;
		try {
			fileOut = new PrintStream(new FileOutputStream("./commands.txt", true), true);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		System.setOut(fileOut);

		Set<String> keyAC = map.keySet();

		Iterator<String> iterator1 = keyAC.iterator();

		while (iterator1.hasNext()) {
			String AC = iterator1.next();
			List<String> ips = ipLocalandPublicPerController.get(AC);
			List<String> nodes = map.get(AC);

			if (AC.endsWith("_UPC")) {
				String ipLocal = ips.get(0);
				Iterator<String> iterator2 = nodes.iterator();

				while (iterator2.hasNext()) {
					System.out.println("Commands to migrate:" + "\n"
							+ "sudo ovs-vsctl del-controller "
							+ iterator2.next());
				}

				Iterator<String> iterator3 = nodes.iterator();
				while (iterator3.hasNext()) {
					System.out.println("\n" + "sudo ovs-vsctl set-controller "
							+ iterator3.next() + " tcp:" + ipLocal + ":6633");
				}
			} else {
				String ipPublic = ips.get(1);
				Iterator<String> iterator2 = nodes.iterator();

				while (iterator2.hasNext()) {
					System.out.println("Commands to migrate:" + "\n"
							+ "sudo ovs-vsctl del-controller "
							+ iterator2.next());
				}

				Iterator<String> iterator3 = nodes.iterator();
				while (iterator3.hasNext()) {
					System.out.println("\n" + "sudo ovs-vsctl set-controller "
							+ iterator3.next() + " tcp:" + ipPublic + ":6633");
				}
			}
		}
		
		System.setOut(originalOut);
	}

	/*
	public static void checkFailoverOwnACs(String ipControllerFail,
			String ipControllerTarget) throws UnknownHostException {
		String controllerfail = ipControllerFail;
		String controllertarget = ipControllerTarget;
		List<String> switchList = TutorialL2Forwarding.switchesPerController
				.get(controllerfail);
		for (int i = 0; i < switchList.size(); i++) {
			try {
				if (controllerfail != null) {
					Process p = Runtime.getRuntime().exec(
							"sudo ovs-vsctl -- get Controller "
									+ switchList.get(i) + " is_connected");
					BufferedReader newInput = new BufferedReader(
							new InputStreamReader(p.getInputStream()));
					String newInputString = newInput.readLine();
					System.out.println("IncomingCmd::checkFailover():"); // Is
																			// " + switchList.get(i) + "
																			// connected
																			// to
																			// current
																			// controller?
																			// "
																			// +
																			// newInputString);
					if (newInputString == null) {
						p = Runtime.getRuntime().exec(
								"sudo ovs-vsctl del-controller "
										+ switchList.get(i));
						Thread.sleep(1000);
						p = Runtime.getRuntime().exec(
								"sudo ovs-vsctl set-controller "
										+ switchList.get(i) + " tcp:"
										+ controllertarget + ":6633");
						System.out
								.println("ExecutingCmd::ovs-vsctl set-controller");
					}
				}
			} catch (IOException e) {
				System.out
						.println("IncomingCmd::checkFailover(): Error executing commands for switch "
								+ switchList.get(i));
			} catch (InterruptedException ie) {
				System.out
						.println("IncomingCmd::checkFailover(): Error with thread sleep");
			}
		}
	}

	public static void checkFailoverOtherACs(String ipControllerFail,
			String ipControllerTarget, String nameGCOwner, String nameACFail)
			throws UnknownHostException {
		String controllerfail = ipControllerFail;
		String controllertarget = ipControllerTarget;
		List<String> switchList = TutorialL2Forwarding.switchesACperGC.get(
				nameGCOwner).get(nameACFail);
		for (int i = 0; i < switchList.size(); i++) {
			try {
				if (controllerfail != null) {
					Process p = Runtime.getRuntime().exec(
							"sudo ovs-vsctl -- get Controller "
									+ switchList.get(i) + " is_connected");
					BufferedReader newInput = new BufferedReader(
							new InputStreamReader(p.getInputStream()));
					String newInputString = newInput.readLine();
					System.out.println("IncomingCmd::checkFailover():"); // Is
																			// " + switchList.get(i) + "
																			// connected
																			// to
																			// current
																			// controller?
																			// "
																			// +															// newInputString);
					if (newInputString == null) {
						p = Runtime.getRuntime().exec(
								"sudo ovs-vsctl del-controller "
										+ switchList.get(i));
						Thread.sleep(1000);
						p = Runtime.getRuntime().exec(
								"sudo ovs-vsctl set-controller "
										+ switchList.get(i) + " tcp:"
										+ controllertarget + ":6633");
						System.out
								.println("ExecutingCmd::ovs-vsctl set-controller");
					}
				}
			} catch (IOException e) {
				System.out
						.println("IncomingCmd::checkFailover(): Error executing commands for switch "
								+ switchList.get(i));
			} catch (InterruptedException ie) {
				System.out
						.println("IncomingCmd::checkFailover(): Error with thread sleep");
			}
		}
	}
	*/

	/*
	 * private void floodingPacket(NodeConnectorRef ingressNodeConnectorRef,
	 * byte[] payload) { LOG.debug("ENTERING floodingPacket"); NodeRef
	 * ingressNodeRef = InventoryUtils.getNodeRef(ingressNodeConnectorRef);
	 * NodeConnectorId ingressNodeConnectorId =
	 * InventoryUtils.getNodeConnectorId(ingressNodeConnectorRef); NodeId
	 * ingressNodeId = InventoryUtils.getNodeId(ingressNodeConnectorRef); String
	 * ingressNodeConnectorIdStr = ingressNodeConnectorId.getValue(); NodeKey
	 * ingressNodeKey = new NodeKey(ingressNodeId);
	 * 
	 * InstanceIdentifier<Node> nodeIdentifier =
	 * InstanceIdentifier.builder(Nodes.class).
	 * child(Node.class,ingressNodeKey).toInstance(); Node ingressNode =
	 * GenericTransactionUtils.readData(dataBroker,
	 * LogicalDatastoreType.OPERATIONAL, nodeIdentifier);
	 * 
	 * if (ingressNode != null) { List<NodeConnector> nodeConnectorList =
	 * ingressNode.getNodeConnector();
	 * LOG.debug(" SWITCH with {} connectors in floodingPacket",
	 * nodeConnectorList.size());
	 * 
	 * for (NodeConnector nodeConnector : nodeConnectorList) { NodeConnectorId
	 * nodeConnectorId = nodeConnector.getId(); String nodeConnectorIdStr =
	 * nodeConnectorId.getValue(); NodeConnectorRef nodeConnectorRef =
	 * InventoryUtils.getNodeConnectorRef(nodeConnectorId);
	 * LOG.debug(" TESTING connector {} in floodingPacket", nodeConnectorIdStr);
	 * 
	 * if (!nodeConnectorIdStr.equals(ingressNodeConnectorIdStr) &&
	 * !nodeConnectorIdStr.contains("LOCAL")) {
	 * LOG.debug(" FLOODING port by port, sent to port {}", nodeConnectorIdStr);
	 * packetOut(ingressNodeRef, nodeConnectorRef, payload); } } } }
	 * 
	 * private void packetOut(NodeRef egressNodeRef, NodeConnectorRef
	 * egressNodeConnectorRef, byte[] payload) {
	 * Preconditions.checkNotNull(packetProcessingService);
	 * LOG.debug("Flooding packet of size {} out of port {}", payload.length,
	 * egressNodeConnectorRef);
	 * 
	 * //Construct input for RPC call to packet processing service
	 * TransmitPacketInput input = new TransmitPacketInputBuilder()
	 * .setPayload(payload) .setNode(egressNodeRef)
	 * .setEgress(egressNodeConnectorRef) .build();
	 * packetProcessingService.transmitPacket(input); }
	 * 
	 * private void programL2Flow(NodeId nodeId, String dstMac, NodeConnectorId
	 * ingressNodeConnectorId, NodeConnectorId egressNodeConnectorId) {
	 * 
	 * /* Programming a flow involves: 1. Creating a Flow object that has a
	 * match and a list of instructions, 2. Adding Flow object as an
	 * augmentation to the Node object in the inventory. 3. FlowProgrammer
	 * module of OpenFlowPlugin will pick up this data change and eventually
	 * program the switch.
	 * 
	 * 
	 * //Creating match object MatchBuilder matchBuilder = new MatchBuilder();
	 * MatchUtils.createEthDstMatch(matchBuilder, new MacAddress(dstMac), null);
	 * // MatchUtils.createInPortMatch(matchBuilder, ingressNodeConnectorId);
	 * 
	 * // Instructions List Stores Individual Instructions InstructionsBuilder
	 * isb = new InstructionsBuilder(); List<Instruction> instructions =
	 * Lists.newArrayList(); InstructionBuilder ib = new InstructionBuilder();
	 * ApplyActionsBuilder aab = new ApplyActionsBuilder(); ActionBuilder ab =
	 * new ActionBuilder(); List<Action> actionList = Lists.newArrayList();
	 * 
	 * // Set output action OutputActionBuilder output = new
	 * OutputActionBuilder();
	 * output.setOutputNodeConnector(egressNodeConnectorId);
	 * output.setMaxLength(65535); //Send full packet and No buffer
	 * ab.setAction(new
	 * OutputActionCaseBuilder().setOutputAction(output.build()).build());
	 * ab.setOrder(0); ab.setKey(new ActionKey(0)); actionList.add(ab.build());
	 * 
	 * // Create Apply Actions Instruction aab.setAction(actionList);
	 * ib.setInstruction(new
	 * ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
	 * ib.setOrder(0); ib.setKey(new InstructionKey(0));
	 * instructions.add(ib.build());
	 * 
	 * // Create Flow FlowBuilder flowBuilder = new FlowBuilder();
	 * flowBuilder.setMatch(matchBuilder.build());
	 * 
	 * String flowId = "L2_Rule_" + dstMac; flowBuilder.setId(new
	 * FlowId(flowId)); FlowKey key = new FlowKey(new FlowId(flowId));
	 * flowBuilder.setBarrier(true); flowBuilder.setTableId((short)0);
	 * flowBuilder.setKey(key); flowBuilder.setPriority(32768);
	 * flowBuilder.setFlowName(flowId); flowBuilder.setHardTimeout(0);
	 * flowBuilder.setIdleTimeout(0);
	 * flowBuilder.setInstructions(isb.setInstruction(instructions).build());
	 * 
	 * InstanceIdentifier<Flow> flowIID =
	 * InstanceIdentifier.builder(Nodes.class) .child(Node.class, new
	 * NodeKey(nodeId)) .augmentation(FlowCapableNode.class) .child(Table.class,
	 * new TableKey(flowBuilder.getTableId())) .child(Flow.class,
	 * flowBuilder.getKey()) .build();
	 * GenericTransactionUtils.writeData(dataBroker,
	 * LogicalDatastoreType.CONFIGURATION, flowIID, flowBuilder.build(), true);
	 * 
	 * 
	 * }
	 */

	private void programL2FlowOperational(NodeId nodeId, String dstMac,
			NodeConnectorId ingressNodeConnectorId,
			NodeConnectorId egressNodeConnectorId) {

		/*
		 * Programming a flow involves: 1. Creating a Flow object that has a
		 * match and a list of instructions, 2. Adding Flow object as an
		 * augmentation to the Node object in the inventory. 3. FlowProgrammer
		 * module of OpenFlowPlugin will pick up this data change and eventually
		 * program the switch.
		 */

		// Creating match object
		MatchBuilder matchBuilder = new MatchBuilder();
		MatchUtils
				.createEthDstMatch(matchBuilder, new MacAddress(dstMac), null);
		// MatchUtils.createInPortMatch(matchBuilder, ingressNodeConnectorId);

		// Instructions List Stores Individual Instructions
		InstructionsBuilder isb = new InstructionsBuilder();
		List<Instruction> instructions = Lists.newArrayList();
		InstructionBuilder ib = new InstructionBuilder();
		ApplyActionsBuilder aab = new ApplyActionsBuilder();
		ActionBuilder ab = new ActionBuilder();
		List<Action> actionList = Lists.newArrayList();

		// Set output action
		OutputActionBuilder output = new OutputActionBuilder();
		output.setOutputNodeConnector(egressNodeConnectorId);
		output.setMaxLength(65535); // Send full packet and No buffer
		ab.setAction(new OutputActionCaseBuilder().setOutputAction(
				output.build()).build());
		ab.setOrder(0);
		ab.setKey(new ActionKey(0));
		actionList.add(ab.build());

		// Create Apply Actions Instruction
		aab.setAction(actionList);
		ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(
				aab.build()).build());
		ib.setOrder(0);
		ib.setKey(new InstructionKey(0));
		instructions.add(ib.build());

		// Create Flow
		FlowBuilder flowBuilder = new FlowBuilder();
		flowBuilder.setMatch(matchBuilder.build());

		String flowId = "L2_Rule_" + dstMac;
		flowBuilder.setId(new FlowId(flowId));
		FlowKey key = new FlowKey(new FlowId(flowId));
		flowBuilder.setBarrier(true);
		flowBuilder.setTableId((short) 0);
		flowBuilder.setKey(key);
		flowBuilder.setPriority(32768);
		flowBuilder.setFlowName(flowId);
		flowBuilder.setHardTimeout(0);
		flowBuilder.setIdleTimeout(0);
		flowBuilder.setInstructions(isb.setInstruction(instructions).build());

		InstanceIdentifier<Flow> flowIID = InstanceIdentifier
				.builder(Nodes.class).child(Node.class, new NodeKey(nodeId))
				.augmentation(FlowCapableNode.class)
				.child(Table.class, new TableKey(flowBuilder.getTableId()))
				.child(Flow.class, flowBuilder.getKey()).build();
		GenericTransactionUtils.writeData(dataBroker,
				LogicalDatastoreType.OPERATIONAL, flowIID, flowBuilder.build(),
				true);

	}
}
