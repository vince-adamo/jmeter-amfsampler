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

/**
 * Factory to return the appropriate AmfSampler for use with classes that need
 * an AmfSampler
 *
 */
public class AmfSamplerFactory {

    /** Use the the default Java AMF implementation */
    public static final String AMF_SAMPLER_JAVA = "AmfSampler"; //$NON-NLS-1$

    public static final String DEFAULT_CLASSNAME = AMF_SAMPLER_JAVA; //$NON-NLS-1$

    private AmfSamplerFactory() {
        // Not intended to be instantiated
    }

    /**
     * Create a new instance of the default sampler
     *
     * @return instance of default sampler
     */
    public static AmfSampler newInstance() {
        return newInstance(DEFAULT_CLASSNAME);
    }

    /**
     * Create a new instance of the required sampler type
     *
     * @param alias AMF_SAMPLER or AMF_3_SAMPLER
     * @return the appropriate sampler
     * @throws UnsupportedOperationException if alias is not recognised
     */
    public static AmfSampler newInstance(String alias) {
        if (alias.length() == 0) {
            alias = DEFAULT_CLASSNAME;
        }
        if (alias.equals(AMF_SAMPLER_JAVA)) {
            return new AmfSampler();
        }
        throw new UnsupportedOperationException("Cannot create class: " + alias);
    }

}
