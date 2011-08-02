/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jmeter.protocol.amf.sampler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;

import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import com.thoughtworks.xstream.XStream;

import flex.messaging.io.ClassAliasRegistry;
import flex.messaging.io.MessageDeserializer;
import flex.messaging.io.SerializationContext;
import flex.messaging.io.amf.ActionContext;
import flex.messaging.io.amf.ActionMessage;
import flex.messaging.io.amf.AmfMessageDeserializer;
import flex.messaging.io.amf.AmfMessageSerializer;
import flex.messaging.io.amf.MessageBody;
import flex.messaging.io.amf.MessageHeader;
import flex.messaging.io.amf.client.AMFConnection;
import flex.messaging.io.amf.client.AMFHeaderProcessor;
import flex.messaging.io.amf.client.exceptions.ClientStatusException;
import flex.messaging.io.amf.client.exceptions.ServerStatusException;
import flex.messaging.io.amf.client.exceptions.ServerStatusException.HttpResponseInfo;
import flex.messaging.messages.AcknowledgeMessageExt;
import flex.messaging.messages.AsyncMessageExt;
import flex.messaging.messages.CommandMessageExt;

/**
 * An extension to flex.messaging.io.amf.client.AMFConnection that uses inherited methods
 * for serializing/deserializing AMF messages to/from a byte array.  It is expected
 * that the caller of this class uses an external connection class, as opposed to the
 * internal URLConnection within AMFConnection, to actually send/receive the messages.
 * 
 */
public class AmfMessageHelper extends AMFConnection {

    //--------------------------------------------------------------------------
    // Static Initializer
    //--------------------------------------------------------------------------
	
	static {
		// Register known Endpoint aliases to prevent ClassNotFoundException's during
		// response message deserialization.
		ClassAliasRegistry aliasRegistry = ClassAliasRegistry.getRegistry();
		aliasRegistry.registerAlias(AcknowledgeMessageExt.CLASS_ALIAS, "flex.messaging.messages.AcknowledgeMessageExt");
		aliasRegistry.registerAlias(CommandMessageExt.CLASS_ALIAS, "flex.messaging.messages.CommandMessageExt");
		aliasRegistry.registerAlias(AsyncMessageExt.CLASS_ALIAS, "flex.messaging.messages.AsyncMessageExt");
	}

    //--------------------------------------------------------------------------
    // Private Static Variables
    //--------------------------------------------------------------------------

    private static final Logger log = LoggingManager.getLoggerForClass();

    //--------------------------------------------------------------------------
    // Private Variables
    //--------------------------------------------------------------------------

    private ActionContext actionContext = null;
    private SerializationContext serializationContext = null;
    private String command = "";
    private int responseCode = -1;
    private String responseMessage = "";

    //--------------------------------------------------------------------------
    // Protected Variables
    //--------------------------------------------------------------------------

    /**
     * List of AMF message headers.
     */
    protected List<MessageHeader> amfHeaders;

    /**
     * An AMF connection may have an AMF header processor where AMF headers
     * can be passed to as they are encountered in AMF response messages.
     */
    protected AMFHeaderProcessor amfHeaderProcessor;

    /**
     * The XStream instance used for converting an object to an XML string.
     */
	protected XStream stream = null;

    //--------------------------------------------------------------------------
    //
    // Constructor
    //
    //--------------------------------------------------------------------------

    /**
     * Creates a default AMF Message Helper instance.
     */
    public AmfMessageHelper() {
    	// Initialize response counter to 1 for compatibility with Flourine-Fx
    	responseCounter = 1;
    }
	
	public void setResponseCode(String responseCode) {
		this.responseCode = -1;
		if (responseCode != null && responseCode.length() > 0) {
			try {
				Integer rc = Integer.valueOf(responseCode);
				this.responseCode = rc.intValue();
			} catch (NumberFormatException ex) {
				this.responseCode = -1;
			}
		}
	}

	/**
	 * @return the stream
	 */
	public XStream getXStream() {
		return stream;
	}

	/**
	 * @param stream the stream to set
	 */
	public void setXStream(XStream stream) {
		this.stream = stream;
	}

	/**
	 * @return the responseMessage
	 */
	public String getResponseMessage() {
		return responseMessage;
	}

	/**
	 * @param responseMessage the responseMessage to set
	 */
	public void setResponseMessage(String responseMessage) {
		this.responseMessage = responseMessage;
	}

	/**
	 * @return the responseCode
	 */
	public int getResponseCode() {
		return responseCode;
	}

    /**
     * Initialize the required contexts for processing an AMF message. 
     * 
     */
	public void open() {
        serializationContext = new SerializationContext();
        serializationContext.createASObjectForMissingType = true;
        serializationContext.instantiateTypes = true;
        actionContext = new ActionContext();
	}
	
