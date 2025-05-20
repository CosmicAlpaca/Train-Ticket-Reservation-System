package test.java.com.shashi.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.shashi.beans.TrainBean;
import com.shashi.beans.TrainException;
import com.shashi.constant.ResponseCode;
import com.shashi.service.impl.TrainServiceImpl;
import com.shashi.utility.DBUtil;

@ExtendWith(MockitoExtension.class)
class TrainServiceImplTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private ResultSet mockResultSet;

    @InjectMocks
    private TrainServiceImpl trainService;

    private MockedStatic<DBUtil> mockedDBUtil;

    @BeforeEach
    void setUp() throws Exception {
        // Mock the static DBUtil.getConnection() method
        mockedDBUtil = Mockito.mockStatic(DBUtil.class);
        mockedDBUtil.when(DBUtil::getConnection).thenReturn(mockConnection);

        // Common mock behavior for PreparedStatement
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
    }

    @AfterEach
    void tearDown() {
        // Close the static mock
        mockedDBUtil.close();
    }

    private TrainBean createSampleTrain() {
        TrainBean train = new TrainBean();
        train.setTr_no(12345L);
        train.setTr_name("Test Express");
        train.setFrom_stn("Station A");
        train.setTo_stn("Station B");
        train.setSeats(100);
        train.setFare(500.00);
        return train;
    }

    @Test
    void testAddTrain_Success() throws SQLException, TrainException {
        TrainBean train = createSampleTrain();
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true); // Simulate successful insertion indication

        String result = trainService.addTrain(train);

        assertEquals(ResponseCode.SUCCESS.toString(), result);
        verify(mockPreparedStatement).setLong(1, train.getTr_no());
        verify(mockPreparedStatement).setString(2, train.getTr_name());
        verify(mockPreparedStatement).setString(3, train.getFrom_stn());
        verify(mockPreparedStatement).setString(4, train.getTo_stn());
        verify(mockPreparedStatement).setLong(5, train.getSeats());
        verify(mockPreparedStatement).setDouble(6, train.getFare());
        verify(mockPreparedStatement).executeQuery();
        verify(mockPreparedStatement).close();
    }
    
    @Test
    void testAddTrain_Failure_NoRowAffected() throws SQLException, TrainException {
        // This test highlights a potential issue in the original code.
        // INSERT typically uses executeUpdate() and checks the int return value.
        // executeQuery() for INSERT is unusual. If rs.next() is false, it means no result set was generated.
        TrainBean train = createSampleTrain();
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false); // Simulate failure or no result set

        String result = trainService.addTrain(train);

        assertEquals(ResponseCode.FAILURE.toString(), result);
        verify(mockPreparedStatement).executeQuery();
        verify(mockPreparedStatement).close();
    }


    @Test
    void testAddTrain_SQLException() throws SQLException, TrainException {
        TrainBean train = createSampleTrain();
        when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException("DB Error"));

        String result = trainService.addTrain(train);

        assertTrue(result.startsWith(ResponseCode.FAILURE.toString()));
        assertTrue(result.contains("DB Error"));
        verify(mockPreparedStatement).close(); // Should still try to close
    }
    
    @Test
    void testAddTrain_TrainExceptionFromDBUtil() throws SQLException, TrainException {
        // Test scenario where DBUtil.getConnection() itself throws TrainException
        mockedDBUtil.when(DBUtil::getConnection).thenThrow(new TrainException("DBUtil Connection Failed"));
        TrainBean train = createSampleTrain();

        // Re-initialize trainService or use a fresh instance if @InjectMocks doesn't re-evaluate DBUtil call
        // For simplicity, we'll assume the injected service will re-attempt DBUtil.getConnection()
        
        String result = trainService.addTrain(train);
        
        assertTrue(result.startsWith(ResponseCode.FAILURE.toString()));
        assertTrue(result.contains("DBUtil Connection Failed"));
        // PreparedStatement wouldn't be created or closed in this specific path
        verify(mockConnection, never()).prepareStatement(anyString());
        verify(mockPreparedStatement, never()).close();
    }


    @Test
    void testDeleteTrainById_Success() throws SQLException, TrainException {
        String trainNo = "12345";
        when(mockPreparedStatement.executeUpdate()).thenReturn(1); // 1 row affected

        String result = trainService.deleteTrainById(trainNo);

        assertEquals(ResponseCode.SUCCESS.toString(), result);
        verify(mockPreparedStatement).setString(1, trainNo);
        verify(mockPreparedStatement).executeUpdate();
        verify(mockPreparedStatement).close();
    }

    @Test
    void testDeleteTrainById_Failure_NoRowAffected() throws SQLException, TrainException {
        String trainNo = "12345";
        when(mockPreparedStatement.executeUpdate()).thenReturn(0); // 0 rows affected

        String result = trainService.deleteTrainById(trainNo);

        assertEquals(ResponseCode.FAILURE.toString(), result);
        verify(mockPreparedStatement).executeUpdate();
        verify(mockPreparedStatement).close();
    }

    @Test
    void testDeleteTrainById_SQLException() throws SQLException, TrainException {
        String trainNo = "12345";
        when(mockPreparedStatement.executeUpdate()).thenThrow(new SQLException("Delete Error"));

        String result = trainService.deleteTrainById(trainNo);

        assertTrue(result.startsWith(ResponseCode.FAILURE.toString()));
        assertTrue(result.contains("Delete Error"));
        verify(mockPreparedStatement).close();
    }

    @Test
    void testUpdateTrain_Success() throws SQLException, TrainException {
        TrainBean train = createSampleTrain();
        // The original code uses executeQuery and rs.next() for UPDATE, which is unconventional.
        // Typically, executeUpdate() returning an int (rows affected) is used.
        // We test the code as is.
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true); // Simulate success indication

        String result = trainService.updateTrain(train);

        assertEquals(ResponseCode.SUCCESS.toString(), result);
        verify(mockPreparedStatement).setString(1, train.getTr_name());
        verify(mockPreparedStatement).setString(2, train.getFrom_stn());
        verify(mockPreparedStatement).setString(3, train.getTo_stn());
        verify(mockPreparedStatement).setLong(4, train.getSeats());
        verify(mockPreparedStatement).setDouble(5, train.getFare());
        verify(mockPreparedStatement).setDouble(6, train.getTr_no()); // Original code uses setDouble for tr_no here
        verify(mockPreparedStatement).executeQuery();
        verify(mockPreparedStatement).close();
    }
    
    @Test
    void testUpdateTrain_Failure_NoRowAffected() throws SQLException, TrainException {
        TrainBean train = createSampleTrain();
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false); 

        String result = trainService.updateTrain(train);

        assertEquals(ResponseCode.FAILURE.toString(), result);
        verify(mockPreparedStatement).executeQuery();
        verify(mockPreparedStatement).close();
    }

    @Test
    void testUpdateTrain_SQLException() throws SQLException, TrainException {
        TrainBean train = createSampleTrain();
        when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException("Update Error"));

        String result = trainService.updateTrain(train);

        assertTrue(result.startsWith(ResponseCode.FAILURE.toString()));
        assertTrue(result.contains("Update Error"));
        verify(mockPreparedStatement).close();
    }

    @Test
    void testGetTrainById_Found() throws SQLException, TrainException {
        String trainNo = "12345";
        TrainBean expectedTrain = createSampleTrain();
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true).thenReturn(false); // Found one row
        when(mockResultSet.getDouble("fare")).thenReturn(expectedTrain.getFare());
        when(mockResultSet.getString("from_stn")).thenReturn(expectedTrain.getFrom_stn());
        when(mockResultSet.getString("to_stn")).thenReturn(expectedTrain.getTo_stn());
        when(mockResultSet.getString("tr_name")).thenReturn(expectedTrain.getTr_name());
        when(mockResultSet.getLong("tr_no")).thenReturn(expectedTrain.getTr_no());
        when(mockResultSet.getInt("seats")).thenReturn(expectedTrain.getSeats());

        TrainBean actualTrain = trainService.getTrainById(trainNo);

        assertNotNull(actualTrain);
        assertEquals(expectedTrain.getTr_no(), actualTrain.getTr_no());
        assertEquals(expectedTrain.getTr_name(), actualTrain.getTr_name());
        verify(mockPreparedStatement).setString(1, trainNo);
        verify(mockPreparedStatement).executeQuery();
        verify(mockPreparedStatement).close();
    }

    @Test
    void testGetTrainById_NotFound() throws SQLException, TrainException {
        String trainNo = "99999";
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false); // No row found

        TrainBean actualTrain = trainService.getTrainById(trainNo);

        assertNull(actualTrain);
        verify(mockPreparedStatement).executeQuery();
        verify(mockPreparedStatement).close();
    }

    @Test
    void testGetTrainById_SQLException() throws SQLException {
        String trainNo = "12345";
        when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException("Fetch Error"));

        TrainException exception = assertThrows(TrainException.class, () -> {
            trainService.getTrainById(trainNo);
        });

        assertEquals("Fetch Error", exception.getMessage());
        verify(mockPreparedStatement).close(); // Should still try to close
    }

    @Test
    void testGetAllTrains_Success_MultipleTrains() throws SQLException, TrainException {
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        // Simulate two trains
        when(mockResultSet.next()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(mockResultSet.getDouble("fare")).thenReturn(500.0, 600.0);
        when(mockResultSet.getString("from_stn")).thenReturn("Station A", "Station C");
        when(mockResultSet.getString("to_stn")).thenReturn("Station B", "Station D");
        when(mockResultSet.getString("tr_name")).thenReturn("Express 1", "Express 2");
        when(mockResultSet.getLong("tr_no")).thenReturn(12345L, 67890L);
        when(mockResultSet.getInt("seats")).thenReturn(100, 150);

        List<TrainBean> trains = trainService.getAllTrains();

        assertNotNull(trains);
        assertEquals(2, trains.size());
        assertEquals(12345L, trains.get(0).getTr_no());
        assertEquals("Express 1", trains.get(0).getTr_name());
        assertEquals(67890L, trains.get(1).getTr_no());
        assertEquals("Express 2", trains.get(1).getTr_name());
        verify(mockPreparedStatement).executeQuery();
        verify(mockPreparedStatement).close();
    }

    @Test
    void testGetAllTrains_Success_NoTrains() throws SQLException, TrainException {
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false); // No trains found

        List<TrainBean> trains = trainService.getAllTrains();

        assertNotNull(trains);
        assertTrue(trains.isEmpty());
        verify(mockPreparedStatement).executeQuery();
        verify(mockPreparedStatement).close();
    }

    @Test
    void testGetAllTrains_SQLException() throws SQLException {
        when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException("Fetch All Error"));

        TrainException exception = assertThrows(TrainException.class, () -> {
            trainService.getAllTrains();
        });

        assertEquals("Fetch All Error", exception.getMessage());
        verify(mockPreparedStatement).close();
    }

    @Test
    void testGetTrainsBetweenStations_Success_Found() throws SQLException, TrainException {
        String fromStation = "StationX";
        String toStation = "StationY";
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true).thenReturn(false); // One train found
        when(mockResultSet.getDouble("fare")).thenReturn(700.0);
        when(mockResultSet.getString("from_stn")).thenReturn("StationX_Exact");
        when(mockResultSet.getString("to_stn")).thenReturn("StationY_Exact");
        when(mockResultSet.getString("tr_name")).thenReturn("Intercity");
        when(mockResultSet.getLong("tr_no")).thenReturn(11223L);
        when(mockResultSet.getInt("seats")).thenReturn(200);

        List<TrainBean> trains = trainService.getTrainsBetweenStations(fromStation, toStation);

        assertNotNull(trains);
        assertEquals(1, trains.size());
        assertEquals(11223L, trains.get(0).getTr_no());
        assertEquals("Intercity", trains.get(0).getTr_name());
        verify(mockPreparedStatement).setString(1, "%" + fromStation + "%");
        verify(mockPreparedStatement).setString(2, "%" + toStation + "%");
        verify(mockPreparedStatement).executeQuery();
        verify(mockPreparedStatement).close();
    }

    @Test
    void testGetTrainsBetweenStations_Success_NotFound() throws SQLException, TrainException {
        String fromStation = "NonExistentA";
        String toStation = "NonExistentB";
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false); // No trains found

        List<TrainBean> trains = trainService.getTrainsBetweenStations(fromStation, toStation);

        assertNotNull(trains);
        assertTrue(trains.isEmpty());
        verify(mockPreparedStatement).executeQuery();
        verify(mockPreparedStatement).close();
    }

    @Test
    void testGetTrainsBetweenStations_SQLException() throws SQLException {
        String fromStation = "StationX";
        String toStation = "StationY";
        when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException("Search Error"));

        TrainException exception = assertThrows(TrainException.class, () -> {
            trainService.getTrainsBetweenStations(fromStation, toStation);
        });

        assertEquals("Search Error", exception.getMessage());
        verify(mockPreparedStatement).close();
    }
}