package org.example.expert.domain.todo.dto.response;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;

@Getter
public class TodoSearchResponse {
    private final String title;
    private final int managerCount;
    private final int totalComment;



    @QueryProjection
    public TodoSearchResponse(String title, int managerCount, int totalComment) {
        this.title = title;
        this.managerCount = managerCount;
        this.totalComment = totalComment;
    }
}
