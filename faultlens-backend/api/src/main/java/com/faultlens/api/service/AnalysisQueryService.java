package com.faultlens.api.service;

import com.faultlens.analyzer.model.Analysis;
import com.faultlens.analyzer.repository.AnalysisRepository;
import com.faultlens.analyzer.service.AnalyzerService;
import com.faultlens.api.mapper.ApiDtoMapper;
import com.faultlens.common.dto.AnalysisResultDto;
import com.faultlens.common.dto.PagedResponse;
import com.faultlens.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisQueryService {
    private final AnalysisRepository repository;
    private final AnalyzerService analyzerService;
    private final ApiDtoMapper mapper;

    /**
     * Lists analyses.
     */
    public PagedResponse<AnalysisResultDto> list(int page, int size) {
        Page<Analysis> result = repository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "analyzedAt")));
        return PagedResponse.<AnalysisResultDto>builder()
                .content(result.map(mapper::toAnalysisDto).getContent())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    /**
     * Returns analysis detail.
     */
    public AnalysisResultDto get(Long id) {
        return repository.findById(id)
                .map(mapper::toAnalysisDto)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found: " + id));
    }

    /**
     * Triggers analysis for a group.
     */
    @Transactional
    public AnalysisResultDto trigger(Long groupId) {
        return analyzerService.analyzeGroup(groupId);
    }
}
