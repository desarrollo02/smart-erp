package py.com.logixone.web.shell;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import py.com.logixone.plugin.api.ScreenElementId;
import py.com.logixone.plugin.api.ScreenInteraction;
import py.com.logixone.plugin.api.SelectorSourceDefinition;
import py.com.logixone.plugin.api.SelectorLoadingStrategy;
import py.com.logixone.kernel.application.company.screen.ComposedScreen;
import py.com.logixone.kernel.application.security.access.TrustedCompanyAccess;
import py.com.logixone.kernel.application.security.access.TrustedCompanyAccessStatus;
import py.com.logixone.kernel.application.security.access.TrustedNavigationView;
import py.com.logixone.web.security.TrustedWebAccess;
import py.com.logixone.web.security.TrustedWebAccessException;
import py.com.logixone.web.security.TrustedCompanySession;
import py.com.logixone.web.selector.NativeSelectorSourceCatalog;

/** Request-only model for the authenticated shell and its direct-route guard. */
@Named("shellView")
@RequestScoped
public class ShellViewBean {

    private static final Logger LOGGER = System.getLogger(ShellViewBean.class.getName());
    private static final int MAX_ROUTE_LENGTH = 160;
    private static final String SELECTOR_OPTION_PARAMETER_PREFIX = "selectorOption:";
    private static final String SELECTOR_SEARCH_PARAMETER_PREFIX = "selectorSearch:";
    private static final String SELECTOR_QUERY_PARAMETER_PREFIX = "selectorQuery:";
    private static final String SELECTOR_VALUE_PARAMETER_PREFIX = "selectorValue:";
    private static final String FLOORPLAN_INPUT_PARAMETER_PREFIX = "floorplanInput.";
    private static final Set<String> SCREEN_MODES = Set.of("directory", "create", "detail");

    @Inject
    TrustedWebAccess access;

    @Inject
    ShellTextCatalog textCatalog;

    @Inject
    ShellScreenRegistry screenRegistry;

    @Inject
    Instance<ScreenInteraction.Handler> interactionHandlers;

    @Inject
    HttpServletRequest request;

    @Inject
    TrustedCompanySession companySession;

    @Inject
    SelectorReturnContextStore selectorReturnContexts;

    @Inject
    NativeSelectorReturnContextStore nativeSelectorReturnContexts;

    @Inject
    NativeSelectorReturnViewBean nativeSelectorReturn;

    private ShellState state = ShellState.LOADING;
    private boolean prepared;
    private String selectedCompanyId;
    private String requestedRoute;
    private String requestedMode = "directory";
    private String requestedTab = "summary";
    private String actorDisplayName = "Sesión segura";
    private String companyDisplayName = "Empresa activa";
    private String activeTitle = "Función disponible";
    private String demonstrationValue;
    private String requestedActionId;
    private String selectedResourceId;
    private Long selectedResourceVersion;
    private String selectorContextId;
    private String selectorReturnId;
    private boolean selectorReturnAvailable;
    private boolean selectorReturnRestored;
    private String selectorReturnLabel = "Volver al formulario anterior";
    private Map<String, String> inputValues = new HashMap<>();
    private Map<String, String> selectorSearchValues = new HashMap<>();
    private Map<String, Integer> selectorSearchOffsets = new HashMap<>();
    private Map<String, ShellSelectorOptionPageView> selectorOptionPages = new HashMap<>();
    private boolean selectorSelectionProcessed;
    private boolean selectorSearchProcessed;
    private int tableOffset;
    private int tablePageSize = 50;
    private int requestedTablePageDirection;
    private List<ShellCompanyOptionView> companyOptions = List.of();
    private List<ShellMenuItemView> menuItems = List.of();
    private ShellScreenView activeScreen;
    private ShellScreenInteractionView activeInteraction = ShellScreenInteractionView.empty();
    private ScreenInteraction.Handler activeInteractionHandler;
    private Map<ScreenElementId, SelectorSourceDefinition> activeSelectorSources = Map.of();
    private Set<ScreenElementId> authorizedSelectorManagement = Set.of();

    @PostConstruct
    void initialize() {
        String routeParameter = request.getParameter("route");
        if (routeParameter != null && !routeParameter.isBlank()) {
            requestedRoute = routeParameter;
        }
        requestedMode = normalized(request.getParameter("mode"), SCREEN_MODES, "directory");
        requestedTab = normalizedTab(request.getParameter("tab"));
        String resourceParameter = request.getParameter("resource");
        String versionParameter = request.getParameter("version");
        selectorContextId = normalizedToken(request.getParameter("selectorContext"));
        selectorReturnId = normalizedToken(request.getParameter("selectorReturn"));
        if (isDetailMode() && resourceParameter != null && !resourceParameter.isBlank()) {
            selectedResourceId = resourceParameter;
            if (versionParameter != null && !versionParameter.isBlank()) {
                try {
                    long parsedVersion = Long.parseLong(versionParameter);
                    if (parsedVersion >= 0) {
                        selectedResourceVersion = parsedVersion;
                    }
                } catch (NumberFormatException ignored) {
                    // An invalid browser value never becomes an operation context.
                }
            }
        }
        prepare();
    }

