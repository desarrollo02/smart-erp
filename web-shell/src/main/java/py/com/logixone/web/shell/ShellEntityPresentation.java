package py.com.logixone.web.shell;

import java.util.Objects;

/** Shell-owned copy for one directory/create/detail entity journey. */
record ShellEntityPresentation(
        String contextLabel,
        String createTitle,
        String createDescription,
        String newActionLabel,
        String backActionLabel,
        String detailDescription,
        String summaryDescription) {

    ShellEntityPresentation {
        contextLabel = Objects.requireNonNull(contextLabel, "contextLabel");
        createTitle = Objects.requireNonNull(createTitle, "createTitle");
        createDescription = Objects.requireNonNull(createDescription, "createDescription");
        newActionLabel = Objects.requireNonNull(newActionLabel, "newActionLabel");
        backActionLabel = Objects.requireNonNull(backActionLabel, "backActionLabel");
        detailDescription = Objects.requireNonNull(detailDescription, "detailDescription");
        summaryDescription = Objects.requireNonNull(summaryDescription, "summaryDescription");
    }
}
