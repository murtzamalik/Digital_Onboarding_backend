package com.bank.cebos.service.admin;

import com.bank.cebos.dto.admin.BankAdminUserResponse;
import com.bank.cebos.dto.admin.CreateBankAdminUserRequest;
import com.bank.cebos.entity.BankAdminUser;
import com.bank.cebos.enums.BankAdminRole;
import com.bank.cebos.repository.BankAdminUserRepository;
import com.bank.cebos.service.auth.BankAdminLoginLockoutService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminBankAdminUserService {

  private static final String ACTIVE = "ACTIVE";

  private final BankAdminUserRepository bankAdminUserRepository;
  private final PasswordEncoder passwordEncoder;
  private final BankAdminLoginLockoutService bankAdminLoginLockoutService;

  public AdminBankAdminUserService(
      BankAdminUserRepository bankAdminUserRepository,
      PasswordEncoder passwordEncoder,
      BankAdminLoginLockoutService bankAdminLoginLockoutService) {
    this.bankAdminUserRepository = bankAdminUserRepository;
    this.passwordEncoder = passwordEncoder;
    this.bankAdminLoginLockoutService = bankAdminLoginLockoutService;
  }

  @Transactional(readOnly = true)
  public Page<BankAdminUserResponse> list(Pageable pageable) {
    return bankAdminUserRepository.findAllByOrderByEmailAsc(pageable).map(AdminBankAdminUserService::toDto);
  }

  @Transactional
  public BankAdminUserResponse updatePassword(long id, String newPassword) {
    BankAdminUser u =
        bankAdminUserRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    u.setPasswordHash(passwordEncoder.encode(newPassword));
    BankAdminUser saved = bankAdminUserRepository.save(u);
    bankAdminLoginLockoutService.clearOnSuccess(saved.getEmail());
    return toDto(saved);
  }

  @Transactional
  public BankAdminUserResponse create(CreateBankAdminUserRequest request) {
    String email = request.email().trim();
    if (bankAdminUserRepository.existsByEmailIgnoreCase(email)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
    }
    BankAdminRole role;
    try {
      role = BankAdminRole.valueOf(request.role().trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid bank admin role");
    }
    BankAdminUser u = new BankAdminUser();
    u.setEmail(email);
    u.setPasswordHash(passwordEncoder.encode(request.password()));
    u.setFullName(request.fullName().trim());
    u.setRole(role.name());
    u.setStatus(ACTIVE);
    return toDto(bankAdminUserRepository.save(u));
  }

  private static BankAdminUserResponse toDto(BankAdminUser u) {
    return new BankAdminUserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.getStatus());
  }
}
