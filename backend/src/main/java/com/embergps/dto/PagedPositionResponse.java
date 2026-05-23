package com.embergps.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** Paginated list of GPS positions. */
@Data
@Builder
public class PagedPositionResponse {

    private List<GpsPositionDto> positions;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;
}