    public void prepare() {
        if (prepared) {
            return;
        }
        prepared = true;
        try {
            TrustedCompanyAccess companyAccess = access.current();
            if (companyAccess.status() == TrustedCompanyAccessStatus.SELECTION_REQUIRED) {
                List<ShellCompanyOptionView> options = new ArrayList<>();
                for (int index = 0; index < companyAccess.availableCompanyIds().size(); index++) {
                    options.add(new ShellCompanyOptionView(
                            companyAccess.availableCompanyIds().get(index).toString(),
                            "Empresa autorizada " + (index + 1)));
                }
                companyOptions = List.copyOf(options);
                restoreNativeCompanySelection();
                state = ShellState.SELECTION_REQUIRED;
                return;
            }

            TrustedNavigationView navigation = access.navigation();
            actorDisplayName = navigation.actorDisplayName();
            companyOptions = navigation.companies().stream()
                    .map(option -> new ShellCompanyOptionView(
                            option.companyId().toString(), option.presentationLabel()))
                    .toList();
            selectedCompanyId = navigation.context().companyId().toString();
            restoreNativeCompanySelection();
            companyDisplayName = companyOptions.stream()
                    .filter(option -> option.getId().equals(selectedCompanyId))
                    .map(ShellCompanyOptionView::getLabel)
                    .findFirst()
                    .orElse("Empresa activa");
            menuItems = navigation.menuItems().stream()
                    .map(item -> new ShellMenuItemView(
                            item.pluginId(),
                            item.menuId(),
                            textCatalog.menuLabel(item.labelKey()),
                            item.route(),
                            item.requiredPermission()))
                    .toList();

            if (requestedRoute != null) {
                authorizeRequestedRoute();
            } else {
                state = ShellState.READY;
            }
        } catch (TrustedWebAccessException denied) {
            state = ShellState.DENIED;
        } catch (RuntimeException unexpected) {
            LOGGER.log(Level.ERROR,
                    "event=shell_view_failed type={0}",
                    unexpected.getClass().getName());
            state = ShellState.ERROR;
        }
    }

    private void restoreNativeCompanySelection() {
        nativeSelectorReturn.restore("/app/index.xhtml").ifPresent(restoration -> {
            if (!restoration.usageId().equals(NativeSelectorSourceCatalog.APP_COMPANY_SWITCHER)
                    && !restoration.usageId().equals(
                            NativeSelectorSourceCatalog.APP_COMPANY_SELECTION)) {
                return;
            }
            String restoredCompany = restoration.inputs().get("selected_company_id");
            if (restoredCompany != null && companyOptions.stream()
                    .anyMatch(option -> option.getId().equals(restoredCompany))) {
                selectedCompanyId = restoredCompany;
            }
        });
    }

    private void authorizeRequestedRoute() {
        if (requestedRoute.isBlank()
                || requestedRoute.length() > MAX_ROUTE_LENGTH
                || !requestedRoute.startsWith("/")
                || requestedRoute.startsWith("//")) {
            state = ShellState.DENIED;
            return;
        }
        ShellMenuItemView active = menuItems.stream()
                .filter(item -> item.getRoute().equals(requestedRoute))
                .findFirst()
                .orElse(null);
        if (active == null) {
            state = ShellState.DENIED;
            return;
        }
        if (active.requiredPermission().isEmpty()) {
            state = ShellState.DENIED;
            return;
        }
        var screenId = screenRegistry.screenFor(active.pluginId(), active.getRoute())
                .orElse(null);
        if (screenId == null) {
            state = ShellState.DENIED;
            return;
        }
        ComposedScreen composed = access.requireScreen(
                screenId,
                active.pluginId(),
                active.requiredPermission().orElseThrow());
        activeScreen = screenRegistry.render(composed, textCatalog).orElse(null);
        if (activeScreen == null) {
            state = ShellState.DENIED;
            return;
        }
        activeTitle = activeScreen.getTitle();
        if (!activeScreen.acceptsDetailTab(requestedTab)) {
            requestedTab = "summary";
        }
        if (activeScreen.isInteractive() && !loadInteraction(Optional.empty())) {
            state = ShellState.DENIED;
            return;
        }
        if (isDetailMode() && !activeInteraction.isHasDetail()) {
            requestedMode = "directory";
            requestedTab = "summary";
        }
        restoreSelectorReturn();
        prepareSelectorReturnLink();
        state = ShellState.READY;
    }

    private boolean loadInteraction(Optional<ScreenElementId> actionId) {
        List<ScreenInteraction.Handler> matches = interactionHandlers.stream()
                .filter(handler -> handler.screenId().equals(
                        new py.com.logixone.plugin.api.ScreenId(
                                activeScreenIdPlugin(), activeScreenIdLocal())))
                .toList();
        if (matches.size() != 1) {
            return false;
        }
        activeInteractionHandler = matches.getFirst();
        if (!loadSelectorSources()) {
            return false;
        }
        ScreenInteraction.Result result;
        try {
            result = activeInteractionHandler.interact(new ScreenInteraction.Request(
                    actionId,
                    typedInputs(),
                    Optional.ofNullable(selectedResourceId),
                    Optional.ofNullable(selectedResourceVersion)));
        } catch (RuntimeException failure) {
            LOGGER.log(Level.ERROR,
                    "event=shell_interaction_failed stage=handler type={0}",
                    failure.getClass().getName());
            throw failure;
        }
        try {
            applyInteraction(result);
        } catch (RuntimeException failure) {
            LOGGER.log(Level.ERROR,
                    "event=shell_interaction_failed stage=render_copy type={0}",
                    failure.getClass().getName());
            throw failure;
        }
        return true;
    }

    private py.com.logixone.plugin.api.PluginId activeScreenIdPlugin() {
        String value = activeScreen.getId();
        return new py.com.logixone.plugin.api.PluginId(value.substring(0, value.indexOf(':')));
    }

    private String activeScreenIdLocal() {
        String value = activeScreen.getId();
        return value.substring(value.indexOf(':') + 1);
    }

    private Map<ScreenElementId, String> typedInputs() {
        return typedInputs(inputValues);
    }

