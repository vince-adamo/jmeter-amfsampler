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
package org.apache.jmeter.protocol.amf.config.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.jmeter.config.Argument;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.config.gui.AbstractConfigGui;
import org.apache.jmeter.config.gui.ArgumentsPanel;
import org.apache.jmeter.gui.util.HorizontalPanel;
import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jmeter.protocol.amf.resources.AmfResourceManager;
import org.apache.jmeter.protocol.amf.sampler.AmfSampler;
import org.apache.jmeter.protocol.amf.sampler.AmfMessageInterface;
import org.apache.jmeter.protocol.amf.sampler.AmfMessage;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.BooleanProperty;
import org.apache.jmeter.testelement.property.PropertyIterator;
import org.apache.jmeter.testelement.property.TestElementProperty;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.jorphan.reflect.ClassFinder;
import org.apache.log.Logger;

import flex.messaging.io.MessageIOConstants;

import static org.apache.jmeter.protocol.http.util.HTTPConstantsInterface.POST;

/**
 * JMeter configuration GUI component that provides configuration support
 * for the AmfSampler.
 * 
 */
public class AmfConfigGui extends AbstractConfigGui implements ActionListener {

	private static final long serialVersionUID = 1L;

    private static final Logger log = LoggingManager.getLoggerForClass();

    private ArgumentsPanel argsPanel;

    private JTextField domain;

    private JTextField port;

    private JTextField proxyHost;

    private JTextField proxyPort;

    private JTextField proxyUser;

    private JTextField proxyPass;

    private JTextField connectTimeOut;

    private JTextField responseTimeOut;

    private JTextField protocol;

    private JTextField path;

    private JCheckBox useKeepAlive;

    private JComboBox classnameCombo;

    private JComboBox objectEncodingCombo;
    
    private String previouslySelectedClassname = "";

    public AmfConfigGui() {
        init();
    }

    public AmfConfigGui(boolean value) {
        init();
    }

    public void clear() {
        domain.setText(""); // $NON-NLS-1$
        useKeepAlive.setSelected(true);
        path.setText(""); // $NON-NLS-1$
        port.setText(""); // $NON-NLS-1$
        proxyHost.setText(""); // $NON-NLS-1$
        proxyPort.setText(""); // $NON-NLS-1$
        proxyUser.setText(""); // $NON-NLS-1$
        proxyPass.setText(""); // $NON-NLS-1$
        connectTimeOut.setText(""); // $NON-NLS-1$
        responseTimeOut.setText(""); // $NON-NLS-1$
        protocol.setText(""); // $NON-NLS-1$
        argsPanel.clear();
    }

    public String getStaticLabel() {
    	return AmfResourceManager.getResString("amf_request_defaults"); // $NON-NLS-1$
    }
    
	/**
	 * This method returns an empty string because getStaticLabel is overridden, which allows 
	 * this component to provide it's own resource management for strings.
	 */
	public String getLabelResource() {
        return ""; 
	}

    public TestElement createTestElement() {
        ConfigTestElement element = new ConfigTestElement();
        element.setName(this.getName());
        element.setProperty(TestElement.GUI_CLASS, this.getClass().getName());
        element.setProperty(TestElement.TEST_CLASS, element.getClass().getName());
        modifyTestElement(element);
        return element;
    }

