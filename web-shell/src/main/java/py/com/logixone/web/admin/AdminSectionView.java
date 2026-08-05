package py.com.logixone.web.admin;

import java.util.Objects;

/** Presentation-only description of an authorized future administration area. */
public final class AdminSectionView {

    private final String icon;
    private final String title;
    private final String description;
    private final String outcome;

    public AdminSectionView(String icon, String title, String description, String outcome) {
        this.icon = Objects.requireNonNull(icon, "icon");
        this.title = Objects.requireNonNull(title, "title");
        this.description = Objects.requireNonNull(description, "description");
        this.outcome = outcome;
    }

    public String getIcon() {
        return icon;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getOutcome() {
        return outcome;
    }

    public boolean isAvailable() {
        return outcome != null;
    }
}
