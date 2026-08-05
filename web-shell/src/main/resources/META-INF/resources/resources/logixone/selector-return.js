(() => {
    "use strict";

    function captureInputs(link, selector, dataKey) {
        const form = link.closest("form");
        if (!form) {
            return;
        }
        const target = form.querySelector("input[name='selectorDraft']");
        if (!target) {
            return;
        }
        const draft = new URLSearchParams();
        form.querySelectorAll(selector).forEach((input) => {
            if (!input.disabled) {
                draft.append(input.dataset[dataKey], input.value || "");
            }
        });
        target.value = draft.toString();
    }

    window.LogixoneSelectorReturn = Object.freeze({
        capture(link) {
            captureInputs(link, "[data-screen-input]", "screenInput");
        },
        captureNative(link) {
            captureInputs(link, "[data-selector-draft]", "selectorDraft");
        }
    });

    document.addEventListener("click", (event) => {
        const target = event.target instanceof Element
            ? event.target.closest("[data-native-selector-return='true']")
            : null;
        if (target) {
            captureInputs(target, "[data-selector-draft]", "selectorDraft");
        }
    }, true);
})();
