package org.example.expert.domain.todo.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Wildcard;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.expert.domain.todo.dto.response.QTodoSearchResponse;
import org.example.expert.domain.todo.dto.response.TodoSearchResponse;
import org.example.expert.domain.todo.entity.Todo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.example.expert.domain.comment.entity.QComment.comment;
import static org.example.expert.domain.manager.entity.QManager.manager;
import static org.example.expert.domain.todo.entity.QTodo.todo;
import static org.example.expert.domain.user.entity.QUser.user;

@Repository
@RequiredArgsConstructor
public class TodoQueryRepositoryImpl implements TodoQueryRepository {

    private final JPAQueryFactory q;

    @Override
    public Optional<Todo> findByIdWithUser(Long todoId) {
        return Optional.ofNullable(
                q.selectFrom(todo)
                        .leftJoin(todo.user, user).fetchJoin()
                        .where(todo.id.eq(todoId))
                        .fetchOne()
        );
    }

    @Override
    public Page<TodoSearchResponse> searchByKeywordAndNicknameWithinDateRange(
            Pageable pageable,
            String keyword, String nickname,
            LocalDateTime startDate, LocalDateTime endDate
    ) {
        List<TodoSearchResponse> results = q
                .select(
                        new QTodoSearchResponse(
                                todo.title,
                                todo.managers.size(),
                                todo.comments.size()
                        )
                )
                .from(todo)
                .leftJoin(todo.managers, manager)
                .leftJoin(todo.comments, comment)
                .leftJoin(manager.user, user)
                //기간 필터링
                .where(orIfPresentForDateRange(startDate, endDate))
                //키워드, 닉네임 포함여부 필터링
                .where(orIfPresentForKeywordAndNickname(keyword, nickname))
                //페이져블 설정
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 총 데이터 카운트
        long value = q
                .select(Wildcard.count)
                .from(todo)
                .leftJoin(todo.managers, manager)
                .leftJoin(todo.comments, comment)
                .leftJoin(manager.user, user)
                .where(orIfPresentForDateRange(startDate, endDate))
                .where(orIfPresentForKeywordAndNickname(keyword, nickname))
                .fetchOne();

        long totalCount = Optional.of(value).orElse(0L);
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        return new PageImpl<>(results, pageable, totalCount);
    }


    // 키워드, 닉네임 유효성 검사
    public Predicate orIfPresentForKeywordAndNickname(String keyword, String nickname) {
        BooleanBuilder builder = new BooleanBuilder();

        if (keyword != null && !keyword.isBlank()) {
            builder.or(todo.title.contains(keyword));
        }

        if (nickname != null && !nickname.isBlank()) {
            builder.or(user.nickname.contains(nickname));
        }

        return builder;
    }

    // 날짜기간 유효성 검사
    public Predicate orIfPresentForDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        BooleanBuilder builder = new BooleanBuilder();

        if (startDate != null && endDate != null) {
            builder.or(todo.createdAt.between(startDate, endDate));
        }

        return builder;
    }
}
