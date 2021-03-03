package org.geektimes.projects.user.repository;

import org.geektimes.projects.user.domain.User;
import org.geektimes.projects.user.sql.DBConnectionManager;
import org.geektimes.projects.user.sql.JdbcTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseUserRepository implements UserRepository {

    private static final Logger logger = Logger.getLogger(DatabaseUserRepository.class.getName());

    private final JdbcTemplate jdbcTemplate = new JdbcTemplate(new DBConnectionManager());

    /**
     * 通用处理方式
     */
    private static final Consumer<Throwable> COMMON_EXCEPTION_HANDLER = e -> logger.log(Level.SEVERE, e.getMessage());

    public static final String INSERT_USER_DML_SQL =
            "INSERT INTO users(name,password,email,phoneNumber) VALUES " +
                    "(?,?,?,?)";

    public static final String QUERY_ALL_USERS_DML_SQL = "SELECT id,name,password,email,phoneNumber FROM users";

    @Override
    public boolean save(User user) {
        return jdbcTemplate.executeInsert("users", user,
                result -> result, COMMON_EXCEPTION_HANDLER);
    }

    @Override
    public boolean deleteById(Long userId) {
        return false;
    }

    @Override
    public boolean update(User user) {
        return false;
    }

    @Override
    public User getById(Long userId) {
        return null;
    }

    @Override
    public User getByNameAndPassword(String userName, String password) {
        return jdbcTemplate.executeQuery("SELECT id,name,password,email,phoneNumber FROM users WHERE name=? and password=?",
                resultSet -> {
                    User user;
                    while (resultSet.next()) {
                        user = jdbcTemplate.fieldMapping(User.class, resultSet);
                        // 返回前释放数据库连接
                        jdbcTemplate.releaseConnection();
                        // 目前数据库没有做名称和密码唯一性，因此resultSet可能存在多条数据，返回第一条
                        return user;
                    }
                    return null;
                }, COMMON_EXCEPTION_HANDLER, userName, password);
    }

    @Override
    public Collection<User> getAll() {
        return jdbcTemplate.executeQuery(QUERY_ALL_USERS_DML_SQL, resultSet -> {
            List<User> users = new ArrayList<>();
            while (resultSet.next()) { // 如果存在并且游标滚动 // SQLException
                User user = jdbcTemplate.fieldMapping(User.class, resultSet);
                users.add(user);
            }
            return users;
        }, COMMON_EXCEPTION_HANDLER);
    }

    @Override
    public void initTable() {
        jdbcTemplate.initTable();
    }
}
