package py.com.logixone.plugins.commercialcatalog.domain;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Controlled classification owned by commercial_catalog. */
public record CatalogClassification(
        CategoryId mainCategory,
        Set<CategoryId> secondaryCategories,
        Optional<BrandId> brand,
        Set<TagId> tags) {

    public CatalogClassification {
        Objects.requireNonNull(mainCategory, "mainCategory");
        secondaryCategories = Set.copyOf(Objects.requireNonNull(secondaryCategories, "secondaryCategories"));
        brand = Objects.requireNonNull(brand, "brand");
        tags = Set.copyOf(Objects.requireNonNull(tags, "tags"));
        if (secondaryCategories.contains(mainCategory)) {
            throw new IllegalArgumentException("Main category cannot also be secondary");
        }
    }
}
