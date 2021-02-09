/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.tibco.collectors;

import com.appdynamics.extensions.tibco.TibcoEMSDestinationCache;
import com.appdynamics.extensions.tibco.TibcoEMSDestinationCache.DestinationType;
import com.appdynamics.extensions.tibco.metrics.Metrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tibco.tibjms.admin.DestinationInfo;
import com.tibco.tibjms.admin.TibjmsAdmin;
import org.slf4j.Logger;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Satish Muddam
 */
public abstract class AbstractMetricCollector implements Runnable {

    TibjmsAdmin conn;
    List<Pattern> includePatterns;
    boolean showSystem;
    boolean showTemp;
    boolean showDynamic;
    Metrics metrics;
    String metricPrefix;
    TibcoEMSDestinationCache destinationCache;

    protected static ObjectMapper objectMapper = new ObjectMapper();


    public AbstractMetricCollector(TibjmsAdmin conn, TibcoEMSDestinationCache dcache, List<Pattern> includePatterns, boolean showSystem,
                                   boolean showTemp, boolean showDynamic, Metrics metrics, String metricPrefix) {
        this.conn = conn;
        this.destinationCache = dcache;
        this.includePatterns = includePatterns;
        this.showSystem = showSystem;
        this.showTemp = showTemp;
        this.showDynamic = showDynamic;
        this.metrics = metrics;
        this.metricPrefix = metricPrefix;
    }

    boolean shouldMonitorDestination(String destName, TibcoEMSDestinationCache.DestinationType destinationType, Logger logger) {
        return shouldMonitorDestination(destName, includePatterns, showSystem, showTemp, showDynamic, destinationType, logger);
    }

    boolean shouldMonitorDestination(String destName, List<Pattern> includePatterns, boolean showSystem, boolean showTemp, boolean showDynamic, DestinationType destinationType, Logger logger) {

        logger.info("Checking includes and excludes for " + destinationType.getType() + " with name " + destName);

        try {
            if (destName.startsWith("$TMP$.") && !showTemp) {
                logger.info("Skipping temporary " + destinationType.getType() + " '" + destName + "'");
                return false;
            }

            if (destName.startsWith("$sys.") && !showSystem) {
                logger.info("Skipping system " + destinationType.getType() + " '" + destName + "'");
                return false;
            }

            if(destinationType != null && destinationCache != null) {
                DestinationInfo di = destinationCache.get(destName, destinationType);
                if(!di.isStatic() && !showDynamic) {
                    logger.info("Skipping dynamic " + destinationType.getType() + " '" + destName);
                    return false;
                }
            }

            if (includePatterns != null && includePatterns.size() > 0) {
                logger.info("Using patterns to include [" + includePatterns + "] to filter");
                for (Pattern patternToInclude : includePatterns) {
                    Matcher matcher = patternToInclude.matcher(destName);
                    if (matcher.matches()) {
                        logger.info(String.format("Including '%s' '%s' due to include pattern '%s'",
                                destinationType.getType(), destName, patternToInclude.pattern()));
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            logger.info("Error in checking includes and excludes for  " + destinationType.getType() + " with name " + destName);
            return false;
        }
    }
}