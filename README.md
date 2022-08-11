# Intelligent_Controller
This repository implements the intelligent controller logic. It implements a DDS mechanism to exchange network information with other controllers. Additionally, it implements a DQN algorithm to deploy VNFs in multiple Kubernetes clusters.

There are two folders: 
- One containing the global controller logic implemented in OpenDaylight distribution through a learning-switch package.<br/>  [learning-switch/implementation/src/main/java/org/sdnhub/odl/tutorial/learningswitch/impl/](learning-switch/implementation/src/main/java/org/sdnhub/odl/tutorial/learningswitch/impl/)
- Another containing the implemented package of a DQN algorithm using Deep Java Library.<br/>  [rlalgo/src/main/java/com/rlresallo/](rlalgo/src/main/java/com/rlresallo/)
