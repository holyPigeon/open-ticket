package com.example.openticket.domain.event.persistence;

import static com.example.openticket.domain.event.QEvent.event;

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

    public Page<Event> findAllWithSearchCondition(EventSearchCondition searchCondition, Pageable pageable) {
        List<Event> events = queryFactory
                .selectFrom(event)
                .where(
                        titleContains(searchCondition.title()),
                        venueContains(searchCondition.venue()),
                        categoryEq(searchCondition.category())
                )
                .orderBy(event.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
        JPAQuery<Long> countQuery = getCountQuery(searchCondition);

        return PageableExecutionUtils.getPage(events, pageable, countQuery::fetchOne);
    }

    private JPAQuery<Long> getCountQuery(EventSearchCondition searchCondition) {
        return queryFactory
                .select(event.count())
                .from(event)
                .where(
                        titleContains(searchCondition.title()),
                        venueContains(searchCondition.venue()),
                        categoryEq(searchCondition.category())
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
