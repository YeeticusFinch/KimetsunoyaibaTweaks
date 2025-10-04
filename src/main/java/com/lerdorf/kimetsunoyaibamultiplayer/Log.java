package com.lerdorf.kimetsunoyaibamultiplayer;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class Log {
	private static final Logger LOGGER = LogUtils.getLogger();
	
	public static void debug(String message, Object... args) {
		try {
		if (Config.logDebug)
			LOGGER.debug(message, args);
        	//System.out.println("[DEBUG] " + format(message, args));
		} catch (Exception e) {}
    }

    public static void info(String message, Object... args) {
    	try {
    	if (Config.logInfo)
    		LOGGER.info(message, args);
        	//System.out.println("[INFO] " + format(message, args));
    	} catch (Exception e) {}
    }

    public static void warn(String message, Object... args) {
    	try {
    	if (Config.logWarning)
    		LOGGER.warn(message, args);
        	//System.out.println("[WARNING] " + format(message, args));
    	}catch (Exception e) {}
    }

    public static void error(String message, Object... args) {
    	try {
    	if (Config.logError)
    		LOGGER.error(message, args);
        	//System.out.println("[ERROR] " + format(message, args));
    	} catch (Exception e) {}
    }
    
    private static String format(String message, Object... args) {
        // Replace `{}` with the corresponding arguments
        for (Object arg : args) {
            message = message.replaceFirst("\\{}", arg == null ? "null" : arg.toString());
        }
        return message;
    }
}
