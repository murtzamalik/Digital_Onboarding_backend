package com.bank.cebos.controller.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.cebos.config.SecurityConfig;
import com.bank.cebos.dto.admin.CorporateClientDetailResponse;
import com.bank.cebos.dto.admin.CreateCorporateClientRequest;
import com.bank.cebos.entity.CorporateClient;
import com.bank.cebos.enums.BankAdminRole;
import com.bank.cebos.enums.PrincipalKind;
import com.bank.cebos.repository.CorporateClientRepository;
import com.bank.cebos.repository.UploadBatchRepository;
import com.bank.cebos.security.CebosUserDetails;
import com.bank.cebos.service.admin.AdminCorporateClientWriteService;
import com.bank.cebos.security.JwtAuthenticationFilter;
import com.bank.cebos.security.PrincipalAccessHelper;
import com.bank.cebos.security.ratelimit.RedisMinuteWindowLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = AdminCorporateClientController.class)
@Import({SecurityConfig.class, PrincipalAccessHelper.class})
class AdminCorporateClientControllerTest {

  @Autowired private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @MockBean private CorporateClientRepository corporateClientRepository;

  @MockBean private AdminCorporateClientWriteService adminCorporateClientWriteService;

  @MockBean private UploadBatchRepository uploadBatchRepository;

  @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;

  @MockBean private RedisMinuteWindowLimiter redisMinuteWindowLimiter;

  @BeforeEach
  void jwtFilterDelegates() throws Exception {
    doAnswer(
            inv -> {
              FilterChain chain = inv.getArgument(2);
              chain.doFilter(
                  inv.getArgument(0, ServletRequest.class),
                  inv.getArgument(1, ServletResponse.class));
              return null;
            })
        .when(jwtAuthenticationFilter)
        .doFilter(any(), any(), any());
  }

  @Test
  void listClientsReturns200WithEmptyPage() throws Exception {
    when(corporateClientRepository.findAll(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

    mockMvc
        .perform(get("/api/v1/admin/clients").with(authentication(bankAdminAuth())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(0))
        .andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  void getClientReturns404WhenMissing() throws Exception {
    when(corporateClientRepository.findById(99L)).thenReturn(Optional.empty());

    mockMvc
        .perform(get("/api/v1/admin/clients/99").with(authentication(bankAdminAuth())))
        .andExpect(status().isNotFound());
  }

  @Test
  void getClientReturns200WithBodyWhenPresent() throws Exception {
    CorporateClient entity = mock(CorporateClient.class);
    when(entity.getId()).thenReturn(1L);
    when(entity.getPublicId()).thenReturn("pub-abc");
    when(entity.getClientCode()).thenReturn("CLI-001");
    when(entity.getLegalName()).thenReturn("Acme Ltd");
    when(entity.getStatus()).thenReturn("ACTIVE");
    Instant created = Instant.parse("2026-04-01T10:00:00Z");
    Instant updated = Instant.parse("2026-04-02T11:30:00Z");
    when(entity.getCreatedAt()).thenReturn(created);
    when(entity.getUpdatedAt()).thenReturn(updated);
    when(corporateClientRepository.findById(1L)).thenReturn(Optional.of(entity));

    mockMvc
        .perform(get("/api/v1/admin/clients/1").with(authentication(bankAdminAuth())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.publicId").value("pub-abc"))
        .andExpect(jsonPath("$.clientCode").value("CLI-001"))
        .andExpect(jsonPath("$.legalName").value("Acme Ltd"))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.createdAt").value("2026-04-01T10:00:00Z"))
        .andExpect(jsonPath("$.updatedAt").value("2026-04-02T11:30:00Z"));
  }

  @Test
  void listClientsReturnsOneRow() throws Exception {
    CorporateClient entity = mock(CorporateClient.class);
    when(entity.getId()).thenReturn(5L);
    when(entity.getPublicId()).thenReturn("pub-5");
    when(entity.getClientCode()).thenReturn("C5");
    when(entity.getLegalName()).thenReturn("Beta Co");
    when(entity.getStatus()).thenReturn("PENDING");
    when(corporateClientRepository.findAll(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(0, 20), 1));

    mockMvc
        .perform(get("/api/v1/admin/clients").with(authentication(bankAdminAuth())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].id").value(5))
        .andExpect(jsonPath("$.content[0].legalName").value("Beta Co"));
  }

  @Test
  void createClientReturns201() throws Exception {
    Instant created = Instant.parse("2026-04-01T10:00:00Z");
    CorporateClientDetailResponse body =
        new CorporateClientDetailResponse(
            3L, "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", "NEW-1", "New Co", "ACTIVE", created, created);
    when(adminCorporateClientWriteService.create(any(CreateCorporateClientRequest.class)))
        .thenReturn(body);

    mockMvc
        .perform(
            post("/api/v1/admin/clients")
                .with(authentication(bankAdminAuth()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new CreateCorporateClientRequest("NEW-1", "New Co"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(3))
        .andExpect(jsonPath("$.clientCode").value("NEW-1"));
  }

  private static UsernamePasswordAuthenticationToken bankAdminAuth() {
    CebosUserDetails principal =
        new CebosUserDetails(
            PrincipalKind.BANK_ADMIN,
            1L,
            null,
            List.of(new SimpleGrantedAuthority(BankAdminRole.SUPER_ADMIN.authority())));
    return new UsernamePasswordAuthenticationToken(principal, null, principal.authorities());
  }
}
