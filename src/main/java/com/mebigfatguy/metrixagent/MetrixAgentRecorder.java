/*
 * metrixagent - a java-agent to produce timing metrics
 * Copyright 2017-2019 MeBigFatGuy.com
 * Copyright 2017-2019 Dave Brosius
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.mebigfatguy.metrixagent;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.jmx.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

public class MetrixAgentRecorder {

    private static final Logger logger = LoggerFactory.getLogger(MetrixAgentRecorder.class);
    private static final MetricRegistry metrics = new MetricRegistry();
    private static final JmxReporter reporter = JmxReporter.forRegistry(metrics).createsObjectNamesWith(new MetrixAgentObjectNameFactory()).build();

    static {
        reporter.start();
    }

    public static void record(long time, String fqMethod) {
        try {
            Timer t = metrics.timer(fqMethod);
            t.update(time, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            // make sure no exceptions leak out of this method
            logger.error("Failed creating and updating metrix for {}", fqMethod);
        }
    }
}
