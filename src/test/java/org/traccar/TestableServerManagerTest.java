package org.traccar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

// Unit test class for TestableServerManager
class TestableServerManagerTest {

    private TestableServerManager serverManager;
    private TrackerConnector mockConnector1;
    private TrackerConnector mockConnector2;
    private BaseProtocol mockProtocol;

    @BeforeEach
    public void setUp() {
        // Create mock objects for TrackerConnector and BaseProtocol (ensure not to depend on real network or protocol behavior)
        mockConnector1 = mock(TrackerConnector.class);
        mockConnector2 = mock(TrackerConnector.class);
        mockProtocol = mock(BaseProtocol.class);

        // Simulate expected protocol behavior for testing purposes.
        when(mockProtocol.getName()).thenReturn("TestProtocol");

        // Provide mock objects to TestableServerManager to ensure full control over dependencies.
        List<TrackerConnector> connectors = Arrays.asList(mockConnector1, mockConnector2);
        List<BaseProtocol> protocols = Arrays.asList(mockProtocol);

        serverManager = new TestableServerManager(connectors, protocols);
    }

    @Test
    public void testStartMethod() throws Exception {
        serverManager.start();
        verify(mockConnector1, times(1)).start();
        verify(mockConnector2, times(1)).start();
    }

    @Test
    public void testGetProtocol() {
        // Ensure getProtocol() retrieves the correct protocol based on its name.
        BaseProtocol result = serverManager.getProtocol("TestProtocol");
        assert result != null;
        assert result.getName().equals("TestProtocol");
    }

    @Test
    public void testDummyMethod() {
        // Validate that the dummy test instance does not contain any protocols.
        TestableServerManager dummyManager = TestableServerManager.createTestInstance();
        assert dummyManager.getProtocol("NonExistent") == null;
    }
}
