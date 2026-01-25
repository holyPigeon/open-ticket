package com.example.openticket.domain.event.persistence;

import static com.example.openticket.domain.event.QEvent.event;

import com.example.openticket.api.service.event.dto.request.EventSearchServiceRequest;
import com.example.openticket.domain.event.Category;
import com.example.openticket.domain.event.Event;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
public class EventRepositoryImpl implements EventRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public Page<Event> findAllWithSearchCondition(EventSearchServiceRequest request, Pageable pageable) {
        List<Event> events = queryFactory
                .selectFrom(event)
                .where(
                        titleContains(request.title()),
                        venueContains(request.venue()),
                        categoryEq(request.category())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize()) // 여기서는 +1 안함 (Page니까)
                .fetch();
        JPAQuery<Long> countQuery = getCountQuery(request);

        return PageableExecutionUtils.getPage(events, pageable, countQuery::fetchOne);
    }

    private JPAQuery<Long> getCountQuery(EventSearchServiceRequest request) {
        return queryFactory
                .select(event.count())
                .from(event)
                .where(
                        titleContains(request.title()),
                        venueContains(request.venue()),
                        categoryEq(request.category())
                );
    }

    private BooleanExpression titleContains(String title) {
        return StringUtils.hasText(title) ? event.title.contains(title) : null;
    }

    private BooleanExpression venueContains(String venue) {
        return StringUtils.hasText(venue) ? event.venue.contains(venue) : null;
    }

    private BooleanExpression categoryEq(Category category) {
        return category != null ? event.category.eq(category) : null;
    }
}
