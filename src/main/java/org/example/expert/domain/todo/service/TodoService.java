package org.example.expert.domain.todo.service;

import lombok.RequiredArgsConstructor;
import org.example.expert.client.WeatherClient;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.todo.dto.request.TodoSaveRequest;
import org.example.expert.domain.todo.dto.request.TodoSearchRequest;
import org.example.expert.domain.todo.dto.response.TodoResponse;
import org.example.expert.domain.todo.dto.response.TodoSaveResponse;
import org.example.expert.domain.todo.dto.response.TodoSearchResponse;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoService {

    private final TodoRepository todoRepository;
    private final WeatherClient weatherClient;

    @Transactional
    public TodoSaveResponse saveTodo(AuthUser authUser, TodoSaveRequest todoSaveRequest) {
        User user = User.fromAuthUser(authUser);

        String weather = weatherClient.getTodayWeather();

        Todo newTodo = new Todo(
                todoSaveRequest.getTitle(),
                todoSaveRequest.getContents(),
                weather,
                user
        );
        Todo savedTodo = todoRepository.save(newTodo);

        return new TodoSaveResponse(
                savedTodo.getId(),
                savedTodo.getTitle(),
                savedTodo.getContents(),
                weather,
                new UserResponse(user.getId(), user.getEmail())
        );
    }

    public Page<TodoResponse> getTodos(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);

        Page<Todo> todos = todoRepository.findAllByOrderByModifiedAtDesc(pageable);

        return todos.map(todo -> new TodoResponse(
                todo.getId(),
                todo.getTitle(),
                todo.getContents(),
                todo.getWeather(),
                new UserResponse(todo.getUser().getId(), todo.getUser().getEmail()),
                todo.getCreatedAt(),
                todo.getModifiedAt()
        ));
    }

    public TodoResponse getTodo(long todoId) {
        Todo todo = todoRepository.findByIdWithUser(todoId)
                .orElseThrow(() -> new InvalidRequestException("Todo not found"));

        User user = todo.getUser();

        return new TodoResponse(
                todo.getId(),
                todo.getTitle(),
                todo.getContents(),
                todo.getWeather(),
                new UserResponse(user.getId(), user.getEmail()),
                todo.getCreatedAt(),
                todo.getModifiedAt()
        );
    }

    public List<TodoResponse> getTodoSearchResults(String weather, String startDate, String endDate) {

        // 공백데이터 허용x, null 값으로 재정의
        weather = nullIfEmpty(weather);
        startDate = nullIfEmpty(startDate);
        endDate = nullIfEmpty(endDate);

        // 형변환
        LocalDateTime fromDate = convertStringToLocalDateTime(startDate);
        LocalDateTime toDate = convertStringToLocalDateTime(endDate);

        // 날짜범위 유효성 검사
        checkDateRange(fromDate,toDate);


        List<Todo> todos = todoRepository.findByWeatherAndDate(weather, fromDate, toDate)
                .orElseThrow(() -> new IllegalArgumentException("조회되는 목록이 없습니다."));

        return todos.stream()
                .map(todo -> new
                        TodoResponse(
                        todo.getId(),
                        todo.getTitle(),
                        todo.getContents(),
                        todo.getWeather(),
                        new UserResponse(todo.getUser().getId(), todo.getUser().getEmail()),
                        todo.getCreatedAt(),
                        todo.getModifiedAt()))
                .collect(Collectors.toList());
    }

    public Page<TodoSearchResponse> searchTodoResults(int page, int size, TodoSearchRequest request) {
        Pageable pageable = PageRequest.of(page - 1, size);

        // 형변환
        LocalDateTime startDate = convertStringToLocalDateTime(request.getStartDate());
        LocalDateTime endDate = convertStringToLocalDateTime(request.getEndDate());

        // 날짜범위 유효성 검사
        checkDateRange(startDate, endDate);

        return todoRepository.searchByKeywordAndNicknameWithinDateRange(pageable, request.getKeyword(), request.getNickname(), startDate, endDate);
    }

    // 데이터 공백이면 null 값 반환
    private String nullIfEmpty(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    // String => LocalDateTime
    private LocalDateTime convertStringToLocalDateTime(String value) {
        try{
            return (value == null || value.isBlank()) ? null : LocalDate.parse(value).atStartOfDay();
        }catch (DateTimeParseException e){
            throw new IllegalArgumentException("날짜데이터 형식이 올바르지 않습니다. yyyy-MM-dd 형식으로 입력해주세요.");
        }
    }

    private void checkDateRange(LocalDateTime startDate, LocalDateTime endDate){
        if ((startDate == null && endDate != null) || (startDate != null && endDate == null)) {
            throw new IllegalArgumentException("시작날짜와 종료날짜가 모두 입력되거나 입력되지 않아야합니다.");
        }
    }
}
