package org.geektimes.projects.user.service.impl;

import org.geektimes.projects.user.domain.User;
import org.geektimes.projects.user.repository.DatabaseUserRepository;
import org.geektimes.projects.user.repository.UserRepository;
import org.geektimes.projects.user.service.UserService;
import org.geektimes.projects.user.sql.DBConnectionManager;

import java.util.Collection;

/**
 * @author tanheyuan
 * @version 1.0
 * @since 2021/3/1
 */
public class UserServiceImpl implements UserService {

    private UserRepository userRepository = new DatabaseUserRepository(new DBConnectionManager());

    @Override
    public boolean register(User user) {
        return userRepository.save(user);
    }

    @Override
    public Collection<User> queryAllUser() {
        return userRepository.getAll();
    }

    @Override
    public boolean deregister(User user) {
        return false;
    }

    @Override
    public boolean update(User user) {
        return false;
    }

    @Override
    public User queryUserById(Long id) {
        return null;
    }

    @Override
    public User queryUserByNameAndPassword(String name, String password) {
        return null;
    }
}
