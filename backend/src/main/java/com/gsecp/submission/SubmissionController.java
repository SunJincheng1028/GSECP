package com.gsecp.submission;

import com.gsecp.common.R;
import com.gsecp.security.UserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "成果提交与进度")
@RestController
@RequestMapping("/api")
public class SubmissionController {

    private final SubmissionService service;

    public SubmissionController(SubmissionService service) {
        this.service = service;
    }

    @PostMapping("/steps/{id}/submissions")
    @PreAuthorize("hasRole('STUDENT')")
    public R<Long> submit(@AuthenticationPrincipal UserPrincipal user,
                          @PathVariable long id,
                          @Valid @RequestBody SubmissionDtos.SubmitRequest request) {
        return R.ok(service.submit(user, id, request));
    }

    @GetMapping("/steps/{id}/my-submission")
    @PreAuthorize("hasRole('STUDENT')")
    public R<SubmissionDtos.SubmissionView> mySubmission(
            @AuthenticationPrincipal UserPrincipal user, @PathVariable long id) {
        return R.ok(service.mySubmission(user, id));
    }

    @GetMapping("/steps/{id}/my-submissions")
    @PreAuthorize("hasRole('STUDENT')")
    public R<List<SubmissionDtos.SubmissionView>> myVersions(
            @AuthenticationPrincipal UserPrincipal user, @PathVariable long id) {
        return R.ok(service.myVersions(user, id));
    }

    @GetMapping("/steps/{id}/submissions")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public R<List<SubmissionDtos.ProgressView>> progress(
            @AuthenticationPrincipal UserPrincipal user, @PathVariable long id) {
        return R.ok(service.progress(user, id));
    }

    @GetMapping("/submissions/{id}")
    public R<SubmissionDtos.SubmissionView> detail(
            @AuthenticationPrincipal UserPrincipal user, @PathVariable long id) {
        return R.ok(service.detail(user, id));
    }
}