    private static Map<ScreenElementId, String> typedInputs(Map<String, String> values) {
        Map<ScreenElementId, String> typed = new HashMap<>();
        values.forEach((key, value) -> typed.put(new ScreenElementId(key), value == null ? "" : value));
        return Map.copyOf(typed);
    }

    private void applyInteraction(ScreenInteraction.Result result) {
        if (!activeScreen.acceptsDynamicStateIds(result.elementStates().keySet())) {
            throw new IllegalArgumentException(
                    "Dynamic state references an element outside the active screen");
        }
        inputValues = new HashMap<>();
        result.inputs().forEach((key, value) -> inputValues.put(key.value(), value));
        selectedResourceId = result.selectedResourceId().orElse(null);
        selectedResourceVersion = result.selectedResourceVersion().orElse(null);
        activeInteraction = ShellScreenInteractionView.from(
                result, activeSelectorSources, authorizedSelectorManagement);
        if (activeInteraction.isHasTable() && activeInteraction.getTable().isPaged()) {
            tableOffset = activeInteraction.getTable().getOffset();
            tablePageSize = activeInteraction.getTable().getPageSize();
        } else {
            tableOffset = 0;
            tablePageSize = 50;
        }
    }

    public String changeTablePage() {
        Map<String, String> submittedInputs = mergeFloorplanSubmittedInputs(
                inputValues, request.getParameterMap());
        String submittedResourceId = selectedResourceId;
        Long submittedResourceVersion = selectedResourceVersion;
        int submittedOffset = tableOffset;
        int submittedPageSize = tablePageSize;
        int direction = requestedTablePageDirection;
        try {
            prepared = false;
            prepare();
            mergeSubmittedSelectorValues(submittedInputs);
            inputValues.putAll(submittedInputs);
            selectedResourceId = submittedResourceId;
            selectedResourceVersion = submittedResourceVersion;
            if (state != ShellState.READY
                    || activeInteractionHandler == null
                    || !activeInteraction.isHasTable()
                    || !activeInteraction.getTable().isPaged()
                    || (direction != -1 && direction != 1)) {
                state = ShellState.DENIED;
                return null;
            }
            int targetOffset = direction < 0
                    ? Math.max(0, submittedOffset - submittedPageSize)
                    : submittedOffset + submittedPageSize;
            ScreenInteraction.Result result = activeInteractionHandler.interact(
                    new ScreenInteraction.Request(
                            Optional.empty(),
                            typedInputs(),
                            Optional.ofNullable(selectedResourceId),
                            Optional.ofNullable(selectedResourceVersion),
                            Optional.of(new ScreenInteraction.TablePageRequest(
                                    targetOffset, submittedPageSize))));
            applyInteraction(result);
            return null;
        } catch (TrustedWebAccessException | IllegalArgumentException denied) {
            state = ShellState.DENIED;
            return null;
        } catch (RuntimeException unexpected) {
            LOGGER.log(Level.ERROR,
                    "event=shell_table_page_failed type={0}",
                    unexpected.getClass().getName());
            state = ShellState.ERROR;
            return null;
        }
    }

