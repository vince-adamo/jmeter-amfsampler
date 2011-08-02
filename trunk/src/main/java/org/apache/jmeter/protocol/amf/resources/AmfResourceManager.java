package org.apache.jmeter.protocol.amf.resources;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

public class AmfResourceManager {

    private static final Logger log = LoggingManager.getLoggerForClass();
	
	private static final String resourceName = "org.apache.jmeter.protocol.amf.resources.messages"; 

    private static ResourceBundle resources = null;
	
	public static String getResString(String key) {
		if (resources == null) {
	        setLocale(JMeterUtils.getLocale());
		}
        String resString = null;
        try {
            resString = resources.getString(key);
        } catch (MissingResourceException mre) {
            log.warn("ERROR! Resource string not found: [" + key + "]", mre);
            resString = "[res_key=" + key + "]";
        }
        return resString;
	}
	
	private static void setLocale(Locale locale) {
		if (locale == null) locale = Locale.getDefault();
        resources = ResourceBundle.getBundle(resourceName, locale);
	}
}
