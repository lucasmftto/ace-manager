package com.acemanager.auth.dto;

import com.acemanager.auth.AppRole;

public record LoginResponse(
        String accessToken,
        AppRole role,
        String email
) {}