    public void searchRequestedSelectorOptions() {
        if (selectorSearchProcessed) {
            return;
        }
        selectorSearchProcessed = true;
        var searches = request.getParameterMap().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(SELECTOR_SEARCH_PARAMETER_PREFIX))
                .toList();
        if (searches.isEmpty()) {
            return;
        }
        if (searches.size() != 1) {
            state = ShellState.DENIED;
            return;
        }
        String parameterName = searches.getFirst().getKey();
        String fieldId = parameterName.substring(SELECTOR_SEARCH_PARAMETER_PREFIX.length());
        int direction;
        try {
            direction = Integer.parseInt(request.getParameter(parameterName));
        } catch (NumberFormatException invalid) {
            state = ShellState.DENIED;
            return;
        }
        Map<String, String> submittedInputs = mergeFloorplanSubmittedInputs(
                inputValues, request.getParameterMap());
        Map<String, String> submittedSearches = new HashMap<>(selectorSearchValues);
        Map<String, Integer> submittedOffsets = new HashMap<>(selectorSearchOffsets);
        String submittedQuery = request.getParameter(SELECTOR_QUERY_PARAMETER_PREFIX + fieldId);
        if (submittedQuery == null || submittedQuery.length() > 100) {
            state = ShellState.DENIED;
            return;
        }
        submittedSearches.put(fieldId, submittedQuery);
        try {
            prepared = false;
            prepare();
            mergeSubmittedSelectorValues(submittedInputs);
            inputValues.putAll(submittedInputs);
            prepared = false;
            prepare();
            selectorSearchValues.putAll(submittedSearches);
            selectorSearchOffsets.putAll(submittedOffsets);
            ScreenElementId elementId = new ScreenElementId(fieldId);
            SelectorSourceDefinition source = activeSelectorSources.get(elementId);
            if (state != ShellState.READY
                    || activeInteractionHandler == null
                    || source == null
                    || source.loadingStrategy() != SelectorLoadingStrategy.SEARCH_ON_DEMAND
                    || (direction < -1 || direction > 1)) {
                state = ShellState.DENIED;
                return;
            }
            int currentOffset = selectorSearchOffsets.getOrDefault(fieldId, 0);
            int offset = switch (direction) {
                case -1 -> Math.max(0, currentOffset - 50);
                case 1 -> currentOffset + 50;
                default -> 0;
            };
            ScreenInteraction.SelectorOptionPage page = activeInteractionHandler.searchOptions(
                    new ScreenInteraction.SelectorOptionRequest(
                            elementId,
                            selectorSearchValues.getOrDefault(fieldId, ""),
                            offset,
                            50));
            LOGGER.log(Level.INFO,
                    "event=shell_selector_search_succeeded field_id={0} query_length={1} "
                            + "total={2} offset={3}",
                    fieldId,
                    selectorSearchValues.getOrDefault(fieldId, "").length(),
                    page.total(),
                    page.offset());
            selectorSearchOffsets.put(fieldId, page.offset());
            selectorOptionPages.put(fieldId, new ShellSelectorOptionPageView(page));
            return;
        } catch (TrustedWebAccessException | IllegalArgumentException denied) {
            state = ShellState.DENIED;
        } catch (RuntimeException unexpected) {
            LOGGER.log(Level.ERROR,
                    "event=shell_selector_search_failed type={0}",
                    unexpected.getClass().getName());
            state = ShellState.ERROR;
        }
    }

    public boolean isSelectorInteractionRequested() {
        return request.getParameterMap().keySet().stream().anyMatch(parameter ->
                parameter.startsWith(SELECTOR_SEARCH_PARAMETER_PREFIX)
                        || parameter.startsWith(SELECTOR_OPTION_PARAMETER_PREFIX));
    }

    public void selectRequestedSelectorOption() {
        if (selectorSelectionProcessed) {
            return;
        }
        selectorSelectionProcessed = true;
        var selections = request.getParameterMap().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(
                        SELECTOR_OPTION_PARAMETER_PREFIX))
                .toList();
        if (selections.isEmpty()) {
            return;
        }
        Map<String, String> submittedInputs = mergeFloorplanSubmittedInputs(
                inputValues, request.getParameterMap());
        Map<String, String> submittedSearches = new HashMap<>(selectorSearchValues);
        Map<String, Integer> submittedOffsets = new HashMap<>(selectorSearchOffsets);
        try {
            prepared = false;
            prepare();
            mergeSubmittedSelectorValues(submittedInputs);
            inputValues.putAll(submittedInputs);
            selectorSearchValues.putAll(submittedSearches);
            selectorSearchOffsets.putAll(submittedOffsets);
            if (selections.size() != 1 || selections.getFirst().getValue().length != 1) {
                throw new IllegalArgumentException("Exactly one selector option is required");
            }
            String fieldId = selections.getFirst().getKey().substring(
                    SELECTOR_OPTION_PARAMETER_PREFIX.length());
            String optionValue = selections.getFirst().getValue()[0];
            ScreenElementId elementId = new ScreenElementId(fieldId);
            SelectorSourceDefinition source = activeSelectorSources.get(elementId);
            String selected = new ScreenInteraction.Option(
                    optionValue, optionValue).value();
            if (state != ShellState.READY
                    || activeInteractionHandler == null
                    || source == null
                    || source.loadingStrategy() != SelectorLoadingStrategy.SEARCH_ON_DEMAND) {
                state = ShellState.DENIED;
                return;
            }
            ScreenInteraction.SelectorOptionPage verification =
                    activeInteractionHandler.searchOptions(
                            new ScreenInteraction.SelectorOptionRequest(
                                    elementId, selected, 0, 50));
            if (verification.options().stream()
                    .noneMatch(option -> option.value().equals(selected))) {
                state = ShellState.DENIED;
                return;
            }
            inputValues.put(fieldId, selected);
            selectorOptionPages.remove(fieldId);
            prepared = false;
            prepare();
        } catch (TrustedWebAccessException | IllegalArgumentException denied) {
            state = ShellState.DENIED;
        } catch (RuntimeException unexpected) {
            LOGGER.log(Level.ERROR,
                    "event=shell_selector_selection_failed type={0}",
                    unexpected.getClass().getName());
            state = ShellState.ERROR;
        }
    }

    private void mergeSubmittedSelectorValues(Map<String, String> submittedInputs) {
        Map<ScreenElementId, String> submittedSelectors = new HashMap<>();
        request.getParameterMap().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(SELECTOR_VALUE_PARAMETER_PREFIX))
                .forEach(entry -> {
                    if (entry.getValue().length != 1) {
                        throw new IllegalArgumentException(
                                "Exactly one selector value is required");
                    }
                    String fieldId = entry.getKey().substring(
                            SELECTOR_VALUE_PARAMETER_PREFIX.length());
                    ScreenElementId elementId = new ScreenElementId(fieldId);
                    if (!activeSelectorSources.containsKey(elementId)) {
                        throw new IllegalArgumentException("Unsupported selector value");
                    }
                    String value = entry.getValue()[0] == null
                            ? "" : entry.getValue()[0].strip();
                    submittedSelectors.put(elementId, value);
                });

        submittedSelectors.forEach((elementId, value) -> {
            SelectorSourceDefinition source = activeSelectorSources.get(elementId);
            if (source.loadingStrategy() != SelectorLoadingStrategy.SEARCH_ON_DEMAND) {
                return;
            }
            validateSearchSelectorValue(elementId, value);
            submittedInputs.put(elementId.value(), value);
        });

        ScreenInteraction.Result contextual = activeInteractionHandler.interact(
                new ScreenInteraction.Request(
                        Optional.empty(), typedInputs(submittedInputs),
                        Optional.ofNullable(selectedResourceId),
                        Optional.ofNullable(selectedResourceVersion)));
        submittedSelectors.forEach((elementId, value) -> {
            SelectorSourceDefinition source = activeSelectorSources.get(elementId);
            if (source.loadingStrategy() == SelectorLoadingStrategy.SEARCH_ON_DEMAND) {
                return;
            }
            if (!value.isEmpty() && contextual.options()
                    .getOrDefault(elementId, List.of()).stream()
                    .noneMatch(option -> option.value().equals(value))) {
                throw new IllegalArgumentException("Invalid selector value");
            }
            submittedInputs.put(elementId.value(), value);
        });
    }

    private void validateSearchSelectorValue(ScreenElementId elementId, String value) {
        if (value.isEmpty()) {
            return;
        }
        String canonical = new ScreenInteraction.Option(value, value).value();
        ScreenInteraction.SelectorOptionPage verification =
                activeInteractionHandler.searchOptions(
                        new ScreenInteraction.SelectorOptionRequest(
                                elementId, canonical, 0, 50));
        if (verification.options().stream()
                .noneMatch(option -> option.value().equals(canonical))) {
            throw new IllegalArgumentException("Invalid selector value");
        }
    }

    private boolean loadSelectorSources() {
        Map<ScreenElementId, SelectorSourceDefinition> declared;
        try {
            declared = Map.copyOf(activeInteractionHandler.selectorSources());
        } catch (RuntimeException invalidContract) {
            LOGGER.log(Level.ERROR,
                    "event=shell_selector_sources_invalid type={0}",
                    invalidContract.getClass().getName());
            return false;
        }
        if (declared.keySet().stream()
                .anyMatch(elementId -> !activeScreen.acceptsSelector(elementId.value()))) {
            return false;
        }
        activeSelectorSources = declared;

        Map<String, Boolean> authorizationCache = new HashMap<>();
        var authorized = new java.util.HashSet<ScreenElementId>();
        declared.forEach((elementId, source) -> {
            if (source.manageable() && authorizedManagement(source, authorizationCache)) {
                authorized.add(elementId);
            }
        });
        authorizedSelectorManagement = Set.copyOf(authorized);
        return true;
    }

    private boolean authorizedManagement(
            SelectorSourceDefinition source, Map<String, Boolean> authorizationCache) {
        String route = source.managementRoute().orElseThrow();
        boolean navigable = menuItems.stream().anyMatch(item -> item.getRoute().equals(route));
        if (!navigable) {
            return false;
        }
        String authorizationKey = source.ownerPluginId().value()
                + ":" + source.managementPermission().orElseThrow().value();
        return authorizationCache.computeIfAbsent(authorizationKey, ignored -> {
            try {
                access.requireAuthorization(
                        source.ownerPluginId(), source.managementPermission().orElseThrow());
                return true;
            } catch (TrustedWebAccessException denied) {
                return false;
            }
        });
    }

    public String openSelectorManagement(String fieldId) {
        try {
            if (state != ShellState.READY
                    || activeScreen == null
                    || !activeScreen.isInteractive()
                    || activeInteractionHandler == null) {
                state = ShellState.DENIED;
                return null;
            }
            ScreenElementId elementId = new ScreenElementId(fieldId);
            SelectorSourceDefinition source = activeSelectorSources.get(elementId);
            if (source == null
                    || !authorizedSelectorManagement.contains(elementId)
                    || !authorizedManagement(source, new HashMap<>())) {
                state = ShellState.DENIED;
                return null;
            }

            Set<String> allowedInputIds = activeScreen.safeDraftInputIds();
            Map<String, String> submittedValues = new HashMap<>(inputValues);
            submittedValues.putAll(SelectorReturnDraft.decode(
                    request.getParameter("selectorDraft")));
            Map<String, String> submittedDraft = SelectorReturnDraft.retain(
                    submittedValues, allowedInputIds);
            ScreenInteraction.Result normalized = activeInteractionHandler.interact(
                    new ScreenInteraction.Request(
                            Optional.empty(),
                            typedInputs(submittedDraft),
                            Optional.ofNullable(selectedResourceId),
                            Optional.ofNullable(selectedResourceVersion)));
            Map<String, String> normalizedInputs = new HashMap<>();
            normalized.inputs().forEach((key, value) -> normalizedInputs.put(key.value(), value));
            Map<String, String> safeDraft = SelectorReturnDraft.retain(
                    normalizedInputs, allowedInputIds);

            var binding = companySession.reference()
                    .orElseThrow(TrustedWebAccessException::forbidden);
            String targetRoute = source.managementRoute().orElseThrow();
            String token = selectorReturnContexts.remember(new SelectorReturnContext(
                    binding.userId().toString(),
                    binding.companyId().toString(),
                    companySession.revision(),
                    requestedRoute,
                    activeTitle,
                    targetRoute,
                    requestedMode,
                    requestedTab,
                    selectedResourceId,
                    selectedResourceVersion,
                    safeDraft));
            return viewOutcome(targetRoute, "directory", "summary", null, null,
                    "selectorContext", token);
        } catch (TrustedWebAccessException | IllegalArgumentException denied) {
            state = ShellState.DENIED;
            return null;
        } catch (RuntimeException unexpected) {
            LOGGER.log(Level.ERROR,
                    "event=shell_selector_management_open_failed type={0}",
                    unexpected.getClass().getName());
            state = ShellState.ERROR;
            return null;
        }
    }

    public String returnToSelectorOrigin() {
        try {
            if (state != ShellState.READY || !selectorReturnAvailable) {
                state = ShellState.DENIED;
                return null;
            }
            var binding = companySession.reference()
                    .orElseThrow(TrustedWebAccessException::forbidden);
            SelectorReturnContext context = selectorReturnContexts.findForTarget(
                            selectorContextId,
                            binding.userId().toString(),
                            binding.companyId().toString(),
                            companySession.revision(),
                            requestedRoute)
                    .orElseThrow(TrustedWebAccessException::forbidden);
            return viewOutcome(
                    context.originRoute(),
                    context.mode(),
                    context.tab(),
                    context.resourceId(),
                    context.resourceVersion(),
                    "selectorReturn",
                    selectorContextId);
        } catch (TrustedWebAccessException | IllegalArgumentException denied) {
            state = ShellState.DENIED;
            return null;
        }
    }

    private void restoreSelectorReturn() {
        if (selectorReturnId == null || activeInteractionHandler == null) {
            return;
        }
        var binding = companySession.reference().orElse(null);
        if (binding == null) {
            return;
        }
        selectorReturnContexts.consumeForOrigin(
                        selectorReturnId,
                        binding.userId().toString(),
                        binding.companyId().toString(),
                        companySession.revision(),
                        requestedRoute)
                .ifPresent(context -> {
                    requestedMode = context.mode();
                    requestedTab = activeScreen.acceptsDetailTab(context.tab())
                            ? context.tab()
                            : "summary";
                    selectedResourceId = context.resourceId();
                    selectedResourceVersion = context.resourceVersion();
                    inputValues = new HashMap<>(SelectorReturnDraft.retain(
                            context.inputs(), activeScreen.safeDraftInputIds()));
                    ScreenInteraction.Result refreshed = activeInteractionHandler.interact(
                            new ScreenInteraction.Request(
                                    Optional.empty(),
                                    typedInputs(),
                                    Optional.ofNullable(selectedResourceId),
                                    Optional.ofNullable(selectedResourceVersion)));
                    applyInteraction(refreshed);
                    if (isDetailMode() && !activeInteraction.isHasDetail()) {
                        requestedMode = "directory";
                        requestedTab = "summary";
                    }
                    selectorReturnRestored = true;
                    selectorReturnId = null;
                });
    }

    private void prepareSelectorReturnLink() {
        selectorReturnAvailable = false;
        if (selectorContextId == null) {
            return;
        }
        var binding = companySession.reference().orElse(null);
        if (binding == null) {
            return;
        }
        selectorReturnContexts.findForTarget(
                        selectorContextId,
                        binding.userId().toString(),
                        binding.companyId().toString(),
                        companySession.revision(),
                        requestedRoute)
                .ifPresent(context -> {
                    selectorReturnAvailable = true;
                    selectorReturnLabel = "Volver a " + context.originTitle();
                });
    }

    private static String viewOutcome(
            String route,
            String mode,
            String tab,
            String resourceId,
            Long resourceVersion,
            String contextParameter,
            String contextToken) {
        StringBuilder outcome = new StringBuilder("/app/view.xhtml?faces-redirect=true")
                .append("&route=").append(query(route))
                .append("&mode=").append(query(mode))
                .append("&tab=").append(query(tab));
        if (resourceId != null && resourceVersion != null) {
            outcome.append("&resource=").append(query(resourceId))
                    .append("&version=").append(resourceVersion);
        }
        return outcome.append('&').append(contextParameter).append('=')
                .append(query(contextToken)).toString();
    }

    private static String query(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public String executeScreenAction() {
        String floorplanAction = request.getParameter("floorplanRequestedAction");
        if (floorplanAction != null) {
            requestedActionId = normalizedAction(floorplanAction);
        }
        Map<String, String> submittedInputs = mergeFloorplanSubmittedInputs(
                inputValues, request.getParameterMap());
        String submittedResourceId = selectedResourceId;
        Long submittedResourceVersion = selectedResourceVersion;
        try {
            prepared = false;
            prepare();
            mergeSubmittedSelectorValues(submittedInputs);
            inputValues.putAll(submittedInputs);
            selectedResourceId = submittedResourceId;
            selectedResourceVersion = submittedResourceVersion;
            if (state != ShellState.READY
                    || activeScreen == null
                    || !activeScreen.isInteractive()
                    || requestedActionId == null
                    || !activeScreen.acceptsAction(requestedActionId)
                    || activeInteractionHandler == null) {
                state = ShellState.DENIED;
                return null;
            }

            ScreenInteraction.Result refreshed = activeInteractionHandler.interact(
                    new ScreenInteraction.Request(
                            Optional.empty(),
                            typedInputs(),
                            Optional.ofNullable(selectedResourceId),
                            Optional.ofNullable(selectedResourceVersion)));
            applyInteraction(refreshed);
            if (!activeInteraction.acceptsAction(requestedActionId)) {
                state = ShellState.DENIED;
                return null;
            }

            Map<String, String> actionInputs = mergeFloorplanActionInputs(
                    inputValues, submittedInputs);
            ScreenElementId actionId = new ScreenElementId(requestedActionId);
            ScreenInteraction.Result result = activeInteractionHandler.interact(new ScreenInteraction.Request(
                    Optional.of(actionId),
                    typedInputs(actionInputs),
                    Optional.ofNullable(selectedResourceId),
                    Optional.ofNullable(selectedResourceVersion)));
            applyInteraction(result);
            boolean successful = result.notices().stream()
                    .anyMatch(notice -> notice.level() == ScreenInteraction.NoticeLevel.SUCCESS);
            if (activeScreen.isCreateAction(actionId.value()) && successful && selectedResourceId != null) {
                requestedMode = "detail";
                requestedTab = "summary";
            } else if (activeScreen.isSearchAction(actionId.value())) {
                requestedMode = "directory";
                requestedTab = "summary";
            }
            return null;
        } catch (TrustedWebAccessException | IllegalArgumentException denied) {
            state = ShellState.DENIED;
            return null;
        } catch (RuntimeException unexpected) {
            LOGGER.log(Level.ERROR,
                    "event=shell_screen_action_failed type={0}",
                    unexpected.getClass().getName());
            state = ShellState.ERROR;
            return null;
        }
    }

    public String refreshFloorplanContext() {
        Map<String, String> submittedInputs = mergeFloorplanSubmittedInputs(
                inputValues, request.getParameterMap());
        String submittedResourceId = selectedResourceId;
        Long submittedResourceVersion = selectedResourceVersion;
        try {
            prepared = false;
            prepare();
            mergeSubmittedSelectorValues(submittedInputs);
            inputValues.putAll(submittedInputs);
            selectedResourceId = submittedResourceId;
            selectedResourceVersion = submittedResourceVersion;
            if (state != ShellState.READY
                    || activeScreen == null
                    || !activeScreen.isInteractive()
                    || activeInteractionHandler == null) {
                state = ShellState.DENIED;
                return null;
            }
            ScreenInteraction.Result refreshed = activeInteractionHandler.interact(
                    new ScreenInteraction.Request(
                            Optional.empty(),
                            typedInputs(),
                            Optional.ofNullable(selectedResourceId),
                            Optional.ofNullable(selectedResourceVersion)));
            applyInteraction(refreshed);
            return null;
        } catch (TrustedWebAccessException | IllegalArgumentException denied) {
            state = ShellState.DENIED;
            return null;
        } catch (RuntimeException unexpected) {
            LOGGER.log(Level.ERROR,
                    "event=shell_floorplan_context_refresh_failed type={0}",
                    unexpected.getClass().getName());
            state = ShellState.ERROR;
            return null;
        }
    }

    public boolean isFloorplanActionRequest() {
        return "true".equals(request.getParameter("floorplanActionRequest"));
    }

    static Map<String, String> mergeFloorplanSubmittedInputs(
            Map<String, String> currentInputs,
            Map<String, String[]> requestParameters) {
        Map<String, String> submittedInputs = new HashMap<>(currentInputs);
        requestParameters.forEach((parameterName, values) -> {
            if (!parameterName.startsWith(FLOORPLAN_INPUT_PARAMETER_PREFIX)) {
                return;
            }
            if (values == null || values.length != 1 || values[0] == null) {
                throw new IllegalArgumentException("invalid floorplan input transport");
            }
            String rawElementId = parameterName.substring(FLOORPLAN_INPUT_PARAMETER_PREFIX.length());
            ScreenElementId elementId = new ScreenElementId(rawElementId);
            submittedInputs.put(elementId.value(), values[0]);
        });
        return submittedInputs;
    }

    static Map<String, String> mergeFloorplanActionInputs(
            Map<String, String> refreshedInputs,
            Map<String, String> submittedInputs) {
        Map<String, String> actionInputs = new HashMap<>(refreshedInputs);
        actionInputs.putAll(submittedInputs);
        return actionInputs;
    }

    public String selectCompany() {
        try {
            selectorReturnContexts.clear();
            nativeSelectorReturnContexts.clear();
            access.selectCompany(selectedCompanyId);
            return "/app/index.xhtml?faces-redirect=true";
        } catch (TrustedWebAccessException denied) {
            state = ShellState.DENIED;
            return null;
        }
    }

    public void logout() throws IOException {
        selectorReturnContexts.clear();
        nativeSelectorReturnContexts.clear();
        access.clear();
        ExternalContext external = FacesContext.getCurrentInstance().getExternalContext();
        external.redirect(external.getRequestContextPath() + "/app/logout");
        FacesContext.getCurrentInstance().responseComplete();
    }

    public boolean isLoading() {
        return state == ShellState.LOADING;
    }

    public boolean isSelectionRequired() {
        return state == ShellState.SELECTION_REQUIRED;
    }

    public boolean isReady() {
        return state == ShellState.READY;
    }

    public boolean isDenied() {
        return state == ShellState.DENIED;
    }

    public boolean isError() {
        return state == ShellState.ERROR;
    }

    public boolean isMultiCompany() {
        return companyOptions.size() > 1;
    }

    public String getSelectedCompanyId() {
        return selectedCompanyId;
    }

    public void setSelectedCompanyId(String selectedCompanyId) {
        this.selectedCompanyId = selectedCompanyId;
    }

    public String getRequestedRoute() {
        return requestedRoute;
    }

    public void setRequestedRoute(String requestedRoute) {
        this.requestedRoute = requestedRoute;
        prepared = false;
    }

    public String getRequestedMode() {
        return requestedMode;
    }

    public void setRequestedMode(String requestedMode) {
        this.requestedMode = normalized(requestedMode, SCREEN_MODES, "directory");
    }

    public String getRequestedTab() {
        return requestedTab;
    }

    public void setRequestedTab(String requestedTab) {
        this.requestedTab = normalizedTab(requestedTab);
    }

    public boolean isDirectoryMode() {
        return "directory".equals(requestedMode);
    }

    public boolean isCreateMode() {
        return "create".equals(requestedMode);
    }

    public boolean isDetailMode() {
        return "detail".equals(requestedMode);
    }

    public boolean isFloorplanElementVisible(String elementId) {
        ShellScreenInteractionView.ElementStateView state =
                activeInteraction.getElementStates().get(elementId);
        return state == null || state.isVisible();
    }

    public boolean isFloorplanRegionVisible(ShellScreenRegionView region) {
        if (region == null) {
            return false;
        }
        boolean hasVisibleField = region.getFields().stream()
                .anyMatch(field -> isFloorplanFieldVisible(region, field));
        boolean hasVisibleTable = region.getTables().stream()
                .anyMatch(table -> isFloorplanTableVisible(region, table));
        boolean hasVisibleAction = region.getActions().stream()
                .anyMatch(action -> isFloorplanActionVisible(region, action));
        return hasVisibleField || hasVisibleTable || hasVisibleAction;
    }

    public boolean isFloorplanFieldVisible(
            ShellScreenRegionView region,
            ShellScreenElementView field) {
        if (region == null || field == null || !isFloorplanElementVisible(field.getId())) {
            return false;
        }
        if (!activeScreen.isFloorplanSeparatedByMode()) {
            return true;
        }
        return isDirectoryMode() ? region.isFilterRegion() : !region.isFilterRegion();
    }

    public boolean isFloorplanTableVisible(
            ShellScreenRegionView region,
            ShellScreenElementView table) {
        if (region == null || table == null
                || !isFloorplanElementVisible(table.getId())
                || !activeInteraction.isHasTable()
                || !activeInteraction.getTable().getElementId().equals(table.getId())) {
            return false;
        }
        if (!activeScreen.isFloorplanSeparatedByMode()) {
            return true;
        }
        return isDirectoryMode() != table.isEditableLines();
    }

    public boolean isFloorplanActionVisible(
            ShellScreenRegionView region,
            ShellScreenElementView action) {
        if (region == null || action == null
                || !isFloorplanElementVisible(action.getId())
                || (activeScreen.isHasFloorplanRowAction() && action.isNavigateIntent())) {
            return false;
        }
        if (!activeScreen.isFloorplanSeparatedByMode()) {
            return true;
        }
        return isDirectoryMode() ? action.isSearchIntent() : !action.isSearchIntent();
    }

    public boolean isSummaryTab() {
        return "summary".equals(requestedTab);
    }

    public boolean isGeneralTab() {
        return "general".equals(requestedTab);
    }

    public boolean isIdentificationsTab() {
        return "identifications".equals(requestedTab);
    }

    public boolean isAddressesTab() {
        return "addresses".equals(requestedTab);
    }

    public boolean isContactsTab() {
        return "contacts".equals(requestedTab);
    }

    public boolean isRolesTab() {
        return "roles".equals(requestedTab);
    }

    public boolean isDetailTabActive(String tabId) {
        return requestedTab.equals(tabId);
    }

    public String getActorDisplayName() {
        return actorDisplayName;
    }

    public String getCompanyDisplayName() {
        return companyDisplayName;
    }

    public String getActiveTitle() {
        return activeTitle;
    }

    public ShellScreenView getActiveScreen() {
        return activeScreen;
    }

    public String getDemonstrationValue() {
        return demonstrationValue;
    }

    public void setDemonstrationValue(String demonstrationValue) {
        this.demonstrationValue = demonstrationValue;
    }

    public String getRequestedActionId() {
        return requestedActionId;
    }

    public void setRequestedActionId(String requestedActionId) {
        this.requestedActionId = requestedActionId;
    }

    public String getSelectedResourceId() {
        return selectedResourceId;
    }

    public void setSelectedResourceId(String selectedResourceId) {
        this.selectedResourceId = selectedResourceId;
    }

    public Long getSelectedResourceVersion() {
        return selectedResourceVersion;
    }

    public void setSelectedResourceVersion(Long selectedResourceVersion) {
        this.selectedResourceVersion = selectedResourceVersion;
    }

    public String getSelectorContextId() {
        return selectorContextId == null ? "" : selectorContextId;
    }

    public void setSelectorContextId(String selectorContextId) {
        this.selectorContextId = normalizedToken(selectorContextId);
        prepared = false;
    }

    public String getSelectorReturnId() {
        return selectorReturnId == null ? "" : selectorReturnId;
    }

    public void setSelectorReturnId(String selectorReturnId) {
        this.selectorReturnId = normalizedToken(selectorReturnId);
        prepared = false;
    }

    public boolean isSelectorReturnAvailable() {
        return selectorReturnAvailable;
    }

    public boolean isSelectorReturnRestored() {
        return selectorReturnRestored;
    }

    public String getSelectorReturnLabel() {
        return selectorReturnLabel;
    }

    public Map<String, String> getInputValues() {
        return inputValues;
    }

    public String selectedOptionLabel(String fieldId) {
        return activeInteraction.selectedOptionLabel(fieldId, inputValues.get(fieldId));
    }

    public Map<String, String> getSelectorSearchValues() {
        return selectorSearchValues;
    }

    public Map<String, Integer> getSelectorSearchOffsets() {
        return selectorSearchOffsets;
    }

    public Map<String, ShellSelectorOptionPageView> getSelectorOptionPages() {
        return selectorOptionPages;
    }

    public int getTableOffset() {
        return tableOffset;
    }

    public void setTableOffset(int tableOffset) {
        this.tableOffset = tableOffset;
    }

    public int getTablePageSize() {
        return tablePageSize;
    }

    public void setTablePageSize(int tablePageSize) {
        this.tablePageSize = tablePageSize;
    }

    public int getRequestedTablePageDirection() {
        return requestedTablePageDirection;
    }

    public void setRequestedTablePageDirection(int requestedTablePageDirection) {
        this.requestedTablePageDirection = requestedTablePageDirection;
    }

    public ShellScreenInteractionView getActiveInteraction() {
        return activeInteraction;
    }

    public List<ShellCompanyOptionView> getCompanyOptions() {
        return companyOptions;
    }

    public List<ShellMenuItemView> getMenuItems() {
        return menuItems;
    }

    private static String normalized(String value, Set<String> accepted, String fallback) {
        if (value == null) {
            return fallback;
        }
        String candidate = value.strip().toLowerCase(java.util.Locale.ROOT);
        return accepted.contains(candidate) ? candidate : fallback;
    }

    private static String normalizedTab(String value) {
        if (value == null) {
            return "summary";
        }
        String candidate = value.strip().toLowerCase(java.util.Locale.ROOT);
        return candidate.matches("[a-z][a-z0-9_]{0,63}") ? candidate : "summary";
    }

    private static String normalizedToken(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String candidate = value.strip().toLowerCase(java.util.Locale.ROOT);
        try {
            return java.util.UUID.fromString(candidate).toString().equals(candidate)
                    ? candidate
                    : null;
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }

    private static String normalizedAction(String value) {
        if (value == null || value.isBlank() || value.length() > 80) {
            return null;
        }
        try {
            return new ScreenElementId(value.strip()).value();
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }
}
