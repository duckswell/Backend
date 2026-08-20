package com.likelion.duckswell.domain.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.likelion.duckswell.domain.course.entity.Course;
import com.likelion.duckswell.domain.course.entity.RoutineType;
import com.likelion.duckswell.domain.course.entity.RoutineTypeCode;
import com.likelion.duckswell.domain.course.repository.CourseRepository;
import com.likelion.duckswell.domain.course.repository.RoutineTypeRepository;
import com.likelion.duckswell.domain.member.entity.Member;
import com.likelion.duckswell.domain.product.entity.Ingredient;
import com.likelion.duckswell.domain.product.entity.IngredientCategory;
import com.likelion.duckswell.domain.product.repository.IngredientRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 인증 없는 API라 심사 기간 중 여러 참가자가 리셋 버튼을 동시에 누를 수 있다.
 * DemoResetService.reset()이 회원 행에 비관적 락을 잡고 정리+재시딩을 끝까지 유지하는지,
 * 그래서 동시 요청에도 코스가 중복 생성되지 않는지를 실제 HTTP 동시 호출로 검증한다.
 *
 * DemoResetService는 routine_type/ingredient 마스터 데이터가 이미 있다고 가정하는데,
 * CI는 매번 빈 DB로 시작하므로(deploy.yml의 mysql 서비스 컨테이너) 여기서 필요한 최소
 * 마스터 데이터를 직접 준비한다 - 로컬처럼 이미 시드돼 있는 환경에서는 중복 저장하지
 * 않도록 존재 여부를 먼저 확인한다.
 *
 * 이 테스트는 @SpringBootTest라 (다른 @DataJpaTest들과 달리) 끝나도 트랜잭션이 롤백되지
 * 않는다 - 여기서 새로 만든 ingredient를 안 지우면, ingredient.name UNIQUE 제약 때문에
 * 같은 이름으로 자기 픽스처를 만드는 다른 테스트(RoutineServiceTest 등)가 이후 같은
 * 스위트 실행에서 깨진다. 그래서 이 테스트가 직접 만든 ingredient만 골라서 뒤에 지운다
 * (이미 존재하던 건 건드리지 않음). routine_type은 FK로 참조하는 course가 남아있을 수
 * 있어 지우지 않는다 - 존재 여부만 확인하고 재사용하는 건 다른 테스트(CourseServiceTest.
 * seedRoutineType)도 쓰는 기존 컨벤션이라 안전하다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DemoResetConcurrencyTest {

    private static final int CONCURRENT_REQUESTS = 5;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private RoutineTypeRepository routineTypeRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    private final List<Long> ingredientIdsCreatedByThisTest = new ArrayList<>();

    @BeforeEach
    void ensureDemoMasterData() {
        if (routineTypeRepository.findById(RoutineTypeCode.COOLDOWN).isEmpty()) {
            routineTypeRepository.save(new RoutineType(RoutineTypeCode.COOLDOWN, "쿨다운", "시술 후 진정 관리", null));
        }
        ensureIngredient("히알루론산");
        ensureIngredient("센텔라");
        ensureIngredient("판테놀");
    }

    @AfterEach
    void cleanUpIngredientsCreatedByThisTest() {
        if (!ingredientIdsCreatedByThisTest.isEmpty()) {
            ingredientRepository.deleteAllById(ingredientIdsCreatedByThisTest);
        }
    }

    private void ensureIngredient(String name) {
        if (ingredientRepository.findByName(name).isEmpty()) {
            Ingredient saved = ingredientRepository.save(new Ingredient(name, IngredientCategory.MOISTURE, name + " 테스트용 성분"));
            ingredientIdsCreatedByThisTest.add(saved.getId());
        }
    }

    @Test
    void 동시에_리셋_요청이_와도_코스가_하나만_생성된다() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        CountDownLatch readyLatch = new CountDownLatch(CONCURRENT_REQUESTS);
        CountDownLatch startLatch = new CountDownLatch(1);

        try {
            List<Future<Void>> futures = new ArrayList<>();
            for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
                futures.add(executor.submit(resetRequestTask(readyLatch, startLatch)));
            }

            boolean allReady = readyLatch.await(10, TimeUnit.SECONDS);
            assertThat(allReady).as("모든 요청 스레드가 준비되지 않아 락 경합을 검증할 수 없습니다").isTrue();
            startLatch.countDown();

            awaitAll(futures);
        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        }

        List<Course> courses = courseRepository.findByMemberIdOrderByStartedAtDescIdDesc(Member.DEFAULT_ID);
        assertThat(courses).hasSize(1);
    }

    /** 개별 요청의 RuntimeException/AssertionError(예: HTTP 200이 아닌 응답)를 테스트 실패로 전파한다. */
    private void awaitAll(List<Future<Void>> futures) {
        for (Future<Void> future : futures) {
            try {
                future.get(30, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                throw new AssertionError("동시 리셋 요청 중 하나가 실패했습니다", e.getCause());
            } catch (TimeoutException e) {
                throw new AssertionError("동시 리셋 요청이 제한 시간 내에 끝나지 않았습니다", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("결과를 기다리는 중 인터럽트되었습니다", e);
            }
        }
    }

    private Callable<Void> resetRequestTask(CountDownLatch readyLatch, CountDownLatch startLatch) {
        return () -> {
            readyLatch.countDown();
            startLatch.await();
            mockMvc.perform(post("/api/demo/reset")).andExpect(status().isOk());
            return null;
        };
    }
}
