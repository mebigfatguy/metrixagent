package com.mebigfatguy.metrixagent;

import java.lang.instrument.Instrumentation;

public class MetrixAgent {

	public static void premain(String agentArguments, Instrumentation instrumentation) {

		String[] packages = agentArguments.split(":");

		MetrixAgentTransformer mutator = new MetrixAgentTransformer(packages);
		instrumentation.addTransformer(mutator);
	}

}