	/**
	 * Serialize an AMF AbstractMessage in preparation for sending it to a remote server using
	 * an external transport mechanism.
	 * 
	 * @param message an Object of type flex.messaging.messages.AbstractMessage.
	 * NOTE: The underlying serialization logic expects to process an object array of message body
	 * data, thus the declaration of a variable number of Object input parameters, but the current
	 * implementation follows the current limitation of AMFConnection of only supporting
	 * one message per request.
	 * 
	 * @return A serialized object of type flex.messaging.io.amf.ActionMessage 
	 * as a byte array.
	 */
	public byte[] serializeMessage(Object... message) {

    	ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();

    	if (message != null) {
    		
			String responseURI = getResponseURI();
			
	        ActionMessage requestMessage = new ActionMessage(getObjectEncoding());
	
	        if (amfHeaders != null)
	        {
	            for (MessageHeader header : amfHeaders)
	                requestMessage.addHeader(header);
	        }
	
	        MessageBody amfMessage = new MessageBody(command, responseURI, message);
	        requestMessage.addBody(amfMessage);
	
	        // Setup for AMF message serializer
	        actionContext.setRequestMessage(requestMessage);
	        
	        AmfMessageSerializer amfMessageSerializer = new AmfMessageSerializer();
	        amfMessageSerializer.initialize(serializationContext, outBuffer, null/*debugTrace*/);
	        
	        try {
				amfMessageSerializer.writeMessage(requestMessage);
			} catch (IOException e) {
	            // ClientStatusException exception = new ClientStatusException(e, ClientStatusException.AMF_CALL_FAILED_CODE);
	            // throw exception;
			}
			
    	}
    	
    	byte[] byteArray = outBuffer.toByteArray(); 

        if (getLogger().isDebugEnabled()) {
	        String temp = new String(byteArray);
	       	getLogger().debug(whoAmI()+"\tAMF Request byte[] [\n"+temp+"\n]");
        }
        
		return byteArray;
	}

	/**
	 * Deserialize a byte array, representing an AMF ActionMessage received
	 * as a response to a remote server request, and return the data object 
	 * that is found within the processed message body.  Note that the current
	 * implementation of AMFConnection returns the data object for the first
	 * message in a multi-message response.
	 * 
	 * @param byteArray A byte array of the message to be processed.
	 *  
	 * @return The data object found within the processed message body.
	 * 
	 * @throws ClassNotFoundException thrown if a required class can not be found
	 * during the serialization of the input byte array.
	 * @throws IOException thrown if an error is encountered while deserializing
	 * the input byte array.
	 * @throws ClientStatusException thrown if an error is encountered while
	 * processing any AMF message headers.
	 * @throws ServerStatusException thrown if the server returns a status
	 * message instead of a result message.
	 */
    public Object getDataObjectFromMessage(byte[] byteArray)
            throws ClassNotFoundException, IOException, ClientStatusException,
            ServerStatusException
    {
        if (getLogger().isDebugEnabled()) {
	        String temp = new String(byteArray);
	       	getLogger().debug(whoAmI()+"\tAMF Response byte[] [\n"+temp+"\n]");
        }
        ByteArrayInputStream bin = new ByteArrayInputStream(byteArray); 
        DataInputStream din = new DataInputStream(bin);
        ActionMessage message = new ActionMessage();
        actionContext.setRequestMessage(message);
        MessageDeserializer deserializer = new AmfMessageDeserializer();
        deserializer.initialize(serializationContext, din, null/*trace*/);
        try {
        	deserializer.readMessage(message, actionContext);
        } catch (Exception ex) {
        	getLogger().error("An exception was encountered while deserializing response. ", ex);
        }
        din.close();
        return processAmfPacket(message);
    }

    /**
     * Release/close contexts required to process an AMF message. 
     * 
     */
	public void close() {
        serializationContext = null;
	}

    /**
     * Generates the HTTP response info for the server status exception.
     *
     * @return The HTTP response info for the server status exception.
     */
    protected HttpResponseInfo generateHttpResponseInfo()
    {
        HttpResponseInfo httpResponseInfo = null;
        int responseCode = getResponseCode();
        String responseMessage = getResponseMessage();
        httpResponseInfo = new HttpResponseInfo(responseCode, responseMessage);
        return httpResponseInfo;
    }

    /**
     * Get a Logger instance which can be used by subclasses to log information.
     * This is the same Logger which is used by the base JavaSampler classes
     * (jmeter.protocol.java).
     *
     * @return a Logger instance which can be used for logging
     */
    protected Logger getLogger() {
        return log;
    }

    /**
     * Generate a String identifier of this test for debugging purposes.
     *
     * @return a String identifier for this test instance
     */
    protected String whoAmI() {
        StringBuilder sb = new StringBuilder();
        sb.append(Thread.currentThread().toString());
        sb.append("@");
        sb.append(Integer.toHexString(hashCode()));
        return sb.toString();
    }

}
