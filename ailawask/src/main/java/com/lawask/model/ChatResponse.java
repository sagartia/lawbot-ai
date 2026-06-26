package com.lawask.model;

import java.util.List;

// record 自動幫你產生 getter、constructor，不需要 Lombok。
public record ChatResponse(
        String answer,
        List<Source> sources) {
    public record Source(
            String lawName,
            String articleNo) {
    }
}
