package com.example.openticket.domain.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Category {

    CONCERT("콘서트"),
    SPORTS("스포츠");

    private final String name;
}
