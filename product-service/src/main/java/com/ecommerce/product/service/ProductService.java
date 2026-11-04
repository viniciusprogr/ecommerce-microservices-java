package com.ecommerce.product.service;

import com.ecommerce.common.web.PageResponse;
import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.exception.CatalogConflictException;
import com.ecommerce.product.exception.ProductNotFoundException;
import com.ecommerce.product.mapper.ProductMapper;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.repository.ProductSpecifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Regras de negocio de produtos. */
@Service
public class ProductService {

    private static final Logger LOG = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final ProductMapper mapper;

    public ProductService(ProductRepository productRepository, CategoryService categoryService, ProductMapper mapper) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
        this.mapper = mapper;
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new CatalogConflictException("A product with SKU '" + request.sku() + "' already exists");
        }
        Category category = categoryService.getCategory(request.categoryId());
        Product saved = productRepository.save(mapper.toEntity(request, category));
        LOG.info("Product created id={} sku={}", saved.getId(), saved.getSku());
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return mapper.toResponse(getProduct(id));
    }

    /**
     * Lista produtos combinando apenas os filtros informados.
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> findAll(Long categoryId, Boolean active, String name, Pageable pageable) {
        Specification<Product> spec = (root, query, cb) -> cb.conjunction();
        if (categoryId != null) {
            spec = spec.and(ProductSpecifications.hasCategory(categoryId));
        }
        if (active != null) {
            spec = spec.and(ProductSpecifications.isActive(active));
        }
        if (StringUtils.hasText(name)) {
            spec = spec.and(ProductSpecifications.nameContains(name));
        }
        return PageResponse.from(productRepository.findAll(spec, pageable), mapper::toResponse);
    }

    /** O SKU e o identificador de negocio e nao pode ser alterado. */
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = getProduct(id);
        if (!product.getSku().equals(request.sku())) {
            throw new CatalogConflictException("The SKU of a product cannot be changed");
        }
        Category category = categoryService.getCategory(request.categoryId());
        product.update(request.name().trim(), request.description(), request.price(), category);
        return mapper.toResponse(productRepository.saveAndFlush(product));
    }

    @Transactional
    public ProductResponse activate(Long id) {
        Product product = getProduct(id);
        product.activate();
        return mapper.toResponse(productRepository.saveAndFlush(product));
    }

    /** Exclusao logica: produtos inativos nao podem ser vendidos, mas pedidos antigos continuam validos. */
    @Transactional
    public void deactivate(Long id) {
        getProduct(id).deactivate();
        LOG.info("Product deactivated id={}", id);
    }

    private Product getProduct(Long id) {
        return productRepository.findWithCategoryById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }
}
