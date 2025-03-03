package org.traccar;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// This class is created separately from ServerManager to demonstrate improved testability
// without modifying the existing Traccar codebase. The goal is to allow unit testing
// with controlled dependencies, ensuring better flexibility and isolation in tests.
public class TestableServerManager {

    private final List<TrackerConnector> connectorList;
    private final Map<String, BaseProtocol> protocolList = new ConcurrentHashMap<>();

    // Constructor takes explicit dependencies instead of relying on dependency injection. (allow test to inject mock objects)
    public TestableServerManager(List<TrackerConnector> connectors, List<BaseProtocol> protocols) {
        this.connectorList = connectors;
        for (BaseProtocol protocol : protocols) {
            protocolList.put(protocol.getName(), protocol);
        }
    }

    // Retrieves a protocol by its name, allowing tests to verify expected behavior.
    public BaseProtocol getProtocol(String name) {
        return protocolList.get(name);
    }

    // Starts all tracker connectors (ensuring that each can be tested in isolation)
    public void start() {
        for (TrackerConnector connector : connectorList) {
            try {
                connector.start();
            } catch (Exception e) {
                System.out.println("Failed to start connector: " + e.getMessage());
            }
        }
    }

    // A static factory method (avoid need of real dependencies)
    public static TestableServerManager createTestInstance() {
        return new TestableServerManager(List.of(), List.of());
    }
}
