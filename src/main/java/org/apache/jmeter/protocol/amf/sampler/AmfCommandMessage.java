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

import java.util.UUID;

import org.apache.jmeter.config.Arguments;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import flex.messaging.messages.AbstractMessage;
import flex.messaging.messages.CommandMessage;
import flex.messaging.messages.Message;

/**
 * This class extends AmfMessage, providing a concrete implementation of the createAbstractMessage()
 * method, returning an AMF CommandMessage populated with variables defined for the AmfSampler
 * test instance. 
 * <p>
 * Most custom implementations of this interface will want to extend this class when it is required
 * to send/receive messages of this type.   
 * 
 */
public class AmfCommandMessage extends AmfMessage {

    private static final Logger log = LoggingManager.getLoggerForClass();

    protected static final String MESSAGING_VERSION_PARAMETER_NAME = "DSMessagingVersion";
    protected static final String MESSAGING_VERSION_PARAMETER_DEFAULT = "1";

    protected static final Integer DEFAULT_MESSAGING_VERSION = 1;

    protected static final String COMMAND_MESSAGE_ID_PARAMETER_NAME = "Operation";
    protected static final String COMMAND_MESSAGE_ID_PARAMETER_DEFAULT = "";

    /**
     * The remote operation name.
     */
    protected Integer messagingVersion = null;

    /**
     * The remote operation name.
     */
    protected Integer commandOperation = null;

	/**
     * Setup AMF Test.
     *
     * @param context
     *            the context to run with. This provides access to
     *            initialization parameters.
     */
    public void setupTest(AmfSamplerContext context) {
    	super.setupTest(context);
    	
    	messagingVersion = context.getIntParameter(MESSAGING_VERSION_PARAMETER_NAME, DEFAULT_MESSAGING_VERSION);
    	
        commandOperation = context.getIntParameter(COMMAND_MESSAGE_ID_PARAMETER_NAME,
        		CommandMessage.UNKNOWN_OPERATION);
        
    }

	protected AbstractMessage createAbstractMessage(AmfSamplerContext context) {

		CommandMessage message = new CommandMessage();
		message.setDestination(destination);
		message.setMessageId(UUID.randomUUID().toString());
		message.setHeader(Message.FLEX_CLIENT_ID_HEADER, flexClientId);
		message.setHeader(CommandMessage.MESSAGING_VERSION, messagingVersion);
		((CommandMessage)message).setOperation(commandOperation);

		return message;
	}

    /**
     * Provide a list of parameters which this test supports.
     *
     * @return a specification of the parameters used by this test which should
     *         be listed in the GUI, or null if no parameters should be listed.
     */
    public Arguments getDefaultParameters() {
        Arguments params = new Arguments();
        params.addArgument(FLEX_CLIENT_ID_PARAMETER_NAME, FLEX_CLIENT_ID_PARAMETER_DEFAULT);
        params.addArgument(MESSAGING_VERSION_PARAMETER_NAME, MESSAGING_VERSION_PARAMETER_DEFAULT);
        params.addArgument(COMMAND_MESSAGE_ID_PARAMETER_NAME, COMMAND_MESSAGE_ID_PARAMETER_DEFAULT);
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
