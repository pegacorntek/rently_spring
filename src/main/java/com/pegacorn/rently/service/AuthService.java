package com.pegacorn.rently.service;

import com.pegacorn.rently.constant.MessageConstant;
import com.pegacorn.rently.dto.auth.*;
import com.pegacorn.rently.entity.OtpVerification;
import com.pegacorn.rently.entity.User;
import com.pegacorn.rently.entity.UserRole;
import com.pegacorn.rently.exception.ApiException;
import com.pegacorn.rently.repository.OtpVerificationRepository;
import com.pegacorn.rently.repository.UserRepository;
import com.pegacorn.rently.repository.UserRoleRepository;
import com.pegacorn.rently.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int OTP_COOLDOWN_SECONDS = 120;

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final SmsService smsService;
    private final ActivityLogService activityLogService;
    private final TaskService taskService;

    @Transactional
    public void requestOtp(OtpRequest request) {
        String phone = normalizePhone(request.phone());
        if (request.type() == OtpVerification.OtpType.REGISTER) {
            if (userRepository.existsByPhone(phone)) {
                throw ApiException.conflict(MessageConstant.PHONE_ALREADY_EXISTS);
            }
        } else if (request.type() == OtpVerification.OtpType.RESET_PASSWORD) {
            if (!userRepository.existsByPhone(phone)) {
                throw ApiException.notFound(MessageConstant.PHONE_NOT_FOUND);
            }
        } else if (request.type() == OtpVerification.OtpType.TENANT_VERIFICATION) {
            if (!userRepository.existsByPhone(phone)) {
                throw ApiException.notFound(MessageConstant.USER_NOT_FOUND);
            }
        }

        // Rate limiting: check if OTP was sent recently
        checkOtpRateLimit(phone, request.type());

        // Invalidate existing OTPs for this phone and type
        otpVerificationRepository.deleteByPhoneAndType(phone, request.type());

        String otpCode = generateOtp();

        OtpVerification otp = OtpVerification.builder()
                .id(UUID.randomUUID().toString())
                .phone(phone)
                .otpCode(otpCode)
                .type(request.type())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .verified(false)
                .createdAt(LocalDateTime.now())
                .build();

        otpVerificationRepository.save(otp);

        // Send OTP via SMS
        smsService.sendOtp(phone, otpCode);
    }

    @Transactional
    public void verifyOtp(VerifyOtpRequest request) {
        String phone = normalizePhone(request.phone());
        // Debug: Log current time
        LocalDateTime now = LocalDateTime.now();
        System.out.println("Verifying OTP for " + phone + " at " + now);

        // Find unverified OTP
        OtpVerification.OtpType type = OtpVerification.OtpType.REGISTER;
        // Basic verify assumes REGISTER unless context implies otherwise, but here we
        // can stick to REGISTER default
        // or simplistic overloading. For clarity, let's keep this for REGISTER/generic.

        OtpVerification otp = otpVerificationRepository
                .findLatestUnverifiedOtp(phone, type, now)
                .orElseThrow(() -> {
                    System.out.println("No valid unverified OTP found for " + phone);
                    return ApiException.badRequest(MessageConstant.OTP_INVALID_OR_EXPIRED);
                });

        if (!otp.getOtpCode().equals(request.otp())) {
            throw ApiException.badRequest(MessageConstant.OTP_INVALID);
        }

        otpVerificationRepository.markAsVerified(otp.getId());
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        String phone = normalizePhone(request.phone());
        if (!request.password().equals(request.retypePassword())) {
            throw ApiException.badRequest(MessageConstant.PASSWORD_MISMATCH);
        }

        if (userRepository.existsByPhone(phone)) {
            throw ApiException.conflict(MessageConstant.PHONE_ALREADY_EXISTS);
        }

        // Verify OTP was validated (find verified OTP)
        OtpVerification otp = otpVerificationRepository
                .findLatestVerifiedOtp(phone, OtpVerification.OtpType.REGISTER, LocalDateTime.now())
                .orElseThrow(() -> ApiException.badRequest(MessageConstant.OTP_REQUIRED_FIRST));

        if (!otp.getOtpCode().equals(request.otp())) {
            throw ApiException.badRequest(MessageConstant.OTP_INVALID);
        }

        String userId = UUID.randomUUID().toString();
        User user = User.builder()
                .id(userId)
                .phone(phone)
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName() != null ? request.fullName() : phone)
                .status(User.UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        // Assign LANDLORD role by default
        UserRole role = UserRole.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .role(User.Role.LANDLORD)
                .createdAt(LocalDateTime.now())
                .build();
        userRoleRepository.save(role);

        // Clean up all OTPs for this phone number
        otpVerificationRepository.deleteByPhone(phone);

        user.setRoles(List.of(User.Role.LANDLORD));

        String token = jwtService.generateToken(userId, List.of("LANDLORD"));
        String refreshToken = jwtService.generateRefreshToken(userId);

        return new LoginResponse(UserDto.fromEntity(user), token, refreshToken);
    }

    public LoginResponse login(LoginRequest request) {
        String phone = normalizePhone(request.phone());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(phone, request.password()));
        } catch (LockedException | DisabledException e) {
            throw ApiException.forbidden(MessageConstant.ACCOUNT_LOCKED);
        } catch (Exception e) {
            throw ApiException.unauthorized(MessageConstant.CREDENTIALS_INVALID);
        }

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> ApiException.unauthorized(MessageConstant.CREDENTIALS_INVALID));

        List<User.Role> roles = userRoleRepository.findRolesByUserId(user.getId());
        user.setRoles(roles);

        List<String> roleNames = roles.stream().map(Enum::name).toList();
        String token = jwtService.generateToken(user.getId(), roleNames);
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        // Log user login
        activityLogService.logUserLogin(user.getId(), user.getPhone());

        return new LoginResponse(UserDto.fromEntity(user), token, refreshToken);
    }

    public LoginResponse refreshToken(String refreshToken) {
        if (jwtService.isTokenExpired(refreshToken)) {
            throw ApiException.unauthorized(MessageConstant.REFRESH_TOKEN_EXPIRED);
        }

        String userId = jwtService.extractUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized(MessageConstant.USER_NOT_FOUND));

        if (user.getStatus() == User.UserStatus.LOCKED) {
            throw ApiException.forbidden(MessageConstant.ACCOUNT_LOCKED);
        }

        List<User.Role> roles = userRoleRepository.findRolesByUserId(user.getId());
        user.setRoles(roles);

        List<String> roleNames = roles.stream().map(Enum::name).toList();
        String newToken = jwtService.generateToken(user.getId(), roleNames);
        String newRefreshToken = jwtService.generateRefreshToken(user.getId());

        return new LoginResponse(UserDto.fromEntity(user), newToken, newRefreshToken);
    }

    @Transactional
    public void forgotPasswordOtp(String rawPhone) {
        String phone = normalizePhone(rawPhone);
        if (!userRepository.existsByPhone(phone)) {
            throw ApiException.notFound(MessageConstant.PHONE_NOT_FOUND);
        }

        // Rate limiting: check if OTP was sent recently
        checkOtpRateLimit(phone, OtpVerification.OtpType.RESET_PASSWORD);

        // Invalidate existing OTPs for this phone
        otpVerificationRepository.deleteByPhoneAndType(phone, OtpVerification.OtpType.RESET_PASSWORD);

        String otpCode = generateOtp();

        OtpVerification otp = OtpVerification.builder()
                .id(UUID.randomUUID().toString())
                .phone(phone)
                .otpCode(otpCode)
                .type(OtpVerification.OtpType.RESET_PASSWORD)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .verified(false)
                .createdAt(LocalDateTime.now())
                .build();

        otpVerificationRepository.save(otp);

        // Send OTP via SMS
        smsService.sendOtp(phone, otpCode);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String phone = normalizePhone(request.phone());
        if (!request.newPassword().equals(request.retypePassword())) {
            throw ApiException.badRequest(MessageConstant.PASSWORD_MISMATCH);
        }

        // Find unverified OTP (reset password verifies and uses in one step)
        OtpVerification otp = otpVerificationRepository
                .findLatestUnverifiedOtp(phone, OtpVerification.OtpType.RESET_PASSWORD, LocalDateTime.now())
                .orElseThrow(() -> ApiException.badRequest(MessageConstant.OTP_INVALID_OR_EXPIRED));

        if (!otp.getOtpCode().equals(request.otp())) {
            throw ApiException.badRequest(MessageConstant.OTP_INVALID);
        }

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.USER_NOT_FOUND));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Mark OTP as used
        otpVerificationRepository.markAsVerified(otp.getId());
    }

    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request) {
        if (!request.newPassword().equals(request.retypePassword())) {
            throw ApiException.badRequest(MessageConstant.RETYPE_PASSWORD_MISMATCH);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest(MessageConstant.CURRENT_PASSWORD_INCORRECT);
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    public boolean checkUserExistence(String rawPhone) {
        String phone = normalizePhone(rawPhone);
        return userRepository.existsByPhone(phone);
    }

    @Transactional
    public UserDto verifyTenantAccessOtp(VerifyOtpRequest request) {
        String phone = normalizePhone(request.phone());
        OtpVerification otp = otpVerificationRepository
                .findLatestUnverifiedOtp(phone, OtpVerification.OtpType.TENANT_VERIFICATION,
                        LocalDateTime.now())
                .orElseThrow(() -> ApiException.badRequest(MessageConstant.OTP_INVALID_OR_EXPIRED));

        if (!otp.getOtpCode().equals(request.otp())) {
            throw ApiException.badRequest(MessageConstant.OTP_INVALID);
        }

        otpVerificationRepository.markAsVerified(otp.getId());

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.USER_NOT_FOUND));

        user.setRoles(userRoleRepository.findRolesByUserId(user.getId()));
        return UserDto.fromEntity(user);
    }

    public UserDto getMe(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.USER_NOT_FOUND));

        user.setRoles(userRoleRepository.findRolesByUserId(userId));
        return UserDto.fromEntity(user);
    }

    @Transactional
    public UserDto updateProfile(String userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.USER_NOT_FOUND));

        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName());
        }
        if (request.email() != null) {
            user.setEmail(request.email().isBlank() ? null : request.email());
        }
        if (request.idNumber() != null) {
            String newIdNumber = request.idNumber().isBlank() ? null : request.idNumber();
            // Check if ID number is used by another user
            if (newIdNumber != null && userRepository.existsByIdNumberAndIdNot(newIdNumber, userId)) {
                throw ApiException.conflict(MessageConstant.ID_NUMBER_CONFLICT);
            }
            user.setIdNumber(newIdNumber);
        }
        if (request.idIssueDate() != null) {
            user.setIdIssueDate(request.idIssueDate());
        }
        if (request.idIssuePlace() != null) {
            user.setIdIssuePlace(request.idIssuePlace().isBlank() ? null : request.idIssuePlace());
        }
        if (request.gender() != null) {
            user.setGender(request.gender().isBlank() ? null : User.Gender.valueOf(request.gender()));
        }
        if (request.dateOfBirth() != null) {
            user.setDateOfBirth(request.dateOfBirth());
        }
        if (request.placeOfOrigin() != null) {
            user.setPlaceOfOrigin(request.placeOfOrigin().isBlank() ? null : request.placeOfOrigin());
        }
        if (request.placeOfResidence() != null) {
            user.setPlaceOfResidence(request.placeOfResidence().isBlank() ? null : request.placeOfResidence());
        }

        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        user.setRoles(userRoleRepository.findRolesByUserId(userId));
        return UserDto.fromEntity(user);
    }

    @Transactional
    public UserDto updateBankSettings(String userId, UpdateBankSettingsRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.USER_NOT_FOUND));

        if (request.bankName() != null) {
            user.setBankName(request.bankName().isBlank() ? null : request.bankName());
        }
        if (request.bankCode() != null) {
            user.setBankCode(request.bankCode().isBlank() ? null : request.bankCode());
        }
        if (request.bankAccountNumber() != null) {
            user.setBankAccountNumber(request.bankAccountNumber().isBlank() ? null : request.bankAccountNumber());
        }
        if (request.bankAccountHolder() != null) {
            user.setBankAccountHolder(request.bankAccountHolder().isBlank() ? null : request.bankAccountHolder());
        }

        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        user.setRoles(userRoleRepository.findRolesByUserId(userId));
        return UserDto.fromEntity(user);
    }

    private String generateOtp() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }

    private void checkOtpRateLimit(String phone, OtpVerification.OtpType type) {
        otpVerificationRepository.findLatestByPhoneAndType(phone, type)
                .ifPresent(lastOtp -> {
                    LocalDateTime cooldownEnd = lastOtp.getCreatedAt().plusSeconds(OTP_COOLDOWN_SECONDS);
                    if (LocalDateTime.now().isBefore(cooldownEnd)) {
                        long remainingSeconds = Duration.between(LocalDateTime.now(), cooldownEnd).getSeconds();
                        throw ApiException
                                .badRequest("Vui lòng đợi " + remainingSeconds + " giây trước khi gửi lại OTP");
                    }
                });
    }

    private String normalizePhone(String phone) {
        if (phone == null)
            return null;
        if (phone.startsWith("+84")) {
            return "0" + phone.substring(3);
        }
        return phone;
    }
}
