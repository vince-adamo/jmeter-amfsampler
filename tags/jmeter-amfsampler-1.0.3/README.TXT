
License Information:

	This code is released under the terms and conditions specified by the Apache License 2.0.  A copy of this License is included in the distribution as LICENSE-2_0.txt and is also available at http://www.apache.org/licenses/LICENSE-2.0. 
	
	This code includes a subclass of flex.messaging.io.amf.client.AMFConnection, a BlazeDS library class, that is released under the GNU Lesser General Public License 3.0.  As such, this code is considered a "Combined Work" under that license and therefore can be released "under terms of your choice", provided several conditions are met, with one of them being the statement that the BlazeDS "Library" is used by this code and is covered under the LGPLv3 license.  A copy of this license is also included in this distribution as lgpl-3_0.txt.  
	
Deploying jmeter_amfsampler:

	The following is a list of steps to deploy this sampler in your test environment: 
	
	1. Copy the file, jmeter_amfsampler.jar, into your {jmeter.home}\lib\ext subdirectory, where {jmeter.home} is the location of your local JMeter installation.
	
	2. Add the following two lines to the JMeter saveservice.properties file located in {jmeter.home}\bin:
	
	AMFSampler=org.apache.jmeter.protocol.amf.sampler.AmfSampler
	AMFSamplerGui=org.apache.jmeter.protocol.amf.control.gui.AmfSamplerGui
	
	3. Copy the desired blazeds-common and blazeds.core jar files into the {jmeter.home}\lib subdirectory.  The current revision of jmeter_amfsampler.jar has been tested with the 3.2.0.3978 versions of these two jars.
	
	4. Start JMeter. The new sampler should be displayed as "AMF Request" under the Add > Sampler menu selection. 
	
	Configuring an "AMF Request" sampler:
	
	1. Define the Web Server and HTTP Request parameters as with any HTTP Request sampler.  Note that these parameters can be defined using an HTTP Request Defaults test element.  
	
	2. Define the AMF request parameters as follows:
	
	   Object Encoding Version: 3 (this is the only supported option at this time)
	
	   Classname: Select either org.apache.jmeter.protocol.amf.sampler.AmfCommandMessage or org.apache.jmeter.protocol.amf.sampler.AmfRemotingMessage.
	            
	         The AmfCommandMessage requires the following "Send Parameters With Request" (they should be added automatically if this class is selected):
	         
	            DSId - Flex message header that identifies the application's AMF channel Id.
	            DSMessagingVersion - Flex message header that identifies the messaging version. 
	            Operation - Command Message Id, e.g. Enter 5 for a Client Ping operation.  See flex.messaging.messages.CommandMessages for possible values. 
	   
	         The AmfRemotingMessage requires the following "Send Parameters With Request" (they should be added automatically if this class is selected):
	         
	            Channel Definition Id - Flex application's channel definition id (e.g. my-amf)
	            Destination - Identifies the destination that the message targets.
	            Operation Name - Identifies the desired remoting message operation.
	            
	         The preceding parameters are usually identified by examining the actual AMF Request/Response message interactions between a Flex app running in a browser and the remote flex server.  This can be easily done using the AMF Explorer for FireBug in FireFox (see http://amfexplorer.riaforge.org/). 
	            
	         In addition, for the AmfRemotingMessage classname, one or more of the following simple parameters may be added to support your specific application Remoting messages:
	   
	            Parameter Names: AMF_IntParameter_nnn, AMF_LongParameter_nnn, AMF_Parameter_nnn
	
	            Where ,
	
	               AMF_IntParameter_nnn is an Integer parameter,
	               AMF_LongParameter_nnn is a Long parameter,
	               AMF_Parameter_nnn is a String parameter, and
	               
	               nnn at the end of a parameter name identifies the argument number for that parameter to be sent in the remoting message. 
	
	               NOTE: See org.apache.jmeter.protocol.java.sampler.JavaSamplerContext for more descriptions regarding the parameter types.
	
	               Example: 
	
	                    Name             Value
	
	               AMF_Parameter_1	   {username}
	               AMF_Parameter_2	   {password}
	
	               NOTE: If your application's remote AMF API requires more complex arguments then integers, longs and strings, you may implement custom messages by extending AmfRemotingMessage.  You should override the setupTest method and add your custom application classes to the amfParameters map.  The new clases will then be available in the Classname dropdown list.  If necessary, override the processResult method to provide custom processing of the result object.
				   
			The plugin will capture several pieces of information during a sample and update a corresponding JMeter variable that can be used in subsequent requests.  The following JMeter parameters can be used following any given sample:
	
				${DSId} – contains the previous responses Flex client Id, or DSId variable;
				${lastOperationSucceeded} – contains ‘true’ if a response was received, otherwise it contains ‘false’.  The default implementation primarily relies on the presence of a non-null Flex client Id to be present for the response to be considered successful.  Custom implementations can set this variable based on more application-specific data. 
