(function () {
    "use strict";

    var ACTION_FOCUS_KEY = "logixone.floorplan.action-focus";
    var CONTROL_FOCUS_KEY = "logixone.floorplan.control-focus";

    function remember(key, value) {
        try {
            window.sessionStorage.setItem(key, value);
        } catch (unavailableStorage) {
            // Focus recovery is progressive enhancement; submitting must still work.
        }
    }

    function consume(key) {
        try {
            var value = window.sessionStorage.getItem(key);
            window.sessionStorage.removeItem(key);
            return value;
        } catch (unavailableStorage) {
            return null;
        }
    }

    function focusElement(element) {
        if (!element) {
            return;
        }
        if (!element.hasAttribute("tabindex")) {
            element.setAttribute("tabindex", "-1");
        }
        element.focus();
    }

    function restoreFocus() {
        var controlId = consume(CONTROL_FOCUS_KEY);
        if (controlId) {
            var matchingControl = Array.from(document.querySelectorAll("[data-screen-input]"))
                .find(function (control) {
                    return control.getAttribute("data-screen-input") === controlId;
                });
            if (matchingControl && !matchingControl.disabled) {
                focusElement(matchingControl);
                return;
            }
        }

        if (consume(ACTION_FOCUS_KEY)) {
            focusElement(document.querySelector("[aria-invalid='true']")
                || document.querySelector(".screen-notice[role='alert']")
                || document.querySelector(".screen-notice[role='status']")
                || document.querySelector(".floorplan-page-header h1"));
        }
    }

    function transportInputs(form) {
        form.querySelectorAll("[data-floorplan-submitted-input]").forEach(function (input) {
            input.remove();
        });
        form.querySelectorAll("[data-screen-input]").forEach(function (control) {
            if (control.disabled) {
                return;
            }
            var submitted = document.createElement("input");
            submitted.type = "hidden";
            submitted.name = "floorplanInput." + control.getAttribute("data-screen-input");
            submitted.value = control.value;
            submitted.setAttribute("data-floorplan-submitted-input", "true");
            form.appendChild(submitted);
        });
    }

    function containingForm(source) {
        return source.tagName === "FORM" ? source : source.form;
    }

    function transport(source) {
        var form = containingForm(source);
        if (!form) {
            return false;
        }
        transportInputs(form);
        return true;
    }

    function refresh(control) {
        var form = containingForm(control);
        if (!form) {
            return false;
        }
        remember(CONTROL_FOCUS_KEY, control.getAttribute("data-screen-input") || "");
        form.elements.floorplanActionRequest.value = "false";
        form.elements.floorplanRequestedAction.value = "";
        transportInputs(form);
        var bridge = form.querySelector("[data-floorplan-context-bridge]");
        if (!bridge) {
            return false;
        }
        if (form.requestSubmit) {
            form.requestSubmit(bridge);
        } else {
            bridge.click();
        }
        return false;
    }

    function markActionSubmission() {
        remember(ACTION_FOCUS_KEY, "true");
    }

    document.addEventListener("DOMContentLoaded", restoreFocus);

    window.LogixoneFloorplan = Object.freeze({
        markActionSubmission: markActionSubmission,
        refresh: refresh,
        transport: transport
    });
}());
