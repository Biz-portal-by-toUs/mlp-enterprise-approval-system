package com.multi.mlpenterpriseapprovalsystem.schedule.todo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 투두리스트 서비스
 *
 * @author : 권지영
 * @filename : TodoService
 * @since : 2026. 1. 4. 일요일
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoService {
}
