package com.likelion.duckswell.domain.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.likelion.duckswell.domain.course.entity.Course;
import com.likelion.duckswell.domain.course.repository.CourseRepository;
import com.likelion.duckswell.domain.member.entity.Member;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

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
        CountDownLatch doneLatch = new CountDownLatch(CONCURRENT_REQUESTS);

        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    mockMvc.perform(MockMvcRequestBuilders.post("/api/demo/reset"));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(10, TimeUnit.SECONDS);
        startLatch.countDown();
        boolean finished = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        List<Course> courses = courseRepository.findByMemberIdOrderByStartedAtDescIdDesc(Member.DEFAULT_ID);
        assertThat(courses).hasSize(1);
    }
}
