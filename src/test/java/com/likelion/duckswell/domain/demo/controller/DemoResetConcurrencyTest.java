package com.likelion.duckswell.domain.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.likelion.duckswell.domain.course.entity.Course;
import com.likelion.duckswell.domain.course.repository.CourseRepository;
import com.likelion.duckswell.domain.member.entity.Member;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 인증 없는 API라 심사 기간 중 여러 참가자가 리셋 버튼을 동시에 누를 수 있다.
 * DemoResetService.reset()이 회원 행에 비관적 락을 잡고 정리+재시딩을 끝까지 유지하는지,
 * 그래서 동시 요청에도 코스가 중복 생성되지 않는지를 실제 HTTP 동시 호출로 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DemoResetConcurrencyTest {

    private static final int CONCURRENT_REQUESTS = 5;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

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
