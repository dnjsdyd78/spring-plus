package org.example.expert.domain.todo.dto.request;

import lombok.Getter;

@Getter
public class TodoSearchRequest {
    private final String keyword;
    private final String nickname;
    private final String startDate;
    private final String endDate;

    public TodoSearchRequest(String keyword, String nickname, String startDate, String endDate) {
        this.keyword = keyword;
        this.nickname = nickname;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
