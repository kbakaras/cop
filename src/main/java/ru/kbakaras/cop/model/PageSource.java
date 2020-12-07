package ru.kbakaras.cop.model;

import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class PageSource {

    public final String title;
    public final String content;

    public final List<ImageSource> imageSourceList;
}
