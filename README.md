# metrixagent

metrixagent is a java agent that publishes method timing information to jmx using metrics-core.

To use, do

java -javaagent:metrixagent.jar=package1,package2 YourProgram

where package1, package2, package3 are package prefixes that you want to instrument.
