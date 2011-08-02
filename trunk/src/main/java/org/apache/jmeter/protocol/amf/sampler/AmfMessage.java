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

import static org.apache.jmeter.protocol.amf.sampler.AmfRequestVariable.FLEX_CLIENT_ID_VARIABLE;
import static org.apache.jmeter.protocol.amf.sampler.AmfRequestVariable.LAST_OPERATION_SUCCEEDED_VARIABLE;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import com.thoughtworks.xstream.XStream;

import flex.messaging.io.amf.client.exceptions.ClientStatusException;
import flex.messaging.io.amf.client.exceptions.ServerStatusException;
import flex.messaging.messages.AbstractMessage;
import flex.messaging.messages.AcknowledgeMessage;
import flex.messaging.messages.AcknowledgeMessageExt;
import flex.messaging.messages.Message;

/**
 * An abstract implementation of AmfMessageInterface. This implementation provides 
 * default implementations of most of the methods in the interface, as well as some
 * convenience methods, in order to simplify development of AmfMessageInterface implementations.
 * <p>
 * Most custom implementations of this interface will want to extend either the
 * AmfCommandMessage or AmfRemotingMessage classes, which extend this class, and
 * provide concrete implementations of the createMessage() method.  Custom implementations
 * will also most likely providing application specific implementations the protected 
 * processResult() methods from this class.   
 * 
 */
public abstract class AmfMessage implements AmfMessageInterface {

    //--------------------------------------------------------------------------
    // Static Variables
    //--------------------------------------------------------------------------

    private static final Logger log = LoggingManager.getLoggerForClass();

    private static final String OBJECT_ENCODING_ID_PARAMETER_NAME = "Object Encoding Id";

    protected static final String FLEX_CLIENT_ID_PARAMETER_NAME = "DSId";
    protected static final String FLEX_CLIENT_ID_PARAMETER_DEFAULT = "";

    protected static final String DESTINATION_PARAMETER_NAME = "Destination";
    protected static final String DESTINATION_PARAMETER_DEFAULT = "";

    protected static final String SOURCE_PARAMETER_NAME = "Source";
    protected static final String SOURCE_PARAMETER_DEFAULT = "";

    private static final String ENDPOINT_PARAMETER_NAME = "Channel Definition Id";
    private static final String ENDPOINT_PARAMETER_DEFAULT = "my-amf";

    //--------------------------------------------------------------------------
    // Private Variables
    //--------------------------------------------------------------------------

    private int responseCode = -1;
    private String responseMessage = "";

    /**
     * JMeter variables are maintained through the thread context
     */ 
    private transient JMeterContext threadContext = null;

    //--------------------------------------------------------------------------
    // Protected Variables
    //--------------------------------------------------------------------------

    protected AmfMessageHelper amfMessageHelper = new AmfMessageHelper();

    /**
     * The Flex message source.
     */
    protected String source = "";

    /**
     * The Flex message destination.
     */
    protected String destination = "";

    /**
     * This field corresponds to the Flex channel definition id, or "DSEndpoint", message header
     */
    protected String endpointId = "";
    
    /**
     * This field corresponds to the Flex client id, or "DSId", message header
     */
    protected String flexClientId = null;

    /**
     * The XStream instance used for converting an object to an XML string.
     */
	protected XStream stream = new XStream();
	
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
     * Setup AMF Test.
     *
     * @param context
     *            the context to run with. This provides access to
     *            initialization parameters.
     */
    public void setupTest(AmfSamplerContext context) {
        getLogger().debug(whoAmI() + "\tsetupTest()");
        listParameters(context);

        flexClientId = context.getParameter(FLEX_CLIENT_ID_PARAMETER_NAME, "nil");
        	
        endpointId = context.getParameter(ENDPOINT_PARAMETER_NAME, "");
        
        source = context.getParameter(SOURCE_PARAMETER_NAME, "");
        
        destination = context.getParameter(DESTINATION_PARAMETER_NAME, "");
        
        amfMessageHelper.setObjectEncoding(context.getIntParameter(OBJECT_ENCODING_ID_PARAMETER_NAME,
        		AmfMessageHelper.getDefaultObjectEncoding()));
        
        amfMessageHelper.setXStream(stream);
    }
    
    public void open() {
    	amfMessageHelper.open();
    }

	public byte[] createRequest(AmfSamplerContext context) {
        AbstractMessage message = createAbstractMessage(context);
        if (getLogger().isDebugEnabled()) {
        	getLogger().debug(whoAmI()+"\tAMF Request [\n"+stream.toXML(message)+"\n]");
        }
        return amfMessageHelper.serializeMessage(message);
	}

