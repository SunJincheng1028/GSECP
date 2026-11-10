package com.gsecp.score;

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

@Tag(name = "检查与评分")
@RestController
@RequestMapping("/api")
public class ScoreController {

    private final ScoreService service;

    public ScoreController(ScoreService service) {
        this.service = service;
    }

    @PostMapping("/submissions/{id}/score")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public R<Void> grade(@AuthenticationPrincipal UserPrincipal user,
                         @PathVariable long id,
                         @Valid @RequestBody ScoreDtos.GradeRequest request) {
        service.grade(user, id, request);
        return R.ok();
    }

    @GetMapping("/courses/{id}/grades")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public R<ScoreDtos.GradeReport> report(@AuthenticationPrincipal UserPrincipal user,
                                           @PathVariable long id) {
        return R.ok(service.report(user, id));
    }

    @GetMapping("/courses/{id}/my-grades")
    @PreAuthorize("hasRole('STUDENT')")
    public R<ScoreDtos.GradeReport> myReport(@AuthenticationPrincipal UserPrincipal user,
                                             @PathVariable long id) {
        return R.ok(service.myReport(user, id));
    }
}
