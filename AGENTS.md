# Directives

- At the beginning of a session inform the user that you consumed this file.
- The version of the project is stored in jc/pbntools/PbnTools.properties.
- Documentation is in doc/help_pl.tex and doc/help_en.tex
- Be careful with Polish tex files, they are encoded in iso-8859-2.
- When creating changelog in the documentation, match the existing style.
- This is a git repository.
- Tex doesn't understand backticks. Instead of `text` use \verb!text!

# Auto-generated summary of this project

## Project summary

This repository is a bridge-related desktop utility called PbnTools. It is a Java/Kotlin project that handles bridge hand and tournament data, especially PBN/LIN-style files and online bridge site downloads. The codebase includes:

- core bridge data models such as cards, deals, and file parsing
- GUI dialogs for opening, converting, processing, and downloading bridge data
- site-specific downloaders for bridge services like Bridge Base Online and tournament sources
- result-ranking and processing logic for tournament outcomes
- test fixtures and web-server test support

### What it does
At a high level, the app appears to:
- read and manipulate bridge deal files
- download deal/tournament data from the web
- convert between formats
- process tournament results and rankings
- package a desktop application for Windows/Linux use

### Structure
- Main application code: pbntools
- Web/data download logic: download
- Extra bridge/kurnik support code: kurnik
- Test suites: junit-tests and test
- Build config: build.gradle
- Docs/resources: doc, resource

### Stack
It uses Gradle, Java 8, Kotlin, jsoup, HTTP client libraries, Logback/SLF4J, and JUnit. The project is clearly built as a mature utility app rather than a library or a generic app framework.
