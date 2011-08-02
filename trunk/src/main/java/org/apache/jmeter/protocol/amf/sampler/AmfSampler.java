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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.http.control.CacheManager;
import org.apache.jmeter.protocol.http.sampler.HTTPSampleResult;
import org.apache.jmeter.protocol.http.sampler.HTTPSampler2;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.Interruptible;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.jorphan.util.JOrphanUtils;
import org.apache.log.Logger;

import flex.messaging.messages.AbstractMessage;
import flex.messaging.messages.AcknowledgeMessage;
import flex.messaging.messages.AcknowledgeMessageExt;

/**
 * A sampler for testing Flex/BlazeDS applications using Adobe's AMF protocol over Http.
 * <p>
 * This sampler extends HTTPSampler2, taking advantage of that samplers use of the Apache 
 * Commons HTTPClient and associated built-in support for cookie management (via the HTTP Cookie Manager).
 * This was found to be necessary when working with certain load balancers and sticky session handling via cookies, 
 * which tend to favor the behavior of the Apache Commons HTTPClient over the default Java implementation
 * used within, for example, flex.messaging.io.amf.client.AMFConnection.
 * <p>
 * This sampler does, though, use an extension to AMFConnection for support in the serialization and deserialization 
 * of AMF messages sent to/from a remote server.  Serialized AMF messages are sent to the remote server as an HTTP POST
 * method request entity and the Http response body received from the remote server are deserialized and processed.
 * <p>
 * This sampler borrows from the AbstractJavaSamplerClient concept, supporting custom pre/post-processing implementations
 * based on user-supplied extensions of the AmfCommandMessage and AmfRemotingMessage classes provided with this
 * sampler.
 *
 */
public class AmfSampler extends HTTPSampler2 implements Interruptible {

	private static final long serialVersionUID = 1L;

    private static final Logger log = LoggingManager.getLoggerForClass();

    public static final String RESPONSE_CODE_200 = "200"; // $NON-NLS-1$

    /**
     * Property key representing the arguments to use in the AmfMessageInterface.
     */
    public static final String ARGUMENTS = "AmfSampler.arguments"; // $NON-NLS-1$

    /**
     * Property key representing the classname of the AmfMessageInterface to user.
     */
    public static final String CLASSNAME = "AmfSampler.classname"; // $NON-NLS-1$

    /**
     * Property key representing the object encoding version to set in the AmfMessageInterface.
     */
    public static final String OBJECT_ENCODING_VERSION = "AmfSampler.objectEncoding"; // $NON-NLS-1$

    /**
     * The AmfMessageInterface instance used by this sampler to actually perform
     * the sample.
     */
    private transient AmfMessageInterface amfRequest = null;

    /**
     * The AmfSamplerContext instance used by this sampler to hold information
     * related to the test run, such as the parameters specified for the sampler
     * client.
     */
    private transient AmfSamplerContext context = null;

    /**
     * Sets the Classname attribute of the JavaConfig object
     *
     * @param classname
     *            the new Classname value
     */
    public void setClassname(String classname) {
        setProperty(CLASSNAME, classname);
    }

    /**
     * Gets the Classname attribute of the JavaConfig object
     *
     * @return the Classname value
     */
    public String getClassname() {
        return getPropertyAsString(CLASSNAME);
    }

    /**
     * Performs a test sample.
     *
     * The <code>sample()</code> method retrieves the reference to the Java
     * client and calls its <code>runTest()</code> method.
     *
     * @see JavaSamplerClient#runTest(JavaSamplerContext)
     *
     * @param entry
     *            the Entry for this sample
     * @return test SampleResult
     */
    public SampleResult sample(Entry entry) {

    	Arguments arguments = (Arguments) getProperty(AmfSampler.ARGUMENTS).getObjectValue();
    	arguments.addArgument(OBJECT_ENCODING_VERSION, getProperty(OBJECT_ENCODING_VERSION).getStringValue());
        context = new AmfSamplerContext(arguments);
        
        if (amfRequest == null) {
            createAmfRequest();
            amfRequest.setupTest(context);
        }

        amfRequest.open();

        SampleResult result = null;
        try {
	        // Issue Http request
	        result = super.sample();
	        
	        if (result.getResponseCode().equals(RESPONSE_CODE_200)) {
	        	
	        	// decode and process AMF message response
	            amfRequest.processResponse(result);
	            
	        }
        } finally {
        	amfRequest.close();
        }

        return result;
    }

