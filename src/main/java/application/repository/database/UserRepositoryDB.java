package application.repository.database;

import application.domain.Friend;
import application.domain.User;
import application.repository.UserRepository;
import application.utils.database.DataBase;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.sql.*;

public class UserRepositoryDB extends AbstractRepositoryDB<UUID, User> implements UserRepository {
    public UserRepositoryDB(DataBase dataBase) {
        super(dataBase, "users");
    }

    @Override
    public Optional<User> extractEntity(ResultSet resultSet) throws SQLException {
        UUID id = resultSet.getObject("user_id", UUID.class);
        String email = resultSet.getString("email");
        String firstName = resultSet.getString("first_name");
        String lastName = resultSet.getString("last_name");
        String password = resultSet.getString("password");
        LocalDate registerDate = resultSet.getDate("register_date").toLocalDate();
        LocalDate birthDate = resultSet.getDate("birth_date").toLocalDate();
        String biography = resultSet.getString("biography");
        return Optional.of(new User(id, email, firstName, lastName, password,
                registerDate, birthDate, biography));
    }

    public Optional<Friend> extractFriend(ResultSet resultSet) throws SQLException {
        UUID id = resultSet.getObject("user_id", UUID.class);
        String email = resultSet.getString("email");
        String firstName = resultSet.getString("first_name");
        String lastName = resultSet.getString("last_name");
        String password = resultSet.getString("password");
        LocalDate registerDate = resultSet.getDate("register_date").toLocalDate();
        LocalDate birthDate = resultSet.getDate("birth_date").toLocalDate();
        String biography = resultSet.getString("biography");
        LocalDateTime friendsFrom = null;
        if (resultSet.getTimestamp("friends_from") != null) {
            friendsFrom = resultSet.getTimestamp("friends_from").toLocalDateTime();
        }
        int commonFriends = resultSet.getInt("common_friends");
        return Optional.of(new Friend(id, email, firstName, lastName, password,
                registerDate, birthDate, biography, friendsFrom, commonFriends));
    }

    @Override
    protected PreparedStatement findStatement(Connection connection, UUID id) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(
        "SELECT * " +
            "FROM users " +
            "WHERE user_id = ?");

