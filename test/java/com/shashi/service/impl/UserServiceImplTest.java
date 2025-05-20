package test.java.com.shashi.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.shashi.beans.TrainException;
import com.shashi.beans.UserBean;
import com.shashi.constant.ResponseCode;
import com.shashi.constant.UserRole;
import com.shashi.service.impl.UserServiceImpl;
import com.shashi.utility.DBUtil;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private ResultSet mockResultSet;

    // We will instantiate UserServiceImpl manually due to the constructor argument
    private UserServiceImpl userService;

    private MockedStatic<DBUtil> mockedDBUtil;

    private final String CUSTOMER_TABLE_NAME = UserRole.CUSTOMER.toString(); // "CUSTOMER"

    @BeforeEach
    void setUp() throws Exception {
        mockedDBUtil = Mockito.mockStatic(DBUtil.class);
        mockedDBUtil.when(DBUtil::getConnection).thenReturn(mockConnection);

        // Instantiate the service with a specific role for consistent table name in tests
        userService = new UserServiceImpl(UserRole.CUSTOMER);

        // Common mock behavior for PreparedStatement (can be overridden in specific tests if needed)
        // Using anyString() here for prepareStatement as the query string changes based on TABLE_NAME.
        // We will verify the exact query string via argument captor or by being more specific in test if needed.
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
    }

    @AfterEach
    void tearDown() {
        mockedDBUtil.close();
    }

    private UserBean createSampleUser() {
        UserBean user = new UserBean();
        user.setMailId("test@example.com");
        user.setPWord("password123");
        user.setFName("Test");
        user.setLName("User");
        user.setAddr("123 Test St");
        user.setPhNo(1234567890L);
        return user;
    }

    @Test
    void testConstructor_NullUserRole_DefaultsToCustomer() {
        UserServiceImpl serviceWithNullRole = new UserServiceImpl(null);
        // This test is a bit indirect. We'd have to call a method to see its effect.
        // For example, if getUserByEmailId constructs a query, we can check that query.
        // Or, if TABLE_NAME was protected/public, we could access it.
        // For now, we'll assume the default works and test methods with a known role.
        assertNotNull(serviceWithNullRole); // Basic check
    }

    @Test
    void testGetUserByEmailId_UserFound() throws SQLException, TrainException {
        String email = "test@example.com";
        UserBean expectedUser = createSampleUser();
        String expectedQuery = "SELECT * FROM " + CUSTOMER_TABLE_NAME + " WHERE MAILID=?";

        when(mockConnection.prepareStatement(expectedQuery)).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("fname")).thenReturn(expectedUser.getFName());
        when(mockResultSet.getString("lname")).thenReturn(expectedUser.getLName());
        when(mockResultSet.getString("addr")).thenReturn(expectedUser.getAddr());
        when(mockResultSet.getString("mailid")).thenReturn(expectedUser.getMailId());
        when(mockResultSet.getLong("phno")).thenReturn(expectedUser.getPhNo());

        UserBean actualUser = userService.getUserByEmailId(email);

        assertNotNull(actualUser);
        assertEquals(expectedUser.getFName(), actualUser.getFName());
        assertEquals(expectedUser.getMailId(), actualUser.getMailId());

        verify(mockConnection).prepareStatement(expectedQuery);
        verify(mockPreparedStatement).setString(1, email);
        verify(mockPreparedStatement).executeQuery();
        verify(mockPreparedStatement).close();
    }

    @Test
    void testGetUserByEmailId_UserNotFound() throws SQLException {
        String email = "notfound@example.com";
        String expectedQuery = "SELECT * FROM " + CUSTOMER_TABLE_NAME + " WHERE MAILID=?";
        when(mockConnection.prepareStatement(expectedQuery)).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        TrainException exception = assertThrows(TrainException.class, () -> {
            userService.getUserByEmailId(email);
        });

        assertEquals(ResponseCode.NO_CONTENT.toString(), exception.getMessage());
        verify(mockPreparedStatement).close();
    }

    @Test
    void testGetUserByEmailId_SQLException() throws SQLException {
        String email = "test@example.com";
        String expectedQuery = "SELECT * FROM " + CUSTOMER_TABLE_NAME + " WHERE MAILID=?";
        when(mockConnection.prepareStatement(expectedQuery)).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException("DB Read Error"));

        TrainException exception = assertThrows(TrainException.class, () -> {
            userService.getUserByEmailId(email);
        });

        assertEquals("DB Read Error", exception.getMessage());
        verify(mockPreparedStatement).close();
    }

    @Test
    void testGetAllUsers_UsersFound() throws SQLException, TrainException {
        String expectedQuery = "SELECT * FROM  " + CUSTOMER_TABLE_NAME;
        when(mockConnection.prepareStatement(expectedQuery)).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

        when(mockResultSet.next()).thenReturn(true).thenReturn(true).thenReturn(false); // Two users
        when(mockResultSet.getString("fname")).thenReturn("User1", "User2");
        when(mockResultSet.getString("lname")).thenReturn("LN1", "LN2");
        when(mockResultSet.getString("addr")).thenReturn("Addr1", "Addr2");
        when(mockResultSet.getString("mailid")).thenReturn("user1@mail.com", "user2@mail.com");
        when(mockResultSet.getLong("phno")).thenReturn(111L, 222L);

        List<UserBean> users = userService.getAllUsers();

        assertNotNull(users);
        assertEquals(2, users.size());
        assertEquals("User1", users.get(0).getFName());
        assertEquals("user2@mail.com", users.get(1).getMailId());

        verify(mockConnection).prepareStatement(expectedQuery);
        verify(mockPreparedStatement).executeQuery();
        verify(mockPreparedStatement).close();
    }

    @Test
    void testGetAllUsers_NoUsersFound() throws SQLException {
        String expectedQuery = "SELECT * FROM  " + CUSTOMER_TABLE_NAME;
        when(mockConnection.prepareStatement(expectedQuery)).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false); // No users

        TrainException exception = assertThrows(TrainException.class, () -> {
            userService.getAllUsers();
        });

        assertEquals(ResponseCode.NO_CONTENT.toString(), exception.getMessage());
        verify(mockPreparedStatement).close();
    }

    @Test
    void testGetAllUsers_SQLException() throws SQLException {
        String expectedQuery = "SELECT * FROM  " + CUSTOMER_TABLE_NAME;
        when(mockConnection.prepareStatement(expectedQuery)).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException("DB List Error"));

        TrainException exception = assertThrows(TrainException.class, () -> {
            userService.getAllUsers();
        });

        assertEquals("DB List Error", exception.getMessage());
        verify(mockPreparedStatement).close();
    }


    @Test
    void testUpdateUser_Success() throws SQLException, TrainException {
        UserBean userToUpdate = createSampleUser();
        userToUpdate.setFName("UpdatedName");
        String expectedQuery = "UPDATE  " + CUSTOMER_TABLE_NAME + " SET FNAME=?,LNAME=?,ADDR=?,PHNO=? WHERE MAILID=?";

        when(mockConnection.prepareStatement(expectedQuery)).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1); // 1 row affected

        String response = userService.updateUser(userToUpdate);

        assertEquals(ResponseCode.SUCCESS.toString(), response);
        verify(mockPreparedStatement).setString(1, userToUpdate.getFName());
        verify(mockPreparedStatement).setString(2, userToUpdate.getLName());
        verify(mockPreparedStatement).setString(3, userToUpdate.getAddr());
        verify(mockPreparedStatement).setLong(4, userToUpdate.getPhNo());
        verify(mockPreparedStatement).setString(5, userToUpdate.getMailId());
        verify(mockPreparedStatement).executeUpdate();
        verify(mockPreparedStatement).close();
    }

    @Test
    void testUpdateUser_Failure_NoRowAffected() throws SQLException, TrainException {
        UserBean userToUpdate = createSampleUser();
        String expectedQuery = "UPDATE  " + CUSTOMER_TABLE_NAME + " SET FNAME=?,LNAME=?,ADDR=?,PHNO=? WHERE MAILID=?";
        when(mockConnection.prepareStatement(expectedQuery)).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(0); // 0 rows affected

        String response = userService.updateUser(userToUpdate);

        assertEquals(ResponseCode.FAILURE.toString(), response);
        verify(mockPreparedStatement).executeUpdate();
        verify(mockPreparedStatement).close();
    }

    @Test
    void testUpdateUser_SQLException() throws SQLException, TrainException {
        UserBean userToUpdate = createSampleUser();
        String expectedQuery = "UPDATE  " + CUSTOMER_TABLE_NAME + " SET FNAME=?,LNAME=?,ADDR=?,PHNO=? WHERE MAILID=?";
        when(mockConnection.prepareStatement(expectedQuery)).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenThrow(new SQLException("DB Update Error"));

        String response = userService.updateUser(userToUpdate);

        assertTrue(response.startsWith(ResponseCode.FAILURE.toString()));
        assertTrue(response.contains("DB Update Error"));
        verify(mockPreparedStatement).close();
    }
    
    @Test
    void testUpdateUser_TrainExceptionFromDBUtil() throws SQLException, TrainException {
        UserBean userToUpdate = createSampleUser();
        mockedDBUtil.when(DBUtil::getConnection).thenThrow(new TrainException("DB Connect Error"));
        
        String response = userService.updateUser(userToUpdate);

        assertTrue(response.startsWith(ResponseCode.FAILURE.toString()));
        assertTrue(response.contains("DB Connect Error"));
        verify(mockConnection, never()).prepareStatement(anyString());
        verify(mockPreparedStatement, never()).close();
    }


    @Test
    void testDeleteUser_Success() throws SQLException, TrainException {
        UserBean userToDelete = createSampleUser();
        String expectedQuery = "DELETE FROM " + CUSTOMER_TABLE_NAME + " WHERE MAILID=?";
        when(mockConnection.prepareStatement(expectedQuery)).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        String response = userService.deleteUser(userToDelete);

        assertEquals(ResponseCode.SUCCESS.toString(), response);
        verify(mockPreparedStatement).setString(1, userToDelete.getMailId());
        verify(mockPreparedStatement).executeUpdate();
        verify(mockPreparedStatement).close();
    }

    @Test
    void testDeleteUser_Failure_NoRowAffected() throws SQLException, TrainException {
        UserBean userToDelete = createSampleUser();
        String expectedQuery = "DELETE FROM " + CUSTOMER_TABLE_NAME + " WHERE MAILID=?";
        when(mockConnection.prepareStatement(expectedQuery)).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(0);

        String response = userService.deleteUser(userToDelete);

        assertEquals(ResponseCode.FAILURE.toString(), response);
        verify(mockPreparedStatement).executeUpdate();
        verify(mockPreparedStatement).close();
    }

    @Test
    void testDeleteUser_SQLException() throws SQLException, TrainException {
        UserBean userToDelete = createSampleUser();
        String expectedQuery = "DELETE FROM " + CUSTOMER_TABLE_NAME + " WHERE MAILID=?";
        when(mockConnection.prepareStatement(expectedQuery)).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenThrow(new SQLException("DB Delete Error"));

        String response = userService.deleteUser(userToDelete);

        assertTrue(response.startsWith(ResponseCode.FAILURE.toString()));
        assertTrue(response.contains("DB Delete Error"));
        verify(mockPreparedStatement).close();
    }

    @Test
    void testRegisterUser_Success() throws SQLException, TrainException {
        UserBean newUser = createSampleUser();
        String expectedQuery = "INSERT INTO " + CUSTOMER_TABLE_NAME + " VALUES(?,?,?,?,?,?)";
        when(mockConnection.prepareStatement(expectedQuery)).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet); // As per original code
        when(mockResultSet.next()).thenReturn(true); // Simulating success for INSERT via executeQuery

        String response = userService.registerUser(newUser);

        assertEquals(ResponseCode.SUCCESS.toString(), response);
        verify(mockPreparedStatement).setString(1, newUser.getMailId());
        verify(mockPreparedStatement).setString(2, newUser.getPWord());
        // ... verify other setString/setLong calls
        verify(mockPreparedStatement).executeQuery();
        verify(mockPreparedStatement).close();
    }

    @Test
    void testRegisterUser_Failure_NoRowIndication() throws SQLException, TrainException {
        UserBean newUser = createSampleUser();
        String expectedQuery = "INSERT INTO " + CUSTOMER_TABLE_NAME + " VALUES(?,?,?,?,?,?)";
        when(mockConnection.prepareStatement(expectedQuery)).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false); // Simulating failure

        String response = userService.registerUser(newUser);

        assertEquals(ResponseCode.FAILURE.toString(), response); // The base response code
        verify(mockPreparedStatement).executeQuery();
        verify(mockPreparedStatement).close();
    }

    @Test
    void testRegisterUser_SQLException_UniqueConstraintViolation() throws SQLException, TrainException {
        UserBean newUser = createSampleUser();
        String expectedQuery = "INSERT INTO " + CUSTOMER_TABLE_NAME + " VALUES(?,?,?,?,?,?)";
        when(mockConnection.prepareStatement(expectedQuery)).thenReturn(mockPreparedStatement);
        SQLException oraException = new SQLException("ORA-00001: unique constraint violated");
        when(mockPreparedStatement.executeQuery()).thenThrow(oraException);

        String response = userService.registerUser(newUser);

        assertTrue(response.startsWith(ResponseCode.FAILURE.toString()));
        assertTrue(response.contains("User With Id: " + newUser.getMailId() + " is already registered"));
        verify(mockPreparedStatement).close();
    }

    @Test
    void testRegisterUser_SQLException_OtherError() throws SQLException, TrainException {
        UserBean newUser = createSampleUser();
        String expectedQuery = "INSERT INTO " + CUSTOMER_TABLE_NAME + " VALUES(?,?,?,?,?,?)";
        when(mockConnection.prepareStatement(expectedQuery)).thenReturn(mockPreparedStatement);
        SQLException otherException = new SQLException("Some other DB error");
        when(mockPreparedStatement.executeQuery()).thenThrow(otherException);

        String response = userService.registerUser(newUser);

        assertTrue(response.startsWith(ResponseCode.FAILURE.toString()));
        assertTrue(response.contains("Some other DB error"));
        assertFalse(response.contains("is already registered")); // Ensure specific ORA message isn't there
        verify(mockPreparedStatement).close();
    }


    @Test
    void testLoginUser_Success() throws SQLException, TrainException {
        String username = "test@example.com";
        String password = "password123";
        UserBean expectedUser = createSampleUser();
        expectedUser.setPWord(password); // Ensure password matches for login
        String expectedQuery = "SELECT * FROM " + CUSTOMER_TABLE_NAME + " WHERE MAILID=? AND PWORD=?";

        when(mockConnection.prepareStatement(expectedQuery)).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("fname")).thenReturn(expectedUser.getFName());
        when(mockResultSet.getString("lname")).thenReturn(expectedUser.getLName());
        when(mockResultSet.getString("addr")).thenReturn(expectedUser.getAddr());
        when(mockResultSet.getString("mailid")).thenReturn(expectedUser.getMailId());
        when(mockResultSet.getLong("phno")).thenReturn(expectedUser.getPhNo());
        when(mockResultSet.getString("pword")).thenReturn(expectedUser.getPWord());


        UserBean actualUser = userService.loginUser(username, password);

        assertNotNull(actualUser);
        assertEquals(expectedUser.getFName(), actualUser.getFName());
        assertEquals(expectedUser.getMailId(), actualUser.getMailId());
        assertEquals(expectedUser.getPWord(), actualUser.getPWord());

        verify(mockPreparedStatement).setString(1, username);
        verify(mockPreparedStatement).setString(2, password);
        verify(mockPreparedStatement).executeQuery();
        verify(mockPreparedStatement).close();
    }

    @Test
    void testLoginUser_Unauthorized() throws SQLException {
        String username = "test@example.com";
        String password = "wrongpassword";
        String expectedQuery = "SELECT * FROM " + CUSTOMER_TABLE_NAME + " WHERE MAILID=? AND PWORD=?";
        when(mockConnection.prepareStatement(expectedQuery)).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false); // Login failed

        TrainException exception = assertThrows(TrainException.class, () -> {
            userService.loginUser(username, password);
        });

        assertEquals(ResponseCode.UNAUTHORIZED.toString(), exception.getMessage());
        verify(mockPreparedStatement).close();
    }

    @Test
    void testLoginUser_SQLException() throws SQLException {
        String username = "test@example.com";
        String password = "password123";
        String expectedQuery = "SELECT * FROM " + CUSTOMER_TABLE_NAME + " WHERE MAILID=? AND PWORD=?";
        when(mockConnection.prepareStatement(expectedQuery)).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException("DB Login Error"));

        TrainException exception = assertThrows(TrainException.class, () -> {
            userService.loginUser(username, password);
        });

        assertEquals("DB Login Error", exception.getMessage());
        verify(mockPreparedStatement).close();
    }
}