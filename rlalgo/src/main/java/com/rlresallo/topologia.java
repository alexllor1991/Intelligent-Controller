

/*
WARNING: THIS FILE IS AUTO-GENERATED. DO NOT MODIFY.

This file was generated from .idl using "rtiddsgen".
The rtiddsgen tool is part of the RTI Connext distribution.
For more information, type 'rtiddsgen -help' at a command shell
or consult the RTI Connext manual.
*/

package com.rlresallo;

import com.rti.dds.infrastructure.*;
import com.rti.dds.infrastructure.Copyable;
import java.io.Serializable;
import com.rti.dds.cdr.CdrHelper;

public class topologia   implements Copyable, Serializable{

    public String Identificador=  "" ; /* maximum length = ((org.sdnhub.odl.tutorial.learningswitch.impl.MAX_SIZE.VALUE)) */
    public String NodeId=  "" ; /* maximum length = ((org.sdnhub.odl.tutorial.learningswitch.impl.MAX_SIZE.VALUE)) */
    public String TerminationPointId=  "" ; /* maximum length = ((org.sdnhub.odl.tutorial.learningswitch.impl.MAX_SIZE.VALUE)) */
    public String LinkId=  "" ; /* maximum length = ((org.sdnhub.odl.tutorial.learningswitch.impl.MAX_SIZE.VALUE)) */
    public String SourceNode=  "" ; /* maximum length = ((org.sdnhub.odl.tutorial.learningswitch.impl.MAX_SIZE.VALUE)) */
    public String SourceNodeTp=  "" ; /* maximum length = ((org.sdnhub.odl.tutorial.learningswitch.impl.MAX_SIZE.VALUE)) */
    public String DestinationNode=  "" ; /* maximum length = ((org.sdnhub.odl.tutorial.learningswitch.impl.MAX_SIZE.VALUE)) */
    public String DestinationNodeTp=  "" ; /* maximum length = ((org.sdnhub.odl.tutorial.learningswitch.impl.MAX_SIZE.VALUE)) */

    public topologia() {

    }
    public topologia (topologia other) {

        this();
        copy_from(other);
    }

    public static Object create() {

        topologia self;
        self = new  topologia();
        self.clear();
        return self;

    }

    public void clear() {

        Identificador=  ""; 
        NodeId=  ""; 
        TerminationPointId=  ""; 
        LinkId=  ""; 
        SourceNode=  ""; 
        SourceNodeTp=  ""; 
        DestinationNode=  ""; 
        DestinationNodeTp=  ""; 
    }

    public boolean equals(Object o) {

        if (o == null) {
            return false;
        }        

        if(getClass() != o.getClass()) {
            return false;
        }

        topologia otherObj = (topologia)o;

        if(!Identificador.equals(otherObj.Identificador)) {
            return false;
        }
        if(!NodeId.equals(otherObj.NodeId)) {
            return false;
        }
        if(!TerminationPointId.equals(otherObj.TerminationPointId)) {
            return false;
        }
        if(!LinkId.equals(otherObj.LinkId)) {
            return false;
        }
        if(!SourceNode.equals(otherObj.SourceNode)) {
            return false;
        }
        if(!SourceNodeTp.equals(otherObj.SourceNodeTp)) {
            return false;
        }
        if(!DestinationNode.equals(otherObj.DestinationNode)) {
            return false;
        }
        if(!DestinationNodeTp.equals(otherObj.DestinationNodeTp)) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int __result = 0;
        __result += Identificador.hashCode(); 
        __result += NodeId.hashCode(); 
        __result += TerminationPointId.hashCode(); 
        __result += LinkId.hashCode(); 
        __result += SourceNode.hashCode(); 
        __result += SourceNodeTp.hashCode(); 
        __result += DestinationNode.hashCode(); 
        __result += DestinationNodeTp.hashCode(); 
        return __result;
    }

    /**
    * This is the implementation of the <code>Copyable</code> interface.
    * This method will perform a deep copy of <code>src</code>
    * This method could be placed into <code>topologiaTypeSupport</code>
    * rather than here by using the <code>-noCopyable</code> option
    * to rtiddsgen.
    * 
    * @param src The Object which contains the data to be copied.
    * @return Returns <code>this</code>.
    * @exception NullPointerException If <code>src</code> is null.
    * @exception ClassCastException If <code>src</code> is not the 
    * same type as <code>this</code>.
    * @see com.rti.dds.infrastructure.Copyable#copy_from(java.lang.Object)
    */
    public Object copy_from(Object src) {

        topologia typedSrc = (topologia) src;
        topologia typedDst = this;

        typedDst.Identificador = typedSrc.Identificador;
        typedDst.NodeId = typedSrc.NodeId;
        typedDst.TerminationPointId = typedSrc.TerminationPointId;
        typedDst.LinkId = typedSrc.LinkId;
        typedDst.SourceNode = typedSrc.SourceNode;
        typedDst.SourceNodeTp = typedSrc.SourceNodeTp;
        typedDst.DestinationNode = typedSrc.DestinationNode;
        typedDst.DestinationNodeTp = typedSrc.DestinationNodeTp;

        return this;
    }

    public String toString(){
        return toString("", 0);
    }

    public String toString(String desc, int indent) {
        StringBuffer strBuffer = new StringBuffer();        

        if (desc != null) {
            CdrHelper.printIndent(strBuffer, indent);
            strBuffer.append(desc).append(":\n");
        }

        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("Identificador: ").append(Identificador).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("NodeId: ").append(NodeId).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("TerminationPointId: ").append(TerminationPointId).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("LinkId: ").append(LinkId).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("SourceNode: ").append(SourceNode).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("SourceNodeTp: ").append(SourceNodeTp).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("DestinationNode: ").append(DestinationNode).append("\n");  
        CdrHelper.printIndent(strBuffer, indent+1);        
        strBuffer.append("DestinationNodeTp: ").append(DestinationNodeTp).append("\n");  

        return strBuffer.toString();
    }

}