    /**
     * Save the GUI values in the sampler.
     *
     * @param element
     */
	public void modifyTestElement(TestElement element) {

		// Set Http request specific test elements
        Arguments args = (Arguments) new Arguments(); // no Http arguments used for this sampler
        element.setProperty(new TestElementProperty(HTTPSamplerBase.ARGUMENTS, args));
        element.setProperty(HTTPSamplerBase.DOMAIN, domain.getText());
        element.setProperty(HTTPSamplerBase.PORT, port.getText());
        element.setProperty(HTTPSamplerBase.PROXYHOST, proxyHost.getText(),"");
        element.setProperty(HTTPSamplerBase.PROXYPORT, proxyPort.getText(),"");
        element.setProperty(HTTPSamplerBase.PROXYUSER, proxyUser.getText(),"");
        element.setProperty(HTTPSamplerBase.PROXYPASS, proxyPass.getText(),"");
        element.setProperty(HTTPSamplerBase.CONNECT_TIMEOUT, connectTimeOut.getText());
        element.setProperty(HTTPSamplerBase.RESPONSE_TIMEOUT, responseTimeOut.getText());
        element.setProperty(HTTPSamplerBase.PROTOCOL, protocol.getText());
        element.setProperty(HTTPSamplerBase.CONTENT_ENCODING, "");
        element.setProperty(HTTPSamplerBase.PATH, path.getText());
        element.setProperty(HTTPSamplerBase.METHOD, POST); // AMF Requests use POST method
        element.setProperty(new BooleanProperty(HTTPSamplerBase.USE_KEEPALIVE, useKeepAlive.isSelected()));
        element.setProperty(new BooleanProperty(HTTPSamplerBase.FOLLOW_REDIRECTS, false));
        element.setProperty(new BooleanProperty(HTTPSamplerBase.AUTO_REDIRECTS, false));
        element.setProperty(new BooleanProperty(HTTPSamplerBase.DO_MULTIPART_POST, false));
        
		// Set AMF request specific test elements
        element.setProperty(AmfSampler.OBJECT_ENCODING_VERSION, String.valueOf(objectEncodingCombo.getSelectedItem()));
        element.setProperty(new TestElementProperty(AmfSampler.ARGUMENTS, (Arguments)argsPanel.createTestElement()));
        element.setProperty(AmfSampler.CLASSNAME, String.valueOf(classnameCombo.getSelectedItem()));
    }

    /**
     * Set the text, etc. in the UI.
     *
     * @param el
     *            contains the data to be displayed
     */
    public void configure(TestElement el) {
    	
        setName(el.getName());
        
        // Configure Http request specific properties
        domain.setText(el.getPropertyAsString(HTTPSamplerBase.DOMAIN));
        String portString = el.getPropertyAsString(HTTPSamplerBase.PORT);
        // Only display the port number if it is meaningfully specified
        if (portString.equals(HTTPSamplerBase.UNSPECIFIED_PORT_AS_STRING)) {
            port.setText(""); // $NON-NLS-1$
        } else {
            port.setText(portString);
        }
        proxyHost.setText(el.getPropertyAsString(HTTPSamplerBase.PROXYHOST));
        proxyPort.setText(el.getPropertyAsString(HTTPSamplerBase.PROXYPORT));
        proxyUser.setText(el.getPropertyAsString(HTTPSamplerBase.PROXYUSER));
        proxyPass.setText(el.getPropertyAsString(HTTPSamplerBase.PROXYPASS));
        connectTimeOut.setText(el.getPropertyAsString(HTTPSamplerBase.CONNECT_TIMEOUT));
        responseTimeOut.setText(el.getPropertyAsString(HTTPSamplerBase.RESPONSE_TIMEOUT));
        protocol.setText(el.getPropertyAsString(HTTPSamplerBase.PROTOCOL));
        path.setText(el.getPropertyAsString(HTTPSamplerBase.PATH));
        useKeepAlive.setSelected(((AbstractTestElement) el).getPropertyAsBoolean(HTTPSamplerBase.USE_KEEPALIVE));
        
        // Configure AMF request specific properties
        objectEncodingCombo.setSelectedItem(el.getPropertyAsString(AmfSampler.OBJECT_ENCODING_VERSION));
        argsPanel.configure((TestElement) el.getProperty(AmfSampler.ARGUMENTS).getObjectValue());
        classnameCombo.setSelectedItem(el.getPropertyAsString(AmfSampler.CLASSNAME));
    }

    private void init() {// called from ctor, so must not be overridable
    	
        this.setLayout(new BorderLayout());
        
        this.add(getWebServerAndTimeoutsPanel(), BorderLayout.NORTH);
        
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BorderLayout());
        centerPanel.add(getHttpRequestPanel(), BorderLayout.NORTH);
        centerPanel.add(getAmfRequestPanel(), BorderLayout.CENTER);
        this.add(centerPanel, BorderLayout.CENTER);
        
