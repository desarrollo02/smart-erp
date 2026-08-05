package py.com.logixone.plugin.api;

import java.util.Objects;

/** Typed and closed customization requests; none can relax a server-side rule. */
public sealed interface ScreenChange {

    record Label(ScreenElementId elementId, ScreenTextKey labelKey) implements ScreenChange {
        public Label {
            Objects.requireNonNull(elementId, "elementId");
            Objects.requireNonNull(labelKey, "labelKey");
        }
    }

    record Help(ScreenElementId elementId, ScreenTextKey helpKey) implements ScreenChange {
        public Help {
            Objects.requireNonNull(elementId, "elementId");
            Objects.requireNonNull(helpKey, "helpKey");
        }
    }

    record Hide(ScreenElementId elementId) implements ScreenChange {
        public Hide {
            Objects.requireNonNull(elementId, "elementId");
        }
    }

    record Disable(ScreenElementId elementId) implements ScreenChange {
        public Disable {
            Objects.requireNonNull(elementId, "elementId");
        }
    }

    record Require(ScreenElementId elementId) implements ScreenChange {
        public Require {
            Objects.requireNonNull(elementId, "elementId");
        }
    }

    record Move(ScreenElementId elementId, int position) implements ScreenChange {
        public Move {
            Objects.requireNonNull(elementId, "elementId");
            if (position < 0) {
                throw new IllegalArgumentException("Screen element position must be non-negative");
            }
        }
    }

    record SlotContent(ScreenSlotId slotId, ScreenFragmentId fragmentId, int position)
            implements ScreenChange {
        public SlotContent {
            Objects.requireNonNull(slotId, "slotId");
            Objects.requireNonNull(fragmentId, "fragmentId");
            if (position < 0) {
                throw new IllegalArgumentException("Screen slot content position must be non-negative");
            }
        }
    }
}
