package com.gsecp.step;

import com.gsecp.common.R;
import com.gsecp.security.UserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "步骤与交付项")
@RestController
@RequestMapping("/api")
public class StepController {

    private final StepService service;

    public StepController(StepService service) {
        this.service = service;
    }

    @GetMapping("/courses/{courseId}/steps")
    public R<List<StepDtos.StepView>> list(@AuthenticationPrincipal UserPrincipal user,
                                           @PathVariable long courseId) {
        return R.ok(service.list(user, courseId));
    }

    @PostMapping("/courses/{courseId}/steps")
    public R<Long> create(@AuthenticationPrincipal UserPrincipal user,
                          @PathVariable long courseId,
                          @Valid @RequestBody StepDtos.StepSaveRequest request) {
        return R.ok(service.create(user, courseId, request));
    }

    @GetMapping("/steps/{id}")
    public R<StepDtos.StepView> detail(@AuthenticationPrincipal UserPrincipal user,
                                       @PathVariable long id) {
        return R.ok(service.detail(user, id));
    }

    @PutMapping("/steps/{id}")
    public R<Void> update(@AuthenticationPrincipal UserPrincipal user,
                          @PathVariable long id,
                          @Valid @RequestBody StepDtos.StepSaveRequest request) {
        service.update(user, id, request);
        return R.ok();
    }

    @PatchMapping("/steps/{id}/status")
    public R<Void> updateStatus(@AuthenticationPrincipal UserPrincipal user,
                                @PathVariable long id,
                                @Valid @RequestBody StepDtos.UpdateStatusRequest request) {
        service.updateStatus(user, id, request.status());
        return R.ok();
    }
}
