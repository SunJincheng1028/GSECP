package com.gsecp.classroom;

import com.gsecp.common.R;
import com.gsecp.security.UserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "班级与邀请码")
@RestController
@RequestMapping("/api/classes")
public class ClassController {

    private final ClassService service;

    public ClassController(ClassService service) {
        this.service = service;
    }

    @GetMapping
    public R<List<ClassDtos.ClassView>> list(@AuthenticationPrincipal UserPrincipal user) {
        return R.ok(service.list(user));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public R<Void> create(@AuthenticationPrincipal UserPrincipal user,
                          @Valid @RequestBody ClassDtos.CreateClassRequest request) {
        service.create(user, request);
        return R.ok();
    }

    @GetMapping("/{id}")
    public R<ClassDtos.ClassView> detail(@AuthenticationPrincipal UserPrincipal user,
                                         @PathVariable long id) {
        return R.ok(service.detail(user, id));
    }

    @GetMapping("/{id}/members")
    public R<List<ClassDtos.MemberView>> members(@AuthenticationPrincipal UserPrincipal user,
                                                 @PathVariable long id) {
        return R.ok(service.members(user, id));
    }

    @DeleteMapping("/{id}/members/{studentId}")
    public R<Void> removeMember(@AuthenticationPrincipal UserPrincipal user,
                                @PathVariable long id,
                                @PathVariable long studentId) {
        service.removeMember(user, id, studentId);
        return R.ok();
    }

    @GetMapping("/{id}/invite-codes")
    public R<List<ClassDtos.InviteCodeView>> inviteCodes(@AuthenticationPrincipal UserPrincipal user,
                                                         @PathVariable long id) {
        return R.ok(service.inviteCodes(user, id));
    }

    @PostMapping("/{id}/invite-codes")
    public R<String> createInviteCode(@AuthenticationPrincipal UserPrincipal user,
                                      @PathVariable long id,
                                      @RequestBody ClassDtos.CreateInviteCodeRequest request) {
        return R.ok(service.createInviteCode(user, id, request));
    }

    @PatchMapping("/{id}/invite-codes/{codeId}")
    public R<Void> updateInviteCode(@AuthenticationPrincipal UserPrincipal user,
                                    @PathVariable long id,
                                    @PathVariable long codeId,
                                    @RequestBody ClassDtos.UpdateInviteCodeRequest request) {
        service.updateInviteCode(user, id, codeId, Boolean.TRUE.equals(request.enabled()));
        return R.ok();
    }
}
