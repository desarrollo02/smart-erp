package py.com.logixone.web.shell;

import java.util.Objects;

/** Closed, non-executable fragment presentation selected from a public fragment id. */
public final class ShellScreenFragmentView {

    private final String id;
    private final String eyebrow;
    private final String title;
    private final String body;
    private final String toneClass;

    ShellScreenFragmentView(
            String id,
            String eyebrow,
            String title,
            String body,
            String toneClass) {
        this.id = Objects.requireNonNull(id, "id");
        this.eyebrow = Objects.requireNonNull(eyebrow, "eyebrow");
        this.title = Objects.requireNonNull(title, "title");
        this.body = Objects.requireNonNull(body, "body");
        this.toneClass = Objects.requireNonNull(toneClass, "toneClass");
    }

    public String getId() {
        return id;
    }

    public String getEyebrow() {
        return eyebrow;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getStyleClass() {
        return "screen-fragment " + toneClass;
    }
}
