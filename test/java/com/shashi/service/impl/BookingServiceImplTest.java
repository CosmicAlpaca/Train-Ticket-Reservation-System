package test.java.com.shashi.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.shashi.beans.HistoryBean;
import com.shashi.beans.TrainException;
import com.shashi.constant.ResponseCode;
import com.shashi.service.impl.BookingServiceImpl;
import com.shashi.utility.DBUtil;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private ResultSet mockResultSet;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private MockedStatic<DBUtil> mockedDBUtil;
    private MockedStatic<UUID> mockedUUID;
    private final String testUuidString = "123e4567-e89b-12d3-a456-426614174000";
    private final UUID testUuid = UUID.fromString(testUuidString);

    @BeforeEach
    void setUp() throws Exception {
        mockedDBUtil = Mockito.mockStatic(DBUtil.class);
        mockedDBUtil.when(DBUtil::getConnection).thenReturn(mockConnection);

        mockedUUID = Mockito.mockStatic(UUID.class);
        mockedUUID.when(UUID::randomUUID).thenReturn(testUuid);


        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
    }

    @AfterEach
    void tearDown() {
        mockedDBUtil.close();
        mockedUUID.close();
    }

    private HistoryBean createSampleHistoryBean() {
        HistoryBean history = new HistoryBean();
        history.setMailId("test@example.com");
        history.setTr_no("T123");
        history.setDate("2023-10-27");
        history.setFrom_stn("Station A");
        history.setTo_stn("Station B");
        history.setSeats(2);
        history.setAmount(200.00);
        // transId is set by the service
        return history;
    }

    @Test
    void testGetAllBookingsByCustomerId_Success_FoundBookings() throws SQLException, TrainException {
        String customerEmailId = "test@example.com";
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        // Simulate two bookings found
        when(mockResultSet.next()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(mockResultSet.getString("transid")).thenReturn("TXN001", "TXN002");
        when(mockResultSet.getString("from_stn")).thenReturn("Station A", "Station C");
        when(mockResultSet.getString("to_stn")).thenReturn("Station B", "Station D");
        when(mockResultSet.getString("date")).thenReturn("2023-10-26", "2023-10-27");
        when(mockResultSet.getString("mailid")).thenReturn(customerEmailId); // Both for the same user
        when(mockResultSet.getInt("seats")).thenReturn(2, 1);
        when(mockResultSet.getDouble("amount")).thenReturn(200.50, 150.75);
        when(mockResultSet.getString("tr_no")).thenReturn("T123", "T456");

        List<HistoryBean> bookings = bookingService.getAllBookingsByCustomerId(customerEmailId);

        assertNotNull(bookings);
        assertEquals(2, bookings.size());

        assertEquals("TXN001", bookings.get(0).getTransId());
        assertEquals("Station A", bookings.get(0).getFrom_stn());
        assertEquals(2, bookings.get(0).getSeats());

        assertEquals("TXN002", bookings.get(1).getTransId());
        assertEquals("Station C", bookings.get(1).getFrom_stn());
        assertEquals(1, bookings.get(1).getSeats());

        verify(mockPreparedStatement).setString(1, customerEmailId);
        verify(mockPreparedStatement).executeQuery();
        verify(mockPreparedStatement).close();
    }

    @Test
    void testGetAllBookingsByCustomerId_Success_NoBookingsFound() throws SQLException, TrainException {
        String customerEmailId = "newuser@example.com";
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false); // No bookings

        List<HistoryBean> bookings = bookingService.getAllBookingsByCustomerId(customerEmailId);

        assertNotNull(bookings);
        assertTrue(bookings.isEmpty());

        verify(mockPreparedStatement).setString(1, customerEmailId);
        verify(mockPreparedStatement).executeQuery();
        verify(mockPreparedStatement).close();
    }

    @Test
    void testGetAllBookingsByCustomerId_SQLException() throws SQLException {
        String customerEmailId = "test@example.com";
        when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException("Database query error"));

        TrainException exception = assertThrows(TrainException.class, () -> {
            bookingService.getAllBookingsByCustomerId(customerEmailId);
        });

        assertEquals("Database query error", exception.getMessage());
        verify(mockPreparedStatement).setString(1, customerEmailId);
        verify(mockPreparedStatement).executeQuery();
        verify(mockPreparedStatement).close(); // Ensure close is called even on exception
    }

    @Test
    void testGetAllBookingsByCustomerId_TrainExceptionFromDBUtil() throws TrainException, SQLException {
        String customerEmailId = "test@example.com";
        // Override the DBUtil mock for this specific test
        mockedDBUtil.when(DBUtil::getConnection).thenThrow(new TrainException("DB Connection Failed"));

        TrainException exception = assertThrows(TrainException.class, () -> {
            bookingService.getAllBookingsByCustomerId(customerEmailId);
        });
        
        assertEquals("DB Connection Failed", exception.getMessage());
        verify(mockConnection, never()).prepareStatement(anyString()); // PreparedStatement should not be created
        verify(mockPreparedStatement, never()).close(); // And thus not closed
    }


    @Test
    void testCreateHistory_Success() throws SQLException, TrainException {
        HistoryBean inputDetails = createSampleHistoryBean();
        when(mockPreparedStatement.executeUpdate()).thenReturn(1); // 1 row affected

        HistoryBean createdHistory = bookingService.createHistory(inputDetails);

        assertNotNull(createdHistory);
        assertEquals(testUuidString, createdHistory.getTransId()); // Check if the mocked UUID is set
        assertEquals(inputDetails.getMailId(), createdHistory.getMailId());
        assertEquals(inputDetails.getTr_no(), createdHistory.getTr_no());
        assertEquals(inputDetails.getDate(), createdHistory.getDate());
        assertEquals(inputDetails.getFrom_stn(), createdHistory.getFrom_stn());
        assertEquals(inputDetails.getTo_stn(), createdHistory.getTo_stn());
        assertEquals(inputDetails.getSeats(), createdHistory.getSeats());
        assertEquals(inputDetails.getAmount(), createdHistory.getAmount());

        verify(mockPreparedStatement).setString(1, testUuidString); // Verify UUID was used
        verify(mockPreparedStatement).setString(2, inputDetails.getMailId());
        verify(mockPreparedStatement).setString(3, inputDetails.getTr_no());
        verify(mockPreparedStatement).setString(4, inputDetails.getDate());
        verify(mockPreparedStatement).setString(5, inputDetails.getFrom_stn());
        verify(mockPreparedStatement).setString(6, inputDetails.getTo_stn());
        verify(mockPreparedStatement).setLong(7, inputDetails.getSeats());
        verify(mockPreparedStatement).setDouble(8, inputDetails.getAmount());
        verify(mockPreparedStatement).executeUpdate();
        verify(mockPreparedStatement).close();
    }

    @Test
    void testCreateHistory_Failure_NoRowAffected() throws SQLException {
        HistoryBean inputDetails = createSampleHistoryBean();
        when(mockPreparedStatement.executeUpdate()).thenReturn(0); // 0 rows affected

        TrainException exception = assertThrows(TrainException.class, () -> {
            bookingService.createHistory(inputDetails);
        });

        // The original code throws new TrainException(ResponseCode.INTERNAL_SERVER_ERROR);
        // which means the message will be "INTERNAL_SERVER_ERROR"
        assertEquals(ResponseCode.INTERNAL_SERVER_ERROR.toString(), exception.getMessage());

        verify(mockPreparedStatement).executeUpdate();
        verify(mockPreparedStatement).close();
    }

    @Test
    void testCreateHistory_SQLException() throws SQLException {
        HistoryBean inputDetails = createSampleHistoryBean();
        when(mockPreparedStatement.executeUpdate()).thenThrow(new SQLException("Database insert error"));

        TrainException exception = assertThrows(TrainException.class, () -> {
            bookingService.createHistory(inputDetails);
        });

        assertEquals("Database insert error", exception.getMessage());
        verify(mockPreparedStatement).executeUpdate();
        verify(mockPreparedStatement).close();
    }
    
    @Test
    void testCreateHistory_TrainExceptionFromDBUtil() throws TrainException, SQLException {
        HistoryBean inputDetails = createSampleHistoryBean();
        // Override the DBUtil mock for this specific test
        mockedDBUtil.when(DBUtil::getConnection).thenThrow(new TrainException("DB Connection Failed on Create"));

        TrainException exception = assertThrows(TrainException.class, () -> {
            bookingService.createHistory(inputDetails);
        });
        
        assertEquals("DB Connection Failed on Create", exception.getMessage());
        verify(mockConnection, never()).prepareStatement(anyString());
        verify(mockPreparedStatement, never()).close();
    }
}