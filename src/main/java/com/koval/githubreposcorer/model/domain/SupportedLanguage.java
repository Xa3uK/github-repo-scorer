package com.koval.githubreposcorer.model.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum SupportedLanguage {

    TYPESCRIPT("TypeScript"),
    PYTHON("Python"),
    JAVASCRIPT("JavaScript"),
    JAVA("Java"),
    CSHARP("C#"),
    GO("Go"),
    PHP("PHP"),
    SHELL("Shell"),
    KOTLIN("Kotlin"),
    SWIFT("Swift"),
    RUST("Rust"),
    RUBY("Ruby"),
    DART("Dart"),
    SCALA("Scala"),
    R("R"),
    OBJECTIVE_C("Objective-C"),
    GROOVY("Groovy"),
    POWERSHELL("PowerShell"),
    HASKELL("Haskell"),
    ELIXIR("Elixir");

    private final String githubName;

    private static final Map<String, SupportedLanguage> LOOKUP =
        Arrays.stream(values())
            .collect(Collectors.toMap(
                l -> l.githubName.toLowerCase(),
                l -> l
            ));

    public static Optional<SupportedLanguage> fromString(String value) {
        if (value == null) return Optional.empty();
        return Optional.ofNullable(LOOKUP.get(value.trim().toLowerCase()));
    }

    public static String validValues() {
        return Arrays.stream(values())
                .map(SupportedLanguage::getGithubName)
                .collect(Collectors.joining(", "));
    }
}