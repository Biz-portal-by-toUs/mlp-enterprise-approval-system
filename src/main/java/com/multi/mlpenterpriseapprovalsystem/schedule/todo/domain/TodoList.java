package com.multi.mlpenterpriseapprovalsystem.schedule.todo.domain;

import com.multi.mlpenterpriseapprovalsystem.common.domain.BaseEntity;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 투두리스트 엔티티
 *
 * @author : 김승기
 * @filename : TodoList
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "todo_list")
public class TodoList extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long todoNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", referencedColumnName = "emp_id")
    private Employee employee;

    private String title;
    private Boolean isDone;

    public static TodoList create(Employee employee, String title) {
        TodoList t = new TodoList();
        t.employee = employee;
        t.title = title;
        t.isDone = false;
        return t;
    }

    public void update(String title, Boolean isDone) {
        if (title != null) this.title = title;
        if (isDone != null) this.isDone = isDone;
    }

    public void toggleDone() {
        this.isDone = (this.isDone == null) ? Boolean.TRUE : !this.isDone;
    }
}