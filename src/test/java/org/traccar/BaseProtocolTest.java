package org.traccar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.traccar.model.Command;
import org.traccar.sms.SmsManager;

class BaseProtocolTest {

    private BaseProtocol mockProtocol;
    private SmsManager mockSmsManager; // Mocked dependency to isolate SMS functionality

    @BeforeEach
    public void setUp() {
        // Create a mock instance of SmsManager to prevent real SMS operations
        mockSmsManager = mock(SmsManager.class);

        // Use a concrete subclass of BaseProtocol to ensure proper behavior
        mockProtocol = new TestProtocol();

        // Inject the mocked SmsManager into the protocol instance
        mockProtocol.setSmsManager(mockSmsManager);
    }

    @Test
    public void testSendTextCommand_Success() throws Exception {
        // Arrange: Set up a mock Command object with expected values
        Command mockCommand = mock(Command.class);
        when(mockCommand.getType()).thenReturn(Command.TYPE_CUSTOM);
        when(mockCommand.getString(Command.KEY_DATA)).thenReturn("Test SMS Message");

        String destAddress = "1234567890";

        // Act: Call the method that should trigger an SMS message
        mockProtocol.sendTextCommand(destAddress, mockCommand);

        // Assert: Verify that the sendMessage() method was called exactly once with the expected parameters
        verify(mockSmsManager, times(1)).sendMessage(destAddress, "Test SMS Message", true);
    }

    @Test
    public void testSendTextCommand_NoSmsManager() {
        // Arrange: Create a protocol instance without setting an SmsManager
        BaseProtocol protocolWithoutSms = new TestProtocol();

        Command mockCommand = mock(Command.class);
        when(mockCommand.getType()).thenReturn(Command.TYPE_CUSTOM);
        when(mockCommand.getString(Command.KEY_DATA)).thenReturn("Test SMS Message");

        // Act & Assert: Expect an exception since SmsManager is not set
        Exception exception = assertThrows(RuntimeException.class, () -> {
            protocolWithoutSms.sendTextCommand("1234567890", mockCommand);
        });

        assertEquals("SMS is not enabled", exception.getMessage());
    }

    // Concrete subclass to ensure the test runs without issues in BaseProtocol
    static class TestProtocol extends BaseProtocol {
        public TestProtocol() {
            super();
        }
    }
}
