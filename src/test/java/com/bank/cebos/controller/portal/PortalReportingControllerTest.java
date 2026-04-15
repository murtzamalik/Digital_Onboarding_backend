package com.bank.cebos.controller.portal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.cebos.enums.PortalRole;
import com.bank.cebos.enums.PrincipalKind;
import com.bank.cebos.security.CebosUserDetails;
import com.bank.cebos.service.reporting.ReportingService;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PortalReportingControllerTest {

  private MockMvc mockMvc;
  private ReportingService reportingService;

  @BeforeAll
  void initOnce() throws IOException {
    reportingService = mock(ReportingService.class);
    applyReportingStubbing(reportingService);
    PortalReportingController controller = new PortalReportingController(reportingService);
    CebosUserDetails portalUser =
        new CebosUserDetails(
            PrincipalKind.PORTAL,
            1L,
            42L,
            List.of(new SimpleGrantedAuthority(PortalRole.ADMIN.authority())));
    HandlerMethodArgumentResolver principalResolver =
        new HandlerMethodArgumentResolver() {
          @Override
          public boolean supportsParameter(MethodParameter parameter) {
            return CebosUserDetails.class.isAssignableFrom(parameter.getParameterType());
          }

          @Override
          public Object resolveArgument(
              MethodParameter parameter,
              ModelAndViewContainer mavContainer,
              NativeWebRequest webRequest,
              WebDataBinderFactory binderFactory) {
            return portalUser;
          }
        };
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setCustomArgumentResolvers(principalResolver)
            .build();
  }

  private static void applyReportingStubbing(ReportingService reportingService) throws IOException {
    doAnswer(
            inv -> {
              try {
                OutputStream o = inv.getArgument(2);
                o.write("batch,line\n".getBytes(StandardCharsets.UTF_8));
              } catch (IOException e) {
                throw new UncheckedIOException(e);
              }
              return null;
            })
        .when(reportingService)
        .streamBatchReport(eq("BATCH-123"), eq(42L), any(OutputStream.class));
    doAnswer(
            inv -> {
              try {
                OutputStream o = inv.getArgument(2);
                o.write("month,line\n".getBytes(StandardCharsets.UTF_8));
              } catch (IOException e) {
                throw new UncheckedIOException(e);
              }
              return null;
            })
        .when(reportingService)
        .streamMonthlyReport(eq("2026-04"), eq(42L), any(OutputStream.class));
    doAnswer(
            inv -> {
              try {
                OutputStream o = inv.getArgument(1);
                o.write("blocked,line\n".getBytes(StandardCharsets.UTF_8));
              } catch (IOException e) {
                throw new UncheckedIOException(e);
              }
              return null;
            })
        .when(reportingService)
        .streamBlockedReport(eq(42L), any(OutputStream.class));
  }

  @Test
  void portalStreamingReportsInvokeReportingService() throws Exception {
    MvcResult monthly =
        mockMvc
            .perform(get("/api/v1/portal/reports/monthly").param("month", "2026-04"))
            .andExpect(request().asyncStarted())
            .andReturn();
    mockMvc.perform(asyncDispatch(monthly)).andExpect(status().isOk());

    MvcResult batch =
        mockMvc
            .perform(get("/api/v1/portal/reports/batch/BATCH-123"))
            .andExpect(request().asyncStarted())
            .andReturn();
    mockMvc
        .perform(asyncDispatch(batch))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition", "attachment; filename=\"batch-report-BATCH-123.csv\""));

    MvcResult blocked =
        mockMvc
            .perform(get("/api/v1/portal/reports/blocked"))
            .andExpect(request().asyncStarted())
            .andReturn();
    mockMvc.perform(asyncDispatch(blocked)).andExpect(status().isOk());

    verify(reportingService).streamMonthlyReport(eq("2026-04"), eq(42L), any(OutputStream.class));
    verify(reportingService).streamBatchReport(eq("BATCH-123"), eq(42L), any(OutputStream.class));
    verify(reportingService).streamBlockedReport(eq(42L), any(OutputStream.class));
  }
}
