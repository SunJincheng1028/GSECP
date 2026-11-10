package com.gsecp.course;

import com.gsecp.common.R;
import com.gsecp.security.UserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Tag(name = "课程")
@RestController
@RequestMapping("/api")
public class CourseController {

    private final CourseService service;

    public CourseController(CourseService service) {
        this.service = service;
    }

    @GetMapping("/classes/{classId}/courses")
    public R<List<CourseDtos.CourseView>> list(@AuthenticationPrincipal UserPrincipal user,
                                               @PathVariable long classId) {
        return R.ok(service.list(user, classId));
    }

    @PostMapping("/classes/{classId}/courses")
    public R<Void> create(@AuthenticationPrincipal UserPrincipal user,
                          @PathVariable long classId,
                          @Valid @RequestBody CourseDtos.CreateCourseRequest request) {
        service.create(user, classId, request);
        return R.ok();
    }

    @GetMapping("/courses/{id}")
    public R<CourseDtos.CourseView> detail(@AuthenticationPrincipal UserPrincipal user,
                                           @PathVariable long id) {
        return R.ok(service.detail(user, id));
    }

    @PatchMapping("/courses/{id}/status")
    public R<Void> updateStatus(@AuthenticationPrincipal UserPrincipal user,
                                @PathVariable long id,
                                @Valid @RequestBody CourseDtos.UpdateStatusRequest request) {
        service.updateStatus(user, id, request.status().toUpperCase());
        return R.ok();
    }
}
