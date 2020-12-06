package ru.kbakaras.cop.adoc.model;

import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class PageSource {

    public final String title;
    public final String content;

    public final List<ImageSource> imageSourceList;
}
