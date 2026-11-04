package com.ecommerce.customer.service;

import com.ecommerce.common.exception.BusinessRuleException;
import com.ecommerce.common.web.PageResponse;
import com.ecommerce.customer.dto.CustomerRequest;
import com.ecommerce.customer.dto.CustomerResponse;
import com.ecommerce.customer.entity.Customer;
import com.ecommerce.customer.exception.CustomerNotFoundException;
import com.ecommerce.customer.exception.DuplicateCustomerException;
import com.ecommerce.customer.mapper.CustomerMapper;
import com.ecommerce.customer.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de negocio de clientes.
 *
 * <p>Dependencias sao recebidas pelo construtor (injecao de dependencia): isso deixa as dependencias
 * explicitas, permite campos {@code final} e facilita os testes, pois basta passar mocks no construtor.</p>
 */
@Service
public class CustomerService {

    private static final Logger LOG = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository repository;
    private final CustomerMapper mapper;

    public CustomerService(CustomerRepository repository, CustomerMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        Customer customer = mapper.toEntity(request);
        if (repository.existsByEmail(customer.getEmail())) {
            throw new DuplicateCustomerException("email");
        }
        if (repository.existsByDocument(customer.getDocument())) {
            throw new DuplicateCustomerException("document");
        }
        Customer saved = repository.save(customer);
        LOG.info("Customer created id={}", saved.getId());
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CustomerResponse findById(Long id) {
        return mapper.toResponse(getCustomer(id));
    }

    /**
     * Lista clientes paginados.
     *
     * @param active filtro opcional: {@code null} retorna todos
     */
    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> findAll(Boolean active, Pageable pageable) {
        Page<Customer> page = active == null
                ? repository.findAll(pageable)
                : repository.findByActive(active, pageable);
        return PageResponse.from(page, mapper::toResponse);
    }

    /**
     * Atualiza os dados cadastrais. O CPF e imutavel: identifica a pessoa e nao deve mudar.
     */
    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = getCustomer(id);
        if (!customer.getDocument().equals(mapper.normalizeDocument(request.document()))) {
            throw new BusinessRuleException("The customer document (CPF) cannot be changed");
        }
        if (repository.existsByEmailAndIdNot(mapper.normalizeEmail(request.email()), id)) {
            throw new DuplicateCustomerException("email");
        }
        mapper.updateEntity(customer, request);
        // Nao precisamos chamar save(): a entidade esta gerenciada e o Hibernate grava as mudancas
        // no commit da transacao ("dirty checking"). saveAndFlush forca o UPDATE agora para que
        // updatedAt e version retornem atualizados na resposta.
        return mapper.toResponse(repository.saveAndFlush(customer));
    }

    /**
     * Desativa o cliente (exclusao logica). Pedidos antigos continuam referenciando o cliente,
     * por isso nao apagamos o registro.
     */
    @Transactional
    public void deactivate(Long id) {
        Customer customer = getCustomer(id);
        customer.deactivate();
        LOG.info("Customer deactivated id={}", id);
    }

    @Transactional
    public CustomerResponse activate(Long id) {
        Customer customer = getCustomer(id);
        customer.activate();
        return mapper.toResponse(repository.saveAndFlush(customer));
    }

    private Customer getCustomer(Long id) {
        return repository.findById(id).orElseThrow(() -> new CustomerNotFoundException(id));
    }
}