        preparedStatement.setObject(1, id, Types.OTHER);
        return preparedStatement;
    }

    @Override
    protected PreparedStatement saveStatement(Connection connection, User user) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(
        "INSERT INTO users(user_id, email, first_name, last_name, password, register_date, birth_date, biography) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
        preparedStatement.setObject(1, user.getID(), Types.OTHER);
        preparedStatement.setString(2, user.getMailAddress());
        preparedStatement.setString(3, user.getFirstName());
        preparedStatement.setString(4, user.getLastName());
        preparedStatement.setString(5, user.getPassword());
        preparedStatement.setDate(6, Date.valueOf(user.getRegisterDate()));
        preparedStatement.setDate(7, Date.valueOf(user.getBirthDate()));
        preparedStatement.setString(8, user.getBiography());
        return preparedStatement;
    }

    @Override
    protected PreparedStatement deleteStatement(Connection connection, UUID id) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(
        "DELETE FROM users " +
            "WHERE user_id = ?");
        preparedStatement.setObject(1, id, Types.OTHER);
        return preparedStatement;
    }

    @Override
    protected PreparedStatement updateStatement(Connection connection, User user) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(
        "UPDATE users " +
            "SET email = ?, first_name = ?, last_name = ?, password = ?, register_date = ?, birth_date = ?, biography = ? " +
            "WHERE user_id = ?");
        preparedStatement.setString(1, user.getMailAddress());
        preparedStatement.setString(2, user.getFirstName());
        preparedStatement.setString(3, user.getLastName());
        preparedStatement.setString(4, user.getPassword());
        preparedStatement.setDate(5, Date.valueOf(user.getRegisterDate()));
        preparedStatement.setDate(6, Date.valueOf(user.getBirthDate()));
        preparedStatement.setString(7, user.getBiography());
        preparedStatement.setObject(8, user.getID(), Types.OTHER);
        return preparedStatement;
    }

    @Override
    public Optional<User> findByMail(String mail) {
        if (mail == null) {
            throw new IllegalArgumentException("The mail address must not be null!");
        }
        try (Connection connection = getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT * " +
                            "FROM users " +
                            "WHERE email = ?");
            preparedStatement.setString(1, mail);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return extractEntity(resultSet);
            }

        } catch (SQLException ignored) {}
        return Optional.empty();
    }

    @Override
    public List<Friend> findFriendsOf(UUID fromUserID, UUID ofUserID) {
        List<Friend> friends = new ArrayList<>();
        try (Connection connection = getConnection()) {
            PreparedStatement preparedStatement;
            if (!fromUserID.equals(ofUserID)) {
                preparedStatement = connection.prepareStatement(
            "SELECT U.*, F3.friends_from AS friends_from, count(F3) AS common_friends " +
                "FROM friendships F1 " +
                "   INNER JOIN users U ON F1.first_user = U.user_id AND F1.second_user = ? " +
                "   LEFT JOIN friendships F2 ON F1.first_user = F2.first_user OR F1.first_user = F2.second_user " +
                "   LEFT JOIN friendships F3 ON (F1.first_user = F2.first_user AND F3.second_user = F2.second_user AND F3.first_user = ?) " +
                "       OR (F1.first_user = F2.second_user AND F3.second_user = F2.first_user AND F3.first_user = ?) " +
                "       OR (F1.first_user = F2.first_user AND F3.first_user = F2.second_user AND F3.second_user = ?) " +
                "       OR (F1.first_user = F2.second_user AND F3.first_user = F2.first_user AND F3.second_user = ?) " +
                "GROUP BY user_id, email, first_name, last_name, password, register_date, birth_date, biography, F3.friends_from " +
                "UNION " +
                "SELECT U.*, F3.friends_from AS friends_from, count(F3) AS common_friends " +
                "FROM friendships F1 " +
                "   INNER JOIN users U ON F1.second_user = U.user_id AND F1.first_user = ? " +
                "   LEFT JOIN friendships F2 ON F1.second_user = F2.first_user OR F1.second_user = F2.second_user " +
                "   LEFT JOIN friendships F3 ON (F1.first_user = F2.first_user AND F3.second_user = F2.second_user AND F3.first_user = ?) " +
                "       OR (F1.first_user = F2.second_user AND F3.second_user = F2.first_user AND F3.first_user = ?) " +
                "       OR (F1.first_user = F2.first_user AND F3.first_user = F2.second_user AND F3.second_user = ?) " +
                "       OR (F1.first_user = F2.second_user AND F3.first_user = F2.first_user AND F3.second_user = ?) " +
                "GROUP BY user_id, email, first_name, last_name, password, register_date, birth_date, biography, F3.friends_from " +
                "ORDER BY first_name, last_name");
            }
            else {
                preparedStatement = connection.prepareStatement(
            "SELECT U.*, F1.friends_from AS friends_from, count(F3) AS common_friends " +
                "FROM friendships F1 " +
                "   INNER JOIN users U ON F1.first_user = U.user_id AND F1.second_user = ? " +
                "   LEFT JOIN friendships F2 ON F1.first_user = F2.first_user OR F1.first_user = F2.second_user " +
                "   LEFT JOIN friendships F3 ON (F1.first_user = F2.first_user AND F3.second_user = F2.second_user AND F3.first_user = ?) " +
                "       OR (F1.first_user = F2.second_user AND F3.second_user = F2.first_user AND F3.first_user = ?) " +
                "       OR (F1.first_user = F2.first_user AND F3.first_user = F2.second_user AND F3.second_user = ?) " +
                "       OR (F1.first_user = F2.second_user AND F3.first_user = F2.first_user AND F3.second_user = ?) " +
                "GROUP BY user_id, email, first_name, last_name, password, register_date, birth_date, biography, F1.friends_from " +
                "UNION " +
                "SELECT U.*, F1.friends_from AS friends_from, count(F3) AS common_friends " +
                "FROM friendships F1 " +
                "   INNER JOIN users U ON F1.second_user = U.user_id AND F1.first_user = ? " +
                "   LEFT JOIN friendships F2 ON F1.second_user = F2.first_user OR F1.second_user = F2.second_user " +
                "   LEFT JOIN friendships F3 ON (F1.first_user = F2.first_user AND F3.second_user = F2.second_user AND F3.first_user = ?) " +
                "       OR (F1.first_user = F2.second_user AND F3.second_user = F2.first_user AND F3.first_user = ?) " +
                "       OR (F1.first_user = F2.first_user AND F3.first_user = F2.second_user AND F3.second_user = ?) " +
                "       OR (F1.first_user = F2.second_user AND F3.first_user = F2.first_user AND F3.second_user = ?) " +
                "GROUP BY user_id, email, first_name, last_name, password, register_date, birth_date, biography, F1.friends_from " +
                "ORDER BY first_name, last_name");
            }

            preparedStatement.setObject(1, ofUserID, Types.OTHER);
            preparedStatement.setObject(2, fromUserID, Types.OTHER);
            preparedStatement.setObject(3, fromUserID, Types.OTHER);
            preparedStatement.setObject(4, fromUserID, Types.OTHER);
            preparedStatement.setObject(5, fromUserID, Types.OTHER);
            preparedStatement.setObject(6, ofUserID, Types.OTHER);
            preparedStatement.setObject(7, fromUserID, Types.OTHER);
            preparedStatement.setObject(8, fromUserID, Types.OTHER);
            preparedStatement.setObject(9, fromUserID, Types.OTHER);
            preparedStatement.setObject(10, fromUserID, Types.OTHER);



            System.out.println(preparedStatement);

            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                extractFriend(resultSet).ifPresent(friends::add);
            }
        }
        catch (SQLException ignored) {}
        return friends;
    }

    @Override
    public List<Friend> findByName(UUID fromUserID, String match) {
        List<Friend> friends = new ArrayList<>();
        try (Connection connection = getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(
            "SELECT U.*, F1.friends_from AS friends_from, count(F3) AS common_friends " +
                "FROM users U " +
                "   LEFT JOIN friendships F1 ON F1.first_user = user_id AND F1.second_user = ?" +
                "   LEFT JOIN friendships F2 ON F1.first_user = F2.first_user OR F1.first_user = F2.second_user " +
                "   LEFT JOIN friendships F3 ON (F1.first_user = F2.first_user AND F3.second_user = F2.second_user AND F3.first_user = ?) " +
                "       OR (F1.first_user = F2.second_user AND F3.second_user = F2.first_user AND F3.first_user = ?) " +
                "       OR (F1.first_user = F2.first_user AND F3.first_user = F2.second_user AND F3.second_user = ?) " +
                "       OR (F1.first_user = F2.second_user AND F3.first_user = F2.first_user AND F3.second_user = ?) " +
                "WHERE (CONCAT(first_name, ' ', last_name) ILIKE ? OR CONCAT(last_name, ' ', first_name) ILIKE ?) " +
                "   AND user_id <= ? " +
                "GROUP BY user_id, email, first_name, last_name, password, register_date, birth_date, biography, F1.friends_from " +
                "UNION " +
                "SELECT U.*, F1.friends_from AS friends_from, count(F3) AS common_friends " +
                "FROM users U " +
                "   LEFT JOIN friendships F1 ON F1.second_user = user_id AND F1.first_user = ? " +
                "   LEFT JOIN friendships F2 ON F1.second_user = F2.first_user OR F1.second_user = F2.second_user " +
                "   LEFT JOIN friendships F3 ON (F1.first_user = F2.first_user AND F3.second_user = F2.second_user AND F3.first_user = ?) " +
                "       OR (F1.first_user = F2.second_user AND F3.second_user = F2.first_user AND F3.first_user = ?) " +
                "       OR (F1.first_user = F2.first_user AND F3.first_user = F2.second_user AND F3.second_user = ?) " +
                "       OR (F1.first_user = F2.second_user AND F3.first_user = F2.first_user AND F3.second_user = ?) " +
                "WHERE (CONCAT(first_name, ' ', last_name) ILIKE ? OR CONCAT(last_name, ' ', first_name) ILIKE ?) " +
                "   AND user_id >= ? " +
                "GROUP BY user_id, email, first_name, last_name, password, register_date, birth_date, biography , F1.friends_from " +
                "ORDER BY common_friends DESC, first_name, last_name " +
                "LIMIT 10");
            preparedStatement.setObject(1, fromUserID, Types.OTHER);
            preparedStatement.setObject(2, fromUserID, Types.OTHER);
            preparedStatement.setObject(3, fromUserID, Types.OTHER);
            preparedStatement.setObject(4, fromUserID, Types.OTHER);
            preparedStatement.setObject(5, fromUserID, Types.OTHER);
            preparedStatement.setString(6, "%" + match + "%");
            preparedStatement.setString(7, "%" + match + "%");
            preparedStatement.setObject(8, fromUserID, Types.OTHER);

            preparedStatement.setObject(9, fromUserID, Types.OTHER);
            preparedStatement.setObject(10, fromUserID, Types.OTHER);
            preparedStatement.setObject(11, fromUserID, Types.OTHER);
            preparedStatement.setObject(12, fromUserID, Types.OTHER);
            preparedStatement.setObject(13, fromUserID, Types.OTHER);
            preparedStatement.setString(14, "%" + match + "%");
            preparedStatement.setString(15, "%" + match + "%");
            preparedStatement.setObject(16, fromUserID, Types.OTHER);

            System.out.println(preparedStatement);

            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                extractFriend(resultSet).ifPresent(friends::add);
            }
        } catch (SQLException ignored) {}
        return friends;
    }

    @Override
    public List<Friend> findCommonFriends(UUID fromUserID, UUID withUserID) {
        List<Friend> friends = new ArrayList<>();
        try (Connection connection = getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(
            "SELECT U.*, F1.friends_from AS friends_from, count(F3) AS common_friends " +
                 "FROM users U " +
                 "    INNER JOIN friendships F ON (F.first_user = user_id AND F.second_user = ?) " +
                 "      OR (F.second_user = user_id AND F.first_user = ?) " +
                 "    INNER JOIN friendships F1 ON F1.first_user = user_id AND F1.second_user = ? " +
                 "    INNER JOIN friendships F2 ON F1.first_user = F2.first_user OR F1.first_user = F2.second_user " +
                 "    INNER JOIN friendships F3 ON (F1.first_user = F2.first_user AND F3.second_user = F2.second_user AND F3.first_user = ?) " +
                 "      OR (F1.first_user = F2.second_user AND F3.second_user = F2.first_user AND F3.first_user = ?) " +
                 "      OR (F1.first_user = F2.first_user AND F3.first_user = F2.second_user AND F3.second_user = ?) " +
                 "      OR (F1.first_user = F2.second_user AND F3.first_user = F2.first_user AND F3.second_user = ?) " +
                 "GROUP BY user_id, email, first_name, last_name, password, register_date, birth_date, biography , F1.friends_from UNION " +
                 "SELECT U.*, F1.friends_from AS friends_from, count(F3.first_user) AS common_friends " +
                 "FROM users U " +
                 "    INNER JOIN friendships F ON (F.first_user = user_id AND F.second_user = ?) " +
                 "      OR (F.second_user = user_id AND F.first_user = ?) " +
                 "    INNER JOIN friendships F1 ON F1.second_user = user_id AND F1.first_user = ? " +
                 "    INNER JOIN friendships F2 ON F1.second_user = F2.first_user OR F1.second_user = F2.second_user " +
                 "    INNER JOIN friendships F3 ON (F1.first_user = F2.first_user AND F3.second_user = F2.second_user AND F3.first_user = ?) " +
                 "      OR (F1.first_user = F2.second_user AND F3.second_user = F2.first_user AND F3.first_user = ?) " +
                 "      OR (F1.first_user = F2.first_user AND F3.first_user = F2.second_user AND F3.second_user = ?) " +
                 "      OR (F1.first_user = F2.second_user AND F3.first_user = F2.first_user AND F3.second_user = ?) " +
                 "GROUP BY user_id, email, first_name, last_name, password, register_date, birth_date, biography , F1.friends_from");

            preparedStatement.setObject(1, withUserID, Types.OTHER);
            preparedStatement.setObject(2, withUserID, Types.OTHER);
            preparedStatement.setObject(3, fromUserID, Types.OTHER);
            preparedStatement.setObject(4, fromUserID, Types.OTHER);
            preparedStatement.setObject(5, fromUserID, Types.OTHER);
            preparedStatement.setObject(6, fromUserID, Types.OTHER);
            preparedStatement.setObject(7, fromUserID, Types.OTHER);

            preparedStatement.setObject(8, withUserID, Types.OTHER);
            preparedStatement.setObject(9, withUserID, Types.OTHER);
            preparedStatement.setObject(10, fromUserID, Types.OTHER);
            preparedStatement.setObject(11, fromUserID, Types.OTHER);
            preparedStatement.setObject(12, fromUserID, Types.OTHER);
            preparedStatement.setObject(13, fromUserID, Types.OTHER);
            preparedStatement.setObject(14, fromUserID, Types.OTHER);

            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                extractFriend(resultSet).ifPresent(friends::add);
            }
        } catch (SQLException ignored) {}
        return friends;
    }

    @Override
    public Optional<Friend> findFriend(UUID fromUserID, UUID friendID) {
        try (Connection connection = getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(
            "SELECT U.*, F1.friends_from AS friends_from, count(F3) AS common_friends " +
                "FROM users U " +
                "    LEFT JOIN friendships F1 ON F1.first_user = user_id " +
                "    LEFT JOIN friendships F2 ON F1.first_user = F2.first_user OR F1.first_user = F2.second_user " +
                "    LEFT JOIN friendships F3 ON (F1.first_user = F2.first_user AND F3.second_user = F2.second_user AND F3.first_user = ?) " +
                "      OR (F1.first_user = F2.second_user AND F3.second_user = F2.first_user AND F3.first_user = ?) " +
                "      OR (F1.first_user = F2.first_user AND F3.first_user = F2.second_user AND F3.second_user = ?) " +
                "      OR (F1.first_user = F2.second_user AND F3.first_user = F2.first_user AND F3.second_user = ?) " +
                "WHERE user_id = ? AND user_id <= ? " +
                "GROUP BY user_id, email, first_name, last_name, password, register_date, birth_date, biography , F1.friends_from UNION " +
                "SELECT U.*, F1.friends_from AS friends_from, count(F3.first_user) AS common_friends " +
                "FROM users U " +
                "    LEFT JOIN friendships F1 ON F1.second_user = user_id" +
                "    LEFT JOIN friendships F2 ON F1.second_user = F2.first_user OR F1.second_user = F2.second_user " +
                "    LEFT JOIN friendships F3 ON (F1.first_user = F2.first_user AND F3.second_user = F2.second_user AND F3.first_user = ?) " +
                "      OR (F1.first_user = F2.second_user AND F3.second_user = F2.first_user AND F3.first_user = ?) " +
                "      OR (F1.first_user = F2.first_user AND F3.first_user = F2.second_user AND F3.second_user = ?) " +
                "      OR (F1.first_user = F2.second_user AND F3.first_user = F2.first_user AND F3.second_user = ?) " +
                "WHERE user_id = ? AND user_id >= ? " +
                "GROUP BY user_id, email, first_name, last_name, password, register_date, birth_date, biography , F1.friends_from");

            preparedStatement.setObject(1, fromUserID, Types.OTHER);
            preparedStatement.setObject(2, fromUserID, Types.OTHER);
            preparedStatement.setObject(3, fromUserID, Types.OTHER);
            preparedStatement.setObject(4, fromUserID, Types.OTHER);
            preparedStatement.setObject(5, friendID, Types.OTHER);
            preparedStatement.setObject(6, fromUserID, Types.OTHER);


            preparedStatement.setObject(7, fromUserID, Types.OTHER);
            preparedStatement.setObject(8, fromUserID, Types.OTHER);
            preparedStatement.setObject(9, fromUserID, Types.OTHER);
            preparedStatement.setObject(10, fromUserID, Types.OTHER);
            preparedStatement.setObject(11, friendID, Types.OTHER);
            preparedStatement.setObject(12, fromUserID, Types.OTHER);

            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return extractFriend(resultSet);
            }
        } catch (SQLException ignored) {}

        return Optional.empty();
    }
}