    @Override
    protected HTTPSampleResult sample(URL url, String method, boolean areFollowingRedirect, int frameDepth) {

        String urlStr = url.toString();

        log.debug("Start : sample " + urlStr);
        log.debug("method " + method);

        PostMethod httpMethod = new PostMethod(urlStr);

        String contentType = "application/x-amf";
        
        // Create an encrypted AMF request and add it as the POST request body
        byte[] amfMessage = amfRequest.createRequest(context);
        
        if (amfMessage != null) {
	        ByteArrayRequestEntity requestEntity = new ByteArrayRequestEntity(amfMessage, contentType); 
	        httpMethod.setRequestEntity(requestEntity);
        }

        HTTPSampleResult res = new HTTPSampleResult();
        res.setMonitor(isMonitor());

        res.setSampleLabel(urlStr); // May be replaced later
        res.setHTTPMethod(method);
        res.setURL(url);

        res.sampleStart(); // Count the retries as well in the time
        HttpClient client = null;
        InputStream instream = null;
        try {
            // Set any default request headers
            setDefaultRequestHeaders(httpMethod);
            // Setup connection
            client = setupConnection(url, httpMethod, res);
            savedClient = client;

            // Execute POST
            int statusCode = client.executeMethod(httpMethod);

            // Needs to be done after execute to pick up all the headers
            res.setRequestHeaders(getConnectionHeaders(httpMethod));

            // Request sent. Now get the response:
            instream = httpMethod.getResponseBodyAsStream();

            if (instream != null) {// will be null for HEAD

                Header responseHeader = httpMethod.getResponseHeader(HEADER_CONTENT_ENCODING);
                if (responseHeader!= null && ENCODING_GZIP.equals(responseHeader.getValue())) {
                    instream = new GZIPInputStream(instream);
                }
                res.setResponseData(readResponse(res, instream, (int) httpMethod.getResponseContentLength()));
            }

            res.sampleEnd();
            // Done with the sampling proper.

            // Now collect the results into the HTTPSampleResult:

            res.setSampleLabel(httpMethod.getURI().toString());
            // Pick up Actual path (after redirects)

            res.setResponseCode(Integer.toString(statusCode));
            res.setSuccessful(isSuccessCode(statusCode));

            res.setResponseMessage(httpMethod.getStatusText());

            String ct = null;
            org.apache.commons.httpclient.Header h
                = httpMethod.getResponseHeader(HEADER_CONTENT_TYPE);
            if (h != null)// Can be missing, e.g. on redirect
            {
                ct = h.getValue();
                res.setContentType(ct);// e.g. text/html; charset=ISO-8859-1
                res.setEncodingAndType(ct);
            }

            res.setResponseHeaders(getResponseHeaders(httpMethod));

            // Store any cookies received in the cookie manager:
            saveConnectionCookies(httpMethod, res.getURL(), getCookieManager());

            // Save cache information
            final CacheManager cacheManager = getCacheManager();
            if (cacheManager != null){
                cacheManager.saveDetails(httpMethod, res);
            }

            log.debug("End : sample");
            httpMethod.releaseConnection();
            return res;
        } catch (IllegalArgumentException e)// e.g. some kinds of invalid URL
        {
            res.sampleEnd();
            HTTPSampleResult err = errorResult(e, res);
            err.setSampleLabel("Error: " + url.toString());
            return err;
        } catch (IOException e) {
            res.sampleEnd();
            HTTPSampleResult err = errorResult(e, res);
            err.setSampleLabel("Error: " + url.toString());
            return err;
        } finally {
            savedClient = null;
            JOrphanUtils.closeQuietly(instream);
            if (httpMethod != null) {
                httpMethod.releaseConnection();
            }
        }
    }

    protected void setDefaultRequestHeaders(HttpMethod httpMethod) {
    	httpMethod.setRequestHeader("Cache-Control", "no-cache");
    	httpMethod.setRequestHeader("Accept", "*/*");
    	httpMethod.setRequestHeader("Accept-Encoding", "gzip, deflate");
    }
    
    public void testEnded() {
    	super.testEnded();
    	
    }

    /**
     * Returns reference to <code>AmfMessageInterface</code>.
     *
     * The <code>createAMFRequest()</code> method uses reflection to create an
     * instance of the specified AMF request client. If the class can not be
     * found, the method returns a reference to <code>this</code> object.
     *
     * @return JavaSamplerClient reference.
     */
    private void createAmfRequest() {
        log.debug(whoAmI() + "Creating AMF Request");
        try {
            Class<?> javaClass = Class.forName(getClassname().trim(), false, Thread.currentThread()
                    .getContextClassLoader());
            amfRequest = (AmfMessageInterface) javaClass.newInstance();

            if (log.isDebugEnabled()) {
                log.debug(whoAmI() + "\tCreated:\t" + getClassname() + "@"
                        + Integer.toHexString(amfRequest.hashCode()));
            }
        } catch (Exception e) {
            log.error(whoAmI() + "\tException creating: " + getClassname(), e);
            amfRequest = new ErrorSamplerClient();
        }
    }

    /**
     * Generate a String identifier of this instance for debugging purposes.
     *
     * @return a String identifier for this sampler instance
     */
    private String whoAmI() {
        StringBuilder sb = new StringBuilder();
        sb.append(Thread.currentThread().getName());
        sb.append("@");
        sb.append(Integer.toHexString(hashCode()));
        sb.append("-");
        sb.append(getName());
        return sb.toString();
    }

    /**
     * An {@link AmfMessageInterface} implementation used for error handling. If an
     * error occurs while creating the real AmfMessageInterface object, it is
     * replaced with an instance of this class. Each time a sample occurs with
     * this class, the result is marked as a failure so the user can see that
     * the test failed.
     */
    class ErrorSamplerClient extends AmfMessage {
        /**
         * Return SampleResult with data on error.
         *
         * @see JavaSamplerClient#runTest(JavaSamplerContext)
         */
        public SampleResult runTest(JavaSamplerContext p_context) {
            log.debug(whoAmI() + "\trunTest");
            Thread.yield();
            SampleResult results = new SampleResult();
            results.setSuccessful(false);
            results.setResponseData(("Class not found: " + getClassname()), null);
            results.setSampleLabel("ERROR: " + getClassname());
            return results;
        }

		@Override
		protected boolean processResult(AcknowledgeMessage ackMessage) {
			return false;
		}

		@Override
		protected boolean processResult(AcknowledgeMessageExt ackMessage) {
			return false;
		}

		@Override
		protected AbstractMessage createAbstractMessage(
				AmfSamplerContext context) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Arguments getDefaultParameters() {
			// TODO Auto-generated method stub
			return null;
		}
    }

}
