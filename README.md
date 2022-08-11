# Intelligent_Controller
This repository implements the intelligent controller logic. It implements a DDS mechanism to exchange network information with other controllers. Additionally, it implements a DQN algorithm to deploy VNFs in multiple Kubernetes clusters.

There are two folders: 
- One containing the global controller logic implemented in OpenDaylight distribution through a learning-switch package. It includes the DDS implementation to exchange network information with other controllers.<br/>  [learning-switch/implementation/src/main/java/org/sdnhub/odl/tutorial/learningswitch/impl/](learning-switch/implementation/src/main/java/org/sdnhub/odl/tutorial/learningswitch/impl/)
- Another containing the implemented package of a DQN algorithm using Deep Java Library. It also incorporates the DDS implementation in case of using it without an SDN controller. Thus, it can communicate with other elements through the DDS.<br/>  [rlalgo/src/main/java/com/rlresallo/](rlalgo/src/main/java/com/rlresallo/)
