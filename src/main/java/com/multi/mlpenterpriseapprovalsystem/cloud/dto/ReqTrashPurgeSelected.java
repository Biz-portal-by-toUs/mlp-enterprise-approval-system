package com.multi.mlpenterpriseapprovalsystem.cloud.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.hibernate.cache.spi.support.AbstractReadWriteAccess;

import java.util.List;

/**
 * 휴지통에서 사용자가 선택한 항목(파일/폴더)을 영구 삭제(PURGE)하기 위한 요청 DTO입니다.
 *
 * items에는 삭제 대상의 유형(type: FILE/FOLDER)과 식별자(id)를 포함하며,
 * 리스트는 비어있을 수 없고 각 항목은 유효성 검증(@Valid)을 수행합니다.
 *
 * @author : 송현님
 * @filename : ReqTrashPurgeSelected
 * @since : 2026-01-07 오후 1:18 수요일
 */
public class ReqTrashPurgeSelected {

    @NotEmpty
    private List<@Valid Item> items;

    public ReqTrashPurgeSelected() {
    }

    public ReqTrashPurgeSelected(List<Item> items) {
        this.items = items;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public static class Item {

        @NotNull
        private String type; // "FILE" | "FOLDER"

        @NotNull
        private Long id;

        public Item() {
        }

        public Item(String type, Long id) {
            this.type = type;
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }
}
