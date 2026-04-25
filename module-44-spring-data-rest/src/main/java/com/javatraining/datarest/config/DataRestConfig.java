package com.javatraining.datarest.config;

import com.javatraining.datarest.model.Product;
import com.javatraining.datarest.projection.ProductSummary;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

/**
 * RepositoryRestConfigurer - programmatic configuration for Spring Data REST.
 *
 * Use this for settings that cannot be expressed in application.properties:
 *
 *   exposeIdsFor(Class...)
 *     Include the entity ID field in the response body.
 *     Without this: { "name": "Laptop", "_links": { "self": { "href": ".../products/1" } } }
 *     With this:    { "id": 1, "name": "Laptop", "_links": { "self": { ... } } }
 *
 *   getProjectionConfiguration().addProjection(Class)
 *     Spring Data REST auto-discovers @Projection interfaces only when they are in the
 *     same package as the entity class (or a sub-package of it). Projections in other
 *     packages must be explicitly registered here.
 *
 * Settings also available in application.properties:
 *   spring.data.rest.base-path, default-page-size, return-body-on-create, etc.
 */
@Configuration
public class DataRestConfig implements RepositoryRestConfigurer {

    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config,
                                                      CorsRegistry cors) {
        config.exposeIdsFor(Product.class);

        // Register projection explicitly - it lives in the projection package, not the
        // entity package, so Spring Data REST won't auto-discover it via package scan.
        config.getProjectionConfiguration().addProjection(ProductSummary.class);
    }
}
