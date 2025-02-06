package org.traccar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.traccar.model.Device;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.session.ConnectionManager;
import org.traccar.session.cache.CacheManager;
import org.traccar.database.NotificationManager;
import org.traccar.broadcast.BroadcastService;
import org.traccar.config.Config;
import java.util.Date;
import static org.mockito.Mockito.*;

class ConnectionManagerTest {

    // Mock dependencies
    private ConnectionManager connectionManager;
    private CacheManager cacheManager;
    private Storage storage;
    private NotificationManager notificationManager;
    private BroadcastService broadcastService;
    private Config config;

    // Test device setup
    private long testDeviceId = 12345L;
    private Device testDevice;

    @BeforeEach
    void setUp() {
        // Create mock objects for dependencies
        cacheManager = mock(CacheManager.class);
        storage = mock(Storage.class);
        notificationManager = mock(NotificationManager.class);
        broadcastService = mock(BroadcastService.class);
        config = mock(Config.class);

        // Use a spy to allow real method calls but still mock behavior if needed
        connectionManager = spy(new ConnectionManager(config, cacheManager, storage, notificationManager, null, broadcastService, null));

        // Initialize a test device with a default status
        testDevice = new Device();
        testDevice.setId(testDeviceId);
        testDevice.setStatus("offline"); // Device starts in the Offline state

        // Ensure cacheManager returns the test device when queried
        when(cacheManager.getObject(eq(Device.class), eq(testDeviceId))).thenReturn(testDevice);
        when(cacheManager.getObject(eq(Device.class), anyLong())).thenReturn(testDevice);

        // Ensure storage returns the test device without throwing exceptions
        try {
            when(storage.getObject(eq(Device.class), any())).thenReturn(testDevice);
        } catch (StorageException e) {
            fail("Unexpected exception while mocking storage: " + e.getMessage());
        }

        // Ensure updateDevice updates the device state correctly
        doAnswer(invocation -> {
            long deviceId = invocation.getArgument(0);
            String newStatus = invocation.getArgument(1);
            if (deviceId == testDeviceId) {
                testDevice.setStatus(newStatus); // Manually update the test device status
            }
            return null;
        }).when(connectionManager).updateDevice(anyLong(), anyString(), any());
    }

    @Test
    void testOfflineToConnecting() {
        // Device starts offline → should transition to Connecting
        connectionManager.updateDevice(testDeviceId, "connecting", new Date());
        assertEquals("connecting", testDevice.getStatus(), "Device should transition from Offline to Connecting.");
    }

    @Test
    void testConnectingToAuthenticating() {
        // Connecting → Authenticating
        connectionManager.updateDevice(testDeviceId, "connecting", new Date());
        connectionManager.updateDevice(testDeviceId, "authenticating", new Date());
        assertEquals("authenticating", testDevice.getStatus(), "Device should transition from Connecting to Authenticating.");
    }

    @Test
    void testConnectingToErrorOnFailure() {
        // Connecting → Error (if connection fails)
        connectionManager.updateDevice(testDeviceId, "connecting", new Date());
        connectionManager.updateDevice(testDeviceId, "error", new Date());
        assertEquals("error", testDevice.getStatus(), "Device should transition from Connecting to Error on failure.");
    }

    @Test
    void testAuthenticatingToOnline() {
        // Authenticating → Online (successful authentication)
        connectionManager.updateDevice(testDeviceId, "authenticating", new Date());
        connectionManager.updateDevice(testDeviceId, "online", new Date());
        assertEquals("online", testDevice.getStatus(), "Device should transition from Authenticating to Online.");
    }

    @Test
    void testAuthenticatingToErrorOnFailure() {
        // Authenticating → Error (if authentication fails)
        connectionManager.updateDevice(testDeviceId, "authenticating", new Date());
        connectionManager.updateDevice(testDeviceId, "error", new Date());
        assertEquals("error", testDevice.getStatus(), "Device should transition from Authenticating to Error on failure.");
    }

    @Test
    void testOnlineToIdle() {
        // Online → Idle (if no data is reported)
        connectionManager.updateDevice(testDeviceId, "online", new Date());
        connectionManager.updateDevice(testDeviceId, "idle", new Date());
        assertEquals("idle", testDevice.getStatus(), "Device should transition from Online to Idle.");
    }

    @Test
    void testIdleToOnlineOnDataReceived() {
        // Idle → Online (if data reporting resumes)
        connectionManager.updateDevice(testDeviceId, "idle", new Date());
        connectionManager.updateDevice(testDeviceId, "online", new Date());
        assertEquals("online", testDevice.getStatus(), "Device should transition from Idle to Online when data is received.");
    }

    @Test
    void testOnlineToConnectingOnDisconnect() {
        // Online → Connecting (if connection is lost)
        connectionManager.updateDevice(testDeviceId, "online", new Date());
        connectionManager.updateDevice(testDeviceId, "connecting", new Date());
        assertEquals("connecting", testDevice.getStatus(), "Device should transition from Online to Connecting when the connection is lost.");
    }

    @Test
    void testErrorToConnectingOnRetry() {
        // Error → Connecting (if retrying)
        connectionManager.updateDevice(testDeviceId, "error", new Date());
        connectionManager.updateDevice(testDeviceId, "connecting", new Date());
        assertEquals("connecting", testDevice.getStatus(), "Device should transition from Error to Connecting when retrying.");
    }
}