	public void processResponse(SampleResult result) {
		
		byte[] httpResponse = result.getResponseData();
		
		if (httpResponse != null) {
			
			setResponseCode(result.getResponseCode());
	        setResponseMessage(result.getResponseMessage());
			
			Object amfResponse;
			try {
				amfResponse = amfMessageHelper.getDataObjectFromMessage(httpResponse);
		        if (getLogger().isDebugEnabled() && amfResponse != null) {
		        	getLogger().debug(whoAmI()+"\tAMF Response [\n"+stream.toXML(amfResponse)+"\n]");
		        }
	            // process result
	           	if (amfResponse != null && amfResponse instanceof AcknowledgeMessage) {
	           		processResult((AcknowledgeMessage)amfResponse);
	           	} else if (amfResponse != null && amfResponse instanceof AcknowledgeMessageExt) {
	                processResult((AcknowledgeMessageExt)amfResponse);
	           	} else if (result != null) {
	                getLogger().error(whoAmI() + "\tunexpected result type ["+result.getClass().getName()+"]");
	           	} else {
	                getLogger().error(whoAmI() + "\tnull result object.");
	           	}
			} catch (ClassNotFoundException ex) {
	            getLogger().error(whoAmI() + "\tClassNotFoundException thrown. ", ex);
			} catch (IOException ex) {
	            getLogger().error(whoAmI() + "\tIOException thrown. ", ex);
			} catch (ClientStatusException ex) {
	            getLogger().error(whoAmI() + "\tClientStatusException thrown. ", ex);
			} catch (ServerStatusException ex) {
	            getLogger().error(whoAmI() + "\tServerStatusException thrown. ", ex);
			}
			
		} else {
			
		}
		
	}
    
    public void close() {
    	amfMessageHelper.close();
    }

	protected abstract AbstractMessage createAbstractMessage(AmfSamplerContext context);

	/**
     * Process the AcknowledgeMessage instance received from the remote service call.
     * 
     * @param ackMessage
     */
    protected boolean processResult(AcknowledgeMessage ackMessage) {
    	boolean resultOK = true;
    	
		if (getLogger().isDebugEnabled())
			getLogger().debug(whoAmI()+"\t[\n"+stream.toXML(ackMessage)+"\n]");
		
		// Extract the Flex Client Id from the AMF Message Headers and save 
		// this to a JMeter variable so it can be used in subsequent messages.
   		Object flexClientId = ackMessage.getHeader(Message.FLEX_CLIENT_ID_HEADER);
   		if (flexClientId != null && flexClientId instanceof String) {
			putVariable(FLEX_CLIENT_ID_VARIABLE.getName(), (String)flexClientId);
   		} else {
   			resultOK = false;
   		}
   		
    	putVariable(LAST_OPERATION_SUCCEEDED_VARIABLE.getName(), resultOK);
   		return resultOK;
    }
    
    /**
     * Process the AcknowledgeMessageExt instance received from the remote service call.
     * 
     * @param ackMessage
     */
    protected boolean processResult(AcknowledgeMessageExt ackMessage) {
    	boolean resultOK = true;
    	
		if (getLogger().isDebugEnabled())
			getLogger().debug(whoAmI()+"\t[\n"+stream.toXML(ackMessage)+"\n]");
		
		// Extract the Flex Client Id from the AMF Message Headers and save 
		// this to a JMeter variable so it can be used in subsequent messages.
   		Object flexClientId = ackMessage.getHeader(Message.FLEX_CLIENT_ID_HEADER);
   		if (flexClientId != null && flexClientId instanceof String) {
			putVariable(FLEX_CLIENT_ID_VARIABLE.getName(), (String)flexClientId);
   		} else {
   			resultOK = false;
   		}
   		
    	putVariable(LAST_OPERATION_SUCCEEDED_VARIABLE.getName(), resultOK);
   		return resultOK;
    }

    /**
     * Provide a list of parameters which this test supports.
     *
     * @return a specification of the parameters used by this test which should
     *         be listed in the GUI, or null if no parameters should be listed.
     */
    public abstract Arguments getDefaultParameters();

    /**
     * Provide a list of non-remoting service specific parameters which tests
     * that extend this class can use as common parameters.
     *
     * @return a specification of the parameters used by this test which should
     *         be listed in the GUI, or null if no parameters should be listed.
     */
    protected Arguments getBaseDefaultParameters() {
        Arguments params = new Arguments();
        params.addArgument(ENDPOINT_PARAMETER_NAME, ENDPOINT_PARAMETER_DEFAULT);
        return params;
    }

