package com.faultlens.api.service;

import com.faultlens.analyzer.model.Deployment;
import com.faultlens.analyzer.repository.DeploymentRepository;
import com.faultlens.api.dto.DeploymentDto;
import com.faultlens.api.mapper.ApiDtoMapper;
import com.faultlens.common.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DeploymentService {
    private final DeploymentRepository repository;
    private final ApiDtoMapper mapper;

    /**
     * Lists deployment history.
     */
    @Transactional(readOnly = true)
    public PagedResponse<DeploymentDto> list(int page, int size) {
        Page<DeploymentDto> result = repository.findAllByOrderByDeployedAtDesc(PageRequest.of(page, size))
                .map(mapper::toDeploymentDto);
        return PagedResponse.<DeploymentDto>builder()
                .content(result.getContent())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    /**
     * Creates a deployment record.
     */
    public DeploymentDto create(DeploymentDto deployment) {
        return mapper.toDeploymentDto(repository.save(mapper.toDeploymentEntity(deployment)));
    }
}
