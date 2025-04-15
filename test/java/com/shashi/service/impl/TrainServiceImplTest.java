package test.java.com.shashi.service.impl;

import com.shashi.beans.TrainBean;
import com.shashi.beans.TrainException;
import com.shashi.constant.ResponseCode;
import com.shashi.service.impl.TrainServiceImpl;
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
class TrainServiceImplTest {

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    private TrainServiceImpl trainService;

    @BeforeEach
    void setUp() {
        trainService = new TrainServiceImpl();
    }

    @Test
    void testAddTrain_Success() throws SQLException {
        // Arrange
        TrainBean train = createTrainBean();
        when(resultSet.next()).thenReturn(true);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act
            String result = trainService.addTrain(train);

            // Assert
            assertEquals(ResponseCode.SUCCESS.toString(), result);
            verify(preparedStatement).setLong(1, train.getTr_no());
            verify(preparedStatement).setString(2, train.getTr_name());
            verify(preparedStatement).setString(3, train.getFrom_stn());
            verify(preparedStatement).setString(4, train.getTo_stn());
            verify(preparedStatement).setLong(5, train.getSeats());
            verify(preparedStatement).setDouble(6, train.getFare());
            verify(preparedStatement).close();
        }
    }

    @Test
    void testAddTrain_Failure() throws SQLException {
        // Arrange
        TrainBean train = createTrainBean();
        when(resultSet.next()).thenReturn(false);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act
            String result = trainService.addTrain(train);

            // Assert
            assertEquals(ResponseCode.FAILURE.toString(), result);
            verify(preparedStatement).close();
        }
    }

    @Test
    void testAddTrain_SQLException() throws SQLException {
        // Arrange
        TrainBean train = createTrainBean();
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("Insert failed"));
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act
            String result = trainService.addTrain(train);

            // Assert
            assertTrue(result.startsWith(ResponseCode.FAILURE.toString()));
            assertTrue(result.contains("Insert failed"));
        }
    }

    @Test
    void testDeleteTrainById_Success() throws SQLException {
        // Arrange
        String trainNo = "10001";
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act
            String result = trainService.deleteTrainById(trainNo);

            // Assert
            assertEquals(ResponseCode.SUCCESS.toString(), result);
            verify(preparedStatement).setString(1, trainNo);
            verify(preparedStatement).close();
        }
    }

    @Test
    void testDeleteTrainById_Failure() throws SQLException {
        // Arrange
        String trainNo = "10001";
        when(preparedStatement.executeUpdate()).thenReturn(0);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act
            String result = trainService.deleteTrainById(trainNo);

            // Assert
            assertEquals(ResponseCode.FAILURE.toString(), result);
            verify(preparedStatement).close();
        }
    }

    @Test
    void testDeleteTrainById_SQLException() throws SQLException {
        // Arrange
        String trainNo = "10001";
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("Delete failed"));
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act
            String result = trainService.deleteTrainById(trainNo);

            // Assert
            assertTrue(result.startsWith(ResponseCode.FAILURE.toString()));
            assertTrue(result.contains("Delete failed"));
        }
    }

    @Test
    void testUpdateTrain_Success() throws SQLException {
        // Arrange
        TrainBean train = createTrainBean();
        when(resultSet.next()).thenReturn(true);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act
            String result = trainService.updateTrain(train);

            // Assert
            assertEquals(ResponseCode.SUCCESS.toString(), result);
            verify(preparedStatement).setString(1, train.getTr_name());
            verify(preparedStatement).setString(2, train.getFrom_stn());
            verify(preparedStatement).setString(3, train.getTo_stn());
            verify(preparedStatement).setLong(4, train.getSeats());
            verify(preparedStatement).setDouble(5, train.getFare());
            verify(preparedStatement).setDouble(6, train.getTr_no());
            verify(preparedStatement).close();
        }
    }

    @Test
    void testUpdateTrain_Failure() throws SQLException {
        // Arrange
        TrainBean train = createTrainBean();
        when(resultSet.next()).thenReturn(false);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act
            String result = trainService.updateTrain(train);

            // Assert
            assertEquals(ResponseCode.FAILURE.toString(), result);
            verify(preparedStatement).close();
        }
    }

    @Test
    void testUpdateTrain_SQLException() throws SQLException {
        // Arrange
        TrainBean train = createTrainBean();
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("Update failed"));
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act
            String result = trainService.updateTrain(train);

            // Assert
            assertTrue(result.startsWith(ResponseCode.FAILURE.toString()));
            assertTrue(result.contains("Update failed"));
        }
    }

    @Test
    void testGetTrainById_Success() throws SQLException, TrainException {
        // Arrange
        String trainNo = "10001";
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getDouble("fare")).thenReturn(500.50);
        when(resultSet.getString("from_stn")).thenReturn("StationA");
        when(resultSet.getString("to_stn")).thenReturn("StationB");
        when(resultSet.getString("tr_name")).thenReturn("Express");
        when(resultSet.getLong("tr_no")).thenReturn(10001L);
        when(resultSet.getInt("seats")).thenReturn(100);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act
            TrainBean train = trainService.getTrainById(trainNo);

            // Assert
            assertNotNull(train);
            assertEquals(500.50, train.getFare());
            assertEquals("StationA", train.getFrom_stn());
            assertEquals("StationB", train.getTo_stn());
            assertEquals("Express", train.getTr_name());
            assertEquals(10001L, train.getTr_no());
            assertEquals(100, train.getSeats());
            verify(preparedStatement).setString(1, trainNo);
            verify(preparedStatement).close();
        }
    }

    @Test
    void testGetTrainById_NotFound() throws SQLException, TrainException {
        // Arrange
        String trainNo = "99999";
        when(resultSet.next()).thenReturn(false);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act
            TrainBean train = trainService.getTrainById(trainNo);

            // Assert
            assertNull(train);
            verify(preparedStatement).setString(1, trainNo);
            verify(preparedStatement).close();
        }
    }

    @Test
    void testGetTrainById_SQLException() throws SQLException {
        // Arrange
        String trainNo = "10001";
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("Query failed"));
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act & Assert
            TrainException exception = assertThrows(TrainException.class,
                    () -> trainService.getTrainById(trainNo));
            assertEquals("Query failed", exception.getErrorMessage());
        }
    }

    @Test
    void testGetAllTrains_Success() throws SQLException, TrainException {
        // Arrange
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getDouble("fare")).thenReturn(500.50, 600.75);
        when(resultSet.getString("from_stn")).thenReturn("StationA", "StationC");
        when(resultSet.getString("to_stn")).thenReturn("StationB", "StationD");
        when(resultSet.getString("tr_name")).thenReturn("Express", "Superfast");
        when(resultSet.getLong("tr_no")).thenReturn(10001L, 10002L);
        when(resultSet.getInt("seats")).thenReturn(100, 150);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act
            List<TrainBean> trains = trainService.getAllTrains();

            // Assert
            assertNotNull(trains);
            assertEquals(2, trains.size());
            TrainBean firstTrain = trains.get(0);
            assertEquals(500.50, firstTrain.getFare());
            assertEquals("StationA", firstTrain.getFrom_stn());
            assertEquals("StationB", firstTrain.getTo_stn());
            assertEquals("Express", firstTrain.getTr_name());
            assertEquals(10001L, firstTrain.getTr_no());
            assertEquals(100, firstTrain.getSeats());
            verify(preparedStatement).close();
        }
    }

    @Test
    void testGetAllTrains_EmptyResult() throws SQLException, TrainException {
        // Arrange
        when(resultSet.next()).thenReturn(false);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act
            List<TrainBean> trains = trainService.getAllTrains();

            // Assert
            assertNotNull(trains);
            assertTrue(trains.isEmpty());
            verify(preparedStatement).close();
        }
    }

    @Test
    void testGetAllTrains_SQLException() throws SQLException {
        // Arrange
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("Query failed"));
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act & Assert
            TrainException exception = assertThrows(TrainException.class,
                    () -> trainService.getAllTrains());
            assertEquals("Query failed", exception.getErrorMessage());
        }
    }

    @Test
    void testGetTrainsBetweenStations_Success() throws SQLException, TrainException {
        // Arrange
        String fromStation = "StationA";
        String toStation = "StationB";
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getDouble("fare")).thenReturn(500.50);
        when(resultSet.getString("from_stn")).thenReturn("StationA");
        when(resultSet.getString("to_stn")).thenReturn("StationB");
        when(resultSet.getString("tr_name")).thenReturn("Express");
        when(resultSet.getLong("tr_no")).thenReturn(10001L);
        when(resultSet.getInt("seats")).thenReturn(100);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act
            List<TrainBean> trains = trainService.getTrainsBetweenStations(fromStation, toStation);

            // Assert
            assertNotNull(trains);
            assertEquals(1, trains.size());
            TrainBean train = trains.get(0);
            assertEquals(500.50, train.getFare());
            assertEquals("StationA", train.getFrom_stn());
            assertEquals("StationB", train.getTo_stn());
            assertEquals("Express", train.getTr_name());
            assertEquals(10001L, train.getTr_no());
            assertEquals(100, train.getSeats());
            verify(preparedStatement).setString(1, "%StationA%");
            verify(preparedStatement).setString(2, "%StationB%");
            verify(preparedStatement).close();
        }
    }

    @Test
    void testGetTrainsBetweenStations_EmptyResult() throws SQLException, TrainException {
        // Arrange
        String fromStation = "StationX";
        String toStation = "StationY";
        when(resultSet.next()).thenReturn(false);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act
            List<TrainBean> trains = trainService.getTrainsBetweenStations(fromStation, toStation);

            // Assert
            assertNotNull(trains);
            assertTrue(trains.isEmpty());
            verify(preparedStatement).setString(1, "%StationX%");
            verify(preparedStatement).setString(2, "%StationY%");
            verify(preparedStatement).close();
        }
    }

    @Test
    void testGetTrainsBetweenStations_SQLException() throws SQLException {
        // Arrange
        String fromStation = "StationA";
        String toStation = "StationB";
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("Query failed"));
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act & Assert
            TrainException exception = assertThrows(TrainException.class,
                    () -> trainService.getTrainsBetweenStations(fromStation, toStation));
            assertEquals("Query failed", exception.getErrorMessage());
        }
    }

    private TrainBean createTrainBean() {
        TrainBean train = new TrainBean();
        train.setTr_no(10001L);
        train.setTr_name("Express");
        train.setFrom_stn("StationA");
        train.setTo_stn("StationB");
        train.setSeats(100);
        train.setFare(500.50);
        return train;
    }
}