package com.embergps.dto;

import lombok.Builder;
import lombok.Data;

/** Standard error envelope returned for all 4xx/5xx responses. */
@Data
@Builder
public class ErrorResponse {

    private int status;
    private String error;
    private String message;
    private String path;
    private String timestamp;
}
