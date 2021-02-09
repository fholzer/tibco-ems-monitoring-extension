package com.appdynamics.extensions.tibco;

import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.tibco.tibjms.admin.DestinationInfo;
import com.tibco.tibjms.admin.QueueInfo;
import com.tibco.tibjms.admin.TibjmsAdmin;
import com.tibco.tibjms.admin.TibjmsAdminException;
import com.tibco.tibjms.admin.TopicInfo;
import java.util.Collection;
import java.util.HashMap;

/**
 *
 * @author fholzer
 */
public class TibcoEMSDestinationCache {
    private static final org.slf4j.Logger logger = ExtensionsLoggerFactory.getLogger(TibcoEMSDestinationCache.class);

    public enum DestinationType {
        QUEUE(DestinationInfo.QUEUE_TYPE, "Queue"), TOPIC(DestinationInfo.TOPIC_TYPE, "Topic");

        private final int id;
        private final String type;

        DestinationType(int id, String type) {
            this.id = id;
            this.type = type;
        }

        public int getId() {
            return id;
        }

        public String getType() {
            return type;
        }

        public static DestinationType byType(String type) {
            if (QUEUE.getType().equalsIgnoreCase(type)) {
                return QUEUE;
            } else if (TOPIC.getType().equalsIgnoreCase(type)) {
                return TOPIC;
            }

            logger.error("Invalid type [ " + type + " ] specified");
            throw new RuntimeException("Invalid type [ " + type + " ] specified");
        }

        public static DestinationType byId(int id) {
            if (QUEUE.getId() == id) {
                return QUEUE;
            } else if (TOPIC.getId() == id) {
                return TOPIC;
            }

            logger.error("Invalid detination type id [ " + id + " ] specified");
            throw new RuntimeException("Invalid destination type id [ " + id + " ] specified");
        }
    }
    
    TibjmsAdmin conn;
    HashMap<String, TopicInfo> topicInfoCache = new HashMap<>();
    HashMap<String, QueueInfo> queueInfoCache = new HashMap<>();
    
    public TibcoEMSDestinationCache(TibjmsAdmin conn, TopicInfo[] topicInfos, QueueInfo[] queueInfos) {
        this.conn = conn;
        for(QueueInfo queueInfo : queueInfos) {
            queueInfoCache.put(queueInfo.getName(), queueInfo);
        }
        
        for(TopicInfo topicInfo : topicInfos) {
            topicInfoCache.put(topicInfo.getName(), topicInfo);
        }
    }
    
    public DestinationInfo get(String name, DestinationType type) throws TibjmsAdminException {
        switch(type) {
            case QUEUE:
                if(!queueInfoCache.containsKey(name)) {
                    QueueInfo qi = conn.getQueue(name);
                    queueInfoCache.put(name, qi);
                    return qi;
                }
                return queueInfoCache.get(name);
                
            case TOPIC:
                if(!topicInfoCache.containsKey(name)) {
                    TopicInfo ti = conn.getTopic(name);
                    topicInfoCache.put(name, ti);
                    return ti;
                }
                return topicInfoCache.get(name);
                
            default:
                throw new RuntimeException("Unknown destination type: " + type);
        }
    }
    
    public Collection<QueueInfo> getAllQueues() {
        return queueInfoCache.values();
    }
    
    public Collection<TopicInfo> getAllTopics() {
        return topicInfoCache.values();
    }
}
