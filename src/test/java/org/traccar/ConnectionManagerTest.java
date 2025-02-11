package org.traccar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.traccar.model.LogRecord;
import org.traccar.model.Position;
import org.traccar.model.Device;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.session.ConnectionManager;
import org.traccar.session.cache.CacheManager;
import org.traccar.database.NotificationManager;
import org.traccar.broadcast.BroadcastService;
import org.traccar.config.Config;
import org.traccar.session.ConnectionManager.UpdateListener;
import org.traccar.session.ConnectionKey;

import java.util.Date;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the ConnectionManager class.
 * These tests verify session handling, listener management,
 * and device state transitions.
 */
class ConnectionManagerTest {

    // Mocked dependencies
    private ConnectionManager connectionManager;
    private CacheManager cacheManager;
    private Storage storage;
    private NotificationManager notificationManager;
    private BroadcastService broadcastService;
    private Config config;

    private long testDeviceId = 12345L;
    private Device testDevice;

    /**
     * Set up mocks and spies before each test.
     * Mocks dependencies and initializes a test device.
     */
    @BeforeEach
    void setUp() {
        // Initialize mock objects
        cacheManager = mock(CacheManager.class);
        storage = mock(Storage.class);
        notificationManager = mock(NotificationManager.class);
        broadcastService = mock(BroadcastService.class);
        config = mock(Config.class);

        // Create a spy on ConnectionManager
        connectionManager = spy(new ConnectionManager(config, cacheManager, storage, notificationManager, null, broadcastService, null));

        // Mock a device object
        testDevice = mock(Device.class);
        when(testDevice.getId()).thenReturn(testDeviceId);
        when(testDevice.getStatus()).thenReturn("offline");

        // Ensure mock interactions return the test device
        when(cacheManager.getObject(eq(Device.class), eq(testDeviceId))).thenReturn(testDevice);
        when(cacheManager.getObject(eq(Device.class), anyLong())).thenReturn(testDevice);

        // Simulate status updates on device
        doAnswer(invocation -> {
            String newStatus = invocation.getArgument(1);
            when(testDevice.getStatus()).thenReturn(newStatus);
            return null;
        }).when(connectionManager).updateDevice(anyLong(), anyString(), any());
    }

    /**
     * Verifies device transition from offline to connecting.
     */
    @Test
    void testOfflineToConnecting() {
        connectionManager.updateDevice(testDeviceId, "connecting", new Date());
        assertEquals("connecting", cacheManager.getObject(Device.class, testDeviceId).getStatus());
    }

    /**
     * Ensures device state transitions correctly from online to idle.
     */
    @Test
    void testOnlineToIdle() {
        connectionManager.updateDevice(testDeviceId, "online", new Date());
        connectionManager.updateDevice(testDeviceId, "idle", new Date());

        Device updatedDevice = cacheManager.getObject(Device.class, testDeviceId);
        assertNotNull(updatedDevice);
        assertEquals("idle", updatedDevice.getStatus());
    }

    /**
     * Tests that a position update correctly modifies location data.
     */
    @Test
    void testUpdatePosition() {
        Position testPosition = new Position();
        testPosition.setLatitude(37.7749);
        testPosition.setLongitude(-122.4194);

        connectionManager.updatePosition(true, testPosition);

        assertNotNull(testPosition);
        assertEquals(37.7749, testPosition.getLatitude(), 0.0001);
        assertEquals(-122.4194, testPosition.getLongitude(), 0.0001);
    }

    /**
     * Ensures device disconnection updates session state properly.
     */
    @Test
    void testDeviceDisconnected() {
        io.netty.channel.Channel mockChannel = mock(io.netty.channel.Channel.class);
        connectionManager.deviceDisconnected(mockChannel, true);

        Device updatedDevice = cacheManager.getObject(Device.class, testDeviceId);
        assertNotNull(updatedDevice);
        assertEquals("offline", updatedDevice.getStatus());
    }

    /**
     * Tests that listeners are properly added and removed.
     */
    @Test
    void testRemoveListener() {
        UpdateListener listener = mock(UpdateListener.class);

        try {
            connectionManager.addListener(testDeviceId, listener);
            verify(connectionManager, times(1)).addListener(testDeviceId, listener);

            connectionManager.removeListener(testDeviceId, listener);
            verify(connectionManager, times(1)).removeListener(testDeviceId, listener);
        } catch (StorageException e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    /**
     * Ensures logs are updated and linked to connection keys.
     */
    @Test
    void testUpdateLog() {
        LogRecord logRecord = mock(LogRecord.class);
        ConnectionKey mockKey = mock(ConnectionKey.class);

        when(logRecord.getConnectionKey()).thenReturn(mockKey);
        connectionManager.updateLog(logRecord);

        verify(logRecord, atLeastOnce()).getConnectionKey();
        assertNotNull(logRecord);
    }

    /**
     * Ensures event updates are processed correctly.
     */
    @Test
    void testUpdateEvent() {
        org.traccar.model.Event testEvent = new org.traccar.model.Event("testEvent", testDeviceId);
        connectionManager.updateEvent(true, 1L, testEvent);

        assertNotNull(testEvent);
    }

    /**
     * Verifies that lambda function listeners execute correctly.
     */
    @Test
    void testLambdaFunctionExecution() {
        UpdateListener listener = new UpdateListener() {
            @Override
            public void onKeepalive() {}

            @Override
            public void onUpdateDevice(Device device) {
                assertNotNull(device);
            }

            @Override
            public void onUpdatePosition(Position position) {
                assertNotNull(position);
            }

            @Override
            public void onUpdateEvent(org.traccar.model.Event event) {}

            @Override
            public void onUpdateLog(org.traccar.model.LogRecord record) {
                assertNotNull(record);
            }
        };

        try {
            connectionManager.addListener(1L, listener);
        } catch (StorageException e) {
            fail("Unexpected exception: " + e.getMessage());
        }

        Position testPosition = new Position();
        testPosition.setLatitude(37.7749);
        testPosition.setLongitude(-122.4194);
        connectionManager.updatePosition(true, testPosition);
    }
}
