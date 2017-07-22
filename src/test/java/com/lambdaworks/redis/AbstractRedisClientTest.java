/*
 * Copyright 2011-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lambdaworks.redis;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import com.lambdaworks.redis.api.sync.RedisCommands;

/**
 * @author Will Glozer
 * @author Mark Paluch
 */
public abstract class AbstractRedisClientTest extends AbstractTest {

    protected static RedisClient client;
    protected RedisCommands<String, String> redis;

    @BeforeClass
    public static void setupClient() {
        client = DefaultRedisClient.get();
        client.setOptions(ClientOptions.create());
    }

    protected static RedisClient newRedisClient() {
        return RedisClient.create(RedisURI.Builder.redis(host, port).build());
    }

    protected RedisCommands<String, String> connect() {
        RedisCommands<String, String> connect = client.connect().sync();
        return connect;
    }

    @Before
    public void openConnection() throws Exception {
        client.setOptions(ClientOptions.builder().build());
        redis = connect();
        boolean scriptRunning;
        do {

            scriptRunning = false;

            try {
                redis.flushall();
                redis.flushdb();
            } catch (RedisException e) {
                if (e.getMessage() != null && e.getMessage().contains("BUSY")) {
                    scriptRunning = true;
                    try {
                        redis.scriptKill();
                    } catch (RedisException e1) {
                        // I know, it sounds crazy, but there is a possibility where one of the commands above raises BUSY.
                        // Meanwhile the script ends and a call to SCRIPT KILL says NOTBUSY.
                    }
                }
            }
        } while (scriptRunning);
    }

    @After
    public void closeConnection() throws Exception {
        if (redis != null) {
            redis.close();
        }
    }

    public abstract class WithPasswordRequired {
        protected abstract void run(RedisClient client) throws Exception;

        public WithPasswordRequired() throws Exception {
            try {
                redis.configSet("requirepass", passwd);
                redis.auth(passwd);

                RedisClient client = newRedisClient();
                try {
                    run(client);
                } finally {
                    FastShutdown.shutdown(client);
                }
            } finally {

                redis.configSet("requirepass", "");
            }
        }
    }
}