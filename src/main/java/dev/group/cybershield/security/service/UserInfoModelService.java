package dev.group.cybershield.security.service;

import dev.group.cybershield.common.exception.BadRequestException;
import dev.group.cybershield.security.model.Role;
import dev.group.cybershield.security.model.UserInfoModel;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class UserInfoModelService {

    private List<UserInfoModel> allUsers;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    public void setAllUsers() {
        allUsers = new ArrayList<>(List.of(UserInfoModel.builder().id(1).email("admin@gmail.com").password(passwordEncoder.encode("ADMIN@123")).status("A").roles(Set.of(Role.STANDARD_USER)).build(),
                UserInfoModel.builder().id(2).email("customer@gmail.com").password(passwordEncoder.encode("CUSTOMER@123")).status("A").roles(Set.of(Role.STANDARD_USER)).build(),
                UserInfoModel.builder().id(3).email("vendor@gmail.com").password(passwordEncoder.encode("VENDOR@123")).status("A").roles(Set.of(Role.STANDARD_USER)).build()));
    }

    public UserInfoModel loadUserByUsername(String username) {
        List<UserInfoModel> filteredUserInfoModels = allUsers.stream().filter(userModel -> userModel.getEmail().equalsIgnoreCase(username)).toList();
        if (filteredUserInfoModels.isEmpty()) {
            throw new UsernameNotFoundException("User Not Found");
        }
        return (filteredUserInfoModels.size() == 1) ? filteredUserInfoModels.get(0) : null;
    }

    public UserInfoModel addUser(UserInfoModel userInfoModel) {
        try {
            UserInfoModel userInfoModelFound = this.loadUserByUsername(userInfoModel.getEmail());
            throw new BadRequestException("User already exists !");
        } catch (UsernameNotFoundException e) {
            this.allUsers.add(userInfoModel);
        }
        return userInfoModel;
    }

}
