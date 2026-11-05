package com.example.ticketing;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.ticketing.entity.*;
import com.example.ticketing.repository.*;
import com.example.ticketing.service.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookingFlowIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired UserRepository users;
  @Autowired PerformanceRepository performances;
  @Autowired SeatRepository seats;
  @Autowired BookingRepository bookings;
  @Autowired PaymentRepository payments;
  @Autowired QueueRepository queues;
  @Autowired PasswordEncoder passwords;
  @Autowired QueueService queueService;
  @Autowired SeatService seatService;
  @Autowired BookingService bookingService;
  User alice, bob, admin;
  Performance performance, otherPerformance;
  Seat seat;
  MockHttpSession aliceSession, bobSession, adminSession;

  @BeforeEach
  void setup() throws Exception {
    SecurityContextHolder.clearContext();
    payments.deleteAll();
    bookings.deleteAll();
    queues.deleteAll();
    seats.deleteAll();
    performances.deleteAll();
    users.deleteAll();
    alice = user("alice", UserType.MEMBER);
    bob = user("bob", UserType.MEMBER);
    admin = user("admin", UserType.ADMIN);
    performance = performance("테스트 공연");
    otherPerformance = performance("다른 공연");
    seat = newSeat(performance, "A-1");
    aliceSession = login("alice");
    bobSession = login("bob");
    adminSession = login("admin");
  }

  User user(String name, UserType type) {
    var u = new User();
    u.setUsername(name);
    u.setPassword(passwords.encode("password123"));
    u.setUserType(type);
    return users.save(u);
  }

  Performance performance(String title) {
    var p = new Performance();
    p.setTitle(title);
    p.setPlace("테스트홀");
    p.setPerformanceDate(LocalDateTime.now().plusDays(30));
    return performances.save(p);
  }

  Seat newSeat(Performance p, String number) {
    var s = new Seat();
    s.setSeatNumber(number);
    s.setPerformance(p);
    s.setStatus(SeatStatus.AVAILABLE);
    s.setPrice(10000);
    return seats.save(s);
  }

  MockHttpSession login(String username) throws Exception {
    var result =
        mvc.perform(
                post("/users/login")
                    .with(csrf())
                    .contentType("application/json")
                    .content(
                        json.writeValueAsString(
                            Map.of("username", username, "password", "password123"))))
            .andExpect(status().isOk())
            .andReturn();
    return (MockHttpSession) result.getRequest().getSession(false);
  }

  JsonNode body(MvcResult r) throws Exception {
    return json.readTree(r.getResponse().getContentAsString());
  }

  void admission(User user) {
    var q = new QueueEntry();
    q.setUserId(user.getId());
    q.setPerformanceId(performance.getPerformanceId());
    q.setStatus(QueueStatus.ALLOWED);
    q.setExpiresAt(LocalDateTime.now(Clock.systemUTC()).plusMinutes(5));
    queues.save(q);
  }

  void hold(MockHttpSession session) throws Exception {
    mvc.perform(post("/seats/" + seat.getId() + "/hold").session(session).with(csrf()))
        .andExpect(status().isOk());
  }

  Long book(MockHttpSession session) throws Exception {
    return body(mvc.perform(
                post("/bookings")
                    .session(session)
                    .with(csrf())
                    .contentType("application/json")
                    .content(
                        json.writeValueAsString(
                            Map.of(
                                "performanceId",
                                performance.getPerformanceId(),
                                "seatIds",
                                List.of(seat.getId()),
                                "userId",
                                bob.getId()))))
            .andExpect(status().isOk())
            .andReturn())
        .get("bookingId")
        .asLong();
  }

  Long pending() throws Exception {
    admission(alice);
    hold(aliceSession);
    return book(aliceSession);
  }

  Long payment(Long bookingId, int amount) throws Exception {
    return body(mvc.perform(
                post("/payments")
                    .session(aliceSession)
                    .with(csrf())
                    .contentType("application/json")
                    .content(
                        json.writeValueAsString(
                            Map.of("bookingId", bookingId, "amount", amount, "method", "card"))))
            .andExpect(status().isOk())
            .andReturn())
        .get("id")
        .asLong();
  }

  @Test
  void anonymousAndMemberCannotUseAdminOrGlobalBookingList() throws Exception {
    mvc.perform(get("/admin.html")).andExpect(status().isUnauthorized());
    mvc.perform(get("/admin.html").session(aliceSession)).andExpect(status().isForbidden());
    mvc.perform(get("/bookings").session(aliceSession)).andExpect(status().isForbidden());
    mvc.perform(get("/bookings").session(adminSession)).andExpect(status().isOk());
  }

  @Test
  void csrfIsRequiredForLoginAndMutations() throws Exception {
    mvc.perform(
            post("/users/login")
                .contentType("application/json")
                .content("{\"username\":\"alice\",\"password\":\"password123\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(post("/seats/" + seat.getId() + "/hold").session(aliceSession))
        .andExpect(status().isForbidden());
  }

  @Test
  void sessionContainsActualUserAndLogoutInvalidatesIt() throws Exception {
    mvc.perform(get("/users/me").session(bobSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(bob.getId()));
    mvc.perform(post("/users/logout").session(bobSession).with(csrf())).andExpect(status().isOk());
    assertThat(bobSession.isInvalid()).isTrue();
    mvc.perform(get("/users/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void wrongPasswordDoesNotAuthenticate() throws Exception {
    mvc.perform(
            post("/users/login")
                .with(csrf())
                .contentType("application/json")
                .content("{\"username\":\"alice\",\"password\":\"incorrect\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void signupCannotGrantAdminAndPasswordIsNotReturned() throws Exception {
    mvc.perform(
            post("/users/signup")
                .with(csrf())
                .contentType("application/json")
                .content(
                    "{\"username\":\"newuser\",\"password\":\"password123\",\"userType\":\"ADMIN\"}"))
        .andExpect(status().isOk());
    assertThat(users.findByUsername("newuser").orElseThrow().getUserType())
        .isEqualTo(UserType.MEMBER);
    var id = pending();
    var response =
        mvc.perform(get("/bookings/" + id).session(aliceSession))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(response).doesNotContain("password", "$2a$", "\"ip\"");
  }

  @Test
  void ownershipAppliesToReadCancelAndUserListing() throws Exception {
    var id = pending();
    mvc.perform(get("/bookings/" + id).session(bobSession)).andExpect(status().isForbidden());
    mvc.perform(delete("/bookings/" + id).session(bobSession).with(csrf()))
        .andExpect(status().isForbidden());
    mvc.perform(get("/bookings/user/" + alice.getId()).session(bobSession))
        .andExpect(status().isForbidden());
    assertThat(bookings.findById(id).orElseThrow().getUser().getId()).isEqualTo(alice.getId());
  }

  @Test
  void waitingUserCannotSelfAdmitAndCannotUseAnotherQueue() throws Exception {
    var result =
        mvc.perform(
                post("/queue/join")
                    .param("performanceId", performance.getPerformanceId().toString())
                    .session(aliceSession)
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();
    var id = body(result).get("id").asLong();
    mvc.perform(
            post("/queue/enter")
                .param("queueId", String.valueOf(id))
                .session(aliceSession)
                .with(csrf()))
        .andExpect(status().isConflict());
    mvc.perform(get("/queue/check").param("queueId", String.valueOf(id)).session(bobSession))
        .andExpect(status().isForbidden());
    queueService.processQueue();
    mvc.perform(
            post("/queue/enter")
                .param("queueId", String.valueOf(id))
                .session(aliceSession)
                .with(csrf()))
        .andExpect(status().isOk());
    mvc.perform(
            post("/queue/join")
                .param("performanceId", performance.getPerformanceId().toString())
                .session(aliceSession)
                .with(csrf()))
        .andExpect(jsonPath("$.id").value(id));
  }

  @Test
  void seatHoldRequiresAdmissionForThatPerformance() throws Exception {
    mvc.perform(post("/seats/" + seat.getId() + "/hold").session(aliceSession).with(csrf()))
        .andExpect(status().isConflict());
    admission(alice);
    hold(aliceSession);
    var q = queues.findAll().get(0);
    q.setExpiresAt(LocalDateTime.now(Clock.systemUTC()).minusSeconds(1));
    queues.save(q);
    mvc.perform(post("/seats/" + seat.getId() + "/hold").session(aliceSession).with(csrf()))
        .andExpect(status().isConflict());
  }

  @Test
  void anotherUserCannotBookHeldSeats() throws Exception {
    admission(alice);
    admission(bob);
    hold(aliceSession);
    mvc.perform(post("/seats/" + seat.getId() + "/hold").session(bobSession).with(csrf()))
        .andExpect(status().isConflict());
    mvc.perform(
            post("/bookings")
                .session(bobSession)
                .with(csrf())
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "performanceId",
                            performance.getPerformanceId(),
                            "seatIds",
                            List.of(seat.getId())))))
        .andExpect(status().isConflict());
  }

  @Test
  void expiredHoldCannotBecomeBookingAndCanBeHeldAgain() throws Exception {
    admission(alice);
    admission(bob);
    hold(aliceSession);
    var s = seats.findById(seat.getId()).orElseThrow();
    s.setHoldExpiresAt(LocalDateTime.now(Clock.systemUTC()).minusSeconds(1));
    seats.save(s);
    mvc.perform(
            post("/bookings")
                .session(aliceSession)
                .with(csrf())
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "performanceId",
                            performance.getPerformanceId(),
                            "seatIds",
                            List.of(seat.getId())))))
        .andExpect(status().isConflict());
    hold(bobSession);
    assertThat(seats.findById(seat.getId()).orElseThrow().getHeldByUserId()).isEqualTo(bob.getId());
  }

  @Test
  void foreignPerformanceAndDuplicateSeatsAreRejected() throws Exception {
    admission(alice);
    hold(aliceSession);
    mvc.perform(
            post("/bookings")
                .session(aliceSession)
                .with(csrf())
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "performanceId",
                            otherPerformance.getPerformanceId(),
                            "seatIds",
                            List.of(seat.getId())))))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/bookings")
                .session(aliceSession)
                .with(csrf())
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "performanceId",
                            performance.getPerformanceId(),
                            "seatIds",
                            List.of(seat.getId(), seat.getId())))))
        .andExpect(status().isBadRequest());
    assertThat(seats.findById(seat.getId()).orElseThrow().getStatus()).isEqualTo(SeatStatus.HELD);
  }

  @Test
  void amountTamperingAndPaymentOwnershipAreRejected() throws Exception {
    var id = pending();
    mvc.perform(
            post("/payments")
                .session(aliceSession)
                .with(csrf())
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        Map.of("bookingId", id, "amount", 1, "method", "card"))))
        .andExpect(status().isBadRequest());
    var paymentId = payment(id, 10000);
    mvc.perform(post("/payments/" + paymentId + "/confirm").session(bobSession).with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void duplicatePaymentRequestsAndConfirmationAreIdempotent() throws Exception {
    var id = pending();
    var paymentId = payment(id, 10000);
    assertThat(payment(id, 10000)).isEqualTo(paymentId);
    for (int i = 0; i < 2; i++)
      mvc.perform(post("/payments/" + paymentId + "/confirm").session(aliceSession).with(csrf()))
          .andExpect(status().isOk());
    assertThat(payments.count()).isEqualTo(1);
    assertThat(bookings.findById(id).orElseThrow().getStatus()).isEqualTo(BookingStatus.COMPLETED);
  }

  @Test
  void cancellationReleasesSeatAndDoesNotReleaseLaterBooking() throws Exception {
    var id = pending();
    var paymentId = payment(id, 10000);
    mvc.perform(post("/payments/" + paymentId + "/confirm").session(aliceSession).with(csrf()))
        .andExpect(status().isOk());
    mvc.perform(delete("/bookings/" + id).session(aliceSession).with(csrf()))
        .andExpect(status().isOk());
    assertThat(payments.findById(paymentId).orElseThrow().getStatus())
        .isEqualTo(PaymentStatus.CANCELED);
    admission(bob);
    hold(bobSession);
    var second = book(bobSession);
    mvc.perform(delete("/bookings/" + id).session(aliceSession).with(csrf()))
        .andExpect(status().isOk());
    assertThat(seats.findById(seat.getId()).orElseThrow().getStatus())
        .isEqualTo(SeatStatus.RESERVED);
    assertThat(bookings.findById(second).orElseThrow().getStatus())
        .isEqualTo(BookingStatus.PENDING);
  }

  @Test
  void paymentCancelAlsoReleasesSeat() throws Exception {
    var id = pending();
    var paymentId = payment(id, 10000);
    mvc.perform(post("/payments/" + paymentId + "/cancel").session(aliceSession).with(csrf()))
        .andExpect(status().isOk());
    assertThat(seats.findById(seat.getId()).orElseThrow().getStatus())
        .isEqualTo(SeatStatus.AVAILABLE);
    assertThat(bookings.findById(id).orElseThrow().getStatus()).isEqualTo(BookingStatus.CANCELED);
  }

  @Test
  void abandonedBookingExpiresAndCannotBePaid() throws Exception {
    var id = pending();
    var paymentId = payment(id, 10000);
    var booking = bookings.findById(id).orElseThrow();
    booking.setExpiresAt(LocalDateTime.now(Clock.systemUTC()).minusSeconds(1));
    bookings.save(booking);
    mvc.perform(post("/payments/" + paymentId + "/confirm").session(aliceSession).with(csrf()))
        .andExpect(status().isConflict());
    bookingService.expirePendingBookings();
    assertThat(seats.findById(seat.getId()).orElseThrow().getStatus())
        .isEqualTo(SeatStatus.AVAILABLE);
    assertThat(payments.findById(paymentId).orElseThrow().getStatus())
        .isEqualTo(PaymentStatus.CANCELED);
  }

  @Test
  void simultaneousHoldHasOnlyOneWinner() throws Exception {
    admission(alice);
    admission(bob);
    var start = new CountDownLatch(1);
    var pool = Executors.newFixedThreadPool(2);
    try {
      var a = pool.submit(() -> raceHold("alice", start));
      var b = pool.submit(() -> raceHold("bob", start));
      start.countDown();
      assertThat(List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(200, 409);
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void seatHoldLimitIsEnforcedOnServer() throws Exception {
    admission(alice);
    hold(aliceSession);
    for (int i = 2; i <= 4; i++) {
      var next = newSeat(performance, "A-" + i);
      mvc.perform(post("/seats/" + next.getId() + "/hold").session(aliceSession).with(csrf()))
          .andExpect(status().isOk());
    }
    var fifth = newSeat(performance, "A-5");
    mvc.perform(post("/seats/" + fifth.getId() + "/hold").session(aliceSession).with(csrf()))
        .andExpect(status().isConflict());
  }

  @Test
  void fiveSeatBookingIsRejectedBeforeMutation() throws Exception {
    var ids = new ArrayList<Long>();
    for (int i = 0; i < 5; i++) ids.add(newSeat(performance, "B-" + i).getId());
    mvc.perform(
            post("/bookings")
                .session(aliceSession)
                .with(csrf())
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        Map.of("performanceId", performance.getPerformanceId(), "seatIds", ids))))
        .andExpect(status().isBadRequest());
    assertThat(bookings.count()).isZero();
  }

  @Test
  void multiSeatBookingRollsBackWhenOneHoldIsInvalid() throws Exception {
    admission(alice);
    hold(aliceSession);
    var other = newSeat(performance, "B-1");
    mvc.perform(
            post("/bookings")
                .session(aliceSession)
                .with(csrf())
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "performanceId",
                            performance.getPerformanceId(),
                            "seatIds",
                            List.of(seat.getId(), other.getId())))))
        .andExpect(status().isConflict());
    assertThat(seats.findById(seat.getId()).orElseThrow().getStatus()).isEqualTo(SeatStatus.HELD);
    assertThat(bookings.count()).isZero();
  }

  @Test
  void simultaneousBookingCannotReserveSeatTwice() throws Exception {
    admission(alice);
    hold(aliceSession);
    var start = new CountDownLatch(1);
    var pool = Executors.newFixedThreadPool(2);
    try {
      Callable<Integer> task =
          () -> {
            SecurityContextHolder.getContext()
                .setAuthentication(
                    UsernamePasswordAuthenticationToken.authenticated("alice", "", List.of()));
            try {
              start.await();
              bookingService.createBooking(performance.getPerformanceId(), List.of(seat.getId()));
              return 200;
            } catch (ResponseStatusException ex) {
              return ex.getStatusCode().value();
            } finally {
              SecurityContextHolder.clearContext();
            }
          };
      var a = pool.submit(task);
      var b = pool.submit(task);
      start.countDown();
      assertThat(List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(200, 409);
      assertThat(bookings.count()).isEqualTo(1);
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void adminSeatCountsAreAppliedAndCannotBeGeneratedTwice() throws Exception {
    var id = otherPerformance.getPerformanceId();
    mvc.perform(
            post("/admin/performances/" + id + "/seats")
                .param("vipCount", "1")
                .param("rCount", "2")
                .param("sCount", "0")
                .session(adminSession)
                .with(csrf()))
        .andExpect(status().isOk());
    assertThat(seats.findByPerformance_PerformanceId(id)).hasSize(3);
    mvc.perform(post("/admin/performances/" + id + "/seats").session(adminSession).with(csrf()))
        .andExpect(status().isConflict());
  }

  @Test
  void queueJoinIsIdempotentDuringWaiting() throws Exception {
    for (int i = 0; i < 2; i++)
      mvc.perform(
              post("/queue/join")
                  .param("performanceId", performance.getPerformanceId().toString())
                  .session(aliceSession)
                  .with(csrf()))
          .andExpect(status().isOk());
    assertThat(queues.count()).isEqualTo(1);
  }

  @Test
  void invalidSignupAndAdminPerformanceDataAreRejected() throws Exception {
    mvc.perform(
            post("/users/signup")
                .with(csrf())
                .contentType("application/json")
                .content("{\"username\":\"newuser\",\"password\":\"short\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/admin/performances")
                .session(adminSession)
                .with(csrf())
                .contentType("application/json")
                .content("{\"title\":\"\",\"price\":-1}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void batchHoldRollsBackAllSeatsWhenOneSeatIsUnavailable() throws Exception {
    admission(alice);
    admission(bob);
    hold(bobSession);
    var available = newSeat(performance, "Z-1");
    mvc.perform(
            post("/seats/hold")
                .session(aliceSession)
                .with(csrf())
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "performanceId",
                            performance.getPerformanceId(),
                            "seatIds",
                            List.of(available.getId(), seat.getId())))))
        .andExpect(status().isConflict());
    assertThat(seats.findById(available.getId()).orElseThrow().getStatus())
        .isEqualTo(SeatStatus.AVAILABLE);
    assertThat(seats.findById(seat.getId()).orElseThrow().getHeldByUserId()).isEqualTo(bob.getId());
  }

  @Test
  void batchHoldAndBookingUseSameOwner() throws Exception {
    admission(alice);
    var second = newSeat(performance, "A-2");
    var ids = List.of(seat.getId(), second.getId());
    mvc.perform(
            post("/seats/hold")
                .session(aliceSession)
                .with(csrf())
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        Map.of("performanceId", performance.getPerformanceId(), "seatIds", ids))))
        .andExpect(status().isOk());
    mvc.perform(
            post("/bookings")
                .session(aliceSession)
                .with(csrf())
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        Map.of("performanceId", performance.getPerformanceId(), "seatIds", ids))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.seats.length()").value(2));
  }

  int raceHold(String username, CountDownLatch start) throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(username, "", List.of()));
    try {
      start.await();
      seatService.holdSeat(seat.getId());
      return 200;
    } catch (ResponseStatusException ex) {
      return ex.getStatusCode().value();
    } finally {
      SecurityContextHolder.clearContext();
    }
  }
}
