# Intelligent_Controller
This repository implements the intelligent controller logic. It implements a DDS mechanism to exchange network information with other entities. Additionally, it implements a DQN algorithm to deploy VNFs in multiple-cluster domains.

The implemented package of a DQN algorithm uses Deep Java Library. It also incorporates the DDS implementation in case of using it without an SDN controller. Thus, it can communicate with other elements through the DDS.<br/>  [rlalgo/src/main/java/com/rlresallo/](rlalgo/src/main/java/com/rlresallo/)

In case of running the **rlalgo package** independent of an SDN controller are necessary some steps:
- Compile the package using the following command: 
  
  mvn clean install -nsu -DskipTests -e
- Export environment variables used by the application:

  export NDDSHOME=/path-to-rti-directory/rti_connext_dds-5.2.3/
  
  export LD_LIBRARY_PATH=/path-to-rti-directory/rti_connext_dds-5.2.3/lib/x64Linux3gcc4.8.2/:/path-to-djl-engine-directory/.djl.ai/mxnet/1.9.0-mkl-linux-x86_64/
  
  export RTI_LICENSE_FILE=/path-to-rti-directory/rti_connext_dds-5.2.3/rti_license.dat
  
  export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/
- Execute the application:

  mvn exec:java -Dexec.mainClass="com.rlresallo.ODLBrain"
