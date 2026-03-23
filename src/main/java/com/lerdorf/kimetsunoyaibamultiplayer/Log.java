package com.lerdorf.kimetsunoyaibamultiplayer;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class Log {
	private static final Logger LOGGER = LoggerFactory.getLogger("KimetsunoyaibaTweaks");
    private static final Logger FALLBACK_LOGGER = LogUtils.getLogger();
    private static final String PREFIX = "[Kimetsunoyaiba Tweaks] ";
    private static final ConcurrentMap<String, AtomicLong> LAST_LOG_TIMES = new ConcurrentHashMap<>();
	
	public static void debug(String message, Object... args) {
		try {
		if (Config.logDebug)
			LOGGER.debug(PREFIX + message, args);
		} catch (Exception e) {}
    }

    public static void debugVisible(String message, Object... args) {
        try {
            if (Config.logDebug) {
                LOGGER.warn(PREFIX + "[DEBUG] " + message, args);
            }
        } catch (Exception e) {}
    }

    public static void debugEvery(String key, long intervalMs, String message, Object... args) {
        try {
            if (Config.logDebug && shouldLogNow("debug:" + key, intervalMs)) {
                LOGGER.debug(PREFIX + message, args);
            }
        } catch (Exception e) {}
    }

    public static void debugVisibleEvery(String key, long intervalMs, String message, Object... args) {
        try {
            if (Config.logDebug && shouldLogNow("debug-visible:" + key, intervalMs)) {
                LOGGER.warn(PREFIX + "[DEBUG] " + message, args);
            }
        } catch (Exception e) {}
    }

    public static void startupProbe(String context) {
        String message = PREFIX + "[PROBE] Logger reachable from " + context;
        try {
            LOGGER.warn(message);
        } catch (Exception ignored) {
            try {
                FALLBACK_LOGGER.warn(message);
            } catch (Exception ignoredAgain) {}
        }
    }

    public static void alwaysWarn(String message, Object... args) {
        String format = PREFIX + message;
        try {
            LOGGER.warn(format, args);
        } catch (Exception ignored) {
            try {
                FALLBACK_LOGGER.warn(format, args);
            } catch (Exception ignoredAgain) {}
        }
    }

    public static void alwaysError(String message, Object... args) {
        String format = PREFIX + message;
        try {
            LOGGER.error(format, args);
        } catch (Exception ignored) {
            try {
                FALLBACK_LOGGER.error(format, args);
            } catch (Exception ignoredAgain) {}
        }
    }

    public static void startupProbeOnce(String context) {
        if (shouldLogNow("probe:" + context, Long.MAX_VALUE)) {
            startupProbe(context);
        }
    }

    public static void debugVisibleIfSlow(String key, long startNanos, long thresholdMs, String message, Object... args) {
        try {
            if (!Config.logDebug) {
                return;
            }
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
            if (elapsedMs < thresholdMs) {
                return;
            }
            if (!shouldLogNow("slow:" + key, 1000L)) {
                return;
            }

            StringBuilder format = new StringBuilder(PREFIX)
                .append("[DEBUG] [SLOW ")
                .append(elapsedMs)
                .append("ms] ")
                .append(message);
            LOGGER.warn(format.toString(), args);
        } catch (Exception ignored) {}
    }

    public static void info(String message, Object... args) {
    	try {
    	if (Config.logInfo)
    		LOGGER.info(PREFIX + message, args);
    	} catch (Exception e) {}
    }

    public static void infoEvery(String key, long intervalMs, String message, Object... args) {
        try {
            if (Config.logInfo && shouldLogNow("info:" + key, intervalMs)) {
                LOGGER.info(PREFIX + message, args);
            }
        } catch (Exception e) {}
    }

    public static void warn(String message, Object... args) {
    	try {
    	if (Config.logWarning)
    		LOGGER.warn(PREFIX + message, args);
    	}catch (Exception e) {}
    }

    public static void warnEvery(String key, long intervalMs, String message, Object... args) {
        try {
            if (Config.logWarning && shouldLogNow("warn:" + key, intervalMs)) {
                LOGGER.warn(PREFIX + message, args);
            }
        } catch (Exception e) {}
    }

    public static void error(String message, Object... args) {
    	try {
    	if (Config.logError)
    		LOGGER.error(PREFIX + message, args);
    	} catch (Exception e) {}
    }

    public static void errorEvery(String key, long intervalMs, String message, Object... args) {
        try {
            if (Config.logError && shouldLogNow("error:" + key, intervalMs)) {
                LOGGER.error(PREFIX + message, args);
            }
        } catch (Exception e) {}
    }

    private static boolean shouldLogNow(String key, long intervalMs) {
        long now = System.currentTimeMillis();
        AtomicLong lastTime = LAST_LOG_TIMES.computeIfAbsent(key, unused -> new AtomicLong(0L));

        while (true) {
            long previous = lastTime.get();
            if (now - previous < intervalMs) {
                return false;
            }
            if (lastTime.compareAndSet(previous, now)) {
                return true;
            }
        }
    }
}