    /**
     * Dump a list of the parameters in this context to the debug log.
     *
     * @param context
     *            the context which contains the initialization parameters.
     */
    protected void listParameters(JavaSamplerContext context) {
        if (getLogger().isDebugEnabled()) {
            Iterator<String> argsIt = context.getParameterNamesIterator();
            while (argsIt.hasNext()) {
                String name = argsIt.next();
                getLogger().debug(whoAmI()+name + "=" + context.getParameter(name));
            }
        }
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

    /**
     * Get a named JMeter variable
     * 
     * @param varName
     * @return
     */
    protected String getVariable(String varName) {
        JMeterVariables jmvars = getThreadContext().getVariables();
        String value = jmvars.get(varName);
        if (value != null) 
        	return value;
        else
        	return "";
    }

    /**
     * Get a named JMeter Integer variable
     * 
     * @param varName
     * @return
     */
    protected Integer getIntVariable(String varName) {
    	Integer retValue = null;
        String value = getVariable(varName);
        if (value.length() > 0) {
    		try {
    			retValue = Integer.valueOf(value);
    		} catch (NumberFormatException ex) {
    			retValue = null;
    		}
        }
        return retValue;
    }

    /**
     * Get a named JMeter Long variable
     * 
     * @param varName
     * @return
     */
    protected Long getLongVariable(String varName) {
    	Long retValue = null;
        String value = getVariable(varName);
        if (value.length() > 0) {
    		try {
    			retValue = Long.valueOf(value);
    		} catch (NumberFormatException ex) {
    			retValue = null;
    		}
        }
        return retValue;
    }

    /**
     * Get a named JMeter Boolean variable
     * 
     * @param varName
     * @return
     */
    protected Boolean getBooleanVariable(String varName) {
    	Boolean retValue = null;
        String value = getVariable(varName);
        if (value.length() > 0) {
    		try {
    			retValue = Boolean.valueOf(value);
    		} catch (NumberFormatException ex) {
    			retValue = null;
    		}
        }
        return retValue;
    }

    /**
     * Store the value of a named JMeter variable
     * 
     * @param varName
     * @param value
     */
    protected void putVariable(String varName, String value) {
        JMeterVariables jmvars = getThreadContext().getVariables();
    	if (value != null && value.trim().length() > 0) { 
    		jmvars.put(varName, value.toString());
    	}
    };

    /**
     * Store the value of a named JMeter variable
     * 
     * @param varName
     * @param value
     */
    protected void putVariable(String varName, Integer value) {
        JMeterVariables jmvars = getThreadContext().getVariables();
    	if (value != null) { 
    		jmvars.put(varName, value.toString());
    	}
    };

    /**
     * Store the value of a named JMeter variable
     * 
     * @param varName
     * @param value
     */
    protected void putVariable(String varName, Long value) {
        JMeterVariables jmvars = getThreadContext().getVariables();
    	if (value != null) { 
    		jmvars.put(varName, value.toString());
    	}
    };

    /**
     * Store the value of a named JMeter variable
     * 
     * @param varName
     * @param value
     */
    protected void putVariable(String varName, Boolean value) {
        JMeterVariables jmvars = getThreadContext().getVariables();
    	if (value != null) { 
    		jmvars.put(varName, value.toString());
    	}
    };
    
    protected int getUserNumber() {
    	return getThreadContext().getThreadNum();
    }

    /**
     * @return Returns the threadContext.
     */
    protected JMeterContext getThreadContext() {
        if (threadContext == null) {
            /*
             * Only samplers have the thread context set up by JMeterThread at
             * present, so suppress the warning for now
             */
            // log.warn("ThreadContext was not set up - should only happen in
            // JUnit testing..."
            // ,new Throwable("Debug"));
            threadContext = JMeterContextService.getContext();
        }
        return threadContext;
    }
    
    protected void dumpJMeterVariables() {
    	if (getLogger().isDebugEnabled()) {
            JMeterVariables jmvars = getThreadContext().getVariables();
    		StringBuilder logMessage = new StringBuilder("JMeterVariables [\n");
    		Iterator<Entry<String, Object>> iter = jmvars.getIterator();
    		while (iter.hasNext()) {
    			Entry<String, Object> entry = iter.next();
    			logMessage.append(entry.getKey().toString());
    			logMessage.append("=");
    			logMessage.append(entry.getValue().toString());
        		logMessage.append("\n");
    		}
    		logMessage.append("]");
    		getLogger().debug(logMessage.toString());
    	}
    }

}
