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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.jmeter.config.Arguments;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import flex.messaging.messages.AbstractMessage;
import flex.messaging.messages.Message;
import flex.messaging.messages.RemotingMessage;

/**
 * This class extends AmfMessage, providing a concrete implementation of the createAbstractMessage()
 * method, returning an AMF RemotingMessage populated with variables defined for the AmfSampler
 * test instance.
 * <p>
 * Most custom implementations of this interface will want to extend this class when it is required
 * to send/receive messages of this type.  Custom implementations might, for example, override
 * createAbstractMessage(), providing application specific request parameters, prior to calling
 * super.createAbstractMessage().
 * 
 */
public class AmfRemotingMessage extends AmfMessage {

    private static final Logger log = LoggingManager.getLoggerForClass();

    private static final String OPERATION_PARAMETER_NAME = "Operation Name";
    private static final String OPERATION_PARAMETER_DEFAULT = "";

    private static final String AMF_INT_PARAMETER_PREFIX = "AMF_IntParameter_";
    private static final String AMF_LONG_PARAMETER_PREFIX = "AMF_LongParameter_";
    private static final String AMF_STRING_PARAMETER_PREFIX = "AMF_Parameter_";

    /**
     * The set of parameters that should be used for the specific remote call.
     */
    protected List<Object> amfParameters = new ArrayList<Object>();

    /**
     * The remote operation name.
     */
    protected String operationName = null;

	/**
     * Setup AMF Test.
     *
     * @param context
     *            the context to run with. This provides access to
     *            initialization parameters.
     */
    public void setupTest(AmfSamplerContext context) {
    	super.setupTest(context);
    	
        operationName = context.getParameter(OPERATION_PARAMETER_NAME, "");

        // Build AMF RemotingMessage parameters map from context parameters
		Map<Integer,Object> amfParameterMap = new HashMap<Integer,Object>();
        Iterator<String> parameterNames = context.getParameterNamesIterator();
        while (parameterNames.hasNext()) {
        	String parameterName = parameterNames.next();
        	if (parameterName.startsWith(AMF_INT_PARAMETER_PREFIX)) {
        		int index = getAMFParameterIndex(parameterName);
        		if (index >= 0) {
        			amfParameterMap.put(Integer.valueOf(index), context.getIntParameter(parameterName));
        		}
        	} else if (parameterName.startsWith(AMF_LONG_PARAMETER_PREFIX)) {
        		int index = getAMFParameterIndex(parameterName);
        		if (index >= 0) {
        			amfParameterMap.put(Integer.valueOf(index), context.getLongParameter(parameterName));
        		}
        	} else if (parameterName.startsWith(AMF_STRING_PARAMETER_PREFIX)) {
        		int index = getAMFParameterIndex(parameterName);
        		if (index >= 0) {
        			amfParameterMap.put(Integer.valueOf(index), context.getParameter(parameterName));
        		}
        	}
        }
        
        // Add sorted AMF parameters map entries to the AMF parameters list member variable
	    SortedSet<Integer> sortedset= new TreeSet<Integer>(amfParameterMap.keySet());
	    Iterator<Integer> iter = sortedset.iterator();
	    while (iter.hasNext()) {
    		amfParameters.add(amfParameterMap.get(iter.next()));
	    }
    }
    
    /**
     * Get the AMF call parameter index from the parameter name.
     * 
     * @param parameterName
     * @return The AMF call parameter index from the parameter name.
     */
    protected int getAMFParameterIndex(String parameterName) {
    	int index = -1;
    	int lastIndex = parameterName.lastIndexOf('_');
    	if (lastIndex != -1 && lastIndex+1 < parameterName.length()) {
    		try {
    			index = Integer.valueOf(parameterName.substring(lastIndex+1));
    		} catch (NumberFormatException ex) {
    			index = -1;
    		}
    	}
    	return index;
    }

	protected AbstractMessage createAbstractMessage(AmfSamplerContext context) {

		RemotingMessage message = new RemotingMessage();
        message.setSource(source);
        message.setDestination(destination);
        message.setMessageId(UUID.randomUUID().toString());
        message.setHeader(Message.ENDPOINT_HEADER, endpointId);
        message.setHeader(Message.FLEX_CLIENT_ID_HEADER, flexClientId);
        ((RemotingMessage)message).setOperation(operationName);
        ((RemotingMessage)message).setParameters(amfParameters);

		return message;
	}

    /**
     * Provide a list of parameters which this test supports.
     *
     * @return a specification of the parameters used by this test which should
     *         be listed in the GUI, or null if no parameters should be listed.
     */
    public Arguments getDefaultParameters() {
        Arguments params = getBaseDefaultParameters();
        params.addArgument(SOURCE_PARAMETER_NAME, SOURCE_PARAMETER_DEFAULT);
        params.addArgument(DESTINATION_PARAMETER_NAME, DESTINATION_PARAMETER_DEFAULT);
        params.addArgument(OPERATION_PARAMETER_NAME, OPERATION_PARAMETER_DEFAULT);
        return params;
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

}
