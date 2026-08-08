package com.likelion.duckswell.domain.course.repository;

import com.likelion.duckswell.domain.course.entity.Course;
import com.likelion.duckswell.domain.course.entity.CourseStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByMemberIdAndStatus(Long memberId, CourseStatus status);

    List<Course> findByMemberIdOrderByStartedAtDesc(Long memberId);
}
