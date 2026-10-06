package com.trading.userservice.service;

import com.trading.userservice.Repository.UserRepository;
import com.trading.userservice.dto.AuthResponse;
import com.trading.userservice.dto.LoginRequest;
import com.trading.userservice.dto.RegisterRequest;
import com.trading.userservice.dto.UserResponse;
import com.trading.userservice.entity.User;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService
{
    public final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String,Object> kafkaTemplate;
    private static final String USER_REGISTERED_TOPIC = "user_registered";

    // Register a new trader
    public AuthResponse register(RegisterRequest registerRequest)
    {
        log.info("Register request : {}", registerRequest.getEmail());
        if(userRepository.existsByEmail(registerRequest.getEmail()))
        {
            throw new RuntimeException("Email already exists"+registerRequest.getEmail());
        }
        User user = User.builder()
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .email(registerRequest.getEmail())
                .password(registerRequest.getPassword())
                .walletBalance(registerRequest.getInitialDeposit()!=null
                    ?registerRequest.getInitialDeposit()
                    : BigDecimal.valueOf(10000))
                .build();

        User savedUser = userRepository.save(user);
        log.info("Saved user : {}", savedUser.getId());

        // Publish user.registered event

        Map<String ,Object> userRegisteredEvent = new HashMap<>();
        userRegisteredEvent.put("user",savedUser.getId());
        userRegisteredEvent.put("email",savedUser.getEmail());
        userRegisteredEvent.put("firstName",savedUser.getFirstName());
        userRegisteredEvent.put("lastName",savedUser.getLastName());
        userRegisteredEvent.put("walletBalance",savedUser.getWalletBalance());
        kafkaTemplate.send(USER_REGISTERED_TOPIC, savedUser.getId(),userRegisteredEvent);

        String token = generatedAccessToken(savedUser.getId(), savedUser.getEmail());
        String refreshToken = generatedRefreshToken(savedUser.getId());
        return buildAuthResponse(savedUser,token,refreshToken);
    }

    // Login

    public AuthResponse login(LoginRequest loginRequest)
    {
        log.info("Login request : {}", loginRequest.getEmail());
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"+loginRequest.getEmail()));

        if(!passwordEncoder.matches(loginRequest.getPassword(),user.getPassword()))
        {
            throw new RuntimeException("Invalid credentials");
        }
        log.info("Logged in user : {}", user.getId());
        String token = generatedAccessToken(savedUser.getId(), user.getEmail());
        String refreshToken = generatedRefreshToken(user.getId());
        return buildAuthResponse(user,token,refreshToken);
    }

    /*
    * Get user by id
    * called by other user by feign
    * @Param userId
    * @return
    * */
    public UserRepository getUserById(String userId)
    {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"+userId));

        return mapToResponse(user);
    }

    public UserResponse addFunds(String userId, BigDecimal amount )
    {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"+userId));

        user.setWalletBalance(user.getWalletBalance().add(amount));
        User savedUser = userRepository.save(user);

        log.info("Funds added : {}", amount, userId);
        return mapToResponse(user);
    }

    public UserResponse deductFunds(String userId, BigDecimal amount)
    {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"+userId));

        if(user.getWalletBalance().compareTo(amount)<0)
        {
            throw  new RuntimeException("Insufficient Wallet Balance");
        }

        user.setWalletBalance(user.getWalletBalance().subtract(amount));
        User savedUser = userRepository.save(user);
        log.info("Funds deducted : {}", amount, userId);
        return mapToResponse(user);
    }

    public UserResponse creditFunds(String userId, BigDecimal amount)
    {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"+userId));


        user.setWalletBalance(user.getWalletBalance().add(amount));
        User savedUser = userRepository.save(user);
        log.info("Funds credited : {}", amount, userId);
        return mapToResponse(user);
    }


    private AuthResponse buildAuthResponse(User user, String token, String refreshToken)
    {
        AuthResponse authResponse = new AuthResponse();

    }

    private UserResponse mapToResponse(User user)
    {
        UserResponse userResponse = new UserResponse();
        userResponse.setId(user.getId());
        userResponse.setEmail(user.getEmail());
        userResponse.setFirstName(user.getFirstName());
        userResponse.setLastName(user.getLastName());
        userResponse.setWalletBalance(user.getWalletBalance());
        userResponse.setStatus(user.getStatus());
        userResponse.setCreatedAt(user.getCreatedAt());

        return userResponse;
    }

}

