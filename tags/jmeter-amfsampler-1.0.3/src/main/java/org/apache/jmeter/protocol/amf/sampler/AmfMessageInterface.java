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

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.samplers.SampleResult;

public interface AmfMessageInterface {

    /**
     * Do any initialization required by this client that is not required
     * on a per sample basis. It is generally recommended to do any initialization 
     * such as getting parameter values that do not change during test iterations
     * in the setupTest method rather than the createRequest method in order to add as
     * little overhead as possible to the test.
     *
     * @param context
     *            the context to run with. This provides access to
     *            initialization parameters.
     */
    public void setupTest(AmfSamplerContext context);

    /**
     * Perform any non request-specific setup that is required by this client
     * on a per sample basis. It is generally recommended to perform the minimal amount
     * of initialization within this method to add as little overhead as possible 
     * to the test.
     */
	public void open();

	/**
     * Provide a list of parameters which this request supports. Any parameter
     * names and associated values returned by this method will appear in the
     * GUI by default so the user doesn't have to remember the exact names. The
     * user can add other parameters which are not listed here. If this method
     * returns null then no parameters will be listed. If the value for some
     * parameter is null then that parameter will be listed in the GUI with an
     * empty value.
     *
     * @return a specification of the parameters used by this test which should
     *         be listed in the GUI, or null if no parameters should be listed.
     */
    public Arguments getDefaultParameters();

	/**
	 * Create a serialized request for this message type given the sample context.
	 * 
	 * @return a serialized request object as a byte array. 
	 * 
	 */
	public byte[] createRequest(AmfSamplerContext context);

	/**
	 * Deserialize and process response data contained with the SampleResult instance.
	 * 
	 * @param result an instance of the SampleResult object returned from the runTest()
	 * method in AmfSampler. 
	 */
	public void processResponse(SampleResult result);

    /**
     * Perform any cleanup required by this client that is required on a per sample basis.
     */
    public void close();

}
