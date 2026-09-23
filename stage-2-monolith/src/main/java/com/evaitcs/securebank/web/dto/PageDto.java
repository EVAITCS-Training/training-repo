package com.evaitcs.securebank.web.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * A stable JSON envelope for paginated results.
 *
 * WHY NOT serialize Spring Data's Page<T> directly? Its JSON shape is an
 * internal implementation detail that has changed between versions (and
 * serializes with warnings). Defining our own envelope keeps the API
 * contract OURS — clients can rely on content/page/size/totalElements/
 * totalPages forever. This is the "DTO at the edge" rule applied to
 * infrastructure types too.
 *
 * `from(page, mapper)` maps entity pages to DTO pages in one step:
 *   PageDto.from(txPage, TransactionResponse::from)
 */
public record PageDto<T>(
        List<T> content,
        int page,           // 0-based (Spring convention)
        int size,
        long totalElements,
        int totalPages
) {
    public static <E, T> PageDto<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageDto<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
