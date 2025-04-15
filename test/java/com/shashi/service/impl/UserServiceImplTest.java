package test.java.com.shashi.service.impl;

import com.shashi.beans.TrainException;
import com.shashi.beans.UserBean;
import com.shashi.constant.ResponseCode;
import com.shashi.constant.UserRole;
import com.shashi.service.impl.UserServiceImpl;
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
class UserServiceImplTest {

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(UserRole.CUSTOMER);
    }

    @Test
    void testGetUserByEmailId_Success() throws SQLException, TrainException {
        // Arrange
        UserBean expectedUser = createUserBean();
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("fname")).thenReturn("John");
        when(resultSet.getString("lname")).thenReturn("Doe");
        when(resultSet.getString("addr")).thenReturn("123 Main St");
        when(resultSet.getString("mailid")).thenReturn("john@example.com");
        when(resultSet.getLong("phno")).thenReturn(1234567890L);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act
            UserBean actualUser = userService.getUserByEmailId("john@example.com");

            // Assert
            assertNotNull(actualUser);
            assertEquals(expectedUser.getFName(), actualUser.getFName());
            assertEquals(expectedUser.getLName(), actualUser.getLName());
            assertEquals(expectedUser.getAddr(), actualUser.getAddr());
            assertEquals(expectedUser.getMailId(), actualUser.getMailId());
            assertEquals(expectedUser.getPhNo(), actualUser.getPhNo());
            verify(preparedStatement).close();
        }
    }

    @Test
    void testGetUserByEmailId_NoContent() throws SQLException {
        // Arrange
        when(resultSet.next()).thenReturn(false);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act & Assert
            TrainException exception = assertThrows(TrainException.class,
                    () -> userService.getUserByEmailId("nonexistent@example.com"));
            assertEquals(ResponseCode.NO_CONTENT.getMessage(), exception.getErrorMessage());
            verify(preparedStatement).close();
        }
    }

    @Test
    void testGetUserByEmailId_SQLException() throws SQLException {
        // Arrange
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act & Assert
            TrainException exception = assertThrows(TrainException.class,
                    () -> userService.getUserByEmailId("john@example.com"));
            assertEquals("DB Error", exception.getErrorMessage());
        }
    }

    @Test
    void testGetAllUsers_Success() throws SQLException, TrainException {
        // Arrange
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getString("fname")).thenReturn("John", "Jane");
        when(resultSet.getString("lname")).thenReturn("Doe", "Smith");
        when(resultSet.getString("addr")).thenReturn("123 Main St", "456 Elm St");
        when(resultSet.getString("mailid")).thenReturn("john@example.com", "jane@example.com");
        when(resultSet.getLong("phno")).thenReturn(1234567890L, 9876543210L);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act
            List<UserBean> users = userService.getAllUsers();

            // Assert
            assertNotNull(users);
            assertEquals(2, users.size());
            assertEquals("John", users.get(0).getFName());
            assertEquals("Jane", users.get(1).getFName());
            verify(preparedStatement).close();
        }
    }

    @Test
    void testGetAllUsers_NoContent() throws SQLException {
        // Arrange
        when(resultSet.next()).thenReturn(false);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act & Assert
            TrainException exception = assertThrows(TrainException.class,
                    () -> userService.getAllUsers());
            assertEquals(ResponseCode.NO_CONTENT.getMessage(), exception.getErrorMessage());
            verify(preparedStatement).close();
        }
    }

    @Test
    void testGetAllUsers_SQLException() throws SQLException {
        // Arrange
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act & Assert
            TrainException exception = assertThrows(TrainException.class,
                    () -> userService.getAllUsers());
            assertEquals("DB Error", exception.getErrorMessage());
        }
    }

    @Test
    void testUpdateUser_Success() throws SQLException {
        // Arrange
        UserBean user = createUserBean();
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act
            String result = userService.updateUser(user);

            // Assert
            assertEquals(ResponseCode.SUCCESS.toString(), result);
            verify(preparedStatement).setString(1, user.getFName());
            verify(preparedStatement).setString(2, user.getLName());
            verify(preparedStatement).setString(3, user.getAddr());
            verify(preparedStatement).setLong(4, user.getPhNo());
            verify(preparedStatement).setString(5, user.getMailId());
            verify(preparedStatement).close();
        }
    }

    @Test
    void testUpdateUser_Failure() throws SQLException {
        // Arrange
        UserBean user = createUserBean();
        when(preparedStatement.executeUpdate()).thenReturn(0);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act
            String result = userService.updateUser(user);

            // Assert
            assertEquals(ResponseCode.FAILURE.toString(), result);
            verify(preparedStatement).close();
        }
    }

    @Test
    void testUpdateUser_SQLException() throws SQLException {
        // Arrange
        UserBean user = createUserBean();
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act
            String result = userService.updateUser(user);

            // Assert
            assertTrue(result.startsWith(ResponseCode.FAILURE.toString()));
            assertTrue(result.contains("DB Error"));
        }
    }

    @Test
    void testDeleteUser_Success() throws SQLException {
        // Arrange
        UserBean user = createUserBean();
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act
            String result = userService.deleteUser(user);

            // Assert
            assertEquals(ResponseCode.SUCCESS.toString(), result);
            verify(preparedStatement).setString(1, user.getMailId());
            verify(preparedStatement).close();
        }
    }

    @Test
    void testDeleteUser_Failure() throws SQLException {
        // Arrange
        UserBean user = createUserBean();
        when(preparedStatement.executeUpdate()).thenReturn(0);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act
            String result = userService.deleteUser(user);

            // Assert
            assertEquals(ResponseCode.FAILURE.toString(), result);
            verify(preparedStatement).close();
        }
    }

    @Test
    void testDeleteUser_SQLException() throws SQLException {
        // Arrange
        UserBean user = createUserBean();
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act
            String result = userService.deleteUser(user);

            // Assert
            assertTrue(result.startsWith(ResponseCode.FAILURE.toString()));
            assertTrue(result.contains("DB Error"));
        }
    }

    @Test
    void testRegisterUser_Success() throws SQLException {
        // Arrange
        UserBean user = createUserBean();
        when(resultSet.next()).thenReturn(true);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act
            String result = userService.registerUser(user);

            // Assert
            assertEquals(ResponseCode.SUCCESS.toString(), result);
            verify(preparedStatement).setString(1, user.getMailId());
            verify(preparedStatement).setString(2, user.getPWord());
            verify(preparedStatement).setString(3, user.getFName());
            verify(preparedStatement).setString(4, user.getLName());
            verify(preparedStatement).setString(5, user.getAddr());
            verify(preparedStatement).setLong(6, user.getPhNo());
            verify(preparedStatement).close();
        }
    }

    @Test
    void testRegisterUser_DuplicateKey() throws SQLException {
        // Arrange
        UserBean user = createUserBean();
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("ORA-00001: unique constraint violated"));
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act
            String result = userService.registerUser(user);

            // Assert
            assertTrue(result.startsWith(ResponseCode.FAILURE.toString()));
            assertTrue(result.contains("User With Id: john@example.com is already registered"));
        }
    }

    @Test
    void testRegisterUser_SQLException() throws SQLException {
        // Arrange
        UserBean user = createUserBean();
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act
            String result = userService.registerUser(user);

            // Assert
            assertTrue(result.startsWith(ResponseCode.FAILURE.toString()));
            assertTrue(result.contains("DB Error"));
        }
    }

    @Test
    void testLoginUser_Success() throws SQLException, TrainException {
        // Arrange
        UserBean expectedUser = createUserBean();
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("fname")).thenReturn("John");
        when(resultSet.getString("lname")).thenReturn("Doe");
        when(resultSet.getString("addr")).thenReturn("123 Main St");
        when(resultSet.getString("mailid")).thenReturn("john@example.com");
        when(resultSet.getLong("phno")).thenReturn(1234567890L);
        when(resultSet.getString("pword")).thenReturn("password");
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act
            UserBean actualUser = userService.loginUser("john@example.com", "password");

            // Assert
            assertNotNull(actualUser);
            assertEquals(expectedUser.getFName(), actualUser.getFName());
            assertEquals(expectedUser.getPWord(), actualUser.getPWord());
            verify(preparedStatement).close();
        }
    }

    @Test
    void testLoginUser_Unauthorized() throws SQLException {
        // Arrange
        when(resultSet.next()).thenReturn(false);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act & Assert
            TrainException exception = assertThrows(TrainException.class,
                    () -> userService.loginUser("john@example.com", "wrongpassword"));
            assertEquals(ResponseCode.UNAUTHORIZED.getMessage(), exception.getErrorMessage());
            verify(preparedStatement).close();
        }
    }

    @Test
    void testLoginUser_SQLException() throws SQLException {
        // Arrange
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("DB Error"));
        try (MockedStatic<DBUtil> dbUtil = mockStatic(DBUtil.class)) {
            dbUtil.when(DBUtil::getConnection).thenReturn(connection);

            // Act & Assert
            TrainException exception = assertThrows(TrainException.class,
                    () -> userService.loginUser("john@example.com", "password"));
            assertEquals("DB Error", exception.getErrorMessage());
        }
    }

    private UserBean createUserBean() {
        UserBean user = new UserBean();
        user.setFName("John");
        user.setLName("Doe");
        user.setAddr("123 Main St");
        user.setMailId("john@example.com");
        user.setPhNo(1234567890L);
        user.setPWord("password");
        return user;
    }
}