package py.com.logixone.plugins.inventory.api;

/** Neutral origin reference; it never exposes another plugin's DTO or entity. */
public record StockSourceReference(String sourceType, String sourceId) {
    public StockSourceReference {
        sourceType = ContractValues.code(sourceType, "sourceType", 64);
        sourceId = ContractValues.text(sourceId, "sourceId", 160);
    }
}
