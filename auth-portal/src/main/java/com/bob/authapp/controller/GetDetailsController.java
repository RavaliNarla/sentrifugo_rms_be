package com.bob.authapp.controller;

import com.bob.authapp.model.AllUsersResponse;
import com.bob.authapp.model.GetCandidateResponse;
import com.bob.authapp.model.GetUserResponse;
import com.bob.authapp.service.GetDetailsService;
import com.bob.db.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/getdetails")
@Validated
public class GetDetailsController {
    @Autowired
    private GetDetailsService getDetailsService;

    @GetMapping("/csrf-token")
    public ResponseEntity<ApiResponse<CsrfToken>> getCsrfToken(HttpServletRequest request) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        return ResponseEntity.ok(ApiResponse.ok(csrfToken,"CSRF"));
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'RECRUITER', 'COMMITEE_MEMBER', 'COMMITTEE_MEMBER', 'ZONAL_HR')")
    @PostMapping("/users")
    public ResponseEntity<ApiResponse<GetUserResponse>> getUser(@RequestParam @NotBlank @Email String email){
            GetUserResponse user = getDetailsService.getUserByEmail(email);
            ApiResponse<GetUserResponse> response = ApiResponse.ok(user,"User fetched successfully");
            return ResponseEntity.ok(response);
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUserDetails(){
        GetUserResponse user = getDetailsService.getUserByToken();
        return ResponseEntity.ok(user);
    }

    @PostMapping("/candidates")
    public ResponseEntity<ApiResponse<GetCandidateResponse>> getCandidateDetails(@RequestParam @NotBlank @Email String email){
            GetCandidateResponse candidate = getDetailsService.getCandidateByEmail(email);
            ApiResponse<GetCandidateResponse> response = ApiResponse.ok(candidate,"Candidate fetched successfully");
            return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN')")
    @GetMapping("/users/all")
    public ResponseEntity< ApiResponse<List<AllUsersResponse>>> getAllUsers() {

            List<AllUsersResponse> users = getDetailsService.getAllUsers();
            ApiResponse<List<AllUsersResponse>> response = ApiResponse.ok(users,"Users fetched successfully");
            return ResponseEntity.ok(response);

    }

    @PreAuthorize("hasAnyAuthority('ADMIN')")
    @GetMapping("/users/search")
    public ResponseEntity<ApiResponse<Page<AllUsersResponse>>> searchUsers(
            @RequestParam
            @NotBlank(message = "Please provide a search term to find users")
            @Size(max = 100, message = "Search term cannot exceed 100 characters") String search,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page number cannot be negative") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Page size must be at least 1") @Max(value = 50, message = "Page size cannot exceed 50") int size) {
            
            // Enforce pagination cap (defense in depth)
            if (size > 50) {
                size = 50;
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));
            Page<AllUsersResponse> users = getDetailsService.searchUsers(search, pageable);
            ApiResponse<Page<AllUsersResponse>> response = ApiResponse.ok(users,"Users fetched successfully");
            return ResponseEntity.ok(response);


    }
}
