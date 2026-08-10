package com.likelion.duckswell.domain.course.controller;

import com.likelion.duckswell.domain.course.dto.CourseResponse;
import com.likelion.duckswell.domain.course.dto.CourseStartRequest;
import com.likelion.duckswell.domain.course.dto.CurrentCourseResponse;
import com.likelion.duckswell.domain.course.service.CourseService;
import com.likelion.duckswell.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController implements CourseApi {

    private final CourseService courseService;

    @Override
    @PostMapping("/start")
    public ResponseEntity<ApiResponse<CourseResponse>> startCourse(@Valid @RequestBody CourseStartRequest request) {
        return ResponseEntity.ok(ApiResponse.success(courseService.startCourse(request)));
    }

    @Override
    @PostMapping("/{courseId}/end")
    public ResponseEntity<ApiResponse<CourseResponse>> endCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(ApiResponse.success(courseService.endCourse(courseId)));
    }

    @Override
    @PostMapping("/restart-focus")
    public ResponseEntity<ApiResponse<CourseResponse>> restartFocusCourse() {
        return ResponseEntity.ok(ApiResponse.success(courseService.restartFocusCourse()));
    }

    @Override
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getCourseHistory() {
        return ResponseEntity.ok(ApiResponse.success(courseService.getCourseHistory()));
    }

    @Override
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<CurrentCourseResponse>> getCurrentCourse() {
        return ResponseEntity.ok(ApiResponse.success(courseService.getCurrentCourse().orElse(null)));
    }
}