        this.add(getProxyServerPanel(), BorderLayout.SOUTH);
    }

    /**
     * Create a panel containing the webserver (domain+port) and timeouts (connect+request).
     *
     * @return the panel
     */
    protected final JPanel getWebServerAndTimeoutsPanel() {
        // WEB SERVER PANEL
        JPanel webServerPanel = new HorizontalPanel();
        webServerPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                JMeterUtils.getResString("web_server"))); // $NON-NLS-1$
        final JPanel domainPanel = getDomainPanel();
        final JPanel portPanel = getPortPanel();
        webServerPanel.add(domainPanel, BorderLayout.CENTER);
        webServerPanel.add(portPanel, BorderLayout.EAST);

        JPanel timeOut = new HorizontalPanel();
        timeOut.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                JMeterUtils.getResString("web_server_timeout_title"))); // $NON-NLS-1$
        final JPanel connPanel = getConnectTimeOutPanel();
        final JPanel reqPanel = getResponseTimeOutPanel();
        timeOut.add(connPanel);
        timeOut.add(reqPanel);

        JPanel webServerTimeoutPanel = new VerticalPanel();
        webServerTimeoutPanel.add(webServerPanel, BorderLayout.CENTER);
        webServerTimeoutPanel.add(timeOut, BorderLayout.EAST);

        JPanel bigPanel = new VerticalPanel();
        bigPanel.add(webServerTimeoutPanel);
        return bigPanel;
    }

    protected final JPanel getHttpRequestPanel() {

        JPanel httpRequestPanel = new JPanel();
        httpRequestPanel.setLayout(new BorderLayout());
        httpRequestPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                JMeterUtils.getResString("web_request"))); // $NON-NLS-1$

        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        northPanel.add(getProtocolAndKeepAlivePanel());
        northPanel.add(getPathPanel());

        httpRequestPanel.add(northPanel, BorderLayout.NORTH);
        
        return httpRequestPanel;
    }

    protected final JPanel getAmfRequestPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                AmfResourceManager.getResString("amf_request"))); // $NON-NLS-1$

        panel.add(getObjectEncodingAndClassnamePanel(), BorderLayout.NORTH);
        panel.add(getParameterPanel(), BorderLayout.CENTER);
        
        return panel;
    }

    protected final JPanel getObjectEncodingAndClassnamePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(getObjectEncodingVersionPanel(), BorderLayout.NORTH);
        panel.add(getClassnamePanel(), BorderLayout.SOUTH);
        return panel;
    }

    /**
     * Create a panel containing the proxy server details
     *
     * @return the panel
     */
    protected final JPanel getProxyServerPanel(){
        JPanel proxyServer = new HorizontalPanel();
        proxyServer.add(getProxyHostPanel(), BorderLayout.CENTER);
        proxyServer.add(getProxyPortPanel(), BorderLayout.EAST);

        JPanel proxyLogin = new HorizontalPanel();
        proxyLogin.add(getProxyUserPanel());
        proxyLogin.add(getProxyPassPanel());

        JPanel proxyServerPanel = new HorizontalPanel();
        proxyServerPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                JMeterUtils.getResString("web_proxy_server_title"))); // $NON-NLS-1$
        proxyServerPanel.add(proxyServer, BorderLayout.CENTER);
        proxyServerPanel.add(proxyLogin, BorderLayout.EAST);

        return proxyServerPanel;
    }

    /**
     * This method defines the Panel for the HTTP path, 'Follow Redirects'
     * 'Use KeepAlive', and 'Use multipart for HTTP POST' elements.
     *
     * @return JPanel The Panel for the path, 'Follow Redirects' and 'Use
     *         KeepAlive' elements.
     */
    protected Component getPathPanel() {
        path = new JTextField(15);

        JLabel label = new JLabel(JMeterUtils.getResString("path")); //$NON-NLS-1$
        label.setLabelFor(path);

        JPanel pathPanel = new JPanel(new BorderLayout(5, 0));
        pathPanel.add(label, BorderLayout.WEST);
        pathPanel.add(path, BorderLayout.CENTER);
        pathPanel.setMinimumSize(pathPanel.getPreferredSize());

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(pathPanel);

        return panel;
    }

    protected JPanel getProtocolAndKeepAlivePanel() {
        
        JLabel protocolLabel = new JLabel(JMeterUtils.getResString("protocol")); // $NON-NLS-1$
        protocolLabel.setLabelFor(protocol);

        protocol = new JTextField(10);
        
        JPanel protocolPanel = new JPanel(new BorderLayout(5, 0));
        protocolPanel.add(protocolLabel, BorderLayout.WEST);
        protocolPanel.add(protocol, BorderLayout.CENTER);
        
        JPanel keepAlivePanel = new JPanel(new BorderLayout(15, 0));
        useKeepAlive = new JCheckBox(JMeterUtils.getResString("use_keepalive")); // $NON-NLS-1$
        useKeepAlive.setSelected(true);
        keepAlivePanel.add(useKeepAlive);
        
    	JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.add(protocolPanel, BorderLayout.WEST);
        panel.add(keepAlivePanel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Create a panel with GUI components allowing the user to select an
     * AmfMessageInterface class.
     *
     * @return a panel containing the relevant components
     */
    private JPanel getClassnamePanel() {
        List<String> possibleClasses = new ArrayList<String>();

        try {
            // Find all the classes which implement the JavaSamplerClient
            // interface.
            possibleClasses = ClassFinder.findClassesThatExtend(JMeterUtils.getSearchPaths(),
                    new Class[] { AmfMessage.class });

            // Remove the JavaConfig class from the list since it only
            // implements the interface for error conditions.

            possibleClasses.remove(AmfSampler.class.getName() + "$ErrorSamplerClient");
        } catch (Exception e) {
            log.debug("Exception getting interfaces.", e);
        }

        previouslySelectedClassname = "";

        JLabel label = new JLabel(AmfResourceManager.getResString("amf_request_classname")); // $NON-NLS-1$

        classnameCombo = new JComboBox(possibleClasses.toArray());
        classnameCombo.setSelectedIndex(0); // Select first entry by default
        classnameCombo.addActionListener(this);
        classnameCombo.setEditable(false);
        label.setLabelFor(classnameCombo);
        
        HorizontalPanel panel = new HorizontalPanel();
        panel.add(label);
        panel.add(classnameCombo);

        return panel;
    }

    /**
     * Create a panel with GUI components allowing the user to select an
     * AMF Object Encoding Version.
     *
     * @return a panel containing the relevant components
     */
    private JPanel getObjectEncodingVersionPanel() {
    	
        List<String> values = new ArrayList<String>();
        values.add(" "+String.valueOf(MessageIOConstants.AMF3));

        JLabel label = new JLabel(AmfResourceManager.getResString("amf_object_encoding_version")); // $NON-NLS-1$

        objectEncodingCombo = new JComboBox(values.toArray());
        objectEncodingCombo.setEditable(false);
        label.setLabelFor(objectEncodingCombo);

        HorizontalPanel panel = new HorizontalPanel();
        panel.add(label);
        panel.add(objectEncodingCombo);

        return panel;
    }

    protected JPanel getParameterPanel() {
        argsPanel = new ArgumentsPanel(JMeterUtils.getResString("paramtable")); // $NON-NLS-1$
        return argsPanel;
    }

    private JPanel getPortPanel() {
        port = new JTextField(4);

        JLabel label = new JLabel(JMeterUtils.getResString("web_server_port")); // $NON-NLS-1$
        label.setLabelFor(port);

        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.add(label, BorderLayout.WEST);
        panel.add(port, BorderLayout.CENTER);

        return panel;
    }

    private JPanel getProxyPortPanel() {
        proxyPort = new JTextField(4);

        JLabel label = new JLabel(JMeterUtils.getResString("web_server_port")); // $NON-NLS-1$
        label.setLabelFor(proxyPort);

        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.add(label, BorderLayout.WEST);
        panel.add(proxyPort, BorderLayout.CENTER);

        return panel;
    }

    private JPanel getConnectTimeOutPanel() {
        connectTimeOut = new JTextField(4);

        JLabel label = new JLabel(JMeterUtils.getResString("web_server_timeout_connect")); // $NON-NLS-1$
        label.setLabelFor(connectTimeOut);

        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.add(label, BorderLayout.WEST);
        panel.add(connectTimeOut, BorderLayout.CENTER);

        return panel;
    }

    private JPanel getResponseTimeOutPanel() {
        responseTimeOut = new JTextField(4);

        JLabel label = new JLabel(JMeterUtils.getResString("web_server_timeout_response")); // $NON-NLS-1$
        label.setLabelFor(responseTimeOut);

        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.add(label, BorderLayout.WEST);
        panel.add(responseTimeOut, BorderLayout.CENTER);

        return panel;
    }

    private JPanel getDomainPanel() {
        domain = new JTextField(20);

        JLabel label = new JLabel(JMeterUtils.getResString("web_server_domain")); // $NON-NLS-1$
        label.setLabelFor(domain);

        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.add(label, BorderLayout.WEST);
        panel.add(domain, BorderLayout.CENTER);
        return panel;
    }

    private JPanel getProxyHostPanel() {
        proxyHost = new JTextField(20);

        JLabel label = new JLabel(JMeterUtils.getResString("web_server_domain")); // $NON-NLS-1$
        label.setLabelFor(proxyHost);

        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.add(label, BorderLayout.WEST);
        panel.add(proxyHost, BorderLayout.CENTER);
        return panel;
    }

    private JPanel getProxyUserPanel() {
        proxyUser = new JTextField(5);

        JLabel label = new JLabel(JMeterUtils.getResString("username")); // $NON-NLS-1$
        label.setLabelFor(proxyUser);

        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.add(label, BorderLayout.WEST);
        panel.add(proxyUser, BorderLayout.CENTER);
        return panel;
    }

    private JPanel getProxyPassPanel() {
        proxyPass = new JTextField(5);

        JLabel label = new JLabel(JMeterUtils.getResString("password")); // $NON-NLS-1$
        label.setLabelFor(proxyPass);

        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.add(label, BorderLayout.WEST);
        panel.add(proxyPass, BorderLayout.CENTER);
        return panel;
    }

	public void actionPerformed(ActionEvent evt) {
        if (evt.getSource() == classnameCombo) {
            String className = ((String) classnameCombo.getSelectedItem()).trim();
            try {
            	AmfMessageInterface request = (AmfMessageInterface) Class.forName(className, true,
                        Thread.currentThread().getContextClassLoader()).newInstance();

                Arguments currArgs = new Arguments();
                argsPanel.modifyTestElement(currArgs);
                Map<String, String> currArgsMap = currArgs.getArgumentsAsMap();

                if (currArgsMap.isEmpty()) {
	                Arguments newArgs = new Arguments();
	                Arguments requestParams = null;
	                try {
	                    requestParams = request.getDefaultParameters();
	                } catch (AbstractMethodError e) {
	                    log.warn("AmfMessageInterface doesn't implement "
	                            + "getDefaultParameters.  Default parameters won't "
	                            + "be shown.  Please update your client class: " + className);
	                }
	
	                if (requestParams != null) {
	                    PropertyIterator i = requestParams.getArguments().iterator();
	                    while (i.hasNext()) {
	                        Argument arg = (Argument) i.next().getObjectValue();
	                        String name = arg.getName();
	                        String value = arg.getValue();
	
	                        // If a user has set parameters in one test, and then
	                        // selects a different test which supports the same
	                        // parameters, those parameters should have the same
	                        // values that they did in the original test.
	                        if (currArgsMap.containsKey(name)) {
	                            String newVal = currArgsMap.get(name);
	                            if (newVal != null && newVal.length() > 0) {
	                                value = newVal;
	                            }
	                        }
	                        newArgs.addArgument(name, value);
	                    }
	                }

	                argsPanel.configure(newArgs);
                }
                
            } catch (Exception e) {
                log.error("Error getting argument list for " + className, e);
            }
        }
	}

}
