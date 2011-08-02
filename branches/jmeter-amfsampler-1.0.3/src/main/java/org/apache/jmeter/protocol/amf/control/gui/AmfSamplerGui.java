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
package org.apache.jmeter.protocol.amf.control.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import org.apache.jmeter.protocol.amf.config.gui.AmfConfigGui;
import org.apache.jmeter.protocol.amf.resources.AmfResourceManager;
import org.apache.jmeter.protocol.amf.sampler.AmfSampler;
import org.apache.jmeter.protocol.amf.sampler.AmfSamplerFactory;
import org.apache.jmeter.samplers.gui.AbstractSamplerGui;
import org.apache.jmeter.testelement.TestElement;

/**
 * The JMeter GUI component which manage the AmfSampler.
 *
 */
public class AmfSamplerGui extends AbstractSamplerGui {

	private static final long serialVersionUID = 1L;

	private AmfConfigGui amfConfigGui;

    public AmfSamplerGui() {
        init();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(TestElement element) {
        super.configure(element);
        final AmfSampler sampler = (AmfSampler) element;
        amfConfigGui.configure(sampler);
    }

    /**
     * {@inheritDoc}
     */
    public TestElement createTestElement() {
        AmfSampler sampler = AmfSamplerFactory.newInstance();// create default sampler
        modifyTestElement(sampler);
        return sampler;
    }

    /**
     * Modifies a given TestElement to mirror the data in the gui components.
     * <p>
     * {@inheritDoc}
     */
    public void modifyTestElement(TestElement element) {
        final AmfSampler sampler = (AmfSampler) element;
        sampler.clear();
        amfConfigGui.modifyTestElement(sampler);
        this.configureTestElement(sampler);
    }

    /**
     * {@inheritDoc}
     */
    public String getStaticLabel() {
        return AmfResourceManager.getResString("amf_request"); // $NON-NLS-1$
    }

	/**
	 * This method returns an empty string because getStaticLabel is overridden, which allows 
	 * this component to provide it's own resource management for strings.
	 */
	@Override
	public String getLabelResource() {
		return "";
	}

    private void init() {// called from ctor, so must not be overridable
        setLayout(new BorderLayout(0, 5));
        setBorder(makeBorder());

        add(makeTitlePanel(), BorderLayout.NORTH);

        // URL CONFIG
        amfConfigGui = new AmfConfigGui();
        add(amfConfigGui, BorderLayout.CENTER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Dimension getPreferredSize() {
        return getMinimumSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearGui() {
        super.clearGui();
        amfConfigGui.clear();
    }

}
