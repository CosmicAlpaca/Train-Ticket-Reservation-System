package test.java.com.shashi.service.impl;

import com.shashi.beans.HistoryBean;
import com.shashi.beans.TrainException;
import com.shashi.constant.ResponseCode;
import com.shashi.service.impl.BookingServiceImpl;
import com.shashi.utility.DBUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    private BookingServiceImpl bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingServiceImpl();
    }

    @Test
    void testGetAllBookingsByCustomerId_Success() throws SQLException, TrainException {
        // Arrange
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getString("transid")).thenReturn("trans1", "trans2");
        when(resultSet.getString("from_stn")).thenReturn("StationA", "StationB");
        when(resultSet.getString("to_stn")).thenReturn("StationB", "StationC");
        when(resultSet.getString("date")).thenReturn("2025-04-15", "2025-04-16");
        when(resultSet.getString("mailid")).thenReturn("user@example.com");
        when(resultSet.getInt("seats")).thenReturn(2, 3);
        when(resultSet.getDouble("amount")).thenReturn(100.50, 150.75);
        when(resultSet.getString("tr_no")).thenReturn("10001", "10002");
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act
            List<HistoryBean> bookings = bookingService.getAllBookingsByCustomerId("user@example.com");

            // Assert
            assertNotNull(bookings);
            assertEquals(2, bookings.size());
            HistoryBean firstBooking = bookings.get(0);
            assertEquals("trans1", firstBooking.getTransId());
            assertEquals("StationA", firstBooking.getFrom_stn());
            assertEquals("StationB", firstBooking.getTo_stn());
            assertEquals("2025-04-15", firstBooking.getDate());
            assertEquals("user@example.com", firstBooking.getMailId());
            assertEquals(2, firstBooking.getSeats());
            assertEquals(100.50, firstBooking.getAmount());
            assertEquals("10001", firstBooking.getTr_no());
            verify(preparedStatement).setString(1, "user@example.com");
            verify(preparedStatement).close();
        }
    }

    @Test
    void testGetAllBookingsByCustomerId_EmptyResult() throws SQLException, TrainException {
        // Arrange
        when(resultSet.next()).thenReturn(false);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act
            List<HistoryBean> bookings = bookingService.getAllBookingsByCustomerId("user@example.com");

            // Assert
            assertNotNull(bookings);
            assertTrue(bookings.isEmpty());
            verify(preparedStatement).setString(1, "user@example.com");
            verify(preparedStatement).close();
        }
    }

    @Test
    void testGetAllBookingsByCustomerId_SQLException() throws SQLException {
        // Arrange
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act & Assert
            TrainException exception = assertThrows(TrainException.class,
                    () -> bookingService.getAllBookingsByCustomerId("user@example.com"));
            assertEquals("DB Error", exception.getErrorMessage());
        }
    }

    @Test
    void testCreateHistory_Success() throws SQLException, TrainException {
        // Arrange
        HistoryBean details = createHistoryBean();
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act
            HistoryBean result = bookingService.createHistory(details);

            // Assert
            assertNotNull(result);
            assertNotNull(result.getTransId());
            assertEquals(details.getMailId(), result.getMailId());
            assertEquals(details.getTr_no(), result.getTr_no());
            assertEquals(details.getDate(), result.getDate());
            assertEquals(details.getFrom_stn(), result.getFrom_stn());
            assertEquals(details.getTo_stn(), result.getTo_stn());
            assertEquals(details.getSeats(), result.getSeats());
            assertEquals(details.getAmount(), result.getAmount());
            verify(preparedStatement).setString(2, details.getMailId());
            verify(preparedStatement).setString(3, details.getTr_no());
            verify(preparedStatement).setString(4, details.getDate());
            verify(preparedStatement).setString(5, details.getFrom_stn());
            verify(preparedStatement).setString(6, details.getTo_stn());
            verify(preparedStatement).setLong(7, (long) details.getSeats());
            verify(preparedStatement).setDouble(8, details.getAmount());
            verify(preparedStatement).close();
        }
    }

    @Test
    void testCreateHistory_Failure() throws SQLException {
        // Arrange
        HistoryBean details = createHistoryBean();
        when(preparedStatement.executeUpdate()).thenReturn(0);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act & Assert
            TrainException exception = assertThrows(TrainException.class,
                    () -> bookingService.createHistory(details));
            assertEquals(ResponseCode.INTERNAL_SERVER_ERROR.getMessage(), exception.getErrorMessage());
            verify(preparedStatement).close();
        }
    }

    @Test
    void testCreateHistory_SQLException() throws SQLException {
        // Arrange
        HistoryBean details = createHistoryBean();
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act & Assert
            TrainException exception = assertThrows(TrainException.class,
                    () -> bookingService.createHistory(details));
            assertEquals("DB Error", exception.getErrorMessage());
        }
    }

    private HistoryBean createHistoryBean() {
        HistoryBean bean = new HistoryBean();
        bean.setMailId("user@example.com");
        bean.setTr_no("10001");
        bean.setDate("2025-04-15");
        bean.setFrom_stn("StationA");
        bean.setTo_stn("StationB");
        bean.setSeats(2);
        bean.setAmount(100.50);
        return bean;
    }
}