package dev.group.cybershield.login.service;

import dev.group.cybershield.entity.Users;
import dev.group.cybershield.login.model.UserDTO;
import dev.group.cybershield.repository.UsersRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class UserServices {

    @Autowired
    private UsersRepo userRepo;

    public Boolean verifyUser(UserDTO reqBody) {
        Users user = userRepo.findByEmail(reqBody.getEmail());
        if (user != null) {
            return user.getPassword() != null && user.getPassword().equalsIgnoreCase(reqBody.getPassword());
        }
        return false;
    }
}
