/*
 * metrixagent - a java-agent to produce timing metrics
 * Copyright 2017 MeBigFatGuy.com
 * Copyright 2017 Dave Brosius
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

import java.lang.instrument.Instrumentation;

public class MetrixAgent {

    public static void premain(String agentArguments, Instrumentation instrumentation) {

        String[] parts = agentArguments.split("\\#");

        String[] packages = parts[0].split("(:|;)");
        boolean debugFlag;

        if (parts.length > 1) {
            debugFlag = "debug".equals(parts[1]);
        } else {
            debugFlag = false;
        }

        MetrixAgentTransformer mutator = new MetrixAgentTransformer(packages, debugFlag);
        instrumentation.addTransformer(mutator);
    }

}
