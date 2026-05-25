package com.loglens.api.service;

import com.loglens.analyzer.model.Analysis;
import com.loglens.analyzer.repository.AnalysisRepository;
import com.loglens.analyzer.service.AnalyzerService;
import com.loglens.api.mapper.ApiDtoMapper;
import com.loglens.common.dto.AnalysisResultDto;
import com.loglens.common.dto.PagedResponse;
import com.loglens.common.exception.ResourceNotFoundException;
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
