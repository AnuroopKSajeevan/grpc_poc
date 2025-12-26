package com.aksgrpc.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "com.aksgrpc.server.repository")
public class MongoDBConfig {

}

