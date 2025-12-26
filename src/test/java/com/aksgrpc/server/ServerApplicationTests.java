package com.aksgrpc.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ServerApplicationTests {

    @Test
    void applicationClassExists() {
        ServerApplication application = new ServerApplication();
        assertNotNull(application);
    }

}
