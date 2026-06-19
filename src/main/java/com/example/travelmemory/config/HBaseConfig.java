package com.example.travelmemory.config;

import java.io.IOException;

import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration(proxyBeanMethods = false)
public class HBaseConfig {

    @Bean(destroyMethod = "close")
    @Lazy
    public Connection hBaseConnection(HBaseProperties properties) throws IOException {
        org.apache.hadoop.conf.Configuration configuration = HBaseConfiguration.create();
        configuration.set(HConstants.ZOOKEEPER_QUORUM, properties.quorum());
        configuration.setInt(HConstants.ZOOKEEPER_CLIENT_PORT, properties.clientPort());
        configuration.set(HConstants.ZOOKEEPER_ZNODE_PARENT, properties.znodeParent());
        return ConnectionFactory.createConnection(configuration);
    }
}
