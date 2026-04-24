package com.javatraining.datarest.event;

import com.javatraining.datarest.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.rest.core.annotation.HandleAfterCreate;
import org.springframework.data.rest.core.annotation.HandleAfterDelete;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

/**
 * @RepositoryEventHandler — listens for Spring Data REST repository lifecycle events.
 *
 * Event types (before/after variants for each):
 *   BeforeCreate / AfterCreate   — fired on POST
 *   BeforeSave   / AfterSave     — fired on PUT and PATCH
 *   BeforeDelete / AfterDelete   — fired on DELETE
 *   BeforeLinkSave / AfterLinkSave — fired when associations are changed via PUT on a link
 *
 * The class-level @RepositoryEventHandler(Product.class) scopes all handlers in this
 * class to Product entities only. Without it, the handlers receive events for all types.
 *
 * Method parameter type determines which entity type the handler receives.
 * Multiple entity types can be handled in one class by annotating with multiple params.
 */
@Component
@RepositoryEventHandler
public class ProductEventHandler {

    private static final Logger log = LoggerFactory.getLogger(ProductEventHandler.class);

    /**
     * Runs before INSERT (POST /api/products).
     * Good place to: set defaults, normalise data, enforce invariants.
     */
    @HandleBeforeCreate
    public void handleBeforeCreate(Product product) {
        // Normalise category to uppercase — enforced at the persistence boundary
        if (product.getCategory() != null) {
            product.setCategory(product.getCategory().toUpperCase());
        }
        // Set the default for active (entity field has no @Builder.Default here)
        product.setActive(true);
    }

    @HandleAfterCreate
    public void handleAfterCreate(Product product) {
        log.info("Product created: id={} name={}", product.getId(), product.getName());
    }

    /**
     * Runs before UPDATE (PUT or PATCH /api/products/{id}).
     * Normalise category on update too so the invariant holds regardless of operation.
     */
    @HandleBeforeSave
    public void handleBeforeSave(Product product) {
        if (product.getCategory() != null) {
            product.setCategory(product.getCategory().toUpperCase());
        }
    }

    @HandleAfterDelete
    public void handleAfterDelete(Product product) {
        log.info("Product deleted: id={}", product.getId());
    }
}
